package org.wuthirdpersonview.client;

import com.wurmonline.client.game.PlayerObj;
import com.wurmonline.client.renderer.PlayerBodyRenderable;
import com.wurmonline.client.renderer.cell.AttachedCellRenderable;
import com.wurmonline.client.renderer.model.AbstractModelData;
import com.wurmonline.client.renderer.model.ModelResourceWrapper;
import com.wurmonline.client.renderer.model.collada.ColladaModelData;
import com.wurmonline.client.renderer.model.collada.animation.Joint;
import com.wurmonline.client.renderer.model.collada.importer.ColladaModel;
import com.wurmonline.client.renderer.model.collada.math.Matrix4f;
import com.wurmonline.client.renderer.model.collada.math.Quaternion;
import com.wurmonline.math.Vector3f;
import java.util.List;
import org.wuthirdpersonview.core.CameraMath;
import org.wuthirdpersonview.core.HolsterItemClass;
import org.wuthirdpersonview.core.HolsterSnapshot;

/** Adds an anatomically constrained, staged arm reach over native animation. */
final class HolsterArmAnimator {
    private static final float ACTIVE_EPSILON = 0.001f;
    private static final float ARM_ELBOW_FOLD_START = 0.24f;
    private static final float ARM_SHOULDER_PREPARED = 0.44f;
    private static final float ARM_SHOULDER_RETURN_START = 0.58f;
    private static final float ARM_AT_MOUNT = 0.62f;
    private static final float ARM_ELBOW_RETURNED = 0.84f;
    private static final float CENTER_SWORD_UPPER_ARM_LEAN_RADIANS =
            (float) Math.toRadians(8.0f);
    private static final float MIN_ELBOW_FLEX_RADIANS =
            (float) Math.toRadians(5.0f);
    private static final float MAX_ELBOW_FLEX_RADIANS =
            (float) Math.toRadians(140.0f);
    private static final float MAX_SHOULDER_TWIST_RADIANS =
            (float) Math.toRadians(80.0f);

    private volatile ColladaModelData targetModelData;
    private volatile ArmPlan leftPlan;
    private volatile ArmPlan rightPlan;
    private volatile float weaponAmount;
    private volatile float shieldAmount;
    private volatile float weaponReach;
    private volatile float shieldReach;
    private volatile boolean active;

    void update(PlayerObj player, HolsterSnapshot state) {
        if (player == null || state == null || !state.isTransitioning()) {
            deactivate();
            return;
        }
        PlayerBodyRenderable body = player.getPlayerBody();
        if (body == null || body.getEquippedTool() != null) {
            deactivate();
            return;
        }
        ModelResourceWrapper wrapper = body.getModelWrapper();
        AbstractModelData modelData = wrapper == null ? null : wrapper.getModelData();
        if (!(modelData instanceof ColladaModelData)) {
            deactivate();
            return;
        }

        AttachedCellRenderable left = body.getEquipment(HolsterPlacement.LEFT_HAND_SLOT);
        AttachedCellRenderable right = body.getEquipment(HolsterPlacement.RIGHT_HAND_SLOT);
        AttachedCellRenderable shield = body.getEquipment(HolsterPlacement.SHIELD_SLOT);
        ArmPlan plannedLeft = plan(body, left, HolsterPlacement.LEFT_HAND_SLOT);
        ArmPlan plannedRight = plan(body, right, HolsterPlacement.RIGHT_HAND_SLOT);
        if (shield != null && state.reachForShield() > ACTIVE_EPSILON)
            plannedLeft = plan(body, shield, HolsterPlacement.SHIELD_SLOT);

        targetModelData = (ColladaModelData) modelData;
        leftPlan = plannedLeft;
        rightPlan = plannedRight;
        weaponAmount = state.weaponAmount;
        shieldAmount = state.shieldAmount;
        weaponReach = state.reachForWeapon();
        shieldReach = state.reachForShield();
        active = (plannedLeft != null || plannedRight != null)
                && (weaponReach > ACTIVE_EPSILON || shieldReach > ACTIVE_EPSILON);
    }

    void deactivate() {
        active = false;
        targetModelData = null;
        leftPlan = null;
        rightPlan = null;
        weaponAmount = 0.0f;
        shieldAmount = 0.0f;
        weaponReach = 0.0f;
        shieldReach = 0.0f;
    }

