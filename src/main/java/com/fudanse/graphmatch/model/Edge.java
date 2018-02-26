package com.fudanse.graphmatch.model;

public class Edge {

	private String label;
	private String name;

	private String sort;

	public Edge() {

	}

	public Edge(String label, String name) {
		this.label = label;
		this.name = name;
	}

	public Edge(String label, String name, String sort) {
		this.label = label;
		this.name = name;
		this.sort = sort;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

}
