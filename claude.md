# Phantasm — claude.md

Complete reference for the Phantasm mod. All current features, systems, files, and known gaps.

---

## Overview

**Phantasm** is a client-side Fabric optimization mod for Minecraft 1.21.11 targeting servers that use **ModelEngine**, **ItemsAdder**, **Oraxen**, and/or **Nexo**. It is invisible to servers — no server-side component, no mod detection. It activates optimizations selectively based on which plugins it detects via entity-based heuristics (and one channel handshake for ModelEngine).

---

## Build Info

| Field | Value |
|---|---|
| Minecraft | 1.21.11 |
| Yarn mappings | 1.21.11+build.3 |
| Fabric Loader | ≥ 0.15.0 |
| Fabric API | 0.141.4+1.21.11 |
| Java | 21 |
| Mod ID | `phantasm` |
| Mod name | Phantasium |
| Mod version | 1.0.0 |
| License | MIT |
| Environment | Client only |

```bash
./gradlew build
# Output: build/libs/phantasm-1.0.0.jar
```

---

## Plugin Detection

`ServerPluginDetector` uses entity-based detection for all four plugins. ModelEngine also sends a real channel handshake (`modelengine:bulk_data`) but all others rely solely on entity heuristics. State resets on disconnect so it never bleeds between servers.

| Plugin | Detection method |
|---|---|
| ModelEngine | `modelengine:bulk_data` channel packet (also: bone count > 0 fallback) |
| ItemsAdder | `FurnitureRegistry` — first ITEMS_ADDER-sourced confirmed furniture entity |
| Oraxen | `FurnitureRegistry` — first ORAXEN-sourced confirmed furniture entity |
| Nexo | `FurnitureRegistry` — first NEXO-sourced confirmed furniture entity |

`ServerPluginDetector` exposes both raw latch reads (no side effects, used in F3 overlay) and entity-fallback queries (`hasModelEngine()`, `hasOraxen()`, etc.) that trigger detection on first call. Use raw reads inside confirmation logic to avoid re-entrance.

---

## Config

`PhantasmConfig` — saved to `config/phantasm.json`. Loaded once on init via `PhantasmConfig.load()`. All fields are `volatile` for cross-thread visibility (render thread + ForkJoinPool read; main thread writes once at load time).

| Field | Default | Description |
|---|---|---|
| `enableFrustumCulling` | `true` | S2 frustum + back-angle cull |
| `enableOcclusionCulling` | `true` | S3 async occlusion raycast |
| `enablePacketDrops` | `true` | S5 distant packet drop + IO2 static furniture drop |
| `enableMipmapCap` | `true` | TX1 mipmap level cap |
| `enableLabelSkip` | `true` | TX2 nametag skip |
| `enableShadowSkip` | `true` | ME4 shadow skip |
| `enableHitboxSkip` | `true` | TX3 hitbox geometry skip |
| `enableLodThrottling` | `true` | LOD bone update throttle |
| `enableF3Overlay` | `true` | F3 diagnostic overlay |
| `enableParticleSuppression` | `true` | Particle packet drop for bones/furniture |
| `enableParallelSpriteLoad` | `true` | Parallel PNG decode on resource pack load |
| `closeDistanceBlocks` | `16.0` | Entities closer than this skip back-angle + occlusion cull. Snapped to preset: 8/16/32/64/128 |
| `lodDistanceBlocks` | `32.0` | Bone groups beyond this distance are LOD-throttled |
| `lodSkipTicks` | `3` | Ticks to skip per LOD-throttled bone group update cycle |
| `lodRenderDistanceBlocks` | `96.0` | Hard render cutoff — entities beyond this are culled |

---

## Feature Systems

### S — Spatial Culling

**S1 — Per-frame frustum snapshot** (`MixinWorldRenderer`)
Injects into `WorldRenderer.setupFrustum` at RETURN and stores the current `Frustum` in `ClientFrustumCache`. Called once per frame. All frustum checks across the mod read from this cache.

**S2 — Frustum + back-angle cull** (`MixinShouldRender`)
Injects into `EntityRenderer.shouldRender` at HEAD. For `ItemDisplayEntity` and `ArmorStandEntity` that are bones, furniture, or player armor, runs:
1. `frustum.isVisible(boundingBox)` — frustum cull
2. Hard LOD render distance cutoff (`lodRenderDistanceBlocks`)
3. Camera-forward dot product — culls entities > 130° behind camera (cos threshold `cos(130°)`)

