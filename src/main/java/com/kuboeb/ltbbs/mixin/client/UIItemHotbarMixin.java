package com.kuboeb.ltbbs.mixin.client;

import com.kuboeb.ltbbs.CurrentEdit;
import com.kuboeb.ltbbs.ILittleTilesBlockForm;
import com.kuboeb.ltbbs.LittleTilesUtil;
import mchorse.bbs_mod.forms.forms.BlockForm;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "mchorse.bbs_mod.ui.forms.editors.panels.widgets.UIUnifiedPickOverlayPanel$UIItemHotbar")
public class UIItemHotbarMixin {
    @Inject(method = "subMouseClicked", at = @At("HEAD"))
    private void ltbbs$captureLittleTiles(UIContext context, CallbackInfoReturnable<Boolean> cir) {
        if (context.mouseButton != 0 || MinecraftClient.getInstance().player == null) {
            return;
        }

        UIElement element = (UIElement) (Object) this;
        int hotbarX = element.area.mx(196);
        int hotbarY = element.area.my(20);
        int slot = (context.mouseX - hotbarX) / 22;

        if (slot < 0 || slot >= 9 || context.mouseX < hotbarX + slot * 22 || context.mouseX >= hotbarX + slot * 22 + 20
                || context.mouseY < hotbarY || context.mouseY >= hotbarY + 20) {
            return;
        }

        ItemStack stack = MinecraftClient.getInstance().player.getInventory().getStack(slot);
        BlockForm form = CurrentEdit.form;
        if (!LittleTilesUtil.isLittleTilesBuild(stack) || !(form instanceof ILittleTilesBlockForm)) {
            return;
        }

        ValueString value = ((ILittleTilesBlockForm) form).ltbbs$getLittleTiles();
        value.set(LittleTilesUtil.encode(stack));
    }
}
