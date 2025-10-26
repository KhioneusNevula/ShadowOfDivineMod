package com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.entity_preds;

import java.util.Optional;

import com.gm910.sotdivine.deities_and_parties.deity.sphere.genres.provider.ProviderType;
import com.gm910.sotdivine.deities_and_parties.villagers.ModBrainElements;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * A provider which selects for what is in an item frame
 */
public record IsWorshiper(String name) implements ITypeSpecificProvider<LivingEntity> {

	public static final ResourceLocation PATH = ResourceLocation.withDefaultNamespace("is_worshiper");

	@Override
	public boolean matches(ServerLevel level, LivingEntity instance) {
		return instance.getBrain().getMemory(ModBrainElements.MemoryModuleTypes.PARTY_ID.get())
				.filter((iv) -> iv.equals(name)).isPresent();
	}

	@Override
	public LivingEntity generateRandom(ServerLevel level, Optional<LivingEntity> prior) {
		prior.orElseThrow().getBrain().setMemory(ModBrainElements.MemoryModuleTypes.PARTY_ID.get(), name);

		return prior.orElseThrow();
	}

	@Override
	public Class<LivingEntity> entityClass() {
		return LivingEntity.class;
	}

	@Override
	public ProviderType<IsWorshiper> providerType() {
		throw new UnsupportedOperationException("Not a real provider");
	}

	@Override
	public ResourceLocation path() {
		return PATH;
	}

	@Override
	public final String toString() {
		return "Worships(" + name + ")";
	}

}
