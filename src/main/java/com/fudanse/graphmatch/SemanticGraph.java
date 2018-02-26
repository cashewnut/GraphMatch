package com.fudanse.graphmatch;

import java.util.ArrayList;
import java.util.List;

import com.fudanse.graphmatch.enums.EnumNodeType;
import com.fudanse.graphmatch.model.Vertex;
import com.fudanse.graphmatch.util.ConvertEnumUtil;

import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.expr.AssignExpr;
import japa.parser.ast.expr.BinaryExpr;
import japa.parser.ast.expr.EnclosedExpr;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.UnaryExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.BreakStmt;
import japa.parser.ast.stmt.DoStmt;
import japa.parser.ast.stmt.ExpressionStmt;
import japa.parser.ast.stmt.ForStmt;
import japa.parser.ast.stmt.ForeachStmt;
import japa.parser.ast.stmt.IfStmt;
import japa.parser.ast.stmt.ReturnStmt;
import japa.parser.ast.stmt.Statement;
import japa.parser.ast.stmt.SwitchEntryStmt;
import japa.parser.ast.stmt.SwitchStmt;
import japa.parser.ast.stmt.WhileStmt;

public class SemanticGraph {

	private CompilationUnit cu;
	private List<Vertex> roots;
	private int id;

	public SemanticGraph() {
		roots = new ArrayList<Vertex>();
		id = 0;
	}

	public SemanticGraph(CompilationUnit cu) {
		roots = new ArrayList<Vertex>();
		this.cu = cu;
		id = 0;
	}

	public void analyzeMethod(String methodName) {
		List<Statement> stmts = getMethodBodyByName(methodName).getBody().getStmts();
		 //Vertex root = createGraph(stmts.get(1));
		for (Statement stmt : stmts) {
			System.out.println("-----------------------------------------");
			Vertex root = createGraph(stmt);
			roots.add(root);
			System.out.println("content(id):" + root.getContent() + "(" + root.getId() + ")");
			show(root);
		}

	}

