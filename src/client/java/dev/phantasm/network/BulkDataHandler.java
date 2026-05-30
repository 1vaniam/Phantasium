package dev.phantasm.network;

import dev.phantasm.PhantasmClient;
import dev.phantasm.registry.LodThrottleRegistry;
import dev.phantasm.registry.ModelEngineRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.network.PacketByteBuf;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/*
 decodes and applies the modelengine:bulk_data packet
 LOD throttling: bone groups whose root is beyond lodDistanceBlocks skip
*/
public final class BulkDataHandler {

    private BulkDataHandler() {}

    public static void handle(BulkEntityDataPayload payload) {
        MinecraftClient mc = MinecraftClient.getInstance();
        PacketByteBuf buf = payload.data();
        BoneEntry[] entries = decode(buf);
        mc.execute(() -> apply(mc, entries));
    }

    /** Reusable decode buffer = eliminates one Object[8] allocation per bone per packet */
    private static final ThreadLocal<Object[]> DECODE_BUF = ThreadLocal.withInitial(() -> new Object[8]);

    private record BoneEntry(int entityId, byte bitmask, Object[] fields) {}

    private static BoneEntry[] decode(PacketByteBuf buf) {
        byte packetType = buf.readByte();
        if (packetType != BulkEntityDataPayload.PACKET_TYPE_BULK_DATA) {
            PhantasmClient.LOGGER.warn("[Phantasm] Unknown bulk_data packet type: {}", packetType);
            return new BoneEntry[0];
        }

        int count = buf.readVarInt();
        BoneEntry[] entries = new BoneEntry[count];

        for (int i = 0; i < count; i++) {
            int entityId = buf.readVarInt();
            byte bitmask = buf.readByte();
            Object[] fields = DECODE_BUF.get();
            // Clear only slots that may have been written by a previous bone in a different packet
            java.util.Arrays.fill(fields, null);

            if (hasBit(bitmask, BulkEntityDataPayload.FIELD_TRANSLATION))
                fields[BulkEntityDataPayload.FIELD_TRANSLATION] = readHalfVec3(buf);
            if (hasBit(bitmask, BulkEntityDataPayload.FIELD_LEFT_ROTATION))
                fields[BulkEntityDataPayload.FIELD_LEFT_ROTATION] = readHalfQuat(buf);
            if (hasBit(bitmask, BulkEntityDataPayload.FIELD_SCALE))
                fields[BulkEntityDataPayload.FIELD_SCALE] = readHalfVec3(buf);
            if (hasBit(bitmask, BulkEntityDataPayload.FIELD_RIGHT_ROTATION))
                fields[BulkEntityDataPayload.FIELD_RIGHT_ROTATION] = readHalfQuat(buf);
            if (hasBit(bitmask, BulkEntityDataPayload.FIELD_TRANSFORM_DURATION))
                fields[BulkEntityDataPayload.FIELD_TRANSFORM_DURATION] = buf.readVarInt();
            if (hasBit(bitmask, BulkEntityDataPayload.FIELD_GLOW_DATA))
                fields[BulkEntityDataPayload.FIELD_GLOW_DATA] = new GlowData(buf.readByte(), buf.readInt());
            if (hasBit(bitmask, BulkEntityDataPayload.FIELD_BRIGHTNESS))
                fields[BulkEntityDataPayload.FIELD_BRIGHTNESS] = buf.readInt();
            if (hasBit(bitmask, BulkEntityDataPayload.FIELD_RENDER_DATA))
                fields[BulkEntityDataPayload.FIELD_RENDER_DATA] = new RenderData(buf.readByte(), buf.readFloat(), buf.readByte());

            entries[i] = new BoneEntry(entityId, bitmask, fields);
        }

        return entries;
    }

