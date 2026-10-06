package org.wuthirdpersonview.client;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.wurmonline.client.WurmEventHandler;
import com.wurmonline.client.collision.advanced.CollisionManagerAdvanced;
import com.wurmonline.client.collision.advanced.CollisionView;
import com.wurmonline.client.collision.advanced.Shape;
import com.wurmonline.client.collision.advanced.Transform;
import com.wurmonline.client.game.CaveDataBuffer;
import com.wurmonline.client.game.PlayerObj;
import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.WorldRender;
import com.wurmonline.client.renderer.PlayerBodyRenderable;
import com.wurmonline.client.renderer.Matrix;
import com.wurmonline.client.renderer.cell.AttachedCellRenderable;
import com.wurmonline.client.renderer.model.collada.ColladaAnimationJob;
import com.wurmonline.client.renderer.model.collada.ColladaModelData;
import com.wurmonline.client.renderer.model.collada.animation.Joint;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;
import javax.vecmath.Vector3f;
import javassist.ClassPool;
import javassist.CtClass;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.junit.Test;

public class PinnedClientContractTest {
    @Test
    public void modLoaderWillInvokeCameraLifecycleMethods() {
        assertTrue(PreInitable.class.isAssignableFrom(WUThirdPersonViewMod.class));
        assertTrue(Initable.class.isAssignableFrom(WUThirdPersonViewMod.class));
    }

    @Test
    public void startupPhasesLeaveRuntimeReflectionUntilAllModsFinishHooking() throws Exception {
        WUThirdPersonViewMod mod = new WUThirdPersonViewMod();
        mod.configure(new Properties());
        mod.preInit();
        mod.init();

        Field cameraFields = WUThirdPersonViewMod.class.getDeclaredField("cameraFields");
        cameraFields.setAccessible(true);
        assertNull(cameraFields.get(mod));

        Field collision = WUThirdPersonViewMod.class.getDeclaredField("collision");
        collision.setAccessible(true);
        assertNull(collision.get(mod));

        Field headAnimator = WUThirdPersonViewMod.class.getDeclaredField("headAnimator");
        headAnimator.setAccessible(true);
        assertNull(headAnimator.get(mod));

        Field freeLookIndicator = WUThirdPersonViewMod.class
                .getDeclaredField("freeLookIndicator");
        freeLookIndicator.setAccessible(true);
        assertNull(freeLookIndicator.get(mod));

        Field runtimeReady = WUThirdPersonViewMod.class.getDeclaredField("runtimeReady");
        runtimeReady.setAccessible(true);
        assertFalse(runtimeReady.getBoolean(mod));
    }

