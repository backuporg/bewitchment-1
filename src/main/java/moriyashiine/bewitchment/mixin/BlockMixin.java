package moriyashiine.bewitchment.mixin;

import moriyashiine.bewitchment.api.BewitchmentAPI;
import moriyashiine.bewitchment.api.interfaces.entity.ContractAccessor;
import moriyashiine.bewitchment.api.interfaces.entity.FortuneAccessor;
import moriyashiine.bewitchment.common.block.SaltLineBlock;
import moriyashiine.bewitchment.common.misc.BWUtil;
import moriyashiine.bewitchment.common.registry.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(Block.class)
public abstract class BlockMixin {
	@Inject(method = "getDroppedStacks(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/item/ItemStack;)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
	private static void getDroppedStacks(BlockState state, ServerWorld world, BlockPos pos, @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack stack, CallbackInfoReturnable<List<ItemStack>> callbackInfo) {
		if (entity instanceof LivingEntity) {
			boolean damage = false;
			if (state.getBlock() instanceof SaltLineBlock && BewitchmentAPI.isWeakToSilver((LivingEntity) entity)) {
				damage = true;
			}
			else if (state.getBlock() == BWObjects.GARLIC_CROP && BewitchmentAPI.isVampire(entity, true)) {
				damage = true;
			}
			else if (state.getBlock() == BWObjects.ACONITE_CROP && BewitchmentAPI.isWerewolf(entity, true)) {
				damage = true;
			}
			if (damage) {
				entity.damage(DamageSource.MAGIC, ((LivingEntity) entity).getMaxHealth() * 1 / 2f);
			}
			List<ItemStack> drops = callbackInfo.getReturnValue();
			if (!drops.isEmpty() && !EnchantmentHelper.get(stack).containsKey(Enchantments.SILK_TOUCH)) {
				if (entity instanceof FortuneAccessor) {
					FortuneAccessor fortuneAccessor = (FortuneAccessor) entity;
					if (fortuneAccessor.getFortune() != null) {
						if (fortuneAccessor.getFortune().fortune == BWFortunes.TREASURE && world.random.nextFloat() < 1 / 25f) {
							Set<ItemStack> treasure = new HashSet<>();
							for (int i = 0; i < world.random.nextInt(1) + 1; i++) {
								switch (world.random.nextInt(4)) {
									case 0:
										treasure.add(new ItemStack(Items.DIAMOND, MathHelper.nextInt(world.random, 1, 3)));
										break;
									case 1:
										treasure.add(new ItemStack(Items.GOLD_INGOT, MathHelper.nextInt(world.random, 1, 5)));
										break;
									case 2:
										treasure.add(new ItemStack(Items.IRON_INGOT, MathHelper.nextInt(world.random, 2, 10)));
										break;
									case 3:
										treasure.add(new ItemStack(BWObjects.SILVER_INGOT, MathHelper.nextInt(world.random, 1, 5)));
										break;
									default:
										treasure.add(ItemStack.EMPTY);
								}
							}
							drops.addAll(treasure);
							fortuneAccessor.getFortune().duration = 0;
						}
						else if (fortuneAccessor.getFortune().fortune == BWFortunes.INFESTED && world.random.nextFloat() < 1 / 25f) {
							SilverfishEntity silverfishEntity = EntityType.SILVERFISH.create(world);
							if (silverfishEntity != null) {
								silverfishEntity.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null, null);
								silverfishEntity.updatePositionAndAngles(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, world.random.nextFloat() * 360, 0);
								world.spawnEntity(silverfishEntity);
								fortuneAccessor.getFortune().duration = 0;
							}
						}
					}
				}
				if (entity instanceof ContractAccessor) {
					ContractAccessor contractAccessor = (ContractAccessor) entity;
					if (contractAccessor.hasContract(BWContracts.GREED)) {
						boolean foundOre = false;
						for (int i = 0; i < drops.size(); i++) {
							if (BWTags.ORES.contains(state.getBlock().asItem())) {
								drops.set(i, new ItemStack(drops.get(i).getItem(), drops.get(i).getCount() * 2));
								foundOre = true;
							}
						}
						if (foundOre && contractAccessor.hasNegativeEffects() && world.random.nextFloat() < 1 / 3f) {
							drops.clear();
						}
					}
					if (contractAccessor.hasContract(BWContracts.PRIDE)) {
						boolean foundOre = false;
						for (Recipe<?> smeltingRecipe : world.getRecipeManager().listAllOfType(RecipeType.SMELTING)) {
							for (int i = 0; i < drops.size(); i++) {
								if (BWTags.ORES.contains(drops.get(i).getItem())) {
									for (Ingredient ingredient : smeltingRecipe.getPreviewInputs()) {
										if (ingredient.test(drops.get(i))) {
											drops.set(i, new ItemStack(smeltingRecipe.getOutput().getItem(), smeltingRecipe.getOutput().getCount() * drops.get(i).getCount()));
											foundOre = true;
										}
									}
								}
							}
						}
						if (foundOre && contractAccessor.hasNegativeEffects() && world.random.nextFloat() < 1 / 8f) {
							world.createExplosion(entity, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, Explosion.DestructionType.BREAK);
							world.getEntitiesByClass(LivingEntity.class, new Box(pos).expand(4), LivingEntity::isAlive).forEach(livingEntity -> livingEntity.damage(DamageSource.explosion((LivingEntity) entity), 12));
						}
					}
				}
				if (state.getBlock() instanceof CropBlock && state.get(((CropBlock) state.getBlock()).getAgeProperty()) == ((CropBlock) state.getBlock()).getMaxAge() && BWUtil.getArmorPieces((LivingEntity) entity, armorStack -> armorStack.getItem() instanceof ArmorItem && ((ArmorItem) armorStack.getItem()).getMaterial() == BWMaterials.HEDGEWITCH_ARMOR) >= 3) {
					for (int i = 0; i < drops.size(); i++) {
						drops.set(i, new ItemStack(drops.get(i).getItem(), drops.get(i).getCount() + 1));
					}
				}
			}
		}
	}
}
