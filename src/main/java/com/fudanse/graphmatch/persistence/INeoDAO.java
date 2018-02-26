package com.fudanse.graphmatch.persistence;

import com.fudanse.graphmatch.model.Edge;
import com.fudanse.graphmatch.model.NeoNode;

public interface INeoDAO {
	
	public Integer saveNeoNode(NeoNode node);
	
	public void addLabel(Integer id,String label);
	
	public void saveEdge(Integer left,Integer right,Edge e);
	
}
