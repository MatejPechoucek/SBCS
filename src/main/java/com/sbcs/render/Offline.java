package com.sbcs.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

import java.awt.image.BufferedImage;

/**
 * Where the offline previews (tools/GuiPreview) step in for the GPU. In the game {@link #hooks} is always null and
 * nothing here runs; a preview sets it before drawing a real screen through a Java2D stand-in for the GUI, so the
 * three things that need the GPU or a live game - uploading a generated texture, uploading a {@link PixelLayer}, and
 * a head's downloaded skin - go to the preview instead.
 */
public final class Offline {
    private Offline() {}

    public interface Hooks {
        /** A generated texture, under the Identifier it would have been uploaded as. */
        void upload(Identifier id, BufferedImage image);

        /** A PixelLayer's frame: {@code w x h} cells of straight ARGB, each {@code unit} GUI px. */
        void layer(GuiGraphicsExtractor g, int[] argb, int w, int h, int unit);

        /** The bank name of a head's skin, or null. */
        String headTexture(HeadSkins set, String name);
    }

    /** Null in the game. */
    public static Hooks hooks;
}
