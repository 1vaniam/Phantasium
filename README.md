# Phantasm

Client-side optimization mod for Minecraft **1.21.11** (Fabric) targeting servers that use **ModelEngine**, **ItemsAdder**, and/or **Oraxen**.

## Systems

| ID   | What it does                                               | Target                                      |
|------|------------------------------------------------------------|---------------------------------------------|
| S1   | Per-frame frustum snapshot                                 | `WorldRenderEvents.START`                   |
| S2   | Frustum + back-angle cull                                  | `Entity.shouldRender` mixin                 |
| S3   | Async occlusion raycasting (1-2 frame stale)               | 2 background threads + `shouldRender` mixin |
| S4   | Skip collision on furniture/bones                          | `Entity.isPushable` mixin                   |
| S5   | Drop distant position packets (>48 blocks)                 | `ClientPlayNetworkHandler` mixin            |
| ME1  | ModelEngine bone group registry from SetPassengers         | `onSetPassengers` mixin                     |
| ME2  | Group-level cull (1 check per model, not per bone)         | `shouldRender` mixin (uses ME1 root box)    |
| ME3  | Instantly reject server-culled (invisible) bones           | `onEntityTrackerUpdate` mixin               |
| ME4  | Collision skip for ME bones                                | `isPushable` mixin                          |
| IO1  | IA/Oraxen furniture registry from AddEntity + SetEntityData| `onEntitySpawn` + `onEntityTrackerUpdate`   |
| IO2  | Drop packets for static furniture (no move for 3s+)        | `onEntityPosition` mixin                    |
| IO3  | ActionBar glyph dedup (IA/Oraxen PUA unicode)              | `InGameHud.setOverlayMessage` mixin         |
| IO4  | PUA glyph codepoint cache warm-up in text renderer         | `TextRenderer.drawLayer` mixin              |

## Setup

Requirements: Java 21, Fabric Loader ≥ 0.18.1, Fabric API 0.141.2+1.21.11.

```bash
./gradlew build
# Output: build/libs/phantasm-1.0.0.jar
```

Drop the jar in your `.minecraft/mods/` folder alongside Fabric API.

## Notes

- **VulkanMod compatible** — no injections into `WorldRenderer.renderEntities` (see S6 in design notes). All cancellation happens at `shouldRender` on the entity class.
- S3 occlusion results are intentionally 1-2 frames stale; this is imperceptible.
- IO2 static threshold is 60 ticks (~3 seconds). Movement resets the flag automatically.
- IO4 is a lookup cache warm-up, not a render skip — it speeds up subsequent frames by pre-populating the PUA codepoint set.