    void applyBeforeSkinning(ColladaModelData modelData) {
        if (!active || modelData == null || modelData != targetModelData) return;
        ColladaModel model = modelData.getModel();
        if (model == null) return;

        ArmPlan left = leftPlan;
        ArmPlan right = rightPlan;
        boolean twoHanded = (left != null && left.twoHanded)
                || (right != null && right.twoHanded);
        if (twoHanded) {
            ArmPlan item = right != null && right.twoHanded ? right : left;
            if (weaponReach > ACTIVE_EPSILON) {
                if (item.type == HolsterItemClass.TWO_HANDED_SWORD_CENTER)
                    applyCenterSword(modelData, model, item, weaponAmount);
                else if (usesSupportHand(item.type))
                    applyTwoHanded(modelData, model, item, weaponAmount);
                else
                    applyStaged(modelData, model, item, weaponAmount);
            }
            return;
        }

        if (left != null) {
            float reach = left.shield ? shieldReach : weaponReach;
            float amount = left.shield ? shieldAmount : weaponAmount;
            if (reach > ACTIVE_EPSILON)
                applyStaged(modelData, model, left, amount);
        }
        if (right != null && weaponReach > ACTIVE_EPSILON)
            applyStaged(modelData, model, right, weaponAmount);
    }

    private static void applyTwoHanded(ColladaModelData data, ColladaModel model,
                                       ArmPlan item, float amount) {
        if (data == null || item == null) return;
        HolsterPlacement.GripPose gripPose = item.animationGripPose(data);
        if (gripPose == null) return;
        Vector3f grip = gripPose.point;
        Vector3f support = new Vector3f(grip);
        support.addLocal(0.24f, -0.30f, 0.02f);
        poseStagedArm(model, false, grip, amount,
                upperDirectionFor(item.type, false, false));
        poseStagedArm(model, true, support, amount,
                upperDirectionFor(item.type, true, true));
    }

    private static void applyStaged(ColladaModelData data, ColladaModel model,
                                    ArmPlan item, float amount) {
        if (data == null || item == null) return;
        HolsterPlacement.GripPose gripPose = item.animationGripPose(data);
        if (gripPose == null) return;
        poseStagedArm(model, item.leftHand, gripPose.point, amount,
                upperDirectionFor(item.type, item.leftHand, false));
    }

    /**
     * Poses a centre-back sword as a deliberately staged shoulder and elbow
     * movement. The upper arm first reaches a near-vertical preparation pose;
     * the second stage then solves both arm bones to the physical handle and
     * aligns the wrist with its axis. Drawing unwinds those movements in reverse.
     */
    private static void applyCenterSword(ColladaModelData data, ColladaModel model,
                                         ArmPlan item, float amount) {
        if (data == null || item == null) return;
        HolsterPlacement.GripPose gripPose = item.animationGripPose(data);
        if (gripPose == null) return;
        poseCenterSwordArm(model, item.leftHand, gripPose.point,
                gripPose.axis, amount);
    }

    static void poseCenterSwordArm(ColladaModel model, boolean left,
                                   Vector3f mountTarget, float amount) {
        poseCenterSwordArm(model, left, mountTarget, null, amount);
    }

    static void poseCenterSwordArm(ColladaModel model, boolean left,
                                   Vector3f gripTarget, Vector3f gripAxis,
                                   float amount) {
        poseStagedArm(model, left, gripTarget, amount,
                centerSwordUpperDirection(left));
        float lockWeight = centerSwordWristLockWeight(amount);
        if (lockWeight <= ACTIVE_EPSILON) return;
        Joint shoulder = model.getJoint(left ? "LeftArm" : "RightArm");
        Joint elbow = model.getJoint(left ? "LeftForeArm" : "RightForeArm");
        Joint hand = model.getJoint(left ? "LeftHand" : "RightHand");
        if (shoulder == null || elbow == null || hand == null) return;
        solveTwoBoneWrist(shoulder, elbow, hand, gripTarget, lockWeight);
        if (gripAxis != null && gripAxis.lengthSquared() > 0.000001f) {
            Vector3f currentAxis = handGripAxis(hand);
            rotateSubtreeWeighted(hand, currentAxis, gripAxis,
                    position(hand), lockWeight);
        }
    }