Entities within `closeDistanceBlocks` skip back-angle and occlusion cull (frustum only). For ME bones, uses the **root pivot's** bounding box (group-level cull, see ME2).

**S3 — Async occlusion culling** (`AsyncOcclusionWorker`, `OcclusionCullCache`)
After S2 passes, submits a raycast job to a 2-thread background pool (`phantasm-occlusion`, priority NORM-1). Raycasts from camera to entity bounding box center using `RaycastContext.ShapeType.VISUAL`. Results written to `OcclusionCullCache` (concurrent hash map). Results are 1-2 frames stale — imperceptible in practice. Occluded entities are rejected in the next `shouldRender` call.

**S4 — Collision skip for bones and furniture** (`MixinIsPushable`)
Injects into `ArmorStandEntity.isPushable` and returns false for any entity tracked as a ME bone or IA/Oraxen/Nexo furniture. `DisplayEntity` already returns false natively.

**S5 — Distant packet drop** (`MixinNetworkHandlerPositionSync`)
Injects into `ClientPlayNetworkHandler.onEntityPosition` and `onEntityVelocityUpdate`. Drops packets for ME bones and furniture more than **48 blocks** from the player. Skips entity interpolation updates for entities too far away to matter visually.

---

### ME — ModelEngine

**ME1 — Bone group registry** (`MixinNetworkHandlerPassengers`, `ModelEngineRegistry`)
Intercepts `SetPassengers` packets. All passengers of a pivot entity are registered as its bones (`passengerId → rootId`) in `ModelEngineRegistry`. Also handles `PlayerEntity` vehicles separately — those route to `PlayerArmorRegistry` (see PA1). Registry is cleaned up on `RemoveEntities`.

**ME2 — Group-level cull** (`MixinShouldRender`)
When `shouldRender` is called for a bone, uses ME1 to look up the root pivot's ID, then culls using the **root's bounding box** instead of the bone's. Frustum/occlusion check runs once per model group, not once per bone (5–15 bones per mob).

**ME3 — Server-culled bone rejection** (`MixinNetworkHandlerTrackerUpdate`, `ModelEngineRegistry`)
Watches `SetEntityData` packets for the invisible flag (entity flags index 0, bit 0x20) on known bone entities. Marks them `invisibleBones` in `ModelEngineRegistry`. `shouldRender` rejects these immediately. `MixinBoneRenderStateEarlyExit` also cancels `render()` at HEAD for these bones before any state population runs.

**ME4 — Shadow skip** (`MixinShadowRenderer`)
Injects into `EntityRenderer.getAndUpdateRenderState` at RETURN. Sets `renderState.shadowRadius = 0f` for any bone entity. Prevents shadow geometry from being submitted. Eliminates 5–15 shadow draw calls per mob per frame.

**LOD — Bone update throttling** (`BulkDataHandler`, `LodThrottleRegistry`)
On `modelengine:bulk_data` packets, checks each bone group's root distance. If root is beyond `lodDistanceBlocks`, skips the DataTracker write for `lodSkipTicks` ticks. `viewRange` on render data is clamped to `lodRenderDistanceBlocks / 64.0` to cap ME's own view range setting.

---

### IO — ItemsAdder / Oraxen / Nexo (Furniture)

**IO1 — Furniture registry** (`MixinNetworkHandlerEntities`, `MixinNetworkHandlerTrackerUpdate`, `FurnitureRegistry`)
Two-stage detection:
- Stage 1 (`onEntitySpawn`): sees `ITEM_DISPLAY` or `ARMOR_STAND` → staged as pending with spawn Y and tick.
- Stage 2 (`onEntityTrackerUpdate`): sees custom model data (tracked data index 8) that is non-zero → confirmed as furniture.

Source is inferred from item namespace in spawn data:
- `ia:` prefix → `ITEMS_ADDER`
- `oraxen:` or `o:` → `ORAXEN`
- `nexo:` → `NEXO`
- anything else → `GENERIC`

False-positive prevention: reject if passenger-of-player (would be PA1), if Y changes > 0.1 since spawn, or if entity has a non-null custom name. Pending entities expire after `MAX_PENDING_TICKS` (200 ticks / ~10 seconds) via `evictStalePending()` called on `END_CLIENT_TICK`.

**IO2 — Static furniture packet drop** (`MixinNetworkHandlerPositionSync`, `FurnitureRegistry`)
Records `lastMoveTick` on each position packet. If no movement for **60+ ticks (~3 seconds)**, subsequent position packets are dropped. Movement resets the static flag automatically.

