package com.fudanse.graphmatch.model;

public class VarNode {

	private Integer id;

	private String signal;

	public VarNode(Integer id, String signal) {
		this.id = id;
		this.signal = signal;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getSignal() {
		return signal;
	}

	public void setSignal(String signal) {
		this.signal = signal;
	}

}
