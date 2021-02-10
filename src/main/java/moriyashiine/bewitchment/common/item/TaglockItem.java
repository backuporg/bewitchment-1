package moriyashiine.bewitchment.common.item;

import moriyashiine.bewitchment.api.interfaces.entity.ContractAccessor;
import moriyashiine.bewitchment.common.Bewitchment;
import moriyashiine.bewitchment.common.block.entity.interfaces.Lockable;
import moriyashiine.bewitchment.common.block.entity.interfaces.SigilHolder;
import moriyashiine.bewitchment.common.block.entity.interfaces.TaglockHolder;
import moriyashiine.bewitchment.common.misc.BWUtil;
import moriyashiine.bewitchment.common.registry.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("ConstantConditions")
public class TaglockItem extends Item {
	public TaglockItem(Settings settings) {
		super(settings);
	}
	
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		BlockPos pos = context.getBlockPos();
		BlockState state = world.getBlockState(pos);
		PlayerEntity player = context.getPlayer();
		boolean client = world.isClient;
		if (state.getBlock() instanceof BedBlock) {
			if (!client && player != null && player.isSneaking()) {
				MinecraftServer server = world.getServer();
				if (server != null) {
					PlayerEntity earliestSleeper = null;
					for (ServerPlayerEntity playerEntity : server.getPlayerManager().getPlayerList()) {
						BlockPos bedPos = playerEntity.getSpawnPointPosition();
						if (bedPos != null && bedPos.equals(pos) && (earliestSleeper == null || playerEntity.getSleepTimer() < earliestSleeper.getSleepTimer())) {
							earliestSleeper = playerEntity;
						}
					}
					if (earliestSleeper != null) {
						return useTaglock(player, earliestSleeper, context.getHand(), false, true);
					}
				}
			}
			return ActionResult.success(client);
		}
		else {
			ItemStack stack = context.getStack();
			BlockEntity blockEntity = world.getBlockEntity(state.getBlock() instanceof DoorBlock && state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos);
			if (blockEntity instanceof TaglockHolder) {
				TaglockHolder taglockHolder = (TaglockHolder) blockEntity;
				if (player.getUuid().equals(taglockHolder.getOwner())) {
					int firstEmpty = taglockHolder.getFirstEmptySlot();
					if (firstEmpty != -1) {
						if (!client) {
							taglockHolder.getTaglockInventory().set(firstEmpty, stack.split(1));
							taglockHolder.syncTaglockHolder(world, blockEntity);
							blockEntity.markDirty();
						}
						return ActionResult.success(client);
					}
				}
			}
			else if (blockEntity instanceof Lockable) {
				if (hasTaglock(stack)) {
					Lockable lockable = (Lockable) blockEntity;
					if (player.getUuid().equals(lockable.getOwner()) && lockable.getLocked()) {
						UUID uuid = getTaglockUUID(stack);
						if (!lockable.getEntities().contains(uuid)) {
							if (!client) {
								if (lockable.getEntities().isEmpty()) {
									lockable.setModeOnWhitelist(true);
								}
								lockable.getEntities().add(uuid);
								if (!player.isCreative()) {
									stack.decrement(1);
								}
								lockable.syncLockable(world, blockEntity);
								blockEntity.markDirty();
							}
							return ActionResult.success(client);
						}
					}
				}
			}
			else if (blockEntity instanceof SigilHolder) {
				if (hasTaglock(stack)) {
					if (!client) {
						SigilHolder sigilHolder = (SigilHolder) blockEntity;
						if (sigilHolder.getSigil() != null) {
							UUID uuid = getTaglockUUID(stack);
							if (!sigilHolder.getEntities().contains(uuid)) {
								if (sigilHolder.getEntities().isEmpty()) {
									sigilHolder.setModeOnWhitelist(true);
								}
								sigilHolder.getEntities().add(uuid);
								if (!player.isCreative()) {
									stack.decrement(1);
								}
								sigilHolder.syncSigilHolder(world, blockEntity);
								blockEntity.markDirty();
							}
						}
					}
					return ActionResult.success(client);
				}
			}
		}
		return super.useOnBlock(context);
	}
	
	@Override
	public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
		return useTaglock(user, entity, hand, true, false);
	}
	
	@Environment(EnvType.CLIENT)
	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		if (hasTaglock(stack)) {
			tooltip.add(new LiteralText(getTaglockName(stack)).formatted(Formatting.GRAY));
			CompoundTag tag = stack.getTag();
			if (tag.contains("UsedForScrying")) {
				if (tag.contains("Failed")) {
					tooltip.add(new TranslatableText("Failed").formatted(Formatting.DARK_GRAY));
				}
				else {
					BlockPos pos = BlockPos.fromLong(stack.getTag().getLong("LocationPos"));
					tooltip.add(new TranslatableText(Bewitchment.MODID + ".location_tooltip", pos.getX(), pos.getY(), pos.getZ(), stack.getTag().getString("LocationWorld")).formatted(Formatting.DARK_GRAY));
					tooltip.add(new TranslatableText(Bewitchment.MODID + ".level_tooltip", stack.getTag().getInt("Level")).formatted(Formatting.DARK_GRAY));
					tooltip.add(new LiteralText("Curses: " + tag.get("Curses")).formatted(Formatting.DARK_GRAY));
					tooltip.add(new LiteralText("Contracts: " + tag.get("Contracts")).formatted(Formatting.DARK_GRAY));
					tooltip.add(new TranslatableText(Bewitchment.MODID + ".transformation_tooltip", new TranslatableText(stack.getTag().getString("Transformation"))).formatted(Formatting.DARK_GRAY));
					tooltip.add(new TranslatableText(Bewitchment.MODID + ".familiar_tooltip", new TranslatableText("entity." + tag.getString("Familiar").replace(":", "."))).formatted(Formatting.DARK_GRAY));
					tooltip.add(new TranslatableText(Bewitchment.MODID + ".pledge_tooltip", new TranslatableText(stack.getTag().getString("Pledge"))).formatted(Formatting.DARK_GRAY));
				}
			}
		}
	}
	
	public static ActionResult useTaglock(PlayerEntity user, LivingEntity entity, Hand hand, boolean checkVisibility, boolean bed) {
		ItemStack stack = user.getStackInHand(hand);
		if (entity.isAlive() && !BWTags.UNTAGLOCKABLE.contains(entity.getType()) && !hasTaglock(stack)) {
			boolean failed = false;
			BlockPos sigilPos = BWUtil.getClosestBlockPos(entity.getBlockPos(), 16, currentPos -> user.world.getBlockEntity(currentPos) instanceof SigilHolder && ((SigilHolder) user.world.getBlockEntity(currentPos)).getSigil() == BWSigils.SLIPPERY);
			if (sigilPos == null && bed) {
				sigilPos = BWUtil.getClosestBlockPos(user.getBlockPos(), 16, currentPos -> user.world.getBlockEntity(currentPos) instanceof SigilHolder && ((SigilHolder) user.world.getBlockEntity(currentPos)).getSigil() == BWSigils.SLIPPERY);
			}
			if (sigilPos != null) {
				BlockEntity blockEntity = user.world.getBlockEntity(sigilPos);
				SigilHolder sigil = (SigilHolder) blockEntity;
				if (sigil.test(entity)) {
					sigil.setUses(sigil.getUses() - 1);
					blockEntity.markDirty();
					failed = true;
				}
			}
			if (!failed && checkVisibility && !entity.isSleeping()) {
				double targetYaw = entity.getHeadYaw() % 360;
				double userYaw = user.getHeadYaw() % 360;
				if (userYaw < 0) {
					userYaw += 360;
				}
				if (targetYaw < 0) {
					targetYaw += 360;
				}
				failed = Math.abs(targetYaw - userYaw) > 120;
			}
			if (((ContractAccessor) user).hasContract(BWContracts.TREACHERY)) {
				failed = false;
			}
			else if (((ContractAccessor) entity).hasContract(BWContracts.TREACHERY) && ((ContractAccessor) entity).hasNegativeEffects()) {
				failed = false;
			}
			if (failed) {
				if (entity instanceof PlayerEntity) {
					((PlayerEntity) entity).sendMessage(new TranslatableText("bewitchment.taglock_fail", user.getDisplayName().getString()), false);
				}
				user.world.playSound(null, entity.getBlockPos(), BWSoundEvents.ENTITY_GENERIC_PLING, SoundCategory.PLAYERS, 1, 1);
				return ActionResult.FAIL;
			}
			boolean client = user.world.isClient;
			if (!client) {
				if (entity instanceof MobEntity) {
					((MobEntity) entity).setPersistent();
				}
				BWUtil.addItemToInventoryAndConsume(user, hand, putTaglock(new ItemStack(BWObjects.TAGLOCK), entity));
			}
			return ActionResult.success(client);
		}
		return ActionResult.FAIL;
	}
	
	public static ItemStack putTaglock(ItemStack stack, Entity entity) {
		stack.getOrCreateTag().putUuid("OwnerUUID", entity.getUuid());
		stack.getOrCreateTag().putString("OwnerName", entity.getDisplayName().getString());
		stack.getOrCreateTag().putBoolean("FromPlayer", entity instanceof PlayerEntity);
		return stack;
	}
	
	public static ItemStack copyTo(ItemStack from, ItemStack to) {
		if (hasTaglock(from)) {
			to.getOrCreateTag().putUuid("OwnerUUID", from.getOrCreateTag().getUuid("OwnerUUID"));
			to.getOrCreateTag().putString("OwnerName", from.getOrCreateTag().getString("OwnerName"));
			to.getOrCreateTag().putBoolean("FromPlayer", from.getOrCreateTag().getBoolean("FromPlayer"));
		}
		return to;
	}
	
	public static boolean hasTaglock(ItemStack stack) {
		return stack.hasTag() && stack.getOrCreateTag().contains("OwnerUUID");
	}
	
	public static void removeTaglock(ItemStack stack) {
		if (stack.hasTag()) {
			stack.getOrCreateTag().remove("OwnerUUID");
			stack.getOrCreateTag().remove("OwnerName");
			stack.getOrCreateTag().remove("FromPlayer");
		}
	}
	
	public static UUID getTaglockUUID(ItemStack stack) {
		if (hasTaglock(stack)) {
			return stack.getOrCreateTag().getUuid("OwnerUUID");
		}
		return null;
	}
	
	public static String getTaglockName(ItemStack stack) {
		if (hasTaglock(stack)) {
			return stack.getOrCreateTag().getString("OwnerName");
		}
		return "";
	}
	
	public static boolean isTaglockFromPlayer(ItemStack stack) {
		return hasTaglock(stack) && stack.getOrCreateTag().getBoolean("FromPlayer");
	}
}