**IO3 — ActionBar deduplication** (`MixinInGameHud`)
Caches the last overlay message string. If `setOverlayMessage` is called with identical content to the last call, the update is cancelled. Oraxen/IA servers spam the action bar with PUA glyph HUDs every tick — eliminates redundant text layout recalculations.

**IO4 — PUA codepoint cache** (`MixinTextRenderer`, `PuaCodepointRegistry`)
Injects into `TextRenderer$Drawer.accept()`. Collects Private Use Area (U+E000–U+F8FF) codepoints encountered during text rendering into a static `IntOpenHashSet`. Warms up the glyph lookup path for subsequent renders of the same PUA characters.

---

### NX — Nexo-Specific

**NX2 — Block model cache** (`NexoBlockModelCache`, `MixinBlockModelShaper`)
Nexo maps custom blocks onto NoteBlock (800 states), Tripwire, and Chorus Flower. This cache stores `BlockState → BlockStateModel` for those three block types, bypassing repeated model lookups from `BlockModelShaper`. Invalidated on resource pack reload via `NexoBlockModelCacheInvalidator`.

**NX4 — Zero-width PUA glyph detection** (`MixinBitmapFont`)
Injects into `BitmapFont.getGlyph()`. For PUA codepoints with advance = 0f, registers them in `PuaCodepointRegistry.registerZeroWidth()`. Only active when Nexo is detected. Used by `MixinTextRenderer` to skip zero-width glyphs during layout.

---

### TX — Texture / Visual

**TX1 — Mipmap level cap** (`MixinMipmapSkip`)
Injects into `SpriteLoader.load()` via `@ModifyArgs` on `AtlasManager`. For plugin atlases, returns mipmap level 1 instead of the full chain. Only active when a plugin is detected. Reduces VRAM and atlas stitch time on resource pack load.

**TX2 — Parallel sprite loading** (`MixinMipmapSkip`)
Replaces vanilla's `Util.getMainWorkerExecutor()` with a dedicated `ForkJoinPool` sized to `(availableProcessors - 1)` (minimum 2) for plugin atlases. Cuts atlas stitch time by 50–70% on multi-core machines for packs with 500+ sprites.

Both TX1 and TX2 share the same `@ModifyArgs` injection and apply to:
- `textures/atlas/items.png` — primary IA/Oraxen/Nexo item sprites
- `textures/atlas/blocks.png` — Nexo block sprites, some Oraxen packs
- Any atlas whose namespace is `oraxen`, `itemsadder`, `ia`, `nexo`, or `modelengine`

A `SimpleSynchronousResourceReloadListener` registered in `PhantasmClient` (`phantasm:injection_guard`) warns via WARN-level log if the injection never fired after a resource reload, indicating an intermediary drift that has silently disabled both TX1 and TX2.

**TX2 — Nametag skip** (`MixinLabelSkip`)
Injects into `EntityRenderer.getAndUpdateRenderState` at RETURN. Sets `renderState.displayName = null` for ME bones and furniture. Renderer skips `renderLabelIfPresent` entirely when `displayName` is null. Eliminates 500+ label checks per frame on a populated server.

**TX3 — Hitbox geometry skip** (`MixinHitboxSkip`)
Injects into `EntityRenderer.appendHitboxes` at HEAD. Cancels for render states where `width == 0 && height == 0` (signature of ME bone `ItemDisplay` entities). Prevents debug hitbox geometry spam when F3+B is enabled.

**TX4 — ItemDisplay transform cache** (`DisplayTransformCache`, `MixinItemDisplayTransform`)
Caches `DisplayEntity.RenderState` (an immutable record) per entity ID. On each render, if the cached state matches, the transform re-computation is skipped. Cache entry is invalidated by `MixinNetworkHandlerTrackerUpdate` on every `SetEntityData` packet for that entity. Thread-safe via `ConcurrentHashMap` (network thread invalidates, render thread reads/writes).

---

### PA — Player Armor

**PA1 — Player armor registry** (`MixinNetworkHandlerPassengers`, `PlayerArmorRegistry`)
When a `SetPassengers` packet has a `PlayerEntity` as the vehicle (not a mob pivot), passengers are registered in `PlayerArmorRegistry` (`passengerEntityId → playerId`) rather than `ModelEngineRegistry`. This covers custom armor rendered via ArmorStand/ItemDisplay passengers riding the player.

