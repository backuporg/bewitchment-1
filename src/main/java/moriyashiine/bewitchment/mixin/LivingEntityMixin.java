package moriyashiine.bewitchment.mixin;

import moriyashiine.bewitchment.api.BewitchmentAPI;
import moriyashiine.bewitchment.api.interfaces.entity.*;
import moriyashiine.bewitchment.api.registry.Contract;
import moriyashiine.bewitchment.api.registry.Curse;
import moriyashiine.bewitchment.client.network.packet.SpawnExplosionParticlesPacket;
import moriyashiine.bewitchment.client.network.packet.SpawnSmokeParticlesPacket;
import moriyashiine.bewitchment.common.block.entity.BrazierBlockEntity;
import moriyashiine.bewitchment.common.block.entity.GlyphBlockEntity;
import moriyashiine.bewitchment.common.block.entity.SigilBlockEntity;
import moriyashiine.bewitchment.common.block.entity.interfaces.SigilHolder;
import moriyashiine.bewitchment.common.entity.interfaces.CaduceusFireballAccessor;
import moriyashiine.bewitchment.common.entity.interfaces.InsanityTargetAccessor;
import moriyashiine.bewitchment.common.entity.interfaces.MasterAccessor;
import moriyashiine.bewitchment.common.entity.interfaces.WerewolfAccessor;
import moriyashiine.bewitchment.common.entity.living.*;
import moriyashiine.bewitchment.common.entity.living.util.BWHostileEntity;
import moriyashiine.bewitchment.common.item.AthameItem;
import moriyashiine.bewitchment.common.item.TaglockItem;
import moriyashiine.bewitchment.common.misc.BWUtil;
import moriyashiine.bewitchment.common.recipe.AthameDropRecipe;
import moriyashiine.bewitchment.common.recipe.IncenseRecipe;
import moriyashiine.bewitchment.common.registry.*;
import moriyashiine.bewitchment.common.world.BWUniversalWorldState;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.*;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.EntityDamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.EntityTypeTags;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ConstantConditions")
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements BloodAccessor, FamiliarAccessor, CurseAccessor, ContractAccessor {
	private static final TrackedData<Integer> BLOOD = DataTracker.registerData(LivingEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Boolean> IS_FAMILIAR = DataTracker.registerData(LivingEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	
	private final List<Curse.Instance> curses = new ArrayList<>();
	private final List<Contract.Instance> contracts = new ArrayList<>();
	
	@Shadow
	public abstract EntityGroup getGroup();
	
	@Shadow
	@Nullable
	public abstract StatusEffectInstance getStatusEffect(StatusEffect effect);
	
	@Shadow
	public abstract boolean hasStatusEffect(StatusEffect effect);
	
	@Shadow
	public abstract boolean addStatusEffect(StatusEffectInstance effect);
	
	@Shadow
	public abstract boolean removeStatusEffect(StatusEffect type);
	
	@Shadow
	public abstract boolean clearStatusEffects();
	
	@Shadow
	public abstract float getMaxHealth();
	
	@Shadow
	public abstract float getHealth();
	
	@Shadow
	public abstract void heal(float amount);
	
	@Shadow
	public abstract void setHealth(float health);
	
	@Shadow
	public abstract boolean isSleeping();
	
	@Shadow
	protected abstract float getSoundVolume();
	
	@Shadow
	protected abstract float getSoundPitch();
	
	@Shadow
	public float flyingSpeed;
	
	public LivingEntityMixin(EntityType<?> type, World world) {
		super(type, world);
	}
	
	@Override
	public int getBlood() {
		return dataTracker.get(BLOOD);
	}
	
	@Override
	public void setBlood(int blood) {
		dataTracker.set(BLOOD, blood);
	}
	
	@Override
	public boolean getFamiliar() {
		return dataTracker.get(IS_FAMILIAR);
	}
	
	@Override
	public void setFamiliar(boolean familiar) {
		dataTracker.set(IS_FAMILIAR, familiar);
	}
	
	@Override
	public List<Curse.Instance> getCurses() {
		return curses;
	}
	
	@Override
	public List<Contract.Instance> getContracts() {
		return contracts;
	}
	
	@Override
	public boolean hasNegativeEffects() {
		return !world.isClient && !BewitchmentAPI.isPledged(world, BWPledges.BAPHOMET, getUuid());
	}
	
	@Inject(method = "tick", at = @At("HEAD"))
	private void tick(CallbackInfo callbackInfo) {
		if (!world.isClient) {
			LivingEntity livingEntity = (LivingEntity) (Object) this;
			int damage = 0;
			if (BewitchmentAPI.isWeakToSilver(livingEntity)) {
				damage += BWUtil.getArmorPieces(livingEntity, stack -> BWTags.SILVER_ARMOR.contains(stack.getItem()));
				if (BewitchmentAPI.isHoldingSilver(livingEntity, Hand.MAIN_HAND)) {
					damage++;
				}
				if (BewitchmentAPI.isHoldingSilver(livingEntity, Hand.OFF_HAND)) {
					damage++;
				}
			}
			if (livingEntity.getMainHandStack().getItem() == BWObjects.GARLIC && BewitchmentAPI.isVampire(this, true)) {
				damage++;
			}
			if (livingEntity.getOffHandStack().getItem() == BWObjects.GARLIC && BewitchmentAPI.isVampire(this, true)) {
				damage++;
			}
			if (livingEntity.getMainHandStack().getItem() == BWObjects.ACONITE && BewitchmentAPI.isWerewolf(this, true)) {
				damage++;
			}
			if (livingEntity.getOffHandStack().getItem() == BWObjects.ACONITE && BewitchmentAPI.isWerewolf(this, true)) {
				damage++;
			}
			if (damage > 0) {
				damage(BWDamageSources.MAGIC_COPY, damage);
			}
			if (BWTags.HAS_BLOOD.contains(getType()) && !BewitchmentAPI.isVampire(this, true)) {
				if (random.nextFloat() < (isSleeping() ? 1 / 50f : 1 / 500f)) {
					fillBlood(1, false);
				}
			}
			for (int i = curses.size() - 1; i >= 0; i--) {
				Curse.Instance instance = curses.get(i);
				instance.curse.tick(livingEntity);
				instance.duration--;
				if (instance.duration <= 0) {
					curses.remove(i);
				}
			}
			for (int i = contracts.size() - 1; i >= 0; i--) {
				Contract.Instance instance = contracts.get(i);
				instance.contract.tick(livingEntity, hasNegativeEffects());
				instance.duration--;
				if (instance.duration <= 0) {
					contracts.remove(i);
				}
			}
		}
		if (getFamiliar()) {
			if (!world.isClient) {
				if (age % 100 == 0) {
					heal(1);
				}
			}
			else {
				world.addParticle(ParticleTypes.ENCHANT, getParticleX(getWidth()), getY() + MathHelper.nextFloat(random, 0, getHeight()), getParticleZ(getWidth()), 0, 0, 0);
			}
		}
		if ((Object) this instanceof PlayerEntity && BewitchmentAPI.isWerewolf(this, false)) {
			flyingSpeed *= 1.5f;
		}
	}
	
	@ModifyVariable(method = "addStatusEffect", at = @At("HEAD"))
	private StatusEffectInstance modifyStatusEffect(StatusEffectInstance effect) {
		if (!world.isClient && !effect.isAmbient()) {
			StatusEffectType type = ((StatusEffectAccessor) effect.getEffectType()).bw_getType();
			if ((type == StatusEffectType.HARMFUL && hasCurse(BWCurses.COMPROMISED)) || (type == StatusEffectType.BENEFICIAL && (Object) this instanceof PlayerEntity && BewitchmentAPI.getFamiliar((PlayerEntity) (Object) this) == BWEntityTypes.TOAD)) {
				return new StatusEffectInstance(effect.getEffectType(), effect.getDuration(), effect.getAmplifier() + 1, false, effect.shouldShowParticles(), effect.shouldShowIcon());
			}
		}
		return effect;
	}
	
	@ModifyVariable(method = "applyDamage", at = @At(value = "INVOKE", shift = At.Shift.BEFORE, target = "Lnet/minecraft/entity/LivingEntity;getHealth()F"))
	private float modifyDamage0(float amount, DamageSource source) {
		if (!world.isClient) {
			amount = BWDamageSources.handleDamage((LivingEntity) (Object) this, source, amount);
		}
		return amount;
	}
	
	@ModifyVariable(method = "applyArmorToDamage", at = @At("HEAD"))
	private float modifyDamage1(float amount, DamageSource source) {
		if (!world.isClient) {
			Entity trueSource = source.getAttacker();
			Entity directSource = source.getSource();
			if (!source.isOutOfWorld() && (hasStatusEffect(BWStatusEffects.ETHEREAL) || (directSource instanceof LivingEntity && ((LivingEntity) directSource).hasStatusEffect(BWStatusEffects.ETHEREAL)))) {
				return 0;
			}
			if (!source.isOutOfWorld()) {
				if (!BWUtil.getBlockPoses(getBlockPos(), 16, currentPos -> world.getBlockEntity(currentPos) instanceof GlyphBlockEntity && ((GlyphBlockEntity) world.getBlockEntity(currentPos)).ritualFunction == BWRitualFunctions.PREVENT_DAMAGE).isEmpty()) {
					return 0;
				}
			}
			if (amount > 0 && (Object) this instanceof PlayerEntity && !BewitchmentAPI.isVampire(this, true)) {
				ItemStack poppet = BewitchmentAPI.getPoppet(world, BWObjects.VAMPIRIC_POPPET, null, (PlayerEntity) (Object) this);
				if (!poppet.isEmpty()) {
					LivingEntity owner = BewitchmentAPI.getTaglockOwner(world, poppet);
					if (!BewitchmentAPI.isVampire(owner, true) && !getUuid().equals(owner.getUuid()) && owner.damage(BWDamageSources.VAMPIRE, amount)) {
						if (poppet.damage((int) (amount * (BewitchmentAPI.getFamiliar((PlayerEntity) (Object) this) == EntityType.WOLF && random.nextBoolean() ? 0.5f : 1)), random, null) && poppet.getDamage() >= poppet.getMaxDamage()) {
							poppet.decrement(1);
						}
						return 0;
					}
				}
			}
			if (source.isFire() || source == DamageSource.DROWN || source == DamageSource.FALL || source == DamageSource.FLY_INTO_WALL) {
				ItemStack poppet = BewitchmentAPI.getPoppet(world, BWObjects.PROTECTION_POPPET, this, null);
				if (!poppet.isEmpty()) {
					if (poppet.damage((int) (amount * ((Object) this instanceof PlayerEntity && BewitchmentAPI.getFamiliar((PlayerEntity) (Object) this) == EntityType.WOLF && random.nextBoolean() ? 0.5f : 1)), random, null) && poppet.getDamage() >= poppet.getMaxDamage()) {
						poppet.decrement(1);
					}
					return 0;
				}
			}
			if (directSource instanceof FireballEntity) {
				if (trueSource instanceof PlayerEntity && ((CaduceusFireballAccessor) directSource).getFromCaduceus()) {
					amount *= 2;
				}
				if (trueSource instanceof BaphometEntity) {
					amount *= 3;
				}
			}
			if (directSource instanceof WitherSkullEntity && trueSource instanceof LilithEntity) {
				amount *= 3;
			}
			if (amount > 0 && trueSource instanceof FortuneAccessor && ((FortuneAccessor) trueSource).getFortune() != null && ((FortuneAccessor) trueSource).getFortune().fortune == BWFortunes.HAWKEYE && source.isProjectile()) {
				((FortuneAccessor) trueSource).getFortune().duration = 0;
				amount *= 3;
			}
			if (trueSource instanceof PlayerEntity && BewitchmentAPI.isWeakToSilver((LivingEntity) trueSource)) {
				ItemStack poppet = BewitchmentAPI.getPoppet(world, BWObjects.JUDGMENT_POPPET, this, null);
				if (!poppet.isEmpty()) {
					if (poppet.damage((Object) this instanceof PlayerEntity && BewitchmentAPI.getFamiliar((PlayerEntity) (Object) this) == EntityType.WOLF && random.nextBoolean() ? 0 : 1, random, null) && poppet.getDamage() >= poppet.getMaxDamage()) {
						poppet.decrement(1);
					}
					amount /= 4;
				}
			}
			if (directSource instanceof LivingEntity) {
				ItemStack poppet = BewitchmentAPI.getPoppet(world, BWObjects.FATIGUE_POPPET, this, null);
				if (!poppet.isEmpty() && ((LivingEntity) directSource).addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 1)) && poppet.damage((Object) this instanceof PlayerEntity && BewitchmentAPI.getFamiliar((PlayerEntity) (Object) this) == EntityType.WOLF && random.nextBoolean() ? 0 : 1, random, null) && poppet.getDamage() >= poppet.getMaxDamage()) {
					poppet.decrement(1);
				}
			}
			if (directSource instanceof LivingEntity && ((LivingEntity) directSource).hasStatusEffect(BWStatusEffects.ENCHANTED)) {
				amount /= 4;
				amount += ((LivingEntity) directSource).getStatusEffect(BWStatusEffects.ENCHANTED).getAmplifier();
			}
			if (hasStatusEffect(BWStatusEffects.MAGIC_SPONGE) && source.getMagic()) {
				float magicAmount = (0.3f + (0.1f * getStatusEffect(BWStatusEffects.MAGIC_SPONGE).getAmplifier()));
				amount *= (1 - magicAmount);
				if (this instanceof MagicAccessor) {
					((MagicAccessor) this).fillMagic((int) (amount * magicAmount), false);
				}
			}
			if (!(source instanceof EntityDamageSource && ((EntityDamageSource) source).isThorns())) {
				if (directSource instanceof LivingEntity) {
					LivingEntity livingAttacker = (LivingEntity) directSource;
					if (BewitchmentAPI.isWeakToSilver(livingAttacker)) {
						int armorPieces = BWUtil.getArmorPieces(livingAttacker, stack -> BWTags.SILVER_ARMOR.contains(stack.getItem()));
						if (armorPieces > 0) {
							directSource.damage(DamageSource.thorns(this), armorPieces);
						}
						amount *= (1 - (0.125f * armorPieces));
					}
				}
			}
			if (source == DamageSource.FALL) {
				BlockPos sigilPos = BWUtil.getClosestBlockPos(getBlockPos(), 16, currentPos -> world.getBlockEntity(currentPos) instanceof SigilHolder && ((SigilHolder) world.getBlockEntity(currentPos)).getSigil() == BWSigils.HEAVY);
				if (sigilPos != null) {
					BlockEntity blockEntity = world.getBlockEntity(sigilPos);
					SigilHolder sigil = (SigilHolder) blockEntity;
					if (sigil.test(this)) {
						sigil.setUses(sigil.getUses() - 1);
						blockEntity.markDirty();
						amount *= 3;
					}
				}
			}
			if (hasCurse(BWCurses.FORESTS_WRATH) && (source.isFire() || ((directSource instanceof LivingEntity && ((LivingEntity) directSource).getMainHandStack().getItem() instanceof AxeItem)))) {
				amount *= 2;
			}
			if (hasCurse(BWCurses.SUSCEPTIBILITY) && (source.isFire() || (directSource instanceof LivingEntity && (((LivingEntity) directSource).getGroup() == EntityGroup.AQUATIC || ((LivingEntity) directSource).getGroup() == BewitchmentAPI.DEMON)))) {
				amount *= 2;
			}
			if (((Object) this instanceof PlayerEntity) && hasContract(BWContracts.FAMINE)) {
				amount *= (1 - (0.025f * (20 - ((PlayerEntity) (Object) this).getHungerManager().getFoodLevel())));
			}
			if (directSource instanceof PlayerEntity && ((ContractAccessor) directSource).hasNegativeEffects() && ((ContractAccessor) directSource).hasContract(BWContracts.FAMINE)) {
				amount *= (1 - (0.025f * (20 - ((PlayerEntity) directSource).getHungerManager().getFoodLevel())));
			}
			if (directSource instanceof LivingEntity && ((ContractAccessor) directSource).hasContract(BWContracts.WRATH)) {
				amount *= (1 + (0.025f * (((LivingEntity) directSource).getMaxHealth() - ((LivingEntity) directSource).getHealth())));
			}
			if (hasNegativeEffects() && hasContract(BWContracts.WRATH)) {
				amount *= (1 + (0.025f * (getMaxHealth() - getHealth())));
			}
			if (source.getMagic() && (Object) this instanceof LivingEntity) {
				int armorPieces = BWUtil.getArmorPieces((LivingEntity) (Object) this, stack -> {
					if (stack.getItem() instanceof ArmorItem) {
						ArmorMaterial material = ((ArmorItem) stack.getItem()).getMaterial();
						return material == BWMaterials.HEDGEWITCH_ARMOR || material == BWMaterials.ALCHEMIST_ARMOR || material == BWMaterials.BESMIRCHED_ARMOR || material == BWMaterials.HARBINGER_ARMOR;
					}
					return false;
				});
				if (armorPieces > 0) {
					amount *= (1 - (0.2f * armorPieces));
				}
			}
			if (getFamiliar()) {
				amount /= 8;
			}
			if (BewitchmentAPI.isVampire(directSource, false)) {
				amount /= 8;
			}
			if ((getVehicle() != null && getVehicle().getType() == BWEntityTypes.CYPRESS_BROOM) || (source.getAttacker() != null && source.getAttacker().getVehicle() != null && source.getAttacker().getVehicle().getType() == BWEntityTypes.CYPRESS_BROOM)) {
				amount *= 0.2f;
			}
		}
		return amount;
	}
	
	@ModifyVariable(method = "damage", at = @At("HEAD"))
	private DamageSource modifyDamage2(DamageSource source) {
		if (!world.isClient) {
			Entity attacker = source.getSource();
			if (attacker instanceof LivingEntity && ((LivingEntity) attacker).hasStatusEffect(BWStatusEffects.ENCHANTED)) {
				return attacker instanceof PlayerEntity ? new BWDamageSources.MagicPlayer(attacker) : new BWDamageSources.MagicMob(attacker);
			}
		}
		return source;
	}
	
	@Inject(method = "damage", at = @At("HEAD"), cancellable = true)
	private void damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (!world.isClient) {
			if (BewitchmentAPI.isVampire(this, true) && source.isFire()) {
				callbackInfo.setReturnValue(damage(BWDamageSources.SUN, amount * 2));
				return;
			}
			if (this instanceof InsanityTargetAccessor) {
				if (((InsanityTargetAccessor) this).getInsanityTargetUUID().isPresent()) {
					callbackInfo.setReturnValue(false);
					return;
				}
			}
			Entity trueSource = source.getAttacker();
			if (trueSource instanceof LeonardEntity) {
				removeStatusEffect(BWStatusEffects.MAGIC_SPONGE);
			}
			if (trueSource instanceof BaphometEntity) {
				removeStatusEffect(StatusEffects.FIRE_RESISTANCE);
			}
			if (trueSource instanceof LilithEntity) {
				removeStatusEffect(StatusEffects.STRENGTH);
			}
			if (trueSource instanceof HerneEntity) {
				removeStatusEffect(StatusEffects.RESISTANCE);
			}
			if (trueSource instanceof Pledgeable) {
				((Pledgeable) trueSource).setTimeSinceLastAttack(0);
			}
			Entity directSource = source.getSource();
			if (directSource instanceof PlayerEntity) {
				ItemStack stack = ((PlayerEntity) directSource).getMainHandStack();
				if (stack.getItem() instanceof TaglockItem) {
					TaglockItem.useTaglock((PlayerEntity) directSource, (LivingEntity) (Object) this, Hand.MAIN_HAND, true, false);
					callbackInfo.setReturnValue(false);
					return;
				}
			}
			if (hasStatusEffect(BWStatusEffects.DEFLECTION) && directSource != null && EntityTypeTags.ARROWS.contains(directSource.getType())) {
				int amplifier = getStatusEffect(BWStatusEffects.DEFLECTION).getAmplifier() + 1;
				Vec3d velocity = directSource.getVelocity();
				directSource.setVelocity(velocity.getX() * 2 * amplifier, velocity.getY() * 2 * amplifier, velocity.getZ() * 2 * amplifier);
				callbackInfo.setReturnValue(false);
				return;
			}
			if (amount > 0) {
				if (hasStatusEffect(BWStatusEffects.LEECHING)) {
					heal(amount * (getStatusEffect(BWStatusEffects.LEECHING).getAmplifier() + 1) / 4);
				}
				if (hasStatusEffect(BWStatusEffects.THORNS) && !(source instanceof EntityDamageSource && ((EntityDamageSource) source).isThorns())) {
					directSource.damage(DamageSource.thorns(directSource), 2 * (getStatusEffect(BWStatusEffects.THORNS).getAmplifier() + 1));
				}
				if (hasStatusEffect(BWStatusEffects.VOLATILITY) && !source.isExplosive()) {
					for (LivingEntity entity : world.getEntitiesByClass(LivingEntity.class, getBoundingBox().expand(3), LivingEntity::isAlive)) {
						entity.damage(DamageSource.explosion(((LivingEntity) (Object) this)), 4 * (getStatusEffect(BWStatusEffects.VOLATILITY).getAmplifier() + 1));
					}
					world.playSound(null, getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.NEUTRAL, 1, 1);
					PlayerLookup.tracking(this).forEach(playerEntity -> SpawnExplosionParticlesPacket.send(playerEntity, this));
					if (((Object) this) instanceof PlayerEntity) {
						SpawnExplosionParticlesPacket.send((PlayerEntity) (Object) this, this);
					}
					removeStatusEffect(BWStatusEffects.VOLATILITY);
				}
				if (directSource instanceof ContractAccessor && ((ContractAccessor) directSource).hasContract(BWContracts.PESTILENCE)) {
					addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100));
					addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100));
				}
				if (source == DamageSource.DROWN && (Object) this instanceof PlayerEntity) {
					ItemStack poppet = BewitchmentAPI.getPoppet(world, BWObjects.VOODOO_POPPET, null, (PlayerEntity) (Object) this);
					if (!poppet.isEmpty()) {
						LivingEntity owner = BewitchmentAPI.getTaglockOwner(world, poppet);
						if (!owner.getUuid().equals(getUuid())) {
							if (poppet.damage(BewitchmentAPI.getFamiliar((PlayerEntity) (Object) this) == EntityType.WOLF && random.nextBoolean() ? 0 : 1, random, null) && poppet.getDamage() >= poppet.getMaxDamage()) {
								poppet.decrement(1);
							}
							ItemStack potentialPoppet = BewitchmentAPI.getPoppet(world, BWObjects.VOODOO_PROTECTION_POPPET, owner, null);
							if (!potentialPoppet.isEmpty()) {
								if (potentialPoppet.damage(owner instanceof PlayerEntity && BewitchmentAPI.getFamiliar((PlayerEntity) owner) == EntityType.WOLF && random.nextBoolean() ? 0 : 1, random, null) && potentialPoppet.getDamage() >= potentialPoppet.getMaxDamage()) {
									potentialPoppet.decrement(1);
								}
								return;
							}
							owner.damage(source, amount);
						}
					}
				}
			}
		}
	}
	
	@Inject(method = "getJumpVelocity", at = @At("RETURN"), cancellable = true)
	private void getJumpVelocity(CallbackInfoReturnable<Float> callbackInfo) {
		if ((Object) this instanceof PlayerEntity && BewitchmentAPI.isWerewolf(this, false)) {
			callbackInfo.setReturnValue(callbackInfo.getReturnValue() * 1.5f);
		}
	}
	
	@Inject(method = "getGroup", at = @At("HEAD"), cancellable = true)
	private void getGroup(CallbackInfoReturnable<EntityGroup> callbackInfo) {
		if (((Object) this instanceof PlayerEntity) && BewitchmentAPI.isVampire(this, true)) {
			callbackInfo.setReturnValue(EntityGroup.UNDEAD);
		}
	}
	
	@Inject(method = "tryUseTotem", at = @At("HEAD"), cancellable = true)
	private void tryUseTotem0(DamageSource source, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (!world.isClient && BewitchmentAPI.isVampire(this, true)) {
			callbackInfo.setReturnValue(false);
		}
	}
	
	@Inject(method = "tryUseTotem", at = @At("RETURN"), cancellable = true)
	private void tryUseTotem1(DamageSource source, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (!world.isClient && !BewitchmentAPI.isVampire(this, true)) {
			if (!callbackInfo.getReturnValue()) {
				ItemStack poppet = BewitchmentAPI.getPoppet(world, BWObjects.DEATH_PROTECTION_POPPET, this, null);
				if (!poppet.isEmpty()) {
					if (poppet.damage((Object) this instanceof PlayerEntity && BewitchmentAPI.getFamiliar((PlayerEntity) (Object) this) == EntityType.WOLF && random.nextBoolean() ? 0 : 1, random, null) && poppet.getDamage() >= poppet.getMaxDamage()) {
						poppet.decrement(1);
					}
					setHealth(1);
					clearStatusEffects();
					addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 900, 1));
					addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 100, 1));
					addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 800, 0));
					callbackInfo.setReturnValue(true);
				}
			}
			if (callbackInfo.getReturnValue() && this instanceof TransformationAccessor && hasCurse(BWCurses.SUSCEPTIBILITY)) {
				if (((TransformationAccessor) this).getTransformation() == BWTransformations.HUMAN) {
					if (source.getSource() instanceof VampireEntity || (BewitchmentAPI.isVampire(source.getSource(), true) && BewitchmentAPI.isPledged(world, BWPledges.LILITH, source.getSource().getUuid()))) {
						((TransformationAccessor) this).getTransformation().onRemoved((LivingEntity) (Object) this);
						((TransformationAccessor) this).setTransformation(BWTransformations.VAMPIRE);
						((TransformationAccessor) this).getTransformation().onAdded((LivingEntity) (Object) this);
						PlayerLookup.tracking(this).forEach(foundPlayer -> SpawnSmokeParticlesPacket.send(foundPlayer, this));
						if ((Object) this instanceof PlayerEntity) {
							SpawnSmokeParticlesPacket.send((PlayerEntity) (Object) this, this);
						}
						world.playSound(null, getBlockPos(), BWSoundEvents.ENTITY_GENERIC_CURSE, getSoundCategory(), getSoundVolume(), getSoundPitch());
					}
					else if (BewitchmentAPI.isWerewolf(source.getSource(), false)) {
						((TransformationAccessor) this).getTransformation().onRemoved((LivingEntity) (Object) this);
						((TransformationAccessor) this).setTransformation(BWTransformations.WEREWOLF);
						((TransformationAccessor) this).getTransformation().onAdded((LivingEntity) (Object) this);
						int variant = -1;
						if (source.getSource() instanceof WerewolfEntity) {
							variant = source.getSource().getDataTracker().get(BWHostileEntity.VARIANT);
						}
						else if (source.getSource() instanceof WerewolfAccessor && BewitchmentAPI.isPledged(world, BWPledges.HERNE, source.getSource().getUuid())) {
							variant = ((WerewolfAccessor) source.getSource()).getWerewolfVariant();
						}
						if (variant > -1) {
							((WerewolfAccessor) this).setWerewolfVariant(variant);
						}
						PlayerLookup.tracking(this).forEach(foundPlayer -> SpawnSmokeParticlesPacket.send(foundPlayer, this));
						if ((Object) this instanceof PlayerEntity) {
							SpawnSmokeParticlesPacket.send((PlayerEntity) (Object) this, this);
						}
						world.playSound(null, getBlockPos(), BWSoundEvents.ENTITY_GENERIC_CURSE, getSoundCategory(), getSoundVolume(), getSoundPitch());
					}
				}
			}
		}
	}
	
	@Inject(method = "isAffectedBySplashPotions", at = @At("HEAD"), cancellable = true)
	private void isAffectedBySplashPotions(CallbackInfoReturnable<Boolean> callbackInfo) {
		if (!world.isClient) {
			if (this instanceof MasterAccessor && ((MasterAccessor) this).getMasterUUID() != null) {
				callbackInfo.setReturnValue(false);
			}
		}
	}
	
	@Inject(method = "canHaveStatusEffect", at = @At("RETURN"), cancellable = true)
	private void canHaveStatusEffect(StatusEffectInstance effect, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (callbackInfo.getReturnValue() && !world.isClient && !effect.isAmbient()) {
			StatusEffectType type = ((StatusEffectAccessor) effect.getEffectType()).bw_getType();
			if (type == StatusEffectType.BENEFICIAL) {
				if (hasCurse(BWCurses.UNLUCKY) && random.nextBoolean()) {
					callbackInfo.setReturnValue(false);
				}
			}
			if (type != StatusEffectType.HARMFUL) {
				BlockPos sigilPos = BWUtil.getClosestBlockPos(getBlockPos(), 16, currentPos -> world.getBlockEntity(currentPos) instanceof SigilHolder && ((SigilHolder) world.getBlockEntity(currentPos)).getSigil() == BWSigils.RUIN);
				if (sigilPos != null) {
					BlockEntity blockEntity = world.getBlockEntity(sigilPos);
					SigilHolder sigil = (SigilHolder) blockEntity;
					if (sigil.test(this)) {
						sigil.setUses(sigil.getUses() - 1);
						blockEntity.markDirty();
						callbackInfo.setReturnValue(false);
					}
				}
			}
		}
	}
	
	@Inject(method = "canBreatheInWater", at = @At("RETURN"), cancellable = true)
	private void canBreatheInWater(CallbackInfoReturnable<Boolean> callbackInfo) {
		if (callbackInfo.getReturnValue() && !world.isClient && hasStatusEffect(BWStatusEffects.GILLS)) {
			callbackInfo.setReturnValue(true);
		}
	}
	
	@Inject(method = "isClimbing", at = @At("RETURN"), cancellable = true)
	private void isClimbing(CallbackInfoReturnable<Boolean> callbackInfo) {
		if (!callbackInfo.getReturnValue() && hasStatusEffect(BWStatusEffects.CLIMBING) && horizontalCollision) {
			callbackInfo.setReturnValue(true);
		}
	}
	
	@Inject(method = "blockedByShield", at = @At("HEAD"), cancellable = true)
	private void blockedByShield(DamageSource source, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (BewitchmentAPI.isWerewolf(source.getSource(), false)) {
			callbackInfo.setReturnValue(false);
		}
	}
	
	@Inject(method = "handleFallDamage", at = @At("HEAD"), cancellable = true)
	private void handleFallDamage(float fallDistance, float damageMultiplier, CallbackInfoReturnable<Boolean> callbackInfo) {
		if ((Object) this instanceof PlayerEntity && BewitchmentAPI.getFamiliar((PlayerEntity) (Object) this) == BWEntityTypes.OWL) {
			callbackInfo.setReturnValue(false);
		}
	}
	
	@Inject(method = "fall", at = @At("HEAD"), cancellable = true)
	private void fall(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition, CallbackInfo callbackInfo) {
		if ((Object) this instanceof PlayerEntity && BewitchmentAPI.getFamiliar((PlayerEntity) (Object) this) == BWEntityTypes.OWL) {
			callbackInfo.cancel();
		}
	}
	
	@Inject(method = "dropEquipment", at = @At("TAIL"))
	private void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops, CallbackInfo callbackInfo) {
		if (!world.isClient && allowDrops) {
			Entity attacker = source.getSource();
			if (attacker instanceof LivingEntity) {
				LivingEntity livingAttacker = (LivingEntity) attacker;
				if (livingAttacker.getMainHandStack().getItem() instanceof AthameItem && livingAttacker.preferredHand == Hand.MAIN_HAND) {
					for (AthameDropRecipe recipe : world.getRecipeManager().listAllOfType(BWRecipeTypes.ATHAME_DROP_RECIPE_TYPE)) {
						if (recipe.entity_type.equals(getType()) && world.random.nextFloat() < recipe.chance * (lootingMultiplier + 1)) {
							ItemStack drop = recipe.getOutput().copy();
							if (recipe.entity_type == EntityType.PLAYER) {
								drop.getOrCreateTag().putString("SkullOwner", getName().getString());
							}
							ItemScatterer.spawn(world, getX() + 0.5, getY() + 0.5, getZ() + 0.5, drop);
						}
					}
					if (livingAttacker instanceof PlayerEntity && livingAttacker.getOffHandStack().getItem() == Items.GLASS_BOTTLE && getBlood() > 30) {
						world.playSound(null, attacker.getBlockPos(), SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.PLAYERS, 1, 0.5f);
						BWUtil.addItemToInventoryAndConsume((PlayerEntity) livingAttacker, Hand.OFF_HAND, new ItemStack(BWObjects.BOTTLE_OF_BLOOD));
					}
				}
			}
		}
	}
	
	@Inject(method = "onDeath", at = @At("TAIL"))
	private void onDeath(DamageSource source, CallbackInfo callbackInfo) {
		if (!world.isClient) {
			Entity attacker = source.getSource();
			if (attacker instanceof PlayerEntity) {
				PlayerEntity player = (PlayerEntity) attacker;
				ItemStack stack = player.getMainHandStack();
				if (stack.getItem() instanceof AthameItem && player.preferredHand == Hand.MAIN_HAND) {
					BlockPos glyph = BWUtil.getClosestBlockPos(getBlockPos(), 6, currentPos -> world.getBlockEntity(currentPos) instanceof GlyphBlockEntity);
					if (glyph != null) {
						((GlyphBlockEntity) world.getBlockEntity(glyph)).onUse(world, glyph, player, Hand.MAIN_HAND, (LivingEntity) (Object) this);
					}
					if (world.isNight()) {
						boolean chicken = (Object) this instanceof ChickenEntity;
						if ((chicken && world.getBiome(getBlockPos()).getCategory() == Biome.Category.EXTREME_HILLS) || ((Object) this instanceof WolfEntity && world.getBiome(getBlockPos()).getCategory() == Biome.Category.FOREST)) {
							BlockPos brazierPos = BWUtil.getClosestBlockPos(getBlockPos(), 8, currentPos -> {
								BlockEntity blockEntity = world.getBlockEntity(currentPos);
								return blockEntity instanceof BrazierBlockEntity && ((BrazierBlockEntity) blockEntity).incenseRecipe.effect == BWStatusEffects.MORTAL_COIL;
							});
							if (brazierPos != null) {
								world.createExplosion(this, brazierPos.getX() + 0.5, brazierPos.getY() + 0.5, brazierPos.getZ() + 0.5, 3, Explosion.DestructionType.DESTROY);
								Entity entity = chicken ? BWEntityTypes.LILITH.create(world) : BWEntityTypes.HERNE.create(world);
								if (entity instanceof MobEntity) {
									((MobEntity) entity).initialize((ServerWorldAccess) world, world.getLocalDifficulty(brazierPos), SpawnReason.EVENT, null, null);
									entity.updatePositionAndAngles(brazierPos.getX() + 0.5, brazierPos.getY(), brazierPos.getZ() + 0.5, world.random.nextFloat() * 360, 0);
									world.spawnEntity(entity);
								}
							}
						}
					}
				}
				if (getGroup() == EntityGroup.ARTHROPOD && BewitchmentAPI.getFamiliar(player) == BWEntityTypes.TOAD) {
					player.heal(player.getMaxHealth() * 1 / 4f);
				}
				if (BewitchmentAPI.isWerewolf(player, false)) {
					player.getHungerManager().add(4, 1);
				}
			}
			if (attacker instanceof LivingEntity) {
				if (((ContractAccessor) attacker).hasContract(BWContracts.VIOLENCE)) {
					((LivingEntity) attacker).heal(((LivingEntity) attacker).getMaxHealth() * 1 / 4f);
					if (((ContractAccessor) attacker).hasNegativeEffects()) {
						((LivingEntity) attacker).addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER, 600, 2));
					}
				}
			}
			BWUniversalWorldState worldState = BWUniversalWorldState.get(world);
			for (int i = worldState.familiars.size() - 1; i >= 0; i--) {
				if (getUuid().equals(worldState.familiars.get(i).getRight().getUuid("UUID"))) {
					worldState.familiars.remove(i);
					worldState.markDirty();
					break;
				}
			}
		}
	}
	
	@Inject(method = "wakeUp", at = @At("TAIL"))
	private void wakeUp(CallbackInfo callbackInfo) {
		if (!world.isClient) {
			BWUtil.getBlockPoses(getBlockPos(), 12, foundPos -> world.getBlockEntity(foundPos) instanceof BrazierBlockEntity && ((BrazierBlockEntity) world.getBlockEntity(foundPos)).incenseRecipe != null).forEach(foundPos -> {
				IncenseRecipe recipe = ((BrazierBlockEntity) world.getBlockEntity(foundPos)).incenseRecipe;
				int durationMultiplier = 1;
				BlockPos nearestSigil = BWUtil.getClosestBlockPos(getBlockPos(), 16, foundSigil -> world.getBlockEntity(foundSigil) instanceof SigilBlockEntity && ((SigilBlockEntity) world.getBlockEntity(foundSigil)).getSigil() == BWSigils.EXTENDING);
				if (nearestSigil != null) {
					BlockEntity blockEntity = world.getBlockEntity(nearestSigil);
					SigilHolder sigil = ((SigilHolder) blockEntity);
					if (sigil.test(this)) {
						sigil.setUses(sigil.getUses() - 1);
						blockEntity.markDirty();
						durationMultiplier = 2;
					}
				}
				addStatusEffect(new StatusEffectInstance(recipe.effect, 24000 * durationMultiplier, recipe.amplifier, true, false));
			});
		}
	}
	
	@Inject(method = "addStatusEffect", at = @At("HEAD"))
	private void addStatusEffect(StatusEffectInstance effect, CallbackInfoReturnable<Boolean> callbackInfo) {
		if (!world.isClient && !effect.isAmbient() && (Object) this instanceof PlayerEntity) {
			ItemStack poppet = BewitchmentAPI.getPoppet(world, BWObjects.VOODOO_POPPET, null, (PlayerEntity) (Object) this);
			if (!poppet.isEmpty()) {
				LivingEntity owner = BewitchmentAPI.getTaglockOwner(world, poppet);
				if (!owner.getUuid().equals(getUuid())) {
					if (poppet.damage(BewitchmentAPI.getFamiliar((PlayerEntity) (Object) this) == EntityType.WOLF && random.nextBoolean() ? 0 : 1, random, null) && poppet.getDamage() >= poppet.getMaxDamage()) {
						poppet.decrement(1);
					}
					ItemStack potentialPoppet = BewitchmentAPI.getPoppet(world, BWObjects.VOODOO_PROTECTION_POPPET, owner, null);
					if (!potentialPoppet.isEmpty()) {
						if (potentialPoppet.damage(owner instanceof PlayerEntity && BewitchmentAPI.getFamiliar((PlayerEntity) owner) == EntityType.WOLF && random.nextBoolean() ? 0 : 1, random, null) && potentialPoppet.getDamage() >= potentialPoppet.getMaxDamage()) {
							potentialPoppet.decrement(1);
						}
						return;
					}
					owner.addStatusEffect(effect);
				}
			}
		}
	}
	
	@Inject(method = "readCustomDataFromTag", at = @At("TAIL"))
	private void readCustomDataFromTag(CompoundTag tag, CallbackInfo callbackInfo) {
		if (tag.contains("Blood")) {
			setBlood(tag.getInt("Blood"));
		}
		setFamiliar(tag.getBoolean("IsFamiliar"));
		ListTag curses = tag.getList("Curses", NbtType.COMPOUND);
		for (int i = 0; i < curses.size(); i++) {
			CompoundTag curse = curses.getCompound(i);
			addCurse(new Curse.Instance(BWRegistries.CURSES.get(new Identifier(curse.getString("Curse"))), curse.getInt("Duration")));
		}
		ListTag contracts = tag.getList("Contracts", NbtType.COMPOUND);
		for (int i = 0; i < contracts.size(); i++) {
			CompoundTag contract = contracts.getCompound(i);
			addContract(new Contract.Instance(BWRegistries.CONTRACTS.get(new Identifier(contract.getString("Contract"))), contract.getInt("Duration")));
		}
	}
	
	@Inject(method = "writeCustomDataToTag", at = @At("TAIL"))
	private void writeCustomDataToTag(CompoundTag tag, CallbackInfo callbackInfo) {
		tag.putInt("Blood", getBlood());
		tag.putBoolean("IsFamiliar", getFamiliar());
		tag.put("Curses", toTagCurse());
		tag.put("Contracts", toTagContract());
	}
	
	@Inject(method = "initDataTracker", at = @At("TAIL"))
	private void initDataTracker(CallbackInfo callbackInfo) {
		dataTracker.startTracking(BLOOD, MAX_BLOOD);
		dataTracker.startTracking(IS_FAMILIAR, false);
	}
}
