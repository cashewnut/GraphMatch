package com.fudanse.graphmatch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fudanse.graphmatch.enums.EnumNeoNodeLabelType;
import com.fudanse.graphmatch.enums.EnumNeoNodeRelation;
import com.fudanse.graphmatch.model.Edge;
import com.fudanse.graphmatch.model.NeoNode;
import com.fudanse.graphmatch.model.VarNode;
import com.fudanse.graphmatch.service.INeoNodeService;
import com.fudanse.graphmatch.service.NeoNodeService;
import com.fudanse.graphmatch.util.ConvertEnumUtil;
import com.fudanse.graphmatch.util.CypherStatment;
import com.fudanse.graphmatch.util.FileUtil;

import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
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
 * 项目转图
 * 
 * @author xiyaoguo
 *
 */
public class ProjectToGraph {

	private INeoNodeService service;
	// private CompilationUnit cu;
	private Map<String, Integer> pgNIdPair = new HashMap<>();// package和对应的node的id
	// private Integer rootId;

	public ProjectToGraph() {
		this.service = new NeoNodeService();
	}

	public ProjectToGraph(CompilationUnit cu) {
		this.service = new NeoNodeService();
	}

	public void analyzePj(String filePath) {
		File file = new File(filePath);
		System.out.println(file.getName());
		analyzePj(file);
		file.renameTo(new File("/home/fdse/xiyaoguo/javacode/app1/" + file.getName()));
		System.out.println(file.getName() + "has been moved!");
	}

	public void analyzePj(File file) {
		if (file == null)
			return;
		if (!file.isDirectory())
			return;
		String fileName = file.getName();
		if (fileName.endsWith(".tar.gz"))
			fileName = fileName.substring(0, fileName.length() - 7);
		List<File> javaFiles = FileUtil.getJavaFiles(file);
		System.out.println("this file has " + javaFiles.size() + " class");
		if (javaFiles.size() > 25)
			return;
		NeoNode pjNode = new NeoNode(EnumNeoNodeLabelType.PROJECT.getValue(), fileName);
		pjNode = service.saveNode(pjNode);
		// this.rootId = pjNode.getId();

		javaFiles.forEach((n) -> System.out.println(n.getAbsolutePath()));
		analyzeJavaFile(pjNode, javaFiles);
	}

	private void analyzeJavaFile(NeoNode pjNode, List<File> javaFiles) {
		for (File javaFile : javaFiles) {
			CompilationUnit cu = FileUtil.openCU(javaFile);
			Integer pgId = createPackage(pjNode, cu); // package
			Integer classId = createClass(cu);// class
			if (classId == null)
				continue;
			if (pgId != null)
				service.saveEdge(pgId, classId, CypherStatment.PARNET);
			List<BodyDeclaration> body = getBodyList(cu);
			Map<String, String> fieldMap = getField(body);
			List<MethodDeclaration> methods = getMethodList(body);
			for (MethodDeclaration method : methods) {
				Map<String, String> methodFieldMap = fieldMap;
				Integer methodId = createMethod(method, methodFieldMap);
				service.saveEdge(classId, methodId, CypherStatment.PARNET);
				createMethodBody(method, methodId, methodFieldMap);
			}
			// createMethod(methods,classId);
		}
	}

	/**
	 * 
	 * @param cu
	 * @return map key:var value:type
	 */
	private Map<String, String> getField(List<BodyDeclaration> body) {
		Map<String, String> map = new HashMap<>();
		List<FieldDeclaration> fds = body.stream().filter((n) -> (n instanceof FieldDeclaration))
				.map((n) -> (FieldDeclaration) n).collect(Collectors.toList());
		for (FieldDeclaration fd : fds) {
			String type = fd.getType().toString();
			for (VariableDeclarator vd : fd.getVariables()) {
				String var = vd.getId().getName();
				map.put(var, type);
			}
		}
		return map;
	}

