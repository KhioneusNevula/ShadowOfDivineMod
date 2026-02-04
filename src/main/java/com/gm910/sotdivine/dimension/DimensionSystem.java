package com.gm910.sotdivine.dimension;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;

import com.gm910.sotdivine.concepts.deity.IDeity;
import com.gm910.sotdivine.concepts.parties.system_storage.IPartySystem;
import com.gm910.sotdivine.util.FieldUtils;
import com.mojang.logging.LogUtils;

import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayer.RespawnConfig;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.ProblemReporter.RootElementPathElement;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.level.storage.ServerLevelData;

/**
 * 
 */
class DimensionSystem extends SavedData implements IDimensionSystem {

	protected static final WeakHashMap<ServerLevel, DimensionSystem> cachedSystems = new WeakHashMap<>(1);
	/***
	 * For {@link IDimensionSystem#getCached()} if we don't have an available level
	 * instance; returns last retrieved
	 */
	protected static DimensionSystem mostRecentCached = null;

	protected MinecraftServer server = null;

	protected Map<ResourceKey<Level>, DimensionProperties> dimensionDefinitions = new HashMap<>();

	public DimensionSystem() {
	}

	public DimensionSystem(Map<ResourceKey<Level>, DimensionProperties> map) {
		this.dimensionDefinitions = new HashMap<>(map);
	}

	@Override
	public Map<ResourceKey<Level>, DimensionProperties> getDimensions() {
		return Collections.unmodifiableMap(dimensionDefinitions);
	}

	@Override
	public void onLoad(MinecraftServer server) {
		for (var entry : dimensionDefinitions.entrySet()) {
			loadDimension(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public DimensionProperties removeDimension(ResourceKey<Level> dimension) {
		DimensionProperties properties = this.dimensionDefinitions.remove(dimension);
		if (properties != null) {

			ServerLevel level = server.getLevel(dimension);
			this.repositionAllPlayers(level);

			server.schedule(server.wrapRunnable(() -> {
				FieldUtils.<MinecraftServer, Map<ResourceKey<Level>, ServerLevel>>getField(MinecraftServer.class,
						"levels", "P", server).remove(level.dimension());
			}));

		}
		return properties;
	}

	@Override
	public void setProperties(ResourceKey<Level> dimension, DimensionProperties properties) {
		this.dimensionDefinitions.put(dimension, properties);

		this.setDirty();
	}

	@Override
	public void addDimension(ResourceKey<Level> dimension, DimensionProperties definition, boolean loadIn) {
		if (dimensionDefinitions.put(dimension, definition) == null) {
			this.setDirty();
			LogUtils.getLogger().debug("Created new dimension " + dimension.location());
			if (loadIn)
				loadDimension(dimension, definition);
		}
	}

	private void loadDimension(ResourceKey<Level> dimension, DimensionProperties definition) {
		IDeity deity = IPartySystem.getOrCreate(server).getDeityByName(definition.deity()).get();
		if (server.getLevel(dimension) == null) {
			LogUtils.getLogger().debug("Loading ad-hoc dimension " + dimension.location() + " from " + definition);
			DerivedLevelData derivedleveldata = new DerivedLevelData(server.getWorldData(),
					(ServerLevelData) server.overworld().getLevelData());
			Executor executor = FieldUtils.getField(MinecraftServer.class, "executor", "ay", server);
			LevelStorageSource.LevelStorageAccess storageSource = FieldUtils.getField(MinecraftServer.class,
					"storageSource", "f", server);
			long j = FieldUtils.getField(BiomeManager.class, "biomeZoomSeed", "f",
					server.overworld().getBiomeManager());
			ChunkProgressListener lisa = FieldUtils.getField(ChunkMap.class, "progressListener", "F",
					server.overworld().getChunkSource().chunkMap);
			ServerLevel serverlevel1 = new ServerLevel(server, executor, storageSource, derivedleveldata, dimension,
					definition.theme().stem().get(), lisa, server.getWorldData().isDebugWorld(), j,
					definition.powers().stream().<CustomSpawner>map(
							(power) -> (ServerLevel p_45839_, boolean p_45840_, boolean p_45841_) -> {
								power.tick(deity, p_45839_, p_45840_, p_45841_);
							}).toList(),
					false, server.overworld().getRandomSequences());
			server.overworld().getWorldBorder()
					.addListener(new BorderChangeListener.DelegateBorderChangeListener(serverlevel1.getWorldBorder()));
			FieldUtils.<MinecraftServer, Map<ResourceKey<Level>, ServerLevel>>getField(MinecraftServer.class, "levels",
					"P", server).put(dimension, serverlevel1);
			LogUtils.getLogger().debug("Finished loading ad-hoc dimension " + dimension.location());
			server.markWorldsDirty();
			net.minecraftforge.event.ForgeEventFactory.onLevelLoad(serverlevel1);
		} else {

			LogUtils.getLogger().debug("Skipping ad-hoc dimension " + dimension.location() + " because it is loaded");
		}
	}

	/**
	 * Moves all players from this world to the overworld
	 * 
	 * @param fromLevel
	 */
	private void repositionAllPlayers(ServerLevel level) {

		LevelStorageSource.LevelStorageAccess storageSource = FieldUtils.getField(MinecraftServer.class,
				"storageSource", "f", server);
		PlayerDataStorage playerData = storageSource.createPlayerStorage();

		Set<UUID> alreadyMoved = new HashSet<>();

		level.players().forEach(p -> {
			if (p.getRespawnConfig() == null || p.getRespawnConfig().dimension().equals(level.dimension())) {
				p.setRespawnPosition(new ServerPlayer.RespawnConfig(server.overworld().dimension(),
						server.overworld().getSharedSpawnPos(), 0.0F, false), false);
			}
			p.kill(level);
			alreadyMoved.add(p.getUUID());
		});
		try (ProblemReporter.ScopedCollector sc = new ProblemReporter.ScopedCollector(
				new RootElementPathElement(level.dimension()), LogUtils.getLogger())) {
			for (File playerFile : playerData.getPlayerDataFolder().listFiles((dir, name) -> name.contains(".dat"))) {
				if (playerFile.exists() && playerFile.isFile()) {
					try {
						UUID uuid;
						if (playerFile.getName().endsWith(".dat_old")) {
							uuid = UUID.fromString(playerFile.getName().replace(".dat_old", ""));
						} else {
							uuid = UUID.fromString(playerFile.getName().replace(".dat", ""));
						}
						if (alreadyMoved.contains(uuid)) {
							continue;
						}

						Optional.of(NbtIo.readCompressed(playerFile.toPath(), NbtAccounter.unlimitedHeap()))
								.ifPresent(v -> {
									v.putString(ServerPlayer.TAG_DIMENSION,
											server.overworld().dimension().location().toString());
									v.putFloat(ServerPlayer.TAG_HEALTH, 0);
									if (!(v.read("respawn", ServerPlayer.RespawnConfig.CODEC)
											.orElse(null) instanceof RespawnConfig rspc)
											|| rspc.dimension().equals(level.dimension())) {

										v.store("respawn", ServerPlayer.RespawnConfig.CODEC,
												new ServerPlayer.RespawnConfig(server.overworld().dimension(),
														server.overworld().getSharedSpawnPos(), 0.0F, false));
									}
									try {
										NbtIo.writeCompressed(v, playerFile.toPath());
									} catch (IOException e) {
										throw new RuntimeException(e);
									}

								});

					} catch (Exception exception) {
						LogUtils.getLogger().warn("Failed to reposition specific player {}", playerFile.getName());
					}
				}
			}
		}

	}

}
