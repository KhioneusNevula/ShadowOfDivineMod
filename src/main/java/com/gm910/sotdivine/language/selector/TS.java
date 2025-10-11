package com.gm910.sotdivine.language.selector;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gm910.sotdivine.language.element.IConstituentTemplate;
import com.gm910.sotdivine.language.element.IWordTemplate;
import com.gm910.sotdivine.language.feature.ISemanticSpecificationValue;
import com.gm910.sotdivine.language.feature.ISemanticSpecificationValue.SemanticConstraint;
import com.gm910.sotdivine.language.feature.ISpecificationValue;
import com.gm910.sotdivine.language.generation.GeneratedConstituent;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

class TS implements ITemplateSelector {

	Collection<String> categories;
	Multimap<String, ISpecificationValue> featuresMap;
	Map<String, ISemanticSpecificationValue> semanticsMap;
	Map<String, ISemanticSpecificationValue> aSemanticsMap;
	private boolean head;

	TS() {
		this.categories = new HashSet<String>();
		this.featuresMap = MultimapBuilder.hashKeys().hashSetValues().build();
		this.semanticsMap = new HashMap<>();
		this.aSemanticsMap = new HashMap<>();
	}

	TS setHead() {
		this.head = true;
		return this;
	}

	TS addCategories(Collection<String> srs) {
		this.categories.addAll(srs);
		return this;
	}

	TS addFeatures(Multimap<String, ISpecificationValue> srs) {
		this.featuresMap.putAll(srs);
		return this;
	}

	TS addSemantics(Map<String, ISemanticSpecificationValue> srs) {
		this.semanticsMap.putAll(srs);
		this.semanticsMap.remove(null);
		return this;
	}

	TS addAdjunctSemantics(Map<String, ISemanticSpecificationValue> srs) {
		this.aSemanticsMap.putAll(srs);
		this.aSemanticsMap.remove(null);
		return this;
	}

	@Override
	public Collection<String> category() {
		return categories;
	}

	@Override
	public boolean isHead() {
		return head;
	}

	@Override
	public Multimap<String, ISpecificationValue> features() {
		return Multimaps.unmodifiableMultimap(featuresMap);
	}

	@Override
	public Map<String, ISemanticSpecificationValue> semantics() {
		return Collections.unmodifiableMap(semanticsMap);
	}

	@Override
	public Map<String, ISemanticSpecificationValue> adjunctSemantics() {
		return Collections.unmodifiableMap(aSemanticsMap);
	}

	@Override
	public Map<String, ISpecificationValue> obtainSetVariables(GeneratedConstituent element) {
		Map<String, ISpecificationValue> varMap = new HashMap<>();
		featuresMap.entries().stream().filter((en) -> en.getValue().isVariable()).forEach((en) -> {
			if (element.getFeaturesEditable().containsKey(en.getKey())) {
				varMap.put(en.getValue().getVariable().get(),
						ISpecificationValue.literal(element.getFeaturesEditable().get(en.getKey())));
			}
		});

		return varMap;
	}

