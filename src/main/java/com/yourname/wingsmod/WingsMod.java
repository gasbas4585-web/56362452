package com.yourname.wingsmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.SinglePartEntityModel;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class WingsMod implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "wingsmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModConfig config;

    @Override
    public void onInitialize() {
        LOGGER.info("Wings Mod initialized!");
        config = new ModConfig();
        config.load();
    }

    @Override
    public void onInitializeClient() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                registrationHelper.register(new CosmeticLayer(playerRenderer));
            }
        });
    }

    public static class ModConfig {
        public int wingType = 0;
        public boolean haloEnabled = false;
        public boolean hornsEnabled = false;
        public boolean wingAnimation = true;
        public boolean feathersEnabled = true;
        public int featherFrequency = 3;
        public float wingScale = 1.0F;
        public float haloScale = 1.0F;
        public float hornsScale = 1.0F;
        public float wingSpeed = 1.0F;

        private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
        private File file;

        public void load() {
            file = FabricLoader.getInstance().getConfigDir().resolve("wingsmod.json").toFile();
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                    if (loaded != null) {
                        wingType = loaded.wingType;
                        haloEnabled = loaded.haloEnabled;
                        hornsEnabled = loaded.hornsEnabled;
                        wingAnimation = loaded.wingAnimation;
                        feathersEnabled = loaded.feathersEnabled;
                        featherFrequency = loaded.featherFrequency;
                        wingScale = loaded.wingScale;
                        haloScale = loaded.haloScale;
                        hornsScale = loaded.hornsScale;
                        wingSpeed = loaded.wingSpeed;
                    }
                } catch (IOException | RuntimeException e) {
                    LOGGER.error("Failed to load config", e);
                }
            }
            save();
        }

        public void save() {
            if (file == null) {
                file = FabricLoader.getInstance().getConfigDir().resolve("wingsmod.json").toFile();
            }
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(this, writer);
            } catch (IOException e) {
                LOGGER.error("Failed to save config", e);
            }
        }
    }

    // The geometry below is kept from the supplied model; only the ModelPart
    // construction was updated to the 1.21.10 ModelData API.
    public static class AngelWingModel extends SinglePartEntityModel<LivingEntity> {
        private final ModelPart root;
        private final ModelPart[] wings = new ModelPart[4];

        public AngelWingModel() {
            ModelData data = new ModelData();
            ModelPartData modelRoot = data.getRoot();
            String[] names = {"topLeft", "bottomLeft", "topRight", "bottomRight"};
            float[] pivotsX = {-2.0F, -2.5F, 2.0F, 2.5F};
            float[] pivotsY = {1.0F, 2.5F, 1.0F, 2.5F};
            float[] rolls = {0.0F, 0.3F, 0.0F, -0.3F};

            for (int w = 0; w < 4; w++) {
                ModelPartData wing = modelRoot.addChild(names[w], ModelPartBuilder.create(), ModelTransform.pivot(pivotsX[w], pivotsY[w], 0.0F));
                wing.addChild("__base", ModelPartBuilder.create(), ModelTransform.NONE);
                for (int i = 0; i < 14; i++) {
                    float size = 0.8F + i * 0.12F;
                    float angle = -0.8F + i * 0.12F;
                    wing.addChild("feather" + i,
                            ModelPartBuilder.create().uv(0, 0).cuboid(-0.08F, -size, -0.04F, 0.16F, size * 1.6F, 0.08F),
                            ModelTransform.of(0.4F + i * 0.18F, -0.3F - i * 0.12F, 0.0F, angle, 0.15F + i * 0.04F, 0.0F));
                    if (i % 3 == 0) {
                        wing.addChild("small" + i,
                                ModelPartBuilder.create().uv(0, 2).cuboid(-0.06F, -size * 0.8F, -0.06F, 0.12F, size * 0.8F, 0.12F),
                                ModelTransform.of(0.3F + i * 0.15F, -0.5F - i * 0.1F, 0.0F, angle - 0.2F, 0.15F + i * 0.04F, 0.0F));
                    }
                }
            }
            root = TexturedModelData.of(data, 64, 64).createModel();
            for (int i = 0; i < 4; i++) {
                wings[i] = root.getChild(names[i]);
                wings[i].roll = rolls[i];
            }
        }

        @Override public ModelPart getPart() { return root; }

        @Override
        public void setAngles(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
            float flight = entity.getFallDistance() > 0.5F ? animationProgress : 0;
            float open = MathHelper.clamp(flight * 0.5F, 0, 1);
            for (int i = 0; i < 4; i++) {
                float offset = i * 0.15F;
                if (i < 2) {
                    wings[i].roll = -1.3F + open * 1.2F + MathHelper.sin(flight * 2.0F + offset) * 0.3F;
                    wings[i].yaw = -0.3F + open * 0.2F;
                } else {
                    wings[i].roll = 1.3F - open * 1.2F - MathHelper.sin(flight * 2.0F + offset) * 0.3F;
                    wings[i].yaw = 0.3F - open * 0.2F;
                }
                if (i == 1 || i == 3) wings[i].pitch = MathHelper.sin(flight * 1.5F + offset) * 0.15F;
                if (!WingsMod.config.wingAnimation) {
                    wings[i].roll /= 2.0F;
                    wings[i].pitch = 0;
                }
            }
        }
    }

    public static class DemonWingModel extends SinglePartEntityModel<LivingEntity> {
        private final ModelPart root, leftWing, rightWing;

        public DemonWingModel() {
            ModelData data = new ModelData();
            ModelPartData modelRoot = data.getRoot();
            ModelPartData left = modelRoot.addChild("leftWing", ModelPartBuilder.create(), ModelTransform.pivot(-2.0F, 0.0F, 0.0F));
            ModelPartData right = modelRoot.addChild("rightWing", ModelPartBuilder.create(), ModelTransform.pivot(2.0F, 0.0F, 0.0F));
            populateWing(left, -2.0F, "left");
            populateWing(right, 2.0F, "right");
            root = TexturedModelData.of(data, 128, 128).createModel();
            leftWing = root.getChild("leftWing");
            rightWing = root.getChild("rightWing");
        }

        private void populateWing(ModelPartData wing, float pivotX, String side) {
            wing.addChild("membrane", ModelPartBuilder.create().uv(0, 0).cuboid(pivotX < 0 ? -9.0F : -7.0F, -7.0F, -0.5F, 16.0F, 14.0F, 1.0F), ModelTransform.NONE);
            float[] lengths = {4.5F, 6.0F, 6.5F, 5.5F, 4.0F};
            float[] angles = {-0.9F, -0.4F, 0.0F, 0.4F, 0.9F};
            for (int i = 0; i < 5; i++) {
                ModelPartData bone = wing.addChild(side + "Bone" + i,
                        ModelPartBuilder.create().uv(32, 0).cuboid(-0.25F, -lengths[i], -0.25F, 0.5F, lengths[i], 0.5F),
                        ModelTransform.of((pivotX < 0 ? -5.0F : -3.0F) + i * 2.2F, 0, 0, angles[i] * (pivotX < 0 ? 1 : -1), 0, 0));
                ModelPartData tip = bone.addChild(side + "Tip" + i, ModelPartBuilder.create().uv(40, 0).cuboid(-0.35F, -lengths[i] - 0.6F, -0.35F, 0.7F, 0.8F, 0.7F), ModelTransform.pivot(0, -lengths[i], 0));
                tip.addChild(side + "Claw" + i, ModelPartBuilder.create().uv(44, 0).cuboid(-0.15F, -lengths[i] - 1.0F, -0.15F, 0.3F, 0.5F, 0.3F), ModelTransform.pivot(0, -lengths[i] - 0.6F, 0));
            }
        }

        @Override public ModelPart getPart() { return root; }

        @Override
        public void setAngles(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
            float flight = entity.getFallDistance() > 0.5F ? animationProgress : 0;
            float open = MathHelper.clamp(flight * 0.4F, 0, 1);
            float flap = MathHelper.sin(flight * 1.5F * WingsMod.config.wingSpeed) * 0.5F;
            float heavy = MathHelper.sin(flight * 1.5F + 0.5F) * 0.3F;
            leftWing.roll = -1.2F + open * 1.4F + flap;
            leftWing.yaw = -0.2F + open * 0.3F;
            leftWing.pitch = heavy * 0.2F;
            rightWing.roll = 1.2F - open * 1.4F - flap;
            rightWing.yaw = 0.2F - open * 0.3F;
            rightWing.pitch = heavy * 0.2F;
            if (!WingsMod.config.wingAnimation) {
                leftWing.roll /= 2.0F;
                rightWing.roll /= 2.0F;
            }
        }
    }

    public static class HaloModel extends SinglePartEntityModel<LivingEntity> {
        private final ModelPart root, halo;
        public HaloModel() {
            ModelData data = new ModelData();
            ModelPartData r = data.getRoot();
            ModelPartData h = r.addChild("halo", ModelPartBuilder.create().uv(0, 0).cuboid(-9, -0.5F, -9, 18, 1, 18), ModelTransform.pivot(0, -6, 0));
            h.addChild("glow", ModelPartBuilder.create().uv(0, 18).cuboid(-8.5F, -0.3F, -8.5F, 17, 0.6F, 17), ModelTransform.NONE);
            h.addChild("inner", ModelPartBuilder.create().uv(0, 35).cuboid(-7, -0.2F, -7, 14, 0.4F, 14), ModelTransform.NONE);
            root = TexturedModelData.of(data, 32, 32).createModel();
            halo = root.getChild("halo");
        }
        @Override public ModelPart getPart() { return root; }
        @Override public void setAngles(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
            halo.yaw = animationProgress * 0.04F;
            halo.roll = MathHelper.sin(animationProgress * 0.02F) * 0.05F;
        }
    }

    public static class HornsModel extends SinglePartEntityModel<LivingEntity> {
        private final ModelPart root, leftHorn, rightHorn;
        public HornsModel() {
            ModelData data = new ModelData();
            ModelPartData r = data.getRoot();
            createHornData(r, "leftHorn", -3.5F);
            createHornData(r, "rightHorn", 3.5F);
            root = TexturedModelData.of(data, 32, 32).createModel();
            leftHorn = root.getChild("leftHorn");
            rightHorn = root.getChild("rightHorn");
        }

        private void createHornData(ModelPartData parent, String name, float pivotX) {
            ModelPartData horn = parent.addChild(name, ModelPartBuilder.create(), ModelTransform.pivot(pivotX, -5.5F, 0));
            ModelPartData base = horn.addChild("base", ModelPartBuilder.create().uv(0, 0).cuboid(-0.6F, 0, -0.6F, 1.2F, 2.5F, 1.2F), ModelTransform.NONE);
            ModelPartData mid = base.addChild("mid", ModelPartBuilder.create().uv(8, 0).cuboid(-0.5F, -2, -0.5F, 1, 2, 1), ModelTransform.of(0, 0, 0, pivotX < 0 ? -0.4F : -0.3F, 0, pivotX < 0 ? 0.3F : -0.3F));
            ModelPartData tip = mid.addChild("tip", ModelPartBuilder.create().uv(16, 0).cuboid(-0.35F, -2, -0.35F, 0.7F, 2, 0.7F), ModelTransform.of(0, -2, 0, pivotX < 0 ? -0.3F : -0.2F, 0, 0));
            tip.addChild("point", ModelPartBuilder.create().uv(24, 0).cuboid(-0.15F, -0.8F, -0.15F, 0.3F, 0.8F, 0.3F), ModelTransform.pivot(0, -2, 0));
        }

        @Override public ModelPart getPart() { return root; }
        @Override public void setAngles(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
            float walk = limbAngle * 0.05F;
            leftHorn.roll = 0.1F + walk;
            rightHorn.roll = -0.1F - walk;
            leftHorn.pitch = MathHelper.sin(animationProgress * 0.02F) * 0.02F;
            rightHorn.pitch = MathHelper.sin(animationProgress * 0.02F + 0.5F) * 0.02F;
        }
    }

    public static class CosmeticLayer extends FeatureRenderer<PlayerEntity, PlayerEntityModel<PlayerEntity>> {
        private final AngelWingModel angelWings = new AngelWingModel();
        private final DemonWingModel demonWings = new DemonWingModel();
        private final HaloModel halo = new HaloModel();
        private final HornsModel horns = new HornsModel();
        private static final Identifier ANGEL = Identifier.of(MOD_ID, "textures/entity/angel.png");
        private static final Identifier ANGEL_GLOW = Identifier.of(MOD_ID, "textures/entity/angel_glow.png");
        private static final Identifier DEMON = Identifier.of(MOD_ID, "textures/entity/demon.png");
        private static final Identifier HALO_TEX = Identifier.of(MOD_ID, "textures/entity/halo.png");
        private static final Identifier HORNS_TEX = Identifier.of(MOD_ID, "textures/entity/horns.png");

        public CosmeticLayer(FeatureRendererContext<PlayerEntity, PlayerEntityModel<PlayerEntity>> context) { super(context); }

        @Override
        public void render(MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider vertexConsumers, int light,
                           PlayerEntity player, float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                           float headYaw, float headPitch) {
            if (player.isInvisible()) return;
            ModConfig cfg = WingsMod.config;
            if (cfg.wingType != 0) {
                matrices.push();
                matrices.translate(0, 0.6, 0.2);
                matrices.scale(cfg.wingScale, cfg.wingScale, cfg.wingScale);
                if (cfg.wingType == 1) {
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5));
                    angelWings.setAngles(player, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
                    angelWings.getPart().render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutout(ANGEL)), light, OverlayTexture.DEFAULT_UV);
                    angelWings.getPart().render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(ANGEL_GLOW)), 0xF000F0, OverlayTexture.DEFAULT_UV);
                } else {
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10));
                    demonWings.setAngles(player, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
                    demonWings.getPart().render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutout(DEMON)), light, OverlayTexture.DEFAULT_UV);
                }
                matrices.pop();
            }
            if (cfg.haloEnabled) {
                matrices.push();
                matrices.translate(0, 1.9, 0);
                matrices.scale(cfg.haloScale, cfg.haloScale, cfg.haloScale);
                float pulse = 1.0F + MathHelper.sin((player.age + tickDelta) * 0.08F) * 0.02F;
                matrices.scale(pulse, pulse, pulse);
                halo.setAngles(player, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
                halo.getPart().render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityTranslucentEmissive(HALO_TEX)), 0xF000F0, OverlayTexture.DEFAULT_UV);
                matrices.pop();
            }
            if (cfg.hornsEnabled) {
                matrices.push();
                matrices.translate(0, 0.6, 0);
                matrices.scale(cfg.hornsScale, cfg.hornsScale, cfg.hornsScale);
                horns.setAngles(player, limbAngle, limbDistance, animationProgress, headYaw, headPitch);
                horns.getPart().render(matrices, vertexConsumers.getBuffer(RenderLayer.getEntityCutout(HORNS_TEX)), light, OverlayTexture.DEFAULT_UV);
                matrices.pop();
            }
        }
    }

    public static class ConfigScreen extends Screen {
        private final Screen parent;
        private final ModConfig cfg;
        public ConfigScreen(Screen parent) {
            super(Text.literal("§6§lWings Mod Settings"));
            this.parent = parent;
            this.cfg = WingsMod.config;
        }
        @Override protected void init() {
            int y = 25, spacing = 24;
            String[] wingLabels = {"§cВимкнено", "§bАнгельські", "§cДемонічні"};
            addDrawableChild(CyclingButtonWidget.builder((String value) -> Text.literal(value))
                    .values(wingLabels)
                    .initially(wingLabels[MathHelper.clamp(cfg.wingType, 0, 2)])
                    .build(10, y, 190, 20, Text.literal("§fТип крил:"), (btn, val) -> {
                        for (int i = 0; i < wingLabels.length; i++) {
                            if (wingLabels[i].equals(val)) { cfg.wingType = i; break; }
                        }
                    }));
            y += spacing;
            addDrawableChild(CyclingButtonWidget.onOffBuilder(Text.literal("Увімкнено"), Text.literal("Вимкнено"))
                    .initially(cfg.haloEnabled).build(10, y, 190, 20, Text.literal("§fНімб:"), (btn, val) -> cfg.haloEnabled = val)); y += spacing;
            addDrawableChild(CyclingButtonWidget.onOffBuilder(Text.literal("Увімкнено"), Text.literal("Вимкнено"))
                    .initially(cfg.hornsEnabled).build(10, y, 190, 20, Text.literal("§fРога:"), (btn, val) -> cfg.hornsEnabled = val)); y += spacing;
            addDrawableChild(CyclingButtonWidget.onOffBuilder(Text.literal("Увімкнено"), Text.literal("Вимкнено"))
                    .initially(cfg.wingAnimation).build(10, y, 190, 20, Text.literal("§fАнімація крил:"), (btn, val) -> cfg.wingAnimation = val)); y += spacing;
            addDrawableChild(CyclingButtonWidget.onOffBuilder(Text.literal("Увімкнено"), Text.literal("Вимкнено"))
                    .initially(cfg.feathersEnabled).build(10, y, 190, 20, Text.literal("§fВипадіння пер:"), (btn, val) -> cfg.feathersEnabled = val)); y += spacing;
            addDrawableChild(new SliderWidget(10, y, 190, 20, Text.literal("§fЧастота пер"), cfg.featherFrequency / 5.0) {
                @Override protected void updateMessage() { String[] labels = {"Вимк", "Рідко", "Норм", "Часто", "Дуже часто", "Макс"}; setMessage(Text.literal("§fЧастота пер: §e" + labels[(int)Math.round(value * 5)])); }
                @Override protected void applyValue() { cfg.featherFrequency = (int)Math.round(value * 5); }
            }); y += spacing;
            addDrawableChild(new SliderWidget(10, y, 190, 20, Text.literal("§fРозмір крил"), (cfg.wingScale - 0.5) / 1.5) {
                @Override protected void updateMessage() { setMessage(Text.literal("§fРозмір крил: §e" + String.format("%.1f", 0.5 + value * 1.5))); }
                @Override protected void applyValue() { cfg.wingScale = (float)(0.5 + value * 1.5); }
            }); y += spacing;
            addDrawableChild(new SliderWidget(10, y, 190, 20, Text.literal("§fШвидкість крил"), (cfg.wingSpeed - 0.5) / 1.5) {
                @Override protected void updateMessage() { setMessage(Text.literal("§fШвидкість крил: §e" + String.format("%.1f", 0.5 + value * 1.5))); }
                @Override protected void applyValue() { cfg.wingSpeed = (float)(0.5 + value * 1.5); }
            });
            addDrawableChild(ButtonWidget.builder(Text.literal("§a§lЗБЕРЕГТИ"), btn -> { cfg.save(); close(); }).dimensions(width / 2 - 80, height - 28, 75, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("§cСКИНУТИ"), btn -> { resetConfig(); clearChildren(); init(); }).dimensions(width / 2 + 5, height - 28, 75, 20).build());
        }
        private void resetConfig() { cfg.wingType=0; cfg.haloEnabled=false; cfg.hornsEnabled=false; cfg.wingAnimation=true; cfg.feathersEnabled=true; cfg.featherFrequency=3; cfg.wingScale=1.0F; cfg.haloScale=1.0F; cfg.hornsScale=1.0F; cfg.wingSpeed=1.0F; }
        @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) { renderBackground(context, mouseX, mouseY, delta); context.drawCenteredTextWithShadow(textRenderer, title, width/2, 8, 0xFFFF55); super.render(context, mouseX, mouseY, delta); }
        @Override public void close() { cfg.save(); if (client != null) client.setScreen(parent); }
    }
}
