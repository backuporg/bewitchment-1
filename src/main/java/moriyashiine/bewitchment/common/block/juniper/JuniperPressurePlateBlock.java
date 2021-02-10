package moriyashiine.bewitchment.common.block.juniper;

import com.terraformersmc.terraform.wood.block.TerraformPressurePlateBlock;
import moriyashiine.bewitchment.common.block.entity.TaglockHolderBlockEntity;
import moriyashiine.bewitchment.common.block.entity.interfaces.TaglockHolder;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("ConstantConditions")
public class JuniperPressurePlateBlock extends TerraformPressurePlateBlock implements BlockEntityProvider {
	public JuniperPressurePlateBlock(Settings settings) {
		super(settings);
	}
	
	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockView world) {
		return new TaglockHolderBlockEntity();
	}
	
	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		ActionResult result = TaglockHolder.onUse(world, pos, player);
		if (result != ActionResult.PASS) {
			return result;
		}
		return super.onUse(state, world, pos, player, hand, hit);
	}
	
	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) {
		super.onPlaced(world, pos, state, placer, itemStack);
		if (!world.isClient && placer != null) {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			TaglockHolder taglockHolder = (TaglockHolder) blockEntity;
			taglockHolder.setOwner(placer.getUuid());
			taglockHolder.syncTaglockHolder(world, blockEntity);
			blockEntity.markDirty();
		}
	}
	
	@Override
	public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
		if (!world.isClient && state.getBlock() != newState.getBlock()) {
			BlockEntity blockEntity = world.getBlockEntity(pos);
			if (blockEntity instanceof TaglockHolder) {
				ItemScatterer.spawn(world, pos, ((TaglockHolder) blockEntity).getTaglockInventory());
			}
		}
		super.onStateReplaced(state, world, pos, newState, moved);
	}
	
	@Override
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if (entity instanceof LivingEntity) {
			TaglockHolder taglockHolder = ((TaglockHolder) world.getBlockEntity(pos));
			if (!entity.getUuid().equals(taglockHolder.getOwner())) {
				taglockHolder.addTaglock(world, pos, entity);
			}
		}
		super.onEntityCollision(state, world, pos, entity);
	}
}
