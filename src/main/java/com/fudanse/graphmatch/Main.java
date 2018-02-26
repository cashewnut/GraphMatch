package com.fudanse.graphmatch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fudanse.graphmatch.model.NeoNode;
import com.fudanse.graphmatch.model.VarNode;
import com.fudanse.graphmatch.service.INeoNodeService;
import com.fudanse.graphmatch.service.NeoNodeService;
import com.fudanse.graphmatch.util.CypherStatment;
import com.fudanse.graphmatch.util.FileUtil;

import japa.parser.ast.CompilationUnit;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.TypeDeclaration;
import japa.parser.ast.stmt.Statement;

/**
 * Hello world!
 *
 */
public class Main {
	
	public void sss(Map<String,String> map){
		map.put("aaa", "bbb");
	}
	
	public void s(NeoNode node){
		node.setBelongto(5);
	}
	
	public static void main(String[] args) {
//		SemanticGraph sg = new SemanticGraph(FileUtil.openCU("/Users/xiyaoguo/Desktop/XListView.java"));
//		sg.analyzeMethod("onTouchEvent");
//		/*Map<NeoNode,String> map = new HashMap<>();
//		NeoNode n1 = new NeoNode("ss","ss");
//		NeoNode n2 = new NeoNode("ss","ss");
//		map.put(n1, "aaa");
//		System.out.println(map.get(n2));*/
////		INeoNodeService service = new NeoNodeService();
////		NeoNode node1 = new NeoNode("aaa","sss");
////		service.saveNode(node1);
//		//node1.setId(1);
//		//test t = new tests();
//		//t.modify(node1);
//		//node1.setId(1);
//		//service.saveNode(node1);
//		//System.out.println(node1.getId());
//		List<String> strs = new ArrayList<>();
//		for(String str : strs) {
//			System.out.println("str");
//		}
		ProjectToGraph pg = new ProjectToGraph(FileUtil.openCU("/Users/xiyaoguo/Desktop/XListView.java"));
		CompilationUnit cu = FileUtil.openCU("/Users/xiyaoguo/Desktop/XListView.java");
		List<BodyDeclaration> bodyList = null;
		for (TypeDeclaration type : cu.getTypes()) 
			if (type instanceof ClassOrInterfaceDeclaration) 
				bodyList = type.getMembers();
		MethodDeclaration md = null;
		for (BodyDeclaration body : bodyList) {
			if (body instanceof MethodDeclaration && ((MethodDeclaration) body).getName().equals("onTouchEvent"))
				md = (MethodDeclaration) body;
		}
		List<Statement> stmts = md.getBody().getStmts();
		NeoNode preNode = null;
		
		INeoNodeService service = new NeoNodeService();

		for (Statement stmt : stmts) {
			System.out.println("-----------------------------------------");
			NeoNode node = pg.create(stmt, new HashMap<String, String>(), new HashMap<String, VarNode>());
			if (preNode != null)
				service.saveEdge(preNode.getId(), node.getId(), CypherStatment.ORDER);
			preNode = node;
		}
		
		
	}
}
