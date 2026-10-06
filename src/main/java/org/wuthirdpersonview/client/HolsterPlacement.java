package org.wuthirdpersonview.client;

import com.wurmonline.client.renderer.Matrix;
import com.wurmonline.client.renderer.cell.AttachedCellRenderable;
import com.wurmonline.client.renderer.model.AbstractModelData;
import com.wurmonline.client.renderer.model.ModelResourceWrapper;
import com.wurmonline.client.renderer.model.collada.AbstractColladaModelData;
import com.wurmonline.client.renderer.model.collada.animation.Joint;
import com.wurmonline.client.renderer.model.collada.importer.ColladaModel;
import com.wurmonline.client.renderer.model.collada.importer.ColladaOffset;
import com.wurmonline.client.renderer.model.collada.math.Matrix4f;
import com.wurmonline.client.renderer.model.collada.math.Quaternion;
import com.wurmonline.math.Vector3f;
import org.wuthirdpersonview.core.HolsterItemClass;

/** A skeleton mount, an exact model attachment point, and their relative pose. */
final class HolsterPlacement {
    static final int LEFT_HAND_SLOT = 0;
    static final int RIGHT_HAND_SLOT = 1;
    static final int SHIELD_SLOT = 11;

    final String anchor;
    final float x;
    final float y;
    final float z;
    final float modelX;
    final float modelY;
    final float modelZ;
    final float rotationX;
    final float rotationY;
    final float rotationZ;
    final float scale;
    final float transferClearance;

    private HolsterPlacement(String anchor, float x, float y, float z,
                             float modelX, float modelY, float modelZ,
                             float rotationX, float rotationY, float rotationZ,
                             float scale, float transferClearance) {
        this.anchor = anchor;
        this.x = x;
        this.y = y;
        this.z = z;
        this.modelX = modelX;
        this.modelY = modelY;
        this.modelZ = modelZ;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
        this.scale = scale;
        this.transferClearance = transferClearance;
    }

    static HolsterPlacement forItem(HolsterItemClass type, int slot) {
        // Wurm's bind skeleton uses negative X for the character's right side.
        boolean left = slot == LEFT_HAND_SLOT;
        float side = left ? 1.0f : -1.0f;
        switch (type) {
            case SHORT_BELT:
                return new HolsterPlacement("Spine", side * 0.28f,
                        -0.12f, -0.10f, 0.0f, 0.0f, 0.0f,
                        -90.0f, 0.0f, left ? -8.0f : 8.0f,
                        1.0f, 0.08f);
            case MEDIUM_SHOULDER:
                // A secondary dual-wield item is mirrored to prevent overlap.
                return new HolsterPlacement("SpineTop", side * 0.23f,
                        0.23f, -0.17f, 0.0f, 0.0f, 0.0f,
                        -90.0f, 0.0f, left ? -18.0f : 18.0f,
                        1.0f, 0.13f);
            case TWO_HANDED_SWORD_CENTER:
                return new HolsterPlacement("SpineTop", -0.02f, 0.38f, -0.12f,
                        0.0f, 0.03f, 0.02f,
                        -90.0f, 0.0f, 0.0f, 1.0f, 0.16f);
            case TWO_HANDED_DIAGONAL:
                return new HolsterPlacement("SpineTop", -0.24f, 0.30f, -0.19f,
                        0.0f, 0.0f, 0.0f,
                        -90.0f, 0.0f, 25.0f, 1.0f, 0.18f);
            case SHIELD_LARGE:
                return new HolsterPlacement("SpineTop", 0.0f, 0.05f, -0.25f,
                        0.0f, 0.04f, 0.0f,
                        -90.0f, 0.0f, 180.0f, 1.0f, 0.20f);
            case SHIELD_MEDIUM:
                return new HolsterPlacement("SpineTop", 0.0f, 0.06f, -0.22f,
                        0.0f, 0.04f, 0.0f,
                        -90.0f, 0.0f, 180.0f, 1.0f, 0.17f);
            case SHIELD_SMALL:
            default:
                return new HolsterPlacement("SpineTop", 0.0f, 0.08f, -0.19f,
                        0.0f, 0.04f, 0.0f,
                        -90.0f, 0.0f, 180.0f, 1.0f, 0.14f);
        }
    }

    HolsterPlacement withValues(String newAnchor,
                                float newX, float newY, float newZ,
                                float newModelX, float newModelY, float newModelZ,
                                float newRotationX, float newRotationY,
                                float newRotationZ) {
        return new HolsterPlacement(newAnchor, newX, newY, newZ,
                newModelX, newModelY, newModelZ,
                newRotationX, newRotationY, newRotationZ,
                scale, transferClearance);
    }

    HolsterPlacement withAll(String newAnchor,
                             float newX, float newY, float newZ,
                             float newModelX, float newModelY, float newModelZ,
                             float newRotationX, float newRotationY,
                             float newRotationZ, float newScale,
                             float newTransferClearance) {
        return new HolsterPlacement(newAnchor, newX, newY, newZ,
                newModelX, newModelY, newModelZ,
                newRotationX, newRotationY, newRotationZ,
                newScale, newTransferClearance);
    }

    Matrix localMatrix(AttachedCellRenderable attachment, String sourceAnchor,
                       boolean male) {
        return localMatrix(sourceOffset(attachment, sourceAnchor), male);
    }

