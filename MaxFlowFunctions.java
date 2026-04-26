package gralog.maxflow.functions;

import gralog.algorithm.*;
import gralog.structure.*; 
import java.util.*;

import gralog.rendering.GralogGraphicsContext;
import gralog.rendering.GralogColor;


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

    //residual graph built by adding reversed edges
    public static Map<Edge, Edge> residualGraph(Structure c) {
        Map<Edge, Edge> reverse = new HashMap<>();
        List<Edge> original = new ArrayList<>(c.getEdges());

        for (Edge e : original) {
            double capacity = e.weight;
            e.weight = capacity;
            gralog.structure.Edge rev = c.addEdge(e.getTarget(), e.getSource());
            rev.weight = 0.0;
            rev.type = GralogGraphicsContext.LineType.DASHED;   //dashed for visual distinction

            reverse.put(e, rev);
            reverse.put(rev, e);
        }

        return reverse;
    }

    // breadth first search, find sinks reachable from sources
    public static Vertex bfs(Structure c, ArrayList<Vertex> S,
                            ArrayList<Vertex> T, Map<Edge, Edge> reverse,
                            HashMap<Vertex, Edge> parentEdge, HashMap<Vertex, Vertex> parentVertex) {

        LinkedList<Vertex> queue = new LinkedList<>();
        HashSet<Vertex> visited = new HashSet<>();

        for (Vertex s : S) {
            queue.add(s);
            visited.add(s);
            parentVertex.put(s, null);
            parentEdge.put(s, null);
        }

        while (queue.isEmpty() == false) {
            Vertex u = queue.poll();
            for (Edge e : u.getOutgoingEdges()) {
                Vertex v = e.getTarget();
                if (e.weight > 0 && (visited.contains(v) == false)) {
                    visited.add(v);
                    queue.add(v);
                    parentVertex.put(v, u);
                    parentEdge.put(v, e);

                    if (T.contains(v)) {
                        return v; // return the sink vertex
                    }
                }
            }
        }
        return null; // no paths left o7
    }

    // min cut partition, red and blue for least common colour blindness issues 
    public static void highlightMinCutPartition(Structure c, ArrayList<Vertex> sources) {
        Set<Vertex> reachable = new HashSet<Vertex>();
        Queue<Vertex> queue = new LinkedList<Vertex>();
        
        for (Vertex source : sources) {
            reachable.add(source);
            queue.add(source);
        }
        
        while (!queue.isEmpty()) {
            Vertex current = queue.poll();
            for (Edge e : (Set<Edge>) c.getEdges()) {
                if (e.getSource() == current && e.weight > 0) {
                    Vertex target = e.getTarget();
                    if (!reachable.contains(target)) {
                        reachable.add(target);
                        queue.add(target);
                    }
                }
            }
        }
        
        for (Vertex v : (Collection<Vertex>) c.getVertices()) {
            if (reachable.contains(v)) {
                v.fillColor = GralogColor.RED;
            } else {
                v.fillColor = GralogColor.BLUE;
            }
        }
        
        // min cut edges found and highlighted for visual indication
        for (Edge e : (Set<Edge>) c.getEdges()) {
            Vertex v = e.getSource();
            Vertex u = e.getTarget();

            if (reachable.contains(v) && !reachable.contains(u) && e.weight == 0 && e.type != GralogGraphicsContext.LineType.DASHED)
                e.color = GralogColor.RED;
        }
    }


}