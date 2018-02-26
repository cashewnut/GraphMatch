package com.fudanse.graphmatch.service;

import com.fudanse.graphmatch.model.Edge;
import com.fudanse.graphmatch.model.NeoNode;
import com.fudanse.graphmatch.persistence.INeoDAO;
import com.fudanse.graphmatch.persistence.Neo4JDAO;

public class NeoNodeService implements INeoNodeService {

	private INeoDAO neoDAO = new Neo4JDAO();

	@Override
	public NeoNode saveNode(NeoNode node) {
		if (node != null) {
			Integer id = neoDAO.saveNeoNode(node);
			node.setId(id);
		}
		return node;
	}

	@Override
	public boolean saveEdge(Integer left, Integer right, Edge e) {
		if (left != null && right != null)
			neoDAO.saveEdge(left, right, e);
		return true;
	}

}
