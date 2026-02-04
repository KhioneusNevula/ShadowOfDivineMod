package com.gm910.sotdivine.magic.afterlife;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.gm910.sotdivine.concepts.parties.party.resource.type.ISoulResource;
import com.gm910.sotdivine.magic.afterlife.ISoulState.LifeState;
import com.gm910.sotdivine.util.WorldUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

/**
 * For ease of access to souls
 */
public class Soul {

	private Afterlife afterlife;

	private UUID uuid;

	private Component description;

	private EntityType<?> type;

	/**
	 * If this entity has been deleted from the world and/or server
	 */
	private boolean noLongerExists;

	/**
	 * If this refers to an entity in the world, then keep a reference to the entity
	 */
	private final EntityReference<Entity> reference;

	/**
	 * See {@link #createSoulFromUUIDUnsafely(UUID, IAfterlife)}; afterlife is set
	 * to null in this case
	 * 
	 * @param id
	 * @return
	 */
	public static Soul createSoulFromUUIDUnsafely(UUID id) {
		return new Soul(id, null);
	}

	/**
	 * Creates soul from uuid 'unsafely', i.e. not making it persistent/marked, or
	 * registering it to the afterlife
	 * 
	 * @param id
	 * @return
	 */
	public static Soul createSoulFromUUIDUnsafely(UUID id, IAfterlife after) {
		return new Soul(id, after);
	}

	/**
	 * Creates a soul instance from the given entity 'unsafely', i.e. not making it
	 * persistent/marked, or registering it to the afterlife
	 * 
	 * @param entity
	 * @return
	 */
	public static Soul createSoulFromEntityUnsafely(Entity entity) {
		return createSoulFromEntityUnsafely(entity,
				Optional.of(entity).map(e -> e.getServer()).map(e -> IAfterlife.get(e)).orElse(null));
	}

	/**
	 * Creates a soul instance from the given entity 'unsafely', i.e. not making it
	 * persistent/marked, or registering it to the afterlife
	 * 
	 * @param entity
	 * @return
	 */
	public static Soul createSoulFromEntityUnsafely(Entity entity, IAfterlife afterlife) {
		return new Soul(entity, afterlife);
	}

	private Soul(UUID en, @Nullable IAfterlife after) {
		this.uuid = en;
		this.reference = new EntityReference<Entity>(en);
		this.afterlife = (Afterlife) after;
	}

	private Soul(Entity en, @Nullable IAfterlife after) {
		this.uuid = en.getUUID();
		this.reference = new EntityReference<Entity>(en);
		this.afterlife = (Afterlife) after;
		this.type = en.getType();
	}

	private Soul(Soul uuid, Optional<EntityType<?>> eType, Optional<Component> description) {
		this(uuid.uuid, null);
		this.type = eType.orElse(null);
		this.description = description.orElse(null);
	}

	private Soul(UUID id) {
		this(id, null);
	}

