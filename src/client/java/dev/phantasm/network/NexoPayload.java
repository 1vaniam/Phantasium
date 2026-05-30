package dev.phantasm.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/*
 Registers nexo:channel so the server knows we speak Nexos protocol
 Receiving any packet on this channel confirms Nexo is installed server-side
*/
public record NexoPayload(PacketByteBuf data) implements CustomPayload {

    public static final CustomPayload.Id<NexoPayload> ID =
            new CustomPayload.Id<>(Identifier.of("nexo", "channel"));

    public static final PacketCodec<PacketByteBuf, NexoPayload> CODEC =
            CustomPayload.codecOf(NexoPayload::write, NexoPayload::read);

    private void write(PacketByteBuf buf) {
        buf.writeBytes(data.copy());
    }

    private static NexoPayload read(PacketByteBuf buf) {
        return new NexoPayload(new PacketByteBuf(buf.readBytes(buf.readableBytes())));
    }

    @Override
    public CustomPayload.Id<NexoPayload> getId() { return ID; }
}
