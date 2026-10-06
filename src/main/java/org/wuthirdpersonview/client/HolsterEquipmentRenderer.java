package org.wuthirdpersonview.client;

import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.Color;
import com.wurmonline.client.renderer.Matrix;
import com.wurmonline.client.renderer.ModelRenderMode;
import com.wurmonline.client.renderer.PlayerBodyRenderable;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.backend.RenderState;
import com.wurmonline.client.renderer.cell.ArmorMeshData;
import com.wurmonline.client.renderer.cell.AttachedCellRenderable;
import com.wurmonline.client.renderer.cell.CellRenderable;
import com.wurmonline.client.renderer.cell.PlayerArmorBuilder;
import com.wurmonline.client.renderer.model.AbstractModelData;
import com.wurmonline.client.renderer.model.ModelDataBounds;
import com.wurmonline.client.renderer.model.ModelResourceWrapper;
import com.wurmonline.client.renderer.model.collada.ColladaModelData;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.wuthirdpersonview.core.HolsterItemClass;
import org.wuthirdpersonview.core.HolsterItemClassifier;
import org.wuthirdpersonview.core.HolsterSnapshot;

/** Render-time replacement for the local player's three combat equipment slots. */
public final class HolsterEquipmentRenderer {
    private static final Logger LOG = Logger.getLogger(HolsterEquipmentRenderer.class.getName());
    private static final Set<String> LOGGED_FAILURES = Collections.synchronizedSet(
            new HashSet<String>());
    private static volatile boolean disabled;

    private HolsterEquipmentRenderer() { }

    public static boolean allowVanillaHandEquipment(PlayerBodyRenderable body,
                                                    boolean nativeDecision) {
        try {
            if (disabled) return nativeDecision;
            if (!isLocalPlayer(body)) return nativeDecision;
            HolsterSnapshot state = WUThirdPersonViewMod.holsterSnapshot();
            if (state == null || !state.replacesVanillaEquipment()) return nativeDecision;
            // Vanilla already replaces weapon and shield locals with this transient tool.
            return nativeDecision && body != null && body.getEquippedTool() != null;
        } catch (Throwable failedOpen) {
            return nativeDecision;
        }
    }

    public static void render(PlayerBodyRenderable body, Queue queue, Matrix parentMatrix,
                              Color color, ModelRenderMode mode,
                              ModelResourceWrapper parentModel, RenderState renderState,
                              AttachedCellRenderable[] equipment) {
        if (disabled) return;
        if (!isLocalPlayer(body)) return;
        HolsterSnapshot state = WUThirdPersonViewMod.holsterSnapshot();
        if (state == null || !state.replacesVanillaEquipment() || body == null
                || equipment == null || parentModel == null || !parentModel.isLoaded()) return;
        try {
            renderSlot(body, queue, parentMatrix, color, mode, parentModel, renderState,
                    equipment, HolsterPlacement.SHIELD_SLOT, state.shieldAmount);
            renderSlot(body, queue, parentMatrix, color, mode, parentModel, renderState,
                    equipment, HolsterPlacement.LEFT_HAND_SLOT, state.weaponAmount);
            renderSlot(body, queue, parentMatrix, color, mode, parentModel, renderState,
                    equipment, HolsterPlacement.RIGHT_HAND_SLOT, state.weaponAmount);
        } catch (Throwable failure) {
            disabled = true;
            logOnce("render", "Holster equipment rendering failed; vanilla rendering will resume",
                    failure);
        }
    }

