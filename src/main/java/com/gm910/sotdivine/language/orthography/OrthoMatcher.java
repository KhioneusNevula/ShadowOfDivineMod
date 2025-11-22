package com.gm910.sotdivine.language.orthography;

import java.util.ArrayList;
import java.util.List;

import com.gm910.sotdivine.language.phonology.IPhonology;
import com.gm910.sotdivine.language.phonology.phonemes.PhoneSet;
import com.gm910.sotdivine.language.phonology.phonemes.Phoneme;
import com.google.common.collect.Range;

import net.minecraft.network.chat.Component;

public record OrthoMatcher(List<PhoneSet> sequence, Range<Integer> replaceIndices, Component surfaceForm)
		implements IOrthoMatcher {

	@Override
	public List<Range<Integer>> findForReplacement(List<Phoneme> phonemes, IPhonology phonology) {
		List<Range<Integer>> ranges = new ArrayList<>();

		for (int i = sequence.size() - 1; i < phonemes.size(); i++) {
			int start = i - (sequence.size() - 1);
			for (int j = 0; j < sequence.size(); j++) {
				if (!sequence.get(j).contains(phonemes.get(start + j), phonology)) {
					ranges.add(Range.closed(start, i));
					i += sequence.size() - 1;
					break;
				}
			}
		}
		return ranges;
	}
}
