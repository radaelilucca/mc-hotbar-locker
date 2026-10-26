package com.radaeli.hotbarlocker.forge;

import com.radaeli.hotbarlocker.HotbarLockService;
import com.radaeli.hotbarlocker.HotbarLocker;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.Supplier;

public final class HotbarLockerForgeNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(HotbarLocker.MOD_ID, "main"), () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

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

    public static void toggle(int slot) { CHANNEL.sendToServer(new Toggle(slot)); }
    public static void sync(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new State(HotbarLockService.mask(player)));
    }

    private static void handleToggle(Toggle packet, Supplier<NetworkEvent.Context> supplier) {
        ServerPlayer player = supplier.get().getSender();
        if (player != null && packet.slot() >= 0 && packet.slot() < HotbarLocker.HOTBAR_SIZE) {
            boolean locked = HotbarLockService.state(player).toggle(packet.slot());
            player.displayClientMessage(Component.translatable(
                    locked ? "message.hotbarlocker.locked" : "message.hotbarlocker.unlocked", packet.slot() + 1), true);
            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS,
                    0.35f, locked ? 0.65f : 1.25f);
            sync(player);
        }
        supplier.get().setPacketHandled(true);
    }

    private static void handleState(State packet, Supplier<NetworkEvent.Context> supplier) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> HotbarLockerForgeClient.applyMask(packet.mask()));
        supplier.get().setPacketHandled(true);
    }

    private record Toggle(int slot) { }
    private record State(int mask) { }
}
