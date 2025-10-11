package com.gm910.sotdivine.systems.deity.emanation.spell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Properties of a spell
 * 
 * @author borah
 *
 */
public interface ISpellProperties {

	public static final Codec<ISpellProperties> CODEC = RecordCodecBuilder.create(instance -> // Given an instance
	instance.group(
			Codec.STRING.xmap(SpellAlignment::valueOf, SpellAlignment::name).fieldOf("alignment")
					.forGetter(ISpellProperties::alignment),

			Codec.list(Codec.STRING.xmap(SpellTrait::valueOf, SpellTrait::name)).optionalFieldOf("traits")
					.forGetter((a) -> Optional.of(new ArrayList<>(a.traits())))

	).apply(instance, SpellProperties::new));

	public static SpellProperties create(SpellAlignment alignment, SpellTrait... traits) {
		return new SpellProperties(alignment, Optional.of(Set.of(traits)));
	}

	/**
	 * If this spell is helpful, harmful, both, or neither
	 * 
	 * @return
	 */
	public SpellAlignment alignment();

	/**
	 * This spell's general traits
	 * 
	 * @return
	 */
	public Collection<SpellTrait> traits();
}
