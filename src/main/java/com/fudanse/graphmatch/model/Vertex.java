package com.fudanse.graphmatch.model;

import java.util.ArrayList;
import java.util.List;

public class Vertex {

	private Integer id; // 图的id
	private String content; // 内容
	private List<Vertex> childs; // 孩子结点
	private List<Vertex> dataDependencies; // 数据依赖
	private Vertex loopDependencies; // 循环依赖

	public Vertex() {
		init();
	}

	public Vertex(String content) {
		this.content = content;
		init();
	}

	public Vertex(int id) {
		this.id = id;
		init();
	}

	private void init() {
		childs = new ArrayList<Vertex>();
		dataDependencies = new ArrayList<Vertex>();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public List<Vertex> getChilds() {
		return childs;
	}

	public void setChilds(List<Vertex> childs) {
		this.childs = childs;
	}

	public List<Vertex> getDataDependencies() {
		return dataDependencies;
	}

	public void setDataDependencies(List<Vertex> dataDependencies) {
		this.dataDependencies = dataDependencies;
	}

	public Vertex getLoopDependencies() {
		return loopDependencies;
	}

	public void setLoopDependencies(Vertex loopDependencies) {
		this.loopDependencies = loopDependencies;
	}

	public void addDataDependencies(Vertex v) {
		this.dataDependencies.add(v);
	}

	public void addChilds(Vertex v) {
		if (v != null)
			this.childs.add(v);
	}

}
