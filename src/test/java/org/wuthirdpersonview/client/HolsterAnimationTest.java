package org.wuthirdpersonview.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.wurmonline.client.renderer.Matrix;
import com.wurmonline.client.renderer.model.collada.animation.Joint;
import com.wurmonline.client.renderer.model.collada.importer.ColladaModel;
import com.wurmonline.client.renderer.model.collada.importer.ColladaOffset;
import com.wurmonline.client.renderer.model.collada.math.Matrix4f;
import com.wurmonline.client.renderer.model.collada.math.Quaternion;
import com.wurmonline.math.Vector3f;
import java.util.Arrays;
import org.junit.Test;
import org.wuthirdpersonview.core.HolsterItemClass;

public class HolsterAnimationTest {
    @Test
    public void attachmentTransferHappensOnlyAroundTheReachPeak() {
        assertEquals(0.0f, HolsterMatrixInterpolator.attachmentBlend(0.39f), 0.001f);
        assertEquals(0.5f, HolsterMatrixInterpolator.attachmentBlend(0.50f), 0.001f);
        assertEquals(1.0f, HolsterMatrixInterpolator.attachmentBlend(0.61f), 0.001f);
    }

    @Test
    public void matrixInterpolationPreservesRigidRotationAndMovesOutward() {
        Matrix source = new Matrix().fromTranslationRotation(0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f);
        Matrix target = new Matrix().fromTranslationRotation(2.0f, 0.0f, 0.0f,
                0.0f, (float) Math.PI, 0.0f);
        Matrix parent = new Matrix().identity();
        Matrix result = HolsterMatrixInterpolator.interpolate(source, target, parent,
                0.50f, 0.2f, new Matrix());

        assertEquals(1.0f, result._m30(), 0.01f);
        assertEquals(-0.2f, result._m32(), 0.01f);
        float axisLength = (float) Math.sqrt(result._m00() * result._m00()
                + result._m01() * result._m01() + result._m02() * result._m02());
        assertEquals(1.0f, axisLength, 0.01f);
    }

    @Test
    public void placementMatchesRequestedCarryLocations() {
        HolsterPlacement belt = HolsterPlacement.forItem(HolsterItemClass.SHORT_BELT, 1);
        HolsterPlacement shoulder = HolsterPlacement.forItem(
                HolsterItemClass.MEDIUM_SHOULDER, 1);
        HolsterPlacement diagonal = HolsterPlacement.forItem(
                HolsterItemClass.TWO_HANDED_DIAGONAL, 1);
        HolsterPlacement sword = HolsterPlacement.forItem(
                HolsterItemClass.TWO_HANDED_SWORD_CENTER, 1);
        assertEquals("Spine", belt.anchor);
        assertTrue(belt.x < 0.0f);
        assertEquals("SpineTop", shoulder.anchor);
        assertTrue(shoulder.x < 0.0f);
        assertTrue(shoulder.y > 0.2f);
        assertTrue(diagonal.x < 0.0f);
        assertTrue(diagonal.rotationZ > 20.0f);
        assertEquals(-0.02f, sword.x, 0.001f);
        assertEquals(0.38f, sword.y, 0.001f);
        assertEquals(-0.12f, sword.z, 0.001f);
        assertEquals(-90.0f, sword.rotationX, 0.001f);
        assertEquals(0.0f, sword.rotationZ, 0.001f);
    }

