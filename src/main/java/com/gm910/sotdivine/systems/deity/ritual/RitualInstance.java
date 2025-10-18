package com.gm910.sotdivine.systems.deity.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;

/**
 * An instance of an active ritual,
 */
public record RitualInstance(IRitual ritual, EntityReference<Entity> caster, GlobalPos focusPos) {
	private static Codec<RitualInstance> CODEC = null;

	public static Codec<RitualInstance> codec() {
		if (CODEC == null) {
			CODEC = RecordCodecBuilder.create(instance -> instance
					.group(IRitual.codec().fieldOf("ritual").forGetter(RitualInstance::ritual),
							EntityReference.<Entity>codec().fieldOf("caster").forGetter(RitualInstance::caster),
							GlobalPos.CODEC.fieldOf("pos").forGetter(RitualInstance::focusPos))
					.apply(instance, (r, c, f) -> new RitualInstance(r, c, f)));
		}
		return CODEC;
	}
}
