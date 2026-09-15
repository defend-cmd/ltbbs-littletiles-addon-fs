package com.larisa.ltbbs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Base64;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class LittleTilesUtil {
    public static final String NAMESPACE = "littletiles";

    private LittleTilesUtil() {}

    public static boolean isLittleTilesBuild(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasNbt()) {
            return false;
        }

        Identifier id = Registries.ITEM.getId(stack.getItem());
        return NAMESPACE.equals(id.getNamespace());
    }

    public static String encode(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }

        try {
            NbtCompound tag = stack.writeNbt(new NbtCompound());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NbtIo.write(tag, new DataOutputStream(baos));
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    public static ItemStack decode(String data) {
        if (data == null || data.isEmpty()) {
            return ItemStack.EMPTY;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            NbtCompound tag = NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes)));
            return tag == null ? ItemStack.EMPTY : ItemStack.fromNbt(tag);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }
}
