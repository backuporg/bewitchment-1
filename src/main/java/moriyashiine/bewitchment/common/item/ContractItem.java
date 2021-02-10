package moriyashiine.bewitchment.common.item;

import moriyashiine.bewitchment.api.interfaces.entity.ContractAccessor;
import moriyashiine.bewitchment.api.registry.Contract;
import moriyashiine.bewitchment.common.Bewitchment;
import moriyashiine.bewitchment.common.registry.BWRegistries;
import moriyashiine.bewitchment.common.registry.BWSoundEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.*;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("ConstantConditions")
public class ContractItem extends Item {
	public ContractItem(Settings settings) {
		super(settings);
	}
	
	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		return ItemUsage.consumeHeldItem(world, user, hand);
	}
	
	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		if (!world.isClient && stack.hasTag()) {
			MinecraftServer server = world.getServer();
			if (server != null) {
				UUID uuid = TaglockItem.getTaglockUUID(stack);
				if (uuid != null) {
					for (ServerWorld serverWorld : server.getWorlds()) {
						Entity entity = serverWorld.getEntity(uuid);
						if (entity != null) {
							Contract contract = BWRegistries.CONTRACTS.get(new Identifier(stack.getOrCreateTag().getString("Contract")));
							if (contract != null) {
								if (entity instanceof ContractAccessor) {
									((ContractAccessor) entity).addContract(new Contract.Instance(contract, 168000));
									contract.finishUsing(user, ((ContractAccessor) user).hasNegativeEffects());
									world.playSound(null, user.getBlockPos(), BWSoundEvents.ITEM_CONTRACT_USE, SoundCategory.PLAYERS, 1, 1);
									if (!(user instanceof PlayerEntity && ((PlayerEntity) user).isCreative())) {
										stack.decrement(1);
									}
								}
								return stack;
							}
						}
					}
					if (user instanceof PlayerEntity) {
						((PlayerEntity) user).sendMessage(new TranslatableText(Bewitchment.MODID + ".invalid_entity", TaglockItem.getTaglockName(stack)), true);
					}
				}
			}
		}
		return stack;
	}
	
	@Override
	public UseAction getUseAction(ItemStack stack) {
		return TaglockItem.hasTaglock(stack) ? UseAction.BOW : UseAction.NONE;
	}
	
	@Override
	public int getMaxUseTime(ItemStack stack) {
		return 32;
	}
	
	@Override
	public void appendStacks(ItemGroup group, DefaultedList<ItemStack> stacks) {
		if (isIn(group)) {
			BWRegistries.CONTRACTS.forEach(contract -> {
				ItemStack stack = new ItemStack(this);
				stack.getOrCreateTag().putString("Contract", BWRegistries.CONTRACTS.getId(contract).toString());
				stacks.add(stack);
			});
		}
	}
	
	@Environment(EnvType.CLIENT)
	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		if (TaglockItem.hasTaglock(stack)) {
			tooltip.add(new LiteralText(TaglockItem.getTaglockName(stack)).formatted(Formatting.GRAY));
		}
		if (stack.hasTag() && stack.getOrCreateTag().contains("Contract")) {
			tooltip.add(new TranslatableText("contract." + stack.getOrCreateTag().getString("Contract").replace(":", ".")).formatted(Formatting.DARK_RED));
		}
	}
}
