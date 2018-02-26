package com.fudanse.graphmatch.enums;

public enum EnumNeoNodeRelation {

	PARENT("Parent"), 
	CDEPENDENCY("CDependency"), 
	DDEPENDENCY("DDependency"),
	TRUE("True"),
	FALSE("False"),
	EQUALS("Equals"),
	IN("In"),
	ORDER("Order"),
	DEFAULT("Default");
	

	private String value;

	private EnumNeoNodeRelation(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
