package moriyashiine.bewitchment.common.item;

import moriyashiine.bewitchment.common.Bewitchment;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("ConstantConditions")
public class WaystoneItem extends Item {
	public WaystoneItem(Settings settings) {
		super(settings);
	}
	
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		if (!(context.getStack().hasTag() && context.getStack().getOrCreateTag().contains("LocationPos"))) {
			World world = context.getWorld();
			boolean client = world.isClient;
			if (!client) {
				context.getStack().getOrCreateTag().putLong("LocationPos", context.getBlockPos().offset(context.getSide()).asLong());
				context.getStack().getOrCreateTag().putString("LocationWorld", world.getRegistryKey().getValue().toString());
			}
			return ActionResult.success(client);
		}
		return super.useOnBlock(context);
	}
	
	@Override
	public boolean isEnchantable(ItemStack stack) {
		return false;
	}
	
	@Environment(EnvType.CLIENT)
	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		if (stack.hasTag() && stack.getTag().contains("LocationPos")) {
			BlockPos pos = BlockPos.fromLong(stack.getTag().getLong("LocationPos"));
			tooltip.add(new TranslatableText(Bewitchment.MODID + ".location_tooltip", pos.getX(), pos.getY(), pos.getZ(), stack.getTag().getString("LocationWorld")).formatted(Formatting.GRAY));
		}
	}
}
