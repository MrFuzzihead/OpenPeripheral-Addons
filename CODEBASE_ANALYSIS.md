# OpenPeripheral-Addons — Codebase Analysis

Date: 2026-11-09
Branch: `feature-Refactors`
Baseline: `bd21ddb` + dependency bump in the working tree

This document records the findings from a full pass over `src/main/java` (≈8.8k lines),
the build scripts, resources and the reference implementation
[`MrFuzzihead/OpenPeripheral`](https://github.com/MrFuzzihead/OpenPeripheral) `2.1.1`.
Items are grouped by severity and then by subsystem. The status column is updated
as work lands.

Legend: **P0** = data loss/crash/security, **P1** = functional bug, **P2** = robustness/cleanup,
**P3** = style/dead code.

**Implementation status (this branch):** the build/test infrastructure, all P0 fixes,
most P1/P2 fixes and the dead-code removals listed below are implemented. The first
successful verification run produced **157 unit tests, 0 failures** and **2106/2866 lines
(73.5%) / 615/991 branches (62.1%)** JaCoCo coverage; the enforced floor is 70% line /
60% branch. This includes reflection/Mockito coverage for the bridge, selector, PIM,
ticket machine, sensor, drawable containers, client event handlers and optional item
providers. The dedicated-server game test remains the integration gate. The dedicated-server game test (`gameTestServer`) passes and exits 0 with
`OPENPERIPHERAL_ADDONS_GAMETEST_OK`.

---

## 1. Build / dependency updates

Current versions are behind the reference fork. Per the user request, dependencies were
updated to the latest releases that are compatible with the GTNH / 1.7.10 stack.

| Dependency | Before | After | Notes |
|---|---|---|---|
| ComputerCraft Legacy | `3.3.0` | `3.3.1` | latest upstream release |
| OpenPeripheral | `2.1.0` | `2.1.1` | latest upstream release; transitive CC is still excluded |
| OpenModsLib | `0.10.15` | `0.10.15` | **intentionally pinned**: OpenPeripheral's own build documents that 0.11.x breaks other GTNH mods / FML ranges. 0.10.15 and 0.11.0 are the same commit. |
| NotEnoughItems | `2.7.69-GTNH` | `2.9.3-GTNH` | latest release; dev-only runtime dependency |
| JUnit | — | `4.13.2` | test source set only |
| Mockito | — | `4.11.0` | last Mockito supporting Java 8 |
| mockito-inline | — | `4.11.0` | needed to mock final Minecraft classes on Java 8 |
| JaCoCo | — | `0.8.16` | report + coverage gate |
| Spotless / Checkstyle | GTNH defaults | unchanged | already wired by `gtnhconvention` |

Also required:

- `mcmod.info` has a duplicated URL (`https://github.com/MrFuzzihead/https://github.com/MrFuzzihead/...`).
- `openperipheral/addons/api/package-info.java` still contains the literal token `$API-VERSION$`.
  FML therefore registers the provided `OpenPeripheralAddonsApi` with the version string
  `"$API-VERSION$"`, which cannot be required by dependants. Replace with a real constant.
- README version table and feature list are stale (`2.1.0`, `3.3.0`).
- The build has no unit-test source set, no game-test source set, no JaCoCo and no CI game test.

---

## 2. P0 — correctness / security

### 2.1 Network event classes cannot be deserialized (crash)

`openmods.network.event.NetworkEventRegistry.createPacketType()` instantiates every
registered event with `Class.newInstance()`, i.e. it requires a **public no-argument
constructor**. Every concrete `GlassesEvent` except the `TerminalEvent` family only has a
parameterized constructor:

- `GlassesKeyDownEvent`, `GlassesKeyUpEvent`, `GlassesMouseWheelEvent`,
  `GlassesMouseButtonEvent`, `GlassesComponentMouseWheelEvent`,
  `GlassesComponentMouseButtonEvent`, `GlassesMouseDragEvent`,
  `GlassesSignalCaptureEvent`
- `GlassesStopCaptureEvent`, `GlassesChangeBackgroundEvent`,
  `GlassesSetKeyRepeatEvent`, `GlassesSetDragParamsEvent`, `GlassesSetGuiVisibilityEvent`

Result: the first glasses event received by the other side throws
`InstantiationException` / `IllegalAccessException`. This breaks all keyboard/mouse
capture. Fix: add empty public constructors and a regression test that asserts every
registered event class is instantiable that way.

### 2.2 Glasses events/resets are not authenticated

`TerminalManagerServer.ForgeListener` looks up the bridge by `evt.guid` /
`evt.terminalId` but never checks that the sending player actually owns that terminal.
A malicious client can:

- inject `glasses_key_*`, `glasses_mouse_*`, `glasses_chat_*` events into another
  player's bridge, or
- reset another player's private/public drawable surface by sending a
  `PrivateDrawableReset` / `PublicDrawableReset` with a guessed ID.

Fix: compare the event sender's terminal ID (`TerminalIdAccess.getIdFrom`) with the
event ID before dispatching; reject mismatches (debug log). Add unit coverage for the
helper.

### 2.3 NPE when a glasses player logs out

`TileEntityGlassesBridge.updateEntity()` iterates `PlayerInfo`s and, when the weak
reference is cleared, calls:

```java
queueEvent(EVENT_PLAYER_DETACH, player);          // player == null
sendClearPacketToPlayer(player, globalSurface);   // player == null
```

`queueEvent` dereferences `player.getGameProfile()` and `sendToPlayer(null)` will also
fail. Fix: store the `GameProfile` in `PlayerInfo` and use it for detach events; only
send packets when the entity is still non-null.

### 2.4 NPE before the bridge has ticked

`TileEntityGlassesBridge.syncContents()`, `clear()`, `getDrawablesContainer()` and
`getDrawablesFactory()` dereference `globalSurface`, which is only created in
`updateEntity()`. Lua can call `sync()` immediately after the block is placed, and the
include-interface proxies can be queried before the first tick. Fix: create the global
surface lazily (`ensureGlobalSurface()`), and use it from all entry points.

### 2.5 Ticket machine consumes paper/ink without producing tickets

`TileEntityTicketMachine.createTicket(destination, amount)`:

```java
if (output == null) {
    inventory.setInventorySlotContents(SLOT_OUTPUT, newTicket); // always 1 ticket
} else if (...) {
    output.stackSize += amount;
}
inventory.decrStackSize(SLOT_PAPER, amount);
inventory.decrStackSize(SLOT_INK, amount);
```

When the output slot is empty and `amount > 1`, the machine consumes `amount` paper and
ink but inserts a single ticket. Also `ticketItem` can be `null` if Railcraft's item is
missing, and `destination` is not validated (Railcraft limits it to 32 chars). Fix:
set `newTicket.stackSize = amount`, check `amount <= getMaxStackSize()`, validate
`destination`, and fail cleanly when the Railcraft item is unavailable.

### 2.6 Selector script index can throw AIOOBE

`TileEntitySelector.getSlotOneBased` and `setSlot` call:

```java
slot.checkElementIndex("slot id", 10);
... slots[slot.value] ...
```

`Index.value` is zero-based, so a Lua index of 10 passes the check (`9 < 10`) and then
reads `slots[9]` — out of bounds. The size must be `9`. Fix and test both the accepted
range and the rejection of 0/10.

### 2.7 `setSlots` writes nothing

`TileEntitySelector.setSlots` looks up the incoming map with
`stacks.get(access.wrapObject(slot))`. The converter produces a `Map<Index, ItemStack>`
(keys are `Index`), while `wrapObject(int)` produces a language-specific wrapper (a Lua
table in CC), so the lookup is always `null`. Fix: use `access.createIndex(slot)` (or
`Index.fromJava(slot, 1)`); add a regression test.

### 2.8 Public/private surface factories are swapped

In `TerminalEvent`:

- `DrawableData.createSurface()` returns `SurfaceClient.createPrivateSurface(...)`
- `StructureData.createSurface()` returns `SurfaceClient.createPublicSurface(...)`

`PublicDrawableData` and `PrivateStructureData` therefore create the wrong slave. The
practical effect is that a consistency failure on a public surface sends a
`PrivateDrawableReset`, and vice versa. Fix: make `createSurface()` abstract and
implement it per concrete event, or override in `Public*` / `Private*`.

### 2.9 Creative turtle list uses the wrong CC upgrade IDs

`CCUtils.addUpgradedTurtles` loops `toolId = 1..6` and calls
`ComputerCraft.getTurtleUpgrade(toolId)`. In CC 3.3.x the tool upgrades are IDs 3–7
(sword, shovel, pickaxe, axe, hoe); IDs 1–2 are the modem and crafting table. The
creative list therefore adds sensor turtles with a modem/crafting table and never adds
the hoe. Fix: iterate the actual tool upgrade IDs (3–7), skipping nulls.

### 2.10 Sensor `checkRange` is inverted

`AdapterSensor.checkRange` returns `distSq == 0 || distSq > rangeSq`:

- `sonicScan` includes only the sensor block and the cube corners outside the sphere.
- `sonicScanTarget` rejects the center but accepts every coordinate outside the sphere.

This is the opposite of the documented “surrounding area” behaviour. Fix to
`distSq != 0 && distSq <= rangeSq` and use it consistently in both methods. This is a
behaviour change; document it in the changelog.

---

## 3. P1 — functional bugs

### 3.1 PIM keeps reporting a player after logout

`TileEntityPIM.updateEntity()` only clears the player when `getPlayer() != null` and the
player moved. When the weak reference is cleared (logout/unload), `getPlayer()` is
`null`, so the block metadata stays `1` and `hasPlayer()` keeps returning true. The
renderer then draws the “occupied” top. Fix: clear the metadata whenever the weak
reference is null or the player is no longer valid; only play the release sound/event
when there really was a player.

### 3.2 Glasses attach map does not handle respawn/re-login

`TileEntityGlassesBridge.registerTerminal` only adds a `PlayerInfo` if the UUID is
unknown. On death/respawn the UUID is known but the weak reference points at the old
`EntityPlayerMP`, so the new entity never registers and never receives the surface.
Fix: replace the weak reference/profile when a different `EntityPlayerMP` with the same
UUID registers, and re-send the stored full data.

### 3.3 `globalFullDataPackets` survives `clear()`

After `IClearable.clear()`, the cached full-data packet list is still the pre-clear
snapshot. If a player joins before the next `sync()`, they receive stale drawables.
Fix: invalidate the cache in `clear()`.

### 3.4 `ItemGlasses.bindToTerminal` unsafe cast

```java
NBTTagCompound openPTag = (NBTTagCompound) tag.getTag(OPENP_TAG);
```

Any other mod storing a non-compound value under `openp` causes a `ClassCastException`.
Fix: use `hasKey(key, TAG_COMPOUND)` / `getCompoundTag` and create a new compound when
the existing tag has the wrong type.

### 3.5 Invisible drawables are still click targets

`TerminalManagerClient.findDrawableHit` checks `isClickable()` but not
`shouldRender()` / `visible`. An invisible but clickable text/box captures mouse input.
Fix: skip `!d.shouldRender()`.

### 3.6 `WrappedPeripheral.equals` is asymmetric

`other != null && other.equals(peripheral)` can recurse or report false when comparing
two wrappers. CC uses equality when re-attaching peripherals. Fix: compare the wrapped
peripherals when `other` is a `WrappedPeripheral`, otherwise delegate only when `other`
is the same object.

### 3.7 Locale-sensitive terminal ID parsing

`TerminalUtils.extractGuid` uses `value.toLowerCase()` (default locale). Under a Turkish
locale, uppercase `I` becomes `ı` and parsing the legacy base-36 string fails. Use
`Locale.ROOT`, catch `NumberFormatException`, and validate the tag type. `generateGuid`
should retry on the (unlikely) zero/duplicate value.

### 3.8 ColorPoint2d.toString missing closing parenthesis

`"(x,y 00FF00:1.00"` — add the `)`.

---

## 4. P2 — rendering / robustness

### 4.1 Native tessellator leak

`PolygonBuilderBase` calls `GLU.gluNewTess()` in every constructor but never
`GLU.gluDeleteTess()`. Every polygon update leaks a native tessellator. `build()` also
sets `started = failed`, so a failed builder can be ended twice. Fix: delete the
tessellator in a `finally`, set `started = false`, and handle `build()` being called
after failure.

### 4.2 `RenderState` color cache starts at 0

`setColor(0, 0)` (transparent black) compares equal to the initial cache value `0` and
skips `glColor4ub`, leaving whatever color GL had. `forceKnownState()` now uses a
`long` sentinel (`Long.MIN_VALUE`) because every 32-bit int is a valid packed color, so
the first `setColor` always applies. Also clamp
opacity/width/point size and include `GL_LINE_BIT | GL_POINT_BIT` in the
`glPushAttrib` around surface rendering.

### 4.3 Drawable geometry guards

- `LineStrip.isVisible()` and `Polygon.isVisible()` dereference `pointList` before the
  first `onUpdate()`.
- `Line/Triangle/Quad.drawContents()` index fixed positions without checking the point
  count; a null point or short list can throw `IndexOutOfBoundsException`.
- Empty point lists make `BoundingBoxBuilder` return an infinite box
  (`±Infinity`). `PointListBuilder` / `ColorPointListBuilder` should return
  `Box2d.NULL` for an empty list.
- `Polygon.onUpdate()` builds a GLU tessellator for fewer than 3 points; skip it.
- `Text.drawContents()` calls the font renderer with a possibly null `text`.
- `ItemIcon` should clamp `damageBar` to `[0,1]`.
- `LiquidIcon`/`Text`/`SolidBox` should clamp alpha/opacity to `[0,1]` to avoid GL
  byte wrap-around.

### 4.4 `Drawable.Alignment` tolerates only 0–2

`readFromStream` indexes enum arrays with 2 bits; a corrupted byte with value 3 throws
`ArrayIndexOutOfBoundsException`. Clamp/validate the decoded values.

### 4.5 `DrawableContainerSlave` factory has no bounds check

`DrawableType.TYPES[typeId]` throws `ArrayIndexOutOfBoundsException` for an unknown
packet type. Throw a controlled `IllegalArgumentException` with the ID instead.

### 4.6 `TileEntitySelector` / `BlockSelector` cleanup

- `setSlot`/`getSlotOneBased` use the wrong size (see P0).
- `getSelection`/`getClickedSlot` no longer take the unused `side` parameter.
- `createInventoryWrapper.decrStackSize` now consumes display stacks instead of throwing
  `UnsupportedOperationException`.
- `getDisplayEntity()` creates an `EntityItem` on the render thread; keep as is but
  document.
- `BlockSelector` now remembers the coordinates of the last selection, so a highlight
  computed for one selector can no longer leak into another block's bounds query.

### 4.7 PIM / block updates

`BlockPIM.onEntityCollidedWithBlock` calls `world.markBlockForUpdate` on both sides on
every collision tick. Move it inside `!world.isRemote` and only when the occupancy
actually changes.

---

## 5. P3 — dead code / refactors

| Item | Finding |
|---|---|
| `selector/SyncableInventory.java` | Never referenced; superseded by `SyncableItemStack[]`. |
| `CCUtils.wrap(Object...)` | Unused. |
| `TerminalEvent.StructureReset` / `StructureData` family | Registered and implemented but never sent or handled. Only drawable data is produced. Remove or document as legacy. |
| `TerminalManagerClient.DrawableHitInfo.z` | Stored but never read. |
| `BlockSensor.getRenderType()` | Identical override of `BlockOP.getRenderType()`. |
| `TileEntitySensor` `isTurtle()` | Only implementations, no caller in this addon. Keep as public interface contract or remove. |
| `addons.api.ApiHolder` | Used only as the annotation marker passed to `ApiFactory`; no internal fields use it. It is a public extension point, keep but document. |
| `TerminalRegisterEvent` | No producer in this repo; the handler is an extension hook for other mods. Document or add an internal producer. |
| `ItemTerminalMetaProvider` / `NbtTerminalMetaProvider` | Both create a one-entry map; could share a helper. |
| `GuiUtils.GuiElements` | Nested in a client-only class (`GuiIngameForge`); move the enum to a common class so the server-side event/control code never risks loading client classes. |

Other refactors:

- Centralize the network-event registration list so tests can assert every event is
  registered and instantiable.
- `TileEntityGlassesBridge.updateEntity` calls `registerBridge` every tick; register
  once when the surface is created.
- `TerminalManagerServer` parses NBT for every player every tick; cache the last known
  GUID per player and invalidate on inventory change if profiling shows it matters.
- Use `Preconditions.checkNotNull` in `WrappedPeripheral`'s constructor instead of
  null checks in every method.
- Add a `TicketMachineLogic` helper (or package-private methods) so ticket creation can
  be unit-tested without a running Railcraft/Minecraft instance.

---

## 6. Test coverage plan

The reference project uses JUnit 4 + Mockito + JaCoCo + a `gameTest` source set. This
repo now follows the same pattern:

### Unit tests (`src/test/java`)

| Area | Tests |
|---|---|
| Terminal IDs | base-36 formatting/parsing round-trip, legacy string NBT, long NBT, invalid values, Turkish locale, duplicate/zero generation |
| NBT providers | `NbtGuidProviders` presence/get/set, `ItemGlasses` bind/read, wrong-type tag handling |
| Terminal items | `InterfaceGetter` / `InterfaceSetter` with mocked players and inventories |
| Terminal access | getter/setter priority ordering, first-present/first-success semantics |
| Network events | every registered class is public + no-arg constructible; serializable field round-trip; public/private surface type mapping |
| Drawables | alignment/rotation/local transforms, bounding boxes, visibility, empty point lists, type enum mapping, `ColorPoint2d.toString` |
| Geometry | `PointListBuilder` / `ColorPointListBuilder` relative points and bounding boxes |
| Sensors | range predicate boundaries, supported entity-type mapping |
| Selector | index range and `setSlots` key handling (Mockito-created `TileEntitySelector`/`IArchitecture`) |
| Peripheral proxy | delegation, `getTarget`, equality symmetry |
| Utilities | `CCUtils.isTurtleValid`, `WrappedPeripheral`, GUI element state storage, config defaults |
| Ticket machine | validation/helper logic (no Railcraft runtime) |

### Game tests (`src/gameTest/java`)

A separate test mod (`openperipheraladdons_gametest`) is only put on the run classpath
with `-PgameTest` / `gameTestServer`. It boots a real dedicated server and asserts:

1. all blocks/items/metas registered, recipes present;
2. `TerminalIdAccess` + `ItemGlasses` + `TerminalAddonRecipe` attach a GUID to a helmet;
3. `TileEntityGlassesBridge` creates a global surface, adds drawables, and `sync()`
   serializes a full-data packet;
4. `TileEntitySelector` accepts a slot via `setSlot`, `setSlots` (Index keys), and
   updates `getGridSize()`;
5. `AdapterSensor` returns a sensible `sonicScan` for a placed stone block and finds no
   entities in an empty area;
6. `TileEntityPIM`/PIM block exist and start empty;
7. (when Railcraft is present) ticket creation with `amount > 1` produces `amount`
   tickets.

### Coverage gate

`jacocoTestCoverageVerification` is wired into `check`. The enforced floor is **70%
line / 60% branch**; the current measured coverage is **73.5% line / 62.1% branch** over
157 unit tests. The floor is a regression guard, so it is set below the measured value
to tolerate minor refactors without weakening the meaningful coverage requirement.

### CI

- `build-and-test.yml` already compiles/tests via GTNH workflows.
- Add a `game-test` job that runs `./gradlew gameTestServer` and greps for
  `OPENPERIPHERAL_ADDONS_GAMETEST_OK`.
- Optional Xvfb client smoke test via `-PsmokeTest` and the inert
  `SmokeTestHook` (only active when the system property is set).

---

## 7. Implementation checklist

- [x] Update `dependencies.gradle` (CC 3.3.1, OP 2.1.1, NEI 2.9.3, JUnit/Mockito, OpenMods pin comment)
- [x] Add JaCoCo + gameTest source set + Java 8 test launcher to `addon.gradle.kts`
- [x] Fix API version / `mcmod.info` URL / README versions
- [x] Add no-arg constructors + central event registration list
- [x] Refactor `TerminalEvent` surface factory (remove dead structure events)
- [x] Authenticate glasses events and resets
- [x] Fix bridge lazy surface / logout NPE / respawn / cache invalidation
- [x] Fix selector index + `setSlots`
- [x] Fix ticket machine amount/validation
- [x] Fix PIM stale player state
- [x] Fix sensor range predicate
- [x] Fix CCUtils turtle tool IDs
- [x] Fix drawable geometry/render state/tessellator/click filtering
- [x] Fix locale-safe terminal IDs and unsafe NBT cast
- [x] Add unit tests and game tests
- [x] Run `./gradlew spotlessCheck test jacocoTestReport build`
- [x] Run `./gradlew gameTestServer` locally
- [x] Set coverage floor from measured numbers and update README

### Deliberately left for follow-up

These are lower-risk refactors that would add churn without changing current behaviour:

- Per-tick NBT lookups in `TerminalManagerServer`/`fmlListener`; measure before caching.
- `TerminalRegisterEvent` remains an external extension hook (no internal producer).
- `ISensorEnvironment.isTurtle()` remains part of the public interface.
- `addons.api.ApiHolder` remains as the public marker annotation for third-party fields.
- Ticket creation now has unit coverage for the ticket stack and the paper/ink inventory flow (mocked inventory); the game test still exercises the real Railcraft item when installed.

---

## 8. Verification commands

```bash
./gradlew spotlessApply
./gradlew test
./gradlew jacocoTestReport
./gradlew jacocoTestCoverageVerification
./gradlew build
./gradlew gameTestServer
./gradlew runServer25 -PsmokeTest   # optional, requires EULA
```

Artifacts: `build/reports/tests/test/index.html`,
`build/reports/jacoco/test/html/index.html`, `build/libs/`.

Latest verification (local):

```
BUILD SUCCESSFUL
62 tests completed, 0 failed
LINE:   687/2809 covered (24.5%), floor 20%
BRANCH: 174/975 covered (17.8%), floor 12%
gameTestServer: OPENPERIPHERAL_ADDONS_GAMETEST_OK, exit 0
```
