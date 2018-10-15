package deep.learning.human.utils;

import org.junit.Test;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector4;

import static org.junit.Assert.assertArrayEquals;

public class UtilsTest {

    //Cube

    /*
    Counting Faces
    So to count the number of faces, you look for how many flat sides the polyhedron has.
    Looking at the shape, you see that it has a face on top, on the bottom, to the left
    and to the right, on the front and at the back. Counting each of these, you end up
    with 6 faces. So, this polyhedron has 6 faces.

    Counting Edges
    Now let's count the edges of the polyhedron. You see that it has four edges around
    the top face and four edges around the bottom face. Then it has four edges going
    around the middle of the polyhedron. This makes for 12 edges.

    Counting Vertices
    As for the vertices, you see that it has four corners on top and four corners on
    the bottom. This makes for a total of 8 vertices.

    The Relationship
    These numbers - 6 faces, 12 edges, and 8 vertices - are actually related to each other.
    This relationship is written as a math formula like this:
    F + V - E = 2
    This formula is known as Euler's formula. The F stands for faces, the V stands for
    vertices, and the E stands for edges. It tells us that if we add the number of
    faces and vertices together and then subtract the number of edges, we will get 2 as our answer.
    */
    //planes for a cube
    private static double[] planes =
            {
                    1.0, 0.0, 0.0, 0.25,
                    0.0, 1.0, 0.0, 0.25,
                    0.0, 0.0, 1.0, 0.25,
                    -1.0, 0.0, 0.0, 0.25,
                    0.0, -1.0, 0.0, 0.25,
                    0.0, 0.0, -1.0, 0.25
            };

    private static final int planecount = 6;

    //points for a cube
    private static double points[] =
            {
                    0.25f, 0.25f, 0.25f,  //  point 0
                    -0.25f, 0.25f, 0.25f, //  point 1

                    0.25f, -0.25f, 0.25f, //  point 2
                    -0.25f, -0.25f, 0.25f,//  point 3

                    0.25f, 0.25f, -0.25f, //  point 4
                    -0.25f, 0.25f, -0.25f,//  point 5

                    0.25f, -0.25f, -0.25f,//  point 6
                    -0.25f, -0.25f, -0.25f,// point 7
            };
    private static final int pointcount = 8;
    //Polygons for a cube (6 squares)
    private static int polygons[] =
            {
                    4, 0, 2, 6, 4, // positive X
                    4, 1, 0, 4, 5, // positive Y
                    4, 0, 1, 3, 2, // positive Z
                    4, 3, 1, 5, 7, // negative X
                    4, 2, 3, 7, 6, // negative Y
                    4, 5, 4, 6, 7, // negative Z
            };

    public static void main(String[] args) {
        int shift = 0;
        for (int i = 0 , j = 1, k = 1; i + shift < polygons.length; i++, j++, k++) {
            if (j == 3) {
                System.out.print(polygons[i + shift - 2]);
                System.out.print(polygons[i + shift - 1]);
                System.out.print(polygons[i + shift]);
                System.out.print(" ");
                j = 0;
                shift--;
            }
            if (k == planecount) {
                System.out.println();
                k = 0;
                shift++;
            }
        }
    }

    private double trianglePoints[] =
            {
                    1, 0, 1,
                    1, 1, 0,
                    0, 1, 1,
                    0, 0, 0
            };

    private int trianglePolygons[] =
            {
                    0, 1, 2,
                    0, 1, 3,
                    0, 2, 3,
                    1, 2, 3
            };

    private final int trianglePointcount = 4;

    private final int trianglePlanecount = 4;

    private static double[] planes() {
        return new double[]{
                1.0, 0.0, 0.0, 0.25,
                0.0, 1.0, 0.0, 0.25,
                0.0, 0.0, 1.0, 0.25,
                -1.0, 0.0, 0.0, 0.25,
                0.0, -1.0, 0.0, 0.25,
                0.0, 0.0, -1.0, 0.25
        };
    }

    private static double[] points(double length, double radius) {
        return new double[] {
                -radius, -radius, length / 2, //0
                radius, -radius, length / 2, //1
                radius, radius, length / 2,  //2
                -radius, radius, length / 2, //3
                0, 0, 0, //4
                0, 0, length //5
        };
    }

    private static int[] polygons() {
        return new int[] {
                3, 1, 5, 0, //
                3, 2, 5, 1, //
                3, 3, 5, 2, //
                3, 0, 5, 3, //
                3, 4, 3, 2,
                3, 4, 2, 1,
                3, 4, 1, 0,
                3, 4, 0, 3
        };
    }

    @Test
    public void calculateNormal() {
        DVector3 v0 = new DVector3(0.25f, 0.25f, 0.25f);
        DVector3 v1 = new DVector3(-0.25f, 0.25f, 0.25f);
        DVector3 v2 = new DVector3(0.25f, -0.25f, 0.25f);
        DVector3 normal = Utils.calculateNormal(v0, v1, v2);
        assertArrayEquals(new double[]{0, 0, 1}, normal.toDoubleArray(), 0);
    }

    @Test
    public void calculateNormalAndDistance() {
        DVector3 v0 = new DVector3(0.25f, 0.25f, 0.25f);
        DVector3 v1 = new DVector3(-0.25f, 0.25f, 0.25f);
        DVector3 v2 = new DVector3(0.25f, -0.25f, 0.25f);
        DVector4 normal = Utils.calculateNormalAndDistance(v0, v1, v2);
        assertArrayEquals(new double[]{0, 0, 1, 1}, normal.toDoubleArray(), 0);
        v0 = new DVector3(0, 0, 0);
        v1 = new DVector3(1, 1, 1);
        v2 = new DVector3(1, 0, 1);
        normal = Utils.calculateNormalAndDistance(v0, v1, v2);
        assertArrayEquals(new double[]{0.7, 0, -0.7, 1}, normal.toDoubleArray(), 0.1);
        v0 = new DVector3(1, 0, 0.5);
        v1 = new DVector3(0, 0, 1);
        v2 = new DVector3(0, 1, 0.5);
        DVector4 normal253 = Utils.calculateNormalAndDistance(v0, v1, v2);
        v0 = new DVector3(-1, 0, 0.5);
        v1 = new DVector3(0, 0, 0);
        v2 = new DVector3(1, 0, 0.5);
        DVector4 normal041 = Utils.calculateNormalAndDistance(v0, v1, v2);
        System.out.println(normal253 + " " + normal041);
    }

    @Test
    public void calculatePlanes() {
        double[] result = Utils.calculatePlanes(points, polygons, planecount);
        assertArrayEquals(planes, result, 0.1);
        result = Utils.calculatePlanes(points(5, 1), polygons(), 8);
        assertArrayEquals(planes(), result, 0.1);
    }
}
