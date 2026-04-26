
import gralog.maxflow.functions.MaxFlowFunctions;

import gralog.algorithm.*;
import gralog.progresshandler.ProgressHandler; // Note: onProgress(c) required to redraw graph.
import gralog.structure.*;

import java.security.AlgorithmParameters;
import java.util.*;

import gralog.gralogfx.piping.Piping;
import gralog.gralogfx.piping.Piping.MessageToConsoleFlag;

import gralog.rendering.GralogGraphicsContext;

import gralog.rendering.GralogColor;
import gralog.gralogfx.panels.PluginControlPanel;
import javafx.application.Platform;

@AlgorithmDescription(name = "Maximum Flow", text = "Finds the maximum flow between 2 selected vertices using the Ford–Fulkerson algorithm.", url = "https://en.wikipedia.org/wiki/Ford–Fulkerson_algorithm")

public class MaxFlow extends Algorithm {

    private volatile boolean playing = false;
    private volatile boolean stopped = false;
    private volatile boolean skipToEnd = false;
    private volatile boolean frontStepped = false;
    private volatile boolean backStepped = false;
    private final Object pauseLock = new Object();

    private HashMap<Edge, Double> originalWeights = null;
    private Map<Edge, Edge> reverseEdgeMap = null;
    double maxFlow;

    private static class AlgorithmState {
        HashMap<Edge, Double> edgeWeights; // all edge weights at this point
        double maxFlow;
        ArrayList<Vertex> lastPath; // path found in this iteration (for highlighting)

        AlgorithmState(Structure c, double maxFlow, ArrayList<Vertex> path) {
            this.maxFlow = maxFlow;
            this.lastPath = path != null ? new ArrayList<>(path) : null;

            // snapshot all edge weights
            this.edgeWeights = new HashMap<Edge, Double>();
            for (Edge e : (Set<Edge>) c.getEdges()) {
                this.edgeWeights.put(e, e.weight);
            }
        }
    }

    private ArrayList<AlgorithmState> stateHistory = new ArrayList<>();
    private int currentStateIndex = -1; // Index in stateHistory we're currently at

    // fulkersonMaxFlow function removed due to the algorithm behaving differently
    // for each button

