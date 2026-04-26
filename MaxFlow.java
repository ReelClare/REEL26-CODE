
import gralog.maxflow.functions.MaxFlowFunctions;

import gralog.algorithm.*;
import gralog.progresshandler.ProgressHandler;
import gralog.structure.*;

import java.security.AlgorithmParameters;
import java.util.*;


@AlgorithmDescription(name = "Maximum Flow", text = "Finds the maximum flow between 2 selected vertices using the Ford–Fulkerson algorithm.", url = "https://en.wikipedia.org/wiki/Ford–Fulkerson_algorithm")
public class MaxFlow extends Algorithm {


    // GrALoG convention: algorithm written separate from "run"
    public static double fulkersonMaxFlow() {

        return 0.0;
    }

    // correctness checks, ensure that the graph drawn by the user is valid.
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
        if (s == null){ 
            //("Please, label all sources with 'S' and sinks with 'T': then the maximum flow from your source to your sink vertices will be computed. Ensure you have no incoming edges to your sources, and no outgoing edges from your sinks."); 
            return("Please label all sources with 'S'. Ensure you have no incoming edges to your sources, and no outgoing edges from your sinks."); 
        } 
        if (t == null){ 
            //("Please, label all sources with 'S' and sinks with 'T': then the maximum flow from your source to your sink vertices will be computed. Ensure you have no incoming edges to your sources, and no outgoing edges from your sinks."); 
            return("Please, label all sinks with 'T'. Ensure you have no incoming edges to your sources, and no outgoing edges from your sinks."); 
        } 
        return "All we got, cheif"; 
}
}