package visibility.gui;

import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.stage.FileChooser;
import org.jooq.lambda.Seq;
import org.jooq.lambda.tuple.Tuple2;
import visibility.algorithm.NaiveIntersection;
import visibility.types.GeometryParser;
import visibility.types.Segment;
import visibility.types.SpatialDataStructure;
import visibility.types.Triangle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.jooq.lambda.tuple.Tuple.tuple;

public class Controller {
    public Canvas canvas;
    private Viewport viewport;
    private GeometryParser parser;
    private List<Triangle> geometry;
    private SpatialDataStructure dataStructure;
    private Point2D pacman;
    private final List<Point2D> ghosts = new ArrayList<>();
    private final List<Tuple2<Segment, Color>> rays = new ArrayList<>();
    private WritableImage snapshot;

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
    }

    public void loadOSMData(ActionEvent actionEvent) {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose an OSM file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("OSM Files", "*.xml", "*.osm"));
        File selectedFile = chooser.showOpenDialog(canvas.getScene().getWindow());
        if (selectedFile != null) {
            geometry = parser.parseFile(selectedFile.getAbsolutePath());
            dataStructure = NaiveIntersection.fromTriangles(geometry);
        }

        snapshot = null;
        setViewport(Viewport.fromTriangles(geometry));
        draw(canvas.getGraphicsContext2D());
    }

    public void clear(ActionEvent actionEvent) {
        pacman = null;
        ghosts.clear();
        rays.clear();
        draw(canvas.getGraphicsContext2D());
    }

    public void addGhostOrPacman(MouseEvent event) {
        if (canvas.getWidth() == 0 || canvas.getHeight() == 0) {
            return;
        }

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

        // Now recalculate all visibility checks
        rays.clear();
        if (pacman != null) {
            rays.addAll(Seq.seq(ghosts).map(g -> {
                Segment ray = new Segment(g, pacman);
                Point2D intersection = dataStructure.intersectWith(new Segment(g, pacman));
                return intersection == null
                        ? tuple(ray, Color.GREEN)
                        : tuple(new Segment(g, intersection), Color.RED);
            }).toList());
        }

        draw(canvas.getGraphicsContext2D());
    }

    private void draw(GraphicsContext gc) {
        if (snapshot == null) {
            gc.setFill(Color.LIGHTGRAY);
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());


            gc.setFill(Color.CORNFLOWERBLUE);

            geometry.forEach(t -> drawTriangle(gc, t));

            snapshot = canvas.snapshot(null, null);
        } else {
            gc.drawImage(snapshot, 0, 0);
        }

        drawRays(gc);
        drawPacman(gc);
        drawGhosts(gc);
    }

    private void drawRays(GraphicsContext gc) {
        for (Tuple2<Segment, Color> ray : rays) {
            Point2D start = viewport.viewportToScreen(canvas.getBoundsInLocal(), ray.v1.getStart());
            Point2D end = viewport.viewportToScreen(canvas.getBoundsInLocal(), ray.v1.getEnd());

            gc.setStroke(ray.v2);
            gc.strokeLine(start.getX(), start.getY(), end.getX(), end.getY());
        }
    }

    private void drawGhosts(GraphicsContext gc) {
        Point2D focus = pacman == null ? null : viewport.viewportToScreen(canvas.getBoundsInLocal(), pacman);
        for (Point2D ghost : ghosts) {
            Point2D p = viewport.viewportToScreen(canvas.getBoundsInLocal(), ghost);
            gc.setFill(Color.PURPLE);
            gc.fillOval(p.getX() - 10, p.getY() - 10, 20, 20);
            gc.setFill(Color.WHITE);
            gc.fillOval(p.getX() - 8, p.getY() - 5, 6, 6);
            gc.fillOval(p.getX() + 2, p.getY() - 5, 6, 6);

            gc.setFill(Color.BLACK);
            Point2D rightEye = p.add(-5, -2);
            Point2D leftEye = p.add(5, -2);
            if (focus != null) {
                // This will make the ghosts leer into pacman's direction
                rightEye = rightEye.add(focus.subtract(rightEye).normalize());
                leftEye = leftEye.add(focus.subtract(leftEye).normalize());
            }
            gc.fillOval(leftEye.getX()-1, leftEye.getY()-1, 2, 2);
            gc.fillOval(rightEye.getX()-1, rightEye.getY()-1, 2, 2);
        }
    }

    private void drawPacman(GraphicsContext gc) {
        if (pacman != null) {
            gc.setFill(Color.YELLOW);
            Point2D p = viewport.viewportToScreen(canvas.getBoundsInLocal(), pacman);
            gc.fillArc(p.getX() - 10, p.getY() - 10, 20, 20, 30, 300, ArcType.ROUND);
            gc.setFill(Color.WHITE);
            gc.fillOval(p.getX() - 5, p.getY() - 8, 6, 6);
            gc.setFill(Color.BLACK);
            gc.fillOval(p.getX() - 2, p.getY() - 6, 2, 2);
        }
    }

    private void drawTriangle(GraphicsContext gc, Triangle t) {
        gc.beginPath();
        for (int i = 0; i < 3; ++i) {
            Point2D p = viewport.viewportToScreen(canvas.getBoundsInLocal(), t.get(i));
            if (i == 0) {
                gc.moveTo(p.getX(), p.getY());
            } else {
                gc.lineTo(p.getX(), p.getY());
            }
        }
        gc.closePath();
        gc.fill();
    }


    public void movePacman(MouseEvent event) {
        if (event.isPrimaryButtonDown()) {
            addGhostOrPacman(event);
        }
    }
}
