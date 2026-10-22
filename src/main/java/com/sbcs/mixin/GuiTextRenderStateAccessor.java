package com.sbcs.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The private fields of a text state, so {@link GuiRenderStateFadeMixin} can copy one faded. */
@Mixin(GuiTextRenderState.class)
public interface GuiTextRenderStateAccessor {
    @Accessor("font") Font sbcs$font();
    @Accessor("text") FormattedCharSequence sbcs$text();
    @Accessor("x") int sbcs$x();
    @Accessor("y") int sbcs$y();
    @Accessor("color") int sbcs$color();
    @Accessor("backgroundColor") int sbcs$backgroundColor();
    @Accessor("dropShadow") boolean sbcs$dropShadow();
    @Accessor("includeEmpty") boolean sbcs$includeEmpty();
}