    @Test
    public void carriedItemDropsHeldOrientationButKeepsNativeScale() {
        HolsterPlacement center = HolsterPlacement.forItem(
                HolsterItemClass.TWO_HANDED_SWORD_CENTER, 1);
        Quaternion heldRotation = new Quaternion();
        heldRotation.fromAngleAxis((float) Math.PI / 2.0f, Vector3f.UNIT_Z);
        ColladaOffset handOffset = new ColladaOffset(0.11f, 0.22f, 0.33f,
                heldRotation, 0.8f, 0.7f);
        Matrix carried = center.localMatrix(handOffset, true);
        Vector3f attached = HolsterPlacement.transformPoint(carried,
                center.modelX, center.modelY, center.modelZ);
        assertEquals(center.x, attached.x, 0.001f);
        assertEquals(center.y, attached.y, 0.001f);
        assertEquals(center.z, attached.z, 0.001f);
        Vector3f pommel = HolsterPlacement.transformPoint(
                carried, 0.0f, 0.03f, 0.204f);
        Vector3f tip = HolsterPlacement.transformPoint(
                carried, 0.0f, 0.03f, -1.107f);
        assertTrue(pommel.y > tip.y);
        assertEquals(0.8f, Math.abs(carried._m00()), 0.001f);
    }

    @Test
    public void animationGripFollowsThePhysicalHandleThroughTheEditedCarryPose() {
        Quaternion heldRotation = new Quaternion();
        heldRotation.fromAngleAxis((float) Math.PI, Vector3f.UNIT_Y);
        ColladaOffset handOffset = new ColladaOffset(0.003f, -0.025f, -0.049f,
                heldRotation, 1.0f, 1.0f);
        Vector3f physicalGrip = new Vector3f(0.0f, 0.03f, 0.02f);

        HolsterPlacement edited = HolsterPlacement.forItem(
                HolsterItemClass.TWO_HANDED_SWORD_CENTER, 1).withValues(
                "SpineTop", -0.08f, 0.24f, -0.09f,
                0.0f, 0.03f, -0.14f,
                -90.0f, 0.0f, 15.0f);
        Matrix carried = edited.localMatrix(handOffset, true);
        Vector3f visualAttachment = HolsterPlacement.transformPoint(carried,
                edited.modelX, edited.modelY, edited.modelZ);
        Vector3f carriedGrip = HolsterPlacement.transformPoint(carried,
                physicalGrip.x, physicalGrip.y, physicalGrip.z);
        Vector3f carriedAxisEnd = HolsterPlacement.transformPoint(carried,
                physicalGrip.x, physicalGrip.y, physicalGrip.z + 1.0f);
        Vector3f carriedAxis = carriedAxisEnd.subtract(carriedGrip).normalizeLocal();
        Matrix4f identityAnchor = new Matrix4f();
        identityAnchor.setIdentity();
        HolsterPlacement.GripPose animationTarget = edited.animationGripPose(
                identityAnchor, handOffset, true, physicalGrip);

        assertEquals(edited.x, visualAttachment.x, 0.001f);
        assertEquals(edited.y, visualAttachment.y, 0.001f);
        assertEquals(edited.z, visualAttachment.z, 0.001f);
        assertVector(carriedGrip, animationTarget.point);
        assertVector(carriedAxis, animationTarget.axis);
        assertEquals(-0.121411f, animationTarget.point.x, 0.001f);
        assertEquals(0.394548f, animationTarget.point.y, 0.001f);
        assertEquals(-0.090000f, animationTarget.point.z, 0.001f);
        assertEquals(-0.258819f, animationTarget.axis.x, 0.001f);
        assertEquals(0.965926f, animationTarget.axis.y, 0.001f);
        assertEquals(0.000000f, animationTarget.axis.z, 0.001f);
        assertTrue(carriedGrip.distance(visualAttachment) > 0.15f);
    }

