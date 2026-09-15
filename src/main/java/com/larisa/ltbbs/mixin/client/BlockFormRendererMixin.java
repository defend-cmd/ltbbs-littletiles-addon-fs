package com.larisa.ltbbs.mixin.client;

import com.larisa.ltbbs.ILittleTilesBlockForm;
import com.larisa.ltbbs.LittleTilesUtil;
import com.larisa.ltbbs.client.LittleTilesMesh;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.util.math.MatrixStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger ltbbs$logger = LoggerFactory.getLogger("ltbbs");
    @Unique
    private String ltbbs$lastData;
    @Unique
    private BakedModel ltbbs$resourceModel;
    @Unique
    private LittleTilesMesh ltbbs$mesh;

    // Both render3D and renderInUI call this after applying BBS's block origin.
    // Keeping those wrappers also preserves BBS lighting, overlays and picking.
    @Inject(method = "renderBlock", at = @At("HEAD"), cancellable = true)
    private void ltbbs$renderStructure(MatrixStack matrices, VertexConsumerProvider consumers,
                                      int light, int overlay, boolean picking, CallbackInfo ci) {
        Object form = ((FormRenderer<?>) (Object) this).getForm();
        if (!(form instanceof ILittleTilesBlockForm littleTiles)) {
            return;
        }

        String data = littleTiles.ltbbs$getLittleTiles().get();
        if (data == null || data.isEmpty()) {
            return;
        }

        BakedModel resourceModel = MinecraftClient.getInstance().getBlockRenderManager()
                .getModel(Blocks.STONE.getDefaultState());
        if (!data.equals(this.ltbbs$lastData) || resourceModel != this.ltbbs$resourceModel) {
            this.ltbbs$lastData = data;
            this.ltbbs$resourceModel = resourceModel;
            this.ltbbs$mesh = null;
            try {
                this.ltbbs$mesh = LittleTilesMesh.bake(LittleTilesUtil.decode(data));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                ltbbs$logger.error("Could not build LittleTiles geometry for a BBS Block Form", error);
            }
        }

        if (this.ltbbs$mesh != null) {
            this.ltbbs$mesh.render(matrices, consumers, light, overlay);
            ci.cancel();
        }
    }
}
