package gralog.gralogfx.panels;

import gralog.rendering.GralogColor;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class MaxFlowLegendPanel extends ScrollPane {
    
    private VBox scrollPane;
    private static final int ITEM_HEIGHT = 30;
    private static final int CANVAS_WIDTH = 60;
    private static final int VERTEX_RADIUS = 10;
    
    public MaxFlowLegendPanel() {
        scrollPane = new VBox(3); // 3 spacing
        scrollPane.setStyle("-fx-padding: 10; -fx-background-color:transparent;");
        
        Label title = new Label("Max Flow Algorithm Legend");
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        scrollPane.getChildren().add(title);
        
        scrollPane.getChildren().add(new Label("")); //spacing
        
        addVertex(GralogColor.GREEN, "Vertex in current augmenting path");
        addVertex(GralogColor.RED, "Source side of min-cut (reachable)");
        addVertex(GralogColor.BLUE, "Sink side of min-cut (unreachable)");
        addVertex(GralogColor.WHITE, "Unvisited/neutral vertex");
        
        scrollPane.getChildren().add(new Label(""));
        
        addEdge(GralogColor.BLACK, false, false, "Original edge (remaining capacity)");
        addEdge(GralogColor.BLACK, true,true, "Reverse edge (current flow)");
        addEdge(GralogColor.RED, false,false, "Min-cut edge (bottleneck)");

        this.setContent(scrollPane);
        this.setFitToWidth(true);
        this.setStyle("-fx-background-color: white;");
    }
    
    private void addVertex(GralogColor gralogColor, String description) {
        VBox item = new VBox(1);
        
        Canvas canvas = new Canvas(CANVAS_WIDTH, ITEM_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        Color colour = convertGralogColour(gralogColor);
        
        gc.setFill(colour);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);
        double centerY = ITEM_HEIGHT / 2.0;
        gc.fillOval(CANVAS_WIDTH / 2.0 - VERTEX_RADIUS, centerY - VERTEX_RADIUS, 
                    VERTEX_RADIUS * 2, VERTEX_RADIUS * 2);
        gc.strokeOval(CANVAS_WIDTH / 2.0 - VERTEX_RADIUS, centerY - VERTEX_RADIUS, 
                      VERTEX_RADIUS * 2, VERTEX_RADIUS * 2);
        
        Label label = new Label(description);
        label.setFont(Font.font("System", 11));
        
        javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(10);
        hbox.getChildren().addAll(canvas, label);
        hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        scrollPane.getChildren().add(hbox);
    }
    

    private void addEdge(GralogColor gralogColor, boolean dashed, boolean backwardsArrow, String description) {
        Canvas canvas = new Canvas(CANVAS_WIDTH, ITEM_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        Color colour = convertGralogColour(gralogColor);
        
        gc.setStroke(colour);
        gc.setLineWidth(2);
        
        double y = ITEM_HEIGHT / 2.0;
        double startX = 5;
        double endX = CANVAS_WIDTH - 5;
        
        if (dashed) {
            gc.setLineDashes(5, 5);
        } else {
            gc.setLineDashes(null);
        }
        
        gc.strokeLine(startX, y, endX, y);
        
  
        double arrowSize = 6;
        gc.setLineDashes(null);  // solid for arrow
        
        if (backwardsArrow) {
            // Arrow pointing LEFT (at start of line)
            gc.strokeLine(startX, y, startX + arrowSize, y - arrowSize / 2);
            gc.strokeLine(startX, y, startX + arrowSize, y + arrowSize / 2);
        } else {
            // Arrow pointing RIGHT (at end of line)
            gc.strokeLine(endX, y, endX - arrowSize, y - arrowSize / 2);
            gc.strokeLine(endX, y, endX - arrowSize, y + arrowSize / 2);
        }
        
        Label label = new Label(description);
        label.setFont(Font.font("System", 11));
        
        javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(10);
        hbox.getChildren().addAll(canvas, label);
        hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        
        scrollPane.getChildren().add(hbox);
    }

    
    // needed to convert GralogColor to JavaFX Color using r, g, b fields, ensure
    // that colours are consistent between graph and legend
    private Color convertGralogColour(GralogColor gralogColor) {
        return Color.rgb(
            gralogColor.r & 0xFF,
            gralogColor.g & 0xFF,
            gralogColor.b & 0xFF
        );
    }
}