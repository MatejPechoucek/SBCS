package com.sbcs.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sbcs.Safety;
import com.sbcs.detect.nucleus.BundleSpotter;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The black box over the Crystal Loot Bundle (Matej, 2026-10-22: "covering the loot on the ground
 * so the user doesnt see it before" the reveal). The haul is printed the moment the fifth crystal
 * goes in, and the bundle lands by the vault in the same moment; until the Vault animation is done it
 * sits under a solid black box, drawn exactly as {@link BundleSpotter#cover()} gives it.
 *
 * Drawn in the world, through the game's own {@code debugQuads} render type (26.2, read with javap:
 * POSITION_COLOR quads, depth-tested, no culling). Depth-tested on purpose: it is hidden behind walls
 * like any block, so it shows the player nothing they could not already see.
 *
 * Always {@link BundleSpotter#BOX}, a fixed box by the vault (Matej, 2026-10-29), while one is waiting.
 */
public final class BundleCover {
    private BundleCover() {}

    private static final int BLACK = 0xFF000000;

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> Safety.run("nucleus", () -> draw(ctx)));
    }

    private static void draw(LevelRenderContext ctx) {
        if (com.sbcs.SbcsClient.config != null && !com.sbcs.SbcsClient.config.nucleusCover) return;
        AABB target = BundleSpotter.cover();
        if (target == null) return;
        AABB b = target;
        Vec3 cam = ctx.levelState().cameraRenderState.pos;
        PoseStack pose = ctx.poseStack();
        pose.pushPose();
        pose.translate(b.minX - cam.x, b.minY - cam.y, b.minZ - cam.z);
        float w = (float) (b.maxX - b.minX), h = (float) (b.maxY - b.minY), d = (float) (b.maxZ - b.minZ);
        ctx.submitNodeCollector().submitCustomGeometry(pose, RenderTypes.debugQuads(),
                (p, vc) -> cube(p, vc, w, h, d));
        pose.popPose();
    }

    private static void cube(PoseStack.Pose p, VertexConsumer vc, float w, float h, float d) {
        quad(p, vc, 0, 0, 0, w, 0, 0, w, 0, d, 0, 0, d);   // bottom
        quad(p, vc, 0, h, 0, 0, h, d, w, h, d, w, h, 0);   // top
        quad(p, vc, 0, 0, 0, 0, h, 0, w, h, 0, w, 0, 0);   // north (-Z)
        quad(p, vc, 0, 0, d, w, 0, d, w, h, d, 0, h, d);   // south (+Z)
        quad(p, vc, 0, 0, 0, 0, 0, d, 0, h, d, 0, h, 0);   // west (-X)
        quad(p, vc, w, 0, 0, w, h, 0, w, h, d, w, 0, d);   // east (+X)
    }

    private static void quad(PoseStack.Pose p, VertexConsumer vc,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3) {
        vc.addVertex(p, x0, y0, z0).setColor(BLACK);
        vc.addVertex(p, x1, y1, z1).setColor(BLACK);
        vc.addVertex(p, x2, y2, z2).setColor(BLACK);
        vc.addVertex(p, x3, y3, z3).setColor(BLACK);
    }
}