	/**
	 * 生成图
	 * 
	 * @param node
	 * @return root结点
	 */
	private Vertex createGraph(Node node) {
		Vertex vertex = null;
		if (node == null)
			return null;

		if (node instanceof IfStmt) { // if语句
			vertex = new Vertex(id++);
			vertex.setContent(EnumNodeType.IfStmt.getValue());
			Node conditionNode = ((IfStmt) node).getCondition();
			Vertex conditionVertex = createGraph(conditionNode);
			vertex.addChilds(conditionVertex);
			Node thenNode = ((IfStmt) node).getThenStmt();
			pcBlockStmt(vertex, thenNode); // 处理{}语句块
			Node elseNode = ((IfStmt) node).getElseStmt();
			Vertex elseVertex = null;
			if (elseNode instanceof IfStmt) {
				elseVertex = createGraph(elseNode);
			} else {
				elseVertex = new Vertex(id++);
				elseVertex.setContent(EnumNodeType.Else.getValue());
				if (elseNode instanceof BlockStmt) {
					pcBlockStmt(elseVertex, elseNode);
				} else {
					Vertex elseStmt = createGraph(elseNode);
					elseVertex.addChilds(elseStmt);
				}
			}
			vertex.addChilds(elseVertex);
		} else if (node instanceof BinaryExpr) { // 二元表达式
			vertex = new Vertex(id++);
			vertex.setContent(ConvertEnumUtil.getBinaryOperator(((BinaryExpr) node).getOperator()));
			Node leftNode = ((BinaryExpr) node).getLeft();
			Vertex leftVertex = createGraph(leftNode);
			vertex.addChilds(leftVertex);
			Node rightNode = ((BinaryExpr) node).getRight();
			Vertex rightVertex = createGraph(rightNode);
			vertex.addChilds(rightVertex);
		} else if (node instanceof AssignExpr) { // 赋值语句
			vertex = new Vertex(id++);
			vertex.setContent(ConvertEnumUtil.getAssignOperator(((AssignExpr) node).getOperator()));
			Node targetNode = ((AssignExpr) node).getTarget();
			Vertex targetVertex = createGraph(targetNode);
			vertex.addChilds(targetVertex);
			Node valueNode = ((AssignExpr) node).getValue();
			Vertex valueVertex = createGraph(valueNode);
			vertex.addChilds(valueVertex);
		} else if (node instanceof IntegerLiteralExpr) { // int型
			vertex = new Vertex(id++);
			vertex.setContent(((IntegerLiteralExpr) node).getValue());
		} else if (node instanceof NameExpr) { // 变量名
			vertex = new Vertex(id++);
			vertex.setContent(((NameExpr) node).getName());
		} else if (node instanceof UnaryExpr) { // Unary 负数或者类似于i++这种形式
			vertex = new Vertex(id++);
			vertex.setContent(node.toString());
		} else if (node instanceof ExpressionStmt) { // 表达式
			Node exprNode = ((ExpressionStmt) node).getExpression();
			vertex = createGraph(exprNode);
		} else if (node instanceof SwitchStmt) { // switch语句
			vertex = new Vertex(id++);
			vertex.setContent(EnumNodeType.SwitchStmt.getValue());
			Node selectorNode = ((SwitchStmt) node).getSelector();
			Vertex selectorVertex = createGraph(selectorNode);
			vertex.addChilds(selectorVertex);
			List<SwitchEntryStmt> seStmts = ((SwitchStmt) node).getEntries();
			for (SwitchEntryStmt seStmt : seStmts) {
				Vertex seStmtVertex = createGraph(seStmt);
				vertex.addChilds(seStmtVertex);
			}
			// System.out.println(seStmt);
		} else if (node instanceof MethodCallExpr) { // 方法调用
			vertex = new Vertex(id++);
			vertex.setContent(node.toString()); // 方法，不再往下细分
		} else if (node instanceof SwitchEntryStmt) { // switch语句的case
			vertex = new Vertex(id++);
			vertex.setContent(((SwitchEntryStmt) node).getLabel().toString());// case不再往下细分
			List<Statement> stmts = ((SwitchEntryStmt) node).getStmts();
			for (Statement stmt : stmts) {
				Vertex stmtVertex = createGraph(stmt);
				vertex.addChilds(stmtVertex);
			}
		} else if (node instanceof BreakStmt) { // break语句
			vertex = new Vertex(id++);
			vertex.setContent(EnumNodeType.Break.getValue());
		} else if (node instanceof ReturnStmt) { // Return语句，不往下细分？？？
			vertex = new Vertex(id++);
			String nodeString = node.toString();
			vertex.setContent(nodeString.substring(0, nodeString.length() - 1)); // 去分号
		} else if (node instanceof ForStmt) { // for语句
			ForStmt forStmt = (ForStmt) node;
			vertex = new Vertex(id++);
			vertex.setContent(EnumNodeType.ForStmt.getValue());
			Vertex initVertex = new Vertex(id++); // init
			initVertex.setContent(EnumNodeType.ForInit.getValue());
			for (Node initStmt : forStmt.getInit()) {
				Vertex initV = createGraph(initStmt);
				initVertex.addChilds(initV);
			}
			vertex.addChilds(initVertex);

			Vertex cmpVertex = createGraph(forStmt.getCompare()); // compare
			vertex.addChilds(cmpVertex);

			Vertex updateVertex = new Vertex(id++); // update
			updateVertex.setContent(EnumNodeType.ForUpdate.getValue());
			for (Node updateStmt : forStmt.getUpdate()) {
				Vertex updateV = createGraph(updateStmt);
				updateVertex.addChilds(updateV);
			}
			vertex.addChilds(updateVertex);

			pcBlockStmt(vertex, forStmt.getBody()); // 处理blockStmt
			// Node initNode = ((ForStmt) node).getInit();
		} else if (node instanceof DoStmt) { // do-while语句
			DoStmt doStmt = (DoStmt) node;
			vertex = new Vertex(id++);
			vertex.setContent(EnumNodeType.DoStmt.getValue());
			Vertex conditionVertex = createGraph(doStmt.getCondition());
			vertex.addChilds(conditionVertex);

			pcBlockStmt(vertex, doStmt.getBody());
		} else if (node instanceof WhileStmt) { // while语句
			WhileStmt whileStmt = (WhileStmt) node;
			vertex = new Vertex(id++);
			vertex.setContent(EnumNodeType.WhileStmt.getValue());
			Vertex conditionVertex = createGraph(whileStmt.getCondition());
			vertex.addChilds(conditionVertex);

			pcBlockStmt(vertex, whileStmt.getBody());
		} else if (node instanceof ForeachStmt) { // foreach语句
			ForeachStmt foreachStmt = (ForeachStmt) node;
			vertex = new Vertex(id++);
			vertex.setContent(EnumNodeType.ForeachStmt.getValue());
			Vertex VDExpr = createGraph(foreachStmt.getVariable());
			vertex.addChilds(VDExpr);

			Vertex iterableVertex = createGraph(foreachStmt.getIterable());
			vertex.addChilds(iterableVertex);

			pcBlockStmt(vertex, foreachStmt.getBody());
		} else if (node instanceof VariableDeclarator) { // 初始化语句 int x = 14
			VariableDeclarator vd = (VariableDeclarator) node;
			vertex = new Vertex(id++);
			if (vd.getInit() == null) {
				vertex.setContent(vd.getId().getName());
			} else {
				vertex.setContent("=");
				Vertex left = new Vertex(id++);
				left.setContent(vd.getId().getName());
				Vertex right = new Vertex(id++);
				right.setContent(vd.getInit().toString());
				vertex.addChilds(left);
				vertex.addChilds(right);
			}
		} else if (node instanceof VariableDeclarationExpr) { // 变量声明语句 类似于int i,j=0
			VariableDeclarationExpr vdExpr = (VariableDeclarationExpr) node;
			vertex = new Vertex(id++);
			vertex.setContent(vdExpr.getType().toString());
			for (VariableDeclarator vd : vdExpr.getVars()) {
				Vertex vdVertex = createGraph(vd);
				vertex.addChilds(vdVertex);
			}
		} else if (node instanceof EnclosedExpr) { // 括号()
			EnclosedExpr enclosedExpr = (EnclosedExpr) node;
			vertex = createGraph(enclosedExpr.getInner());
		} else {
			vertex = new Vertex(id++);
			vertex.setContent(node.toString());
		}
		return vertex;
	}

