package com.larisa.ltbbs.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.larisa.ltbbs.LittleTilesUtil;
import net.minecraft.item.ItemStack;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class StructureCatalog {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<Entry>>() {}.getType();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("ltbbs-littletiles-catalog.json");

    private final List<Entry> entries = new ArrayList<>();

    public void load() {
        entries.clear();

        if (!Files.exists(FILE)) {
            return;
        }

        try {
            List<Entry> loaded = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8), LIST_TYPE);
            if (loaded != null) {
                entries.addAll(loaded);
            }
        } catch (Exception ignored) {
        }
    }

    public Entry add(ItemStack stack) {
        String data = LittleTilesUtil.encode(stack);
        if (data.isEmpty()) {
            return null;
        }

        Entry entry = new Entry();
        entry.name = "LittleTiles structure " + (entries.size() + 1);
        entry.data = data;
        entries.add(entry);
        save();

        return entry;
    }

    public void remove(int index) {
        if (index >= 0 && index < entries.size()) {
            entries.remove(index);
            save();
        }
    }

    public List<Entry> entries() {
        return entries;
    }

    public ItemStack getStack(int index) {
        if (index < 0 || index >= entries.size()) {
            return ItemStack.EMPTY;
        }

        return LittleTilesUtil.decode(entries.get(index).data);
    }

    private void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(entries, LIST_TYPE), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public static final class Entry {
        public String name;
        public String data;
    }
}