	private void createMethodBody(MethodDeclaration method, Integer methodId, Map<String, String> map) {
		BlockStmt blockStmt = method.getBody();// 有可能是抽象类或接口声明的方法，此时body为空
		if (blockStmt == null)
			return;
		List<Statement> stmts = blockStmt.getStmts();
		if (stmts == null)
			stmts = new ArrayList<>();
		NeoNode preNode = null;
		for (Statement stmt : stmts) {
			// System.out.println("-----------------------------------------");
			NeoNode node = create(stmt, map, new HashMap<>());
			service.saveEdge(methodId, node.getId(), CypherStatment.PARNET);
			if (preNode != null)
				service.saveEdge(preNode.getId(), node.getId(), CypherStatment.ORDER);
			preNode = node;
		}
	}

	/**
	 * 
	 * @param node
	 * @param fieldMap
	 * @param varMap<变量名：变量赋值位置>
	 * @return
	 */
	public NeoNode create(Node node, Map<String, String> fieldMap, Map<String, VarNode> varMap) {
		NeoNode nn = null;
		List<String> sortList = null; // 每个语句中的变量名列表
		if (node == null)
			return null;
		if (node instanceof EnclosedExpr) { // 括号()
			EnclosedExpr enclosedExpr = (EnclosedExpr) node;
			nn = create(enclosedExpr.getInner(), fieldMap, varMap);
		} else if (node instanceof ExpressionStmt) { // 表达式
			nn = create(((ExpressionStmt) node).getExpression(), fieldMap, varMap);
		} else if (node instanceof MethodCallExpr) { // 方法调用
			MethodCallExpr methodCallExpr = (MethodCallExpr) node;
			if (!(methodCallExpr.getScope() != null && methodCallExpr.getScope().toString().startsWith("Log"))) {
				nn = new NeoNode(EnumNeoNodeLabelType.METHODCALLEXPR.getValue(), convertType(node, fieldMap));
				service.saveNode(nn);
				sortList = new ArrayList<>();
				dataDependency(fieldMap, varMap, nn, sortList, methodCallExpr);
			} else
				nn = new NeoNode();
		} else if (node instanceof ReturnStmt) { // Return语句，不往下细分？？？
			String nodeString = node.toString();
			nn = new NeoNode(EnumNeoNodeLabelType.RETURNSTMT.getValue(),
					nodeString.substring(0, nodeString.length() - 1));
			service.saveNode(nn);
		} else if (node instanceof StringLiteralExpr) {
			StringLiteralExpr sle = (StringLiteralExpr) node;
			nn = new NeoNode(EnumNeoNodeLabelType.ATOM.getValue(), sle.getValue());
			service.saveNode(nn);
		} else if (node instanceof AssignExpr) {
			AssignExpr assExpr = (AssignExpr) node;
			nn = new NeoNode(EnumNeoNodeLabelType.ASSIGNEXPR.getValue(), convertType(assExpr, fieldMap));
			service.saveNode(nn);
			sortList = new ArrayList<>();
			dataDependency(fieldMap, varMap, nn, sortList, assExpr);
		} else if (node instanceof BinaryExpr) {
			BinaryExpr binaryExpr = (BinaryExpr) node;
			nn = new NeoNode(EnumNeoNodeLabelType.BINARYEXPR.getValue(), convertType(binaryExpr, fieldMap));
			service.saveNode(nn);
			sortList = new ArrayList<>();
			dataDependency(fieldMap, varMap, nn, sortList, binaryExpr);
		} else if (node instanceof VariableDeclarationExpr) {
			VariableDeclarationExpr vde = (VariableDeclarationExpr) node;
			nn = new NeoNode(EnumNeoNodeLabelType.VARIBLEDECLARATIONEXPR.getValue(), convertType(vde, fieldMap));
			service.saveNode(nn);
			sortList = new ArrayList<>();
			dataDependency(fieldMap, varMap, nn, sortList, vde);
		} else if (node instanceof BlockStmt) { // BlockStmt {}
			nn = new NeoNode(EnumNeoNodeLabelType.BLOCKSTMT.getValue(), EnumNeoNodeLabelType.BLOCKSTMT.getValue());
			service.saveNode(nn);
			pcBlockStmt(nn, node, fieldMap, varMap);
		} else if (node instanceof IfStmt) {
			IfStmt ifStmt = (IfStmt) node;
			nn = new NeoNode(EnumNeoNodeLabelType.IFSTMT.getValue(), EnumNeoNodeLabelType.IFSTMT.getValue());
			service.saveNode(nn);
			Expression condition = ifStmt.getCondition();
			NeoNode conditionNN = create(condition, fieldMap, varMap);
			service.saveEdge(nn.getId(), conditionNN.getId(), CypherStatment.PARNET);
			Statement thenStmt = ifStmt.getThenStmt();
			NeoNode thenNN = create(thenStmt, fieldMap, varMap);
			service.saveEdge(nn.getId(), thenNN.getId(), CypherStatment.PARNET);
			service.saveEdge(conditionNN.getId(), thenNN.getId(), CypherStatment.TRUE); // 添加控制依赖
			Statement elseStme = ifStmt.getElseStmt();
			if (elseStme != null) {
				NeoNode elseNN = null;
				if (elseStme instanceof IfStmt) { // 处理else if语句
					elseNN = create(elseStme, fieldMap, varMap);
					service.saveEdge(nn.getId(), elseNN.getId(), CypherStatment.PARNET);
					service.saveEdge(conditionNN.getId(), elseNN.getId(), CypherStatment.FALSE);
				} else {
					elseNN = new NeoNode(EnumNeoNodeLabelType.ELSE.getValue(), EnumNeoNodeLabelType.ELSE.getValue());
					elseNN = service.saveNode(elseNN);
					service.saveEdge(nn.getId(), elseNN.getId(), CypherStatment.PARNET);
					service.saveEdge(conditionNN.getId(), elseNN.getId(), CypherStatment.FALSE);
					pcBlockStmt(elseNN, elseStme, fieldMap, varMap);
				}
			}
		} else if (node instanceof SwitchStmt) {
			SwitchStmt switchStmt = (SwitchStmt) node;
			nn = new NeoNode(EnumNeoNodeLabelType.SWITCHSTMT.getValue(), EnumNeoNodeLabelType.SWITCHSTMT.getValue());
			nn = service.saveNode(nn);
			NeoNode condition = create(switchStmt.getSelector(), fieldMap, varMap);
			service.saveEdge(nn.getId(), condition.getId(), CypherStatment.PARNET);
			List<SwitchEntryStmt> seStmts = ((SwitchStmt) node).getEntries();
			if (seStmts != null)
				for (SwitchEntryStmt seStmt : seStmts) {
					NeoNode entry = create(seStmt, fieldMap, varMap);
					service.saveEdge(nn.getId(), entry.getId(), CypherStatment.PARNET);
					if (!entry.getName().equals("default"))
						service.saveEdge(condition.getId(), entry.getId(), CypherStatment.EQUALS);
					else
						service.saveEdge(condition.getId(), entry.getId(), CypherStatment.DEFAULT);
				}
		} else if (node instanceof SwitchEntryStmt) {
			SwitchEntryStmt seStmt = (SwitchEntryStmt) node;
			nn = new NeoNode(EnumNeoNodeLabelType.SWITCHENTRY.getValue(),
					seStmt.getLabel() == null ? "default" : convertType(seStmt.getLabel(), fieldMap));
			nn = service.saveNode(nn);
			List<Statement> stmts = seStmt.getStmts();
			if (stmts == null)
				stmts = new ArrayList<>();
			NeoNode preNode = null;
			for (Statement stmt : stmts) {
				NeoNode stmtNN = create(stmt, fieldMap, varMap);
				service.saveEdge(nn.getId(), stmtNN.getId(), CypherStatment.PARNET);
				if (preNode != null)
					service.saveEdge(preNode.getId(), stmtNN.getId(), CypherStatment.ORDER);
				preNode = stmtNN;
			}
		} else if (node instanceof DoStmt) {
			DoStmt doStmt = (DoStmt) node;
			nn = new NeoNode(EnumNeoNodeLabelType.DOWHILESTMT.getValue(), EnumNeoNodeLabelType.DOWHILESTMT.getValue());
			service.saveNode(nn);
			NeoNode condition = create(doStmt.getCondition(), fieldMap, varMap);
			service.saveEdge(nn.getId(), condition.getId(), CypherStatment.PARNET);
			NeoNode body = create(doStmt.getBody(), fieldMap, varMap);
			service.saveEdge(nn.getId(), body.getId(), CypherStatment.PARNET);
			service.saveEdge(condition.getId(), body.getId(), CypherStatment.TRUE);
			service.saveEdge(body.getId(), condition.getId(), CypherStatment.CDEPENDENCY);
		} else if (node instanceof WhileStmt) {
			WhileStmt whileStmt = (WhileStmt) node;
			nn = new NeoNode(EnumNeoNodeLabelType.WHILESTMT.getValue(), EnumNeoNodeLabelType.WHILESTMT.getValue());
			service.saveNode(nn);
			NeoNode condition = create(whileStmt.getCondition(), fieldMap, varMap);
			service.saveEdge(nn.getId(), condition.getId(), CypherStatment.PARNET);
			NeoNode body = create(whileStmt.getBody(), fieldMap, varMap);
			service.saveEdge(nn.getId(), body.getId(), CypherStatment.PARNET);
			service.saveEdge(condition.getId(), body.getId(), CypherStatment.TRUE);
			service.saveEdge(body.getId(), condition.getId(), CypherStatment.CDEPENDENCY);
		} else if (node instanceof ForStmt) { // for语句
			ForStmt forStmt = (ForStmt) node;
			nn = new NeoNode(EnumNeoNodeLabelType.FORSTMT.getValue(), EnumNeoNodeLabelType.FORSTMT.getValue());
			service.saveNode(nn);
			String initstr = "";
			if (forStmt.getInit() != null) {
				initstr = forStmt.getInit().stream().map((n) -> convertType(n, fieldMap))
						.collect(Collectors.joining(","));
			}
			NeoNode init = new NeoNode(EnumNeoNodeLabelType.FORINIT.getValue(), initstr); // init
			service.saveNode(init);
			service.saveEdge(nn.getId(), init.getId(), CypherStatment.PARNET);

			NeoNode cmp = create(forStmt.getCompare(), fieldMap, varMap); // compare
			if (cmp != null) {
				service.saveEdge(nn.getId(), cmp.getId(), CypherStatment.PARNET);
				service.saveEdge(init.getId(), cmp.getId(), CypherStatment.CDEPENDENCY);
			}

			NeoNode body = create(forStmt.getBody(), fieldMap, varMap); // body
			if (body != null) {
				service.saveEdge(nn.getId(), body.getId(), CypherStatment.PARNET);
				if (cmp != null)
					service.saveEdge(cmp.getId(), body.getId(), CypherStatment.TRUE);
			}

			String updatestr = "";
			if (forStmt.getUpdate() != null)
				updatestr = forStmt.getUpdate().stream().map((n) -> convertType(n, fieldMap))
						.collect(Collectors.joining(","));
			NeoNode update = new NeoNode(EnumNeoNodeLabelType.FORUPDATE.getValue(), updatestr); // update
			update = service.saveNode(update);
			if (update != null)
				service.saveEdge(nn.getId(), update.getId(), CypherStatment.PARNET);
			if (body != null && update != null)
				service.saveEdge(body.getId(), update.getId(), CypherStatment.CDEPENDENCY);
			if (update != null && cmp != null)
				service.saveEdge(update.getId(), cmp.getId(), CypherStatment.CDEPENDENCY);
		} else if (node instanceof ForeachStmt) { // foreach语句
			ForeachStmt foreachStmt = (ForeachStmt) node;
			nn = new NeoNode(EnumNeoNodeLabelType.FOREACHSTMT.getValue(), EnumNeoNodeLabelType.FOREACHSTMT.getValue());
			service.saveNode(nn);
			NeoNode vdNN = create(foreachStmt.getVariable(), fieldMap, varMap);
			service.saveEdge(nn.getId(), vdNN.getId(), CypherStatment.PARNET);
			NeoNode iterable = create(foreachStmt.getIterable(), fieldMap, varMap);
			service.saveEdge(nn.getId(), iterable.getId(), CypherStatment.PARNET);
			NeoNode body = create(foreachStmt.getBody(), fieldMap, varMap);
			service.saveEdge(nn.getId(), body.getId(), CypherStatment.PARNET);
			service.saveEdge(vdNN.getId(), iterable.getId(), CypherStatment.IN);
			service.saveEdge(vdNN.getId(), body.getId(), CypherStatment.CDEPENDENCY);
			service.saveEdge(body.getId(), vdNN.getId(), CypherStatment.CDEPENDENCY);
		} else {
			nn = new NeoNode(EnumNeoNodeLabelType.ATOM.getValue(), node.toString());
			service.saveNode(nn);
		}
		return nn;
	}

