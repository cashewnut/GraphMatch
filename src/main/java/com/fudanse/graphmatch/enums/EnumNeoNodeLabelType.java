package com.fudanse.graphmatch.enums;

public enum EnumNeoNodeLabelType {
	
	IFSTMT("IfStmt"), 
	ATOM("Atom"), 
	RETURNSTMT("ReturnStmt"),
	WHILESTMT("WhileStmt"),
	FORSTMT("ForStmt"),
	FORINIT("ForInit"),
	FORUPDATE("ForUpdate"),
	DOWHILESTMT("DoWhileStmt"),
	FOREACHSTMT("ForEachStmt"),
	ASSIGNEXPR("AssighExpr"),
	SWITCHENTRY("SwitchEntry"),
	VARIBLEDECLARATIONEXPR("VaribleDeclarationExpr"),
	VARIBLEDECLARATOR("VariableDeclartor"),
	TYPE("Type"),
	BINARYEXPR("BinaryExpr"),
	METHODCALLEXPR("MethodCallExpr"),
	BLOCKSTMT("BlockStmt"),
	ELSE("Else"),
	SWITCHSTMT("SwitchStmt"),
	PROJECT("Project"),
	PACKAGE("Package"),
	CLASSORINTERFACE("ClassOrInterface"),
	METHODDECLARATION("MethodDeclaration"),
	CONDITION("Condition");

	private String value;

	private EnumNeoNodeLabelType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return value;
	}
	

}
