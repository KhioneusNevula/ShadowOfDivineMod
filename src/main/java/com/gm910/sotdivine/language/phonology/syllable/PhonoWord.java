package com.gm910.sotdivine.language.phonology.syllable;

import java.util.ArrayList;
import java.util.List;

import com.gm910.sotdivine.language.phonology.phonemes.Phoneme;
import com.gm910.sotdivine.util.CollectionUtils;

/**
 * A phonological utterance
 */
public record PhonoWord(List<Syllable> syllables) {

	/**
	 * Return this phonological word as a single flat string of phonemes
	 * 
	 * @return
	 */
	public List<Phoneme> flatList() {
		List<Phoneme> ls = new ArrayList<>();
		syllables.forEach((s) -> ls.addAll(s.flatList()));
		return ls;
	}

	/**
	 * Returns a list of syllables with their boundaries marked and the word
	 * boundary marked
	 * 
	 * @return
	 */
	public List<Phoneme> boundaryMarkedList(boolean markWordBoundary) {
		return syllables.stream().map((s) -> s.boundaryMarkedList())
				.collect(CollectionUtils.separatorListOfListsCollector(Phoneme.SYLLABLE_BOUNDARY));
	}

	public String syllabifiedPhoneticString() {
		return syllables.stream().map(
				(s) -> s.flatList().stream().map((p) -> p.symbol()).collect(CollectionUtils.setStringCollector("")))
				.collect(CollectionUtils.setStringCollector("."));
	}
}
