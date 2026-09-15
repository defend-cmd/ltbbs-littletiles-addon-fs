package com.larisa.ltbbs.mixin.client;

import com.larisa.ltbbs.ILittleTilesBlockForm;
import com.larisa.ltbbs.LittleTilesUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.renderers.BlockFormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.joml.Vectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mchorse.bbs_mod.forms.renderers.BlockFormRenderer")
public abstract class BlockFormRendererMixin {
    @Unique
    private String ltbbs$lastData;
    @Unique
    private ItemStack ltbbs$cached = ItemStack.EMPTY;
    @Unique
    private float[] ltbbs$structureMin = new float[] {0.0F, 0.0F, 0.0F};

    @SuppressWarnings("rawtypes")
    private BlockForm ltbbs$form() {
        Form form = (Form) ((FormRenderer) (Object) this).getForm();
        return form instanceof BlockForm ? (BlockForm) form : null;
    }

    private ItemStack ltbbs$stack() {
        BlockForm form = this.ltbbs$form();
        if (!(form instanceof ILittleTilesBlockForm)) {
            return null;
        }

        String data = ((ILittleTilesBlockForm) form).ltbbs$getLittleTiles().get();
        if (data == null || data.isEmpty()) {
            return null;
        }

        if (!data.equals(this.ltbbs$lastData)) {
            this.ltbbs$lastData = data;
            this.ltbbs$cached = LittleTilesUtil.decode(data);
            this.ltbbs$structureMin = LittleTilesUtil.getStructureMin(this.ltbbs$cached);
        }

        return this.ltbbs$cached == null || this.ltbbs$cached.isEmpty() ? null : this.ltbbs$cached;
    }

    @Inject(method = "render3D", at = @At("HEAD"), cancellable = true)
    private void ltbbs$render3D(FormRenderingContext context, CallbackInfo ci) {
        if (context.isPicking()) {
            return;
        }

        ItemStack stack = this.ltbbs$stack();
        BlockForm form = this.ltbbs$form();
        if (stack == null || form == null) {
            return;
        }

        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        int light = context.light;

        context.stack.push();
        context.stack.translate(-1.0F - this.ltbbs$structureMin[0], -0.5F - this.ltbbs$structureMin[1],
                -0.5F - this.ltbbs$structureMin[2]);
        CustomVertexConsumerProvider.hijackVertexFormat(l -> RenderSystem.enableBlend());

        Color set = (Color) form.color.get();
        BlockFormRenderer.color.set(context.color);
        BlockFormRenderer.color.mul(set);
        consumers.setSubstitute(BBSRendering.getColorConsumer(BlockFormRenderer.color));

        MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.NONE,
                light, context.overlay, context.stack, consumers, context.entity.getWorld(), 0);

        consumers.draw();
        consumers.setSubstitute(null);
        CustomVertexConsumerProvider.clearRunnables();
        context.stack.pop();
        RenderSystem.enableDepthTest();

        ci.cancel();
    }

    @Inject(method = "renderInUI", at = @At("HEAD"), cancellable = true)
    private void ltbbs$renderInUI(UIContext context, int x1, int y1, int x2, int y2, CallbackInfo ci) {
        ItemStack stack = this.ltbbs$stack();
        BlockForm form = this.ltbbs$form();
        if (stack == null || form == null) {
            return;
        }

        context.batcher.getContext().draw();
        CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
        MatrixStack matrices = context.batcher.getContext().getMatrices();
        Matrix4f uiMatrix = ModelFormRenderer.getUIMatrix(context, x1, y1, x2, y2);

        matrices.push();
        MatrixStackUtils.multiply(matrices, uiMatrix);
        float scale = (Float) form.uiScale.get();
        matrices.scale(scale, scale, scale);
        matrices.translate(-1.0F - this.ltbbs$structureMin[0], -0.5F - this.ltbbs$structureMin[1],
                -0.5F - this.ltbbs$structureMin[2]);
        matrices.peek().getNormalMatrix().getScale(Vectors.EMPTY_3F);
        matrices.peek().getNormalMatrix().scale(1.0F / Vectors.EMPTY_3F.x, -1.0F / Vectors.EMPTY_3F.y, 1.0F / Vectors.EMPTY_3F.z);

        Color set = (Color) form.color.get();
        consumers.setSubstitute(BBSRendering.getColorConsumer(set));
        consumers.setUI(true);

        MinecraftClient.getInstance().getItemRenderer().renderItem(stack, ModelTransformationMode.NONE,
                240, OverlayTexture.DEFAULT_UV, matrices, consumers, MinecraftClient.getInstance().world, 0);

        consumers.draw();
        consumers.setUI(false);
        consumers.setSubstitute(null);
        matrices.pop();

        ci.cancel();
    }
}
