package com.fudanse.graphmatch.util;

import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.types.Path;

public class Neo4JUtil {

//	private final static String uri = "bolt://10.141.221.72:7687";
//	private final static String username = "neo4j";
//	private final static String password = "fdse";
	
	private final static String uri = "bolt://localhost:7687";
	private final static String username = "xiyaoguo@yeah.net";
	private final static String password = "5611786xyy";

	public static Driver getDriver() {
		Driver driver = null;
		try {
			driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return driver;
	}

	public static void closeDriver(Driver driver) {
		try {
			if (driver != null)
				driver.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Session getSession(Driver driver) {
		Session session = null;
		try {
			if (driver != null)
				session = driver.session();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return session;
	}

	public static void closeSession(Session session) {
		try {
			if (session != null)
				session.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Driver driver = Neo4JUtil.getDriver();
		Session session = Neo4JUtil.getSession(driver);
		Transaction tx = session.beginTransaction();

		StatementResult result = tx.run(
				"match p=(n)-[*0..]->(e) where id(n) in [274664, 274763, 273904, 275103, 273925, 274544, 275483, 274524, 274005] and id(e) in [274664, 274763, 273904, 275103, 273925, 274544, 275483, 274524, 274005] return p");
//		List<Record> r = result.list();
		for (Record r : result.list()) {
			Path p = r.get(0).asPath();
			System.out.println(r.get(0));
		}
		//System.out.println(result.list().size());

	}

}
