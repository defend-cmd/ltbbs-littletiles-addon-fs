# LittleTiles BBS FS Addon

An addon for BBS FS 2.6 that adds a LittleTiles structure catalog to the BBS dashboard and
makes BBS Block forms render LittleTiles structures instead of an empty block.

Normally a BBS Block form only stores a `BlockState`. When it renders, it spawns a fresh,
empty block entity, so picking a LittleTiles block shows nothing — the actual geometry lives
in the block entity's NBT, which the form never keeps. This addon captures the held
LittleTiles item and renders that, so the build actually shows up.

## Dashboard catalog

Open the BBS dashboard and select the **LittleTiles** tab in its bottom panel bar. The tab
contains a personal catalog stored in `config/ltbbs-littletiles-catalog.json`.

1. Copy a placed LittleTiles structure to an item (middle-click it in Creative mode).
2. Hold that item in your main hand.
3. In the LittleTiles tab, click **Create from held item**.
4. Select an entry to issue another copy with **Give selected structure**, or remove it with
   **Delete selected**.

The item is sent to the server for validation and is only issued if it is a LittleTiles item
with structure data. This works in singleplayer and multiplayer when the addon is installed
on both the client and server.

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

1. Select a LittleTiles structure from the dashboard catalog and use **Give selected structure**.
2. Create or edit a Block form in BBS FS.
3. Open the block picker and choose the issued LittleTiles item from your inventory.

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
