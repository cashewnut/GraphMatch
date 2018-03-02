package com.fudanse.graphmatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fudanse.graphmatch.enums.EnumNeoNodeLabelType;
import com.fudanse.graphmatch.model.NeoNode;
import com.fudanse.graphmatch.service.INeoNodeService;
import com.fudanse.graphmatch.service.NeoNodeService;
import com.fudanse.graphmatch.util.ConvertEnumUtil;
import com.fudanse.graphmatch.util.CypherStatment;
import com.fudanse.graphmatch.util.FileUtil;

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
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.StringLiteralExpr;
import japa.parser.ast.expr.UnaryExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.stmt.BlockStmt;
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

/**
 * 生成图
 * 
 * @author xiyaoguo
 *
 */
public class GenerateGraph {

	private INeoNodeService service;
	private CompilationUnit cu;
	private Map<Node, NeoNode> nodeNeoNodePair = new HashMap<>(); // node和NeoNode的keyvaluepair
	private Map<String, NeoNode> varNodePair = new HashMap<>(); // 变量和它的跟节点

	public GenerateGraph() {
		this.service = new NeoNodeService();
	}

	public GenerateGraph(CompilationUnit cu) {
		this.service = new NeoNodeService();
		this.cu = cu;
	}

	public void analyzeMethod(String methodName) {
		List<Statement> stmts = getMethodBodyByName(methodName).getBody().getStmts();
		create(stmts.get(4));
		NeoNode preNode = null;

		for (Statement stmt : stmts) {
			System.out.println("-----------------------------------------");
			NeoNode node = create(stmt);
			if (preNode != null)
				service.saveEdge(preNode.getId(), node.getId(), CypherStatment.ORDER);
			preNode = node;
		}

	}

