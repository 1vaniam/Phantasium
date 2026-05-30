package dev.phantasm.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/*
 Registers the oraxen:channel so the server knows we speak Oraxens protocol
 Receiving any packet on this channel confirms Oraxen is installed server-side
*/
public record OraxenPayload(PacketByteBuf data) implements CustomPayload {

    public static final CustomPayload.Id<OraxenPayload> ID =
            new CustomPayload.Id<>(Identifier.of("oraxen", "channel"));

    public static final PacketCodec<PacketByteBuf, OraxenPayload> CODEC =
            CustomPayload.codecOf(OraxenPayload::write, OraxenPayload::read);

    private void write(PacketByteBuf buf) {
        buf.writeBytes(data.copy());
    }

    private static OraxenPayload read(PacketByteBuf buf) {
        return new OraxenPayload(new PacketByteBuf(buf.readBytes(buf.readableBytes())));
    }

    @Override
    public CustomPayload.Id<OraxenPayload> getId() { return ID; }
}
