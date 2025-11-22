package com.gm910.sotdivine.language.phonology.constraints;

import java.util.ArrayList;
import java.util.List;

import com.gm910.sotdivine.language.phonology.IPhonology;
import com.gm910.sotdivine.language.phonology.phonemes.PhoneSet;
import com.gm910.sotdivine.language.phonology.phonemes.Phoneme;
import com.gm910.sotdivine.util.CodecUtils;
import com.google.common.collect.Range;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record SequenceConstraint(List<PhoneSet> sequence) implements IPhonoConstraint {

	public static final MapCodec<SequenceConstraint> MAP_CODEC = RecordCodecBuilder
			.mapCodec(instance -> instance
					.group(CodecUtils.listOrSingleCodec(PhoneSet.CODEC).fieldOf("sequence")
							.forGetter(SequenceConstraint::sequence))
					.apply(instance, (s) -> new SequenceConstraint(s)));

	public static final String NAME = "sequence";

	@Override
	public String constraintType() {
		return NAME;
	}

	@Override
	public List<Range<Integer>> sequenceViolation(List<Phoneme> phonemes, IPhonology phonology) {

		List<Range<Integer>> ranges = new ArrayList<>();
		for (int i = sequence.size() - 1; i < phonemes.size(); i++) {
			int start = i - (sequence.size() - 1);
			for (int j = 0; j < sequence.size(); j++) {
				if (!sequence.get(j).contains(phonemes.get(start + j), phonology)) {
					ranges.add(Range.closed(start, i));
					break;
				}
			}
		}
		return ranges;
	}
}
