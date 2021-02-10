package moriyashiine.bewitchment.common.ritualfunction;

import moriyashiine.bewitchment.api.registry.RitualFunction;
import moriyashiine.bewitchment.common.misc.BWUtil;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PlantBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.particle.ParticleType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.function.Predicate;

public class DestroyPlantsRitualFunction extends RitualFunction {
	public DestroyPlantsRitualFunction(ParticleType<?> startParticle, Predicate<LivingEntity> sacrifice) {
		super(startParticle, sacrifice);
	}
	
	@Override
	public void start(ServerWorld world, BlockPos glyphPos, BlockPos effectivePos, Inventory inventory, boolean catFamiliar) {
		for (BlockPos plant : BWUtil.getBlockPoses(effectivePos, catFamiliar ? 24 : 8, currentPos -> world.getBlockState(currentPos).getBlock() instanceof PlantBlock || world.getBlockState(currentPos).getBlock() instanceof LeavesBlock)) {
			world.breakBlock(plant, true);
		}
		super.start(world, glyphPos, effectivePos, inventory, catFamiliar);
	}
}
