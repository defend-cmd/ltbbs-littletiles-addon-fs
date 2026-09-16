package com.kuboeb.ltbbs.mixin;

import com.kuboeb.ltbbs.ILittleTilesBlockForm;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.settings.values.core.ValueString;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mchorse.bbs_mod.forms.forms.BlockForm")
public class BlockFormMixin implements ILittleTilesBlockForm {
    @Unique
    private ValueString ltbbs$littleTiles;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void ltbbs$addLittleTilesValue(CallbackInfo ci) {
        this.ltbbs$littleTiles = new ValueString("littletiles_data", "");
        ((BlockForm) (Object) this).add(this.ltbbs$littleTiles);
    }

    @Override
    public ValueString ltbbs$getLittleTiles() {
        return this.ltbbs$littleTiles;
    }
}
