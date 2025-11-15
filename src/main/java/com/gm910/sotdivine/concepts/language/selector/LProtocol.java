package com.gm910.sotdivine.concepts.language.selector;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import com.gm910.sotdivine.concepts.language.element.IConstituentTemplate;
import com.gm910.sotdivine.concepts.language.element.IWordTemplate;
import com.gm910.sotdivine.concepts.language.feature.ISemanticSpecificationValue;
import com.gm910.sotdivine.concepts.language.feature.ISpecificationValue;
import com.gm910.sotdivine.concepts.language.feature.ISemanticSpecificationValue.SemanticConstraint;
import com.gm910.sotdivine.concepts.language.generation.GeneratedConstituent;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class LProtocol implements ILSelector {

	public static final String KEY_SEMANTICS_KEY = "key";

	private Map<String, ISemanticSpecificationValue> semantics = new HashMap<>();
	private Map<String, ISemanticSpecificationValue> adjunctSemantics = new HashMap<>();
	private Multimap<String, ISpecificationValue> features = MultimapBuilder.hashKeys().hashSetValues().build();
	private Set<String> categories = new HashSet<>();
	private Set<String> capitalizeCategories = new HashSet<>();

	@Override
	public Map<String, ISpecificationValue> obtainSetVariables(GeneratedConstituent element) {

		return Map.of();
	}

	@Override
	public String toString() {
		return "Protocol{category=" + categories + ",semantics="
				+ (semantics.containsKey(null) ? "(unspecified)" : semantics) + ",adjunctSemantics="
				+ (adjunctSemantics.containsKey(null) ? "(unspecified)" : adjunctSemantics) + ",features=" + features
				+ ",capitalizeCategories=" + capitalizeCategories + "}";
	}

	/**
	 * Return which categories should be capitalized when producing an output
	 * 
	 * @return
	 */
	public Set<String> getCapitalizeCategories() {
		return capitalizeCategories;
	}

	/**
	 * 
	 * @param provider
	 * @param args
	 * @return
	 */
	public LProtocol parse(JsonElement element) {
		try {
			JsonObject object = element.getAsJsonObject();
			if (object.get(IConstituentTemplate.KEY_CATEGORY) instanceof JsonArray array) {
				array.forEach((aa) -> categories.add(aa.getAsString()));
			} else {
				this.categories.add(object.get(IConstituentTemplate.KEY_CATEGORY).getAsString());
			}

			if (object.get(IConstituentTemplate.KEY_CAPITALIZE) instanceof JsonArray array) {
				array.forEach((aa) -> this.capitalizeCategories.add(aa.getAsString()));
			} else if (object.has(IConstituentTemplate.KEY_CAPITALIZE)) {
				this.capitalizeCategories.add(object.get(IConstituentTemplate.KEY_CAPITALIZE).getAsString());
			}

			if (object.get(IConstituentTemplate.KEY_FEATURES) instanceof JsonObject fea) {
				for (String feature : fea.keySet()) {
					if (fea.get(feature) instanceof JsonArray array) {
						array.forEach((x) -> features.put(feature, ISpecificationValue.parse(x)));
					} else {
						ISpecificationValue sv = ISpecificationValue.parse(fea.get(feature));

						features.put(feature, sv);

					}
				}
			}
			for (String semanticsMapJsonKey : new String[] { IConstituentTemplate.KEY_SEMANTICS,
					IConstituentTemplate.KEY_ADJUNCT_SEMANTICS }) {
				Map<String, ISemanticSpecificationValue> semantics = semanticsMapJsonKey
						.equals(IConstituentTemplate.KEY_SEMANTICS) ? this.semantics : this.adjunctSemantics;
				if (object.get(semanticsMapJsonKey) instanceof JsonObject semanticJson) {
					for (String categoire : semanticJson.keySet()) {
						ISemanticSpecificationValue specVal = ISemanticSpecificationValue.empty()
								.parse(semanticJson.get(categoire), true);
						semantics.put(categoire, specVal);
					}
				} else { // true/false
					if (object.get(semanticsMapJsonKey).getAsBoolean()) {
						semantics.clear();
						semantics.put(null, ISemanticSpecificationValue.minimumSpecification());
						// this placeholder value signifies that we derive all our arguments from an
						// environmental map
					} else {
						throw new JsonParseException("");
					}
				}
			}

			return this;
		} catch (Exception e) {
			throw new JsonParseException("While parsing protocol from " + element, e);
		}
	}

	public Set<String> category() {
		return Collections.unmodifiableSet(this.categories);
	}

	public Map<String, ISemanticSpecificationValue> semantics() {
		return Collections.unmodifiableMap(this.semantics);
	}

	public Map<String, ISemanticSpecificationValue> adjunctSemantics() {
		return Collections.unmodifiableMap(this.adjunctSemantics);
	}

	public Multimap<String, ISpecificationValue> features() {
		return Multimaps.unmodifiableMultimap(this.features);
	}

	public LProtocol copy(LProtocol other) {
		adjunctSemantics = new HashMap<>(other.adjunctSemantics);
		categories = new HashSet<>(other.categories);
		features = MultimapBuilder.hashKeys().hashSetValues().build(other.features);
		semantics = new HashMap<>(other.semantics);
		capitalizeCategories = new HashSet<>(other.capitalizeCategories);
		return this;
	}

	@Override
	public LProtocol withUpdatedVariables(Multimap<String, ISpecificationValue> variables) {
		LProtocol protocolNew = new LProtocol();
		protocolNew.copy(this);
		protocolNew.features.asMap().entrySet().forEach((en) -> {
			for (ISpecificationValue spec : new HashSet<>(en.getValue())) {
				if (spec.getVariable().filter(variables::containsKey).isPresent()) {
					variables.get(spec.getVariable().get()).forEach((var) -> en.getValue().add(var));
					en.getValue().remove(spec);
				}
			}
		});
		return protocolNew;
	}

	@Override
	public ILSelector withUpdatedSemanticVariables(Map<String, ISemanticSpecificationValue> variables) {
		if (!variables.values().stream().anyMatch((x) -> x.isVariable() || x.isSelective()))
			throw new IllegalArgumentException("No variable or selector in argument: " + variables);
		LProtocol protocolNew = new LProtocol();
		protocolNew.copy(this);
		Predicate<Entry<String, ISemanticSpecificationValue>> filterer = (en) -> en.getValue().getVariable()
				.filter(variables::containsKey).isPresent();
		List.of(semantics, adjunctSemantics).forEach((seman) -> {
			if (seman.containsKey(null)) {
				seman.remove(null);
				variables.forEach((feature, value) -> seman.put(feature, value));
			} else {
				seman.entrySet().stream().filter(filterer)
						.forEach((en) -> en.setValue(variables.get(en.getValue().getVariable().get())));
			}
		});

		return protocolNew;
	}

	@Override
	public boolean test(IConstituentTemplate testee) {
		if (!this.categories.contains(testee.category())) {
			return false;
		}
		for (String feature : this.features.keySet()) {
			ISpecificationValue compareSpec = testee.features().get(feature);
			if (compareSpec == null) { // if failed to exist then no instrument
				return false;
			}
			if (compareSpec.isVariable()) { // if variable,
				continue;
			}
			boolean match = false;
			for (ISpecificationValue selectorVal : this.features.get(feature)) {
				if (selectorVal.getLiteral().orElse(null) instanceof String comparison) {
					if (comparison.equals(compareSpec.getLiteral().get())) {
						if (selectorVal.opposite() != compareSpec.opposite()) {
							return false;
						} else {
							// System.out.println("Matched!");
							match = true;
							break;
						}
					} else {
						if (selectorVal.opposite() != compareSpec.opposite()) {
							match = true;
							break;
						}
					}
				} else { // if this variable has no value, then it can freely instrument\
					match = true;
					break;
				}
			}
			if (!match) {
				// System.out.println("No matches; fail.");
				return false;
			}
		}
		// no need for strong semantic checks, since phrases have no semantics

		if (testee instanceof IWordTemplate wordTestee) {
			for (String categoire : this.semantics.keySet()) {
				ISemanticSpecificationValue selectorSpec = semantics.get(categoire);
				if (selectorSpec.isVariable() || selectorSpec.isNonSelective())
					continue;
				ISemanticSpecificationValue testeeSpec = wordTestee.semantics().get(categoire);
				if (testeeSpec == null || testeeSpec.isSelective()) {
					if (selectorSpec.mustBeSpecified()) {
						return false;
					}
					continue;
				}
				if (testeeSpec.isVariable())
					continue;
				Collection<String> testeeValues = testeeSpec.getConstraint(SemanticConstraint.VALUE).get();
				for (String item : selectorSpec.getConstraint(SemanticConstraint.REQUIRED).get()) {
					if (!testeeValues.contains(item)) {
						return false;
					}
				}
				for (String item : selectorSpec.getConstraint(SemanticConstraint.DISALLOWED).get()) {
					if (testeeValues.contains(item))
						return false;
				}
				if (selectorSpec.getConstraint(SemanticConstraint.ALLOWED).get().size() > 0) {
					boolean match = false;
					for (String item : selectorSpec.getConstraint(SemanticConstraint.ALLOWED).get()) {
						if (testeeValues.contains(item)) {
							match = true;
							break;
						}
					}
					if (!match)
						return false;
				}
			}
		}

		// System.out.println("Test success.");
		return true;
	}

}
