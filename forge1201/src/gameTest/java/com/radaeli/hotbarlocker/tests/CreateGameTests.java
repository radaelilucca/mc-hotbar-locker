package com.radaeli.hotbarlocker.tests;

import com.radaeli.hotbarlocker.HotbarLockService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import java.util.List;
import java.util.function.Consumer;

/** Optional tests call the REAL installed Create packet and inspect its actual binding NBT. */
@GameTestHolder("hotbarlocker")
public final class CreateGameTests {
    @GameTestGenerator public static List<TestFunction> generate() {
        if (!Boolean.getBoolean("hotbarlocker.testCreate")) return List.of();
        return List.of(test("unlocked_equip", h -> equip(h, 0, 0, 0)),
                test("locked_equip_redirect", h -> equip(h, 1, 0, 1)),
                test("circular_equip", h -> equip(h, 1 << 8, 8, 0)),
                test("all_locked_reject", h -> equip(h, 511, 0, -1)),
                test("occupied_fallback", CreateGameTests::occupiedFallback),
                test("locked_unequip_reject", CreateGameTests::lockedUnequip),
                test("unlocked_unequip", CreateGameTests::unlockedUnequip),
                test("native_range_reject", CreateGameTests::nativeRangeReject));
    }

    private static TestFunction test(String name, Consumer<GameTestHelper> action) {
        return new TestFunction("create", "hotbarlocker.create_" + name, "hotbarlocker:empty", 100, 0, true, action);
    }

    private static ServerPlayer playerAndToolbox(GameTestHelper h) {
        var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", "brown_toolbox"));
        if (block == net.minecraft.world.level.block.Blocks.AIR) throw new AssertionError("Create Toolbox is not installed");
        h.setBlock(BlockPos.ZERO, block);
        // GameTestHelper's mock login has no Netty channel; Forge's custom packets require one.
        ServerPlayer p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                new GameProfile(java.util.UUID.randomUUID(), "toolbox-test"));
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        connection.setProtocol(ConnectionProtocol.PLAY);
        p.connection = new ServerGamePacketListenerImpl(p.server, connection, p);
        connection.setListener(p.connection);
        BlockPos pos = h.absolutePos(BlockPos.ZERO);
        p.teleportTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
        p.getPersistentData().put("CreateToolboxData", new CompoundTag());
        return p;
    }

    private static void equip(GameTestHelper h, int mask, int requested, int expected) {
        ServerPlayer p = playerAndToolbox(h);
        p.getInventory().setItem(requested, new ItemStack(Items.DIAMOND, 7));
        HotbarLockService.setMask(p, mask);
        packet(p, h.absolutePos(BlockPos.ZERO), requested);
        CompoundTag bindings = p.getPersistentData().getCompound("CreateToolboxData");
        if (expected < 0) check(bindings.isEmpty(), "All-locked equip created a binding");
        else check(bindings.contains(Integer.toString(expected)), "Create equip did not bind safe slot " + expected);
        if ((mask & (1 << requested)) != 0) {
            check(p.getInventory().getItem(requested).getCount() == 7, "Create changed locked stack");
            check(!bindings.contains(Integer.toString(requested)), "Create bound locked slot");
        }
        h.succeed();
    }

    private static void occupiedFallback(GameTestHelper h) {
        ServerPlayer p = playerAndToolbox(h);
        for (int i = 0; i < 9; i++) p.getInventory().setItem(i, new ItemStack(Items.DIAMOND, i + 1));
        HotbarLockService.setMask(p, 1);
        packet(p, h.absolutePos(BlockPos.ZERO), 0);
        check(p.getPersistentData().getCompound("CreateToolboxData").contains("1"), "Occupied fallback not bound");
        check(p.getInventory().getItem(0).getCount() == 1, "Occupied fallback changed locked source");
        h.succeed();
    }

    private static void lockedUnequip(GameTestHelper h) {
        ServerPlayer p = playerAndToolbox(h);
        packet(p, h.absolutePos(BlockPos.ZERO), 0);
        HotbarLockService.setMask(p, 1);
        packet(p, null, 0);
        check(p.getPersistentData().getCompound("CreateToolboxData").contains("0"), "Locked unequip altered original binding");
        h.succeed();
    }

    private static void packet(ServerPlayer p, BlockPos toolbox, int hotbarSlot) {
        try {
            Class<?> type = Class.forName("com.simibubi.create.content.equipment.toolbox.ToolboxEquipPacket");
            Object packet = type.getConstructor(BlockPos.class, int.class, int.class).newInstance(toolbox, 0, hotbarSlot);
            var constructor = NetworkEvent.Context.class.getDeclaredConstructor(Connection.class, NetworkDirection.class, int.class);
            constructor.setAccessible(true);
            NetworkEvent.Context context = constructor.newInstance(p.connection.connection, NetworkDirection.PLAY_TO_SERVER, 0);
            type.getMethod("handle", NetworkEvent.Context.class).invoke(packet, context);
        } catch (ReflectiveOperationException e) { throw new RuntimeException("Real Create packet failed", e); }
    }

    private static void unlockedUnequip(GameTestHelper h) {
        ServerPlayer p = playerAndToolbox(h);
        packet(p, h.absolutePos(BlockPos.ZERO), 0);
        packet(p, null, 0);
        check(!p.getPersistentData().getCompound("CreateToolboxData").contains("0"), "Normal unequip was blocked");
        h.succeed();
    }

    private static void nativeRangeReject(GameTestHelper h) {
        ServerPlayer p = playerAndToolbox(h);
        p.teleportTo(p.getX() + 1000, p.getY(), p.getZ());
        HotbarLockService.setMask(p, 1);
        packet(p, h.absolutePos(BlockPos.ZERO), 0);
        check(p.getPersistentData().getCompound("CreateToolboxData").isEmpty(), "Out-of-range equip bypassed Create validation");
        check(p.getInventory().selected == 0, "Rejected native equip changed selection");
        h.succeed();
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