    @Test
    public void shieldModelsAreUprightAndTheirFrontFacesAwayFromTheBack() {
        HolsterItemClass[] shieldTypes = {
                HolsterItemClass.SHIELD_SMALL,
                HolsterItemClass.SHIELD_MEDIUM,
                HolsterItemClass.SHIELD_LARGE
        };
        for (HolsterItemClass type : shieldTypes) {
            HolsterPlacement shield = HolsterPlacement.forItem(
                    type, HolsterPlacement.SHIELD_SLOT);
            Matrix carried = shield.localMatrix((ColladaOffset) null, true);
            // Wurm's shield models have their wide top at negative local Z.
            Vector3f wideTop = HolsterPlacement.transformPoint(
                    carried, 0.0f, 0.04f, -0.535f);
            Vector3f pointedBottom = HolsterPlacement.transformPoint(
                    carried, 0.0f, 0.04f, 0.535f);
            Vector3f frontFace = HolsterPlacement.transformPoint(
                    carried, 0.0f, 0.111f, 0.0f);
            Vector3f attached = HolsterPlacement.transformPoint(carried,
                    shield.modelX, shield.modelY, shield.modelZ);

            assertTrue(wideTop.y > pointedBottom.y);
            assertEquals(wideTop.z, pointedBottom.z, 0.001f);
            assertTrue(frontFace.z < attached.z);
            assertEquals(shield.x, attached.x, 0.001f);
            assertEquals(shield.y, attached.y, 0.001f);
            assertEquals(shield.z, attached.z, 0.001f);
        }
    }

    @Test
    public void carryAnchorsFallBackToAnimatedSpineJoints() {
        ColladaModel wurmRig = new ColladaModel("wurm-rig");
        wurmRig.addJoint(jointAt("SpineTop", 1.0f, 2.0f, 3.0f));
        Matrix4f wurmAnchor = HolsterPlacement.jointAnchorTransform(
                wurmRig, "SpineTop");
        assertEquals(1.0f, wurmAnchor.m03, 0.001f);
        assertEquals(2.0f, wurmAnchor.m13, 0.001f);
        assertEquals(3.0f, wurmAnchor.m23, 0.001f);

        ColladaModel avatarRig = new ColladaModel("avatar-rig");
        avatarRig.addJoint(jointAt("spine_01", 4.0f, 5.0f, 6.0f));
        avatarRig.addJoint(jointAt("spine_03", 7.0f, 8.0f, 9.0f));
        Matrix4f avatarLower = HolsterPlacement.jointAnchorTransform(
                avatarRig, "Spine");
        Matrix4f avatarUpper = HolsterPlacement.jointAnchorTransform(
                avatarRig, "SpineTop");
        assertEquals(4.0f, avatarLower.m03, 0.001f);
        assertEquals(5.0f, avatarLower.m13, 0.001f);
        assertEquals(6.0f, avatarLower.m23, 0.001f);
        assertEquals(7.0f, avatarUpper.m03, 0.001f);
        assertEquals(8.0f, avatarUpper.m13, 0.001f);
        assertEquals(9.0f, avatarUpper.m23, 0.001f);
    }

    @Test
    public void allHolsterFamiliesPrepareTheElbowInFrontOfTheBody() {
        HolsterItemClass[] types = {
                HolsterItemClass.SHORT_BELT,
                HolsterItemClass.MEDIUM_SHOULDER,
                HolsterItemClass.TWO_HANDED_DIAGONAL,
                HolsterItemClass.TWO_HANDED_SWORD_CENTER,
                HolsterItemClass.SHIELD_SMALL,
                HolsterItemClass.SHIELD_MEDIUM,
                HolsterItemClass.SHIELD_LARGE
        };
        for (HolsterItemClass type : types) {
            Vector3f right = HolsterArmAnimator.upperDirectionFor(type, false, false);
            Vector3f left = HolsterArmAnimator.upperDirectionFor(type, true, false);
            assertEquals(1.0f, right.length(), 0.001f);
            assertEquals(1.0f, left.length(), 0.001f);
            assertTrue(right.z > 0.0f);
            assertTrue(left.z > 0.0f);
            assertTrue(HolsterArmAnimator.elbowFlexDegrees(
                    new Vector3f(0.0f, -1.0f, 0.0f), right) <= 172.01f);
            if (type != HolsterItemClass.TWO_HANDED_SWORD_CENTER) {
                assertTrue(right.x < 0.0f);
                assertTrue(left.x > 0.0f);
            }
        }
    }

