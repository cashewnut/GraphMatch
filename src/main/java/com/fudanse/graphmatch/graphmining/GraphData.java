package com.fudanse.graphmatch.graphmining;

import java.util.Date;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

/**
 * Created by Administrator on 2017-12-21.
 */
public class GraphData {
	public static final double sup = 0.6;
	public final static Driver driver = GraphDatabase.driver("bolt://localhost:7687",
			AuthTokens.basic("xiyaoguo@yeah.net", "5611786xyy"));
	public static Session session = null;
	public static Transaction tx = null;

	// public static StatementResult result=null;
	public static void databaseConnection() {
		GraphData.session = GraphData.driver.session();
		GraphData.tx = GraphData.session.beginTransaction();
		System.out.println("Connect graphdatabase successfully!");
	}

	public static StatementResult graphMatch(String str) {
		StatementResult result = GraphData.tx.run(str);
		return result;
	}

	public static void main(String[] args) {
		try {
			GraphData.databaseConnection();
			//onFling
			String str = "match p=(m:MethodDeclaration{name:'onFling'})-[*1..]->(d:MethodCallExpr)-[*0..]->(f) where d.name in ['this.flipper.showPrevious()','this.flipper.showNext()'] return p";
			
			//DOWN
//			String str = "match (n:Project)-[:Parent*0..]->(m:MethodDeclaration{name:'onTouchEvent'})-[*1..]->(b:SwitchStmt{name:'SwitchStmt'})-[:Parent]->(d:SwitchEntry{name:'MotionEvent.ACTION_DOWN'}) with d match p=(d)-[:Parent*1..]->(e:AssighExpr{name:'float = MotionEvent.getY()'})-[*1..]->(f) return p";
			
			//MOVE
//			String str = "match (n:Project)-[:Parent*0..]->(m:MethodDeclaration{name:'onTouchEvent'})-[*1..]->(b:SwitchStmt{name:'SwitchStmt'})-[:Parent]->(d:SwitchEntry{name:'MotionEvent.ACTION_MOVE'}) with d match p=(d)-[:Parent*1..]->(e)  return p";
			
			//UP
//			String str = "match (n:Project)-[:Parent*0..]->(m:MethodDeclaration{name:'onTouchEvent'})-[*1..]->(b:SwitchStmt{name:'SwitchStmt'})-[:Parent]->(d:SwitchEntry{name:'MotionEvent.ACTION_UP'}) with d match p=(d)-[:Parent*1..]->(e)  return p";
			Date begin = new Date();
			StatementResult result = GraphData.graphMatch(str);
			Date media = new Date();
			System.out.println(media.getTime() - begin.getTime());
			AuthoritativePaths auPaths = SubGraphMining.miningProcess(result);
			// System.out.println(auPaths.visualizationPaths());
			// auPaths.outputPath();
			System.out.println(auPaths.visualizationPaths2());
			Date end = new Date();
			System.out.println(end.getTime() - media.getTime());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
