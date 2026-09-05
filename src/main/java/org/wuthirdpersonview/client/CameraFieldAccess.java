package org.wuthirdpersonview.client;

import com.wurmonline.client.renderer.WorldRender;
import java.lang.reflect.Field;
import org.wuthirdpersonview.core.CameraPose;

final class CameraFieldAccess {
    private final Field x;
    private final Field y;
    private final Field z;
    private final Field yaw;
    private final Field pitch;

    CameraFieldAccess() throws ReflectiveOperationException {
        x = field("cameraX");
        y = field("cameraY");
        z = field("cameraZ");
        yaw = field("cameraRotX");
        pitch = field("cameraRotY");
    }

    private static Field field(String name) throws ReflectiveOperationException {
        Field field = WorldRender.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    void apply(WorldRender render, CameraPose pose) throws IllegalAccessException {
        x.setFloat(render, pose.x);
        y.setFloat(render, pose.y);
        z.setFloat(render, pose.z);
        yaw.setFloat(render, pose.yawRadians);
        pitch.setFloat(render, pose.pitchRadians);
    }

    void raiseTo(WorldRender render, float minimumHeight) throws IllegalAccessException {
        float currentHeight = y.getFloat(render);
        float clampedHeight = CameraCollisionProbe.clampHeight(currentHeight, minimumHeight);
        if (clampedHeight != currentHeight) y.setFloat(render, clampedHeight);
    }
}
