package org.wuthirdpersonview.client;

import com.wurmonline.client.renderer.Matrix;
import com.wurmonline.client.renderer.model.collada.math.Matrix4f;
import com.wurmonline.client.renderer.model.collada.math.Quaternion;
import com.wurmonline.math.Vector3f;
import org.wuthirdpersonview.core.CameraMath;

final class HolsterMatrixInterpolator {
    private static final float TRANSFER_START = 0.40f;
    private static final float TRANSFER_END = 0.60f;

    private HolsterMatrixInterpolator() { }

    static float attachmentBlend(float holsterAmount) {
        float linear = (CameraMath.clamp(holsterAmount, 0.0f, 1.0f) - TRANSFER_START)
                / (TRANSFER_END - TRANSFER_START);
        float clamped = CameraMath.clamp(linear, 0.0f, 1.0f);
        return clamped * clamped * (3.0f - 2.0f * clamped);
    }

    static Matrix interpolate(Matrix source, Matrix target, Matrix parent,
                              float holsterAmount, float clearance, Matrix output) {
        float blend = attachmentBlend(holsterAmount);
        if (blend <= 0.0f) return output.set(source);
        if (blend >= 1.0f) return output.set(target);

        Components from = Components.from(source);
        Components to = Components.from(target);
        Quaternion rotation = new Quaternion(from.rotation);
        rotation.slerp(to.rotation, blend);

        float arc = (float) Math.sin(Math.PI * blend) * clearance;
        Vector3f back = localBack(parent);
        float x = lerp(from.x, to.x, blend) + back.x * arc;
        float y = lerp(from.y, to.y, blend) + back.y * arc;
        float z = lerp(from.z, to.z, blend) + back.z * arc;

        Matrix4f value = new Matrix4f();
        rotation.toRotationMatrix(value);
        value.scale(new Vector3f(lerp(from.scaleX, to.scaleX, blend),
                lerp(from.scaleY, to.scaleY, blend),
                lerp(from.scaleZ, to.scaleZ, blend)));
        value.setTranslation(x, y, z);
        return output.fromColladaMatrix(value);
    }

    private static Vector3f localBack(Matrix parent) {
        if (parent == null) return new Vector3f(0.0f, 0.0f, -1.0f);
        Vector3f result = new Vector3f(-parent._m20(), -parent._m21(), -parent._m22());
        float length = result.length();
        if (length < 0.0001f) return new Vector3f(0.0f, 0.0f, -1.0f);
        result.multLocal(1.0f / length);
        return result;
    }

    private static float lerp(float from, float to, float amount) {
        return from + (to - from) * amount;
    }

    private static final class Components {
        final float x;
        final float y;
        final float z;
        final float scaleX;
        final float scaleY;
        final float scaleZ;
        final Quaternion rotation;

        private Components(float x, float y, float z, float scaleX, float scaleY,
                           float scaleZ, Quaternion rotation) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.scaleX = scaleX;
            this.scaleY = scaleY;
            this.scaleZ = scaleZ;
            this.rotation = rotation;
        }

        static Components from(Matrix value) {
            float sx = length(value._m00(), value._m01(), value._m02());
            float sy = length(value._m10(), value._m11(), value._m12());
            float sz = length(value._m20(), value._m21(), value._m22());
            if (sx < 0.0001f) sx = 1.0f;
            if (sy < 0.0001f) sy = 1.0f;
            if (sz < 0.0001f) sz = 1.0f;
            Matrix4f rotationMatrix = new Matrix4f();
            rotationMatrix.setIdentity();
            rotationMatrix.m00 = value._m00() / sx;
            rotationMatrix.m01 = value._m10() / sy;
            rotationMatrix.m02 = value._m20() / sz;
            rotationMatrix.m10 = value._m01() / sx;
            rotationMatrix.m11 = value._m11() / sy;
            rotationMatrix.m12 = value._m21() / sz;
            rotationMatrix.m20 = value._m02() / sx;
            rotationMatrix.m21 = value._m12() / sy;
            rotationMatrix.m22 = value._m22() / sz;
            Quaternion rotation = new Quaternion();
            rotation.fromRotationMatrix(rotationMatrix);
            rotation.normalize();
            return new Components(value._m30(), value._m31(), value._m32(),
                    sx, sy, sz, rotation);
        }

        private static float length(float x, float y, float z) {
            return (float) Math.sqrt(x * x + y * y + z * z);
        }
    }
}
