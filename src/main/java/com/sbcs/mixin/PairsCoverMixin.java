package com.sbcs.mixin;

import com.sbcs.detect.superpairs.PairsWatcher;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Hunt's black boxes in a Superpairs menu: a rare tile is drawn as its box instead of the item
 * ({@code extractSlot}, called per slot with the pose already at the menu's corner), and its tooltip is not drawn
 * ({@code extractTooltip}, which reads {@code hoveredSlot}). Nothing else of the menu changes: clicks, keys and every
 * other tile are the game's. Read only - see {@link PairsWatcher}.
 *
 * Optional ({@code require = 0}): if 26.2 ever moves these, the boxes are lost, not the game. Signatures read with javap
 * from 26.2: {@code extractSlot(GuiGraphicsExtractor, Slot, int, int)} and {@code extractTooltip(GuiGraphicsExtractor,
 * int, int)}, both protected; {@code hoveredSlot}, {@code leftPos}, {@code topPos}, {@code imageWidth},
 * {@code imageHeight} protected fields.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class PairsCoverMixin {

    @Shadow protected Slot hoveredSlot;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow @Final protected int imageWidth;
    @Shadow @Final protected int imageHeight;

    @Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true, require = 0)
    private void sbcs$pairsBox(GuiGraphicsExtractor g, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        com.sbcs.Safety.hook(5);
        boolean[] boxed = {false};
        com.sbcs.Safety.run("superpairs", () -> boxed[0] = PairsWatcher.coverSlot(this, g, slot));
        if (boxed[0]) ci.cancel();
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true, require = 0)
    private void sbcs$pairsTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY, CallbackInfo ci) {
        PairsWatcher.rect(this, leftPos, topPos, imageWidth, imageHeight);
        boolean[] hide = {false};
        if (hoveredSlot != null) com.sbcs.Safety.run("superpairs", () -> hide[0] = PairsWatcher.covered(this, hoveredSlot));
        if (hide[0]) ci.cancel();
    }
}
