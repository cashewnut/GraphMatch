package com.fudanse.graphmatch.graphmining;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.internal.value.PathValue;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Values;
import org.neo4j.driver.v1.types.Path;
/**
 * Created by Administrator on 2018-01-02.
 */
public class SubGraphMining {
    public static AuthoritativePaths miningProcess(StatementResult result){
        Map<Long,List<Path>> pathMap=new HashMap<Long, List<Path>>();
        while(result.hasNext()){
            Record record=result.next();
            PathValue pathValue= (PathValue) record.get(0);
            Path p=pathValue.asPath();
            long startId=p.start().id();
            if(pathMap.get(startId)!=null){
                pathMap.get(startId).add(p);
            }
            else{
                List<Path> paths=new ArrayList<Path>();
                paths.add(p);
                pathMap.put(startId,paths);
            }

        }
        Map<Long,Integer>pathLengthMap=new HashMap<Long, Integer>();
        for(Long key:pathMap.keySet()){
            pathLengthMap.put(key,pathMap.get(key).size());
        }
        List<Map.Entry<Long, Integer>> pathLengthList = new ArrayList<Map.Entry<Long, Integer>>(pathLengthMap.entrySet());
        Collections.sort(pathLengthList, new Comparator<Map.Entry<Long, Integer>>() {
            //升序排序
            public int compare(Map.Entry<Long, Integer> o1, Map.Entry<Long, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
                //return o2.getValue().compareTo(o1.getValue());
            }
        });
        Long auKey=pathLengthList.get((int)Math.ceil(pathMap.size()*GraphData.sup)-1).getKey();
        List<sPath> spList=new ArrayList<sPath>();
        List<Path> pList=pathMap.get(auKey);
        for(int aIndex=0;aIndex<pList.size();aIndex++){
            sPath supportPath=new sPath(1,pList.get(aIndex));
            spList.add(supportPath);
        }
        AuthoritativePaths auPaths=new AuthoritativePaths(spList,auKey,pathMap.size());

        for(sPath sp:auPaths.aPaths){
            String pStr=sp.parsePath();
            for(long key:pathMap.keySet()){
                String str="match p=(a)-[*0..]->"+pStr+" where id(a)={id} return p";
                if(key!=auPaths.aKey){
                    StatementResult res=GraphData.tx.run(str,Values.parameters("id",key));
                    if(res.hasNext()==true){
                        sp.support++;
                    }
                }
            }
        }
        return auPaths;
    }
}
