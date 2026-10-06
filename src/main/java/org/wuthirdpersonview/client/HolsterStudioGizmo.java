package org.wuthirdpersonview.client;

import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.Matrix;
import com.wurmonline.client.renderer.WorldRender;
import com.wurmonline.client.renderer.backend.Primitive;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.cell.CellRenderable;
import java.lang.reflect.Field;

/** Always-visible mount-point marker and anchor-local XYZ axes for Holster Studio. */
final class HolsterStudioGizmo {
    static final float AXIS_HALF_LENGTH = 0.42f;
    static final float AXIS_HALF_WIDTH = 0.009f;
    static final float MARKER_RADIUS = 0.075f;
    private static final Field DEBUG_QUEUE_FIELD = debugQueueField();

    private HolsterStudioGizmo() { }

    static void render(Queue fallbackQueue, Matrix anchorMatrix,
                       HolsterPlacement placement) {
        if (fallbackQueue == null || anchorMatrix == null || placement == null) return;
        Queue queue = overlayQueue(fallbackQueue);
        Matrix pivot = pivotMatrix(anchorMatrix, placement.x, placement.y, placement.z);

        // Player equipment is submitted to the deferred geometry queue. Debug lines
        // must instead use Wurm's forward debug queue to remain coloured and visible.
        axis(queue, pivot, AXIS_HALF_LENGTH, AXIS_HALF_WIDTH, AXIS_HALF_WIDTH,
                1.0f, 0.16f, 0.12f);
        axis(queue, pivot, AXIS_HALF_WIDTH, AXIS_HALF_LENGTH, AXIS_HALF_WIDTH,
                0.18f, 1.0f, 0.24f);
        axis(queue, pivot, AXIS_HALF_WIDTH, AXIS_HALF_WIDTH, AXIS_HALF_LENGTH,
                0.18f, 0.52f, 1.0f);

        Primitive marker = queue.reservePrimitive();
        Primitive.setupLineIcosahedron(marker);
        marker.setColor(1.0f, 0.86f, 0.18f, 1.0f);
        makeAlwaysVisible(marker);
        queue.queue(marker, scaled(pivot, MARKER_RADIUS, MARKER_RADIUS, MARKER_RADIUS));
    }

    static Queue overlayQueue(Queue fallback) {
        if (DEBUG_QUEUE_FIELD == null) return fallback;
        try {
            World world = CellRenderable.getWorld();
            WorldRender renderer = world == null ? null : world.getWorldRenderer();
            Object queue = renderer == null ? null : DEBUG_QUEUE_FIELD.get(renderer);
            return queue instanceof Queue ? (Queue) queue : fallback;
        } catch (Throwable unavailable) {
            return fallback;
        }
    }

    static Matrix pivotMatrix(Matrix anchorMatrix, float x, float y, float z) {
        Matrix local = new Matrix().fromTranslationAndNonUniformScale(
                x, y, z, 1.0f, 1.0f, 1.0f);
        Matrix result = new Matrix();
        Matrix.mult(anchorMatrix, local, result);
        return result;
    }

    private static void axis(Queue queue, Matrix pivot,
                             float scaleX, float scaleY, float scaleZ,
                             float red, float green, float blue) {
        Primitive axis = queue.reservePrimitive();
        Primitive.setupLineBox(axis);
        axis.setColor(red, green, blue, 1.0f);
        makeAlwaysVisible(axis);
        queue.queue(axis, scaled(pivot, scaleX, scaleY, scaleZ));
    }

    private static Matrix scaled(Matrix pivot, float x, float y, float z) {
        Matrix scale = new Matrix().fromTranslationAndNonUniformScale(
                0.0f, 0.0f, 0.0f, x, y, z);
        Matrix result = new Matrix();
        Matrix.mult(pivot, scale, result);
        return result;
    }

    private static void makeAlwaysVisible(Primitive primitive) {
        primitive.depthtest = Primitive.TestFunc.ALWAYS;
        primitive.depthwrite = false;
        primitive.nolight = true;
        primitive.nofog = true;
    }

    private static Field debugQueueField() {
        try {
            Field field = WorldRender.class.getDeclaredField("queueDebug");
            field.setAccessible(true);
            return field;
        } catch (Throwable unavailable) {
            return null;
        }
    }
}