	/**
	 * 处理if/for等语句的语句块，blockstmt或者expression
	 * 
	 * @param id
	 * @param vertex
	 * @param node
	 */
	private void pcBlockStmt(Vertex vertex, Node node) {
		if (node instanceof BlockStmt) { // 如果是带括号的，则把这些语句并列起来，父节点就是if
			List<Statement> stmts = ((BlockStmt) node).getStmts();
			for (Statement stmt : stmts) {
				Vertex stmtVertex = createGraph(stmt);
				vertex.addChilds(stmtVertex);
			}
		} else {
			Vertex thenVertex = createGraph(node);
			vertex.addChilds(thenVertex);
		}
	}

	/**
	 * 打印
	 * 
	 * @param root
	 */
	private void show(Vertex root) {
		if (root == null)
			return;
		for (Vertex v : root.getChilds()) {
			System.out.println("parent:" + root.getContent() + "(" + root.getId() + ")" + "    content:"
					+ v.getContent() + "(" + v.getId() + ")");
			show(v);
		}
	}

	/**
	 * 根据CompilationUnit得到类成员
	 */
	public List<BodyDeclaration> getBodyList() {
		if (cu.getTypes() == null)
			return null;
		for (TypeDeclaration type : cu.getTypes()) {
			if (type instanceof ClassOrInterfaceDeclaration) {
				List<BodyDeclaration> bodyList = type.getMembers();
				return bodyList;
			}
		}
		return null;
	}

	/**
	 * 根据方法名得到body
	 */
	public MethodDeclaration getMethodBodyByName(String methodName) {
		List<BodyDeclaration> bodyList = getBodyList();
		for (BodyDeclaration body : bodyList) {
			if (body instanceof MethodDeclaration && ((MethodDeclaration) body).getName().equals(methodName))
				return (MethodDeclaration) body;
		}
		return null;
	}

	public CompilationUnit getCu() {
		return cu;
	}

	public void setCu(CompilationUnit cu) {
		this.cu = cu;
	}

}
