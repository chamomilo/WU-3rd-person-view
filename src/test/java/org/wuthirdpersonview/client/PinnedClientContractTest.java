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
import com.wurmonline.client.renderer.model.collada.ColladaAnimationJob;
import com.wurmonline.client.renderer.model.collada.ColladaModelData;
import com.wurmonline.client.renderer.model.collada.animation.Joint;
import java.lang.reflect.Field;
import java.util.Properties;
import javax.vecmath.Vector3f;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.Opcode;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
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
        ClassPool pool = HookManager.getInstance().getClassPool();
        assertNotNull(pool.get("com.wurmonline.client.renderer.gui.HeadsUpDisplay")
                .getDeclaredField("eventWindow"));
        assertNotNull(pool.get("com.wurmonline.client.renderer.gui.WurmComponent")
                .getDeclaredField("y"));
        assertNotNull(pool.get("com.wurmonline.client.renderer.gui.WurmComponent")
                .getDeclaredField("x"));
        assertNotNull(pool.get("com.wurmonline.client.renderer.gui.Renderer")
                .getDeclaredMethod("execute", new CtClass[] {
                        pool.get("java.lang.Object")
                }));
        assertNotNull(PlayerObj.class.getDeclaredMethod("turn", int.class, int.class,
                boolean.class));
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
        assertNotNull(CaveDataBuffer.class.getDeclaredMethod(
                "getInterpolatedCeiling", float.class, float.class));
        assertNotNull(CaveDataBuffer.class.getDeclaredMethod(
                "getTileType", int.class, int.class));
        assertNotNull(com.wurmonline.client.game.NearTerrainDataBuffer.class
                .getMethod("getInterpolatedHeight", float.class, float.class));
        assertNotNull(World.class.getDeclaredMethod("getPlayerCurrentTileX"));
        assertNotNull(World.class.getDeclaredMethod("getPlayerCurrentTileY"));
        assertNotNull(World.class.getDeclaredMethod("getPlayerPosX"));
        assertNotNull(World.class.getDeclaredMethod("getPlayerPosY"));
        assertNotNull(pool.get("com.wurmonline.client.renderer.terrain.decorator.DecorationRenderer")
                .getDeclaredMethod("gameTick"));
        assertNotNull(pool.get("com.wurmonline.client.renderer.terrain.decorator.DecorationChunk")
                .getDeclaredMethod("updateSprites"));
        assertNotNull(pool.get("com.wurmonline.client.renderer.terrain.TerrainLod")
                .getDeclaredMethod("tesselateTerrain"));
        assertNotNull(pool.get("com.wurmonline.client.renderer.cell.CellRenderer")
                .getDeclaredMethod("updatePos"));
        assertNotNull(CaveDataBuffer.class.getDeclaredMethod("tileStrip", short.class,
                short.class, short.class, short.class, int[][].class, short[][].class, byte[][].class));
    }

    @Test
    public void javassistHookBodiesCompileAgainstPinnedClient() throws Exception {
        ClientHooks.install();

        CtClass worldRender = HookManager.getInstance().getClassPool().get(
                "com.wurmonline.client.renderer.WorldRender");
        CtMethod visibility = worldRender.getDeclaredMethod("computeLayerVisibility");
        assertTrue("main, reflection and pick visibility must select the camera layer",
                countRenderFocusCalls(visibility, "layer") > 0);
        assertTrue("entrance visibility must use the camera tile",
                countRenderFocusCalls(visibility, "onCaveEntrance") >= 3);
        assertTrue("world lighting must use the camera layer",
                countRenderFocusCalls(worldRender.getDeclaredMethod("renderWorld"), "layer") > 0);
        assertTrue("player model lighting must retain the real player layer",
                countRenderFocusCalls(worldRender.getDeclaredMethod("renderPlayer"), "layer") == 0);

        CtClass caves = HookManager.getInstance().getClassPool().get(
                "com.wurmonline.client.renderer.cave.CaveRender");
        for (String name : new String[] {"prepareRender", "render", "renderSkyOcclusion"}) {
            CtMethod method = caves.getDeclaredMethod(name);
            assertTrue("cave grid and boundary must follow camera X in " + name,
                    countRenderFocusCalls(method, "tileX") > 0);
            assertTrue("cave grid and boundary must follow camera Y in " + name,
                    countRenderFocusCalls(method, "tileY") > 0);
        }
        CtClass caveTile = HookManager.getInstance().getClassPool().get(
                "com.wurmonline.client.renderer.cave.CaveTileData");
        assertTrue("cave face sidedness must follow the camera layer",
                countRenderFocusCalls(caveTile.getDeclaredMethod("render"), "layer") > 0);
        assertTrue("camera views must prepare the target cave before portal checks",
                countRenderFocusCalls(worldRender.getDeclaredMethod("prepareRender"),
                        "cameraOwnsView") >= 2);
        CtClass caveBuffer = HookManager.getInstance().getClassPool().get(
                "com.wurmonline.client.game.CaveDataBuffer");
        assertTrue(countCalls(caveBuffer.getDeclaredMethod("tileStrip"),
                "org.wuthirdpersonview.core.CaveDataCoverage", "beforeStrip") > 0);
        assertTrue(countCalls(caveBuffer.getDeclaredMethod("tileStrip"),
                "org.wuthirdpersonview.core.CaveDataCoverage", "afterStrip") > 0);
        assertTrue(countCalls(caveBuffer.getDeclaredMethod("clear"),
                "org.wuthirdpersonview.core.CaveDataCoverage", "clear") > 0);

        CtClass decorations = HookManager.getInstance().getClassPool().get(
                "com.wurmonline.client.renderer.terrain.decorator.DecorationRenderer");
        int oldWidth = 0;
        int newWidth = 0;
        int oldRadius = 0;
        int newRadius = 0;
        for (CtConstructor constructor : decorations.getDeclaredConstructors()) {
            oldWidth += countBipush(constructor, 15);
            newWidth += countBipush(constructor, 17);
            oldRadius += countBipush(constructor, 7);
            newRadius += countBipush(constructor, 8);
        }
        for (CtMethod method : decorations.getDeclaredMethods()) {
            String name = method.getName();
            if (!"shiftChunks".equals(name) && !"gameTick".equals(name)
                    && !"execute".equals(name)) continue;
            oldWidth += countBipush(method, 15);
            newWidth += countBipush(method, 17);
            oldRadius += countBipush(method, 7);
            newRadius += countBipush(method, 8);
        }
        assertTrue("expanded decoration width must be present", newWidth > 0);
        assertTrue("expanded decoration radius must be present", newRadius > 0);
        assertTrue("compiled 15-tile decoration width must be gone", oldWidth == 0);
        assertTrue("compiled seven-tile decoration radius must be gone", oldRadius == 0);
    }

    @Test
    public void standaloneJarCanExposeItsClassesToJavassist() throws Exception {
        ClassPool isolated = new ClassPool(false);
        isolated.appendClassPath(System.getProperty("thirdPersonView.modJar"));
        assertNotNull(isolated.get("org.wuthirdpersonview.client.WUThirdPersonViewMod"));
        assertNotNull(isolated.get("org.wuthirdpersonview.client.RenderFocus"));
        assertNotNull(isolated.get("org.chamomilo.wurm.update.SharedUpdateHooks"));
    }

    private static int countBipush(CtBehavior behavior, int expected) throws Exception {
        CodeAttribute code = behavior.getMethodInfo().getCodeAttribute();
        if (code == null) return 0;
        int count = 0;
        CodeIterator instructions = code.iterator();
        while (instructions.hasNext()) {
            int index = instructions.next();
            if (instructions.byteAt(index) == Opcode.BIPUSH
                    && instructions.byteAt(index + 1) == expected) count++;
        }
        return count;
    }

    private static int countRenderFocusCalls(CtMethod method, String name) throws Exception {
        return countCalls(method, "org.wuthirdpersonview.client.RenderFocus", name);
    }

    private static int countCalls(CtMethod method, final String owner, final String name) throws Exception {
        final int[] count = {0};
        method.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall call) {
                if (owner.equals(call.getClassName())
                        && name.equals(call.getMethodName())) count[0]++;
            }
        });
        return count[0];
    }

    private static boolean hasMethodNamed(Class<?> type, String name) {
        for (java.lang.reflect.Method method : type.getDeclaredMethods())
            if (name.equals(method.getName())) return true;
        return false;
    }
}
