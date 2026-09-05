package org.wuthirdpersonview.client;

import com.wurmonline.client.game.PlayerObj;
import com.wurmonline.client.renderer.model.AbstractModelData;
import com.wurmonline.client.renderer.model.ModelResourceWrapper;
import com.wurmonline.client.renderer.model.collada.ColladaModelData;
import com.wurmonline.client.renderer.model.collada.animation.Joint;
import com.wurmonline.client.renderer.model.collada.importer.ColladaModel;
import com.wurmonline.client.renderer.model.collada.math.Matrix4f;
import com.wurmonline.client.renderer.model.collada.math.Quaternion;
import java.util.List;
import org.wuthirdpersonview.core.CameraConfig;
import org.wuthirdpersonview.core.CameraMath;

/**
 * Applies the local player's view yaw and pitch to the visible head model.
 * Animation jobs call {@link #applyBeforeSkinning(ColladaModelData)} after
 * Wurm has calculated the normal skeleton pose and immediately before it
 * skins the mesh, so the modification neither accumulates nor replaces the
 * character's normal animations.
 */
final class PlayerHeadAnimator {
    private static final String HEAD_JOINT = "Head";
    private static final float MOVING_SPEED_EPSILON = 0.01f;

    private final CameraConfig config;
    private volatile ColladaModelData targetModelData;
    private volatile float headYawDegrees;
    private volatile float headPitchDegrees;
    private volatile float visualBodyYawDegrees;
    private volatile boolean active;
    private volatile boolean headJointAvailable;
    private ColladaModelData trackedModelData;
    private boolean bodyYawInitialized;

    PlayerHeadAnimator(CameraConfig config) {
        this.config = config;
    }

    void update(PlayerObj player, float viewYawDegrees, float viewPitchDegrees,
                float deltaSeconds) {
        if (!config.animateHead || player == null) {
            deactivate();
            return;
        }
        ModelResourceWrapper wrapper = player.getPlayerBody().getModelWrapper();
        AbstractModelData modelData = wrapper == null ? null : wrapper.getModelData();
        if (!(modelData instanceof ColladaModelData)) {
            deactivate();
            return;
        }
        ColladaModelData collada = (ColladaModelData) modelData;
        if (trackedModelData != collada) {
            trackedModelData = collada;
            bodyYawInitialized = false;
            headJointAvailable = false;
        }
        if (!bodyYawInitialized) {
            visualBodyYawDegrees = wrap360(viewYawDegrees);
            bodyYawInitialized = true;
        }

        float delta = shortestAngle(viewYawDegrees - visualBodyYawDegrees);
        boolean moving = Math.abs(player.getSpeed()) > MOVING_SPEED_EPSILON;
        if (moving) {
            float dt = CameraMath.clamp(deltaSeconds, 0.0f, 0.1f);
            visualBodyYawDegrees = wrap360(visualBodyYawDegrees
                    + angularApproachDelta(delta, dt, config.bodyFollowSeconds));
            delta = shortestAngle(viewYawDegrees - visualBodyYawDegrees);
        }
        if (Math.abs(delta) > config.maxHeadYawDegrees) {
            visualBodyYawDegrees = wrap360(viewYawDegrees
                    - Math.signum(delta) * config.maxHeadYawDegrees);
            delta = shortestAngle(viewYawDegrees - visualBodyYawDegrees);
        }

        headYawDegrees = CameraMath.clamp(delta,
                -config.maxHeadYawDegrees, config.maxHeadYawDegrees);
        headPitchDegrees = CameraMath.clamp(viewPitchDegrees,
                -config.maxHeadPitchDegrees, config.maxHeadPitchDegrees);
        targetModelData = collada;
        active = true;
    }

    void deactivate() {
        active = false;
        targetModelData = null;
        headYawDegrees = 0.0f;
        headPitchDegrees = 0.0f;
        bodyYawInitialized = false;
    }

    float renderedBodyYaw(float nativeYawDegrees) {
        return active && headJointAvailable ? visualBodyYawDegrees : nativeYawDegrees;
    }

    void applyBeforeSkinning(ColladaModelData modelData) {
        if (!active || modelData == null || modelData != targetModelData) return;
        ColladaModel model = modelData.getModel();
        Joint head = model == null ? null : model.getJoint(HEAD_JOINT);
        if (head == null) {
            headJointAvailable = false;
            return;
        }
        headJointAvailable = true;
        float yaw = headYawDegrees;
        float pitch = headPitchDegrees;
        if (Math.abs(yaw) < 0.001f && Math.abs(pitch) < 0.001f) return;

        Quaternion rotation = new Quaternion();
        // The Wurm player model faces local +Z. Increasing native view yaw
        // therefore requires the opposite local Y rotation; pitch has the
        // same sign as Wurm's native camera pitch.
        rotation.fromAngles((float) Math.toRadians(pitch),
                (float) Math.toRadians(-yaw), 0.0f);
        rotateSubtreeAroundHead(head, rotation);
    }

    static void rotateSubtreeAroundHead(Joint head, Quaternion rotation) {
        Matrix4f originalHead = new Matrix4f(head.getBindMatrix());
        Matrix4f rotatedHead = new Matrix4f(originalHead);
        rotatedHead.multLocal(rotation);
        Matrix4f inverseHead = new Matrix4f(originalHead);
        inverseHead.invertLocal();
        Matrix4f delta = new Matrix4f();
        rotatedHead.mult(inverseHead, delta);
        transformSubtree(head, delta);
    }

    private static void transformSubtree(Joint joint, Matrix4f delta) {
        Matrix4f transformed = new Matrix4f();
        delta.mult(joint.getBindMatrix(), transformed);
        joint.getBindMatrix().copy(transformed);
        List<Joint> children = joint.getChildList();
        if (children == null) return;
        for (Joint child : children) {
            if (child != null) transformSubtree(child, delta);
        }
    }

    private static float angularApproachDelta(float delta, float dt, float seconds) {
        if (seconds <= 0.0f) return delta;
        float fraction = 1.0f - (float) Math.exp(-dt / seconds);
        return delta * fraction;
    }

    static float shortestAngle(float value) {
        float wrapped = value % 360.0f;
        if (wrapped > 180.0f) wrapped -= 360.0f;
        if (wrapped < -180.0f) wrapped += 360.0f;
        return wrapped;
    }

    private static float wrap360(float value) {
        float wrapped = value % 360.0f;
        if (wrapped < 0.0f) wrapped += 360.0f;
        return wrapped;
    }
}
