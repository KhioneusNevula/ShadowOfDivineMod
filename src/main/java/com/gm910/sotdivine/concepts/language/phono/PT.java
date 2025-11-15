package com.gm910.sotdivine.concepts.language.phono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.gm910.sotdivine.concepts.language.feature.ISpecificationValue;
import com.gm910.sotdivine.util.ModUtils;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;
import com.mojang.logging.LogUtils;

public class PT implements IPhonotacticDisallow {

	private List<IPhonemeSelector> hotSequence;
	private Multimap<String, IPhonemeSelector> variables;
	private boolean atBegin = false;
	private boolean atEnd = false;

	public PT(List<IPhonemeSelector> sequence, Multimap<String, IPhonemeSelector> variables) {

		hotSequence = new ArrayList<>(sequence);
		if (sequence.get(0).getIdOrVar().equals(Optional.of("#"))) {
			this.atBegin = true;
			hotSequence.removeFirst();
		}
		if (sequence.get(sequence.size() - 1).getIdOrVar().equals(Optional.of("#"))) {
			this.atEnd = true;
			hotSequence.removeLast();
		}

		this.variables = ImmutableMultimap.copyOf(variables);
		for (IPhonemeSelector ps : sequence) {
			if (ps.isVariable() && variables.get(ps.getIdOrVar().get()).isEmpty()) {
				throw new IllegalArgumentException("Unmapped variable: " + ps);
			}
		}
	}

	@Override
	public boolean isAtBegin() {
		return atBegin;
	}

	@Override
	public boolean isAtEnd() {
		return atEnd;
	}

	@Override
	public List<IPhonemeSelector> getPrefix() {
		return Collections.unmodifiableList(hotSequence.subList(0, hotSequence.size() - 1));
	}

	@Override
	public IPhonemeSelector disallowedPhoneme() {
		return hotSequence.get(hotSequence.size() - 1);
	}

	@Override
	public Multimap<String, IPhonemeSelector> variables() {
		return variables;
	}

	@Override
	public String toString() {
		return "Phonotactic{sequence=" + (atBegin ? "(#)" : "") + hotSequence + (atEnd ? "(#)" : "")
				+ (variables.isEmpty() ? "" : ",variables=" + variables) + "}";
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof IPhonotacticDisallow pho) {
			return this.atBegin == pho.isAtBegin() && this.atEnd == pho.isAtEnd()
					&& this.hotSequence.subList(0, hotSequence.size() - 1).equals(pho.getPrefix())
					&& this.disallowedPhoneme().equals(pho.disallowedPhoneme())
					&& this.variables.equals(pho.variables());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.hotSequence.hashCode() + this.variables.hashCode();
	}

