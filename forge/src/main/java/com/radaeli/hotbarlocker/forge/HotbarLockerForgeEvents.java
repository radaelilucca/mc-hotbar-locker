package com.radaeli.hotbarlocker.forge;

import com.mojang.blaze3d.platform.InputConstants;
import com.radaeli.hotbarlocker.HotbarLocker;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;

@Mod.EventBusSubscriber(modid = HotbarLocker.MOD_ID)
public final class HotbarLockerForgeEvents {
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) HotbarLockerForgeNetwork.sync(player);
    }
    @Mod.EventBusSubscriber(modid = HotbarLocker.MOD_ID, value = Dist.CLIENT)
    public static final class Client {
        private static final KeyMapping TOGGLE = new KeyMapping("key.hotbarlocker.toggle", InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.hotbarlocker");
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
            while (TOGGLE.consumeClick()) if (client.player != null) {
                HotbarLockerForgeNetwork.toggle(client.player.getInventory().selected);
            }
        }
        @Mod.EventBusSubscriber(modid = HotbarLocker.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
        public static final class Registration {
            @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(TOGGLE); }
        }
    }
}
