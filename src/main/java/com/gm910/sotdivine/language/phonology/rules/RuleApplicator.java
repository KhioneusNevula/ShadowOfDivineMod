package com.gm910.sotdivine.language.phonology.rules;

import java.util.List;

import com.gm910.sotdivine.language.phonology.IPhonology;
import com.gm910.sotdivine.language.phonology.phonemes.Phoneme;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Applies a ruleOrString at a given position
 */
public record RuleApplicator(Either<IPhonoRule, String> ruleOrString, int rawPosition) {
	public static final Codec<RuleApplicator> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(Codec.either(IPhonoRule.CODEC, Codec.STRING).fieldOf("rule").forGetter(RuleApplicator::ruleOrString),
					Codec.INT.fieldOf("at").forGetter(RuleApplicator::rawPosition))
			.apply(instance, (r, p) -> new RuleApplicator(r, p)));

	public IPhonoRule rule(IPhonology phonology) {
		return ruleOrString.map((r) -> r, (s) -> phonology.rules().getOrDefault(s, IPhonoRule.NOTHING));
	}

	/**
	 * Returns the position but adjusted to match computational indices
	 */
	public int position() {
		return rawPosition - 1;
	}

	/**
	 * Applies this transformation. Calls
	 * {@link IPhonoRule#transform(List, int, IPhonology)}
	 * 
	 * @param appliedTo
	 * @return
	 */
	public List<Phoneme> transform(List<Phoneme> appliedTo, IPhonology phonology) {
		return this.rule(phonology).transform(appliedTo, rawPosition, phonology);
	}
}
