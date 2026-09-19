# Phase 2 — Create Toolbox checklist

Test against Create `6.0.10` for Minecraft `1.21.1` on every loader where Create is installed.

1. Start a client without Create. Confirm Hotbar Locker starts and no Create compatibility class is loaded.
2. Attach a Toolbox compartment to an unlocked selected hotbar slot. Confirm normal equip still works.
3. Lock the selected attached slot, leave a later unlocked slot empty, then pick an item supplied by the Toolbox. Confirm the Toolbox connects to the first empty unlocked slot in circular order, selects it and leaves the locked slot unchanged.
4. With no unlocked slot empty, confirm the first unlocked occupied slot remains the fallback destination.
5. Repeat across the `8 -> 0` hotbar wrap boundary.
6. Lock all nine slots and use Toolbox pick/equip. Confirm the action is safely rejected and no locked slot changes.
7. Lock an equipped Toolbox slot and invoke the Toolbox unequip action. Confirm Create does not return an item into the locked slot.
8. Reconnect and repeat after moving beyond Toolbox range. Check `latest.log` for `MixinApplyError`, `InvalidMixin`, `InjectionError`, or the Hotbar Locker Create bridge error.

The bridge intentionally targets Create's Toolbox packet only; other mods that mutate inventory directly remain Level C compatibility until they use `HotbarLockerApi` or receive a dedicated adapter.
