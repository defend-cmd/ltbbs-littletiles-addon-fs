package com.larisa.ltbbs;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class LTBBS implements ModInitializer {
    public static final String MOD_ID = "ltbbs";
    public static final Identifier GIVE_STRUCTURE = new Identifier(MOD_ID, "give_structure");

    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(GIVE_STRUCTURE, (server, player, handler, buffer, responseSender) -> {
            ItemStack stack = buffer.readItemStack();

            server.execute(() -> {
                if (LittleTilesUtil.isLittleTilesBuild(stack)) {
                    player.giveItemStack(stack.copy());
                }
            });
        });
    }
}
