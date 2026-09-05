package org.wuthirdpersonview.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.wurmonline.client.renderer.model.collada.animation.Joint;
import com.wurmonline.client.renderer.model.collada.math.Matrix4f;
import com.wurmonline.client.renderer.model.collada.math.Quaternion;
import java.util.Arrays;
import org.junit.Test;

public class PlayerHeadAnimatorTest {
    @Test
    public void shortestAngleCrossesNorthWithoutAFullTurn() {
        assertEquals(2.0f, PlayerHeadAnimator.shortestAngle(361.0f - 359.0f), 0.001f);
        assertEquals(-2.0f, PlayerHeadAnimator.shortestAngle(359.0f - 1.0f), 0.001f);
    }

    @Test
    public void subtreeRotationKeepsHeadPivotAndMovesItsChildAroundIt() {
        Joint head = jointAt("Head", 1.0f, 2.0f, 3.0f);
        Joint helmet = jointAt("NullHelmet", 1.0f, 3.0f, 4.0f);
        head.setChildList(Arrays.asList(helmet));

        float beforeX = helmet.getBindMatrix().m03;
        float beforeY = helmet.getBindMatrix().m13;
        float beforeZ = helmet.getBindMatrix().m23;
        float beforeDistance = distance(head.getBindMatrix(), helmet.getBindMatrix());
        Quaternion yaw = new Quaternion();
        yaw.fromAngles(0.0f, (float) Math.toRadians(45.0f), 0.0f);

        PlayerHeadAnimator.rotateSubtreeAroundHead(head, yaw);

        assertEquals(1.0f, head.getBindMatrix().m03, 0.001f);
        assertEquals(2.0f, head.getBindMatrix().m13, 0.001f);
        assertEquals(3.0f, head.getBindMatrix().m23, 0.001f);
        assertEquals(beforeDistance,
                distance(head.getBindMatrix(), helmet.getBindMatrix()), 0.001f);
        assertNotEquals(beforeX + beforeY + beforeZ,
                helmet.getBindMatrix().m03 + helmet.getBindMatrix().m13
                        + helmet.getBindMatrix().m23, 0.001f);
    }

    private static Joint jointAt(String name, float x, float y, float z) {
        Joint joint = new Joint(name);
        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        matrix.setTranslation(x, y, z);
        joint.setBindMatrix(matrix);
        return joint;
    }

    private static float distance(Matrix4f a, Matrix4f b) {
        float x = a.m03 - b.m03;
        float y = a.m13 - b.m13;
        float z = a.m23 - b.m23;
        return (float) Math.sqrt(x * x + y * y + z * z);
    }
}
