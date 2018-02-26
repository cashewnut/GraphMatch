package com.fudanse.graphmatch.enums;

public enum EnumUnaryOperatorType {
	positive("+"), // +
	negative("-"), //-
	preIncrement("pre++"), // ++
	preDecrement("pre--"), // --
	not("!"), // !
	inverse("~"), // ~
	posIncrement("pos++"), // ++
	posDecrement("pos--"); // --
	
	private String value;

	private EnumUnaryOperatorType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
