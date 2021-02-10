package moriyashiine.bewitchment.common.entity.living;

import com.google.common.collect.Sets;
import moriyashiine.bewitchment.api.BewitchmentAPI;
import moriyashiine.bewitchment.api.interfaces.entity.Pledgeable;
import moriyashiine.bewitchment.common.entity.living.util.BWHostileEntity;
import moriyashiine.bewitchment.common.item.HornedSpearItem;
import moriyashiine.bewitchment.common.misc.BWUtil;
import moriyashiine.bewitchment.common.registry.*;
import moriyashiine.bewitchment.mixin.StatusEffectAccessor;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class HerneEntity extends BWHostileEntity implements Pledgeable {
	private final ServerBossBar bossBar;
	private int timeSinceLastAttack = 0;
	
	public HerneEntity(EntityType<? extends HostileEntity> entityType, World world) {
		super(entityType, world);
		bossBar = new ServerBossBar(getDisplayName(), BossBar.Color.RED, BossBar.Style.PROGRESS);
		setPathfindingPenalty(PathNodeType.DANGER_FIRE, 0);
		setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, 0);
		experiencePoints = 75;
	}
	
	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 500).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 24).add(EntityAttributes.GENERIC_ARMOR, 10).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32);
	}
	
	@Override
	public void tick() {
		super.tick();
		if (!world.isClient) {
			bossBar.setPercent(getHealth() / getMaxHealth());
			LivingEntity target = getTarget();
			int timer = age + getEntityId();
			if (timer % 10 == 0) {
				heal(1);
			}
			if (target != null) {
				timeSinceLastAttack++;
				if (timeSinceLastAttack >= 600) {
					BWUtil.teleport(this, target.getX(), target.getY(), target.getZ(), true);
					timeSinceLastAttack = 0;
				}
				lookAtEntity(target, 360, 360);
				if (timer % 80 == 0) {
					HornedSpearItem.spawnEntity(world, this, new ItemStack(BWObjects.HORNED_SPEAR));
					swingHand(Hand.MAIN_HAND);
				}
				if (timer % 600 == 0) {
					summonMinions(this);
				}
			}
			else {
				if (getY() > -64) {
					heal(8);
				}
				timeSinceLastAttack = 0;
			}
		}
	}
	
	@Override
	public String getPledgeID() {
		return BWPledges.HERNE;
	}
	
	@Override
	public EntityType<?> getMinionType() {
		return BWEntityTypes.WEREWOLF;
	}
	
	@Override
	public Collection<StatusEffectInstance> getMinionBuffs() {
		return Sets.newHashSet(new StatusEffectInstance(StatusEffects.STRENGTH, Integer.MAX_VALUE, 1), new StatusEffectInstance(StatusEffects.RESISTANCE, Integer.MAX_VALUE, 1), new StatusEffectInstance(BWStatusEffects.HARDENING, Integer.MAX_VALUE, 1));
	}
	
	@Override
	public void setTimeSinceLastAttack(int timeSinceLastAttack) {
		this.timeSinceLastAttack = timeSinceLastAttack;
	}
	
	@Override
	protected boolean hasShiny() {
		return false;
	}
	
	@Override
	public int getVariants() {
		return 1;
	}
	
	@Override
	public EntityGroup getGroup() {
		return BewitchmentAPI.DEMON;
	}
	
	@Nullable
	@Override
	protected SoundEvent getAmbientSound() {
		return BWSoundEvents.ENTITY_HERNE_AMBIENT;
	}
	
	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return BWSoundEvents.ENTITY_HERNE_HURT;
	}
	
	@Override
	protected SoundEvent getDeathSound() {
		return BWSoundEvents.ENTITY_HERNE_DEATH;
	}
	
	@Override
	public boolean canBeLeashedBy(PlayerEntity player) {
		return false;
	}
	
	@Override
	public boolean canHaveStatusEffect(StatusEffectInstance effect) {
		return ((StatusEffectAccessor) effect.getEffectType()).bw_getType() == StatusEffectType.BENEFICIAL;
	}
	
	@Override
	public boolean isAffectedBySplashPotions() {
		return false;
	}
	
	@Override
	public boolean cannotDespawn() {
		return true;
	}
	
	@Override
	public boolean tryAttack(Entity target) {
		boolean flag = super.tryAttack(target);
		if (flag && target instanceof LivingEntity) {
			((LivingEntity) target).addStatusEffect(new StatusEffectInstance(BWStatusEffects.MORTAL_COIL, 1200));
			target.setOnFireFor(16);
			target.addVelocity(0, 0.2, 0);
			swingHand(Hand.MAIN_HAND);
		}
		return flag;
	}
	
	@Override
	public boolean handleFallDamage(float fallDistance, float damageMultiplier) {
		return false;
	}
	
	@Override
	protected void fall(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
	}
	
	@Override
	public void setTarget(@Nullable LivingEntity target) {
		if (world.isDay()) {
			target = null;
		}
		super.setTarget(target);
	}
	
	@Override
	public void setCustomName(@Nullable Text name) {
		super.setCustomName(name);
		bossBar.setName(getDisplayName());
	}
	
	@Override
	public void onStartedTrackingBy(ServerPlayerEntity player) {
		super.onStartedTrackingBy(player);
		bossBar.addPlayer(player);
	}
	
	@Override
	public void onStoppedTrackingBy(ServerPlayerEntity player) {
		super.onStoppedTrackingBy(player);
		bossBar.removePlayer(player);
	}
	
	@Override
	public void readCustomDataFromTag(CompoundTag tag) {
		super.readCustomDataFromTag(tag);
		if (hasCustomName()) {
			bossBar.setName(getDisplayName());
		}
		timeSinceLastAttack = tag.getInt("TimeSinceLastAttack");
	}
	
	@Override
	public void writeCustomDataToTag(CompoundTag tag) {
		super.writeCustomDataToTag(tag);
		tag.putInt("TimeSinceLastAttack", timeSinceLastAttack);
	}
	
	@Override
	protected void initGoals() {
		goalSelector.add(0, new SwimGoal(this));
		goalSelector.add(1, new MeleeAttackGoal(this, 1, true));
		goalSelector.add(2, new WanderAroundFarGoal(this, 1));
		goalSelector.add(3, new LookAtEntityGoal(this, PlayerEntity.class, 8));
		goalSelector.add(3, new LookAroundGoal(this));
		targetSelector.add(0, new RevengeGoal(this));
		targetSelector.add(1, new FollowTargetGoal<>(this, LivingEntity.class, 10, true, false, entity -> entity.getGroup() != BewitchmentAPI.DEMON && BWUtil.getArmorPieces(entity, stack -> stack.getItem() instanceof ArmorItem && ((ArmorItem) stack.getItem()).getMaterial() == BWMaterials.BESMIRCHED_ARMOR) < 3 && !(entity instanceof PlayerEntity && BewitchmentAPI.isPledged(world, getPledgeID(), entity.getUuid()))));
	}
}
