package com.fudanse.graphmatch.enums;

public enum EnumBinaryOperatorType {
	or("||"), // ||
    and("&&"), // &&
    binOr("|"), // |
    binAnd("&"), // &
    xor("^"), // ^
    equals("=="), // ==
    notEquals("!="), // !=
    less("<"), // <
    greater(">"), // >
    lessEquals("<="), // <=
    greaterEquals(">="), // >=
    lShift("<<"), // <<
    rSignedShift(">>"), // >>
    rUnsignedShift(">>>"), // >>>
    plus("+"), // +
    minus("-"), // -
    times("*"), // *
    divide("/"), // /
    remainder("%"); // %
    
    private String value;
	
    private EnumBinaryOperatorType(String value){
		this.value = value;
	}
    
    public String getValue(){
    	return value;
    }

}
