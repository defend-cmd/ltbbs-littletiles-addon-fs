package com.larisa.ltbbs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

    /**
     * Returns the structure-space point that must be placed at the BBS form origin:
     * the horizontal center of the complete LittleTiles bounds and its lowest Y edge.
     */
    public static float[] getStructureAnchor(ItemStack stack) {
        float[] anchor = new float[] {0.5F, 0.0F, 0.5F};

        if (!isLittleTilesBuild(stack)) {
            return anchor;
        }

        try {
            Class<?> placerClass = Class.forName("team.creative.littletiles.api.common.tool.ILittlePlacer");
            if (!placerClass.isInstance(stack.getItem())) {
                return anchor;
            }

            Method getTiles = placerClass.getMethod("getTiles", ItemStack.class);
            Object group = getTiles.invoke(stack.getItem(), stack);
            if (group == null) {
                return anchor;
            }

            Object bounds = group.getClass().getMethod("getSurroundingBox").invoke(group);
            Object grid = group.getClass().getMethod("getGrid").invoke(group);
            if (bounds == null || grid == null) {
                return anchor;
            }

            Field pixelLength = grid.getClass().getField("pixelLength");
            double scale = pixelLength.getDouble(grid);

            int minX = bounds.getClass().getField("minX").getInt(bounds);
            int minY = bounds.getClass().getField("minY").getInt(bounds);
            int minZ = bounds.getClass().getField("minZ").getInt(bounds);
            int maxX = bounds.getClass().getField("maxX").getInt(bounds);
            int maxZ = bounds.getClass().getField("maxZ").getInt(bounds);

            anchor[0] = (float) ((minX + maxX) * scale * 0.5D);
            anchor[1] = (float) (minY * scale);
            anchor[2] = (float) ((minZ + maxZ) * scale * 0.5D);
        } catch (ReflectiveOperationException ignored) {
        }

        return anchor;
    }
}
