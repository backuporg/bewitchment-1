package moriyashiine.bewitchment.common.block.entity.interfaces;

import moriyashiine.bewitchment.client.network.packet.SyncClientSerializableBlockEntity;
import moriyashiine.bewitchment.client.network.packet.SyncTaglockHolderBlockEntity;
import moriyashiine.bewitchment.common.item.TaglockItem;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.UUID;

@SuppressWarnings("ConstantConditions")
public interface TaglockHolder {
	DefaultedList<ItemStack> getTaglockInventory();
	
	UUID getOwner();
	
	void setOwner(UUID owner);
	
	default void fromTagTaglock(CompoundTag tag) {
		Inventories.fromTag(tag.getCompound("TaglockInventory"), getTaglockInventory());
		if (tag.contains("Owner")) {
			setOwner(tag.getUuid("Owner"));
		}
	}
	
	default void toTagTaglock(CompoundTag tag) {
		tag.put("TaglockInventory", Inventories.toTag(new CompoundTag(), getTaglockInventory()));
		if (getOwner() != null) {
			tag.putUuid("Owner", getOwner());
		}
	}
	
	default ActionResult use(World world, BlockPos pos, LivingEntity user) {
		if (!user.getUuid().equals(getOwner())) {
			addTaglock(world, pos, user);
		}
		return ActionResult.PASS;
	}
	
	default void addTaglock(World world, BlockPos pos, Entity entity) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		TaglockHolder taglockHolder = (TaglockHolder) blockEntity;
		boolean found = false;
		for (ItemStack stack : taglockHolder.getTaglockInventory()) {
			if (stack.getItem() instanceof TaglockItem && TaglockItem.hasTaglock(stack) && entity.getUuid().equals(TaglockItem.getTaglockUUID(stack))) {
				found = true;
				break;
			}
		}
		if (!found) {
			for (int i = 0; i < taglockHolder.getTaglockInventory().size(); i++) {
				ItemStack stack = taglockHolder.getTaglockInventory().get(i);
				if (stack.getItem() instanceof TaglockItem && !TaglockItem.hasTaglock(stack)) {
					TaglockItem.putTaglock(stack, entity);
					syncTaglockHolder(world, blockEntity);
					blockEntity.markDirty();
					break;
				}
			}
		}
	}
	
	default int getFirstEmptySlot() {
		for (int i = 0; i < getTaglockInventory().size(); i++) {
			if (getTaglockInventory().get(i).isEmpty()) {
				return i;
			}
		}
		return -1;
	}
	
	default void syncTaglockHolder(World world, BlockEntity blockEntity) {
		if (world instanceof ServerWorld) {
			PlayerLookup.tracking(blockEntity).forEach(playerEntity -> {
				if (blockEntity instanceof BlockEntityClientSerializable) {
					SyncClientSerializableBlockEntity.send(playerEntity, (BlockEntityClientSerializable) blockEntity);
				}
				SyncTaglockHolderBlockEntity.send(playerEntity, blockEntity);
			});
		}
	}
	
	static ActionResult onUse(World world, BlockPos pos, LivingEntity user) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity instanceof TaglockHolder) {
			return ((TaglockHolder) blockEntity).use(world, pos, user);
		}
		return ActionResult.PASS;
	}
}
