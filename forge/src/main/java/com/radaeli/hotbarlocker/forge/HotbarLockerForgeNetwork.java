package com.radaeli.hotbarlocker.forge;

import com.radaeli.hotbarlocker.HotbarLockService;
import com.radaeli.hotbarlocker.HotbarLocker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class HotbarLockerForgeNetwork {
    private static final SimpleChannel CHANNEL = ChannelBuilder.named(ResourceLocation.fromNamespaceAndPath(HotbarLocker.MOD_ID, "main"))
            .networkProtocolVersion(1).simpleChannel();
    private HotbarLockerForgeNetwork() { }
    public static void initialize() {
        CHANNEL.messageBuilder(Toggle.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> buffer.writeVarInt(packet.slot()))
                .decoder(buffer -> new Toggle(buffer.readVarInt()))
                .consumerMainThread(HotbarLockerForgeNetwork::handleToggle).add();
        CHANNEL.messageBuilder(State.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((packet, buffer) -> buffer.writeVarInt(packet.mask()))
                .decoder(buffer -> new State(buffer.readVarInt()))
                .consumerMainThread(HotbarLockerForgeNetwork::handleState).add();
    }
    public static void toggle(int slot) { CHANNEL.send(new Toggle(slot), PacketDistributor.SERVER.noArg()); }
    public static void sync(ServerPlayer player) { CHANNEL.send(new State(HotbarLockService.mask(player)), PacketDistributor.PLAYER.with(player)); }
    private static void handleToggle(Toggle packet, CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null && packet.slot() >= 0 && packet.slot() < 9) {
            boolean locked = HotbarLockService.state(player).toggle(packet.slot());
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    locked ? "message.hotbarlocker.locked" : "message.hotbarlocker.unlocked", packet.slot() + 1), true);
            player.playNotifySound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.35f, locked ? 0.65f : 1.25f);
            sync(player);
        }
        context.setPacketHandled(true);
    }
    private static void handleState(State packet, CustomPayloadEvent.Context context) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        if (client.player != null) {
            HotbarLockService.setMask(client.player, packet.mask());
        }
        context.setPacketHandled(true);
    }
    private record Toggle(int slot) { }
    private record State(int mask) { }
}