    static void solveTwoBoneWrist(Joint shoulder, Joint elbow, Joint hand,
                                  Vector3f target, float weight) {
        if (shoulder == null || elbow == null || hand == null || target == null)
            return;
        Vector3f shoulderPosition = position(shoulder);
        Vector3f elbowPosition = position(elbow);
        Vector3f handPosition = position(hand);
        Vector3f upper = elbowPosition.subtract(shoulderPosition);
        Vector3f lower = handPosition.subtract(elbowPosition);
        float upperLength = upper.length();
        float lowerLength = lower.length();
        Vector3f shoulderToTarget = target.subtract(shoulderPosition);
        float requestedDistance = shoulderToTarget.length();
        if (upperLength < 0.000001f || lowerLength < 0.000001f
                || requestedDistance < 0.000001f) return;

        Vector3f direction = shoulderToTarget.mult(1.0f / requestedDistance);
        float minimum = Math.abs(upperLength - lowerLength) + 0.00001f;
        float maximum = upperLength + lowerLength - 0.00001f;
        float distance = CameraMath.clamp(requestedDistance, minimum, maximum);
        Vector3f reachableTarget = shoulderPosition.add(direction.mult(distance));
        float along = (upperLength * upperLength - lowerLength * lowerLength
                + distance * distance) / (2.0f * distance);
        float height = (float) Math.sqrt(Math.max(0.0f,
                upperLength * upperLength - along * along));

        Vector3f bend = upper.subtract(direction.mult(upper.dot(direction)));
        if (bend.lengthSquared() < 0.000001f) {
            Vector3f reference = Math.abs(direction.y) < 0.9f
                    ? Vector3f.UNIT_Y : Vector3f.UNIT_Z;
            bend = direction.cross(reference);
        }
        if (bend.lengthSquared() < 0.000001f) return;
        bend.normalizeLocal();
        Vector3f desiredElbow = shoulderPosition.add(direction.mult(along))
                .addLocal(bend.mult(height));
        rotateSubtreeWeighted(shoulder, upper,
                desiredElbow.subtract(shoulderPosition), shoulderPosition, weight);

        Vector3f movedElbow = position(elbow);
        Vector3f movedHand = position(hand);
        rotateSubtreeWeighted(elbow, movedHand.subtract(movedElbow),
                reachableTarget.subtract(movedElbow), movedElbow, weight);
    }

    static Vector3f handGripAxis(Joint hand) {
        Matrix4f value = hand.getBindMatrix();
        Vector3f result = new Vector3f(-value.m02, -value.m12, -value.m22);
        return result.lengthSquared() < 0.000001f
                ? new Vector3f(0.0f, 0.0f, -1.0f)
                : result.normalizeLocal();
    }

    static void poseStagedArm(ColladaModel model, boolean left,
                              Vector3f mountTarget, float amount,
                              Vector3f preparedUpperDirection) {
        Joint shoulder = model.getJoint(left ? "LeftArm" : "RightArm");
        Joint elbow = model.getJoint(left ? "LeftForeArm" : "RightForeArm");
        Joint hand = model.getJoint(left ? "LeftHand" : "RightHand");
        if (shoulder == null || elbow == null || hand == null || mountTarget == null
                || preparedUpperDirection == null) return;

        Vector3f shoulderPosition = position(shoulder);
        Vector3f elbowPosition = position(elbow);
        Vector3f handPosition = position(hand);
        Vector3f nativeUpper = elbowPosition.subtract(shoulderPosition);
        Vector3f nativeLower = handPosition.subtract(elbowPosition);
        if (nativeUpper.lengthSquared() < 0.000001f
                || nativeLower.lengthSquared() < 0.000001f) return;

        float shoulderWeight = stagedShoulderWeight(amount);
        if (shoulderWeight > ACTIVE_EPSILON) {
            rotateSubtreeWeighted(shoulder, nativeUpper, preparedUpperDirection,
                    shoulderPosition, shoulderWeight);
            twistForearmTowardMount(shoulder, elbow, hand, mountTarget,
                    shoulderWeight);
        }

        float elbowWeight = stagedElbowWeight(amount);
        if (elbowWeight > ACTIVE_EPSILON) {
            Vector3f raisedElbow = position(elbow);
            Vector3f raisedHand = position(hand);
            Vector3f upper = raisedElbow.subtract(position(shoulder));
            Vector3f currentLower = raisedHand.subtract(raisedElbow);
            Vector3f desiredLower = constrainedForearmDirection(upper, currentLower,
                    mountTarget.subtract(raisedElbow));
            rotateSubtreeWeighted(elbow, currentLower, desiredLower, raisedElbow,
                    elbowWeight);
        }
    }

