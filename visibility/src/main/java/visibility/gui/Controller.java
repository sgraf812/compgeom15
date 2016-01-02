package visibility.gui;

import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.FillRule;
import javafx.stage.FileChooser;
import visibility.types.GeometryParser;
import visibility.types.Polygon;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Controller {
    public Canvas canvas;
    private Viewport viewport;
    private GeometryParser parser;
    private Iterable<Polygon> geometry;
    private Point2D pacman;
    private final List<Point2D> ghosts = new ArrayList<>();

    public void initialize(GeometryParser parser) {
        this.parser = parser;
    }

    private Viewport getViewport() {
        if (viewport == null && canvas != null) {
            Bounds b = canvas.getBoundsInLocal();
            setViewport(new Viewport(new Rectangle2D(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight())));
        }
        return viewport;
    }

    private void setViewport(Viewport viewport) {
        this.viewport = viewport;
        draw(canvas.getGraphicsContext2D());
    }

    public void loadOSMData(ActionEvent actionEvent) {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose an OSM file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("OSM Files", "*.xml", "*.osm"));
        File selectedFile = chooser.showOpenDialog(canvas.getScene().getWindow());
        if (selectedFile != null) {
            geometry = parser.parseFile(selectedFile.getAbsolutePath());
        }

        setViewport(Viewport.fromPolygons(geometry));
        draw(canvas.getGraphicsContext2D());
    }

    public void clear(ActionEvent actionEvent) {
        pacman = null;
        ghosts.clear();
        draw(canvas.getGraphicsContext2D());
    }

    public void addGhostOrPacman(MouseEvent event) {
        Point2D p = getViewport().screenToViewport(canvas.getBoundsInLocal(), event.getX(), event.getY());
        switch (event.getButton()) {
            case PRIMARY:
                // Place Pacman
                pacman = p;
                break;
            case SECONDARY:
                // Place an additional ghost
                ghosts.add(p);
                break;
            default:
                return;
        }
        draw(canvas.getGraphicsContext2D());
    }

    private void draw(GraphicsContext gc) {
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());


        gc.setFill(Color.CORNFLOWERBLUE);

        for (Polygon poly : geometry) {
            // first we draw the outer face, then we subtract the holes
            drawFace(gc, poly.getOuterFace());

            // This will subtract the holes
            gc.setFillRule(FillRule.EVEN_ODD);

            for (List<Point2D> hole : poly.getHoles()) {
                drawFace(gc, hole);
            }
        }

        drawPacman(gc);
        drawGhosts(gc);
    }

    private void drawGhosts(GraphicsContext gc) {
        gc.setFill(Color.GREEN);
        for (Point2D ghost : ghosts) {
            Point2D p = viewport.viewportToScreen(canvas.getBoundsInLocal(), ghost);
            gc.fillOval(p.getX() - 10, p.getY() - 10, 20, 20);
        }
    }

    private void drawPacman(GraphicsContext gc) {
        if (pacman != null) {
            gc.setFill(Color.YELLOW);
            Point2D p = viewport.viewportToScreen(canvas.getBoundsInLocal(), pacman);
            gc.fillArc(p.getX() - 10, p.getY() - 10, 20, 20, -45, 270, ArcType.ROUND);
        }
    }

    private void drawFace(GraphicsContext gc, List<Point2D> f) {
        gc.beginPath();
        for (int i = 0; i < f.size(); ++i) {
            Point2D p = viewport.viewportToScreen(canvas.getBoundsInLocal(), f.get(i));
            if (i == 0) {
                gc.moveTo(p.getX(), p.getY());
            } else {
                gc.lineTo(p.getX(), p.getY());
            }
        }
        gc.closePath();
        gc.fill();
    }


}
