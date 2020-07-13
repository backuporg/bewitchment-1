package moriyashiine.bewitchment.common.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.block.pattern.BlockPatternBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class UnformedWitchAltarBlock extends Block {
	public static final Set<AltarGroup> ALTAR_MAP = new HashSet<>();
	
	private final BlockPattern pattern = BlockPatternBuilder.start().aisle("AAA", "AAA").where('A', state -> state.getBlockState().getBlock() == this).build();
	
	public UnformedWitchAltarBlock(Settings settings) {
		super(settings);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		boolean client = world.isClient;
		if (!client) {
			AtomicBoolean failed = new AtomicBoolean(true);
			BlockPattern.Result match = pattern.searchAround(world, pos);
			if (match != null && match.getForwards().getAxis() == Direction.Axis.Y) {
				for (AltarGroup group : ALTAR_MAP) {
					if (group.unformed == this) {
						Map<BlockPos, Boolean> altarPoses = getAltarBlockPoses(world, pos);
						boolean wrongCarpet = false;
						for (BlockPos altarPos : altarPoses.keySet()) {
							if (world.getBlockState(altarPos.up()).getBlock() != group.carpet) {
								wrongCarpet = true;
								break;
							}
						}
						if (!wrongCarpet) {
							altarPoses.keySet().forEach(altarPos -> {
								world.setBlockState(altarPos, group.formed.getDefaultState().with(BWProperties.ALTAR_CORE, altarPoses.get(altarPos)));
								world.breakBlock(altarPos.up(), false);
							});
							failed.set(false);
							break;
						}
					}
				}
			}
			if (failed.get()) {
				world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_SNARE, SoundCategory.BLOCKS, 1, 0.8f);
				player.sendMessage(new TranslatableText("altar.invalid"), true);
			}
			else {
				world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 1, 1);
			}
		}
		return ActionResult.success(client);
	}
	
	private Map<BlockPos, Boolean> getAltarBlockPoses(World world, BlockPos pos) {
		Map<BlockPos, Boolean> altarPoses = new HashMap<>();
		BlockPattern.Result match = pattern.searchAround(world, pos);
		if (match != null) {
			for (int w = 0; w < match.getWidth(); w++) {
				for (int h = 0; h < match.getHeight(); h++) {
					altarPoses.put(match.translate(w, h, 0).getBlockPos(), w == 1 && h == 1);
				}
			}
		}
		return altarPoses;
	}
	
	public static class AltarGroup {
		public final Block unformed, formed, carpet;
		
		public AltarGroup(Block unformed, Block formed, Block carpet) {
			this.unformed = unformed;
			this.formed = formed;
			this.carpet = carpet;
		}
	}
}