**PA2 — Player armor cull** (`MixinShouldRender`)
For entities tagged as player armor, culls using the **player's bounding box** rather than the armor entity's. If the player is frustum-culled or occluded, the armor is culled too. Close-distance guard and async occlusion submission use the player ID as the check key.

---

### Particle Suppression

**MixinParticleSuppression** (`ClientPlayNetworkHandler.onParticle`)
Drops `ParticleS2CPacket` packets whose XYZ origin is within 0.5 blocks of any registered bone or furniture entity. Guard: `enableParticleSuppression` config toggle. Uses world entity iteration (main thread only, guarded by `mc.world != null`).

---

### Bone Render State Early Exit

**MixinBoneRenderStateEarlyExit** (`EntityRenderer.render`, priority 900)
Cancels `render()` at HEAD for server-culled bones before any state population or draw call runs. Reads entity ID from the `EntityRenderStateEntityId` duck interface injected onto `EntityRenderState` by `MixinEntityRenderState` and populated by `MixinEntityRendererUpdateState`.

---

## Registry / Cache Classes

| Class | Purpose |
|---|---|
| `ModelEngineRegistry` | `boneToRoot` map + `invisibleBones` set. Populated from SetPassengers + SetEntityData. Copy-on-write volatile map for render-thread safety. |
| `FurnitureRegistry` | Confirmed furniture set with source tag (Oraxen/IA/Nexo/Generic), pending queue, last-move tick for IO2. |
| `PlayerArmorRegistry` | `armorToPlayer` map + `playerToArmor` reverse index. Copy-on-write volatile map. |
| `ServerPluginDetector` | One-way boolean latches per plugin. Entity-based fallback detection in query methods. Reset on disconnect. |
| `LodThrottleRegistry` | `rootId → lastUpdateTick` map. Controls per-group bone update frequency beyond LOD distance. |
| `ClientFrustumCache` | Single volatile `Frustum` reference updated once per frame via S1. |
| `OcclusionCullCache` | Concurrent set of entity IDs confirmed occluded by background raycasts. |
| `AsyncOcclusionWorker` | 2-thread daemon pool for raycast jobs. Priority NORM-1. |
| `DisplayTransformCache` | `entityId → DisplayEntity.RenderState`. ConcurrentHashMap. Invalidated per entity on SetEntityData. |
| `NexoBlockModelCache` | `BlockState → BlockStateModel` for the three Nexo-hijacked block types. Invalidated on resource reload. |
| `PuaCodepointRegistry` | `IntOpenHashSet` of PUA codepoints (U+E000–U+F8FF). Used by IO4 and NX4. |

---

## Mixin List

| File | Target class | What it does |
|---|---|---|
| `MixinWorldRenderer` | `WorldRenderer` | S1 frustum snapshot |
| `MixinShouldRender` | `EntityRenderer` | S2 frustum+angle cull, ME2 group cull, ME3 server-cull reject, S3 occlusion, PA2 player armor cull |
| `MixinItemFrameCulling` | `EntityRenderer` | S2 frustum+angle cull for `ItemFrameEntity` and `GlowItemFrameEntity` |
| `MixinShadowRenderer` | `EntityRenderer` | ME4 shadow skip via `shadowRadius = 0` |
| `MixinLabelSkip` | `EntityRenderer` | TX2 nametag skip via `displayName = null` |
| `MixinHitboxSkip` | `EntityRenderer` | TX3 hitbox geometry skip |
| `MixinMipmapSkip` | `SpriteContents` | TX1 mipmap level cap for custom textures |
| `MixinIsPushable` | `ArmorStandEntity` | S4 collision skip |
| `MixinNetworkHandlerPassengers` | `ClientPlayNetworkHandler` | ME1 bone group registration, PA1 player armor registration |
| `MixinNetworkHandlerEntities` | `ClientPlayNetworkHandler` | IO1 stage 1 spawn detection + registry cleanup on RemoveEntities |
| `MixinNetworkHandlerPositionSync` | `ClientPlayNetworkHandler` | S5 distant packet drop, IO2 static furniture packet drop |
| `MixinNetworkHandlerTrackerUpdate` | `ClientPlayNetworkHandler` | ME3 invisible flag tracking, IO1 stage 2 furniture confirmation, TX4 transform cache invalidation |
| `MixinInGameHud` | `InGameHud` | IO3 ActionBar dedup |
| `MixinTextRenderer` | `TextRenderer$Drawer` | IO4 PUA codepoint cache warmup, NX4 zero-width glyph skip |
| `MixinF3Overlay` | `DebugHud` | F3 diagnostic overlay (plugins detected, bone/cull/furniture counts) |
| `MixinParticleSuppression` | `ClientPlayNetworkHandler` | Particle packet drop for bones/furniture proximity |
| `MixinBlockModelShaper` | `BlockModelShaper` | NX2 Nexo block model cache lookup/store |
| `MixinBitmapFont` | `BitmapFont` | NX4 zero-width PUA glyph detection and registration |
| `MixinBoneRenderStateEarlyExit` | `EntityRenderer` (priority 900) | Early cancel of `render()` for server-culled bones |
| `MixinItemDisplayTransform` | `DisplayEntity$ItemDisplayEntity` | TX4 transform cache read/write |
| `MixinEntityRenderState` | `EntityRenderState` | Duck interface injection — adds `phantasm_entityId` field |
| `MixinEntityRendererUpdateState` | `EntityRenderer` | Stamps entity ID onto render state via duck interface |

