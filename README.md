# LittleTiles BBS FS Addon

An addon for BBS FS 2.6 that makes BBS Block Forms render LittleTiles structures instead of
an empty block.

Normally a BBS Block form only stores a `BlockState`. When it renders, it spawns a fresh,
empty block entity, so picking a LittleTiles block shows nothing — the actual geometry lives
in the block entity's NBT, which the form never keeps. This addon captures the held
LittleTiles item and renders that, so the build actually shows up.

## BBS Block forms

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

1. Copy a placed LittleTiles structure to an item and put it in one of the nine hotbar slots.
2. Create or edit a **Block** form in BBS FS.
3. Click **Choose block**.
4. Click the LittleTiles structure in the picker hotbar at the bottom of the picker.

The addon preserves the complete item NBT on the Block Form and the structure renders in the
form preview and in the world. It also uses the structure's saved LittleTiles origin, so its
lowest corner is anchored to the BBS block instead of being offset below or beside it.
Choosing a regular block keeps BBS's normal block behaviour.

The form now renders the structure. Pick a regular block to go back to normal behaviour.

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

## Compatibility

Built against Minecraft 1.20.1, Fabric Loader, and BBS FS `2.6-1.20.1`. Requires LittleTiles
to be installed for the items to exist. In a Forge instance, BBS FS and this addon require a
Fabric compatibility layer such as Sinytra Connector.

## License

MIT.