	/**
	 * 
	 * @param fieldMap<变量名:变量类型>
	 * @param varMap<变量名:变量声明赋值位置>
	 * @param nn<变量所在节点>
	 * @param sortList<语句中变量列表>
	 * @param node<语句node>
	 */
	private void dataDependency(Map<String, String> fieldMap, Map<String, VarNode> varMap, NeoNode nn,
			List<String> sortList, Node node) {
		if (node instanceof BinaryExpr) {
			BinaryExpr binaryExpr = (BinaryExpr) node;
			sortNameExpr(binaryExpr, fieldMap, sortList);
			for (int i = 0; i < sortList.size(); i++) {
				String var = sortList.get(i);
				if (!varMap.containsKey(var))
					continue;
				VarNode varNode = varMap.get(var);
				service.saveEdge(varNode.getId(), nn.getId(),
						new Edge(EnumNeoNodeRelation.DDEPENDENCY.getValue(), varNode.getSignal(), i + ""));
			}
		} else if (node instanceof AssignExpr) {
			AssignExpr assignExpr = (AssignExpr) node;
			dataDependency(fieldMap, varMap, nn, sortList, assignExpr.getValue());
			VarNode varNode = new VarNode(nn.getId(), fieldMap.get(assignExpr.getTarget().toString()));
			varMap.put(assignExpr.getTarget().toString(), varNode);
		} else if (node instanceof MethodCallExpr) {
			MethodCallExpr methodCallExpr = (MethodCallExpr) node;
			if (methodCallExpr.getArgs() == null)
				return;
			sortNameExpr(methodCallExpr, fieldMap, sortList);
			for (int i = 0; i < sortList.size(); i++) {
				String var = sortList.get(i);
				if (!varMap.containsKey(var))
					continue;
				VarNode varNode = varMap.get(var);
				service.saveEdge(varNode.getId(), nn.getId(),
						new Edge(EnumNeoNodeRelation.DDEPENDENCY.getValue(), varNode.getSignal(), i + ""));
			}
		} else if (node instanceof NameExpr) {
			String name = ((NameExpr) node).getName();
			if (!varMap.containsKey(name))
				return;
			VarNode varNode = varMap.get(name);
			service.saveEdge(varNode.getId(), nn.getId(),
					new Edge(EnumNeoNodeRelation.DDEPENDENCY.getValue(), varNode.getSignal(), "1"));
		} else if (node instanceof VariableDeclarationExpr) {
			VariableDeclarationExpr vde = (VariableDeclarationExpr) node;
			sortNameExpr(vde, fieldMap, sortList);
			for (VariableDeclarator vd : vde.getVars()) {
				if (vd.getInit() == null)
					continue;
				dataDependency(fieldMap, varMap, nn, sortList, vd.getInit());
			}
			List<VariableDeclarator> vds = vde.getVars();
			for (int i = 0; i < vds.size(); i++) {
				VarNode varNode = new VarNode(nn.getId(), fieldMap.get(vds.get(0).getId().toString()));
				varMap.put(vds.get(0).getId().toString(), varNode);
			}

		}
	}

