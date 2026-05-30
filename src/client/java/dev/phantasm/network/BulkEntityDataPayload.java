package dev.phantasm.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/*
 Payload for the modelengine:bulk_data channel
 The server sends a single packet containing transform data for all ME bone entities
 encoded with half-precision floats to cut bandwidth roughly in half
*/
public record BulkEntityDataPayload(PacketByteBuf data) implements CustomPayload {

    public static final byte PACKET_TYPE_BULK_DATA = 0x00;

    public static final int FIELD_TRANSLATION        = 0;
    public static final int FIELD_LEFT_ROTATION      = 1;
    public static final int FIELD_SCALE              = 2;
    public static final int FIELD_RIGHT_ROTATION     = 3;
    public static final int FIELD_TRANSFORM_DURATION = 4;
    public static final int FIELD_GLOW_DATA          = 5;
    public static final int FIELD_BRIGHTNESS         = 6;
    public static final int FIELD_RENDER_DATA        = 7;

    public static final CustomPayload.Id<BulkEntityDataPayload> ID =
            new CustomPayload.Id<>(Identifier.of("modelengine", "bulk_data"));

    public static final PacketCodec<PacketByteBuf, BulkEntityDataPayload> CODEC =
            CustomPayload.codecOf(BulkEntityDataPayload::write, BulkEntityDataPayload::read);

    private void write(PacketByteBuf buf) {
        buf.writeBytes(data.copy());
    }

    private static BulkEntityDataPayload read(PacketByteBuf buf) {
        return new BulkEntityDataPayload(new PacketByteBuf(buf.readBytes(buf.readableBytes())));
    }

    @Override
    public CustomPayload.Id<BulkEntityDataPayload> getId() {
        return ID;
    }
}
