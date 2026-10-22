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
 * goes in, and the bundle lands by the vault in the same moment; until it is picked up it sits under
 * a solid black cube a little larger than itself.
 *
 * Drawn in the world, through the game's own {@code debugQuads} render type (26.2, read with javap:
 * POSITION_COLOR quads, depth-tested, no culling). Depth-tested on purpose: it is hidden behind walls
 * like any block, so it shows the player nothing they could not already see.
 *
 * Only once {@link BundleSpotter} is following the bundle entity itself - never over a guessed spot.
 */
public final class BundleCover {
    private BundleCover() {}

    /** How far the box stands off the bundle on every side, in blocks. */
    private static final double MARGIN = 0.18;
    /** Small bundles (a dropped item) still get a box that reads as one. */
    private static final double MIN_SIZE = 0.9;
    private static final int BLACK = 0xFF000000;

    public static void register() {
        LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> Safety.run("nucleus", () -> draw(ctx)));
    }

    private static void draw(LevelRenderContext ctx) {
        AABB target = BundleSpotter.cover();
        if (target == null) return;
        AABB b = grow(target);
        Vec3 cam = ctx.levelState().cameraRenderState.pos;
        PoseStack pose = ctx.poseStack();
        pose.pushPose();
        pose.translate(b.minX - cam.x, b.minY - cam.y, b.minZ - cam.z);
        float w = (float) (b.maxX - b.minX), h = (float) (b.maxY - b.minY), d = (float) (b.maxZ - b.minZ);
        ctx.submitNodeCollector().submitCustomGeometry(pose, RenderTypes.debugQuads(),
                (p, vc) -> cube(p, vc, w, h, d));
        pose.popPose();
    }

    /** The bundle's box grown by the margin, and to the minimum size about its own centre. */
    private static AABB grow(AABB a) {
        AABB b = a.inflate(MARGIN);
        double cx = (b.minX + b.maxX) / 2, cz = (b.minZ + b.maxZ) / 2;
        double hx = Math.max(MIN_SIZE, b.maxX - b.minX) / 2, hz = Math.max(MIN_SIZE, b.maxZ - b.minZ) / 2;
        double y0 = Math.max(a.minY - 0.02, b.minY), y1 = Math.max(b.maxY, y0 + MIN_SIZE);
        return new AABB(cx - hx, y0, cz - hz, cx + hx, y1, cz + hz);
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
