package com.gm910.sotdivine.language.element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.gm910.sotdivine.language.feature.ISpecificationValue;
import com.gm910.sotdivine.language.selector.ITemplateSelector;
import com.google.common.collect.ImmutableMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

class PLE extends LEAbstract<IPhraseTemplate> implements IPhraseTemplate {

	private Map<Integer, List<ITemplateSelector>> selectors;
	private Collection<String> variables;

	PLE() {
		super(IPhraseTemplate.class);
		selectors = new TreeMap<>();
		this.variables = new HashSet<>();
	}

	@Override
	public void copyAdditional(IPhraseTemplate phrase) {
		this.variables.addAll(phrase.variables());
		for (int i = 0; i < phrase.selectorCount(); i++) {
			this.selectors.put(i, new ArrayList<>(phrase.getSelectors(i)));
		}

	}

	@Override
	protected void parseAdditional(JsonObject object) {
		if (object.get(KEY_VARIABLES) instanceof JsonArray array) {
			// System.out.println("[Loading " + id + "] Parsing variables " + array);
			array.asList().stream().forEach((el) -> this.variables.add(el.getAsString()));
		}
		if (object.get(KEY_SELECTORS) instanceof JsonObject sel) {
			// System.out.println("[Loading " + id + "] Parsing selectors " + sel);
			for (String pairOrNot : sel.keySet()) {

				String[] vals = pairOrNot.split(".");
				if (vals.length == 0)
					vals = new String[] { pairOrNot };

				// System.out.println("[Loading " + id + "] Parsing single selector " +
				// Arrays.toString(vals));
				int index = Integer.parseInt(vals[0]);

				final List<ITemplateSelector> interiorList;
				if (!selectors.containsKey(index)) { // make a list
					selectors.put(index, interiorList = new ArrayList<>());
				} else {
					interiorList = selectors.get(index);
				}

				if (sel.get(pairOrNot) instanceof JsonArray array) {
					array.forEach((x) -> interiorList.add(ITemplateSelector.createNew().parse(x)));
				} else {
					JsonElement parsing = sel.get(pairOrNot);
					Integer index2 = vals.length > 1 ? Integer.parseInt(vals[2]) : null;
					if (index2 != null) {
						interiorList.set(index2,
								ITemplateSelector.createNew().copy(interiorList.get(index2)).parse(parsing));
					} else {
						if (interiorList.size() == 1) {
							interiorList.set(0, ITemplateSelector.createNew().copy(interiorList.get(0)).parse(parsing));
						} else {
							interiorList.add(ITemplateSelector.createNew().parse(parsing));
						}
					}
				}
			}
		}
		selectors.values().forEach((ls) -> ls.removeIf((x) -> x == null));
		if (object.get(KEY_FEATURES) instanceof JsonElement elem) {
			if (elem instanceof JsonObject features) {
			} else if (elem instanceof JsonArray array) {
				int from = object.get(KEY_FEATURES_COPY).getAsInt();
				// System.out.println(
				// "[Loading " + id + "] Parsing features as being variably-derived from
				// selector " + from);
				List<ITemplateSelector> selectorVariations = selectors.get(from);
				for (JsonElement element : array) {
					String feature = element.getAsString();
					// System.out.println("[Loading " + id + "] Checking copiable feature " +
					// feature);
					String varName = null;
					for (int i = 0; i < selectorVariations.size(); i++) { // look for existing variable name
						if (!selectorVariations.get(i).features().get(feature).isEmpty()) {
							for (ISpecificationValue vara : selectorVariations.get(i).features().get(feature)) {
								if (vara.getVariable().isPresent()) {
									// System.out.println("[Loading " + id
									// + "] Found suitable variable name from selector-variation " + i);
									varName = vara.getVariable().get();
									break;
								}
							}
						}
					}
					if (varName == null) { // create new variable name
						varName = feature;
						// System.out.println("[Loading " + id + "] Generating new variable name for " +
						// feature);
						while (this.variables.contains(varName)) {
							varName += "_";
						}
						// System.out.println("[Loading " + id + "] Generated name " + varName);
					}
					ISpecificationValue varVal = ISpecificationValue.var(varName);
					for (int i = 0; i < selectorVariations.size(); i++) { // categorically apply variable name
						ITemplateSelector selector = selectorVariations.get(i);
						// System.out.println("[Loading " + id + "] selector " + from + ", variation " +
						// i
						// + ", to have feature " + feature + " set to variable " + varName);
						selectorVariations.set(i, selector.withFeatures(ImmutableMultimap.of(feature, varVal)));
					}
					this.features.put(feature, varVal);
					this.variables.add(varName);
				}
			} else {
				throw new JsonParseException(elem + " is wrong type: " + elem.getClass());
			}
		}
	}

	@Override
	protected void parseRemovals(JsonObject object) {
		if (object.get(PREFIX_REMOVE + KEY_SELECTORS) instanceof JsonObject selectra) {
			for (String sindex : selectra.keySet()) {
				int index = Integer.parseInt(sindex);
				Set<Integer> toRemove = new TreeSet<>();
				if (selectra.get(sindex) instanceof JsonArray array) {
					array.forEach((x) -> toRemove.add(x.getAsInt()));
				} else {
					toRemove.add(selectra.get(sindex).getAsInt());
				}
				toRemove.forEach((idx) -> selectors.get(index).set(idx, null));
			}
		}
	}

	@Override
	public Collection<IConstituentTemplate> genDerivations(JsonElement el, ElementType type) {
		return Collections.emptySet();
	}

	@Override
	public int selectorCount() {
		return selectors.size();
	}

	@Override
	public Collection<String> variables() {
		return Collections.unmodifiableCollection(variables);
	}

	@Override
	public Collection<ITemplateSelector> getSelectors(int index) {
		return Collections.unmodifiableCollection(selectors.get(index));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj instanceof IPhraseTemplate phrase) {
			return super.equals(obj) && this.variables.equals(phrase.variables())
					&& this.selectors.entrySet().stream().allMatch((x) -> Optional.ofNullable(x.getValue())
							.equals(Optional.ofNullable(phrase.getSelectors(x.getKey()))));

		}
		return false;
	}

	@Override
	public String detailedString() {
		return "{id=" + this.id + ",form=" + this.form + ",category=" + this.category + ",variables=" + this.variables
				+ ",features=" + this.features.toString() + ",selectors=" + this.selectors + "}";
	}

}
