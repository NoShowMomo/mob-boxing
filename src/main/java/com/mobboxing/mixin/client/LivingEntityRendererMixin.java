package com.mobboxing.mixin.client;

import com.mobboxing.BoxingConfig;
import com.mobboxing.client.BoxerPose;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Whole-body pose for every boxing animal, whatever its model:
 *  - fighting: rear up on the hind legs and lean into each punch
 *  - knocked out: fall over onto its side
 * Animals with known front limbs (QuadrupedEntityModel, chickens) also get arm animation from
 * the model mixins; any other model still gets the rearing and lunging.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Unique
    private boolean mobboxing$pushed;

    @Inject(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"))
    private void mobboxing$pose(LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                                VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        mobboxing$pushed = false;
        BoxerPose.State s = BoxerPose.update(entity, tickDelta);
        if (s == null) {
            return;
        }
        matrices.push();
        mobboxing$pushed = true;

        // We are still in world space here; the renderer later rotates by (180 - bodyYaw).
        // Conjugating by that rotation lets us tilt around the animal's own left-right axis.
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, entity.prevBodyYaw, entity.bodyYaw);
        float a = 180.0f - bodyYaw;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(a));
        // From here on: the animal's own frame (forward = -Z, up = +Y, sideways = X).

        float width = entity.getWidth();
        float height = entity.getHeight();

        if (s.ko > 0f) {
            // Fall over onto its side: lift by half its width, shift to stay centred, roll 90 degrees.
            matrices.translate(height * 0.5f * s.ko, width * 0.5f * s.ko, 0f);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90.0f * s.ko));
        }

        if (s.stand > 0f) {
            float lunge = 12.0f * MathHelper.sin(s.swing * (float) Math.PI); // lean into each punch
            float tilt = s.stand * BoxingConfig.get().standAngleDegrees - lunge * s.stand;
            float pivotZ = width * 0.35f; // roughly where the hind feet are
            matrices.translate(0f, 0f, pivotZ);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(tilt));
            matrices.translate(0f, 0f, -pivotZ);
        }

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-a));
    }

    @Inject(
            method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("RETURN"))
    private void mobboxing$unpose(LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices,
                                  VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (mobboxing$pushed) {
            matrices.pop();
            mobboxing$pushed = false;
        }
    }
}
