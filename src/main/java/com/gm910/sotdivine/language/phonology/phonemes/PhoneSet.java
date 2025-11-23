package com.gm910.sotdivine.language.phonology.phonemes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.gm910.sotdivine.language.phonology.IPhonology;
import com.gm910.sotdivine.util.CodecUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A set of phones
 */
public record PhoneSet(Map<String, Object> features, Set<String> exact, PhoneSet excluded) {

	/**
	 * The set of no phonemes
	 */
	public static final PhoneSet EMPTY = new PhoneSet(Map.of(), Set.of(), null);
	/** The set of all phonemes */
	public static final PhoneSet ALL = new PhoneSet(Map.of(), Set.of(), EMPTY);

	public PhoneSet(String... symbols) {
		this(Map.of(), Set.of(symbols), EMPTY);
	}

	public static final PhoneSet MORPHEME_BOUNDARY = Phoneme.MORPHEME_BOUNDARY.asSet();
	public static final PhoneSet SYLLABLE_BOUNDARY = new PhoneSet(Phoneme.SYLLABLE_BOUNDARY.symbol(),
			Phoneme.WORD_BOUNDARY.symbol());
	public static final PhoneSet WORD_BOUNDARY = Phoneme.WORD_BOUNDARY.asSet();
	public static final PhoneSet ONSET_NUCLEUS_BOUNDARY = Phoneme.ONSET_NUCLEUS_BOUNDARY.asSet();
	public static final PhoneSet NUCLEUS_CODA_BOUNDARY = Phoneme.NUCLEUS_CODA_BOUNDARY.asSet();

	private static final Codec<PhoneSet> createCodec() {
		Codec<PhoneSet> fromStringList = CodecUtils.listOrSingleCodec(Codec.STRING)
				.flatComapMap((s) -> new PhoneSet(Map.of(), new HashSet<>(s), EMPTY), (p) -> {
					if (!p.features().isEmpty() || !p.excluded().isEmpty())
						return DataResult.error(() -> "Too complex : " + p);
					return DataResult.success(new ArrayList<>(p.exact()));
				});
		Codec<Map<String, Object>> anyMap = Codec.STRING.<Map<String, Object>>flatXmap(s -> {
			if (s.equalsIgnoreCase("any")) {
				Map<String, Object> oot = new HashMap<>();
				oot.put("", Boolean.TRUE);
				return DataResult.<Map<String, Object>>success(oot);
			}
			return DataResult.<Map<String, Object>>error(() -> "can only be the string 'any'");
		}, s -> {
			if (s.containsKey(""))
				return DataResult.<String>success("any");
			return DataResult.<String>error(() -> "must be anymap");
		});

		Codec<Map<String, Object>> featuresProper = Codec.unboundedMap(Codec.STRING,
				CodecUtils.multiCodecEither(Codec.BOOL, Codec.INT, Codec.STRING));
		Function<Codec<PhoneSet>, Codec<PhoneSet>> phoneSetCodecRecursive = (pscodec) -> RecordCodecBuilder
				.create(instance -> instance
						.group(CodecUtils.multiCodecEither(anyMap, featuresProper).optionalFieldOf("features", Map.of())
								.forGetter(PhoneSet::features),
								CodecUtils.listOrSingleCodec(Codec.STRING).optionalFieldOf("and", List.of())
										.forGetter((s) -> new ArrayList<>(s.exact())),
								pscodec.optionalFieldOf("not", EMPTY).forGetter(PhoneSet::excluded))
						.apply(instance, (f, e, ex) -> new PhoneSet(Map.copyOf(f), new HashSet<>(e), ex)));
		return Codec.recursive("phoneSet", (recursor) -> CodecUtils.multiCodecEither(fromStringList, phoneSetCodecRecursive.apply(recursor)));
	}

	public static final Codec<PhoneSet> CODEC = createCodec();

	/**
	 * If this set does not specify anything is contained in it
	 * 
	 * @return
	 */
	public boolean isEmpty() {
		return this == EMPTY;
	}

	public PhoneSet excluded() {
		if (excluded == null)
			return EMPTY;
		return this.excluded;
	}

	/**
	 * If the given phoneme is in this set
	 * 
	 * @param phone
	 * @return
	 */
	public boolean contains(Phoneme phone, IPhonology phono) {
		if (this == EMPTY)
			return false;
		if (this == ALL)
			return phono.inventory().containsKey(phone.symbol());
		if (this == SYLLABLE_BOUNDARY)
			return phone == Phoneme.WORD_BOUNDARY || phone == Phoneme.SYLLABLE_BOUNDARY;
		if (this == WORD_BOUNDARY)
			return phone == Phoneme.WORD_BOUNDARY;
		if (this == MORPHEME_BOUNDARY)
			return phone == Phoneme.MORPHEME_BOUNDARY;
		if (this == ONSET_NUCLEUS_BOUNDARY)
			return phone == Phoneme.ONSET_NUCLEUS_BOUNDARY;
		if (this == NUCLEUS_CODA_BOUNDARY)
			return phone == Phoneme.NUCLEUS_CODA_BOUNDARY;
		return contains(phone, this.expandSet(exact, phono), excluded.getPhones(phono));
	}

	private boolean contains(Phoneme phone, Set<String> updatedExacts, Collection<Phoneme> updatedExcludeds) {
		if (updatedExacts.contains(phone.symbol()))
			return true;
		if (updatedExcludeds.contains(phone))
			return false;
		if (!features.isEmpty()) {
			if (!features.containsKey("")) {
				if (!phone.features().keySet().containsAll(features.keySet())) {
					return false;
				}
				for (String feature : phone.features().keySet()) {
					if (!phone.features().get(feature).equals(features.get(feature))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private Set<String> expandSet(Set<String> set, IPhonology defs) {
		Set<String> copy = new HashSet<>(set);
		for (String group : set) {
			if (defs.phoneSetDefinitions().containsKey(group)) {
				copy.remove(group);
				defs.phoneSetDefinitions().get(group).getPhones(defs).forEach((p) -> copy.add(p.symbol()));
			}
		}
		return copy;
	}

	/**
	 * Returns a set of all phones that would be part of this set, based on the
	 * given inventory
	 * 
	 * @param inventory
	 * @return
	 */
	public Collection<Phoneme> getPhones(IPhonology phonology) {
		if (this == EMPTY)
			return Set.of();
		if (this == ALL)
			return phonology.inventory().values();
		if (this == SYLLABLE_BOUNDARY)
			return Set.of(Phoneme.WORD_BOUNDARY, Phoneme.SYLLABLE_BOUNDARY);
		if (this == WORD_BOUNDARY)
			return Set.of(Phoneme.WORD_BOUNDARY);
		if (this == MORPHEME_BOUNDARY)
			return Set.of(Phoneme.MORPHEME_BOUNDARY);
		if (this == ONSET_NUCLEUS_BOUNDARY)
			return Set.of(Phoneme.ONSET_NUCLEUS_BOUNDARY);
		if (this == NUCLEUS_CODA_BOUNDARY)
			return Set.of(Phoneme.NUCLEUS_CODA_BOUNDARY);
		Set<Phoneme> phones = new HashSet<>(phonology.inventory().values());
		Set<String> exacts = this.expandSet(exact, phonology);
		Collection<Phoneme> excludeds = this.excluded.getPhones(phonology);

		phones.removeIf((s) -> !contains(s, exacts, excludeds));
		return phones;
	}

}
