package com.mobboxing.mixin.client;

import com.mobboxing.client.BoxerPose;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.QuadrupedEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cows, mooshrooms, pigs, sheep: the two front legs become boxing arms. */
@Mixin(QuadrupedEntityModel.class)
public abstract class QuadrupedEntityModelMixin {
    @Shadow
    protected ModelPart rightFrontLeg;
    @Shadow
    protected ModelPart leftFrontLeg;

    @Inject(method = "setAngles(Lnet/minecraft/entity/Entity;FFFFF)V", at = @At("TAIL"))
    private void mobboxing$boxingArms(Entity entity, float limbAngle, float limbDistance, float animationProgress,
                                      float headYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        BoxerPose.State s = BoxerPose.peek(living);
        if (s != null && s.stand > 0.001f) {
            BoxerPose.poseArms(s, animationProgress, rightFrontLeg, leftFrontLeg);
        }
    }
}
