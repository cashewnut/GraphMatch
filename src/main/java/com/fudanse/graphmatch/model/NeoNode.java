package com.fudanse.graphmatch.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NeoNode implements Serializable{
	
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer id;
	private List<String> labels = new ArrayList<>();
	private String name;
	private Integer belongto;

	public NeoNode() {
	}

	public NeoNode(Integer id) {
		this.id = id;
	}

	public NeoNode(String label, String name) {
		this.labels.add(label);
		this.name = name;
	}

	public NeoNode(String label, String name, Integer belongto) {
		this.labels.add(label);
		this.name = name;
		this.belongto = belongto;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getLabel() {
		return labels.get(0);
	}

	public List<String> getLabels() {
		return labels;
	}

	public void setLabels(List<String> labels) {
		this.labels = labels;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getBelongto() {
		return belongto;
	}

	public void setBelongto(Integer belongto) {
		this.belongto = belongto;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((belongto == null) ? 0 : belongto.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((labels == null) ? 0 : labels.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NeoNode other = (NeoNode) obj;
		if (belongto == null) {
			if (other.belongto != null)
				return false;
		} else if (!belongto.equals(other.belongto))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (labels == null) {
			if (other.labels != null)
				return false;
		} else if (!labels.equals(other.labels))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
	

}
