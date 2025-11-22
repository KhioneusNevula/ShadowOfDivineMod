package com.gm910.sotdivine.language.phonology.constraints;

import java.util.ArrayList;
import java.util.List;

import com.gm910.sotdivine.language.phonology.IPhonology;
import com.gm910.sotdivine.language.phonology.phonemes.Phoneme;
import com.google.common.collect.Range;

public enum DuplicationConstraint implements IPhonoConstraint {
	INSTANCE;

	public static final String NAME = "duplicates";

	@Override
	public String constraintType() {
		return NAME;
	}

	@Override
	public List<Range<Integer>> sequenceViolation(List<Phoneme> phonemes, IPhonology phonology) {
		List<Range<Integer>> ranges = new ArrayList<>();
		for (int i = 0; i < phonemes.size(); i++) {
			Phoneme pho = phonemes.get(i);
			if (i > 0) {
				Phoneme prior = phonemes.get(i - 1);
				if (pho.symbol().equals(prior.symbol())) {
					ranges.add(Range.closed(i - 1, i));
				}
			}
		}
		return ranges;
	}
}
