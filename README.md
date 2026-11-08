# OpenPeripheral-Addons

[![](https://jitpack.io/v/MrFuzzihead/OpenPeripheral-Addons.svg)](https://jitpack.io/#MrFuzzihead/OpenPeripheral-Addons)
[![Build status](https://github.com/MrFuzzihead/OpenPeripheral-Addons/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/MrFuzzihead/OpenPeripheral-Addons/actions/workflows/build-and-test.yml)
[![Latest release](https://img.shields.io/github/v/release/MrFuzzihead/OpenPeripheral-Addons?include_prereleases&sort=semver)](https://github.com/MrFuzzihead/OpenPeripheral-Addons/releases/latest)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.7.10-62a34a)](https://minecraft.wiki/w/Java_Edition_1.7.10)
[![Forge](https://img.shields.io/badge/Forge-10.13.4.1614-1e2b4f)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.7.10.html)
[![ComputerCraft](https://img.shields.io/badge/ComputerCraft-3.3.0-2ea44f)](https://github.com/MrFuzzihead/Computercraft-Legacy)
[![OpenPeripheral](https://img.shields.io/badge/OpenPeripheral-2.1.0-1e2b4f)](https://github.com/MrFuzzihead/OpenPeripheral)

An addon for OpenPeripheral, adding physical blocks and items to the game to enhance your ComputerCraft experience.

## What this is

This repository is a **port/continuation of [OpenPeripheral-Addons](https://github.com/OpenMods/OpenPeripheral-Addons)**,
the Open Mods add-on for OpenPeripheral, targeting Minecraft 1.7.10 and the
[GTNH](https://www.gtnewhorizons.com/) modpack. It is built against
[Computercraft-Legacy](https://github.com/MrFuzzihead/Computercraft-Legacy) 3.3.0 and
[OpenPeripheral](https://github.com/MrFuzzihead/OpenPeripheral) 2.1.0.

It adds physical blocks and items to the game — peripherals, sensors and terminal
accessories that can be placed, wired to computers and driven from Lua — rather
than being ComputerCraft-only software features. It is built **entirely on the
public APIs** of ComputerCraft and OpenPeripheral: no mixins, no core mod, no
bytecode manipulation, and no ComputerCraft reflection.

## Features

- **Terminal Glasses** — wearable glasses with a programmable terminal overlay, driven from Lua over the glasses API.
- **Wireless Keyboard** — a wireless keyboard item for interacting with terminals.
- **Terminal Glasses Bridge** — a block that bridges between terminals and glasses.
- **Player Inventory Manager (PIM)** — a block peripheral exposing the player's inventory to computers.
- **Sensor** — a block peripheral, plus a turtle upgrade variant, for detecting nearby entities and blocks.
- **Item Selector** — a block for selecting and filtering items in a GUI.
- **Peripheral Proxy** — (ComputerCraft only) a block that proxies another peripheral's methods through a single attachment point.
- **Ticket Machine** — (Railcraft only) a block peripheral for issuing and validating Railcraft tickets.
- **Turtle Upgrades** — Sensor and Narcissistic (inventory) turtle upgrades, registered when ComputerCraft is present.

Blocks and items from the ComputerCraft and Railcraft modules are only registered
when the respective mod is loaded, so the addon runs without them.

## Requirements

| Dependency       | Version                                  |
|------------------|------------------------------------------|
| Minecraft        | 1.7.10                                   |
| Minecraft Forge  | 10.13.4.1614                             |
| ComputerCraft    | [3.3.0, ) (`Computercraft-Legacy`)       |
| OpenPeripheral   | [2.1.0, ) (`MrFuzzihead/OpenPeripheral`) |
| OpenModsLib      | [0.10.15, )                              |
| lwjgl3ify        | Required by ComputerCraft 3.x (Java 17+) |
| Railcraft        | Optional (Ticket Machine)                |

## Compiling

Everything is handled by the `com.gtnewhorizons.gtnhconvention` plugin. Load the project in
IntelliJ and run `./gradlew build`, or use the wrapper from the command line:

```
./gradlew build              # compile, test, format check
./gradlew test               # tests only
./gradlew runClient          # dev client (aliased to runClient25; ComputerCraft 3.x requires lwjgl3ify)
./gradlew runServer          # dev server (aliased to runServer25; ComputerCraft 3.x requires lwjgl3ify)
./gradlew spotlessApply      # apply formatting
```

The Gradle daemon is pinned to Java 25 (see `.java-version`); the mod itself targets Java 8
bytecode (via Jabel). The output jar is placed in `build/libs/`. The plain
`runClient`/`runServer` tasks are aliased to the modern `*25` variants, because
ComputerCraft 3.x requires lwjgl3ify, which needs Java 17+ (see
`addon.gradle.kts`).

## Configuration

Options are generated on first launch and defined in
`src/main/java/openperipheral/addons/Config.java`, grouped by category
(`sensor`, `glasses`, `misc`). Most are `@OnLineModifiable`, so changes take
effect without a restart.

## Contributing

Bug reports and pull requests are welcome. If you find a bug that appears to relate to
ComputerCraft or OpenPeripheral themselves, please report it in the
[Computercraft-Legacy](https://github.com/MrFuzzihead/Computercraft-Legacy) or
[OpenPeripheral](https://github.com/MrFuzzihead/OpenPeripheral) repository rather
than here — OpenPeripheral-Addons is a thin layer on their public APIs and is
unlikely to be the cause.

## License

**MIT License** — see [LICENSE](LICENSE), inherited verbatim from upstream
[OpenPeripheral-Addons](https://github.com/OpenMods/OpenPeripheral-Addons) (Copyright (c) 2013 Open Mods).
ComputerCraft itself is distributed under the ComputerCraft Public License; see
[Computercraft-Legacy](https://github.com/MrFuzzihead/Computercraft-Legacy) for its terms.

## Credit

OpenPeripheral was created by [Open Mods](https://github.com/OpenMods). ComputerCraft was created by
[Daniel "dan200" Ratcliffe](https://github.com/dan200). This port is maintained by
[MrFuzzihead](https://github.com/MrFuzzihead) for Minecraft 1.7.10 / GTNH.
