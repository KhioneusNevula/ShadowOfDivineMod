package com.gm910.sotdivine.language.element;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.gm910.sotdivine.language.feature.ISpecificationValue;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * A language provider
 */
abstract class LEAbstract<T extends IConstituentTemplate> implements IConstituentTemplate {

	protected String id = "";
	protected String form = "";
	protected String category = "";
	protected Map<String, ISpecificationValue> features = new HashMap<>();
	private Class<T> clazz;
	protected boolean _isCopy;
	protected boolean _changedID;

	protected LEAbstract(Class<T> clazz) {
		this.clazz = clazz;
	}

	@Override
	public final IConstituentTemplate copy(IConstituentTemplate other) {
		try {
			this._isCopy = true;
			this.category = other.category();
			this.form = other.form();
			this.id = other.id();
			this.features = new HashMap<>(other.features());
			if (clazz.isInstance(other)) {
				copyAdditional(clazz.cast(other));
			}
		} catch (Exception e) {
			throw new RuntimeException("While copying provider from " + other + ": " + e.getMessage(), e);
		}
		return this;
	}

	protected abstract void copyAdditional(T other);

	@Override
	public final IConstituentTemplate parse(JsonElement element) {
		JsonObject object = element.getAsJsonObject();
		// an id to use in messages
		String initialID = Optional.ofNullable(object.get(KEY_ID))
				.or(() -> Optional.ofNullable(object.get(KEY_ID + "-" + KEY_FORM))).map(JsonElement::getAsString)
				.orElse(this.id) + "";
		try {
			// removals
			// System.out.println("[Loading " + initialID + "] Running removals");
			if (object.get(PREFIX_REMOVE + KEY_FEATURES) instanceof JsonElement el) {
				// System.out.println("[Loading " + initialID + "] Removing features ");
				if (el instanceof JsonArray array) {
					array.forEach((ela) -> features.remove(ela.getAsString()));
				} else {
					features.remove(el.getAsString());
				}
			}

			// System.out.println("[Loading " + initialID + "] Additional removals for " +
			// this.elementType());
			parseRemovals(object);
		} catch (Exception e) {
			throw new JsonParseException("During removals: " + e.getMessage(), e);
		}

		try {
			// System.out.println("[Loading " + initialID + "] Running additions");
			// additions
			if (object.get(KEY_ID + "-" + KEY_FORM) instanceof JsonElement id) {
				this.id = id.getAsString();
				this.form = id.getAsString();
				_changedID = true;
			} else {
				if (object.get(KEY_ID) instanceof JsonElement id) {
					this.id = id.getAsString();
					_changedID = true;
				}
				if (object.get(KEY_FORM) instanceof JsonElement forel) {
					this.form = forel.getAsString();
				}
			}
			if (object.get(KEY_CATEGORY) instanceof JsonElement kcat) {
				this.category = kcat.getAsString();
				if (_isCopy && !_changedID) {
					id += ".cat." + kcat.getAsString();
				}
			}

			if (object.get(KEY_FEATURES) instanceof JsonElement elem) {
				if (_isCopy && !_changedID) {
					id += ".feat";
				}
				// System.out.println("[Loading " + initialID + "] Adding features");
				if (elem instanceof JsonObject features) {
					for (String key : features.keySet()) {
						ISpecificationValue val = ISpecificationValue.parse(features.get(key));

						this.features.put(key, val);
						if (this._isCopy && !_changedID) {
							this.id += "." + key + "." + (val.type().toString().toLowerCase().substring(0, 3)) + "."
									+ val.getString();
						}
					}

				} else if (elem instanceof JsonArray) {
					// for phrase class to deal with
				} else {
					throw new JsonParseException(elem + " is wrong type: " + elem.getClass());
				}
			}

			// System.out.println("[Loading " + initialID + "] Additional additions for " +
			// this.elementType());
			parseAdditional(object);
			return this;
		} catch (Exception e) {
			throw new JsonParseException("During additions: " + e.getMessage(), e);

		}
	}

	/**
	 * For child classes to parse specific removals
	 * 
	 * @param provider
	 */
	protected abstract void parseRemovals(JsonObject object);

	/**
	 * For child classes to parse additional things
	 * 
	 * @param provider
	 */
	protected abstract void parseAdditional(JsonObject object);

	@Override
	public String id() {
		return id;
	}

	@Override
	public String form() {
		return form;
	}

	@Override
	public String category() {
		return category;
	}

	@Override
	public Map<String, ISpecificationValue> features() {
		return features;
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			return true;
		}
		if (obj instanceof IConstituentTemplate ile) {
			return this.category.equals(ile.category()) && this.form().equals(ile.form()) && this.id.equals(ile.id())
					&& this.features().equals(ile.features());
		}

		return false;
	}

	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public String toString() {
		return "Template" + this.elementType() + "(" + this.category + ")" + "{form=\"" + this.form + "\",id=" + this.id
				+ "}";
	}

}
