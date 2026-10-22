package com.sbcs.mixin;

import com.sbcs.SbcsClient;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Slayer drops land on the floor as real items about 1.2 s after the boss dies, while Hellion or the Seraph's Glyph is
 * still building up - visible behind the dimmed cutscene and in the gameplay window (the review, A13). From the pre-kill
 * hide to the end of the cutscene no dropped item is drawn: its model, its shadow, any glow another mod puts on it,
 * and the copy in the gameplay window, which is the same world render. Only slayers drop to the floor (the designer,
 * 2026-10-11), so only theirs hide.
 */
@Mixin(EntityRenderDispatcher.class)
public class DroppedItemHideMixin {

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private <E extends Entity> void sbcs$hideDrops(E entity, Frustum frustum, double x, double y, double z,
                                                     CallbackInfoReturnable<Boolean> cir) {
        com.sbcs.Safety.hook(0);
        if (entity instanceof ItemEntity && SbcsClient.hideDroppedItems()) cir.setReturnValue(false);
    }
}
