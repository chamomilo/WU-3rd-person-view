package org.wuthirdpersonview.client;

import javassist.CtClass;
import javassist.CtMethod;
import javassist.ClassPool;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;

final class ClientHooks {
    private ClientHooks() { }

    static void install() throws Exception {
        ClassPool pool = HookManager.getInstance().getClassPool();
        CtClass worldRender = pool.get("com.wurmonline.client.renderer.WorldRender");
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

        CtClass playerBody = pool.get("com.wurmonline.client.renderer.PlayerBodyRenderable");
        CtMethod renderEquipment = playerBody.getDeclaredMethod("renderEquipment",
                new CtClass[] {
                        pool.get("com.wurmonline.client.renderer.backend.Queue"),
                        pool.get("com.wurmonline.client.renderer.Matrix"),
                        pool.get("com.wurmonline.client.renderer.Color"),
                        pool.get("com.wurmonline.client.renderer.ModelRenderMode")
                });
        renderEquipment.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall call) throws javassist.CannotCompileException {
                if ("isShowEquipmentInHands".equals(call.getMethodName())) {
                    call.replace("{ $_ = org.wuthirdpersonview.client.HolsterEquipmentRenderer"
                            + ".allowVanillaHandEquipment((com.wurmonline.client.renderer"
                            + ".PlayerBodyRenderable)$0, $proceed($$)); }");
                }
            }
        });
        renderEquipment.insertAfter("{ org.wuthirdpersonview.client.HolsterEquipmentRenderer"
                + ".render(this, $1, $2, $3, $4, this.playerModel, this.renderState,"
                + " this.equipment); }");

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

        CtClass connection = pool.get(
                "com.wurmonline.client.comm.SimpleServerConnectionClass");
        connection.getDeclaredMethod("sendBmlResponse", new CtClass[] {
                pool.get("java.util.Map"), pool.get("java.lang.String")
        }).insertBefore("{ if (org.wuthirdpersonview.client.WUThirdPersonViewMod"
                + ".handleHolsterStudioResponse($1, $2)) return; }");

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
    }
}
