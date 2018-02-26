package com.fudanse.graphmatch.persistence;

import static org.neo4j.driver.v1.Values.parameters;

import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;

import com.fudanse.graphmatch.model.Edge;
import com.fudanse.graphmatch.model.NeoNode;
import com.fudanse.graphmatch.util.CypherStatment;
import com.fudanse.graphmatch.util.Neo4JUtil;

public class Neo4JDAO implements INeoDAO {

	@Override
	public Integer saveNeoNode(NeoNode node) {
		Driver driver = Neo4JUtil.getDriver();
		Session session = Neo4JUtil.getSession(driver);
		Integer id = session.writeTransaction(new TransactionWork<Integer>() {
			@Override
			public Integer execute(Transaction tx) {
				StatementResult result = tx.run(CypherStatment.getInsertCypher(node),
						parameters("name", node.getName()));
				return result.single().get(0).asInt();
			}
		});
		Neo4JUtil.closeSession(session);
		Neo4JUtil.closeDriver(driver);
		return id;
	}

	@Override
	public void addLabel(Integer id, String label) {
		Driver driver = Neo4JUtil.getDriver();
		Session session = Neo4JUtil.getSession(driver);
		session.run(CypherStatment.getAddLabelCypher(id, label));
		Neo4JUtil.closeSession(session);
		Neo4JUtil.closeDriver(driver);
	}

	@Override
	public void saveEdge(Integer left, Integer right, Edge e) {
		Driver driver = Neo4JUtil.getDriver();
		Session session = Neo4JUtil.getSession(driver);
		session.run(CypherStatment.getInsertCypher(left, right, e));
		Neo4JUtil.closeSession(session);
		Neo4JUtil.closeDriver(driver);
	}

}
