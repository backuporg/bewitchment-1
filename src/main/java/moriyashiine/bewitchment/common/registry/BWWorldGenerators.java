package moriyashiine.bewitchment.common.registry;

import moriyashiine.bewitchment.common.Bewitchment;
import moriyashiine.bewitchment.common.entity.living.HellhoundEntity;
import moriyashiine.bewitchment.common.entity.living.VampireEntity;
import moriyashiine.bewitchment.common.entity.living.WerewolfEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.fabricmc.fabric.api.loot.v1.event.LootTableLoadingCallback;
import net.fabricmc.fabric.impl.biome.modification.BiomeModificationImpl;
import net.fabricmc.fabric.mixin.object.builder.SpawnRestrictionAccessor;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.entry.LootTableEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.UniformIntDistribution;
import net.minecraft.world.gen.decorator.ChanceDecoratorConfig;
import net.minecraft.world.gen.decorator.Decorator;
import net.minecraft.world.gen.feature.*;
import net.minecraft.world.gen.feature.size.FeatureSize;
import net.minecraft.world.gen.feature.size.TwoLayersFeatureSize;
import net.minecraft.world.gen.foliage.AcaciaFoliagePlacer;
import net.minecraft.world.gen.foliage.LargeOakFoliagePlacer;
import net.minecraft.world.gen.foliage.MegaPineFoliagePlacer;
import net.minecraft.world.gen.stateprovider.SimpleBlockStateProvider;
import net.minecraft.world.gen.trunk.ForkingTrunkPlacer;
import net.minecraft.world.gen.trunk.StraightTrunkPlacer;

import java.util.LinkedHashMap;
import java.util.Map;

public class BWWorldGenerators {
	private static final Map<ConfiguredFeature<?, ?>, Identifier> CONFIGURED_FEATURES = new LinkedHashMap<>();
	
	private static final FeatureSize EMPTY_SIZE = new TwoLayersFeatureSize(0, 0, 0);
	
	public static final ConfiguredFeature<TreeFeatureConfig, ?> JUNIPER_TREE = create("juniper_tree", Feature.TREE.configure(new TreeFeatureConfig.Builder(new SimpleBlockStateProvider(BWObjects.JUNIPER_LOG.getDefaultState()), new SimpleBlockStateProvider(BWObjects.JUNIPER_LEAVES.getDefaultState()), new AcaciaFoliagePlacer(UniformIntDistribution.of(2), UniformIntDistribution.of(0)), new ForkingTrunkPlacer(3, 1, 1), EMPTY_SIZE).ignoreVines().build()));
	public static final ConfiguredFeature<TreeFeatureConfig, ?> CYPRESS_TREE = create("cypress_tree", Feature.TREE.configure(new TreeFeatureConfig.Builder(new SimpleBlockStateProvider(BWObjects.CYPRESS_LOG.getDefaultState()), new SimpleBlockStateProvider(BWObjects.CYPRESS_LEAVES.getDefaultState()), new LargeOakFoliagePlacer(UniformIntDistribution.of(2), UniformIntDistribution.of(0), 4), new StraightTrunkPlacer(6, 1, 1), EMPTY_SIZE).ignoreVines().build()));
	public static final ConfiguredFeature<TreeFeatureConfig, ?> ELDER_TREE = create("elder_tree", Feature.TREE.configure(new TreeFeatureConfig.Builder(new SimpleBlockStateProvider(BWObjects.ELDER_LOG.getDefaultState()), new SimpleBlockStateProvider(BWObjects.ELDER_LEAVES.getDefaultState()), new LargeOakFoliagePlacer(UniformIntDistribution.of(2), UniformIntDistribution.of(0), 4), new StraightTrunkPlacer(4, 0, 1), EMPTY_SIZE).ignoreVines().build()));
	public static final ConfiguredFeature<TreeFeatureConfig, ?> DRAGONS_BLOOD_TREE = create("dragons_blood_tree", Feature.TREE.configure(new TreeFeatureConfig.Builder(new SimpleBlockStateProvider(BWObjects.DRAGONS_BLOOD_LOG.getDefaultState().with(BWProperties.NATURAL, true)), new SimpleBlockStateProvider(BWObjects.DRAGONS_BLOOD_LEAVES.getDefaultState()), new MegaPineFoliagePlacer(UniformIntDistribution.of(1), UniformIntDistribution.of(0), UniformIntDistribution.of(3)), new StraightTrunkPlacer(5, 1, 1), EMPTY_SIZE).ignoreVines().build()));
	
