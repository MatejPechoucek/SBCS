package com.sbcs.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.bee.AdultBeeModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.monster.silverfish.SilverfishModel;
import net.minecraft.client.model.monster.spider.SpiderModel;
import net.minecraft.client.model.monster.warden.WardenModel;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.WardenRenderState;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The slayer bosses and their drops' creatures as the game builds them, the way LavaRigs does the lava creatures:
 * the real vanilla models built from their own layers, posed by their own {@code setupAnim} where that needs no
 * world, walked with {@link ModelPart#visit} for every posed polygon and its UVs.
 *
 * <ul>
 *   <li>The <b>Revenant Horror</b> is a zombie in diamond armour (the humanoid mesh, 64 x 64, and its armour shell at
 *       vanilla's 1.0 inflation, 64 x 32); posed by hand - HumanoidModel's own setupAnim reads held items.</li>
 *   <li>The <b>Tarantula Broodfather</b> is the spider, its eyes layer drawn over it.</li>
 *   <li>The <b>Warden</b> plays its own emerge and roar keyframes (AnimationState started at tick 0); its heart layer
 *       is the same body drawn again in warden_heart.png, which is clear except for the heart.</li>
 *   <li>The bee (the mosquito, the wasp), the silverfish (the maggot) and the player-head skull (the drops).</li>
 * </ul>
 * Output as LavaRigs: per polygon four corners of {x, y, z, u, v} in blocks (y up, +Z the model's front), [20] the
 * slot ({@link SlayerScene}'s), [21] flags (1 head, 2 a spider's abdomen, 4 an overlay layer), [22..24] the posed normal.
 */
public final class SlayerRigs implements SlayerScene.Rigs {

    private final ModelPart zombie, armour;
    private final SpiderModel spider;
    private final LivingEntityRenderState spiderState = new LivingEntityRenderState();
    private final WardenModel warden;
    private final WardenRenderState wardenState = new WardenRenderState();
    private final AdultBeeModel bee;
    private final BeeRenderState beeState = new BeeRenderState();
    private final SilverfishModel silverfish;
    private final EntityRenderState silverfishState = new EntityRenderState();
    private final ModelPart skull;

    public SlayerRigs() {
        zombie = LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0f), 64, 64).bakeRoot();
        armour = LayerDefinition.create(HumanoidModel.createMesh(new CubeDeformation(1f), 0f), 64, 32).bakeRoot();
        spider = new SpiderModel(SpiderModel.createSpiderBodyLayer().bakeRoot());
        warden = new WardenModel(WardenModel.createBodyLayer().bakeRoot());
        bee = new AdultBeeModel(AdultBeeModel.createBodyLayer().bakeRoot());
        silverfish = new SilverfishModel(SilverfishModel.createBodyLayer().bakeRoot());
        skull = SkullModel.createHumanoidHeadLayer().bakeRoot();
    }

    // ------------------------------------------------------------------ the Revenant

    @Override
    public List<float[]> zombie(float walk, float yaw, float armsUp, float tip, int lose) {
        pose(zombie, walk, armsUp);
        pose(armour, walk, armsUp);
        List<float[]> out = new ArrayList<>();
        PoseStack ps = living(yaw, tip, 90f);
        emit(zombie, ps, SlayerScene.SLOT_BOSS, false, lose, out);
        emit(armour, living(yaw, tip, 90f), SlayerScene.SLOT_ARMOUR, false, lose | SlayerScene.LOSE_HAT, out);
        return out;
    }

    @Override
    public List<float[]> zombiePart(int part, float yaw, float pitch, float roll) {
        resetParts(zombie);
        resetParts(armour);
        List<float[]> out = new ArrayList<>();
        int keep = ~part & (SlayerScene.LOSE_HEAD | SlayerScene.LOSE_RIGHT_ARM | SlayerScene.LOSE_LEFT_ARM | SlayerScene.LOSE_BODY
                | SlayerScene.LOSE_LEGS | SlayerScene.LOSE_HAT);
        // The part about its own middle: a head about its centre, an arm about its middle.
        float[] mid = switch (part) {
            case SlayerScene.LOSE_HEAD -> new float[]{0f, -4f, 0f};
            case SlayerScene.LOSE_RIGHT_ARM -> new float[]{-6f, 6f, 0f};
            case SlayerScene.LOSE_LEFT_ARM -> new float[]{6f, 6f, 0f};
            default -> new float[]{0f, 6f, 0f};
        };
        for (int layer = 0; layer < 2; layer++) {
            PoseStack ps = new PoseStack();
            ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
            ps.mulPose(Axis.XP.rotationDegrees(pitch));
            ps.mulPose(Axis.ZP.rotationDegrees(roll));
            ps.scale(-1f, -1f, 1f);
            ps.translate(-mid[0] / 16f, -mid[1] / 16f, -mid[2] / 16f);
            // A severed arm is drawn bare: the sleeve stays on the body, and the hand is what crawls.
            if (layer == 1 && part == SlayerScene.LOSE_RIGHT_ARM) continue;
            if (layer == 0) emit(zombie, ps, SlayerScene.SLOT_BOSS, false, keep, out);
            else emit(armour, ps, SlayerScene.SLOT_ARMOUR, false, keep | SlayerScene.LOSE_HAT, out);
        }
        return out;
    }

    /** The zombie's pose by hand: arms out in front (raised by {@code armsUp}), legs swinging with the walk. */
    private static void pose(ModelPart root, float walk, float armsUp) {
        resetParts(root);
        float swing = (float) Math.cos(walk * 0.6662f) * 0.9f;
        ModelPart ra = root.getChild("right_arm"), la = root.getChild("left_arm");
        ra.xRot = -1.45f - armsUp + 0.06f * swing;
        la.xRot = -1.45f - armsUp - 0.06f * swing;
        ra.zRot = 0.05f;
        la.zRot = -0.05f;
        root.getChild("right_leg").xRot = swing * 0.8f;
        root.getChild("left_leg").xRot = -swing * 0.8f;
    }

    // ------------------------------------------------------------------ the Tarantula

    @Override
    public List<float[]> spider(float walk, float yaw, float tip, float cave) {
        spiderState.walkAnimationPos = walk;
        spiderState.walkAnimationSpeed = walk == 0f ? 0f : 1f;
        spiderState.yRot = 0f;
        spiderState.xRot = 0f;
        spiderState.ageInTicks = walk * 3f;
        spider.setupAnim(spiderState);
        List<float[]> out = new ArrayList<>();
        int slot = cave > 0.5f ? SlayerScene.SLOT_CAVE_SPIDER : SlayerScene.SLOT_BOSS;
        // SpiderRenderer: a dead spider rolls onto its back (180 degrees, not 90).
        emit(spider.root(), living(yaw, tip, 180f), slot, false, 0, out);
        emit(spider.root(), living(yaw, tip, 180f), SlayerScene.SLOT_EYES, true, 0, out);
        return out;
    }

    // ------------------------------------------------------------------ the Warden

    @Override
    public List<float[]> warden(float yaw, float emergeMs, float roarMs, float tip, boolean heart) {
        wardenState.yRot = 0f;
        wardenState.xRot = 0f;
        wardenState.walkAnimationPos = 0f;
        wardenState.walkAnimationSpeed = 0f;
        wardenState.tendrilAnimation = roarMs >= 0f ? 0.8f : 0.2f;
        wardenState.heartAnimation = 1f;
        // Each keyframe animation as vanilla starts it: at tick 0, read at this many ticks in.
        float age;
        wardenState.emergeAnimationState.stop();
        wardenState.roarAnimationState.stop();
        if (emergeMs >= 0f) {
            wardenState.emergeAnimationState.start(0);
            age = emergeMs / 50f;
        } else if (roarMs >= 0f) {
            wardenState.roarAnimationState.start(0);
            age = roarMs / 50f;
        } else {
            age = 0f;
        }
        wardenState.ageInTicks = age;
        warden.setupAnim(wardenState);
        List<float[]> out = new ArrayList<>();
        emit(warden.root(), living(yaw, tip, 90f), SlayerScene.SLOT_WARDEN, false, 0, out);
        if (heart) emit(warden.root(), living(yaw, tip, 90f), SlayerScene.SLOT_HEART, true, 0, out);
        return out;
    }

    // ------------------------------------------------------------------ the bugs and the drop

    @Override
    public List<float[]> bee(float age, float yaw, float pitch) {
        beeState.ageInTicks = age;
        beeState.rollAmount = 0f;
        beeState.hasStinger = true;
        beeState.isOnGround = false;
        beeState.isAngry = false;
        beeState.hasNectar = false;
        beeState.yRot = 0f;
        beeState.xRot = 0f;
        beeState.walkAnimationPos = 0f;
        beeState.walkAnimationSpeed = 0f;
        bee.setupAnim(beeState);
        PoseStack ps = new PoseStack();
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        ps.mulPose(Axis.XP.rotationDegrees(pitch));
        ps.scale(-1f, -1f, 1f);
        ps.translate(0f, -1.501f, 0f);
        List<float[]> out = new ArrayList<>();
        emit(bee.root(), ps, SlayerScene.SLOT_BEE, false, 0, out);
        return out;
    }

    @Override
    public List<float[]> silverfish(float age, float yaw) {
        silverfishState.ageInTicks = age;
        silverfish.setupAnim(silverfishState);
        List<float[]> out = new ArrayList<>();
        emit(silverfish.root(), living(yaw, 0f, 90f), SlayerScene.SLOT_SILVERFISH, false, 0, out);
        return out;
    }

    @Override
    public List<float[]> skull(float yaw, float pitch, int slot) {
        PoseStack ps = new PoseStack();
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        ps.mulPose(Axis.XP.rotationDegrees(pitch));
        ps.scale(-1f, -1f, 1f);
        ps.translate(0f, 0.25f, 0f);
        List<float[]> out = new ArrayList<>();
        emit(skull, ps, slot, false, 0, out);
        return out;
    }

    // ------------------------------------------------------------------ helpers

    /** LivingEntityRenderer: turned to {@code yaw} (0 faces the camera), tipped over by the death rotation. */
    private static PoseStack living(float yaw, float tip, float flip) {
        PoseStack ps = new PoseStack();
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        if (tip > 0f) ps.mulPose(Axis.ZP.rotationDegrees(tip * flip));
        ps.scale(-1f, -1f, 1f);
        ps.translate(0f, -1.501f, 0f);
        return ps;
    }

    private static void resetParts(ModelPart root) {
        root.getAllParts().forEach(ModelPart::resetPose);
    }

    /** Whether a part's path is one of the parts in {@code lose} (SlayerScene.LOSE_*). */
    private static boolean lost(String path, int lose) {
        if (lose == 0) return false;
        if ((lose & SlayerScene.LOSE_HAT) != 0 && path.contains("hat")) return true;
        if ((lose & SlayerScene.LOSE_HEAD) != 0 && path.contains("head")) return true;
        if ((lose & SlayerScene.LOSE_HEAD) != 0 && path.contains("hat")) return true;
        if ((lose & SlayerScene.LOSE_RIGHT_ARM) != 0 && path.contains("right_arm")) return true;
        if ((lose & SlayerScene.LOSE_LEFT_ARM) != 0 && path.contains("left_arm")) return true;
        if ((lose & SlayerScene.LOSE_BODY) != 0 && path.contains("body")) return true;
        return (lose & SlayerScene.LOSE_LEGS) != 0 && path.contains("_leg");
    }

    private static void emit(ModelPart root, PoseStack ps, int slot, boolean overlay, int lose, List<float[]> out) {
        Vector3f v = new Vector3f();
        root.visit(ps, (pose, path, index, cube) -> {
            if (lost(path, lose)) return;
            Matrix4f m = pose.pose();
            for (ModelPart.Polygon poly : cube.polygons) {
                ModelPart.Vertex[] vs = poly.vertices();
                float[] q = new float[25];
                for (int k = 0; k < 4; k++) {
                    m.transformPosition(vs[k].worldX(), vs[k].worldY(), vs[k].worldZ(), v);
                    q[k * 5] = v.x;
                    q[k * 5 + 1] = v.y;
                    q[k * 5 + 2] = v.z;
                    q[k * 5 + 3] = vs[k].u();
                    q[k * 5 + 4] = vs[k].v();
                }
                q[20] = slot;
                q[21] = (path.contains("head") ? 1f : 0f) + (path.contains("body1") ? 2f : 0f) + (overlay ? 4f : 0f);
                pose.transformNormal(poly.normal(), v);
                q[22] = v.x;
                q[23] = v.y;
                q[24] = v.z;
                out.add(q);
            }
        });
    }
}
