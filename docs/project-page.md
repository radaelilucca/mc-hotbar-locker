# Hotbar Locker

Protect individual hotbar slots from automatic item
replacement. Keep tools, weapons, and essential items in place while using
pick block or equipment from other mods.

## How it works

Select a slot and use the configurable toggle key to lock or unlock it.

The lock affects automatic replacements only. You can still select, use,
consume, damage, and move items manually through your inventory as usual.

## Pick block and Toolbox

When an item would be placed into a locked slot, Hotbar Locker finds the next
unlocked empty slot and selects it. If the entire hotbar is locked, Creative
pick uses available main-inventory space. With no space, protected items stay untouched.

The same behavior is integrated with the **Toolbox** from
[Create](https://www.curseforge.com/minecraft/mc-mods/create): equipment
requested for a locked slot is redirected to an unlocked slot, preferring an
empty space.
If every hotbar slot is locked, Toolbox equip is safely rejected.

Locked slots have an inset gold frame. A short message and distinct lock/unlock
sounds confirm changes. Locks persist across saving, reconnecting and respawning.
Creative inventory clearing preserves locked hotbar stacks.

## Compatibility

- Minecraft 1.20.1: Forge
- Minecraft 1.21.1: Fabric, Forge, and NeoForge
- Create is optional.

## License

Hotbar Locker is distributed under the MIT License.
