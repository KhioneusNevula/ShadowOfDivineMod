package com.gm910.sotdivine.magic.ritual;

import java.util.ArrayList;
import java.util.Collection;

import com.gm910.sotdivine.magic.ritual.pattern.IRitualPattern;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * An instance of an active ritual,
 */
public record RitualInstance(IRitual ritual, EntityReference<Entity> caster, GlobalPos focusPos,
		IRitualPattern successfulPattern, float intensity, Collection<EntityReference<ItemEntity>> offerings) {
	private static Codec<RitualInstance> CODEC = null;

	public static Codec<RitualInstance> codec() {
		if (CODEC == null) {
			CODEC = RecordCodecBuilder.create(
					instance -> instance.group(IRitual.codec().fieldOf("ritual").forGetter(RitualInstance::ritual),
							EntityReference.<Entity>codec().fieldOf("caster").forGetter(RitualInstance::caster),
							GlobalPos.CODEC.fieldOf("pos").forGetter(RitualInstance::focusPos),
							IRitualPattern.eitherCodec().fieldOf("successful_pattern")
									.forGetter(RitualInstance::successfulPattern),
							Codec.FLOAT.fieldOf("intensity").forGetter(RitualInstance::intensity),
							Codec.list(EntityReference.<ItemEntity>codec()).fieldOf("offerings")
									.forGetter((s) -> new ArrayList<>(s.offerings))

					).apply(instance, (r, c, f, sp, i, o) -> new RitualInstance(r, c, f, sp, i, o)));
		}
		return CODEC;
	}
}