	/**
	 * 
	 * @param node<语句node>
	 * @param fieldMap<变量名:变量类型>
	 * @param sortList<语句中变量列表>
	 */
	private void sortNameExpr(Node node, Map<String, String> fieldMap, List<String> sortList) {
		if (node == null)
			return;
		if (node instanceof AssignExpr) {
			AssignExpr assignExpr = (AssignExpr) node;
			sortNameExpr(assignExpr.getTarget(), fieldMap, sortList);
			sortNameExpr(assignExpr.getValue(), fieldMap, sortList);
		} else if (node instanceof BinaryExpr) {
			BinaryExpr binaryExpr = (BinaryExpr) node;
			sortNameExpr(binaryExpr.getLeft(), fieldMap, sortList);
			sortNameExpr(binaryExpr.getRight(), fieldMap, sortList);
		} else if (node instanceof UnaryExpr) {
			UnaryExpr unaryExpr = (UnaryExpr) node;
			sortNameExpr(unaryExpr.getExpr(), fieldMap, sortList);
		} else if (node instanceof MethodCallExpr) {
			MethodCallExpr methodCallExpr = (MethodCallExpr) node;
			if (methodCallExpr.getArgs() != null)
				methodCallExpr.getArgs().stream().forEach((n) -> sortNameExpr(n, fieldMap, sortList));
		} else if (node instanceof NameExpr) {
			NameExpr nameExpr = (NameExpr) node;
			sortList.add(nameExpr.getName());
		} else if (node instanceof VariableDeclarationExpr) {
			VariableDeclarationExpr vde = (VariableDeclarationExpr) node;
			for (VariableDeclarator vd : vde.getVars())
				if (vd.getInit() != null)
					sortNameExpr(vd.getInit(), fieldMap, sortList);
		} else if (node instanceof NameExpr) {
			NameExpr nameExpr = (NameExpr) node;
			sortList.add(nameExpr.getName());
		}
	}