    private static void renderSlot(PlayerBodyRenderable body, Queue queue,
                                   Matrix parentMatrix, Color color, ModelRenderMode mode,
                                   ModelResourceWrapper parentModel, RenderState renderState,
                                   AttachedCellRenderable[] equipment, int slot, float amount) {
        if (slot < 0 || slot >= equipment.length) return;
        AttachedCellRenderable attachment = equipment[slot];
        if (attachment == null) return;
        String sourceAnchor = sourceAnchor(slot);
        HolsterItemClass type = classify(body, attachment, slot);
        HolsterPlacement placement = WUThirdPersonViewMod.holsterPlacement(
                attachment, type, slot);
        AbstractModelData playerData = parentModel.getModelData();
        Matrix targetBase = playerData instanceof ColladaModelData
                ? placement.mountMatrix((ColladaModelData) playerData, parentMatrix)
                : null;
        if (targetBase == null) {
            logOnce("anchor:" + placement.anchor,
                    "Player model has no holster socket or joint " + placement.anchor
                            + "; keeping affected equipment in hand", null);
            renderAt(attachment, queue, parentMatrix, color, mode, parentModel,
                    renderState, sourceAnchor);
            return;
        }

        synchronized (attachment) {
            prepareAt(attachment, parentMatrix, parentModel, sourceAnchor);
            Matrix source = new Matrix(attachment.getModelMatrix());
            if (amount <= 0.0001f) {
                attachment.render(queue, mode, renderState, color);
                return;
            }

            Matrix target = new Matrix();
            Matrix.mult(targetBase,
                    placement.localMatrix(attachment, sourceAnchor, body.isMale()), target);
            if (amount >= 0.9999f) attachment.getModelMatrix().set(target);
            else HolsterMatrixInterpolator.interpolate(source, target, parentMatrix, amount,
                    placement.transferClearance, attachment.getModelMatrix());
            attachment.render(queue, mode, renderState, color);
            WUThirdPersonViewMod.renderHolsterStudioGizmo(
                    attachment, slot, queue, targetBase, placement);
        }
    }

    private static void renderAt(AttachedCellRenderable attachment, Queue queue,
                                 Matrix parentMatrix, Color color, ModelRenderMode mode,
                                 ModelResourceWrapper parentModel, RenderState renderState,
                                 String anchor) {
        synchronized (attachment) {
            prepareAt(attachment, parentMatrix, parentModel, anchor);
            attachment.render(queue, mode, renderState, color);
        }
    }

    private static void prepareAt(AttachedCellRenderable attachment, Matrix parentMatrix,
                                  ModelResourceWrapper parentModel, String anchor) {
        attachment.setNullName(anchor);
        attachment.setParentMatrix(parentMatrix);
        attachment.setParentModel(parentModel);
        attachment.prepareRender();
    }

    static HolsterItemClass classify(PlayerBodyRenderable body,
                                     AttachedCellRenderable attachment, int slot) {
        String name = attachment.getItemName();
        ModelResourceWrapper wrapper = attachment.getModelWrapper();
        if ((name == null || name.trim().isEmpty()) && wrapper != null
                && wrapper.getResourceUrl() != null) name = wrapper.getResourceUrl().toString();
        float longest = longestDimension(wrapper);
        return HolsterItemClassifier.classify(name,
                slot == HolsterPlacement.SHIELD_SLOT,
                slot != HolsterPlacement.SHIELD_SLOT && isTwoHanded(body, name), longest);
    }

    private static boolean isTwoHanded(PlayerBodyRenderable body, String name) {
        try {
            World world = CellRenderable.getWorld();
            if (world == null || name == null) return false;
            ArmorMeshData data = PlayerArmorBuilder.getArmorMeshData(world, name,
                    body.getKingdomId(), body.isMale());
            return data != null && data.getBoolean1() && data.getBoolean2();
        } catch (Throwable unavailable) {
            return false;
        }
    }

    private static boolean isLocalPlayer(PlayerBodyRenderable body) {
        try {
            World world = CellRenderable.getWorld();
            return body != null && world != null && world.getPlayer() != null
                    && world.getPlayer().getPlayerBody() == body;
        } catch (Throwable unavailable) {
            return false;
        }
    }

    private static float longestDimension(ModelResourceWrapper wrapper) {
        try {
            ModelDataBounds bounds = wrapper == null ? null : wrapper.getBounds();
            if (bounds == null) return 0.0f;
            float x = Math.abs(bounds.getX1() - bounds.getX0());
            float h = Math.abs(bounds.getH1() - bounds.getH0());
            float y = Math.abs(bounds.getY1() - bounds.getY0());
            return Math.max(x, Math.max(h, y));
        } catch (Throwable unavailable) {
            return 0.0f;
        }
    }

    static String sourceAnchor(int slot) {
        if (slot == HolsterPlacement.LEFT_HAND_SLOT) return "NullLeftHand";
        if (slot == HolsterPlacement.RIGHT_HAND_SLOT) return "NullRightHand";
        return "NullShield";
    }

    private static void logOnce(String key, String message, Throwable failure) {
        if (!LOGGED_FAILURES.add(key)) return;
        if (failure == null) LOG.warning(message);
        else LOG.log(Level.WARNING, message, failure);
    }
}
