package com.radaeli.hotbarlocker.forge;

import com.mojang.blaze3d.platform.InputConstants;
import com.radaeli.hotbarlocker.HotbarLockService;
import com.radaeli.hotbarlocker.HotbarLocker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = HotbarLocker.MOD_ID, value = Dist.CLIENT)
public final class HotbarLockerForgeClient {
    private static final KeyMapping TOGGLE = new KeyMapping("key.hotbarlocker.toggle",
            InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), "key.categories.hotbarlocker");

    public static void applyMask(int mask) {
        var client = Minecraft.getInstance();
        if (client.player != null) HotbarLockService.setMask(client.player, mask);
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var client = Minecraft.getInstance();
        while (TOGGLE.consumeClick()) {
            if (client.player != null && client.screen == null) {
                HotbarLockerForgeNetwork.toggle(client.player.getInventory().selected);
            }
        }
    }

    @Mod.EventBusSubscriber(modid = HotbarLocker.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(TOGGLE); }
    }
}
