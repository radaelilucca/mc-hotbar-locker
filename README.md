<p align="center" style="text-align: center;"><img src="https://github.com/radaelilucca/mc-hotbar-locker/blob/main/docs/gallery/locked-slot.gif?raw=true" alt="Hotbar Locker"></p>

<p align="center" style="text-align: center;">Keep your important items where you put them</p>

<p align="center" style="background-color: #F4ECD0; text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-hotbar-locker/main/docs/gallery/section-titles/what_it_is.png" alt="WHAT IT IS?" width="267"></p>

Pick block and other equipment actions can replace the item in your selected hotbar slot. **Hotbar Locker** lets you lock individual slots so your tools and supplies stay put when items are placed automatically.

Choose a slot, press the key you assign, and a gold frame traces around the slot, followed by a small padlock, when it is locked. Locks persist with your player, including after reconnecting or respawning.

---

<p align="center" style="background-color: #F4ECD0; text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-hotbar-locker/main/docs/gallery/section-titles/what_can_it_do.png" alt="WHAT CAN IT DO?" width="375"></p>

<p align="center" style="text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-hotbar-locker/main/docs/gallery/locked-hud.png" alt="Locked hotbar slots marked with gold frames and padlocks" width="420"></p>

### Lock the slots you choose

**Protect selected hotbar slots from automatic replacement.** Lock only the items you want to keep in place; the other slots remain available as usual.

- A thin gold frame and a padlock in the corner mark each locked slot, without covering the item, its stack count, or its durability bar. When a locked slot is selected, the selection outline turns gold too. A message and a sound confirm when you lock or unlock it.
- You can still select, use, consume, damage, and move items manually.
- Clearing the Creative inventory preserves locked hotbar stacks.

### Pick block and Toolbox

**Automatic equipment respects your locks.** Pick block uses an unlocked slot when possible. If every hotbar slot is locked, it uses available main-inventory space; if there is none, it leaves protected items alone.

- When Create's Toolbox tries to equip an item into a locked slot, Hotbar Locker redirects it to an unlocked slot when possible.
- If every hotbar slot is locked, Toolbox equipment is rejected safely.
- Create is optional; Hotbar Locker works without it.

---

<p align="center" style="background-color: #F4ECD0; text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-hotbar-locker/main/docs/gallery/section-titles/how-do-i-use-it.png" alt="HOW DO I USE IT?" width="371"></p>

1. Open **Options → Controls → Key Binds** and assign **Toggle hotbar slot lock**.
2. Select the hotbar slot you want to protect.
3. Press your assigned key to lock or unlock that slot. Look for the gold frame, the padlock, and the confirmation message.

> **Heads up:** locking a slot only blocks automatic replacement. It does not stop you from moving or using the item yourself.

---

<p align="center" style="background-color: #F4ECD0; text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-hotbar-locker/main/docs/gallery/section-titles/requirements.png" alt="REQUIREMENTS" width="355"></p>

Install Hotbar Locker on both the client and server.

- **Minecraft 1.20.1:** Forge 47.4.0
- **Minecraft 1.21.1:** Fabric Loader 0.16.9, Forge 52.0.53, or NeoForge 21.1.236
- **Fabric API:** required on Fabric (the project is built with 0.110.0+1.21.1)
- **Create:** optional, for Create Toolbox compatibility

---

<p align="center" style="background-color: #F4ECD0; text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-hotbar-locker/main/docs/gallery/section-titles/found_a_bug.png" alt="FOUND A BUG?" width="317"></p>

[Open an issue on GitHub](https://github.com/radaelilucca/mc-hotbar-locker/issues). Include your Minecraft and loader versions, the action you were performing, and which slots were locked. A screenshot or short video can help show what happened.

---

<p align="center" style="background-color: #F4ECD0; text-align: center;"><img src="https://raw.githubusercontent.com/radaelilucca/mc-hotbar-locker/main/docs/gallery/section-titles/can_i_use.png" alt="CAN I USE THIS IN A MODPACK?" width="679"></p>

**Yes.** Hotbar Locker is licensed under the **MIT License**. See [LICENSE](https://github.com/radaelilucca/mc-hotbar-locker/blob/main/LICENSE).

---

*For development details, see the [development guide](https://github.com/radaelilucca/mc-hotbar-locker/blob/main/docs/development.md), [documentation index](https://github.com/radaelilucca/mc-hotbar-locker/blob/main/docs/README.md), and [CHANGELOG](https://github.com/radaelilucca/mc-hotbar-locker/blob/main/CHANGELOG.md).*
