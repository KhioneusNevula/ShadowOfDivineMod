package com.gm910.sotdivine.systems.party.resource;

import java.util.Optional;

import com.gm910.sotdivine.SOTDMod;
import com.gm910.sotdivine.systems.party.resource.type.DimensionResource;
import com.gm910.sotdivine.systems.party.resource.type.EntityResource;
import com.gm910.sotdivine.systems.party.resource.type.IDResource;
import com.gm910.sotdivine.systems.party.resource.type.IDimensionResource;
import com.gm910.sotdivine.systems.party.resource.type.IEntityResource;
import com.gm910.sotdivine.systems.party.resource.type.IIDResource;
import com.gm910.sotdivine.systems.party.resource.type.IRegionResource;
import com.gm910.sotdivine.systems.party.resource.type.ISoulResource;
import com.gm910.sotdivine.systems.party.resource.type.RegionResource;
import com.gm910.sotdivine.systems.party.resource.type.SoulResource;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.registries.DeferredRegister.RegistryHolder;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

/**
 * SpecificationType categories for {@link IPartyResource}s
 * 
 * @author borah
 *
 * @param <T>
 */
public class PartyResourceType<T extends IPartyResource> {

	private static final RegistryHolder<PartyResourceType<?>> REGISTRY_HOLDER = SOTDMod.PARTY_RESOURCE_TYPES
			.makeRegistry(() -> RegistryBuilder.of());

	private static Optional<Codec<IPartyResource>> O_CODEC = Optional.empty();

	public static final RegistryObject<PartyResourceType<IDimensionResource>> DIMENSION = register("dimension_resource",
			false, true,
			RecordCodecBuilder.mapCodec(instance -> instance
					.group(ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension")
							.forGetter(IDimensionResource::dimension))
					.apply(instance, (d) -> new DimensionResource(d))));

	public static final RegistryObject<PartyResourceType<IRegionResource>> REGION = register("region_resource", false,
			true,
			RecordCodecBuilder.mapCodec(instance -> instance
					.group(ChunkPos.CODEC.fieldOf("chunkPos").forGetter(IRegionResource::chunkPos),
							ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension")
									.forGetter(IRegionResource::dimension))
					.apply(instance, (x, d) -> new RegionResource(x, d))));

	public static final RegistryObject<PartyResourceType<IEntityResource>> ENTITY = register("entity_resource", true,
			true,
			RecordCodecBuilder.mapCodec(instance -> instance
					.group(EntityType.CODEC.fieldOf("type").forGetter(IEntityResource::entityType),
							CompoundTag.CODEC.optionalFieldOf("tag").forGetter(IEntityResource::opTag),
							Codec.BOOL.fieldOf("fungible").forGetter(IEntityResource::isFungible))
					.apply(instance, (x, d, e) -> new EntityResource(x, d, e))));

	public static final RegistryObject<PartyResourceType<ISoulResource>> SOUL = register("soul_resource", false, false,
			CompoundTag.CODEC.<ISoulResource>xmap(SoulResource::new, ISoulResource::getSoulData).fieldOf("soul"));

	private static final MapCodec<IIDResource> UUID_MAPCODEC = UUIDUtil.CODEC
			.<IIDResource>xmap(IDResource::new, IIDResource::memberID).fieldOf("uuid");

	public static final RegistryObject<PartyResourceType<IIDResource>> MEMBER = register("member_resource", false, true,
			UUID_MAPCODEC);

	public static void init() {
		System.out.println("Initializing party resources...");
	}

	public static Codec<PartyResourceType<?>> typeCodec() {
		return REGISTRY_HOLDER.get().getCodec();
	}

	/**
	 * Get the codec for party resources
	 * 
	 * @return
	 */
	public static Optional<Codec<IPartyResource>> resourceCodec() {
		if (O_CODEC.isEmpty()) {
			O_CODEC = Optional.ofNullable(REGISTRY_HOLDER.get()).map((f) -> f.getCodec())
					.map((f) -> f.dispatch("resourceType", IPartyResource::resourceType, PartyResourceType::codec));
		}
		return O_CODEC;
	}

	public static <T extends IPartyResource> RegistryObject<PartyResourceType<T>> register(String id, boolean fungible,
			boolean deed, MapCodec<T> codec) {
		SOTDMod.LOGGER.debug("Registering resource " + id + " to registry " + SOTDMod.PARTY_RESOURCE_TYPES);

		return SOTDMod.PARTY_RESOURCE_TYPES.register(id, () -> new PartyResourceType<T>(id, codec, fungible, deed));
	}

	private MapCodec<T> codec;
	private boolean isf;
	private boolean isd;

	private String name;

	private PartyResourceType(String name, MapCodec<T> codec, boolean fung, boolean deed) {
		this.codec = codec;
		this.isf = fung;
		this.isd = deed;
		this.name = name;
	}

	public MapCodec<T> codec() {
		return codec;
	}

	/**
	 * Whether "identical copies" of this resource can be owned in quantities > 1
	 * 
	 * @return
	 */
	public boolean isFungible() {
		return isf;
	}

	/**
	 * Whether this resource is something that exists independently and is owned by
	 * deed rather than kept fully
	 * 
	 * @return
	 */
	public boolean isDeed() {
		return isd;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "PRT[" + name + "]";
	}

}
