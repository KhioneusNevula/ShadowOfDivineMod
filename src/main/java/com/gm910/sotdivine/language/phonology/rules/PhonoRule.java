package com.gm910.sotdivine.language.phonology.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gm910.sotdivine.language.phonology.IPhonology;
import com.gm910.sotdivine.language.phonology.phonemes.PhoneSet;
import com.gm910.sotdivine.language.phonology.phonemes.Phoneme;
import com.google.common.collect.Lists;

public record PhonoRule(RuleType ruleType, Optional<PhoneSet> optionalElement,
		Optional<Map<String, String>> optionalFeatures, Optional<Integer> optionalDestination) implements IPhonoRule {

	@Override
	public List<Phoneme> transform(List<Phoneme> appliedTo, int position, IPhonology phonology) {
		List<Phoneme> newList = new ArrayList<>(appliedTo);
		Optional<Phoneme> possibleElement = optionalElement.map((ps) -> new ArrayList<>(ps.getPhones(phonology)))
				.filter((s) -> !s.isEmpty()).map((s) -> {
					Collections.shuffle(s);
					return s;
				}).map((l) -> l.getFirst());
		var nonRetrieve = new IllegalStateException(
				"No element retrieved from " + optionalElement + " in " + appliedTo);
		var nonRetrievePos = new IllegalStateException("No rawPosition retrieved from " + optionalDestination);
		var noFeature = new IllegalStateException(
				"No feature found from one of " + optionalFeatures + " in " + appliedTo);
		Map<String, Object> features = ruleType.changeStructure ? Map.of()
				: new HashMap<>(newList.get(position).features());
		switch (ruleType) {
		case CHANGE:
			newList.set(position, possibleElement.orElseThrow(() -> nonRetrieve));
			break;
		case DELETE:
			newList.remove(position);
			break;
		case INSERT:
			newList.add(position, possibleElement.orElseThrow(() -> nonRetrieve));
			break;
		case MOVE:
			int dest = optionalDestination.orElseThrow(() -> nonRetrievePos);
			int factor = dest < position ? 0 : -1;
			newList.add(dest + factor, newList.remove(position));
			break;
		case DELETE_FEATURE:
			optionalFeatures.orElseThrow(() -> noFeature).keySet().forEach((feature) -> features.remove(feature));
			break;
		case INVERT_FEATURE:
			optionalFeatures.orElseThrow(() -> noFeature).keySet().forEach((feature) -> {
				int newval = 0;
				try {
					newval = -Integer.parseInt(features.get(feature).toString());
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				features.put(feature, newval + "");
			});
			break;
		case SET_FEATURE:
			optionalFeatures.orElseThrow(() -> noFeature).forEach((k, v) -> features.put(k, v));
			break;
		default:
			return appliedTo;
		}
		if (!features.isEmpty() && !features.equals(newList.get(position).features())) {
			List<Phoneme> selected = Lists.newArrayList(
					phonology.inventory().values().stream().filter((s) -> s.features().equals(features)).iterator());
			Collections.shuffle(selected);
			if (selected.isEmpty()) {
				return null;
			}
			newList.set(position, selected.getFirst());
		}
		return newList;
	}

}