    /** The upper arm leans eight degrees forwards from vertical. */
    static Vector3f centerSwordUpperDirection(boolean left) {
        return new Vector3f(0.0f,
                (float) Math.cos(CENTER_SWORD_UPPER_ARM_LEAN_RADIANS),
                (float) Math.sin(CENTER_SWORD_UPPER_ARM_LEAN_RADIANS));
    }

    /** Comfortable preparation directions; X is lateral, Y up, and Z forwards. */
    static Vector3f upperDirectionFor(HolsterItemClass type, boolean left,
                                      boolean supportHand) {
        float side = left ? 1.0f : -1.0f;
        Vector3f result;
        switch (type) {
            case SHORT_BELT:
                result = new Vector3f(side * 0.20f, -0.96f, 0.18f);
                break;
            case MEDIUM_SHOULDER:
                result = new Vector3f(side * 0.52f, 0.79f, 0.31f);
                break;
            case TWO_HANDED_DIAGONAL:
                result = supportHand
                        ? new Vector3f(side * 0.66f, 0.66f, 0.35f)
                        : new Vector3f(side * 0.43f, 0.84f, 0.32f);
                break;
            case TWO_HANDED_SWORD_CENTER:
                return centerSwordUpperDirection(left);
            case SHIELD_SMALL:
            case SHIELD_MEDIUM:
            case SHIELD_LARGE:
            default:
                result = new Vector3f(side * 0.68f, 0.66f, 0.31f);
                break;
        }
        return result.normalizeLocal();
    }

    static float stagedShoulderWeight(float amount) {
        float value = CameraMath.clamp(amount, 0.0f, 1.0f);
        if (value <= ARM_SHOULDER_PREPARED)
            return smootherStep(value / ARM_SHOULDER_PREPARED);
        if (value < ARM_SHOULDER_RETURN_START) return 1.0f;
        return 1.0f - smootherStep((value - ARM_SHOULDER_RETURN_START)
                / (1.0f - ARM_SHOULDER_RETURN_START));
    }

    static float stagedElbowWeight(float amount) {
        float value = CameraMath.clamp(amount, 0.0f, 1.0f);
        if (value <= ARM_ELBOW_FOLD_START) return 0.0f;
        if (value <= ARM_AT_MOUNT)
            return smootherStep((value - ARM_ELBOW_FOLD_START)
                    / (ARM_AT_MOUNT - ARM_ELBOW_FOLD_START));
        if (value < ARM_ELBOW_RETURNED)
            return 1.0f - smootherStep((value - ARM_AT_MOUNT)
                    / (ARM_ELBOW_RETURNED - ARM_AT_MOUNT));
        return 0.0f;
    }

    static float centerSwordWristLockWeight(float amount) {
        float value = CameraMath.clamp(amount, 0.0f, 1.0f);
        if (value <= ARM_SHOULDER_PREPARED) return 0.0f;
        if (value <= ARM_AT_MOUNT)
            return smootherStep((value - ARM_SHOULDER_PREPARED)
                    / (ARM_AT_MOUNT - ARM_SHOULDER_PREPARED));
        if (value < ARM_ELBOW_RETURNED)
            return 1.0f - smootherStep((value - ARM_AT_MOUNT)
                    / (ARM_ELBOW_RETURNED - ARM_AT_MOUNT));
        return 0.0f;
    }

