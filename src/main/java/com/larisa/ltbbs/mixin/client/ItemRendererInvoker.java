package com.larisa.ltbbs.mixin.client;

import java.util.List;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemRenderer.class)
public interface ItemRendererInvoker {
    @Invoker("renderBakedItemQuads")
    void ltbbs$renderQuads(MatrixStack matrices, VertexConsumer consumer, List<BakedQuad> quads,
                           ItemStack stack, int light, int overlay);
}