    @SuppressWarnings("unchecked")
    public Object run(Structure c, AlgorithmParameters p, Set<Object> selection,
            ProgressHandler onprogress) throws Exception {

        // lines till while (!stopped) ensuring algorithm can perform on generated graph
        // while giving clear instructions of how to rectify issues
        if (c.getVertices().size() == 0)
            return "The structure should not be empty.";

        for (Edge e : (Set<Edge>) c.getEdges()) {
            if (e.weight < 0d)
                return "The Ford-Fulkerson Algorithm requires non-negative edge weights";
            if (!e.isDirected)
                return ("The Ford-Fulkerson Algorithm requires that all edges are directed");
        }

        ArrayList<Vertex> sources = MaxFlowFunctions.findSources(c);
        ArrayList<Vertex> sinks = MaxFlowFunctions.findSinks(c);
        if (sources == null)
            return ("Please label all sources with 'S'. Ensure you have no incoming edges to your sources, and no outgoing edges from your sinks.");
        if (sinks == null)
            return ("Please, label all sinks with 'T'. Ensure you have no incoming edges to your sources, and no outgoing edges from your sinks.");

        synchronized (pauseLock) {
            playing = false;
            stopped = false;
            skipToEnd = false;
            frontStepped = false;
            backStepped = false;
        }

        // user's graph saved so as to allow it to be recovered when aborting algorithm
        originalWeights = new HashMap<Edge, Double>();
        for (Edge e : (Set<Edge>) c.getEdges()) {
            originalWeights.put(e, e.weight);
        }

        // residual graph generated immediately after algorithm selected
        Map<Edge, Edge> reverse = MaxFlowFunctions.residualGraph(c);
        reverseEdgeMap = reverse;

        double maxFlow = 0;

        // show initial state of algorithm, and communicate with the user how to
        // progress

        onprogress.onProgress(c); // redraw graph to show updates
        waitForStep(); // indefinite wait until user interacts with the algorithm
        if (stopped) { // ISSUE stopping algorithm restores the original graph but only before
                       // termination
            restoreGraph(c, onprogress);
            onprogress.onProgress(c); // restore original graph visually
            return "Original graph restored, select desired algorithm from dropdown menu above.";
        }

        while (!stopped) {

            if (backStepped) {

                if (currentStateIndex <= 0) {
                    // Can't go back further - already at initial state
                    backStepped = false;
                    onprogress.onProgress(c);
                    waitForStep();
                    continue;
                }

                backStepped = false;
                MaxFlowFunctions.recolour(c);
                currentStateIndex--;
                AlgorithmState prevState = stateHistory.get(currentStateIndex);

                // restoring graph state from snapshot
                for (Map.Entry<gralog.structure.Edge, Double> entry : prevState.edgeWeights.entrySet()) {
                    entry.getKey().weight = entry.getValue();
                }

                // restoring algorithm variables
                maxFlow = prevState.maxFlow;
                ArrayList<Vertex> path = prevState.lastPath;

                // MaxFlowFunctions.updateFlowAlongPath(path, pathFlow, parentEdge, reverse);
                // highlight the path from this iteration
                if (prevState.lastPath != null) {
                    for (gralog.structure.Vertex v : prevState.lastPath) {
                        v.fillColor = GralogColor.GREEN; // Green highlighting
                    }
                    for (int i = 0; i < prevState.lastPath.size() - 1; i++) {
                        Vertex from = prevState.lastPath.get(i);
                        Vertex to = prevState.lastPath.get(i + 1);
                    }
                }
                
                playing = false;
                onprogress.onProgress(c);
                waitForStep();

                MaxFlowFunctions.recolour(c);
                onprogress.onProgress(c);

                if (stopped) {
                    restoreGraph(c, onprogress);
                    onprogress.onProgress(c);
                    return "Original graph restored, select desired algorithm from dropdown menu above.";
                }
                continue;
            }

            if (frontStepped) {
                HashMap<Vertex, Vertex> parentVertex = new HashMap<Vertex, Vertex>();
                HashMap<Vertex, Edge> parentEdge = new HashMap<Vertex, Edge>();
                Vertex sink = MaxFlowFunctions.bfs(c, sources, sinks, reverse, parentEdge, parentVertex);

                if (sink == null) {
                    frontStepped = false;
                    break;
                }

                ArrayList<Vertex> path = MaxFlowFunctions.buildPath(sink, parentVertex);
                if (path == null || path.isEmpty()) {
                    frontStepped = false;
                    break;
                }
                double pathFlow = MaxFlowFunctions.calculatePathFlow(path, parentEdge);
                if (pathFlow <= 0) {
                    frontStepped = false;
                    break;
                }

                MaxFlowFunctions.updateFlowAlongPath(path, pathFlow, parentEdge, reverse);
                maxFlow += pathFlow;
                

                // Save state AFTER applying flow so it can restore to this point
                // and remove any future states (if user went back then forward)
                while (stateHistory.size() > currentStateIndex + 1) {
                    stateHistory.remove(stateHistory.size() - 1);
                }
                // Aadding current state
                AlgorithmState newState = new AlgorithmState(c, maxFlow, path);
                stateHistory.add(newState);
                currentStateIndex = stateHistory.size() - 1;

                frontStepped = false;
                playing = false;
                onprogress.onProgress(c); // redraw with highlighted path
                waitForStep(); // PAUSE HERE path stays green

                MaxFlowFunctions.recolour(c);
                onprogress.onProgress(c);

                if (stopped) {
                    restoreGraph(c, onprogress);
                    onprogress.onProgress(c);
                    return "Original graph restored, select desired algorithm from dropdown menu above.";
                }
                continue; //wait for next button press
            }

            if (skipToEnd) {
                while (true) { // instantaneous algorithm completion
                    HashMap<Vertex, Vertex> pv = new HashMap<Vertex, Vertex>();
                    HashMap<Vertex, Edge> pe = new HashMap<Vertex, Edge>();
                    Vertex sink = MaxFlowFunctions.bfs(c, sources, sinks, reverse, pe, pv);
                    if (sink == null)
                        break;
                    ArrayList<Vertex> path = MaxFlowFunctions.buildPath(sink, pv);
                    if (path == null || path.isEmpty())
                        break;
                    double pathFlow = MaxFlowFunctions.calculatePathFlow(path, pe);
                    if (pathFlow <= 0)
                        break;
                    MaxFlowFunctions.updateFlowAlongPath(path, pathFlow, pe, reverse);
                    maxFlow += pathFlow;

                }
                break;
            }

            HashMap<Vertex, Vertex> parentVertex = new HashMap<Vertex, Vertex>();
            HashMap<Vertex, Edge> parentEdge = new HashMap<Vertex, Edge>();
            Vertex sink = MaxFlowFunctions.bfs(c, sources, sinks, reverse, parentEdge, parentVertex);

            if (sink == null)
                break;
            ArrayList<Vertex> path = MaxFlowFunctions.buildPath(sink, parentVertex);
            if (path == null || path.isEmpty())
                break;
            double pathFlow = MaxFlowFunctions.calculatePathFlow(path, parentEdge);
            if (pathFlow <= 0)
                break;

            waitForStep(); // after each update, unless user has pressed pause, wait 3 seconds until
                           // continuing

            MaxFlowFunctions.updateFlowAlongPath(path, pathFlow, parentEdge, reverse);
            maxFlow += pathFlow;



            // Save state after flow update (for backStep)
            while (stateHistory.size() > currentStateIndex + 1) {
                stateHistory.remove(stateHistory.size() - 1);
            }
            AlgorithmState newState = new AlgorithmState(c, maxFlow, path);
            stateHistory.add(newState);
            currentStateIndex = stateHistory.size() - 1;
            
            onprogress.onProgress(c);
            waitForStep(); // 3 seconds
            MaxFlowFunctions.recolour(c);

            if (stopped) { // ISSUE stopping algorithm restores the original graph but only before
                // termination
                restoreGraph(c, onprogress);
                onprogress.onProgress(c); // restore original graph visually
                return "Original graph restored, select desired algorithm from dropdown menu above.";
            }

        }

        MaxFlowFunctions.highlightMinCutPartition(c, sources);
        onprogress.onProgress(c);
        return "Maximum Flow = " + maxFlow;
    }

