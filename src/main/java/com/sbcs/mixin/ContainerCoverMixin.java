package com.sbcs.mixin;

import com.sbcs.detect.dungeon.DungeonRolls;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Ossuary's cover over a reward chest's menu (or a run's page at Croesus): while {@link DungeonRolls#covers} says
 * so, the menu draws nothing of its own - no slots, no hover, no tooltip - and its mouse and keys go to the roll, never
 * to the menu, so nothing can be seen or clicked early. Escape still closes it. The roll itself is drawn after the
 * whole screen ({@link GuiOssuaryMixin}), over anything other mods add to it.
 *
 * Optional ({@code require = 0}): if 26.2 ever moves these, the Ossuary loses its cover, not the game. Signatures read
 * with javap from 26.2.
 */
@Mixin(AbstractContainerScreen.class)
public class ContainerCoverMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    private void sbcs$cover(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial, CallbackInfo ci) {
        com.sbcs.Safety.hook(3);
        if (DungeonRolls.covers(this)) ci.cancel();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void sbcs$click(MouseButtonEvent e, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (DungeonRolls.covers(this) && DungeonRolls.mousePressed(this, e.x(), e.y())) cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true, require = 0)
    private void sbcs$drag(MouseButtonEvent e, double dx, double dy, CallbackInfoReturnable<Boolean> cir) {
        if (DungeonRolls.mouseDragged(this, e.x(), e.y())) cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true, require = 0)
    private void sbcs$release(MouseButtonEvent e, CallbackInfoReturnable<Boolean> cir) {
        if (DungeonRolls.mouseReleased(this, e.x(), e.y())) cir.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true, require = 0)
    private void sbcs$scroll(double x, double y, double h, double v, CallbackInfoReturnable<Boolean> cir) {
        if (DungeonRolls.covers(this)) cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true, require = 0)
    private void sbcs$key(KeyEvent e, CallbackInfoReturnable<Boolean> cir) {
        if (DungeonRolls.keyPressed(this, e.key())) cir.setReturnValue(true);
    }
}