    @Test
    public void pinnedClientStillExposesRequiredCameraAndCollisionContract() throws Exception {
        for (String fieldName : new String[] {
                "cameraX", "cameraY", "cameraZ", "cameraRotX", "cameraRotY"
        }) assertNotNull(WorldRender.class.getDeclaredField(fieldName));
        assertNotNull(WorldRender.class.getDeclaredMethod("calculateCamera"));
        assertNotNull(WorldRender.class.getDeclaredMethod("toggleFreeCamera"));
        assertNotNull(WorldRender.class.getDeclaredMethod("isFreeCamera"));
        assertNotNull(com.wurmonline.client.renderer.gui.HeadsUpDisplay.class
                .getMethod("getWidth"));
        assertNotNull(com.wurmonline.client.renderer.gui.HeadsUpDisplay.class
                .getMethod("getHeight"));
        assertNotNull(com.wurmonline.client.renderer.gui.HeadsUpDisplay.class
                .getMethod("showBml", short.class, String.class, int.class,
                        int.class, float.class, float.class, boolean.class,
                        boolean.class, float.class, float.class, float.class,
                        String.class));
        assertNotNull(com.wurmonline.client.renderer.gui.HeadsUpDisplay.class
                .getMethod("getBmlWindow", short.class));
        assertNotNull(com.wurmonline.client.renderer.gui.WurmComponent.class
                .getMethod("setPosition", int.class, int.class));
        assertNotNull(com.wurmonline.client.comm.SimpleServerConnectionClass.class
                .getMethod("sendBmlResponse", Map.class, String.class));
        ClassPool pool = HookManager.getInstance().getClassPool();
        assertNotNull(pool.get("com.wurmonline.client.renderer.gui.HeadsUpDisplay")
                .getDeclaredField("eventWindow"));
        assertNotNull(pool.get("com.wurmonline.client.renderer.gui.WurmComponent")
                .getDeclaredField("y"));
        assertNotNull(pool.get("com.wurmonline.client.renderer.gui.WurmComponent")
                .getDeclaredField("x"));
        assertNotNull(com.wurmonline.client.renderer.backend.Primitive.class
                .getMethod("setupLineIcosahedron",
                        com.wurmonline.client.renderer.backend.Primitive.class));
        assertNotNull(com.wurmonline.client.renderer.backend.Primitive.class
                .getMethod("setupLineBox",
                        com.wurmonline.client.renderer.backend.Primitive.class));
        assertNotNull(com.wurmonline.client.renderer.backend.Queue.class
                .getMethod("reservePrimitive"));
        assertNotNull(com.wurmonline.client.renderer.WorldRender.class
                .getDeclaredField("queueDebug"));
        assertNotNull(pool.get("com.wurmonline.client.renderer.gui.Renderer")
                .getDeclaredMethod("execute", new CtClass[] {
                        pool.get("java.lang.Object")
                }));
        assertNotNull(PlayerObj.class.getDeclaredMethod("turn", int.class, int.class,
                boolean.class));
        assertNotNull(PlayerObj.class.getMethod("hasTarget"));
        assertNotNull(PlayerObj.class.getMethod("getPlayerBody"));
        assertNotNull(PlayerObj.class.getDeclaredField("mouseSensitivity"));
        assertNotNull(WurmEventHandler.class.getDeclaredMethod("mouseWheeled",
                int.class, int.class, int.class));
        Field view = CollisionManagerAdvanced.class.getDeclaredField("collisionView");
        assertNotNull(view);
        assertNotNull(CollisionView.class.getDeclaredMethod("calcImpact", Shape.class,
                Transform.class, Vector3f.class,
                com.wurmonline.client.collision.advanced.CollisionResult.class,
                com.wurmonline.client.collision.advanced.ContactInfo.class,
                float.class, boolean.class));
        assertNotNull(World.class.getDeclaredMethod("getCaveBuffer"));
        assertNotNull(World.class.getDeclaredMethod("getPlayerRotX"));
        assertNotNull(World.class.getDeclaredMethod("getPlayerRotY"));
        assertNotNull(ColladaModelData.class.getDeclaredMethod("calculateSkins",
                com.wurmonline.client.renderer.model.collada.importer.TriangleMesh.LODLevel.class));
        assertNotNull(PlayerBodyRenderable.class.getDeclaredMethod("renderEquipment",
                com.wurmonline.client.renderer.backend.Queue.class, Matrix.class,
                com.wurmonline.client.renderer.Color.class,
                com.wurmonline.client.renderer.ModelRenderMode.class));
        assertNotNull(PlayerBodyRenderable.class.getDeclaredField("playerModel"));
        assertNotNull(PlayerBodyRenderable.class.getDeclaredField("renderState"));
        assertNotNull(PlayerBodyRenderable.class.getDeclaredField("equipment"));
        assertNotNull(PlayerBodyRenderable.class.getMethod("getEquipment", int.class));
        assertNotNull(PlayerBodyRenderable.class.getMethod("getEquippedTool"));
        assertNotNull(AttachedCellRenderable.class.getMethod("setNullName", String.class));
        assertNotNull(AttachedCellRenderable.class.getMethod("getModelMatrix"));
        assertNotNull(Matrix.class.getMethod("_m30"));
        assertNotNull(ColladaModelData.class.getMethod("getNullTransformation",
                String.class,
                com.wurmonline.client.renderer.model.collada.math.Matrix4f.class));
        assertNotNull(Joint.class.getDeclaredMethod("getBindMatrix"));
        assertTrue(hasMethodNamed(ColladaAnimationJob.class, "applyAnimations"));
        assertNotNull(CaveDataBuffer.class.getDeclaredMethod(
                "shouldHaveNorthWall", int.class, int.class));
        assertNotNull(CaveDataBuffer.class.getDeclaredMethod(
                "shouldHaveWestWall", int.class, int.class));
        assertNotNull(CaveDataBuffer.class.getDeclaredMethod(
                "getInterpolatedFloor", float.class, float.class));
        assertNotNull(com.wurmonline.client.game.NearTerrainDataBuffer.class
                .getMethod("getInterpolatedHeight", float.class, float.class));
    }

    @Test
    public void javassistHookBodiesCompileAgainstPinnedClient() throws Exception {
        ClientHooks.install();
    }

    private static boolean hasMethodNamed(Class<?> type, String name) {
        for (java.lang.reflect.Method method : type.getDeclaredMethods())
            if (name.equals(method.getName())) return true;
        return false;
    }
}
