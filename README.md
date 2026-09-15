# LittleTiles BBS FS Addon

An addon for BBS FS 2.6 that makes BBS Block Forms render LittleTiles structures instead of
an empty block.

Normally a BBS Block form only stores a `BlockState`. When it renders, it spawns a fresh,
empty block entity, so picking a LittleTiles block shows nothing — the actual geometry lives
in the block entity's NBT, which the form never keeps. This addon captures a copied
LittleTiles item and builds a mesh from the geometry stored inside it.

## BBS Block forms

LittleTiles items carry the whole build in their NBT. Their inventory model is not suitable
for a Block Form: LittleTiles recenters it and shrinks large structures to fit one block.

- When you pick a block for a Block form through the inventory picker, the addon checks the
  stack. If it belongs to the `littletiles` namespace and has NBT, it's stored on the form.
- The item is kept as a Base64 string of its NBT, not as a BBS `ValueItemStack`. BBS's own
  serializer can't round-trip LittleTiles' nested byte/int arrays and crashes on copy/exit,
  so the data is treated as an opaque blob and BBS never has to parse it.
- At render time the addon reads `LittleGroup.getRenderingBoxes` and compiles those original
  boxes through CreativeCore. It bypasses `LittleModelItemTilesBig` and the item renderer's
  coordinate transforms. Solid and translucent geometry are compiled separately.
- The mesh is cached per renderer and rebuilt when its saved item or block models change.
  This alpha logs the compiled vertex bounds once per mesh under `ltbbs` for diagnostics.
- The draw hooks BBS's shared `renderBlock` method. BBS keeps control of the world/UI
  origin, user transforms, lighting, color, overlays and picking. No bounds-based centering,
  ground snapping, axis reversal or corrective translation is added by the addon.

BBS integration uses `@Pseudo` mixins. LittleTiles and CreativeCore APIs are accessed through
reflection, with no bundled copies of those mods. A Minecraft invoker draws the compiled
quads with vanilla tint handling without invoking the item model renderer.

1. Copy a placed LittleTiles structure to an item and put it in one of the nine hotbar slots.
2. Create or edit a **Block** form in BBS FS.
3. Click **Choose block**.
4. Click the LittleTiles structure in the picker hotbar at the bottom of the picker.

The addon preserves the complete item NBT on the Block Form. The original positions of the
individual tiles within their block are retained, including intentional gaps underneath or
off-center details. The addon does not apply the inventory model's scale reduction.

When checking placement, use a new model block with default transforms. Position/rotation/
scale adjustments saved on an existing model block or form are intentionally still applied.
The addon does not rewrite worlds or remove manual corrections from previous versions.

## Building

Needs JDK 17. BBS FS isn't redistributed here. Put its jar into `libs/`:

```
libs/bbs-2.6-1.20.1.jar
```

Then:

```
./gradlew build
```

Alternatively, keep the jar outside the repository and pass its path:

```
./gradlew build -Pbbs_fs_jar=/path/to/bbs-2.6-1.20.1.jar
```

The jar lands in `build/libs/`. Drop it into your mods folder next to BBS.

### Offline rendering checks

The optional verification task checks the installed mod bytecode, the remapped BBS hook,
the Minecraft quad-drawing invoker, and the block-origin translations. It also executes
LittleTiles' own grid conversion and render-box constructor in an isolated data-only harness
against a three-part structure fixture, negative coordinates, multiple grids and large bounds.
It does not launch Minecraft or verify GPU output, textures, or a full Connector startup.

```
./gradlew verifyRendering -Pbbs_fs_jar=/path/to/bbs-2.6-1.20.1.jar -Plittletiles_jar=/path/to/LittleTiles.jar -Pcreativecore_jar=/path/to/CreativeCore.jar
```

The current API checks target LittleTiles `1.6.0-pre161` and CreativeCore `2.12.39` for Forge
1.20.1. These dependencies are not redistributed. Verification libraries are not bundled in
the addon jar.

## Compatibility

Built against Minecraft 1.20.1, Fabric Loader, and BBS FS `2.6-1.20.1`. Requires LittleTiles
to be installed for the items to exist. In a Forge instance, BBS FS and this addon require a
Fabric compatibility layer such as Sinytra Connector.

## License

MIT.
