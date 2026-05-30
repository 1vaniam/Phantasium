package dev.phantasm.network;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class DisplayFields {

    public static final int SHARED_DATA_ID          = 0;
    public static final int INTERPOLATION_DELAY_ID  = 8;
    public static final int TRANSFORM_DURATION_ID   = 9;
    public static final int TRANSLATION_ID          = 11;
    public static final int SCALE_ID                = 12;
    public static final int LEFT_ROTATION_ID        = 13;
    public static final int RIGHT_ROTATION_ID       = 14;
    public static final int BILLBOARD_ID            = 15;
    public static final int BRIGHTNESS_ID           = 16;
    public static final int VIEW_RANGE_ID           = 17;
    public static final int GLOW_COLOR_ID           = 22;
    public static final int DISPLAY_TYPE_ID         = 24;

    // VECTOR_3F is TrackedDataHandler<Vector3fc>, cast t concrete type
    @SuppressWarnings("unchecked")
    private static final TrackedDataHandler<Vector3f> VEC3F_HANDLER =
            (TrackedDataHandler<Vector3f>) (Object) TrackedDataHandlerRegistry.VECTOR_3F;

    @SuppressWarnings("unchecked")
    private static final TrackedDataHandler<Quaternionf> QUAT_HANDLER =
            (TrackedDataHandler<Quaternionf>) (Object) TrackedDataHandlerRegistry.QUATERNION_F;

    public static DataTracker.SerializedEntry<Byte> sharedData(byte value) {
        return new DataTracker.SerializedEntry<>(SHARED_DATA_ID, TrackedDataHandlerRegistry.BYTE, value);
    }
    public static DataTracker.SerializedEntry<Integer> interpolationDelay(int value) {
        return new DataTracker.SerializedEntry<>(INTERPOLATION_DELAY_ID, TrackedDataHandlerRegistry.INTEGER, value);
    }
    public static DataTracker.SerializedEntry<Integer> transformDuration(int value) {
        return new DataTracker.SerializedEntry<>(TRANSFORM_DURATION_ID, TrackedDataHandlerRegistry.INTEGER, value);
    }
    public static DataTracker.SerializedEntry<Vector3f> translation(Vector3f value) {
        return new DataTracker.SerializedEntry<>(TRANSLATION_ID, VEC3F_HANDLER, value);
    }
    public static DataTracker.SerializedEntry<Vector3f> scale(Vector3f value) {
        return new DataTracker.SerializedEntry<>(SCALE_ID, VEC3F_HANDLER, value);
    }
    public static DataTracker.SerializedEntry<Quaternionf> leftRotation(Quaternionf value) {
        return new DataTracker.SerializedEntry<>(LEFT_ROTATION_ID, QUAT_HANDLER, value);
    }
    public static DataTracker.SerializedEntry<Quaternionf> rightRotation(Quaternionf value) {
        return new DataTracker.SerializedEntry<>(RIGHT_ROTATION_ID, QUAT_HANDLER, value);
    }
    public static DataTracker.SerializedEntry<Byte> billboard(byte value) {
        return new DataTracker.SerializedEntry<>(BILLBOARD_ID, TrackedDataHandlerRegistry.BYTE, value);
    }
    public static DataTracker.SerializedEntry<Integer> brightness(int value) {
        return new DataTracker.SerializedEntry<>(BRIGHTNESS_ID, TrackedDataHandlerRegistry.INTEGER, value);
    }
    public static DataTracker.SerializedEntry<Float> viewRange(float value) {
        return new DataTracker.SerializedEntry<>(VIEW_RANGE_ID, TrackedDataHandlerRegistry.FLOAT, value);
    }
    public static DataTracker.SerializedEntry<Integer> glowColor(int value) {
        return new DataTracker.SerializedEntry<>(GLOW_COLOR_ID, TrackedDataHandlerRegistry.INTEGER, value);
    }
    public static DataTracker.SerializedEntry<Byte> displayType(byte value) {
        return new DataTracker.SerializedEntry<>(DISPLAY_TYPE_ID, TrackedDataHandlerRegistry.BYTE, value);
    }

    private DisplayFields() {}
}
