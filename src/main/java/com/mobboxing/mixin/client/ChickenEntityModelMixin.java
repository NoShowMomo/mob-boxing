package com.mobboxing.mixin.client;

import com.mobboxing.client.BoxerPose;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.ChickenEntityModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Chickens throw punches with their wings. */
@Mixin(ChickenEntityModel.class)
public abstract class ChickenEntityModelMixin {
    @Shadow
    private ModelPart rightWing;
    @Shadow
    private ModelPart leftWing;

    @Inject(method = "setAngles(Lnet/minecraft/entity/Entity;FFFFF)V", at = @At("TAIL"))
    private void mobboxing$boxingWings(Entity entity, float limbAngle, float limbDistance, float animationProgress,
                                       float headYaw, float headPitch, CallbackInfo ci) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        BoxerPose.State s = BoxerPose.peek(living);
        if (s != null && s.stand > 0.001f) {
            // Cancel the flapping roll so the wings stay tucked in like gloves.
            rightWing.roll = 0f;
            leftWing.roll = 0f;
            BoxerPose.poseArms(s, animationProgress, rightWing, leftWing);
        }
    }
}
