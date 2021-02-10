package moriyashiine.bewitchment.mixin;

import moriyashiine.bewitchment.api.interfaces.entity.TransformationAccessor;
import moriyashiine.bewitchment.common.block.CoffinBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BedBlock.class)
public class BedBlockMixin {
	@Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
	private void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> callbackInfo) {
		if (((TransformationAccessor) player).getAlternateForm()) {
			if (!world.isClient) {
				player.sendMessage(new TranslatableText("block.minecraft.bed.transformation"), true);
			}
			callbackInfo.setReturnValue(ActionResult.success(!world.isClient));
		}
	}
	
	@Inject(method = "onUse", at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lnet/minecraft/entity/player/PlayerEntity;trySleep(Lnet/minecraft/util/math/BlockPos;)Lcom/mojang/datafixers/util/Either;"), cancellable = true)
	private void trySleep(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> callbackInfo) {
		if (state.getBlock() instanceof CoffinBlock) {
			if (world.isDay()) {
				player.trySleep(pos);
				player.sleep(pos);
			}
			else {
				player.sendMessage(new TranslatableText("block.minecraft.bed.coffin"), true);
			}
			callbackInfo.setReturnValue(ActionResult.SUCCESS);
		}
	}
}