    Matrix localMatrix(ColladaOffset offset, boolean male) {
        Quaternion adjustment = new Quaternion();
        adjustment.fromAngles(radians(rotationX), radians(rotationY), radians(rotationZ));
        Matrix4f mount = new Matrix4f();
        adjustment.toRotationMatrix(mount);
        // Hand offsets contain the item's held orientation. Carry mounts use the
        // model's raw axes and retain only its gender-specific scale.
        float itemScale = offset == null ? 1.0f : offset.getScale(male);
        float carriedScale = scale * itemScale;
        mount.scale(new Vector3f(carriedScale, carriedScale, carriedScale));
        Vector3f attachedPoint = mount.mult(new Vector3f(modelX, modelY, modelZ));
        mount.setTranslation(x - attachedPoint.x,
                y - attachedPoint.y, z - attachedPoint.z);
        return new Matrix().fromColladaMatrix(mount);
    }

    Vector3f targetPoint(com.wurmonline.client.renderer.model.collada.ColladaModelData data) {
        return point(data, x, y, z);
    }

    /** Transforms a physical model-space grip through the edited carry pose. */
    GripPose animationGripPose(
            com.wurmonline.client.renderer.model.collada.ColladaModelData data,
            AttachedCellRenderable attachment, String sourceAnchor, boolean male,
            Vector3f modelGrip) {
        return animationGripPose(anchorTransform(data),
                sourceOffset(attachment, sourceAnchor), male, modelGrip);
    }

    GripPose animationGripPose(Matrix4f anchorMatrix, ColladaOffset offset,
                               boolean male, Vector3f modelGrip) {
        if (anchorMatrix == null || modelGrip == null) return null;
        Matrix carry = localMatrix(offset, male);
        Vector3f anchorGrip = transformPoint(carry,
                modelGrip.x, modelGrip.y, modelGrip.z);
        Vector3f anchorAxisEnd = transformPoint(carry,
                modelGrip.x, modelGrip.y, modelGrip.z + 1.0f);
        Vector3f grip = point(anchorMatrix,
                anchorGrip.x, anchorGrip.y, anchorGrip.z);
        Vector3f axisEnd = point(anchorMatrix,
                anchorAxisEnd.x, anchorAxisEnd.y, anchorAxisEnd.z);
        if (grip == null || axisEnd == null) return null;
        Vector3f axis = axisEnd.subtract(grip);
        if (axis.lengthSquared() < 0.000001f) return null;
        return new GripPose(grip, axis.normalizeLocal());
    }

    Matrix mountMatrix(com.wurmonline.client.renderer.model.collada.ColladaModelData data,
                       Matrix parentMatrix) {
        return composeMount(parentMatrix, anchorTransform(data));
    }

    Matrix4f anchorTransform(
            com.wurmonline.client.renderer.model.collada.ColladaModelData data) {
        if (data == null) return null;
        if (data.hasNull(anchor)) {
            Matrix4f transform = data.getNullTransformation(anchor, new Matrix4f());
            if (transform != null) return transform;
        }
        return jointAnchorTransform(data.getModel(), anchor);
    }

    static Matrix4f jointAnchorTransform(ColladaModel model, String anchor) {
        if (model == null || anchor == null) return null;
        Joint joint = model.getJoint(anchor);
        if (joint == null && "Spine".equals(anchor)) joint = model.getJoint("spine_01");
        if (joint == null && "SpineTop".equals(anchor)) joint = model.getJoint("spine_03");
        return joint == null || joint.getBindMatrix() == null
                ? null : new Matrix4f(joint.getBindMatrix());
    }

    static Matrix composeMount(Matrix parentMatrix, Matrix4f anchorTransform) {
        if (anchorTransform == null || parentMatrix == null) return null;
        Matrix anchorMatrix = new Matrix().fromColladaMatrix(anchorTransform);
        Matrix result = new Matrix();
        Matrix.mult(parentMatrix, anchorMatrix, result);
        return result;
    }

    Vector3f point(com.wurmonline.client.renderer.model.collada.ColladaModelData data,
                   float localX, float localY, float localZ) {
        return point(anchorTransform(data), localX, localY, localZ);
    }

    static Vector3f point(Matrix4f anchorTransform,
                          float localX, float localY, float localZ) {
        return anchorTransform == null ? null
                : anchorTransform.mult(new Vector3f(localX, localY, localZ));
    }

    static Vector3f transformPoint(Matrix matrix, float localX, float localY,
                                   float localZ) {
        return new Vector3f(
                localX * matrix._m00() + localY * matrix._m10()
                        + localZ * matrix._m20() + matrix._m30(),
                localX * matrix._m01() + localY * matrix._m11()
                        + localZ * matrix._m21() + matrix._m31(),
                localX * matrix._m02() + localY * matrix._m12()
                        + localZ * matrix._m22() + matrix._m32());
    }

    private static ColladaOffset sourceOffset(AttachedCellRenderable attachment,
                                              String sourceAnchor) {
        ModelResourceWrapper wrapper = attachment == null ? null : attachment.getModelWrapper();
        AbstractModelData data = wrapper == null ? null : wrapper.getModelData();
        if (!(data instanceof AbstractColladaModelData)) return null;
        ColladaModel model = ((AbstractColladaModelData) data).getModel();
        return model == null ? null : model.getModelsOffsetToNullList().get(sourceAnchor);
    }

    private static float radians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    static final class GripPose {
        final Vector3f point;
        final Vector3f axis;

        GripPose(Vector3f point, Vector3f axis) {
            this.point = point;
            this.axis = axis;
        }
    }
}
