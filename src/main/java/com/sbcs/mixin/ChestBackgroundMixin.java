package com.sbcs.mixin;

import com.sbcs.detect.dungeon.DungeonRolls;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The chest menu's own panel goes too while the Ossuary covers it (26.2 draws it in extractBackground). */
@Mixin(ContainerScreen.class)
public class ChestBackgroundMixin {

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void sbcs$cover(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        if (DungeonRolls.covers(this)) ci.cancel();
    }
}