	/**
	 * 处理if/for等语句的语句块，blockstmt或者expression
	 * 
	 * @param NeoNode
	 *            nn
	 * @param Node
	 *            node
	 */
	private void pcBlockStmt(NeoNode nn, Node node, Map<String, String> map, Map<String, VarNode> varMap) {
		if (node instanceof BlockStmt) { // 如果是带括号的，则把这些语句并列起来，父节点就是if
			List<Statement> stmts = ((BlockStmt) node).getStmts();
			NeoNode preNode = null;
			if (stmts != null) {
				for (Statement stmt : stmts) {
					NeoNode stmtNN = create(stmt, map, varMap);
					service.saveEdge(nn.getId(), stmtNN.getId(), CypherStatment.PARNET);
					if (preNode != null)
						service.saveEdge(preNode.getId(), stmtNN.getId(), CypherStatment.ORDER);
					preNode = stmtNN;
				}
			}
		} else {
			NeoNode stmtNN = create(node, map, varMap);
			service.saveEdge(nn.getId(), stmtNN.getId(), CypherStatment.PARNET);
		}
	}

	/**
	 * 创建method节点
	 * 
	 * @param methodDeclaration
	 * @return
	 */
	private Integer createMethod(MethodDeclaration methodDeclaration, Map<String, String> map) {
		NeoNode methodNode = new NeoNode(EnumNeoNodeLabelType.METHODDECLARATION.getValue(),
				methodDeclaration.getName());
		service.saveNode(methodNode);
		if (methodDeclaration.getParameters() != null)
			for (Parameter parameter : methodDeclaration.getParameters()) {
				map.put(parameter.getId().getName(), parameter.getType().toString());
			}
		return methodNode.getId();
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
			vde.getVars().forEach((n) -> map.put(n.getId().getName(), type));
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
		} else if (node instanceof MethodCallExpr) {
			MethodCallExpr methodCallExpr = (MethodCallExpr) node;
			if (methodCallExpr.getScope() != null)
				str = convertType(methodCallExpr.getScope(), map, indexMap) + ".";
			String args = "";
			if (methodCallExpr.getArgs() != null)
				args = methodCallExpr.getArgs().stream().map((n) -> convertType(n, map, indexMap))
						.collect(Collectors.joining(","));
			str = str + methodCallExpr.getName() + "(" + args + ")";
		} else {
			str = node.toString();
		}
		return str;
	}

	/**
	 * 从类中获取方法的bodylist
	 * 
	 * @param body
	 * @return
	 */
	private List<MethodDeclaration> getMethodList(List<BodyDeclaration> body) {
		List<MethodDeclaration> methods = null;
		methods = body.stream().filter((n) -> (n instanceof MethodDeclaration)).map((n) -> ((MethodDeclaration) n))
				.collect(Collectors.toList());
		return methods;
	}

	/**
	 * 创建类节点
	 * 
	 * @param cu
	 * @return
	 */
	private Integer createClass(CompilationUnit cu) {
		TypeDeclaration td = getClass(cu);// td中包含类的信息
		if (td == null)
			return null;
		String className = td.getName();
		NeoNode classNode = new NeoNode(EnumNeoNodeLabelType.CLASSORINTERFACE.getValue(), className);
		service.saveNode(classNode);

		return classNode.getId();
	}

	/**
	 * 从类中获取classorinteface信息
	 * 
	 * @param cu
	 * @return
	 */
	private TypeDeclaration getClass(CompilationUnit cu) {
		if (cu.getTypes() == null)
			return null;
		Optional<TypeDeclaration> op = cu.getTypes().stream().filter((n) -> (n instanceof ClassOrInterfaceDeclaration))
				.findFirst();
		if (!op.isPresent())
			return null;
		return op.get();
	}

	/**
	 * 根据CompilationUnit得到类成员
	 * 
	 * @param cu
	 * @return 类成员
	 */
	private List<BodyDeclaration> getBodyList(CompilationUnit cu) {
		TypeDeclaration td = getClass(cu);
		return td.getMembers();
	}

	/**
	 * 创建package，存入map中
	 * 
	 * @param pjNode
	 * @param cu
	 * @return package's id
	 */
	private Integer createPackage(NeoNode pjNode, CompilationUnit cu) {
		try {
			String pgName = cu.getPackage().getName().toString();
			if (!pgNIdPair.containsKey(pgName)) {
				NeoNode pgNode = new NeoNode(EnumNeoNodeLabelType.PACKAGE.getValue(), pgName);
				pgNode = service.saveNode(pgNode);
				service.saveEdge(pjNode.getId(), pgNode.getId(), CypherStatment.PARNET);
				pgNIdPair.put(pgName, pgNode.getId());
			}
			return pgNIdPair.get(pgName);
		} catch (NullPointerException e) {

		}
		return null;
	}

	public INeoNodeService getService() {
		return service;
	}

	public void setService(INeoNodeService service) {
		this.service = service;
	}

	public static void main(String[] args) {
		/*
		 * List<File> javaFiles = FileUtil.getJavaFiles(new
		 * File("/Users/xiyaoguo/Desktop/TimeZoneEdit.java")); javaFiles.forEach((n) ->
		 * System.out.println(n.getAbsolutePath()));
		 */
		/*
		 * CompilationUnit cu = FileUtil.openCU(new
		 * File("/Users/xiyaoguo/Desktop/TimeZoneEdit.java")); ProjectToGraph pg = new
		 * ProjectToGraph(); List<BodyDeclaration> body = pg.getBodyList(cu);
		 * List<MethodDeclaration> methods = pg.getMethodList(body); Map<String, String>
		 * map = new HashMap<>(); for (MethodDeclaration method : methods) {
		 * pg.createMethod(method, map); } body.size();
		 */
		String basePath = "/Users/xiyaoguo/Documents/flow/";
//		String basePath = "/home/fdse/xiyaoguo/javacode/app/";
		File file = new File(basePath);
		ProjectToGraph pg = null;
		if (file.isDirectory()) {
			String[] pj = file.list();
			for (String path : pj) {
				if (!path.startsWith(".")) {
					pg = new ProjectToGraph();
					pg.analyzePj(basePath + path);
					System.out.println();
				}
			}
		} else {
			System.out.println("the file is not a directory!");
		}
		// pg.analyzePj("/Users/xiyaoguo/Documents/androidproject/WaveSwipeRefreshLayout");
		// pg.analyzePj("/Users/xiyaoguo/Desktop/WorldClock");
		// pg.convertType(node, map)
	}

}
