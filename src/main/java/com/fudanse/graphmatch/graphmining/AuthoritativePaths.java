package com.fudanse.graphmatch.graphmining;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.driver.v1.types.Node;
/**
 * Created by Administrator on 2017-12-25.
 */
public class AuthoritativePaths {
    List<sPath> aPaths;
    long aKey;
    int matchNum=0;
    AuthoritativePaths(){

    }
    AuthoritativePaths(List<sPath> sPaths,long key,int num){
        this.aPaths=sPaths;
        this.aKey=key;
        this.matchNum=num;
    }

    public String visualizationPaths(){
        String visPath="match ";
        long pathIndex=1;
        for(sPath sp:aPaths){
            if(sp.support>=GraphData.sup*matchNum){
                String pathName="p"+pathIndex+"=";
                String str=sp.parsePath()+",";
                visPath+=pathName;
                visPath+=str;
                pathIndex++;
            }
        }
        String finalVisPath=visPath.substring(0,visPath.length()-1);
        finalVisPath+=" return *";
        return finalVisPath;
    }
    public String visualizationPaths2(){
        String visNodes="match (n) where id(n) in ";
        List<Long> auNodeIdList=new ArrayList<Long>();
        for(sPath sp:aPaths){
            if(sp.support>=GraphData.sup){
                List<Node> nodeList=sp.getNodes();
                for(Node n:nodeList){
                    long id=n.id();
                    if(!auNodeIdList.contains(id)){
                        auNodeIdList.add(id);
                    }
                }
            }
        }
        visNodes+=auNodeIdList.toString();
        visNodes+=" return n";
        return visNodes;
    }

    public void outputPath(){
        for(sPath sp:aPaths){
            if(sp.support>=GraphData.sup*matchNum){
                System.out.println(sp.parsePath());
            }
        }
    }
}
