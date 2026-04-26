package gralog.maxflow.functions;


import gralog.algorithm.*;
import gralog.structure.*; 
import java.util.*;

public class MaxFlowFunctions {

    // ensure source vertices fit criteria
    public static ArrayList<Vertex> findSources(Structure c) {
        boolean foundSources = false;
        ArrayList<Vertex> sources = new ArrayList<Vertex>();

        for (Object obj : c.getVertices()) {
            Vertex v = (Vertex) obj;
            boolean hasS = (v.label.contains("S")|| v.label.contains("s"));
            boolean hasNoIncoming = v.getIncomingNeighbours().isEmpty();
            boolean hasOutgoing = !v.getOutgoingNeighbours().isEmpty();

            if (hasS && hasNoIncoming && hasOutgoing) {
                sources.add(v);
                foundSources = true;
            }
        }
        if (foundSources == true)
            return sources;
        else
            return null;
    }

    // ensure sink vertices fit criteria
    public static ArrayList<Vertex> findSinks(Structure c) {
        boolean foundSinks = false;
        ArrayList<Vertex> sinks = new ArrayList<Vertex>();

        for (Object obj : c.getVertices()) {
            Vertex v = (Vertex) obj;
            boolean hasT = (v.label.contains("T")||v.label.contains("t"));
            boolean hasNoOutgoing = v.getOutgoingNeighbours().isEmpty();
            boolean hasIncoming = !v.getIncomingNeighbours().isEmpty();
            if (hasT && hasNoOutgoing && hasIncoming) {
                sinks.add(v);
                foundSinks = true;
            }
        }
        if (foundSinks == true)
            return sinks;
        else
            return null;
    }
}