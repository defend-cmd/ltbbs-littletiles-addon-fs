# LittleTiles BBS Addon

A small addon for [BBS mod](https://modrinth.com/mod/bbs-mod) (CML Edition) that makes the
"Block" form show a LittleTiles structure instead of a plain block.

Normally a BBS Block form only stores a `BlockState`. When it renders, it spawns a fresh,
empty block entity, so picking a LittleTiles block shows nothing — the actual geometry lives
in the block entity's NBT, which the form never keeps. This addon captures the held
LittleTiles item and renders that, so the build actually shows up.

## How it works

LittleTiles items carry the whole build in their NBT and ship their own baked model, so the
trick is just to hold onto that item and draw it.

- When you pick a block for a Block form through the inventory picker, the addon checks the
  stack. If it belongs to the `littletiles` namespace and has NBT, it's stored on the form;
  any normal block clears the slot and behaves as before.
- The item is kept as a Base64 string of its NBT, not as a BBS `ValueItemStack`. BBS's own
  serializer can't round-trip LittleTiles' nested byte/int arrays and crashes on copy/exit,
  so the data is treated as an opaque blob and BBS never has to parse it.
- At render time the stored item is decoded once (cached) and drawn with the vanilla item
  renderer, both in 3D and in the form preview.

All of this is done with `@Pseudo` mixins targeting BBS classes by name, so there's no
compile-time dependency on LittleTiles itself.

## Usage

1. Copy a LittleTiles build to an item (middle-click a placed structure to get it in hand).
2. Create or edit a Block form in BBS.
3. Open the block picker and choose that LittleTiles item from your inventory.

The form now renders the structure. Pick a regular block to go back to normal behaviour.

## Building

Needs JDK 17. BBS isn't redistributed here, so before building drop the BBS CML Edition jar
into `libs/`:

```
libs/bbs-cml-edition-1.10.3-1.20.1.jar
```

Then:

```
./gradlew build
```

The jar lands in `build/libs/`. Drop it into your mods folder next to BBS.

## Compatibility

Built against Minecraft 1.20.1 (Fabric loader, Yarn `1.20.1+build.10`). BBS runs on Forge
1.20.1 via Sinytra Connector, and this addon loads as a Fabric mod the same way. Requires
LittleTiles to be installed for the items to exist.

## License

MIT.
