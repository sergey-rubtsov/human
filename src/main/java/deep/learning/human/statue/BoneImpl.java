package deep.learning.human.statue;

import deep.learning.human.BoneType;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.internal.DxConvex;

import deep.learning.human.HumanBone;
import deep.learning.human.utils.Utils;

public class BoneImpl extends DxConvex implements HumanBone {

    private final String name;

    private final double length;

    private final double radius;

    private final static int FACES = 8;

    private final static int VERTEXES = 6;

    private double[] points;

    private int[] polygons;

    private double[] planes;

    BoneImpl(DSpace space,
             double[] planes,
             int planecount,
             double[] points,
             int pointcount,
             int[] polygons,
             String name,
             double length,
             double radius) {
        super(space, planes, planecount, points, pointcount, polygons);
        this.name = name;
        this.radius = radius;
        this.length = length;
        this.points = points;
        this.polygons = polygons;
        this.planes = planes;
    }

    public static BoneImpl createBone(DSpace space,
                                      double[] planes,
                                      int planecount,
                                      double[] points,
                                      int pointcount,
                                      int[] polygons,
                                      String name,
                                      double length,
                                      double radius) {
        return new BoneImpl(space, planes, planecount, points, pointcount, polygons, name, length, radius);
    }

    public static BoneImpl createBone(DSpace space,
                                      String name,
                                      double length,
                                      double radius) {
        double[] points = points(length, radius);
        int[] polygons = polygons();
        return new BoneImpl(space, Utils.calculatePlanes(points, polygons, FACES), FACES, points, VERTEXES, polygons, name, length, radius);
    }

    private static double[] points(double length, double radius) {
        return new double[] {
                -radius, -radius, 0, //0
                radius, -radius, 0, //1
                radius, radius, 0,  //2
                -radius, radius, 0, //3
                0, 0, -length, //4
                0, 0, length //5
        };
    }

    private static int[] polygons() {
        return new int[] {
                3, 1, 5, 0,
                3, 2, 5, 1,
                3, 3, 5, 2,
                3, 0, 5, 3,
                3, 4, 3, 2,
                3, 4, 2, 1,
                3, 4, 1, 0,
                3, 4, 0, 3
        };
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public BoneType getType() {
        return BoneType.LONG;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public double getLength() {
        return length;
    }

    @Override
    public double[] getPoints() {
        return points;
    }

    public int[] getPolygons() {
        return polygons;
    }

    public int getPlanesNumber() {
        return FACES;
    }

    public int getPointsNumber() {
        return VERTEXES;
    }

    public double[] getPlanes() {
        return planes;
    }
}