	/**
	 * 创建neo4j的图
	 * 
	 * @param node
	 * @return neo4j对应的节点
	 */
	public NeoNode create(Node node) {
		NeoNode nn = null;
		if (node == null)
			return null;
		if (node instanceof EnclosedExpr) { // 括号()
			EnclosedExpr enclosedExpr = (EnclosedExpr) node;
			nn = create(enclosedExpr.getInner());
		} else if (node instanceof VariableDeclarationExpr) { // 变量声明语句 类似于int i,j=0
			VariableDeclarationExpr vdexpr = (VariableDeclarationExpr) node;

			String sen = convertType(vdexpr, new HashMap<>());

			nn = new NeoNode(EnumNeoNodeLabelType.VARIBLEDECLARATIONEXPR.getValue(),
					EnumNeoNodeLabelType.VARIBLEDECLARATIONEXPR.getValue());
			nn = service.saveNode(nn);
			nodeNeoNodePair.put(node, nn);
			NeoNode type = new NeoNode(EnumNeoNodeLabelType.TYPE.getValue(), vdexpr.getType().toString());
			type = service.saveNode(type);
			nodeNeoNodePair.put(vdexpr.getType(), type);
			service.saveEdge(nn.getId(), type.getId(), CypherStatment.PARNET);
			List<VariableDeclarator> vds = vdexpr.getVars();
			for (VariableDeclarator vd : vds) {
				NeoNode vdNN = create(vd);
				service.saveEdge(nn.getId(), vdNN.getId(), CypherStatment.PARNET);
			}
		} else if (node instanceof VariableDeclarator) { // 初始化语句 int x = 14
			VariableDeclarator vd = (VariableDeclarator) node;
			nn = new NeoNode(EnumNeoNodeLabelType.VARIBLEDECLARATOR.getValue(), "=");
			nn = service.saveNode(nn);
			nodeNeoNodePair.put(node, nn);
			NeoNode left = new NeoNode(EnumNeoNodeLabelType.ATOM.getValue(), vd.getId().getName());
			left = service.saveNode(left);
			nodeNeoNodePair.put(vd.getId(), left);
			service.saveEdge(nn.getId(), left.getId(), CypherStatment.PARNET);
			if (vd.getInit() != null) {
				NeoNode right = create(vd.getInit());
				service.saveEdge(nn.getId(), right.getId(), CypherStatment.PARNET);
			}
		} else if (node instanceof BlockStmt) { // BlockStmt {}
			nn = new NeoNode(EnumNeoNodeLabelType.BLOCKSTMT.getValue(), EnumNeoNodeLabelType.BLOCKSTMT.getValue());
			nn = service.saveNode(nn);
			nodeNeoNodePair.put(node, nn);
			pcBlockStmt(nn, node);
		} else if (node instanceof BinaryExpr) { // 二元表达式
			BinaryExpr binExpr = (BinaryExpr) node;
			nn = new NeoNode(EnumNeoNodeLabelType.BINARYEXPR.getValue(),
					ConvertEnumUtil.getBinaryOperator(binExpr.getOperator()));
			nn = service.saveNode(nn);
			nodeNeoNodePair.put(node, nn);
			NeoNode left = create(binExpr.getLeft());
			NeoNode right = create(binExpr.getRight());
			service.saveEdge(nn.getId(), left.getId(), CypherStatment.PARNET);
			service.saveEdge(nn.getId(), right.getId(), CypherStatment.PARNET);
		} else if (node instanceof AssignExpr) { // 赋值语句
			AssignExpr assExpr = (AssignExpr) node;
			nn = new NeoNode(EnumNeoNodeLabelType.ASSIGNEXPR.getValue(),
					ConvertEnumUtil.getAssignOperator(assExpr.getOperator()));
			nn = service.saveNode(nn);
			nodeNeoNodePair.put(node, nn);
			NeoNode target = create(assExpr.getTarget());
			NeoNode value = create(assExpr.getValue());
			service.saveEdge(nn.getId(), target.getId(), CypherStatment.PARNET);
			service.saveEdge(nn.getId(), value.getId(), CypherStatment.PARNET);
		} else if (node instanceof ExpressionStmt) { // 表达式
			nn = create(((ExpressionStmt) node).getExpression());
		} else if (node instanceof MethodCallExpr) { // 方法调用
			nn = new NeoNode(EnumNeoNodeLabelType.METHODCALLEXPR.getValue(), node.toString());
			nn = service.saveNode(nn);
			nodeNeoNodePair.put(node, nn);
		} else if (node instanceof ReturnStmt) { // Return语句，不往下细分？？？
			String nodeString = node.toString();
			nn = new NeoNode(EnumNeoNodeLabelType.RETURNSTMT.getValue(),
					nodeString.substring(0, nodeString.length() - 1));
			nn = service.saveNode(nn);
			nodeNeoNodePair.put(node, nn);
		} else if (node instanceof StringLiteralExpr) {
			StringLiteralExpr sle = (StringLiteralExpr) node;
			nn = new NeoNode(EnumNeoNodeLabelType.ATOM.getValue(), sle.getValue());
			nn = service.saveNode(nn);
			nodeNeoNodePair.put(node, nn);
		} else if (node instanceof IfStmt) { // if语句
			IfStmt ifStmt = (IfStmt) node;
			nn = new NeoNode(EnumNeoNodeLabelType.IFSTMT.getValue(), EnumNeoNodeLabelType.IFSTMT.getValue());
			service.saveNode(nn);
			nodeNeoNodePair.put(node, nn);
			Expression condition = ifStmt.getCondition();
			NeoNode conditionNN = create(condition);
			service.saveEdge(nn.getId(), conditionNN.getId(), CypherStatment.PARNET);
			Node thenNode = ifStmt.getThenStmt();
			NeoNode thenNN = create(thenNode);
			service.saveEdge(nn.getId(), thenNN.getId(), CypherStatment.PARNET);
			service.saveEdge(conditionNN.getId(), thenNN.getId(), CypherStatment.TRUE); // 添加控制依赖
			Node elseNode = ifStmt.getElseStmt();
			NeoNode elseNN = null;
			if (elseNode instanceof IfStmt) { // 处理else if语句
				elseNN = create(elseNode);
				service.saveEdge(nn.getId(), elseNN.getId(), CypherStatment.PARNET);
				service.saveEdge(conditionNN.getId(), elseNN.getId(), CypherStatment.FALSE);
			} else { // 处理else语句(带block和不带block)
				elseNN = new NeoNode(EnumNeoNodeLabelType.ELSE.getValue(), EnumNeoNodeLabelType.ELSE.getValue());
				elseNN = service.saveNode(elseNN);
				nodeNeoNodePair.put(elseNode, elseNN);
				service.saveEdge(nn.getId(), elseNN.getId(), CypherStatment.PARNET);
				service.saveEdge(conditionNN.getId(), elseNN.getId(), CypherStatment.FALSE);
				pcBlockStmt(elseNN, elseNode);
			}
		} else if (node instanceof SwitchStmt) { // switch语句
			SwitchStmt switchStmt = (SwitchStmt) node;
			nn = new NeoNode(EnumNeoNodeLabelType.SWITCHSTMT.getValue(), EnumNeoNodeLabelType.SWITCHSTMT.getValue());
			nn = service.saveNode(nn);
			nodeNeoNodePair.put(node, nn);
			NeoNode condition = create(switchStmt.getSelector());
			service.saveEdge(nn.getId(), condition.getId(), CypherStatment.PARNET);
			List<SwitchEntryStmt> seStmts = ((SwitchStmt) node).getEntries();
			for (SwitchEntryStmt seStmt : seStmts) {
				NeoNode entry = create(seStmt);
				service.saveEdge(nn.getId(), entry.getId(), CypherStatment.PARNET);
				if (!entry.getName().equals("default"))
					service.saveEdge(condition.getId(), entry.getId(), CypherStatment.EQUALS);
			}
		} else if (node instanceof SwitchEntryStmt) { // switch语句的case
			SwitchEntryStmt seStmt = (SwitchEntryStmt) node;
			nn = new NeoNode(EnumNeoNodeLabelType.SWITCHENTRY.getValue(),
					seStmt.getLabel() == null ? "default" : seStmt.getLabel().toString());
			nn = service.saveNode(nn);
			nodeNeoNodePair.put(node, nn);
			List<Statement> stmts = seStmt.getStmts();
			for (Statement stmt : stmts) {
				NeoNode stmtNN = create(stmt);
				service.saveEdge(nn.getId(), stmtNN.getId(), CypherStatment.PARNET);
			}
		} else if (node instanceof DoStmt) { // do-while语句
			DoStmt doStmt = (DoStmt) node;
			nn = new NeoNode(EnumNeoNodeLabelType.DOWHILESTMT.getValue(), EnumNeoNodeLabelType.DOWHILESTMT.getValue());
			nn = service.saveNode(nn);
			nodeNeoNodePair.put(node, nn);
			NeoNode condition = create(doStmt.getCondition());
			service.saveEdge(nn.getId(), condition.getId(), CypherStatment.PARNET);
			NeoNode body = create(doStmt.getBody());
			service.saveEdge(nn.getId(), body.getId(), CypherStatment.PARNET);
			service.saveEdge(condition.getId(), body.getId(), CypherStatment.TRUE);
			service.saveEdge(body.getId(), condition.getId(), CypherStatment.CDEPENDENCY);
		} else if (node instanceof WhileStmt) { // while语句
			WhileStmt whileStmt = (WhileStmt) node;
			nn = new NeoNode(EnumNeoNodeLabelType.WHILESTMT.getValue(), EnumNeoNodeLabelType.WHILESTMT.getValue());
			nn = service.saveNode(nn);
			nodeNeoNodePair.put(node, nn);
			NeoNode condition = create(whileStmt.getCondition());
			service.saveEdge(nn.getId(), condition.getId(), CypherStatment.PARNET);
			NeoNode body = create(whileStmt.getBody());
			service.saveEdge(nn.getId(), body.getId(), CypherStatment.PARNET);
			service.saveEdge(condition.getId(), body.getId(), CypherStatment.TRUE);
			service.saveEdge(body.getId(), condition.getId(), CypherStatment.CDEPENDENCY);
		} else if (node instanceof ForStmt) { // for语句
			ForStmt forStmt = (ForStmt) node;
			nn = new NeoNode(EnumNeoNodeLabelType.FORSTMT.getValue(), EnumNeoNodeLabelType.FORSTMT.getValue());
			nn = service.saveNode(nn);
			nodeNeoNodePair.put(node, nn);
			NeoNode init = new NeoNode(EnumNeoNodeLabelType.FORINIT.getValue(),
					EnumNeoNodeLabelType.FORINIT.getValue()); // init
			init = service.saveNode(init);
			for (Node initStmt : forStmt.getInit()) { // init的语句块
				NeoNode initNN = create(initStmt);
				service.saveEdge(init.getId(), initNN.getId(), CypherStatment.PARNET);
				if (initStmt instanceof AssignExpr) {
					AssignExpr ae = (AssignExpr) initStmt;
					varNodePair.put(ae.getTarget().toString(), initNN);
				} else if (initStmt instanceof VariableDeclarationExpr) {
					VariableDeclarationExpr vde = (VariableDeclarationExpr) initStmt;
					List<VariableDeclarator> vds = vde.getVars();
					for (VariableDeclarator vd : vds) {
						varNodePair.put(vd.getId().getName(), initNN);
					}
				}
			}
			service.saveEdge(nn.getId(), init.getId(), CypherStatment.PARNET);

			NeoNode cmp = create(forStmt.getCompare()); // compare
			service.saveEdge(nn.getId(), cmp.getId(), CypherStatment.PARNET);
			service.saveEdge(init.getId(), cmp.getId(), CypherStatment.CDEPENDENCY);

			NeoNode body = create(forStmt.getBody()); // body
			service.saveEdge(nn.getId(), body.getId(), CypherStatment.PARNET);
			service.saveEdge(cmp.getId(), body.getId(), CypherStatment.TRUE);

			NeoNode update = new NeoNode(EnumNeoNodeLabelType.FORUPDATE.getValue(),
					EnumNeoNodeLabelType.FORUPDATE.getValue()); // update
			update = service.saveNode(update);
			for (Node updateStmt : forStmt.getUpdate()) {
				NeoNode updateNN = create(updateStmt);
				service.saveEdge(update.getId(), updateNN.getId(), CypherStatment.PARNET);
			}
			service.saveEdge(nn.getId(), update.getId(), CypherStatment.PARNET);
			service.saveEdge(body.getId(), update.getId(), CypherStatment.CDEPENDENCY);
			service.saveEdge(update.getId(), cmp.getId(), CypherStatment.CDEPENDENCY);
		} else if (node instanceof ForeachStmt) { // foreach语句
			ForeachStmt foreachStmt = (ForeachStmt) node;
			nn = new NeoNode(EnumNeoNodeLabelType.FOREACHSTMT.getValue(), EnumNeoNodeLabelType.FOREACHSTMT.getValue());
			nn = service.saveNode(nn);
			nodeNeoNodePair.put(node, nn);
			NeoNode vdNN = create(foreachStmt.getVariable());
			service.saveEdge(nn.getId(), vdNN.getId(), CypherStatment.PARNET);
			NeoNode iterable = create(foreachStmt.getIterable());
			service.saveEdge(nn.getId(), iterable.getId(), CypherStatment.PARNET);
			NeoNode body = create(foreachStmt.getBody());
			service.saveEdge(nn.getId(), body.getId(), CypherStatment.PARNET);
			service.saveEdge(vdNN.getId(), iterable.getId(), CypherStatment.IN);
			service.saveEdge(vdNN.getId(), body.getId(), CypherStatment.CDEPENDENCY);
			service.saveEdge(body.getId(), vdNN.getId(), CypherStatment.CDEPENDENCY);
		} else {
			nn = new NeoNode(EnumNeoNodeLabelType.ATOM.getValue(), node.toString());
			nn = service.saveNode(nn);
			nodeNeoNodePair.put(node, nn);
		}

		return nn;
	}

