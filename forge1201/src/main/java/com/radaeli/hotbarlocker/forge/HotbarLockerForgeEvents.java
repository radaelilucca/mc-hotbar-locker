package com.radaeli.hotbarlocker.forge;

import com.radaeli.hotbarlocker.HotbarLockService;
import com.radaeli.hotbarlocker.HotbarLocker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HotbarLocker.MOD_ID)
public final class HotbarLockerForgeEvents {
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { sync(event); }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) { sync(event); }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) { sync(event); }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        HotbarLockService.setMask(event.getEntity(), HotbarLockService.mask(event.getOriginal()));
    }
    private static void sync(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) HotbarLockerForgeNetwork.sync(player);
    }
}
