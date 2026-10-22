package com.sbcs.mixin;

import com.sbcs.detect.dungeon.DungeonRolls;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Where the Ossuary is drawn: after the screen, the toasts and whatever other mods add to the screen, so the roll lies
 * over the reward chest's menu and anything drawn on it (another mod's chest value). 26.2's
 * {@code Gui.extractRenderState(DeltaTracker, boolean, boolean)}, read with javap: the frame's extractor is handed to
 * {@code Hud.extractRenderState} first (taken here as it passes), then the screen, the saving indicator and toasts
 * (each behind a flag), the debug overlay, then {@code Hud.extractDeferredSubtitles} unconditionally - drawn just
 * before that.
 *
 * Optional: if 26.2's order ever moves, the Ossuary is simply not drawn - and then its cover never shows a roll, so
 * {@link DungeonRolls#covers} stops covering once it notices nothing is drawn.
 */
@Mixin(Gui.class)
public class GuiOssuaryMixin {

    @Unique
    private GuiGraphicsExtractor sbcs$frame;

    @ModifyArg(method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"),
            index = 0, require = 0)
    private GuiGraphicsExtractor sbcs$keepFrame(GuiGraphicsExtractor g) {
        sbcs$frame = g;
        return g;
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/DeltaTracker;ZZ)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Hud;extractDeferredSubtitles()V"), require = 0)
    private void sbcs$drawOssuary(DeltaTracker delta, boolean a, boolean b, CallbackInfo ci) {
        GuiGraphicsExtractor g = sbcs$frame;
        sbcs$frame = null;
        if (g == null) return;
        com.sbcs.Safety.hook(4);
        com.sbcs.Safety.run("dungeon", () -> DungeonRolls.drawOver(g));
    }
}