	/**
	 * 处理if/for等语句的语句块，blockstmt或者expression
	 * 
	 * @param NeoNode
	 *            nn
	 * @param Node
	 *            node
	 */
	private void pcBlockStmt(NeoNode nn, Node node) {
		if (node instanceof BlockStmt) { // 如果是带括号的，则把这些语句并列起来，父节点就是if
			List<Statement> stmts = ((BlockStmt) node).getStmts();
			for (Statement stmt : stmts) {
				NeoNode stmtNN = create(stmt);
				service.saveEdge(nn.getId(), stmtNN.getId(), CypherStatment.PARNET);
			}
		} else {
			NeoNode stmtNN = create(node);
			service.saveEdge(nn.getId(), stmtNN.getId(), CypherStatment.PARNET);
		}
	}

	/**
	 * 根据CompilationUnit得到类成员
	 */
	public List<BodyDeclaration> getBodyList() {
		if (cu.getTypes() == null)
			return null;
		List<BodyDeclaration> bodyList = null;
		bodyList = cu.getTypes().stream().filter((n) -> (n instanceof ClassOrInterfaceDeclaration)).findFirst().get()
				.getMembers();
		for (TypeDeclaration type : cu.getTypes()) {
			if (type instanceof ClassOrInterfaceDeclaration) {
				bodyList = type.getMembers();
				return bodyList;
			}
		}
		return null;
	}

