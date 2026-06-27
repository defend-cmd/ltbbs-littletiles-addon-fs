package com.larisa.ltbbs.mixin.client;

import com.larisa.ltbbs.CurrentEdit;
import com.larisa.ltbbs.ILittleTilesBlockForm;
import com.larisa.ltbbs.LittleTilesUtil;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.settings.values.core.ValueString;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIBlockStateEditor")
public class UIBlockStateEditorMixin {
    @Inject(method = "toBlockState", at = @At("HEAD"))
    private void ltbbs$captureLittleTiles(ItemStack stack, CallbackInfoReturnable<BlockState> cir) {
        BlockForm form = CurrentEdit.form;
        if (!(form instanceof ILittleTilesBlockForm)) {
            return;
        }

        ValueString slot = ((ILittleTilesBlockForm) form).ltbbs$getLittleTiles();
        slot.set(LittleTilesUtil.isLittleTilesBuild(stack) ? LittleTilesUtil.encode(stack) : "");
    }
}