    private static void twistForearmTowardMount(Joint shoulder, Joint elbow, Joint hand,
                                                Vector3f mountTarget, float weight) {
        Vector3f shoulderPosition = position(shoulder);
        Vector3f elbowPosition = position(elbow);
        Vector3f upperAxis = elbowPosition.subtract(shoulderPosition);
        if (upperAxis.lengthSquared() < 0.000001f) return;
        upperAxis.normalizeLocal();

        Vector3f lower = position(hand).subtract(elbowPosition);
        Vector3f currentPlane = lower.subtract(upperAxis.mult(lower.dot(upperAxis)));
        Vector3f toMount = mountTarget.subtract(elbowPosition);
        Vector3f desiredPlane = toMount.subtract(upperAxis.mult(toMount.dot(upperAxis)));
        if (desiredPlane.lengthSquared() < 0.000001f) {
            Vector3f back = new Vector3f(0.0f, 0.0f, -1.0f);
            desiredPlane = back.subtract(upperAxis.mult(back.dot(upperAxis)));
        }
        if (currentPlane.lengthSquared() < 0.000001f
                || desiredPlane.lengthSquared() < 0.000001f) return;
        currentPlane.normalizeLocal();
        desiredPlane.normalizeLocal();

        float cosine = CameraMath.clamp(currentPlane.dot(desiredPlane), -1.0f, 1.0f);
        float sine = upperAxis.dot(currentPlane.cross(desiredPlane));
        float angle = CameraMath.clamp((float) Math.atan2(sine, cosine),
                -MAX_SHOULDER_TWIST_RADIANS, MAX_SHOULDER_TWIST_RADIANS) * weight;
        Quaternion twist = new Quaternion();
        twist.fromAngleAxis(angle, upperAxis);
        applyRotation(shoulder, twist, shoulderPosition);
    }

    static Vector3f constrainedForearmDirection(Vector3f upper, Vector3f currentLower,
                                                Vector3f requestedLower) {
        if (upper == null || currentLower == null || requestedLower == null
                || upper.lengthSquared() < 0.000001f
                || currentLower.lengthSquared() < 0.000001f
                || requestedLower.lengthSquared() < 0.000001f) return requestedLower;
        Vector3f upperAxis = new Vector3f(upper).normalizeLocal();
        Vector3f bend = currentLower.subtract(
                upperAxis.mult(currentLower.dot(upperAxis)));
        if (bend.lengthSquared() < 0.000001f) {
            bend = requestedLower.subtract(
                    upperAxis.mult(requestedLower.dot(upperAxis)));
        }
        if (bend.lengthSquared() < 0.000001f)
            bend = upperAxis.cross(Math.abs(upperAxis.y) < 0.9f
                    ? Vector3f.UNIT_Y : Vector3f.UNIT_X);
        bend.normalizeLocal();

        Vector3f request = new Vector3f(requestedLower).normalizeLocal();
        float along = CameraMath.clamp(request.dot(upperAxis), -1.0f, 1.0f);
        float across = request.dot(bend);
        float requestedFlex = across <= 0.0f ? MIN_ELBOW_FLEX_RADIANS
                : (float) Math.atan2(across, along);
        float flex = CameraMath.clamp(requestedFlex,
                MIN_ELBOW_FLEX_RADIANS, MAX_ELBOW_FLEX_RADIANS);
        return upperAxis.mult((float) Math.cos(flex))
                .addLocal(bend.mult((float) Math.sin(flex)));
    }

    static float elbowFlexDegrees(Vector3f upper, Vector3f lower) {
        if (upper == null || lower == null || upper.lengthSquared() < 0.000001f
                || lower.lengthSquared() < 0.000001f) return 0.0f;
        float cosine = new Vector3f(upper).normalizeLocal().dot(
                new Vector3f(lower).normalizeLocal());
        return (float) Math.toDegrees(Math.acos(
                CameraMath.clamp(cosine, -1.0f, 1.0f)));
    }

    static boolean usesSupportHand(HolsterItemClass type) {
        return type == HolsterItemClass.TWO_HANDED_DIAGONAL;
    }

    private static ArmPlan plan(PlayerBodyRenderable body,
                                AttachedCellRenderable attachment, int slot) {
        if (attachment == null) return null;
        HolsterItemClass type = HolsterEquipmentRenderer.classify(body, attachment, slot);
        HolsterPlacement placement = WUThirdPersonViewMod.holsterPlacement(
                attachment, type, slot);
        boolean shield = slot == HolsterPlacement.SHIELD_SLOT;
        boolean twoHanded = type == HolsterItemClass.TWO_HANDED_DIAGONAL
                || type == HolsterItemClass.TWO_HANDED_SWORD_CENTER;
        boolean leftHand = slot == HolsterPlacement.LEFT_HAND_SLOT || shield;
        HolsterPlacement gripReference = type == HolsterItemClass.TWO_HANDED_SWORD_CENTER
                ? HolsterPlacement.forItem(type, slot) : placement;
        Vector3f modelGrip = new Vector3f(gripReference.modelX,
                gripReference.modelY, gripReference.modelZ);
        return new ArmPlan(type, placement, attachment,
                HolsterEquipmentRenderer.sourceAnchor(slot), body.isMale(), modelGrip,
                shield, twoHanded, leftHand);
    }