	@Override
	public boolean test(IConstituentTemplate element) {
		if (!this.category().contains(element.category())) {
			return false;
		}

		/*
		 * System.out.println(">><<Startselector>> Testing " + element +
		 * " with selector " + this + " and environment " + environment);
		 */
		/*
		 * System.out.println("  <<selector>> Info of element " +
		 * element.detailedString());
		 */
		for (String feature : this.features().keySet()) {
			/* System.out.println("  <<selector>> Testing feature " + feature); */
			ISpecificationValue comparingSpecification = element.features().get(feature);
			if (comparingSpecification == null) {
				/*
				 * System.out.println("  <<selector>> Ignoring feature " + feature +
				 * " since it is not specified");
				 */
				continue; // ignore if there is no specification of the feature
			}
			if (comparingSpecification.getLiteral().isEmpty()) {
				/*
				 * System.out.println("  <<selector>> Element has unbound variable " +
				 * comparingSpecification.getVariable().orElse(null) + "; ignoring");
				 */
				continue; // ignore if the element's feature is an unbound variable
			}
			boolean match = false;
			for (ISpecificationValue selectorSpecificationVariant : this.features().get(feature)) {
				/*
				 * System.out .println("  <<selector>> Using selector specification variant " +
				 * selectorSpecificationVariant);
				 */
				String selectorValue = selectorSpecificationVariant.getLiteral().orElse(null);
				if (selectorValue == null) {
					/*
					 * System.out.println("  <<selector>> Selector has unbound variable " +
					 * selectorSpecificationVariant.getVariable().orElse(null) + "; auto-match");
					 */
					match = true; // if unbound variable, then this is a match
					break;
				}
				if (selectorValue.equals(comparingSpecification.getLiteral().get())) {
					if (selectorSpecificationVariant.opposite() != comparingSpecification.opposite()) {

						/*
						 * System.out.println("  <<selector>> Mismatch between selector value " +
						 * selectorValue + "(" + !selectorSpecificationVariant.opposite() + ")" +
						 * " and element value " + comparingSpecification.getLiteral().get() + " (" +
						 * !comparingSpecification.opposite() + ")" + " for element " +
						 * element.detailedString() + " (selector:" + this + ")");
						 */

						return false;
					} else {
						/*
						 * System.out.println("  <<selector>> Match found for feature " + feature +
						 * " between selector value " + selectorValue + " and element value " +
						 * comparingSpecification.getLiteral().get() + " for element " + element);
						 */
						match = true;
						break;
					}
				} else {
					if (selectorSpecificationVariant.opposite() != comparingSpecification.opposite()) {
						match = true;
						break;
					}
				}
			}
			if (!match) {
				/*
				 * System.out.println( ">><<Endselector>> No match for " +
				 * element.detailedString() + " with selector " + this);
				 */
				return false;
			}
		}

		if (element instanceof IWordTemplate wordTestee) {
			for (String categoire : this.semanticsMap.keySet()) {
				ISemanticSpecificationValue selectorSpec = semanticsMap.get(categoire);
				if (selectorSpec.isVariable() || selectorSpec.isNonSelective()) {
					continue;
				}
				ISemanticSpecificationValue testeeSpec = wordTestee.semantics().get(categoire);
				if (testeeSpec == null || testeeSpec.isSelective()) {
					if (selectorSpec.mustBeSpecified()) {
						/*
						 * System.out.println(">><<Endselector>> Category in element " + element.id() +
						 * " not specified (" + categoire + "), but is expected to be: " +
						 * selectorSpec.toString() + " (for selector: " + this + ")");
						 */
						return false;
					}
					continue;
				}
				if (testeeSpec.isVariable()) {
					/*
					 * System.out.println(">><<Endselector>> Ignoring variable for category " +
					 * categoire + " (" + testeeSpec.getVariable().get() + ") of element " + element
					 * + "; selector: " + selectorSpec.toString());
					 */
					continue;
				}
				Collection<String> testeeValues = testeeSpec.getConstraint(SemanticConstraint.VALUE).get();
				for (String item : selectorSpec.getConstraint(SemanticConstraint.REQUIRED).get()) {
					if (!testeeValues.contains(item)) {
						/*
						 * System.out.println(">><<Endselector>> Failed since element " + element.id() +
						 * " does not contain required item " + item + " for category " + categoire +
						 * " out of specified items " + testeeValues + ": " + this);
						 */
						return false;
					}
				}
				for (String item : selectorSpec.getConstraint(SemanticConstraint.DISALLOWED).get()) {
					if (testeeValues.contains(item)) {
						/*
						 * System.out.println(">><<Endselector>> Failed since element " + element.id() +
						 * " contains forbidden item " + item + " for category " + categoire +
						 * " in specified items " + testeeValues + ": " + this);
						 */
						return false;
					}
				}
				if (selectorSpec.getConstraint(SemanticConstraint.ALLOWED).get().size() > 0) {
					boolean match = false;
					for (String item : selectorSpec.getConstraint(SemanticConstraint.ALLOWED).get()) {
						if (testeeValues.contains(item)) {
							match = true;
							break;
						}
					}
					if (!match) {
						/*
						 * System.out.println(">><<Endselector>> Failed since element " + element.id() +
						 * " lacks at least one of " +
						 * selectorSpec.getConstraint(SemanticConstraint.ALLOWED).get() +
						 * " for category " + categoire + " in element items " + testeeValues + ": " +
						 * this);
						 */
						return false;
					}
				}
			}
		}

		/*
		 * System.out.println(">><<Endselector>> Match success for " + element +
		 * " with environment " + environment + " for selector " + this +
		 * "    ,,,element:" + element.detailedString());
		 */
		return true;
	}

