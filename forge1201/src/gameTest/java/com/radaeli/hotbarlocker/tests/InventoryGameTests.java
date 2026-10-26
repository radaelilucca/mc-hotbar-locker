package com.radaeli.hotbarlocker.tests;

import com.radaeli.hotbarlocker.HotbarLockerApi;
import com.radaeli.hotbarlocker.HotbarLockService;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("hotbarlocker")
@PrefixGameTestTemplate(false)
public final class InventoryGameTests {
    @GameTest(template = "empty")
    public static void creativePickSkipsLockedSlot(GameTestHelper h) {
        Player p = h.makeMockPlayer();
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 7));
        HotbarLockService.setMask(p, 1);
        p.getInventory().setPickedItem(new ItemStack(Items.STONE));
        check(h, p.getInventory().selected == 1, "Pick should select unlocked slot 1");
        check(h, p.getInventory().getItem(0).getCount() == 7, "Locked stack changed");
        check(h, p.getInventory().getItem(1).is(Items.STONE), "Picked stack missing");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void circularPickWrapsAndPrefersEmpty(GameTestHelper h) {
        Player p = h.makeMockPlayer();
        for (int i = 0; i < 9; i++) p.getInventory().setItem(i, new ItemStack(Items.DIAMOND));
        p.getInventory().selected = 8;
        p.getInventory().setItem(1, ItemStack.EMPTY);
        HotbarLockService.setMask(p, 1 << 8);
        p.getInventory().setPickedItem(new ItemStack(Items.STONE));
        check(h, p.getInventory().selected == 1, "Empty unlocked slot must beat occupied slot across wrap");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void allLockedCreativePickUsesMainInventory(GameTestHelper h) {
        Player p = h.makeMockPlayer();
        HotbarLockService.setMask(p, 511);
        p.getInventory().setPickedItem(new ItemStack(Items.STONE));
        for (int i = 0; i < 9; i++) check(h, p.getInventory().getItem(i).isEmpty(), "Locked empty slot was written");
        check(h, p.getInventory().getItem(9).is(Items.STONE), "Missing main-inventory fallback");
        check(h, p.getInventory().selected == 0, "Main-inventory fallback changed selection");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullMainInventoryNeverEvictsLockedStack(GameTestHelper h) {
        Player p = h.makeMockPlayer();
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.DIAMOND, i + 1));
        HotbarLockService.setMask(p, 511);
        p.getInventory().setPickedItem(new ItemStack(Items.STONE));
        for (int i = 0; i < 36; i++) {
            check(h, p.getInventory().getItem(i).is(Items.DIAMOND) && p.getInventory().getItem(i).getCount() == i + 1,
                    "Full-inventory pick lost or duplicated stack " + i);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeReplacementPreservesDisplacedStack(GameTestHelper h) {
        Player p = h.makeMockPlayer();
        for (int i = 0; i < 9; i++) p.getInventory().setItem(i, new ItemStack(Items.DIAMOND, i + 1));
        HotbarLockService.setMask(p, 1);
        p.getInventory().setPickedItem(new ItemStack(Items.STONE));
        check(h, p.getInventory().selected == 1, "Replacement used wrong slot");
        check(h, p.getInventory().getItem(9).is(Items.DIAMOND) && p.getInventory().getItem(9).getCount() == 2,
                "Displaced stack not preserved in main inventory");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void occupiedReplacementWithNoMainSpaceAborts(GameTestHelper h) {
        Player p = h.makeMockPlayer();
        for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.DIAMOND));
        p.getInventory().setItem(0, ItemStack.EMPTY);
        HotbarLockService.setMask(p, 1);
        p.getInventory().setPickedItem(new ItemStack(Items.STONE));
        check(h, p.getInventory().getItem(0).isEmpty(), "Locked empty slot used as eviction destination");
        for (int i = 1; i < 36; i++) check(h, p.getInventory().getItem(i).is(Items.DIAMOND), "Unsafe occupied replacement");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void pickExistingLockedHotbarItemStillSelectsIt(GameTestHelper h) {
        Player p = h.makeMockPlayer();
        p.getInventory().setItem(8, new ItemStack(Items.STONE, 12));
        HotbarLockService.setMask(p, 511);
        p.getInventory().setPickedItem(new ItemStack(Items.STONE));
        check(h, p.getInventory().selected == 8 && p.getInventory().getItem(8).getCount() == 12, "Existing locked item not selectable");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void survivalPickSwapsOnlyUnlockedDestination(GameTestHelper h) {
        Player p = h.makeMockSurvivalPlayer();
        HotbarLockService.setMask(p, 1);
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND));
        p.getInventory().setItem(12, new ItemStack(Items.STONE, 32));
        p.getInventory().pickSlot(12);
        check(h, p.getInventory().selected == 1 && p.getInventory().getItem(1).getCount() == 32, "Survival swap missed safe slot");
        check(h, p.getInventory().getItem(12).isEmpty() && p.getInventory().getItem(0).is(Items.DIAMOND), "Swap corrupted source/lock");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void survivalPickWithAllLockedIsNoOp(GameTestHelper h) {
        Player p = h.makeMockSurvivalPlayer();
        HotbarLockService.setMask(p, 511);
        p.getInventory().setItem(12, new ItemStack(Items.STONE, 32));
        p.getInventory().pickSlot(12);
        check(h, p.getInventory().selected == 0 && p.getInventory().getItem(12).getCount() == 32, "Fully locked survival swap mutated inventory");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nbtRoundTripAndMissingTagReset(GameTestHelper h) {
        Player p = h.makeMockPlayer();
        HotbarLockService.setMask(p, 341);
        CompoundTag tag = new CompoundTag();
        p.addAdditionalSaveData(tag);
        check(h, tag.getInt(HotbarLockService.NBT_KEY) == 341, "Mixin did not save mask");
        HotbarLockService.setMask(p, 0);
        p.readAdditionalSaveData(tag);
        check(h, HotbarLockService.mask(p) == 341, "Mixin did not restore mask");
        tag.remove(HotbarLockService.NBT_KEY);
        p.readAdditionalSaveData(tag);
        check(h, HotbarLockService.mask(p) == 0, "Missing player NBT leaked previous state");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void manualMovementAndConsumptionStayAllowed(GameTestHelper h) {
        Player p = h.makeMockSurvivalPlayer();
        HotbarLockService.setMask(p, 511);
        p.getInventory().setItem(0, new ItemStack(Items.STONE, 4));
        p.getInventory().getItem(0).shrink(1);
        check(h, p.getInventory().getItem(0).getCount() == 3, "Normal consumption blocked");
        p.inventoryMenu.clicked(36, 0, net.minecraft.world.inventory.ClickType.PICKUP, p);
        check(h, p.getInventory().getItem(0).isEmpty() && p.inventoryMenu.getCarried().getCount() == 3, "Manual pickup blocked");
        p.inventoryMenu.clicked(37, 0, net.minecraft.world.inventory.ClickType.PICKUP, p);
        check(h, p.getInventory().getItem(1).getCount() == 3 && p.inventoryMenu.getCarried().isEmpty(), "Manual placement into locked slot blocked");
        p.getInventory().swapPaint(1);
        check(h, p.getInventory().selected == 8, "Scroll selection of locked slot blocked");
        ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        p.getInventory().setItem(8, tool);
        tool.hurtAndBreak(1, p, ignored -> { });
        check(h, tool.getDamageValue() == 1, "Normal tool damage blocked");
        p.getFoodData().setFoodLevel(10);
        ItemStack food = new ItemStack(Items.APPLE, 2);
        p.getInventory().setItem(0, food);
        food.finishUsingItem(h.getLevel(), p);
        check(h, food.getCount() == 1 && p.getFoodData().getFoodLevel() > 10, "Normal food consumption blocked");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void integrationApiFiltersLocksAndInvalidSlots(GameTestHelper h) {
        Player p = h.makeMockPlayer();
        HotbarLockService.setMask(p, 1 << 8);
        check(h, HotbarLockerApi.findReplacementSlot(p, 8).orElse(-1) == 0, "API wrap failed");
        HotbarLockService.setMask(p, 511);
        check(h, HotbarLockerApi.findReplacementSlot(p, 8).isEmpty(), "API offered locked destination");
        check(h, HotbarLockerApi.findReplacementSlot(p, -1).isEmpty() && HotbarLockerApi.findReplacementSlot(p, 9).isEmpty(), "API accepted invalid slot");
        h.succeed();
    }

    private static void check(GameTestHelper h, boolean condition, String message) {
        if (!condition) h.fail(message);
    }
}
