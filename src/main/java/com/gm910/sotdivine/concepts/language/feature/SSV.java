package com.gm910.sotdivine.concepts.language.feature;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;

class SSV implements ISemanticSpecificationValue {

	Optional<String> variable = Optional.empty();
	boolean specified;
	Optional<Multimap<SemanticConstraint, String>> conditions = Optional
			.of(MultimapBuilder.enumKeys(SemanticConstraint.class).hashSetValues().build());

	static final SSV minSpec = ((Supplier<SSV>) () -> {
		SSV ssv = new SSV();
		ssv.specified = true;
		return ssv;
	}).get();

	public SSV() {
	}

	@Override
	public boolean mustBeSpecified() {
		return specified;
	}

	@Override
	public Optional<Collection<String>> getConstraint(SemanticConstraint constraint) {
		return conditions.map((map) -> map.get(constraint)).map(Collections::unmodifiableCollection);
	}

	@Override
	public Optional<String> getVariable() {
		return this.variable;
	}

	@Override
	public String toString() {
		if (this.variable.isEmpty()) {
			if (this.isNonSelective()) {
				return "" + this.conditions.get().get(SemanticConstraint.VALUE);
			}
			return "{" + (specified ? "specified=true," : "")
					+ (this.conditions.get().isEmpty() ? "" : "conditions=" + this.conditions.get()) + "}";
		}
		return "<" + this.variable.get() + ">";
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj))
			return true;
		if (obj instanceof ISemanticSpecificationValue issv) {
			return this.specified == issv.mustBeSpecified() && this.variable.equals(issv.getVariable())
					&& Arrays.stream(SemanticConstraint.values())
							.allMatch((con) -> this.getConstraint(con).equals(issv.getConstraint(con)));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.variable.hashCode() + this.conditions.hashCode();
	}

	@Override
	public SSV copy(ISemanticSpecificationValue other) {
		this.specified = other.mustBeSpecified();
		this.variable = other.getVariable();
		if (other.isSpecification()) {
			this.conditions = Optional.of(MultimapBuilder.enumKeys(SemanticConstraint.class).hashSetValues().build());
			for (SemanticConstraint constraint : SemanticConstraint.values())
				this.conditions.get().putAll(constraint, other.getConstraint(constraint).get());
		}
		return this;
	}

	@Override
	public ISemanticSpecificationValue withAdded(SemanticConstraint constraint, Iterable<String> values) {
		SSV newa = new SSV().copy(this);
		newa.conditions.orElseThrow(IllegalStateException::new).get(constraint).addAll(Sets.newHashSet(values));
		return newa;
	}

	@Override
	public ISemanticSpecificationValue withAdded(ISemanticSpecificationValue value) {
		SSV newa = new SSV().copy(this);
		for (SemanticConstraint constraint : SemanticConstraint.values()) {
			newa.conditions.orElseThrow(IllegalStateException::new).get(constraint)
					.addAll(value.getConstraint(constraint).orElseThrow(IllegalStateException::new));
		}
		return newa;
	}

	@Override
	public ISemanticSpecificationValue withRemoved(SemanticConstraint constraint, Iterable<String> values) {
		SSV newa = new SSV().copy(this);
		newa.conditions.orElseThrow(IllegalStateException::new).get(constraint).removeAll(Sets.newHashSet(values));
		return newa;
	}

	@Override
	public ISemanticSpecificationValue parse(JsonElement element, boolean asSelector) {
		if (!asSelector) {
			if (element instanceof JsonArray array) {
				this.conditions.get().putAll(SemanticConstraint.VALUE,
						() -> array.asList().stream().map(JsonElement::getAsString).iterator());
			} else {
				this.conditions.get().put(SemanticConstraint.VALUE, element.getAsString());
			}
		} else {
			if (element instanceof JsonPrimitive primitive) {
				try {
					if (primitive.isBoolean()) {
						// if it is boolean, then this is a minimum specification
						this.specified = primitive.getAsBoolean();
					} else {
						// put in the direct string as allowed, if a selector, and as value, if not
						// selector
						this.conditions.get().put(SemanticConstraint.ALLOWED, primitive.getAsString());
					}
				} catch (Exception e) {
					throw new JsonParseException("While parsing a boolean/string from " + element, e);
				}

			} else if (element instanceof JsonObject object) {
				try {
					if (object.has(KEY_VARIABLE)) { // if variable, nothing else matters
						this.conditions = Optional.empty();
						this.variable = Optional.of(object.get(KEY_VARIABLE).getAsString());
					} else {
						this.specified = Optional.ofNullable(object.get(KEY_SPECIFIED)).map(JsonElement::getAsBoolean)
								.orElse(false);
						for (SemanticConstraint constraint : SemanticConstraint.values()) {
							if (constraint == SemanticConstraint.VALUE)
								continue;
							if (object.has(constraint.key)) {
								JsonElement constraintEl = object.get(constraint.key);
								if (constraintEl instanceof JsonArray array) {
									array.forEach((cons) -> this.conditions.get().put(constraint, cons.getAsString()));
								} else {
									this.conditions.get().put(constraint, constraintEl.getAsString());
								}
							}
						}
					}
				} catch (Exception e) {
					throw new JsonParseException("While parsing a compound from " + element, e);
				}
			} else {
				throw new JsonParseException("Cannot parse " + element);
			}
		}
		return this;
	}

}
