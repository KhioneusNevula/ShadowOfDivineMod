package com.gm910.sotdivine.concepts.language.element;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.stream.Streams;

import com.gm910.sotdivine.concepts.language.feature.ISemanticSpecificationValue;
import com.gm910.sotdivine.concepts.language.feature.ISemanticSpecificationValue.SemanticConstraint;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class WLE extends LEAbstract<IWordTemplate> implements IWordTemplate {

	private Map<String, ISemanticSpecificationValue> semantics;
	private Optional<IWordTemplate> root = Optional.empty();

	WLE() {
		super(IWordTemplate.class);
		this.semantics = new HashMap<>();
	}

	@Override
	public Map<String, ISemanticSpecificationValue> semantics() {
		return Collections.unmodifiableMap(semantics);
	}

	@Override
	public Optional<IWordTemplate> root() {
		return root;
	}

	@Override
	protected void copyAdditional(IWordTemplate other) {
		this.semantics = new HashMap<>(other.semantics());
		this.root = Optional.of(other);
	}

	@Override
	protected void parseRemovals(JsonObject object) {
		if (object.get(PREFIX_REMOVE + KEY_SEMANTICS) instanceof JsonElement el) {
			if (el instanceof JsonArray array) {
				array.forEach((x) -> this.semantics.remove(x.getAsString()));
			} else if (el instanceof JsonObject sema) {
				for (String key : sema.keySet()) {
					Set<String> toRemove = new HashSet<>();
					if (sema.get(key) instanceof JsonArray array) {
						array.forEach((x) -> toRemove.add(x.getAsString()));
					} else if (sema.get(key) instanceof JsonPrimitive prim) {
						if (prim.isBoolean()) {
							toRemove.addAll(this.semantics.get(key).getConstraint(SemanticConstraint.VALUE).get());
						} else {
							toRemove.add(prim.getAsString());
						}
					}
					this.semantics.put(key, this.semantics.get(key).withRemoved(SemanticConstraint.VALUE, toRemove));
				}
			} else {
				this.semantics.remove(el.getAsString());
			}
		}
	}

	@Override
	protected void parseAdditional(JsonObject object) {
		if (object.get(KEY_SEMANTICS) instanceof JsonElement el) {
			if (_isCopy && !_changedID) {
				this.id += ".sem";
			}
			if (el instanceof JsonObject semants) {
				for (String categoire : semants.keySet()) {
					JsonElement spec = semants.get(categoire);
					ISemanticSpecificationValue parsedSpec = ISemanticSpecificationValue.empty().parse(spec, false);
					this.semantics.put(categoire, parsedSpec);
					if (_isCopy && !_changedID) {
						Collection<String> array = parsedSpec.getConstraint(SemanticConstraint.VALUE).get();
						this.id += "." + categoire + "." + array.size() + "." + String.join(".", array);

					}
				}
			}
		}
	}

	@Override
	public Collection<IConstituentTemplate> genDerivations(JsonElement el, ElementType type) {
		if (el instanceof JsonObject object) {
			if (object.get(KEY_DERIVATIONS) instanceof JsonArray array) {
				return Streams.of(array).map((ac) -> {
					// System.out.println("[Word-loading] Creating derivation from word " + this.id
					// + " using json " + ac);
					return IConstituentTemplate.create(type).copy(this).parse(ac);
				}).collect(Collectors.toSet());
			}
		}
		return Collections.emptySet();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof IWordTemplate word) {
			return super.equals(obj) && this.semantics.equals(word.semantics()) && this.root.equals(word.root());
		}
		return false;
	}

	@Override
	public String detailedString() {
		return "{id=" + this.id + ",form=\"" + this.form + "\"" + root.map((x) -> ",root=" + x.id()).orElse("")
				+ ",category=" + this.category + ",features=" + this.features + ",semantics=" + this.semantics + "}";
	}

}
