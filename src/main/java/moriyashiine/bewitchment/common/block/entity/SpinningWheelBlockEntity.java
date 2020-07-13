package moriyashiine.bewitchment.common.block.entity;

import moriyashiine.bewitchment.client.network.message.SyncSpinningRecipeMessage;
import moriyashiine.bewitchment.common.block.entity.util.BWCraftingBlockEntity;
import moriyashiine.bewitchment.common.container.SpinningWheelScreenHandler;
import moriyashiine.bewitchment.common.recipe.SpinningRecipe;
import moriyashiine.bewitchment.common.registry.BWBlockEntityTypes;
import moriyashiine.bewitchment.common.registry.BWObjects;
import moriyashiine.bewitchment.common.registry.BWRecipeTypes;
import net.fabricmc.fabric.api.server.PlayerStream;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Lazy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpinningWheelBlockEntity extends BWCraftingBlockEntity {
	private static final Text DEFAULT_NAME = new TranslatableText(BWObjects.spinning_wheel.getTranslationKey());
	
	private Lazy<SpinningRecipe> lazyRecipe = new Lazy<>(() -> null);
	
	public SpinningWheelBlockEntity() {
		super(BWBlockEntityTypes.spinning_wheel);
	}
	
	@Override
	public void fromTag(BlockState state, CompoundTag tag) {
		lazyRecipe = new Lazy<>(() -> (SpinningRecipe) Objects.requireNonNull(world).getRecipeManager().method_30027(BWRecipeTypes.spinning_type).stream().filter(recipe -> recipe.getId().toString().equals(tag.getString("Recipe"))).findFirst().orElse(null));
		super.fromTag(state, tag);
	}
	
	@Override
	public CompoundTag toTag(CompoundTag tag) {
		SpinningRecipe recipe = getRecipe();
		if (recipe != null) {
			tag.putString("Recipe", recipe.getId().toString());
		}
		return super.toTag(tag);
	}
	
	@Override
	public void setStack(int slot, ItemStack stack) {
		super.setStack(slot, stack);
		if (world != null) {
			SpinningRecipe actualRecipe = null;
			for (Recipe<?> recipe : world.getRecipeManager().method_30027(BWRecipeTypes.spinning_type)) {
				if (recipe instanceof SpinningRecipe) {
					SpinningRecipe foundRecipe = (SpinningRecipe) recipe;
					List<ItemStack> items = new ArrayList<>();
					for (int i = 0; i < size(); i++) {
						ItemStack stackInSlot = getStack(i);
						if (!stackInSlot.isEmpty()) {
							items.add(stackInSlot);
						}
					}
					if (items.size() == foundRecipe.input.size()) {
						for (Ingredient ingredient : foundRecipe.input) {
							for (int i = items.size() - 1; i >= 0; i--) {
								if (ingredient.test(items.get(i))) {
									items.remove(i);
								}
							}
						}
						if (items.isEmpty()) {
							actualRecipe = foundRecipe;
							break;
						}
					}
				}
			}
			setRecipe(actualRecipe);
			markDirty();
			PlayerStream.around(world, pos, 32).forEach(player -> SyncSpinningRecipeMessage.send(player, pos, getRecipe()));
		}
	}
	
	@Override
	protected Text getContainerName() {
		return DEFAULT_NAME;
	}
	
	@Override
	public void tick() {
		if (world != null) {
			SpinningRecipe recipe = getRecipe();
			//todo: drain
			if (recipe != null && canAcceptRecipeOutput()) {
				recipeTime++;
				if (recipeTime >= 200) {
					recipeTime = 0;
					if (!world.isClient) {
						world.playSound(null, pos, SoundEvents.BLOCK_WOOL_BREAK, SoundCategory.BLOCKS, 1, 1.5f);
						for (int i : INPUT_SLOTS) {
							removeStack(i, 1);
						}
						ItemStack output = getStack(4);
						if (output.isEmpty()) {
							output = recipe.output.copy();
						}
						else {
							output.increment(recipe.output.getCount());
						}
						setStack(4, output);
					}
				}
			}
			else if (recipeTime != 0) {
				recipeTime = 0;
				if (!world.isClient) {
					markDirty();
				}
			}
		}
	}
	
	@Override
	protected ScreenHandler createScreenHandler(int syncId, PlayerInventory playerInventory) {
		return new SpinningWheelScreenHandler(syncId, playerInventory, pos);
	}
	
	public SpinningRecipe getRecipe() {
		return lazyRecipe.get();
	}
	
	public void setRecipe(SpinningRecipe recipe) {
		lazyRecipe = new Lazy<>(() -> recipe);
	}
	
	private boolean canAcceptRecipeOutput() {
		ItemStack recipeOutput = getRecipe().output;
		ItemStack inventoryOutput = inventory.get(4);
		if (inventoryOutput.isEmpty()) {
			return true;
		}
		else if (!inventoryOutput.isItemEqualIgnoreDamage(recipeOutput)) {
			return false;
		}
		int count = inventoryOutput.getCount() + recipeOutput.getCount();
		return count <= recipeOutput.getMaxCount() && count <= getMaxCountPerStack();
	}
}