	@Override
	public ITemplateSelector parse(JsonElement fromEl) {
		JsonObject object = fromEl.getAsJsonObject();
		try {
			// removals
			JsonElement rcats = object.get(IConstituentTemplate.PREFIX_REMOVE + IConstituentTemplate.KEY_CATEGORY);
			if (rcats instanceof JsonArray array) {
				array.forEach((x) -> categories.remove(x.getAsString()));
			}
			JsonElement rfeatures = object.get(IConstituentTemplate.PREFIX_REMOVE + IConstituentTemplate.KEY_FEATURES);
			if (rfeatures instanceof JsonArray array) {
				array.forEach((x) -> featuresMap.removeAll(x.getAsString()));
			}
			for (Map<String, ISemanticSpecificationValue> semanticsMap : new Map[] { this.semanticsMap,
					this.aSemanticsMap }) {

				rfeatures = object.get(IConstituentTemplate.PREFIX_REMOVE
						+ (semanticsMap == this.semanticsMap ? IConstituentTemplate.KEY_SEMANTICS
								: IConstituentTemplate.KEY_ADJUNCT_SEMANTICS));

				if (rfeatures instanceof JsonArray array) {
					array.forEach((x) -> semanticsMap.remove(x.getAsString()));
				} else if (rfeatures instanceof JsonObject sema) {
					for (String key : sema.keySet()) {
						for (SemanticConstraint constraint : SemanticConstraint.values()) {
							if (constraint == SemanticConstraint.VALUE)
								continue;
							Set<String> toRemove = new HashSet<>();
							JsonObject complex = sema.get(key).getAsJsonObject();

							if (complex.get(constraint.key) instanceof JsonArray array) {
								array.forEach((x) -> toRemove.add(x.getAsString()));
							} else if (complex.get(constraint.key) instanceof JsonPrimitive prim) {
								if (prim.isBoolean()) {
									toRemove.addAll(semanticsMap.get(key).getConstraint(constraint).get());
								} else {
									toRemove.add(prim.getAsString());
								}
							}
							semanticsMap.put(key, semanticsMap.get(key).withRemoved(constraint, toRemove));
						}
					}
				} else if (rfeatures != null) {
					semanticsMap.remove(rfeatures.getAsString());
				}
			}
		} catch (Exception e) {
			throw new JsonParseException("During removals for selector decoding from " + fromEl + ": " + e.getMessage(),
					e);
		}

		try {
			// additions
			if (object.has(IConstituentTemplate.KEY_IS_HEAD)) {
				this.head = object.get(IConstituentTemplate.KEY_IS_HEAD).getAsBoolean();
			}
			JsonElement cats = object.get(IConstituentTemplate.KEY_CATEGORY);
			if (cats instanceof JsonArray array) {
				array.forEach((el) -> categories.add(el.getAsString()));
			} else if (cats != null) {
				categories.add(cats.getAsString());
			}
			JsonElement featuresa = object.get(IConstituentTemplate.KEY_FEATURES);
			if (featuresa instanceof JsonObject features) {
				for (String feature : features.keySet()) {
					JsonElement specs = features.get(feature);
					featuresMap.put(feature, ISpecificationValue.parse(specs));

				}
			}
			for (String keySemantics : new String[] { IConstituentTemplate.KEY_SEMANTICS,
					IConstituentTemplate.KEY_ADJUNCT_SEMANTICS }) {
				featuresa = object.get(keySemantics);
				if (featuresa instanceof JsonObject semSpecs) {
					for (String categoire : semSpecs.keySet()) {
						JsonElement specs = semSpecs.get(categoire);
						Map<String, ISemanticSpecificationValue> semantics = (keySemantics == IConstituentTemplate.KEY_SEMANTICS
								? this.semanticsMap
								: this.aSemanticsMap);
						ISemanticSpecificationValue ssv = ISemanticSpecificationValue.empty();
						if (semantics.get(categoire) != null) {
							ssv.copy(semantics.get(categoire));
						}
						ssv.parse(specs, true);
						semantics.put(categoire, ssv);
						/*
						 * ISpecificationValue val = ISpecificationValue.parse(specs);
						 * semanticsMap.put(feature, val);
						 */
					}
				}
			}
			return this;
		} catch (Exception e) {
			throw new JsonParseException(
					"During additions for selector decoding from " + fromEl + ": " + e.getMessage(), e);
		}
	}

	@Override
	public TS copy(ITemplateSelector other) {
		try {
			this.categories = new HashSet<>(other.category());
			this.featuresMap = MultimapBuilder.hashKeys().hashSetValues().build(other.features());
			this.semanticsMap = new HashMap<>(other.semantics());
			this.aSemanticsMap = new HashMap<>(other.adjunctSemantics());
			this.head = other.isHead();
		} catch (Exception e) {
			throw new RuntimeException("While copying selector from " + other + ": " + e.getMessage(), e);
		}
		return this;
	}

	@Override
	public ITemplateSelector withAdjunctSemantics(Map<String, ISemanticSpecificationValue> sem) {
		TS copy = new TS().copy(this);
		copy.addAdjunctSemantics(sem);
		return copy;
	}

	@Override
	public ITemplateSelector withHeadSemantics(Map<String, ISemanticSpecificationValue> sem) {
		TS copy = new TS().copy(this);
		copy.addSemantics(sem);
		return copy;
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;
		if (obj instanceof ITemplateSelector ls) {
			return this.categories.equals(ls.category()) && this.featuresMap.equals(ls.features())
					&& this.semanticsMap.equals(ls.semantics());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.categories.hashCode() + this.featuresMap.hashCode() + this.semanticsMap.hashCode();
	}

	@Override
	public String toString() {
		return "@(" + (head ? "head" : "non-head") + "){categories=" + this.categories + ",features=" + this.featuresMap
				+ ",semantics=" + this.semanticsMap + "}";
	}

}
