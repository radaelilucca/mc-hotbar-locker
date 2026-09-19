package com.radaeli.hotbarlocker.neoforge.network;

import com.radaeli.hotbarlocker.HotbarLockService;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class HotbarLockerNeoForgeNetwork {
    private HotbarLockerNeoForgeNetwork() { }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToServer(ToggleLockPayload.TYPE, ToggleLockPayload.CODEC, (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player) || payload.slot() < 0 || payload.slot() >= 9) return;
                    boolean locked = HotbarLockService.state(player).toggle(payload.slot());
                    player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                            locked ? "message.hotbarlocker.locked" : "message.hotbarlocker.unlocked", payload.slot() + 1), true);
                    player.playNotifySound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                            net.minecraft.sounds.SoundSource.PLAYERS, 0.35f, locked ? 0.65f : 1.25f);
                    sync(player);
                }))
                .playToClient(LockStatePayload.TYPE, LockStatePayload.CODEC, (payload, context) -> context.enqueueWork(() -> {
                    net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
                    if (client.player == null) return;
                    HotbarLockService.setMask(client.player, payload.mask());
                }));
    }
    public static void sync(ServerPlayer player) { PacketDistributor.sendToPlayer(player, new LockStatePayload(HotbarLockService.mask(player))); }
}
