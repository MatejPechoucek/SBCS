package com.sbcs.gui;

import com.sbcs.anim.OssuaryTests;
import com.sbcs.detect.dungeon.ChestTier;
import com.sbcs.detect.dungeon.DungeonFloor;
import com.sbcs.detect.dungeon.DungeonRolls;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * "Try it" and {@code /sbcs test dungeon}: a made-up chest ({@link OssuaryTests}) rolled on a screen of its own, since
 * there is no reward chest's menu to cover. The roll is the real one - chain, seals, levers and all - and never
 * counted. Escape, or the roll's end, goes back to where it was opened from (the menu, for Try it).
 */
public class OssuaryTestScreen extends Screen {

    private final OssuaryRoll roll;
    private final Screen parent;

    public OssuaryTestScreen(DungeonFloor floor, ChestTier tier, String outcome, Screen parent) {
        super(Component.literal("SBCS - the Ossuary"));
        OssuaryTests.Test t = OssuaryTests.make(floor, tier, outcome, DungeonRolls.pool());
        this.roll = new OssuaryRoll(t.chest(), DungeonRolls.pool(), true, t.prices());
        this.parent = parent;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (roll.finished()) {
            onClose();
            return;
        }
        roll.draw(g, width, height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent e, boolean doubled) {
        return roll.mousePressed(e.x(), e.y());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
        roll.mouseDragged(e.x(), e.y());
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent e) {
        roll.mouseReleased(e.x(), e.y());
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (e.key() == 32 || e.key() == 259) {
            roll.skip();
            return true;
        }
        return super.keyPressed(e);
    }

    @Override
    public void onClose() {
        roll.end();
        Minecraft.getInstance().gui.setScreen(parent);
    }

    /** Opens a test over whatever screen is up (and back to it after). */
    public static void open(DungeonFloor floor, ChestTier tier, String outcome) {
        Minecraft mc = Minecraft.getInstance();
        Screen now = mc.gui.screen();
        mc.gui.setScreen(new OssuaryTestScreen(floor, tier, outcome, now instanceof SbcsMenuScreen ? now : null));
    }
}
