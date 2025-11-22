package com.gm910.sotdivine.magic.emanation.spell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.gm910.sotdivine.util.CodecUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Properties of a SPELL
 * 
 * @author borah
 *
 */
public interface ISpellProperties {

	public static final Codec<ISpellProperties> CODEC = RecordCodecBuilder.create(instance -> // Given an emanation
	instance.group(SpellAlignment.CODEC.fieldOf("alignment").forGetter(ISpellProperties::alignment),
			CodecUtils.caselessEnumOrOrdinal(SpellPower.class).optionalFieldOf("difficulty", SpellPower.IMPOSSIBLE)
					.forGetter(ISpellProperties::difficulty),
			Codec.list(CodecUtils.caselessEnumCodec(SpellTrait.class)).optionalFieldOf("traits")
					.forGetter((a) -> Optional.of(new ArrayList<>(a.traits())))

	).apply(instance, SpellProperties::new));

	public static SpellProperties create(SpellAlignment alignment, SpellPower difficulty, SpellTrait... traits) {
		return new SpellProperties(alignment, difficulty, Optional.of(Set.of(traits)));
	}

	/**
	 * If this SPELL is helpful, harmful, both, or neither
	 * 
	 * @return
	 */
	public SpellAlignment alignment();

	/**
	 * This SPELL's general traits
	 * 
	 * @return
	 */
	public Collection<SpellTrait> traits();

	/**
	 * Difficulty of a spell
	 * 
	 * @return
	 */
	public SpellPower difficulty();
}
