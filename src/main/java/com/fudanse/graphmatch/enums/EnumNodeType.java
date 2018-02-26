package com.fudanse.graphmatch.enums;

public enum EnumNodeType {

	IfStmt("if"), 
	SwitchStmt("switch"),
	MethodCall("methodcall"),
	ForStmt("for"),
	ForInit("forinit"),
	ForUpdate("forupdate"),
	DoStmt("do"),
	WhileStmt("while"),
	ForeachStmt("foreach"),
	Else("else"),
	Break("break");
	

	private String value;

	private EnumNodeType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

}
