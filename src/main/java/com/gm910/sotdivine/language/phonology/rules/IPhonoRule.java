package com.gm910.sotdivine.language.phonology.rules;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gm910.sotdivine.language.phonology.IPhonology;
import com.gm910.sotdivine.language.phonology.phonemes.PhoneSet;
import com.gm910.sotdivine.language.phonology.phonemes.Phoneme;
import com.gm910.sotdivine.util.CodecUtils;
import com.google.common.base.Functions;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Applies a phonological transformation
 */
public interface IPhonoRule {

	public static final IPhonoRule DELETE = new PhonoRule(RuleType.DELETE, Optional.empty(), Optional.empty(),
			Optional.empty());

	public static final Codec<IPhonoRule> CODEC = RecordCodecBuilder.create(instance -> instance
			.group(CodecUtils.caselessEnumCodec(RuleType.class).fieldOf("type").forGetter(IPhonoRule::ruleType),
					PhoneSet.CODEC.optionalFieldOf("elements").forGetter(IPhonoRule::optionalElement), Codec
							.<Map<String, String>, Map<String, String>>either(
									CodecUtils.listOrSingleCodec(Codec.STRING).flatComapMap(
											(s) -> Maps.asMap(new HashSet<>(s), Functions.identity()),
											(s) -> DataResult.error(() -> "cannot listify map" + s)),
									Codec.unboundedMap(Codec.STRING, Codec.STRING))
							.xmap((s) -> Either.unwrap(s), (s) -> Either.right(s)).optionalFieldOf("features")
							.forGetter(IPhonoRule::optionalFeatures),
					Codec.INT.optionalFieldOf("destination").forGetter(IPhonoRule::optionalDestination))
			.apply(instance, (r, e, f, d) -> new PhonoRule(r, e, f, d)));

	public static final IPhonoRule NOTHING = new PhonoRule(RuleType.NOTHING, Optional.empty(), Optional.empty(),
			Optional.empty());

	/**
	 * Returns the type of ruleOrString that this is
	 * 
	 * @return
	 */
	public RuleType ruleType();

	/**
	 * An optional element to insert or change to (i.e. {@link RuleType#INSERT} or
	 * {@link RuleType#CHANGE}
	 * 
	 * @return
	 */
	public Optional<PhoneSet> optionalElement();

	/**
	 * An optional destination to move a segment to
	 * 
	 * @return
	 */
	public Optional<Integer> optionalDestination();

	/**
	 * An optional set of features to set the value of (i.e.
	 * {@link RuleType#SET_FEATURE} or {@link RuleType#INVERT_FEATURE} or
	 * {@link RuleType#DELETE_FEATURE}). If this is {@link RuleType#INVERT_FEATURE}
	 * or {@link RuleType#DELETE_FEATURE} then the values of the map do not matter
	 * and may be anything.
	 * 
	 * @return
	 */
	public Optional<Map<String, String>> optionalFeatures();

	/**
	 * Applies this transformation at the given rawPosition in a violating sequence of
	 * phonemes. Return null if this was unsuccessful, e.g. changing a feature to a
	 * phoneme that is not present
	 * 
	 * @param appliedTo
	 * @return
	 */
	public List<Phoneme> transform(List<Phoneme> appliedTo, int position, IPhonology phonology);
}