Duck interfaces:
- `EntityRenderStateEntityId` (interface at `dev.phantasm.duck`) — declares `phantasm_getEntityId()` / `phantasm_setEntityId()`
- `MixinEntityRenderState` (at `dev.phantasm.mixin.duck`) — implements the interface on `EntityRenderState`

---

## Disconnect / Dimension Change Cleanup

On `DISCONNECT`:
- `ServerPluginDetector.reset()` — clears all plugin flags
- `DisplayTransformCache.clear()`
- `ModelEngineRegistry.clear()`
- `FurnitureRegistry.clear()`
- `PlayerArmorRegistry.clear()`
- `LodThrottleRegistry.clear()`
- `OcclusionCullCache.clear()`

On `JOIN` (dimension change):
- Same set of registry clears (plugin detector is intentionally not reset — server is still the same)

---

## Compatibility Notes

- **VulkanMod compatible** — no injections into `WorldRenderer.renderEntities`. All render cancellation happens at `shouldRender` / `render()` / render-state level.
- **Iris/Sodium compatible** — no chunk render pipeline modifications.
- **Server-transparent** — pure client-side. Servers see a normal vanilla client.
- `defaultRequire = 1` in mixin config — any inject that fails to find its target crashes on load. Intentional to catch Yarn mapping drift on MC version updates.
- `MixinBoneRenderStateEarlyExit` uses `priority = 900` (lower than default 1000) so it runs before other mods' render injections, ensuring early cancellation is effective.

---

## F3 Overlay

Appended to the right side of the F3 debug screen when `enableF3Overlay = true`. Guard against VulkanMod's double `drawText` call via a marker-string check. Uses raw latch reads (not entity-fallback queries) to avoid render-thread side effects.

Lines shown:
```
[Phantasium] plugins: ME IA OX NX
[Phantasium] bones: <n>  culled: <n>
[Phantasium] furniture: <n>
[Phantasium] close-dist: 16b
```

---

## Known Gaps / Future Work

- **Tracker update drop for culled bones** — `SetEntityData` packets for `isServerCulled` bones are still processed (ME3 marks them invisible, but the packet parsing work still runs). Could be dropped earlier in `MixinNetworkHandlerTrackerUpdate`.
- **GC pressure from render state allocation** — 1.21.11 allocates a new `EntityRenderState` per entity per frame. For 10-bone mobs this is 10 short-lived allocations/frame. Pooling by entity ID would reduce GC pauses / microstutters.
- **Block entity frustum cull** — IA block entity renderers tick every frame regardless of visibility. `ClientFrustumCache` could be reused here.
- **Async texture decode on resource pack load** — TX2 parallelizes sprite decode/scale via a dedicated `ForkJoinPool`. The atlas stitching step itself remains single-threaded (inherent vanilla limitation — no clean mixin point without duplicating atlas logic).
- **TX1/TX2 atlas coverage** — now covers `items.png`, `blocks.png`, and any atlas under a known plugin namespace. If a plugin registers sprites into a completely custom atlas with an unrecognized namespace, it won't be covered.
- **NX2 block model cache coverage** — currently caches NoteBlock, Tripwire, and Chorus Flower only. If Nexo adds support for additional block types, `NexoBlockModelCache.isCandidateBlock()` needs updating.
- **`closeDistanceBlocks` preset snapping** — the config value is snapped to one of `{8, 16, 32, 64, 128}` on load. Non-preset values set manually in the JSON are silently snapped, which can surprise users who want fine-grained control.