	@Override
	public boolean matchPattern(List<IPhoneme> sequence, IPhoneme phoneme) {
		Logger logger = LogUtils.getLogger();
		if (atEnd && phoneme != null) { // if end mismatch
			return false;
		}
		if (!atEnd && phoneme == null) { // when at end, unless our rule is specifically for the end, we always assume
											// failure
			return false;
		}
		if (atEnd && phoneme == null) {
			return true;
		}
		int prefixLength = hotSequence.size();
		int sequenceLength = sequence.size() + 1;
		// String seqString =
		// sequence.stream().map(IPhoneme::form).collect(ModUtils.setStringCollector());
		// logger.debug("Checking [" + seqString + "] + " + phoneme + " (" +
		// prefixLength + "," + sequenceLength + ") : "
		// + this);
		if (prefixLength > sequenceLength) // if the sequence is too short, assume failure
			return false;
		if (atBegin && sequenceLength > prefixLength) { // check we are at beginning
			// logger.debug(phoneme + " failed due to beginning mismatch: " + this);
			return false;
		}
		List<IPhoneme> matchSequence;
		if (sequence.size() < prefixLength)
			matchSequence = new ArrayList<>(sequence);
		else
			matchSequence = new ArrayList<>(sequence.subList(sequence.size() - prefixLength, sequence.size()));
		matchSequence.add(phoneme);
		Set<Map<String, IPhonemeSelector>> varMapSet = new HashSet<>();

		// basically we need to square out the possibilities of variable combinations
		boolean firstOfAll = true;
		for (String firstVar : this.variables.keySet()) {
			if (firstOfAll) {
				for (IPhonemeSelector firstVarValue : this.variables.get(firstVar)) {
					Map<String, IPhonemeSelector> selex = new HashMap<>();
					selex.put(firstVar, firstVarValue);
					varMapSet.add(selex);
				}
			} else {
				boolean first = true;
				for (IPhonemeSelector firstVarValue : this.variables.get(firstVar)) {
					for (Map<String, IPhonemeSelector> innermap : new ArrayList<>(varMapSet)) {
						if (first) {
							innermap.put(firstVar, firstVarValue);
						} else {
							Map<String, IPhonemeSelector> innermap2 = new HashMap<>(innermap);
							innermap2.put(firstVar, firstVarValue);
							varMapSet.add(innermap2);
						}
					}

					first = false;
				}
			}
			firstOfAll = false;
		}

		if (varMapSet.isEmpty()) {
			varMapSet.add(Map.of());
		}

		combosIter: for (Map<String, IPhonemeSelector> varMap : varMapSet) { // iterate combos of possible vars
			Map<String, String> localVarMap = new HashMap<>(); // map of vars checked within the combo
			for (int i = 0; i < prefixLength; i++) { // loop through each phoneme
				IPhonemeSelector currentSelector = this.hotSequence.get(i);
				boolean opposite = currentSelector.opposite();
				int crash = 10;
				while (currentSelector.isVariable()) { // try replace variable
					currentSelector = varMap.get(currentSelector.getIdOrVar().get());
					opposite = opposite != currentSelector.opposite();
					if (crash == 0)
						throw new IllegalArgumentException(
								"Girl what are you doing..." + this + " across " + sequence + " + " + phoneme);
					crash--;
				}
				IPhoneme currentPhoneme = matchSequence.get(i);
				if (currentSelector.getIdenticalSource().isPresent()) { // identical matching
					// logger.debug("Compare identicality at " + i + ": Phoneme " + currentPhoneme +
					// " to selector "
					// + currentSelector);
					IPhoneme identicalSource = matchSequence.get(currentSelector.getIdenticalSource().get());
					if (opposite == (currentPhoneme.equals(identicalSource))) { // if mismatch, next combo
						continue combosIter;
					}
				} else if (currentSelector.features().isEmpty()) {
					// logger.debug("Compare phoneme-id at " + i + ": Phoneme " + currentPhoneme + "
					// to selector "
					// + currentSelector);
					if (opposite == currentPhoneme.id().equals(currentSelector.getIdOrVar().get())) { // if mismatch,
																										// next combo
						continue combosIter;
					}
				} else {
					// logger.debug("Compare phoneme features at " + i + ": Phoneme " +
					// currentPhoneme + " to selector "
					// + currentSelector);
					boolean ffail = false;
					featureIter: for (Entry<String, ISpecificationValue> entry : currentSelector.features().get()
							.entrySet()) {
						if (!currentPhoneme.features().containsKey(entry.getKey())) { // if feature is underspecified,
																						// we cannot instrument
							if (entry.getValue().opposite()) { // if opposite, then it's a instrument, however
								continue;
							}
							ffail = true;
							break;
						}
						boolean featureOpposite = entry.getValue().opposite();
						String expectedValue;
						if (entry.getValue().isVariable()) { // try set variable
							expectedValue = localVarMap.get(entry.getValue().getVariable().get()); // check variable in
																									// local var map
							if (expectedValue == null) {// then check in the phonotactic's map
								expectedValue = Optional.ofNullable(varMap.get(entry.getValue().getVariable().get()))
										.filter((ps) -> ps.features().isEmpty() && ps.getIdOrVar().isPresent())
										.flatMap(IPhonemeSelector::getIdOrVar).orElse(null);
							}
							if (expectedValue == null) { // or add variable value to map and move on
								localVarMap.put(entry.getValue().getVariable().get(),
										currentPhoneme.features().get(entry.getValue().getVariable().get()));
								continue featureIter;
							}
						} else { // otherwise set literal
							expectedValue = entry.getValue().getLiteral().get();
						}

						boolean featureMatch = currentPhoneme.features().get(entry.getKey()).equals(expectedValue);
						if (featureMatch == featureOpposite) { // if feature mismatch, we failed
							ffail = true;
							break featureIter;
						}
					}
					if (ffail != opposite) { // if we failed and opposite is false, or if we didn't fail but opposite is
												// true, go to next combo
						continue combosIter;
					}

				}

			}
			// logger.debug(seqString + " + " + phoneme + " matched with var combo " +
			// varMap + ": " + this);
			return true;

		}
		// logger.debug(seqString + " + " + phoneme + " failed due to full mismatch: " +
		// this);
		return false;
	}

}
