package org.ode4j.math;

import org.junit.Test;

import static org.junit.Assert.*;

public class DQuaternionTest {

    @Test
    public void rotate() {
        double w = 0.7071;
        double x = 0.7071;
        double y = 0;
        double z = 0;
        //rotation around the x-axis by 90 degrees clockwise
        DQuaternionC testObj = new DQuaternion(w, x, y, z);
        DVector3 input = new DVector3(0.0, 0.0, 1.0);
        DVector3 result = testObj.rotate(input);
        assertEquals(0.0, result.get0(), 0.0001);
        assertEquals(-1.0, result.get1(), 0.0001);
        assertEquals(0.0, result.get2(), 0.0001);
    }

    @Test
    public void quaternionFromTwoVectors() {
        DVector3 v = new DVector3(0.0, -1.0, 0.0);
        DVector3 u = new DVector3(0.0, 0.0, 1.0);
        DQuaternionC testObj = new DQuaternion(u, v);
        assertEquals(0.7071, testObj.get0(), 0.0001);
        assertEquals(0.7071, testObj.get1(), 0.0001);
        assertEquals(0.0, testObj.get2(), 0.0001);
        assertEquals(0.0, testObj.get3(), 0.0001);
    }

}