    // BUTTONS FOR INTERACTIVITY

    public void play() {
        synchronized (pauseLock) {
            playing = true;
            pauseLock.notifyAll();
        }
        Platform.runLater(PluginControlPanel::notifyPlayRequested);
    }

    public void pause() {
        synchronized (pauseLock) {
            playing = false;
            // do NOT notifyAll... let the current wait() keep blocking
        }
        Platform.runLater(PluginControlPanel::notifySpontaneousPauseRequested);
    }

    public void stop() {
        synchronized (pauseLock) {
            stopped = true;
            playing = false;
            pauseLock.notifyAll(); // unblock waitForStep so it can see stopped==true
        }
    }

    public void skip() {
        synchronized (pauseLock) {
            skipToEnd = true;
            playing = false;
            pauseLock.notifyAll();
        }
    }

    public void frontStep() {
        synchronized (pauseLock) {
            frontStepped = true;
            playing = false;
            pauseLock.notifyAll();
        }
    }

    public void backStep() {
        synchronized (pauseLock) {
            backStepped = true;
            playing = false;
            pauseLock.notifyAll();

        }
    }

    private void waitForStep() throws InterruptedException {
        synchronized (pauseLock) {
            while (true) {
                if (stopped || skipToEnd || frontStepped || backStepped)
                    return;

                if (playing) {
                    pauseLock.wait(2000); // Auto-advance: wait 2 seconds
                    if (stopped || skipToEnd || frontStepped || backStepped)
                        return;
                    if (!playing)
                        continue;
                    return; // 2 seconds, continue
                } else {
                    pauseLock.wait();
                }
            }
        }
    }

    private void restoreGraph(Structure c, ProgressHandler onprogress) {
        if (originalWeights == null)
            return;

        if (reverseEdgeMap != null) { // remove reverse edges
            for (Map.Entry<Edge, Edge> entry : reverseEdgeMap.entrySet()) {
                if (originalWeights.containsKey(entry.getKey())) {
                    c.removeEdge(entry.getValue());
                }
            }

            // restore original weights
            for (Map.Entry<Edge, Double> entry : originalWeights.entrySet()) {
                entry.getKey().weight = entry.getValue();
            }

            originalWeights = null;
            reverseEdgeMap = null;
            MaxFlowFunctions.recolour(c);

            try {
                onprogress.onProgress(c);
            } catch (Exception ignored) {
            } // redraw
        }
    }
}