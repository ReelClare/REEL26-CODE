
import gralog.maxflow.functions.MaxFlowFunctions;

import gralog.algorithm.*;
import gralog.progresshandler.ProgressHandler; // Note: onProgress(c) required to redraw graph.
import gralog.structure.*;

import java.security.AlgorithmParameters;
import java.util.*;

@AlgorithmDescription( name = "Maximum Flow", 
    text = "Finds the maximum flow between 2 selected vertices using the Ford–Fulkerson algorithm.",
    url = "https://en.wikipedia.org/wiki/Ford–Fulkerson_algorithm" ) 
public class MaxFlow extends Algorithm { 

    public static Structure alterGraph(Structure c){ 
        Map<Edge, Edge> reverse = MaxFlowFunctions.residualGraph(c); 
        return c; 
    } 

    public static double fulkersonMaxFlow( Structure c, ArrayList<Vertex> sources,
                                        ArrayList<Vertex> sinks, Map<Edge, Edge> reverse ) { 
                                            
        double maxFlow = 0; 
        HashMap<Vertex, Vertex> parentVertex = new HashMap<>(); 
        HashMap<Vertex, Edge> parentEdge = new HashMap<>(); 
        
        while (true) { // infinite loop to ensure algorithm is completed, 
                       //exit conditions signified by "break"s 
        
            parentVertex.clear(); 
            parentEdge.clear();

            Vertex sink = MaxFlowFunctions.bfs(c, sources, sinks, reverse, parentEdge, parentVertex); 
            if (sink == null) break;

            ArrayList<Vertex> path = new ArrayList<>();
            for (Vertex v = sink; v != null; v = parentVertex.get(v)) {
                path.add(0, v);
            }
        
            if (path == null) 
                break; 

            double pathFlow = Double.MAX_VALUE; 
        
            for (int i = 1; i < path.size(); i++) { 
                Edge e = parentEdge.get(path.get(i)); 
                pathFlow = Math.min(pathFlow, e.weight); } 
            
            if (pathFlow <= 0) 
                break; 
            
            for (int i = 1; i < path.size(); i++) { 
                Edge e = parentEdge.get(path.get(i)); 
                Edge rev = reverse.get(e); e.weight -= pathFlow; 
                rev.weight += pathFlow; 
            } 
            maxFlow += pathFlow; 
            MaxFlowFunctions.highlightMinCutPartition(c, sources);
        } return maxFlow;
    } 

        public Object run(Structure c, AlgorithmParameters p, Set<Object> selection, 
                        ProgressHandler onprogress) throws Exception { 
            
            if (c.getVertices().size() == 0) 
                return ("The structure should not be empty."); 
            
            for (Edge e : (Set<Edge>) c.getEdges()) { 
                if (e.weight < 0d) 
                    return ("The Ford-Fulkerson Algorithm requires non-negative edge weights");
                if (!e.isDirected)
                    return ("The Ford-Fulkerson Algorithm requires that all edges are directed"); } 


            ArrayList<Vertex> s = MaxFlowFunctions.findSources(c); 
            ArrayList<Vertex> t = MaxFlowFunctions.findSinks(c); 
            if (s == null)
            return("Please label all sources with 'S'. Ensure you have no incoming edges to your sources, and no outgoing edges from your sinks."); 
        if (t == null)
            return("Please, label all sinks with 'T'. Ensure you have no incoming edges to your sources, and no outgoing edges from your sinks."); 
            
            Map<Edge, Edge> reverse = MaxFlowFunctions.residualGraph(c);
            double maxFlow = fulkersonMaxFlow(c, s, t, reverse); 
            onprogress.onProgress(c); // redrawing graph with residual edges and partition
            return "Maximum Flow = " + maxFlow; 
    } 
}