    private static void apply(MinecraftClient mc, BoneEntry[] entries) {
        ClientWorld world = mc.world;
        if (world == null) return;

        ModelEngineRegistry meReg = ModelEngineRegistry.get();
        LodThrottleRegistry lodReg = LodThrottleRegistry.get();
        long currentTick = world.getTime();

        for (BoneEntry entry : entries) {
            Entity entity = world.getEntityById(entry.entityId());
            if (!(entity instanceof DisplayEntity)) continue;

            // LOD throttle: check root distance before applying transforms
            int rootId = meReg.getRootId(entry.entityId());
            if (rootId >= 0 && mc.player != null) {
                Entity root = world.getEntityById(rootId);
                if (root != null) {
                    double dx = root.getX() - mc.player.getX();
                    double dy = root.getY() - mc.player.getY();
                    double dz = root.getZ() - mc.player.getZ();
                    double distSq = dx * dx + dy * dy + dz * dz;
                    if (!lodReg.shouldUpdate(rootId, distSq, currentTick)) {
                        continue; // skip thiz bones transform update this tick
                    }
                }
            }

            List<net.minecraft.entity.data.DataTracker.SerializedEntry<?>> dataValues = new ArrayList<>();
            byte bitmask = entry.bitmask();
            boolean hasTransform = (bitmask & 0x0F) != 0;

            if (hasTransform)
                dataValues.add(DisplayFields.interpolationDelay(0));
            if (hasBit(bitmask, BulkEntityDataPayload.FIELD_TRANSLATION))
                dataValues.add(DisplayFields.translation((Vector3f) entry.fields()[BulkEntityDataPayload.FIELD_TRANSLATION]));
            if (hasBit(bitmask, BulkEntityDataPayload.FIELD_LEFT_ROTATION))
                dataValues.add(DisplayFields.leftRotation((Quaternionf) entry.fields()[BulkEntityDataPayload.FIELD_LEFT_ROTATION]));
            if (hasBit(bitmask, BulkEntityDataPayload.FIELD_SCALE))
                dataValues.add(DisplayFields.scale((Vector3f) entry.fields()[BulkEntityDataPayload.FIELD_SCALE]));
            if (hasBit(bitmask, BulkEntityDataPayload.FIELD_RIGHT_ROTATION))
                dataValues.add(DisplayFields.rightRotation((Quaternionf) entry.fields()[BulkEntityDataPayload.FIELD_RIGHT_ROTATION]));
            if (hasBit(bitmask, BulkEntityDataPayload.FIELD_TRANSFORM_DURATION))
                dataValues.add(DisplayFields.transformDuration((int) entry.fields()[BulkEntityDataPayload.FIELD_TRANSFORM_DURATION]));
            if (hasBit(bitmask, BulkEntityDataPayload.FIELD_GLOW_DATA)) {
                GlowData glow = (GlowData) entry.fields()[BulkEntityDataPayload.FIELD_GLOW_DATA];
                dataValues.add(DisplayFields.sharedData(glow.sharedData()));
                dataValues.add(DisplayFields.glowColor(glow.glowColor()));
            }
            if (hasBit(bitmask, BulkEntityDataPayload.FIELD_BRIGHTNESS))
                dataValues.add(DisplayFields.brightness((int) entry.fields()[BulkEntityDataPayload.FIELD_BRIGHTNESS]));
            if (hasBit(bitmask, BulkEntityDataPayload.FIELD_RENDER_DATA)) {
                RenderData render = (RenderData) entry.fields()[BulkEntityDataPayload.FIELD_RENDER_DATA];
                dataValues.add(DisplayFields.billboard(render.billboard()));
                // LOD: clamp viewRange to lodRenderDistanceBlocks (viewRange 1.0 = 64 blocks)
                float maxRange = (float) (dev.phantasm.config.PhantasmConfig.get().lodRenderDistanceBlocks / 64.0);
                dataValues.add(DisplayFields.viewRange(Math.min(render.viewRange(), maxRange)));
                dataValues.add(DisplayFields.displayType(render.displayType()));
            }

            if (!dataValues.isEmpty()) {
                entity.getDataTracker().writeUpdatedEntries(dataValues);
            }
        }
    }

    private static float halfToFloat(short half) {
        int h = half & 0xFFFF;
        int sign = (h & 0x8000) << 16;
        int exp = (h >>> 10) & 0x1F;
        int mantissa = h & 0x3FF;

        if (exp == 0) {
            if (mantissa == 0) return Float.intBitsToFloat(sign);
            exp = 1;
            while ((mantissa & 0x400) == 0) { mantissa <<= 1; exp--; }
            mantissa &= 0x3FF;
            return Float.intBitsToFloat(sign | ((exp + 127 - 15) << 23) | (mantissa << 13));
        } else if (exp == 31) {
            return Float.intBitsToFloat(sign | 0x7F800000 | (mantissa << 13));
        }
        return Float.intBitsToFloat(sign | ((exp + 127 - 15) << 23) | (mantissa << 13));
    }

    private static Vector3f readHalfVec3(PacketByteBuf buf) {
        return new Vector3f(halfToFloat(buf.readShort()), halfToFloat(buf.readShort()), halfToFloat(buf.readShort()));
    }

    private static Quaternionf readHalfQuat(PacketByteBuf buf) {
        return new Quaternionf(halfToFloat(buf.readShort()), halfToFloat(buf.readShort()), halfToFloat(buf.readShort()), halfToFloat(buf.readShort()));
    }

    private static boolean hasBit(byte bitmask, int bit) {
        return (bitmask & (1 << bit)) != 0;
    }

    private record GlowData(byte sharedData, int glowColor) {}
    private record RenderData(byte billboard, float viewRange, byte displayType) {}
}
