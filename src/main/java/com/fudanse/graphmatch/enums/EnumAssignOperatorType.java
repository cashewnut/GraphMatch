package com.fudanse.graphmatch.enums;

public enum EnumAssignOperatorType {
	assign("="), // =
	plus("+="), // +=
	minus("-="), // -=
	star("*="), // *=
	slash("/="), // /=
	and("&="), // &=
	or("|="), // |=
	xor("^="), // ^=
	rem("%="), // %=
	lShift("<<="), // <<=
	rSignedShift(">>="), // >>=
	rUnsignedShift(">>>="); // >>>=

	private String value;

	private EnumAssignOperatorType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
