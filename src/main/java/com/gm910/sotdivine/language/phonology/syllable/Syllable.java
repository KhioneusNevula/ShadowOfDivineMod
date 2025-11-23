package com.gm910.sotdivine.language.phonology.syllable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.gm910.sotdivine.language.phonology.phonemes.Phoneme;
import com.gm910.sotdivine.util.CollectionUtils;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;

public record Syllable(ListMultimap<SyllablePosition, Phoneme> phonemes) implements Cloneable {

	public Syllable(ListMultimap<SyllablePosition, Phoneme> phonemes) {
		this.phonemes = MultimapBuilder.enumKeys(SyllablePosition.class).arrayListValues().build(phonemes);
	}

	public String asString() {
		return flatList().stream().map((s) -> s.symbol()).collect(CollectionUtils.setStringCollector(""));
	}

	/**
	 * Returns (editable) view of phonemes
	 * 
	 * @return
	 */
	public ListMultimap<SyllablePosition, Phoneme> phonemes() {
		return phonemes;
	}

	/**
	 * Return all phonemes in this as a flat list
	 * 
	 * @return
	 */
	public List<Phoneme> flatList() {
		return new ArrayList<>(phonemes.values());
	}

	/**
	 * Returns a list of phoneme with inserted syllable boundary elements, i.e.
	 * {@link Phoneme#ONSET_NUCLEUS_BOUNDARY}, {@link Phoneme#NUCLEUS_CODA_BOUNDARY}
	 * 
	 * @return
	 */
	public List<Phoneme> boundaryMarkedList() {
		return CollectionUtils.concat(phonemes.get(SyllablePosition.ONSET), List.of(Phoneme.ONSET_NUCLEUS_BOUNDARY),
				phonemes.get(SyllablePosition.NUCLEUS), List.of(Phoneme.NUCLEUS_CODA_BOUNDARY),
				phonemes.get(SyllablePosition.CODA));
	}

	@Override
	public Syllable clone() {
		ListMultimap<SyllablePosition, Phoneme> map2 = MultimapBuilder.hashKeys().arrayListValues().build();
		for (Entry<SyllablePosition, Phoneme> entry : phonemes.entries()) {
			map2.put(entry.getKey(), entry.getValue());
		}

		return new Syllable(map2);
	}

	public static enum SyllablePosition {
		ONSET, NUCLEUS, CODA
	}

}