	public static final MapCodec<Soul> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(UUIDUtil.CODEC.fieldOf("UUID").forGetter(Soul::uuid)).apply(instance, Soul::new));

	/**
	 * The map codec which also stores a description and entity type when needed
	 */
	public static final MapCodec<Soul> MAP_CODEC_WITH_DETAILS = RecordCodecBuilder.mapCodec(instance -> instance
			.group(MAP_CODEC.forGetter(s -> s),
					EntityType.CODEC.optionalFieldOf("type").forGetter(s -> s.getEntityType()),
					ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(s -> s.getDescription()))
			.apply(instance, Soul::new));

	public static final Codec<Soul> CODEC = MAP_CODEC.codec();

	/**
	 * Codec that includes details such as description and entity type
	 */
	public static final Codec<Soul> CODEC_WITH_DETAILS = MAP_CODEC_WITH_DETAILS.codec();

	public static final StreamCodec<ByteBuf, Soul> STREAM_CODEC = StreamCodec.composite(UUIDUtil.STREAM_CODEC,
			Soul::uuid, Soul::new);

	/**
	 * The codec which also streams a description and entity type
	 */
	public static final StreamCodec<RegistryFriendlyByteBuf, Soul> STREAM_CODEC_WITH_DETAILS = StreamCodec.composite(
			STREAM_CODEC, s -> s, ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.ENTITY_TYPE)),
			s -> s.getEntityType(), ComponentSerialization.OPTIONAL_STREAM_CODEC, s -> s.getDescription(), Soul::new);

	public UUID uuid() {
		return uuid;
	}

	/**
	 * Return the description of this soul, or empty if not initialized
	 * 
	 * @return
	 */
	public Optional<Component> getDescription() {
		return Optional.ofNullable(description);
	}

	/**
	 * REturn the entity type of this soul, or empty if not initialized
	 * 
	 * @return
	 */
	public Optional<EntityType<?>> getEntityType() {
		return Optional.ofNullable(type);
	}

	public Soul updateAfterlifeReference(Afterlife afterlife) {
		this.afterlife = afterlife;
		return this;
	}

	/**
	 * If the given level is null; return a reference to the stored afterlife's own
	 * level. If the given level is non-null, update internal afterlife. If no level
	 * can be produced, throw an error.
	 * 
	 * @param input
	 * @return
	 */
	private ServerLevel supplyOrUpdateLevel(@Nullable ServerLevel input) {
		ServerLevel level = input;
		if (level == null) {
			if (afterlife != null) {
				level = this.afterlife.getOverworld();
			} else {
				throw new IllegalArgumentException(
						"Serverlevel is null or afterlife is inaccessible: " + input + ", " + afterlife);
			}
		} else {
			if (afterlife == null || afterlife.getOverworld() != level.getServer().overworld()) {
				afterlife = (Afterlife) IAfterlife.get(level);
			}
		}
		return level;
	}

	public boolean matches(LivingEntity entity) {
		return this.reference.matches(entity);
	}

	/**
	 * If this entity currently exists in a world on the server
	 * 
	 * @param level
	 * @return
	 */
	public boolean existsInAWorld(@Nullable ServerLevel level) {
		return getExistingEntity(level).isPresent();
	}

	/**
	 * If this entity exists in the world or on a server
	 * 
	 * @param level
	 * @return
	 */
	public boolean existsAtAll(@Nullable ServerLevel level) {
		if (noLongerExists) {
			return false;
		}
		noLongerExists = getExistingEntity(level).map(x -> (Object) x).or(() -> getSoulInfoFromAfterlife(level))
				.isEmpty();
		return !noLongerExists;
	}

	/**
	 * Gets this as an existing entity, if it exists ofc
	 * 
	 * @param level
	 * @return
	 */
	public <E extends Entity> Optional<E> getExistingEntity(@Nullable ServerLevel level) {
		try {
			return WorldUtils.findEntityInServer(reference, supplyOrUpdateLevel(level), Entity.class).map(s -> (E) s)
					.map((e) -> {
						this.description = e.getName();
						this.type = e.getType();
						return e;
					});
		} catch (ClassCastException e) {
			return Optional.empty();
		}
	}

	/**
	 * See {@link #getExistingEntity(ServerLevel)}
	 * 
	 * @param server
	 * @return
	 */
	public <E extends Entity> Optional<E> getExistingEntity(@Nullable MinecraftServer server) {
		return getExistingEntity(server == null ? null : server.overworld());
	}

	/**
	 * See {@link #getOrCreateEntity(ServerLevel, Consumer, boolean)}
	 * 
	 * @param level
	 * @return
	 */
	public Optional<Entity> getOrCreateEntity(@Nullable ServerLevel nullableLevel, boolean spawnIfCreated) {
		return getOrCreateEntity(nullableLevel, (Consumer<Entity>) null, spawnIfCreated);
	}

	/**
	 * Same as {@link #getOrCreateEntity(ServerLevel, boolean)}, but if the entity
	 * is created, convert it to the given life state
	 * 
	 * @param nullableLevel
	 * @param stateIfCreated
	 * @return
	 */
	public Optional<Entity> getOrCreateEntity(@Nullable ServerLevel nullableLevel, LifeState stateIfCreated,
			boolean spawnIfCreated) {
		return getOrCreateEntity(nullableLevel, en -> {
			if (en instanceof LivingEntity l) {
				ISoulState.get(l).changeState(stateIfCreated);
			}
		}, spawnIfCreated);
	}

	/**
	 * Either finds the existing entity this contains, or creates a new one (boolean
	 * spawn if you want it to automatically be spawned in the given level or the
	 * overworld). Returns an empty optional if nothing could be found at all
	 * 
	 * @param level
	 * @param ifCreated a callback to run if the entity is created rather than
	 *                  obtained (e.g. turning it into a ghost)
	 * @return
	 */
	public Optional<Entity> getOrCreateEntity(@Nullable ServerLevel nullableLevel, @Nullable Consumer<Entity> ifCreated,
			boolean spawnIfCreated) {
		ServerLevel level = supplyOrUpdateLevel(nullableLevel);
		Optional<Entity> en = this.getExistingEntity(level);
		if (en.isEmpty()) {
			en = this.getSoulInfo(level).flatMap(e -> e.regenerateEntity(level, EntitySpawnReason.NATURAL));

			if (ifCreated != null) {
				en.ifPresent(ifCreated);
			}
			if (spawnIfCreated) {
				en.ifPresent(en2 -> level.addFreshEntityWithPassengers(en2));
			}
		}
		en.ifPresent(e -> {
			this.description = e.getName();
			this.type = e.getType();
		});
		return en;

	}

	/**
	 * Returns the actual soul information. First attempts to get an afterlife soul,
	 * then an existing entity's data
	 * 
	 * @param level
	 * @return
	 */
	public Optional<ISoulResource> getSoulInfo(@Nullable ServerLevel level) {
		ServerLevel level1 = supplyOrUpdateLevel(level);
		return getSoulInfoFromAfterlife(level1)
				.or(() -> this.getExistingEntity(level1).map(s -> ISoulResource.create(s)));
	}

	/**
	 * Returns soul info from the afterlife, if it is present in the afterlife
	 * 
	 * @param level
	 * @return
	 */
	public Optional<ISoulResource> getSoulInfoFromAfterlife(@Nullable ServerLevel level) {
		ServerLevel level1 = supplyOrUpdateLevel(level);
		return afterlife.getSoulResource(this).map(sr -> {
			description = sr.getName(level1);
			type = sr.getEntityType();
			return sr;
		});
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Soul soul) {
			if (this == obj)
				return true;
			return this.uuid.equals(soul.uuid);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.uuid.hashCode();
	}

	@Override
	public String toString() {
		return "Soul(" + (description == null ? ""
				: "\"" + this.type.builtInRegistryHolder().key().location().toShortLanguageKey() + "\", \""
						+ description.getString() + "\", ")
				+ uuid.toString() + ")";
	}

}
