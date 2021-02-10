package moriyashiine.bewitchment.mixin;

import moriyashiine.bewitchment.api.interfaces.entity.CurseAccessor;
import moriyashiine.bewitchment.common.entity.interfaces.MasterAccessor;
import moriyashiine.bewitchment.common.misc.BWUtil;
import moriyashiine.bewitchment.common.registry.BWCurses;
import moriyashiine.bewitchment.common.registry.BWObjects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.FollowTargetGoal;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.mob.MobEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Predicate;

@Mixin(FollowTargetGoal.class)
public abstract class FollowTargetGoalMixin<T extends LivingEntity> extends TrackTargetGoal {
	@Shadow
	protected TargetPredicate targetPredicate;
	
	@Shadow
	protected LivingEntity targetEntity;
	
	public FollowTargetGoalMixin(MobEntity mob, boolean checkVisibility) {
		super(mob, checkVisibility);
	}
	
	@Inject(method = "<init>(Lnet/minecraft/entity/mob/MobEntity;Ljava/lang/Class;IZZLjava/util/function/Predicate;)V", at = @At("TAIL"))
	private void init(MobEntity mob, Class<T> targetClass, int reciprocalChance, boolean checkVisibility, boolean checkCanNavigate, @Nullable Predicate<LivingEntity> predicate, CallbackInfo callbackInfo) {
		if (predicate != null) {
			targetPredicate.setPredicate(predicate.or(livingEntity -> ((CurseAccessor) livingEntity).hasCurse(BWCurses.OUTRAGE)));
		}
	}
	
	@Inject(method = "findClosestTarget", at = @At("TAIL"))
	private void findClosestTarget(CallbackInfo callbackInfo) {
		if (mob instanceof MasterAccessor && ((MasterAccessor) mob).getMasterUUID() == null && mob.isUndead() && targetEntity != null && BWUtil.getArmorPieces(targetEntity, stack -> stack.getItem() == BWObjects.HARBINGER) > 0) {
			targetEntity = null;
		}
	}
}