    static void rotateSubtree(Joint joint, Vector3f from, Vector3f to, Vector3f pivot) {
        Quaternion rotation = rotationBetween(from, to);
        if (rotation == null) return;
        applyRotation(joint, rotation, pivot);
    }

    private static void rotateSubtreeWeighted(Joint joint, Vector3f from, Vector3f to,
                                              Vector3f pivot, float weight) {
        Quaternion target = rotationBetween(from, to);
        if (target == null) return;
        Quaternion rotation = new Quaternion(0.0f, 0.0f, 0.0f, 1.0f);
        rotation.slerp(target, CameraMath.clamp(weight, 0.0f, 1.0f));
        applyRotation(joint, rotation, pivot);
    }

    private static void applyRotation(Joint joint, Quaternion rotation, Vector3f pivot) {
        Matrix4f translateBack = new Matrix4f();
        translateBack.setIdentity();
        translateBack.setTranslation(pivot);
        Matrix4f rotate = new Matrix4f();
        rotation.toRotationMatrix(rotate);
        Matrix4f translateOrigin = new Matrix4f();
        translateOrigin.setIdentity();
        translateOrigin.setTranslation(-pivot.x, -pivot.y, -pivot.z);
        Matrix4f partial = new Matrix4f();
        Matrix4f delta = new Matrix4f();
        translateBack.mult(rotate, partial);
        partial.mult(translateOrigin, delta);
        transformSubtree(joint, delta);
    }

    private static Quaternion rotationBetween(Vector3f from, Vector3f to) {
        if (from == null || to == null || from.lengthSquared() < 0.000001f
                || to.lengthSquared() < 0.000001f) return null;
        Vector3f a = new Vector3f(from).normalizeLocal();
        Vector3f b = new Vector3f(to).normalizeLocal();
        float dot = CameraMath.clamp(a.dot(b), -1.0f, 1.0f);
        if (dot > 0.99999f) return null;
        if (dot < -0.9999f) {
            Vector3f axis = a.cross(Math.abs(a.y) < 0.9f
                    ? Vector3f.UNIT_Y : Vector3f.UNIT_X).normalizeLocal();
            Quaternion opposite = new Quaternion();
            opposite.fromAngleAxis((float) Math.PI, axis);
            return opposite;
        }
        Vector3f cross = a.cross(b);
        Quaternion result = new Quaternion(cross.x, cross.y, cross.z, 1.0f + dot);
        result.normalize();
        return result;
    }

    private static void transformSubtree(Joint joint, Matrix4f delta) {
        Matrix4f transformed = new Matrix4f();
        delta.mult(joint.getBindMatrix(), transformed);
        joint.getBindMatrix().copy(transformed);
        List<Joint> children = joint.getChildList();
        if (children == null) return;
        for (Joint child : children) if (child != null) transformSubtree(child, delta);
    }

    private static Vector3f position(Joint joint) {
        Matrix4f value = joint.getBindMatrix();
        return new Vector3f(value.m03, value.m13, value.m23);
    }

    private static float smootherStep(float value) {
        return value * value * value * (value * (value * 6.0f - 15.0f) + 10.0f);
    }

    private static final class ArmPlan {
        final HolsterItemClass type;
        final HolsterPlacement placement;
        final AttachedCellRenderable attachment;
        final String sourceAnchor;
        final boolean male;
        final Vector3f modelGrip;
        final boolean shield;
        final boolean twoHanded;
        final boolean leftHand;

        private ArmPlan(HolsterItemClass type, HolsterPlacement placement,
                        AttachedCellRenderable attachment, String sourceAnchor,
                        boolean male, Vector3f modelGrip, boolean shield, boolean twoHanded,
                        boolean leftHand) {
            this.type = type;
            this.placement = placement;
            this.attachment = attachment;
            this.sourceAnchor = sourceAnchor;
            this.male = male;
            this.modelGrip = modelGrip;
            this.shield = shield;
            this.twoHanded = twoHanded;
            this.leftHand = leftHand;
        }

        HolsterPlacement.GripPose animationGripPose(ColladaModelData data) {
            return placement.animationGripPose(data, attachment, sourceAnchor,
                    male, modelGrip);
        }
    }
}
