package com.radaeli.hotbarlocker.tests;

import com.mojang.blaze3d.platform.InputConstants;
import com.radaeli.hotbarlocker.HotbarLockService;
import com.radaeli.hotbarlocker.forge.HotbarLockerForgeNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Exercises real input, packets, inventory screens, HUD, sound dispatch and world reload. */
@Mod.EventBusSubscriber(modid = "hotbarlocker_tests", value = Dist.CLIENT)
public final class ClientValidation {
    private static final Minecraft MC = Minecraft.getInstance();
    private static final Queue<Step> STEPS = new ArrayDeque<>();
    private static final List<String> PASSED = new ArrayList<>();
    private static final List<Float> SOUNDS = new CopyOnWriteArrayList<>();
    private static final BlockPos TARGET = new BlockPos(0, 65, 0);
    private static final String WORLD = "validation-" + System.currentTimeMillis();
    private static boolean requestedWorld, prepared, finished;
    private static Step current;
    private static int ticks;
    private static CompletableFuture<Void> serverWork;
    private static KeyMapping toggle;

    @SubscribeEvent public static void sound(PlaySoundEvent event) {
        if (!Boolean.getBoolean("hotbarlocker.clientSmoke") || event.getSound() == null) return;
        if (event.getSound().getLocation().toString().equals("minecraft:entity.experience_orb.pickup")) {
            check(event.getSound().resolve(MC.getSoundManager()) != null, "Toggle sound resource is missing");
            SOUNDS.add(event.getSound().getPitch());
        }
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("hotbarlocker.clientSmoke") || finished || event.phase != TickEvent.Phase.END) return;
        try {
            if (!requestedWorld) {
                if (MC.screen != null && MC.screen.getClass().getSimpleName().contains("AccessibilityOnboarding")) {
                    MC.setScreen(new TitleScreen());
                }
                if (!(MC.screen instanceof TitleScreen)) return;
                requestedWorld = true;
                MC.options.renderDistance().set(4);
                MC.options.simulationDistance().set(5);
                MC.options.guiScale().set(2);
                MC.createWorldOpenFlows().createFreshLevel(WORLD,
                        new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true,
                                new GameRules(), WorldDataConfiguration.DEFAULT),
                        new WorldOptions(42L, false, false),
                        registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
                return;
            }
            if (!prepared) {
                if (MC.player == null || MC.getSingleplayerServer() == null || MC.screen != null) return;
                prepareSteps();
                prepared = true;
            }
            if (current == null) {
                current = STEPS.poll();
                if (current == null) { finish(null); return; }
                ticks = 0;
                current.start.run();
            }
            ticks++;
            if (ticks == 100) LoggerFactory.getLogger("hotbarlocker/validation").info("Waiting for {} (screen={}, mask={}, sounds={})", current.name, MC.screen, mask(), SOUNDS);
            if (ticks >= 15 && current.done.getAsBoolean()) {
                PASSED.add(current.name);
                LoggerFactory.getLogger("hotbarlocker/validation").info("PASS: {}", current.name);
                current = null;
            } else if (ticks > 1800) {
                throw new AssertionError("Timed out: " + current.name);
            }
        } catch (Throwable failure) { finish(failure); }
    }

    private static void prepareSteps() {
        server("Prepare Creative test inventory", p -> {
            check(p.server instanceof net.minecraft.client.server.IntegratedServer, "Expected isolated integrated server");
            ((net.minecraft.client.server.IntegratedServer) p.server).publishServer(GameType.CREATIVE, true, 0);
            p.setGameMode(GameType.CREATIVE);
            p.teleportTo(0.5, 65, 4.5);
            p.serverLevel().setBlockAndUpdate(TARGET, Blocks.GOLD_BLOCK.defaultBlockState());
            p.getInventory().clearContent();
            p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
            HotbarLockService.setMask(p, 0);
            flush(p);
        });
        step("Configurable key locks slot and dispatches low-pitch sound/message", () -> {
            toggle = java.util.Arrays.stream(MC.options.keyMappings)
                    .filter(key -> key.getName().equals("key.hotbarlocker.toggle")).findFirst().orElseThrow();
            toggle.setKey(InputConstants.getKey(298, 0));
            KeyMapping.resetMapping();
            MC.player.getInventory().selected = 0;
            SOUNDS.clear();
            KeyMapping.click(InputConstants.getKey(298, 0));
            com.radaeli.hotbarlocker.forge.HotbarLockerForgeClient.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        }, () -> mask() == 1 && hasSound(0.65f) && overlay().equals(Component.translatable("message.hotbarlocker.locked", 1).getString()));
        screenshot("locked-hud.png");
        step("Key unlocks slot and dispatches high-pitch sound/message", () -> {
            SOUNDS.clear();
            KeyMapping.click(InputConstants.getKey(298, 0));
            com.radaeli.hotbarlocker.forge.HotbarLockerForgeClient.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        }, () -> mask() == 0 && hasSound(1.25f) && overlay().equals(Component.translatable("message.hotbarlocker.unlocked", 1).getString()));
        step("Relock through production packet", () -> HotbarLockerForgeNetwork.toggle(0), () -> mask() == 1);
        step("Real Creative middle-click redirects to unlocked hotbar", ClientValidation::pick, () -> MC.player.getInventory().getItem(1).is(Items.GOLD_BLOCK));
        verify("Server receives Creative redirected pick", p -> p.getInventory().getItem(0).getCount() == 7 &&
                p.getInventory().getItem(1).is(Items.GOLD_BLOCK));

        server("Prepare all-locked Creative fallback", p -> {
            p.getInventory().clearContent();
            HotbarLockService.setMask(p, 511);
            flush(p);
        });
        step("Real Creative pick falls back to main inventory", ClientValidation::pick, () -> MC.player.getInventory().getItem(9).is(Items.GOLD_BLOCK));
        verify("Main-inventory Creative fallback persists on server", p -> {
            for (int i = 0; i < 9; i++) if (!p.getInventory().getItem(i).isEmpty()) return false;
            return p.getInventory().getItem(9).is(Items.GOLD_BLOCK);
        });
        screenshot("all-locked-hud.png");

        server("Prepare occupied Creative replacement", p -> {
            p.getInventory().clearContent();
            for (int i = 0; i < 9; i++) p.getInventory().setItem(i, new ItemStack(Items.DIAMOND, i + 1));
            HotbarLockService.setMask(p, 1);
            flush(p);
        });
        step("Creative pick preserves displaced stack", ClientValidation::pick, () -> MC.player.getInventory().getItem(9).getCount() == 2);
        verify("Displaced stack is synchronized to server", p -> p.getInventory().getItem(9).is(Items.DIAMOND) && p.getInventory().getItem(9).getCount() == 2);

        server("Prepare survival swap", p -> {
            p.setGameMode(GameType.SURVIVAL);
            p.getInventory().clearContent();
            p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
            p.getInventory().setItem(12, new ItemStack(Items.GOLD_BLOCK, 32));
            HotbarLockService.setMask(p, 1);
            flush(p);
        });
        step("Real survival middle-click swaps into unlocked slot", ClientValidation::pick,
                () -> MC.player.getInventory().selected == 1 && MC.player.getInventory().getItem(1).getCount() == 32);
        verify("Survival swap preserves locked stack and source", p -> p.getInventory().getItem(0).getCount() == 7 && p.getInventory().getItem(12).isEmpty());

        server("Prepare Creative clear-inventory test", p -> {
            p.setGameMode(GameType.CREATIVE);
            p.getInventory().clearContent();
            p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
            p.getInventory().setItem(1, new ItemStack(Items.STONE, 8));
            p.getInventory().setItem(12, new ItemStack(Items.GOLD_BLOCK, 32));
            HotbarLockService.setMask(p, 1);
            flush(p);
        });
        step("Creative Shift-trash preserves protected slot", () -> {
            try {
                var screen = new CreativeModeInventoryScreen(MC.player, MC.player.connection.enabledFeatures(), false);
                MC.setScreen(screen);
                var select = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
                select.setAccessible(true);
                select.invoke(screen, net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.get(CreativeModeTabs.INVENTORY));
                var destroy = CreativeModeInventoryScreen.class.getDeclaredField("destroyItemSlot");
                destroy.setAccessible(true);
                var click = CreativeModeInventoryScreen.class.getDeclaredMethod("slotClicked", Slot.class, int.class, int.class, ClickType.class);
                click.setAccessible(true);
                click.invoke(screen, destroy.get(screen), -999, 0, ClickType.QUICK_MOVE);
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        }, () -> MC.player.getInventory().getItem(1).isEmpty() && MC.player.getInventory().getItem(12).isEmpty());
        verify("Creative clear is synchronized without clearing locked stack", p -> p.getInventory().getItem(0).getCount() == 7 && p.getInventory().getItem(1).isEmpty() && p.getInventory().getItem(12).isEmpty());
        step("Close inventory", () -> MC.setScreen(null), () -> MC.screen == null);

        step("Pause menu stays open in a loaded world", () -> MC.setScreen(new PauseScreen(true)),
                () -> MC.screen instanceof PauseScreen);
        step("Close pause menu", () -> MC.setScreen(null), () -> MC.screen == null);

        server("Dimension change keeps server lock state", p -> {
            HotbarLockService.setMask(p, 341);
            p.teleportTo(p.server.getLevel(Level.NETHER), 0.5, 90, 0.5, 0, 0);
        });
        step("Dimension event synchronizes client locks", () -> { }, () -> MC.level.dimension() == Level.NETHER && mask() == 341);
        server("Return to Overworld before respawn", p -> p.teleportTo(p.server.getLevel(Level.OVERWORLD), 0.5, -59, 4.5, 0, 0));
        step("Overworld client state synchronized", () -> { }, () -> MC.level.dimension() == Level.OVERWORLD && mask() == 341);
        server("Kill test player", ServerPlayer::kill);
        step("Respawn retains and synchronizes locks", () -> MC.player.respawn(), () -> MC.player != null && MC.player.isAlive() && mask() == 341);
        verify("Respawn server mask retained", p -> HotbarLockService.mask(p) == 341);
        step("Save and disconnect test world", () -> { MC.level.disconnect(); MC.clearLevel(new TitleScreen()); }, () -> MC.player == null && MC.getSingleplayerServer() == null);
        step("Reload saved world and verify login synchronization", () -> MC.createWorldOpenFlows().loadLevel(new TitleScreen(), WORLD),
                () -> MC.player != null && MC.screen == null && mask() == 341);
        verify("Reload restored persisted server mask", p -> HotbarLockService.mask(p) == 341);
        screenshot("reloaded-hud.png");
    }

    private static void flush(ServerPlayer p) {
        p.getInventory().selected = 0;
        p.inventoryMenu.broadcastChanges();
        p.connection.send(new ClientboundSetCarriedItemPacket(0));
        HotbarLockerForgeNetwork.sync(p);
    }

    private static void server(String name, Consumer<ServerPlayer> action) {
        step(name, () -> {
            serverWork = new CompletableFuture<>();
            var server = MC.getSingleplayerServer();
            var uuid = MC.player.getUUID();
            server.execute(() -> {
                try { action.accept(server.getPlayerList().getPlayer(uuid)); serverWork.complete(null); }
                catch (Throwable e) { serverWork.completeExceptionally(e); }
            });
        }, () -> { if (!serverWork.isDone()) return false; serverWork.join(); return true; });
    }

    private static void verify(String name, Predicate<ServerPlayer> assertion) {
        server(name, p -> check(assertion.test(p), name));
    }

    private static void pick() {
        try {
            MC.player.getInventory().selected = 0;
            MC.hitResult = new BlockHitResult(Vec3.atCenterOf(TARGET), Direction.UP, TARGET, false);
            var method = Minecraft.class.getDeclaredMethod("pickBlock");
            method.setAccessible(true);
            method.invoke(MC);
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }

    private static String overlay() {
        try {
            var field = Gui.class.getDeclaredField("overlayMessageString");
            field.setAccessible(true);
            Component component = (Component) field.get(MC.gui);
            return component == null ? "" : component.getString();
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }

    private static int mask() { return MC.player == null ? -1 : HotbarLockService.mask(MC.player); }
    private static boolean hasSound(float pitch) { return SOUNDS.stream().anyMatch(value -> Math.abs(value - pitch) < 0.02f); }
    private static void screenshot(String name) {
        step("Prepare HUD capture " + name, () -> { MC.setScreen(null); MC.getToasts().clear(); }, () -> true);
        step("Capture " + name, () -> Screenshot.grab(MC.gameDirectory, name, MC.getMainRenderTarget(), ignored -> { }), () -> true);
    }
    private static void step(String name, Runnable start, BooleanSupplier done) { STEPS.add(new Step(name, start, done)); }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private static void finish(Throwable failure) {
        finished = true;
        String text = (failure == null ? "PASS\n" : "FAIL: " + (current == null ? "startup" : current.name) + "\n" + failure + "\n") + String.join("\n", PASSED);
        if (failure != null) LoggerFactory.getLogger("hotbarlocker/validation").error(text, failure);
        try { Files.writeString(Path.of(MC.gameDirectory.toString(), "result.txt"), text); }
        catch (Exception e) { throw new RuntimeException(e); }
        MC.stop();
    }
    private record Step(String name, Runnable start, BooleanSupplier done) { }
}