	private String convertType(Node node, Map<String, String> map) {
		String str = convertType(node, map, new HashMap<>());
		return str;
	}

	private String convertType(Node node, Map<String, String> map, Map<String, Integer> indexMap) {
		String str = "";
		if (node instanceof AssignExpr) {
			AssignExpr assignExpr = (AssignExpr) node;
			str = convertType(assignExpr.getTarget(), map, indexMap) + " "
					+ ConvertEnumUtil.getAssignOperator(assignExpr.getOperator()) + " "
					+ convertType(assignExpr.getValue(), map, indexMap);
		} else if (node instanceof BinaryExpr) {
			BinaryExpr binaryExpr = (BinaryExpr) node;
			str = convertType(binaryExpr.getLeft(), map, indexMap) + " "
					+ ConvertEnumUtil.getBinaryOperator(binaryExpr.getOperator()) + " "
					+ convertType(binaryExpr.getRight(), map, indexMap);
		} else if (node instanceof UnaryExpr) {
			UnaryExpr unaryExpr = (UnaryExpr) node;
			str = convertType(unaryExpr.getExpr(), map, indexMap) + " "
					+ ConvertEnumUtil.getUnaryOperator(unaryExpr.getOperator());
		} else if (node instanceof NameExpr) {
			NameExpr nameExpr = (NameExpr) node;
			if (map.containsKey(nameExpr.getName())) {
				String type = map.get(nameExpr.getName());
				if (indexMap.containsKey(type)) {
					str = type + indexMap.get(type);
					indexMap.put(type, indexMap.get(type) + 1);
				} else {
					str = type;
					indexMap.put(type, 1);
				}
			} else
				str = nameExpr.getName();
		} else if (node instanceof VariableDeclarationExpr) {
			VariableDeclarationExpr vde = (VariableDeclarationExpr) node;
			String type = vde.getType().toString();
			for (VariableDeclarator vd : vde.getVars()) {
				map.put(vd.getId().getName(), type);
			}
			str = vde.getVars().stream().map((n) -> (convertType(n, map, indexMap))).collect(Collectors.joining(","));
		} else if (node instanceof VariableDeclarator) {
			VariableDeclarator vd = (VariableDeclarator) node;
			String type = map.get(vd.getId().getName());
			if (indexMap.containsKey(type)) {
				if (vd.getInit() == null)
					str = type + indexMap.get(type);
				else
					str = type + indexMap.get(type) + " = " + convertType(vd.getInit(), map, indexMap);
				indexMap.put(type, indexMap.get(type) + 1);
			} else {
				if (vd.getInit() == null)
					str = type;
				else
					str = type + " = " + convertType(vd.getInit(), map, indexMap);
				indexMap.put(type, 1);
			}
		} else {
			str = node.toString();
		}
		return str;
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

	private boolean varExisted(Node node, String var) {
		if (node == null || var == null)
			return false;
		if (node instanceof NameExpr) {
			NameExpr nameExpr = (NameExpr) node;
			return var.equals(nameExpr.getName());
		} else if (node instanceof MethodCallExpr) {
			MethodCallExpr methodCallExpr = (MethodCallExpr) node;
			List<Expression> list = methodCallExpr.getArgs();
			for (Expression exp : list) {
				if (varExisted(exp, var))
					return true;
			}
		} else if (node instanceof BinaryExpr) {
			BinaryExpr binaryExpr = (BinaryExpr) node;
			return varExisted(binaryExpr.getLeft(), var) || varExisted(binaryExpr.getRight(), var);
		} else if (node instanceof AssignExpr) {
			AssignExpr assignExpr = (AssignExpr) node;
			return varExisted(assignExpr.getValue(), var);
		} else if (node instanceof ExpressionStmt) {
			ExpressionStmt es = (ExpressionStmt) node;
			return varExisted(es.getExpression(), var);
		}
		// TODO 待完成
		return false;
	}

	public CompilationUnit getCu() {
		return cu;
	}

	public void setCu(CompilationUnit cu) {
		this.cu = cu;
	}

	public INeoNodeService getService() {
		return service;
	}

	public void setService(INeoNodeService service) {
		this.service = service;
	}

	public static void main(String[] args) {
		GenerateGraph gg = new GenerateGraph(FileUtil.openCU("/Users/xiyaoguo/Desktop/XListView.java"));
		gg.analyzeMethod("onTouchEvent");
		// GenerateGraph gg2 = new
		// GenerateGraph(FileUtil.openCU("/Users/xiyaoguo/Desktop/PullToRefreshLinearLayout.java"));
		// gg2.analyzeMethod("onTouch");
		// GenerateGraph gg3 = new
		// GenerateGraph(FileUtil.openCU("/Users/xiyaoguo/Desktop/CustomFrameLayout.java"));
		// gg3.analyzeMethod("onTouchEvent");
		// GenerateGraph gg4 = new
		// GenerateGraph(FileUtil.openCU("/Users/xiyaoguo/Desktop/WaterDropListView.java"));
		// gg4.analyzeMethod("onTouchEvent");
		// GenerateGraph gg5 = new
		// GenerateGraph(FileUtil.openCU("/Users/xiyaoguo/Desktop/QuizActivity.java"));
		// gg5.analyzeMethod("onCreate");
	}
}
