package com.fudanse.graphmatch.graphmining;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Path;
import org.neo4j.driver.v1.types.Relationship;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Administrator on 2017-12-28.
 */

public class sPath {
    double support;
    Path p;
    sPath(double support,Path p){
        this.support=support;
        this.p=p;
    }
    public String parsePath(){
        Iterator<Node> aNodes=p.nodes().iterator();
        Iterator<Relationship> aRelationships=p.relationships().iterator();
        List<Node> aNodeList=new ArrayList<Node>();
        List<Relationship> aRelationList=new ArrayList<Relationship>();
        while(aNodes.hasNext()){
            Node n=aNodes.next();
            aNodeList.add(n);
        }
        while(aRelationships.hasNext()){
            Relationship r=aRelationships.next();
            aRelationList.add(r);
        }
        String pStr=""+"(start{name:"+aNodeList.get(0).get("name")+"})";
        for(int i=1;i<aNodeList.size();i++){
            Node mNode=aNodeList.get(i);
            Relationship mRelationship=aRelationList.get(i-1);
            String mStr="-[:"+mRelationship.type()+"{name:"+mRelationship.get("name")+"}]->"+"({name:"+mNode.get("name")+"})";
            pStr+=mStr;
        }
        return pStr;
    }

    public List<Node> getNodes(){
        List<Node> nodeList=new ArrayList<Node>();
        Iterator<Node>nodes=p.nodes().iterator();
        while(nodes.hasNext()){
            Node n=nodes.next();
            nodeList.add(n);
        }
        return nodeList;
    }

}