    @Test
    public void requestedForearmDirectionIsClampedToHumanElbowFlexion() {
        Vector3f upper = new Vector3f(0.0f, 1.0f, 0.0f);
        Vector3f currentLower = new Vector3f(0.0f, 0.0f, 1.0f);
        Vector3f hyperflexed = HolsterArmAnimator.constrainedForearmDirection(
                upper, currentLower, new Vector3f(0.0f, -1.0f, 0.01f));
        Vector3f hyperextended = HolsterArmAnimator.constrainedForearmDirection(
                upper, currentLower, new Vector3f(0.0f, 1.0f, 0.0f));
        Vector3f backwards = HolsterArmAnimator.constrainedForearmDirection(
                upper, currentLower, new Vector3f(0.0f, 0.0f, -1.0f));

        assertEquals(140.0f,
                HolsterArmAnimator.elbowFlexDegrees(upper, hyperflexed), 0.01f);
        assertEquals(5.0f,
                HolsterArmAnimator.elbowFlexDegrees(upper, hyperextended), 0.01f);
        assertEquals(5.0f,
                HolsterArmAnimator.elbowFlexDegrees(upper, backwards), 0.01f);
        assertTrue(backwards.z > 0.0f);
    }

    @Test
    public void centeredSwordUsesOnlyItsHoldingHand() {
        assertTrue(HolsterArmAnimator.usesSupportHand(
                HolsterItemClass.TWO_HANDED_DIAGONAL));
        assertFalse(HolsterArmAnimator.usesSupportHand(
                HolsterItemClass.TWO_HANDED_SWORD_CENTER));
    }

    @Test
    public void centeredSwordOverlapsShoulderAndElbowPhases() {
        assertEquals(0.0f, HolsterArmAnimator.stagedShoulderWeight(0.0f), 0.001f);
        assertEquals(0.5f, HolsterArmAnimator.stagedShoulderWeight(0.22f), 0.001f);
        assertEquals(1.0f, HolsterArmAnimator.stagedShoulderWeight(0.44f), 0.001f);
        assertEquals(1.0f, HolsterArmAnimator.stagedShoulderWeight(0.58f), 0.001f);
        assertEquals(0.5f, HolsterArmAnimator.stagedShoulderWeight(0.79f), 0.001f);
        assertEquals(0.0f, HolsterArmAnimator.stagedShoulderWeight(1.0f), 0.001f);

        assertEquals(0.0f, HolsterArmAnimator.stagedElbowWeight(0.24f), 0.001f);
        assertEquals(0.5f, HolsterArmAnimator.stagedElbowWeight(0.43f), 0.001f);
        assertEquals(1.0f, HolsterArmAnimator.stagedElbowWeight(0.62f), 0.001f);
        assertEquals(0.5f, HolsterArmAnimator.stagedElbowWeight(0.73f), 0.001f);
        assertEquals(0.0f, HolsterArmAnimator.stagedElbowWeight(0.84f), 0.001f);

        assertBetweenEndpoints(HolsterArmAnimator.stagedShoulderWeight(0.34f));
        assertBetweenEndpoints(HolsterArmAnimator.stagedElbowWeight(0.34f));
        assertBetweenEndpoints(HolsterArmAnimator.stagedShoulderWeight(0.73f));
        assertBetweenEndpoints(HolsterArmAnimator.stagedElbowWeight(0.73f));
    }

    @Test
    public void centeredSwordUpperArmStopsEightDegreesForwardOfVertical() {
        Vector3f right = HolsterArmAnimator.centerSwordUpperDirection(false);
        Vector3f left = HolsterArmAnimator.centerSwordUpperDirection(true);
        float rightAngle = (float) Math.toDegrees(Math.acos(right.dot(Vector3f.UNIT_Y)));
        assertEquals(8.0f, rightAngle, 0.01f);
        assertEquals(0.0f, right.x, 0.001f);
        assertTrue(right.z > 0.0f);
        assertVector(right, left);
    }

