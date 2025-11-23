package com.gm910.sotdivine.language.phonology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.gm910.sotdivine.language.phonology.constraints.PhonoConstraintHolder;
import com.gm910.sotdivine.language.phonology.phonemes.PhoneSet;
import com.gm910.sotdivine.language.phonology.phonemes.Phoneme;
import com.gm910.sotdivine.language.phonology.rules.IPhonoRule;
import com.gm910.sotdivine.language.phonology.syllable.PhonoWord;
import com.gm910.sotdivine.language.phonology.syllable.Syllable;
import com.gm910.sotdivine.language.phonology.syllable.Syllable.SyllablePosition;
import com.gm910.sotdivine.util.CollectionUtils;
import com.gm910.sotdivine.util.RandomUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;

import net.minecraft.util.RandomSource;

public class Phonology implements IPhonology {
	private Map<String, PhoneSet> definitions;
	private Set<PhonoConstraintHolder> forbidden;
	private Map<String, Phoneme> inventory;
	private Map<String, IPhonoRule> rules;
	private Map<PhoneSet, List<Phoneme>> resolvedPhoneSets = new HashMap<>();

	public Phonology(Collection<Phoneme> inventory, Map<String, PhoneSet> definitions,

			Collection<PhonoConstraintHolder> forbidden, Map<String, IPhonoRule> rules) {
		this.inventory = ImmutableMap.copyOf(inventory.stream().collect(Collectors.toMap((s) -> s.symbol(), (s) -> s)));
		this.definitions = ImmutableMap.copyOf(definitions);
		this.forbidden = ImmutableSet.copyOf(forbidden);
		this.rules = ImmutableMap.copyOf(rules);
		var ib = ImmutableMap.<String, IPhonoRule>builder().putAll(rules);
		if (!rules.containsKey(RULE_DELETE))
			ib = ib.put(RULE_DELETE, IPhonoRule.DELETE);
		rules = ib.build();

	}

	private List<Phoneme> resolve(PhoneSet set) {
		return resolvedPhoneSets.computeIfAbsent(set, (s) -> new ArrayList<>(s.getPhones(this)));
	}

	private Optional<Phoneme> randomPhoneme(PhoneSet from, RandomSource rand) {
		var list = resolve(from);
		if (list.isEmpty()) {
			return Optional.empty();
		}
		return RandomUtils.choose(rand, list);
	}

	@Override
	public PhonoWord generateSequence(int minsylla, int maxsylla, RandomSource random) {
		int count = random.nextIntBetweenInclusive(minsylla, maxsylla);
		Map<String, String> stages = new HashMap<>();

		Supplier<ListMultimap<SyllablePosition, Phoneme>> mapSup = () -> MultimapBuilder
				.enumKeys(SyllablePosition.class).arrayListValues().build();

		List<Syllable> syllables = new ArrayList<>();

		Set<Phoneme> possibleNuclei = new HashSet<>(inventory().values());
		possibleNuclei.removeIf((s) -> s.features().getOrDefault(SYLLABIC_FT, 0).equals(-1));
		for (int i = 0; i < count; i++) {
			RandomUtils.choose(random, possibleNuclei).ifPresentOrElse((s) -> {
				var map = mapSup.get();
				map.put(SyllablePosition.NUCLEUS, s);
				syllables.add(new Syllable(map));
			}, () -> {
				throw new IllegalStateException("Could not get any nuclei from inventory : " + possibleNuclei);
			});
		}
		stages.put("stage1",
				syllables.stream().map((s) -> s.asString()).collect(CollectionUtils.setStringCollector("")));

		Set<Phoneme> possibleOnsetsCodas = new HashSet<>(inventory().values());
		possibleOnsetsCodas.removeIf((s) -> s.features().getOrDefault(SYLLABIC_FT, 0).equals(1));
		// first pass: generate onsets
		for (int i = 0; i < syllables.size(); i++) {
			Syllable sound = syllables.get(i);
			RandomUtils.choose(random, possibleOnsetsCodas).ifPresentOrElse((s) -> {
				sound.phonemes().put(SyllablePosition.ONSET, s);
			}, () -> {
				throw new IllegalStateException("Could not get any nuclei from inventory : " + possibleNuclei);
			});
		}

		return new PhonoWord(syllables);
	}

	@Override
	public Map<String, Phoneme> inventory() {
		return inventory;
	}

	@Override
	public Map<String, PhoneSet> phoneSetDefinitions() {
		return definitions;
	}

	@Override
	public Collection<PhonoConstraintHolder> forbidden() {
		return forbidden;
	}

	@Override
	public Map<String, IPhonoRule> rules() {
		return rules;
	}
}
