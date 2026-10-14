# Changelog

## 0.3.0 - 2026-10-14

### Added

- Support Minecraft 1.20.1 on Forge, including hotbar locks, gold frames, lock sounds, Creative inventory protection, and Create Toolbox handling.

## 0.2.0 - 2026-09-28

### Added

- Preserve locked hotbar items when clearing the inventory in Creative mode.

### Fixed

- Fixed crashes when opening the Creative inventory on Fabric and NeoForge.
- Fixed locked-slot frames not appearing on Forge.

## 0.1.1 - 2026-09-20

### Fixed

- Fixed dedicated NeoForge servers failing to start when Hotbar Locker loaded.

## 0.1.0 - 2026-09-19

### Added

- Lock individual hotbar slots against automatic item replacement.
- Redirect vanilla pick actions to an available unlocked slot.
- Show lock frames, confirmations, and distinct lock/unlock sounds.
- Add optional Create Toolbox handling that respects locked slots.
- Support Fabric, Forge, and NeoForge for Minecraft 1.21.1.
