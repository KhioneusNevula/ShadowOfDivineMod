package com.gm910.sotdivine.language.phonology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gm910.sotdivine.language.phonology.constraints.PhonoConstraintHolder;
import com.gm910.sotdivine.language.phonology.phonemes.PhoneSet;
import com.gm910.sotdivine.language.phonology.phonemes.Phoneme;
import com.gm910.sotdivine.language.phonology.rules.IPhonoRule;
import com.gm910.sotdivine.language.phonology.syllable.PhonoWord;
import com.gm910.sotdivine.util.CodecUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.RandomSource;

public interface IPhonology {

	/**
	 * See {@link Phoneme}
	 */
	public static final Set<String> RESERVED_SYMBOLS = Set.of("#", "+", ".", "|", "]");
	/**
	 * the universal feature (with -1 and +1 values) used to select for syllable
	 * nuclei
	 */
	public static final String SYLLABIC_FT = "syllabic";
	/** the universal set used to select for syllable nuclei */
	public static final PhoneSet SYLLABIC_PHONESET = new PhoneSet(Map.of(SYLLABIC_FT, 1), Set.of(), PhoneSet.EMPTY);
	public static final String RULE_DELETE = "delete";

	public static Codec<IPhonology> createCodec() {
		return RecordCodecBuilder
				.create(instance -> instance
						.group(Codec.list(
								Phoneme.CODEC).fieldOf("inventory")
								.forGetter((s) -> new ArrayList<>(s.inventory().values())),
								Codec.unboundedMap(Codec.STRING.validate((s) -> RESERVED_SYMBOLS.contains(s)
										? DataResult.error(() -> "Reserved symbol ", s)
										: DataResult.success(s)), PhoneSet.CODEC).optionalFieldOf("define", Map.of())
										.forGetter(IPhonology::phoneSetDefinitions),
								CodecUtils.listOrSingleCodec(PhonoConstraintHolder.CODEC)
										.optionalFieldOf("forbidden", List.of())
										.forGetter((s) -> new ArrayList<>(s.forbidden())),
								Codec.unboundedMap(Codec.STRING, IPhonoRule.CODEC).optionalFieldOf("rules", Map.of())
										.forGetter(IPhonology::rules))
						.apply(instance, (i, d, f, r) -> new Phonology(i, d, f, r)));
	}

	/**
	 * Generate a sequence of phonological symbols
	 * 
	 * @param minSyllables
	 * @param maxSyllables
	 * @return
	 */
	public PhonoWord generateSequence(int minSyllables, int maxSyllables, RandomSource random);

	/**
	 * Inventory of sounds in this language
	 * 
	 * @return
	 */
	public Map<String, Phoneme> inventory();

	/**
	 * Return the definitions of sets
	 * 
	 * @return
	 */
	public Map<String, PhoneSet> phoneSetDefinitions();

	/**
	 * The defined rules in this phonology
	 * 
	 * @return
	 */
	public Map<String, IPhonoRule> rules();

	/**
	 * Non permitted sequences
	 * 
	 * @return
	 */
	public Collection<PhonoConstraintHolder> forbidden();

}
