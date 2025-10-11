package com.gm910.sotdivine.language.feature;

import java.util.Optional;

class SV implements ISpecificationValue {

	private String name;
	private SpecificationType type;
	private boolean opposite;

	public SV(String name, SpecificationType type, boolean opposite) {
		this.name = name;
		this.type = type;
		this.opposite = opposite;
	}

	@Override
	public SpecificationType type() {
		return type;
	}

	@Override
	public boolean opposite() {
		return opposite;
	}

	public Optional<Boolean> getBooleanValue() {
		return name.equals("true") ? Optional.of(true) : (name.equals("false") ? Optional.of(false) : Optional.empty());
	}

	@Override
	public ISpecificationValue specifyVariable(String val) {
		if (this.isVariable()) {
			return ISpecificationValue.literal(val);
		}
		throw new UnsupportedOperationException();
	}

	@Override
	public Optional<String> getLiteral() {
		if (type == SpecificationType.LITERAL) {
			return Optional.of(name);
		}
		return Optional.empty();
	}

	@Override
	public boolean isBoolean() {
		return this.name.equals("true") || this.name.equals("false");
	}

	@Override
	public Optional<String> getVariable() {
		if (type == SpecificationType.VARIABLE) {
			return Optional.of(name);
		}
		return Optional.empty();
	}

	@Override
	public String getString() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		if (super.equals(obj)) {
			return true;
		}
		if (obj instanceof ISpecificationValue val) {
			return this.type().equals(val.type()) && this.opposite == val.opposite()
					&& this.getLiteral().equals(val.getLiteral()) && this.getVariable().equals(val.getVariable());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (this.name == null ? 0 : name.hashCode()) + this.type.hashCode() + Boolean.hashCode(opposite);
	}

	@Override
	public String toString() {
		switch (type) {
		case LITERAL:
			if (this.opposite) {
				return "!(\"" + this.name + "\")";
			} else {
				return "(\"" + this.name + "\")";
			}
		case VARIABLE:
			if (this.opposite) {
				return "!<" + this.name + ">";
			} else {
				return "<" + this.name + ">";
			}

		}
		return super.toString();
	}

}