    @Test
    public void centeredSwordBlendsBoneMotionAroundTheConfiguredGrip() {
        Vector3f mount = new Vector3f(-0.02f, 1.42f, -0.12f);
        ColladaModel raisedModel = rightArmModel();
        HolsterArmAnimator.poseCenterSwordArm(raisedModel, false, mount, 0.44f);
        Vector3f raisedShoulder = jointPosition(raisedModel, "RightArm");
        Vector3f raisedElbow = jointPosition(raisedModel, "RightForeArm");
        Vector3f raisedHand = jointPosition(raisedModel, "RightHand");
        Vector3f raisedUpper = raisedElbow.subtract(raisedShoulder).normalizeLocal();
        float upperAngle = (float) Math.toDegrees(Math.acos(raisedUpper.dot(Vector3f.UNIT_Y)));
        assertEquals(8.0f, upperAngle, 0.01f);
        Vector3f raisedLower = raisedHand.subtract(raisedElbow);
        Vector3f raisedPlane = raisedLower.subtract(
                raisedUpper.mult(raisedLower.dot(raisedUpper))).normalizeLocal();
        Vector3f toMount = mount.subtract(raisedElbow);
        Vector3f mountPlane = toMount.subtract(
                raisedUpper.mult(toMount.dot(raisedUpper))).normalizeLocal();
        assertTrue(raisedPlane.dot(mountPlane) > 0.0f);

        ColladaModel foldingModel = rightArmModel();
        HolsterArmAnimator.poseCenterSwordArm(foldingModel, false, mount, 0.58f);
        Vector3f foldingShoulder = jointPosition(foldingModel, "RightArm");
        Vector3f foldingElbow = jointPosition(foldingModel, "RightForeArm");
        Vector3f foldingHand = jointPosition(foldingModel, "RightHand");
        assertVector(raisedShoulder, foldingShoulder);
        assertTrue(raisedElbow.distance(foldingElbow) > 0.0001f);
        assertTrue(raisedHand.distance(foldingHand) > 0.05f);

        ColladaModel foldedModel = rightArmModel();
        Vector3f handleAxis = new Vector3f(0.25f, 0.90f, 0.35f).normalizeLocal();
        HolsterArmAnimator.poseCenterSwordArm(
                foldedModel, false, mount, handleAxis, 0.62f);
        Vector3f foldedShoulder = jointPosition(foldedModel, "RightArm");
        Vector3f foldedElbow = jointPosition(foldedModel, "RightForeArm");
        Vector3f foldedHand = jointPosition(foldedModel, "RightHand");
        assertVector(raisedShoulder, foldedShoulder);
        assertTrue(raisedElbow.distance(foldedElbow) > 0.0001f);
        assertVector(mount, foldedHand);
        assertTrue(HolsterArmAnimator.handGripAxis(
                foldedModel.getJoint("RightHand")).dot(handleAxis) > 0.999f);
        Vector3f foldedDirection = foldedHand.subtract(foldedElbow).normalizeLocal();
        Vector3f foldedUpper = foldedElbow.subtract(foldedShoulder).normalizeLocal();
        float foldedFlex = HolsterArmAnimator.elbowFlexDegrees(
                foldedUpper, foldedDirection);
        assertTrue(foldedFlex > 0.0f);
        assertTrue(foldedFlex < 180.0f);

        ColladaModel returnedElbowModel = rightArmModel();
        HolsterArmAnimator.poseCenterSwordArm(returnedElbowModel, false, mount, 0.84f);
        assertTrue(raisedElbow.distance(
                jointPosition(returnedElbowModel, "RightForeArm")) > 0.01f);

        ColladaModel returnedModel = rightArmModel();
        HolsterArmAnimator.poseCenterSwordArm(returnedModel, false, mount, 1.0f);
        assertVector(new Vector3f(-0.20f, 1.40f, 0.0f),
                jointPosition(returnedModel, "RightArm"));
        assertVector(new Vector3f(-0.40f, 1.10f, 0.0f),
                jointPosition(returnedModel, "RightForeArm"));
        assertVector(new Vector3f(-0.45f, 0.86f, 0.12f),
                jointPosition(returnedModel, "RightHand"));
    }

