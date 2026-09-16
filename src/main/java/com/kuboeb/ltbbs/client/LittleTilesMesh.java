package com.kuboeb.ltbbs.client;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.kuboeb.ltbbs.mixin.client.ItemRendererInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LittleTilesMesh {
    private static final Logger LOGGER = LoggerFactory.getLogger("ltbbs");
    private final ItemStack stack;
    private final List<BakedQuad> solid;
    private final List<BakedQuad> translucent;

    private LittleTilesMesh(ItemStack stack, List<BakedQuad> solid, List<BakedQuad> translucent) {
        this.stack = stack;
        this.solid = solid;
        this.translucent = translucent;
    }

    public static LittleTilesMesh bake(ItemStack stack) throws ReflectiveOperationException {
        if (stack.isEmpty()) {
            return null;
        }
        Class<?> placer = Class.forName("team.creative.littletiles.api.common.tool.ILittlePlacer");
        if (!placer.isInstance(stack.getItem())) {
            return null;
        }
        Object group = placer.getMethod("getTiles", ItemStack.class).invoke(stack.getItem(), stack);
        if (group == null) {
            return null;
        }

        Method getBoxes = group.getClass().getMethod("getRenderingBoxes", boolean.class);
        Class<?> facing = Class.forName("team.creative.creativecore.common.util.math.base.Facing");
        Class<?> compiler = Class.forName("team.creative.creativecore.client.render.model.CreativeBakedBoxModel");
        Method compile = compiler.getMethod("compileBoxes", List.class, facing, RenderLayer.class,
                Random.class, boolean.class, List.class);
        Object[] faces = (Object[]) facing.getField("VALUES").get(null);

        List<BakedQuad> solid = compile(getBoxes.invoke(group, false), faces, compile,
                TexturedRenderLayers.getEntityCutout());
        List<BakedQuad> translucent = compile(getBoxes.invoke(group, true), faces, compile,
                TexturedRenderLayers.getItemEntityTranslucentCull());
        LOGGER.info("Compiled LittleTiles block-space mesh: {} solid quads, {} translucent quads, bounds {}",
                solid.size(), translucent.size(), Arrays.toString(bounds(solid, translucent)));
        return new LittleTilesMesh(stack, solid, translucent);
    }

    private static float[] bounds(List<BakedQuad> solid, List<BakedQuad> translucent) {
        float[] bounds = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
        for (List<BakedQuad> layer : List.of(solid, translucent)) {
            for (BakedQuad quad : layer) {
                int[] vertices = quad.getVertexData();
                int stride = vertices.length / 4;
                for (int vertex = 0; vertex < 4; vertex++) {
                    for (int axis = 0; axis < 3; axis++) {
                        float coordinate = Float.intBitsToFloat(vertices[vertex * stride + axis]);
                        bounds[axis] = Math.min(bounds[axis], coordinate);
                        bounds[axis + 3] = Math.max(bounds[axis + 3], coordinate);
                    }
                }
            }
        }
        return bounds;
    }

    private static List<BakedQuad> compile(Object boxes, Object[] faces, Method compiler, RenderLayer layer)
            throws ReflectiveOperationException {
        List<BakedQuad> quads = new ArrayList<>();
        Random random = Random.create(42L);
        for (Object face : faces) {
            compiler.invoke(null, boxes, face, layer, random, true, quads);
        }
        return List.copyOf(quads);
    }

    public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay) {
        renderLayer(matrices, consumers, light, overlay, solid, TexturedRenderLayers.getEntityCutout());
        renderLayer(matrices, consumers, light, overlay, translucent,
                TexturedRenderLayers.getItemEntityTranslucentCull());
    }

    private void renderLayer(MatrixStack matrices, VertexConsumerProvider consumers, int light, int overlay,
                             List<BakedQuad> quads, RenderLayer layer) {
        if (quads.isEmpty()) {
            return;
        }
        VertexConsumer consumer = consumers.getBuffer(layer);
        ((ItemRendererInvoker) MinecraftClient.getInstance().getItemRenderer())
                .ltbbs$renderQuads(matrices, consumer, quads, stack, light, overlay);
    }
}
