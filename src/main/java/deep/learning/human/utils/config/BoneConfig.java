package deep.learning.human.utils.config;

import deep.learning.human.BoneType;
import org.ode4j.math.DVector3;
import org.ode4j.ode.DBody;

public class BoneConfig {

    private BoneType type;

    private double density;

    private String name;

    private DVector3 p1;

    private DVector3 p2;

    private double radius;

    private int faces;

    private int vertexes;

    private double[] points;

    private int[] polygons;

    private double[] planes;

    public BoneConfig() {
    }

    public BoneType getType() {
        return type;
    }

    public void setType(BoneType type) {
        this.type = type;
    }

    public double getDensity() {
        return density;
    }

    public void setDensity(double density) {
        this.density = density;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DVector3 getP1() {
        return p1;
    }

    public void setP1(DVector3 p1) {
        this.p1 = p1;
    }

    public DVector3 getP2() {
        return p2;
    }

    public void setP2(DVector3 p2) {
        this.p2 = p2;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public int getFaces() {
        return faces;
    }

    public void setFaces(int faces) {
        this.faces = faces;
    }

    public int getVertexes() {
        return vertexes;
    }

    public void setVertexes(int vertexes) {
        this.vertexes = vertexes;
    }

    public double[] getPoints() {
        return points;
    }

    public void setPoints(double[] points) {
        this.points = points;
    }

    public int[] getPolygons() {
        return polygons;
    }

    public void setPolygons(int[] polygons) {
        this.polygons = polygons;
    }

    public double[] getPlanes() {
        return planes;
    }

    public void setPlanes(double[] planes) {
        this.planes = planes;
    }

    public BoneConfig(String name, DVector3 p1, DVector3 p2) {
        this.name = name;
        this.p1 = p1;
        this.p2 = p2;
    }
}