    @Test
    public void directMountMatrixAndVisualAttachmentUseTheSameSkeletonSpace() {
        Matrix4f spine = new Matrix4f();
        Quaternion spineRotation = new Quaternion();
        spineRotation.fromAngles(0.17f, -0.29f, 0.41f);
        spineRotation.toRotationMatrix(spine);
        spine.setTranslation(1.0f, 2.0f, 3.0f);
        HolsterPlacement placement = HolsterPlacement.forItem(
                HolsterItemClass.TWO_HANDED_SWORD_CENTER, 1);

        Matrix mount = HolsterPlacement.composeMount(new Matrix().identity(), spine);
        Matrix item = new Matrix();
        Matrix.mult(mount, placement.localMatrix((ColladaOffset) null, true), item);
        Vector3f hand = HolsterPlacement.point(spine,
                placement.x, placement.y, placement.z);
        Vector3f itemAttachment = HolsterPlacement.transformPoint(item,
                placement.modelX, placement.modelY, placement.modelZ);

        assertEquals(hand.x, itemAttachment.x, 0.001f);
        assertEquals(hand.y, itemAttachment.y, 0.001f);
        assertEquals(hand.z, itemAttachment.z, 0.001f);
    }

    @Test
    public void jointRotationKeepsPivotAndMovesAllChildrenRigidly() {
        Joint shoulder = jointAt("RightArm", 0.0f, 1.0f, 0.0f);
        Joint elbow = jointAt("RightForeArm", 1.0f, 1.0f, 0.0f);
        Joint hand = jointAt("RightHand", 2.0f, 1.0f, 0.0f);
        shoulder.setChildList(Arrays.asList(elbow));
        elbow.setChildList(Arrays.asList(hand));
        HolsterArmAnimator.rotateSubtree(shoulder,
                new Vector3f(1.0f, 0.0f, 0.0f),
                new Vector3f(0.0f, 1.0f, 0.0f),
                new Vector3f(0.0f, 1.0f, 0.0f));
        assertEquals(0.0f, shoulder.getBindMatrix().m03, 0.001f);
        assertEquals(1.0f, shoulder.getBindMatrix().m13, 0.001f);
        assertEquals(0.0f, elbow.getBindMatrix().m03, 0.001f);
        assertEquals(2.0f, elbow.getBindMatrix().m13, 0.001f);
        assertEquals(0.0f, hand.getBindMatrix().m03, 0.001f);
        assertEquals(3.0f, hand.getBindMatrix().m13, 0.001f);
    }

    private static Joint jointAt(String name, float x, float y, float z) {
        Joint joint = new Joint(name);
        Matrix4f matrix = new Matrix4f();
        matrix.setIdentity();
        matrix.setTranslation(x, y, z);
        joint.setBindMatrix(matrix);
        return joint;
    }

    private static ColladaModel rightArmModel() {
        ColladaModel model = new ColladaModel("test-arm");
        Joint shoulder = jointAt("RightArm", -0.20f, 1.40f, 0.0f);
        Joint elbow = jointAt("RightForeArm", -0.40f, 1.10f, 0.0f);
        Joint hand = jointAt("RightHand", -0.45f, 0.86f, 0.12f);
        shoulder.setChildList(Arrays.asList(elbow));
        elbow.setChildList(Arrays.asList(hand));
        model.addJoint(shoulder);
        model.addJoint(elbow);
        model.addJoint(hand);
        return model;
    }

    private static Vector3f jointPosition(ColladaModel model, String name) {
        Matrix4f value = model.getJoint(name).getBindMatrix();
        return new Vector3f(value.m03, value.m13, value.m23);
    }

    private static void assertVector(Vector3f expected, Vector3f actual) {
        assertEquals(expected.x, actual.x, 0.001f);
        assertEquals(expected.y, actual.y, 0.001f);
        assertEquals(expected.z, actual.z, 0.001f);
    }

    private static void assertBetweenEndpoints(float value) {
        assertTrue(value > 0.0f);
        assertTrue(value < 1.0f);
    }
}
