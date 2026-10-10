package org.wuthirdpersonview.client;

import javassist.CtClass;
import javassist.CtBehavior;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.ClassClassPath;
import javassist.bytecode.CodeAttribute;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.Opcode;
import javassist.ClassPool;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

final class ClientHooks {
    private ClientHooks() { }

    static void exposeModClasses() {
        HookManager.getInstance().getClassPool().appendClassPath(
                new ClassClassPath(WUThirdPersonViewMod.class));
    }

    static void install() throws Exception {
        exposeModClasses();
        ClassPool pool = HookManager.getInstance().getClassPool();
        CtClass worldRender = pool.get("com.wurmonline.client.renderer.WorldRender");
        worldRender.getDeclaredMethod("calculateCamera").insertBefore(
                "{ org.wuthirdpersonview.client.WUThirdPersonViewMod.beforeCalculateCamera(this); }");
        worldRender.getDeclaredMethod("calculateCamera").insertAfter(
                "{ org.wuthirdpersonview.client.WUThirdPersonViewMod.afterCalculateCamera(this); }");
        worldRender.getDeclaredMethod("renderPlayerModel").instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall call) throws javassist.CannotCompileException {
                if ("com.wurmonline.client.game.World".equals(call.getClassName())
                        && "getPlayerRotX".equals(call.getMethodName())) {
                    call.replace("{ $_ = org.wuthirdpersonview.client.WUThirdPersonViewMod"
                            + ".renderedPlayerBodyYaw($proceed($$)); }");
                }
            }
        });

        CtClass player = pool.get("com.wurmonline.client.game.PlayerObj");
        CtMethod turn = player.getDeclaredMethod("turn", new CtClass[] {
                CtClass.intType, CtClass.intType, CtClass.booleanType
        });
        turn.insertBefore("{ if (org.wuthirdpersonview.client.WUThirdPersonViewMod"
                + ".interceptTurn(this, $1, $2, $3)) return; }");

        CtClass events = pool.get("com.wurmonline.client.WurmEventHandler");
        events.getDeclaredMethod("mouseWheeled", new CtClass[] {
                CtClass.intType, CtClass.intType, CtClass.intType
        }).insertBefore("{ if (org.wuthirdpersonview.client.WUThirdPersonViewMod"
                + ".handleMouseWheel(this.world, $1, $2, $3)) return; }");
        events.getDeclaredMethod("keyPressed", new CtClass[] {
                CtClass.intType, CtClass.charType
        }).insertBefore("{ org.wuthirdpersonview.client.WUThirdPersonViewMod"
                + ".onKeyEvent($1, true); }");
        events.getDeclaredMethod("keyReleased", new CtClass[] {
                CtClass.intType, CtClass.charType
        }).insertBefore("{ org.wuthirdpersonview.client.WUThirdPersonViewMod"
                + ".onKeyEvent($1, false); }");
        events.getDeclaredMethod("releaseAll").insertBefore(
                "{ org.wuthirdpersonview.client.WUThirdPersonViewMod.releaseAllInput(); }");

        CtClass guiRenderer = pool.get("com.wurmonline.client.renderer.gui.Renderer");
        guiRenderer.getDeclaredMethod("execute").insertAfter(
                "{ org.wuthirdpersonview.client.WUThirdPersonViewMod"
                        + ".renderFreeLookIndicator((com.wurmonline.client.renderer.backend.Queue)$1); }");

        CtClass animation = pool.get(
                "com.wurmonline.client.renderer.model.collada.animation.ColladaAnimation");
        animation.getDeclaredMethod("getAnimateCamera").insertBefore(
                "{ if (org.wuthirdpersonview.client.WUThirdPersonViewMod"
                        + ".suppressAnimationCamera()) return false; }");

        CtClass animationJob = pool.get(
                "com.wurmonline.client.renderer.model.collada.ColladaAnimationJob");
        animationJob.getDeclaredMethod("applyAnimations").instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall call) throws javassist.CannotCompileException {
                if ("com.wurmonline.client.renderer.model.collada.ColladaModelData"
                        .equals(call.getClassName())
                        && "calculateSkins".equals(call.getMethodName())) {
                    call.replace("{ org.wuthirdpersonview.client.WUThirdPersonViewMod"
                            + ".beforePlayerSkinning($0); $proceed($$); }");
                }
            }
        });

        installCameraCentredRendering(pool, worldRender);
    }

    private static void installCameraCentredRendering(ClassPool pool, CtClass worldRender)
            throws Exception {
        instrumentWorldPosition(worldRender.getDeclaredMethod("tick"));

        CtClass decorations = pool.get(
                "com.wurmonline.client.renderer.terrain.decorator.DecorationRenderer");
        expandDecorationGridToEightTiles(decorations);
        for (CtMethod method : decorations.getDeclaredMethods())
            instrumentWorldPosition(method);

        CtClass decorationChunk = pool.get(
                "com.wurmonline.client.renderer.terrain.decorator.DecorationChunk");
        instrumentWorldPosition(decorationChunk.getDeclaredMethod("updateSprites"));

        CtClass terrainLod = pool.get("com.wurmonline.client.renderer.terrain.TerrainLod");
        instrumentWorldPosition(terrainLod.getDeclaredMethod("tesselateTerrain"));

        CtClass cells = pool.get("com.wurmonline.client.renderer.cell.CellRenderer");
        instrumentWorldPosition(cells.getDeclaredMethod("updatePos"));
        instrumentWorldPosition(cells.getDeclaredMethod("fastRenderableDist"));

        CtClass cell = pool.get("com.wurmonline.client.renderer.cell.Cell");
        for (CtMethod method : cell.getDeclaredMethods())
            instrumentWorldPosition(method);

        CtClass renderable = pool.get("com.wurmonline.client.renderer.cell.CellRenderable");
        instrumentWorldPosition(renderable.getDeclaredMethod("getSquaredLengthFromPlayer"));
    }

    /** Expands Wurm's compiled 15x15 (radius 7) decoration grid to 17x17. */
    private static void expandDecorationGridToEightTiles(CtClass decorations)
            throws Exception {
        for (CtConstructor constructor : decorations.getDeclaredConstructors())
            replaceDecorationGridConstants(constructor);
        for (CtMethod method : decorations.getDeclaredMethods()) {
            String name = method.getName();
            if ("shiftChunks".equals(name) || "gameTick".equals(name)
                    || "execute".equals(name))
                replaceDecorationGridConstants(method);
        }
    }

    private static void replaceDecorationGridConstants(CtBehavior behavior)
            throws Exception {
        CodeAttribute code = behavior.getMethodInfo().getCodeAttribute();
        if (code == null) return;
        CodeIterator instructions = code.iterator();
        while (instructions.hasNext()) {
            int index = instructions.next();
            if (instructions.byteAt(index) != Opcode.BIPUSH) continue;
            int value = instructions.byteAt(index + 1);
            if (value == 15) instructions.writeByte(17, index + 1);
            else if (value == 7) instructions.writeByte(8, index + 1);
        }
    }

    private static void instrumentWorldPosition(CtMethod method) throws Exception {
        method.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall call) throws javassist.CannotCompileException {
                if (!"com.wurmonline.client.game.World".equals(call.getClassName())) return;
                String name = call.getMethodName();
                if ("getPlayerPosX".equals(name)) {
                    call.replace("{ $_ = org.wuthirdpersonview.client.RenderFocus"
                            + ".x($0, $proceed($$)); }");
                } else if ("getPlayerPosY".equals(name)) {
                    call.replace("{ $_ = org.wuthirdpersonview.client.RenderFocus"
                            + ".y($0, $proceed($$)); }");
                } else if ("getPlayerPosH".equals(name)) {
                    call.replace("{ $_ = org.wuthirdpersonview.client.RenderFocus"
                            + ".height($0, $proceed($$)); }");
                } else if ("getPlayerCurrentTileX".equals(name)) {
                    call.replace("{ $_ = org.wuthirdpersonview.client.RenderFocus"
                            + ".tileX($0, $proceed($$)); }");
                } else if ("getPlayerCurrentTileY".equals(name)) {
                    call.replace("{ $_ = org.wuthirdpersonview.client.RenderFocus"
                            + ".tileY($0, $proceed($$)); }");
                }
            }
        });
    }
}
