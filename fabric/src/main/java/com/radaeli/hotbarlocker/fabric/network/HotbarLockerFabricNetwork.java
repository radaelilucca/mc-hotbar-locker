package com.radaeli.hotbarlocker.fabric.network;

import com.radaeli.hotbarlocker.HotbarLockService;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public final class HotbarLockerFabricNetwork {
    private HotbarLockerFabricNetwork() { }
    public static void initializeServer() {
        PayloadTypeRegistry.playC2S().register(ToggleLockPayload.TYPE, ToggleLockPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LockStatePayload.TYPE, LockStatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ToggleLockPayload.TYPE, (payload, context) -> context.server().execute(() -> {
            ServerPlayer player = context.player();
            if (payload.slot() < 0 || payload.slot() >= 9) return;
            boolean locked = HotbarLockService.state(player).toggle(payload.slot());
            player.displayClientMessage(Component.translatable(
                    locked ? "message.hotbarlocker.locked" : "message.hotbarlocker.unlocked", payload.slot() + 1), true);
            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.35f,
                    locked ? 0.65f : 1.25f);
            sync(player);
        }));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
    }
    public static void sync(ServerPlayer player) { ServerPlayNetworking.send(player, new LockStatePayload(HotbarLockService.mask(player))); }
}
