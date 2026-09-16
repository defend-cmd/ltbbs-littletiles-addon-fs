package com.kuboeb.ltbbs.mixin.client;

import com.kuboeb.ltbbs.CurrentEdit;
import mchorse.bbs_mod.forms.forms.BlockForm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "mchorse.bbs_mod.ui.forms.editors.panels.UIBlockFormPanel")
public class UIBlockFormPanelMixin {
    @Inject(method = "startEdit(Lmchorse/bbs_mod/forms/forms/BlockForm;)V", at = @At("TAIL"))
    private void ltbbs$rememberForm(BlockForm form, CallbackInfo ci) {
        CurrentEdit.form = form;
    }
}