	public static final ConfiguredFeature<?, ?> SILVER_ORE = create("silver_ore", Feature.ORE.configure(new OreFeatureConfig(OreFeatureConfig.Rules.BASE_STONE_OVERWORLD, BWObjects.SILVER_ORE.getDefaultState(), Bewitchment.config.silverSize)).rangeOf(Bewitchment.config.silverMaxHeight).spreadHorizontally().repeat(Bewitchment.config.silverCount));
	public static final ConfiguredFeature<?, ?> SALT_ORE = create("salt_ore", Feature.ORE.configure(new OreFeatureConfig(OreFeatureConfig.Rules.BASE_STONE_OVERWORLD, BWObjects.SALT_ORE.getDefaultState(), Bewitchment.config.saltSize)).rangeOf(Bewitchment.config.saltMaxHeight).spreadHorizontally().repeat(Bewitchment.config.saltCount));
	
	private static <T extends FeatureConfig> ConfiguredFeature<T, ?> create(String name, ConfiguredFeature<T, ?> configuredFeature) {
		CONFIGURED_FEATURES.put(configuredFeature, new Identifier(Bewitchment.MODID, name));
		return configuredFeature;
	}
	
	@SuppressWarnings("UnstableApiUsage")
	public static void init() {
		ConfiguredFeature<?, ?> JUNIPER_TREE_WITH_CHANCE = create("juniper_tree_with_chance", JUNIPER_TREE.decorate(Decorator.CHANCE.configure(new ChanceDecoratorConfig(5))));
		ConfiguredFeature<?, ?> CYPRESS_TREE_WITH_CHANCE = create("cypress_tree_with_chance", CYPRESS_TREE.decorate(Decorator.CHANCE.configure(new ChanceDecoratorConfig(5))));
		ConfiguredFeature<?, ?> ELDER_TREE_WITH_CHANCE = create("elder_tree_with_chance", ELDER_TREE.decorate(Decorator.CHANCE.configure(new ChanceDecoratorConfig(5))));
		CONFIGURED_FEATURES.keySet().forEach(configuredFeature -> Registry.register(BuiltinRegistries.CONFIGURED_FEATURE, CONFIGURED_FEATURES.get(configuredFeature), configuredFeature));
		BiomeModificationImpl.INSTANCE.addModifier(CONFIGURED_FEATURES.get(JUNIPER_TREE), ModificationPhase.ADDITIONS, BiomeSelectors.foundInOverworld().and(context -> context.getBiome().getCategory() == Biome.Category.SAVANNA), context -> context.getGenerationSettings().addBuiltInFeature(GenerationStep.Feature.VEGETAL_DECORATION, JUNIPER_TREE_WITH_CHANCE));
		BiomeModificationImpl.INSTANCE.addModifier(CONFIGURED_FEATURES.get(CYPRESS_TREE), ModificationPhase.ADDITIONS, BiomeSelectors.foundInOverworld().and(context -> context.getBiome().getCategory() == Biome.Category.TAIGA || context.getBiome().getCategory() == Biome.Category.SWAMP), context -> context.getGenerationSettings().addBuiltInFeature(GenerationStep.Feature.VEGETAL_DECORATION, CYPRESS_TREE_WITH_CHANCE));
		BiomeModificationImpl.INSTANCE.addModifier(CONFIGURED_FEATURES.get(ELDER_TREE), ModificationPhase.ADDITIONS, BiomeSelectors.foundInOverworld().and(context -> context.getBiome().getCategory() == Biome.Category.FOREST), context -> context.getGenerationSettings().addBuiltInFeature(GenerationStep.Feature.VEGETAL_DECORATION, ELDER_TREE_WITH_CHANCE));
		BiomeModificationImpl.INSTANCE.addModifier(CONFIGURED_FEATURES.get(SILVER_ORE), ModificationPhase.ADDITIONS, BiomeSelectors.foundInOverworld(), context -> context.getGenerationSettings().addBuiltInFeature(GenerationStep.Feature.UNDERGROUND_ORES, SILVER_ORE));
		BiomeModificationImpl.INSTANCE.addModifier(CONFIGURED_FEATURES.get(SALT_ORE), ModificationPhase.ADDITIONS, BiomeSelectors.foundInOverworld(), context -> context.getGenerationSettings().addBuiltInFeature(GenerationStep.Feature.UNDERGROUND_ORES, SALT_ORE));
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld().and(context -> Bewitchment.config.owlBiomeCategories.contains(context.getBiome().getCategory().getName())), BWEntityTypes.OWL.getSpawnGroup(), BWEntityTypes.OWL, Bewitchment.config.owlWeight, Bewitchment.config.owlMinGroupCount, Bewitchment.config.owlMaxGroupCount);
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld().and(context -> Bewitchment.config.ravenBiomeCategories.contains(context.getBiome().getCategory().getName())), BWEntityTypes.RAVEN.getSpawnGroup(), BWEntityTypes.RAVEN, Bewitchment.config.ravenWeight, Bewitchment.config.ravenMinGroupCount, Bewitchment.config.ravenMaxGroupCount);
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld().and(context -> Bewitchment.config.snakeBiomeCategories.contains(context.getBiome().getCategory().getName())), BWEntityTypes.SNAKE.getSpawnGroup(), BWEntityTypes.SNAKE, Bewitchment.config.snakeWeight, Bewitchment.config.snakeMinGroupCount, Bewitchment.config.snakeMaxGroupCount);
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld().and(context -> Bewitchment.config.toadBiomeCategories.contains(context.getBiome().getCategory().getName())), BWEntityTypes.TOAD.getSpawnGroup(), BWEntityTypes.TOAD, Bewitchment.config.toadWeight, Bewitchment.config.toadMinGroupCount, Bewitchment.config.toadMaxGroupCount);
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld().and(context -> !context.getBiome().getSpawnSettings().getSpawnEntry(BWEntityTypes.GHOST.getSpawnGroup()).isEmpty() && context.getBiome().getCategory() != Biome.Category.OCEAN), BWEntityTypes.GHOST.getSpawnGroup(), BWEntityTypes.GHOST, Bewitchment.config.ghostWeight, Bewitchment.config.ghostMinGroupCount, Bewitchment.config.ghostMaxGroupCount);
		BiomeModifications.addSpawn(BiomeSelectors.foundInTheNether().and(context -> !context.getBiome().getSpawnSettings().getSpawnEntry(BWEntityTypes.HELLHOUND.getSpawnGroup()).isEmpty()), BWEntityTypes.HELLHOUND.getSpawnGroup(), BWEntityTypes.HELLHOUND, Bewitchment.config.hellhoundWeight, Bewitchment.config.hellhoundMinGroupCount, Bewitchment.config.hellhoundMaxGroupCount);
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld().and(context -> !context.getBiome().getSpawnSettings().getSpawnEntry(BWEntityTypes.VAMPIRE.getSpawnGroup()).isEmpty() && context.getBiome().getCategory() != Biome.Category.OCEAN && (context.getBiome().getCategory() == Biome.Category.PLAINS || context.getBiome().getCategory() == Biome.Category.TAIGA)), BWEntityTypes.VAMPIRE.getSpawnGroup(), BWEntityTypes.VAMPIRE, Bewitchment.config.vampireWeight, Bewitchment.config.vampireMinGroupCount, Bewitchment.config.vampireMaxGroupCount);
		BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld().and(context -> !context.getBiome().getSpawnSettings().getSpawnEntry(BWEntityTypes.WEREWOLF.getSpawnGroup()).isEmpty() && context.getBiome().getCategory() != Biome.Category.OCEAN && (context.getBiome().getCategory() == Biome.Category.FOREST || context.getBiome().getCategory() == Biome.Category.TAIGA || context.getBiome().getCategory() == Biome.Category.ICY)), BWEntityTypes.WEREWOLF.getSpawnGroup(), BWEntityTypes.WEREWOLF, Bewitchment.config.werewolfWeight, Bewitchment.config.werewolfMinGroupCount, Bewitchment.config.werewolfMaxGroupCount);
		SpawnRestrictionAccessor.callRegister(BWEntityTypes.HELLHOUND, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HellhoundEntity::canSpawn);
		SpawnRestrictionAccessor.callRegister(BWEntityTypes.VAMPIRE, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.WORLD_SURFACE_WG, VampireEntity::canSpawn);
		SpawnRestrictionAccessor.callRegister(BWEntityTypes.WEREWOLF, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.WORLD_SURFACE_WG, WerewolfEntity::canSpawn);
		LootTableLoadingCallback.EVENT.register((resourceManager, lootManager, identifier, fabricLootSupplierBuilder, lootTableSetter) -> {
			Identifier seeds = new Identifier(Bewitchment.MODID, "inject/seeds");
			Identifier nether_fortress = new Identifier(Bewitchment.MODID, "inject/nether_fortress");
			if (Blocks.GRASS.getLootTableId().equals(identifier) || Blocks.TALL_GRASS.getLootTableId().equals(identifier) || Blocks.FERN.getLootTableId().equals(identifier) || Blocks.LARGE_FERN.getLootTableId().equals(identifier)) {
				fabricLootSupplierBuilder.withPool(LootPool.builder().with(LootTableEntry.builder(seeds).weight(1)).build());
			}
			if (LootTables.NETHER_BRIDGE_CHEST.equals(identifier)) {
				fabricLootSupplierBuilder.withPool(LootPool.builder().with(LootTableEntry.builder(nether_fortress).weight(1)).build());
			}
		});
	}
}
