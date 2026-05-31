package dev.phantasm.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/*
 egisters the itemsadder:channel so the server knows we speak ItemsAdder's protocol
 Receiving any packet on this channel confirms ItemsAdder is installed server-side
 */
public record ItemsAdderPayload(PacketByteBuf data) implements CustomPayload {

    public static final CustomPayload.Id<ItemsAdderPayload> ID =
            new CustomPayload.Id<>(Identifier.of("itemsadder", "channel"));

    public static final PacketCodec<PacketByteBuf, ItemsAdderPayload> CODEC =
            CustomPayload.codecOf(ItemsAdderPayload::write, ItemsAdderPayload::read);

    private void write(PacketByteBuf buf) {
        buf.writeBytes(data.copy());
    }

    private static ItemsAdderPayload read(PacketByteBuf buf) {
        return new ItemsAdderPayload(new PacketByteBuf(buf.readBytes(buf.readableBytes())));
    }

    @Override
    public CustomPayload.Id<ItemsAdderPayload> getId() { return ID; }
}
