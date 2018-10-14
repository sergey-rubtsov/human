/*************************************************************************
 *                                                                       *
 * Open Dynamics Engine, Copyright (C) 2001,2002 Russell L. Smith.       *
 * All rights reserved.  Email: russ@q12.org   Web: www.q12.org          *
 * Open Dynamics Engine 4J, Copyright (C) 2009-2014 Tilmann Zaeschke     *
 * All rights reserved.  Email: ode4j@gmx.de   Web: www.ode4j.org        *
 *                                                                       *
 * This library is free software; you can redistribute it and/or         *
 * modify it under the terms of EITHER:                                  *
 *   (1) The GNU Lesser General Public License as published by the Free  *
 *       Software Foundation; either version 2.1 of the License, or (at  *
 *       your option) any later version. The text of the GNU Lesser      *
 *       General Public License is included with this library in the     *
 *       file LICENSE.TXT.                                               *
 *   (2) The BSD-style license that is included with this library in     *
 *       the file ODE-LICENSE-BSD.TXT and ODE4J-LICENSE-BSD.TXT.         *
 *                                                                       *
 * This library is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the files    *
 * LICENSE.TXT, ODE-LICENSE-BSD.TXT and ODE4J-LICENSE-BSD.TXT for more   *
 * details.                                                              *
 *                                                                       *
 *************************************************************************/
package deep.learning.human.internal;

import org.ode4j.math.DMatrix3C;
import org.ode4j.math.DVector3C;

import static org.ode4j.ode.internal.cpp4j.Cstdio.printf;

/**
 * DrawStuff
 * <p>
 * DrawStuff is a library for rendering simple 3D objects in a virtual
 * environment, for the purposes of demonstrating the features of ODE.
 * It is provided for demonstration purposes and is not intended for
 * production use.
 *
 * <p>Notes:
 * In the virtual world, the z axis is "up" and z=0 is the floor. <br>
 * <br>
 * The user is able to click+drag in the main window to move the camera: <br>
 * - left button - pan and tilt. <br>
 * - right button - forward and sideways. <br>
 * - left + right button (or middle button) - sideways and up. <br>
 */
public class DrawStuff {

    private static DrawStuffApi DS;

    private static DrawStuffApi get() {
        if (DS == null) {
            //default;
            DS = new DrawStuffGL();
            //DS = new DrawStuffNull();
        }
        return DS;
    }

    /**
     * Use lwjgl for output.
     */
    public static void dsSetOutputGL() {
        DS = new DrawStuffGL();
    }

    /**
     * Use no output. This can be useful for benchmarking.
     */
    public static void dsSetOutputNull() {
        DS = new DrawStuffNull();
    }

    // Sourceforge tests require the textures in the drawstuff folder
    public static String DRAWSTUFF_TEXTURE_PATH = "/textures";

    /* high byte is major version, low byte is minor version */
    public static int DS_VERSION = 0x0002;

    /* texture numbers */
    public static enum DS_TEXTURE_NUMBER {
        DS_NONE, // = 0,       /* uses the current color instead of a texture */
        DS_WOOD,
        DS_CHECKERED,
        DS_GROUND,
        DS_SKY;
    }

    /* draw modes */

    //	#define DS_POLYFILL  0
    public static final int DS_POLYFILL = 0;
    //#define DS_WIREFRAME 1
    public static final int DS_WIREFRAME = 1;

    /**
     * Set of functions to be used as callbacks by the simulation loop.
     */
    public abstract static class dsFunctions {
        public final int version = DS_VERSION;            /* put DS_VERSION here */

        /* version 1 data */
        public abstract void start();        /* called before sim loop starts */

        public abstract void step(boolean pause);    /* called before every frame */

        public abstract void command(char cmd);    /* called if a command key is pressed */

        public abstract void stop();        /* called after sim loop exits */

        private String path_to_textures = DRAWSTUFF_TEXTURE_PATH;

        public void dsSetPathToTextures(String drawstuff_texture_path) {
            path_to_textures = drawstuff_texture_path;
        }

        public String dsGetPathToTextures() {
            return path_to_textures;
        }

        public int dsGetVersion() {
            return version;
        }

        /**
         * Prints command line help.
         * Overload this method to print your own help, don't forget
         * to call this method via <tt>super.dsPrintHelp();</tt>.
         */
        public void dsPrintHelp() {
            System.out.println(" -h | -help              : Print this help.");
            System.out.println(" -notex                  : Do not use any textures.");
            System.out.println(" -noshadow[s]            : Do not draw any shadows.");
            System.out.println(" -pause                  : Start the simulation paused.");
            System.out.println(" -texturepath <path>     : Set an alternative path for the textures.");
            System.out.println("                           Default = " + DRAWSTUFF_TEXTURE_PATH);
        }
    }

    /**
     * Does the complete simulation.
     * <p>
     * This function starts running the simulation, and only exits when the simulation is done.
     * Function pointers should be provided for the callbacks.
     * If you filter out arguments beforehand, simply set them to "".
     * To extend the help, overload dsPrintHelp().
     *
     * @param args supports flags like '-notex' '-noshadow' '-pause'
     * @param fn   Callback functions.
     */

    public static void dsSimulationLoop(String[] args,
                                        int window_width, int window_height,
                                        dsFunctions fn) {
        get().dsSimulationLoop(args, window_width, window_height, fn);
    }

    /**
     * Exit with error message.
     * This function displays an error message then exit.
     *
     * @param msgs format strin, like printf, without the newline character.
     */

    public static void dsError(final String... msgs) { //sconst char *msg, ...);
        System.err.print("Error");
        for (String s : msgs) {
            System.out.print(" " + s);
        }
        throw new RuntimeException();
    }

    /**
     * Exit with error message and core dump.
     * this functions tries to dump core or start the debugger.
     *
     * @param msgs format strin, like printf, without the newline character.
     */

    public static void dsDebug(final String... msgs) { // (const char *msg, ...);
        System.err.print("INTERNAL ERROR");
        for (String s : msgs) {
            System.out.print(" " + s);
        }
        throw new RuntimeException();
    }

    /**
     * Print log message.
     *
     * @param msg format string, like printf, without the \n.
     */

    public static void dsPrint(String msg, Object... objs) { //(const char *msg, ...);
        printf(msg, objs);
    }

    /**
     * Sets the viewpoint.
     *
     * @param xyz camera position.
     * @param hpr contains heading, pitch and roll numbers in degrees. heading=0
     *            points along the x axis, pitch=0 is looking towards the horizon, and
     *            roll 0 is "unrotated".
     */

//	public static  void dsSetViewpoint (float xyz[3], float hpr[3]);
    public static void dsSetViewpoint(float xyz[], float hpr[]) {
        get().dsSetViewpoint(xyz, hpr);
    }

    public static void dsSetViewpoint(double xyz[], double hpr[]) {
        get().dsSetViewpoint(toFloat(xyz), toFloat(hpr));
    }

    private static float[] toFloat(double[] d) {
        float[] ret = new float[d.length];
        for (int i = 0; i < d.length; i++) {
            ret[i] = (float) d[i];
        }
        return ret;
    }

    /**
     * Gets the viewpoint.
     *
     * @param xyz position
     * @param hpr heading,pitch,roll.
     */
    public static void dsGetViewpoint(float[] xyz, float[] hpr) {
        get().dsGetViewpoint(xyz, hpr);
    }

    /**
     * Stop the simulation loop.
     * <p>
     * Calling this from within dsSimulationLoop()
     * will cause it to exit and return to the caller. it is the same as if the
     * user used the exit command. using this outside the loop will have no
     * effect.
     */
    public static void dsStop() {
        get().dsStop();
    }

    /**
     * Get the elapsed time (on wall-clock).
     * <p>
     * It returns the nr of seconds since the last call to this function.
     */
    public static double dsElapsedTime() {
        return get().dsElapsedTime();
    }

    /**
     * Toggle the rendering of textures.
     * <p>
     * It changes the way objects are drawn. these changes will apply to all further
     * dsDrawXXX() functions.
     *
     * @param texture_number The texture number must be a DS_xxx texture constant.
     *                       The current texture is colored according to the current color.
     *                       At the start of each frame, the texture is reset to none and the color is
     *                       reset to white.
     */

    public static void dsSetTexture(DS_TEXTURE_NUMBER texture_number) {
        get().dsSetTexture(texture_number);
    }

    /**
     * Set the color with which geometry is drawn.
     *
     * @param red   Red component from 0 to 1
     * @param green Green component from 0 to 1
     * @param blue  Blue component from 0 to 1
     */

    public static void dsSetColor(float red, float green, float blue) {
        get().dsSetColor(red, green, blue);
    }

    public static void dsSetColor(double red, double green, double blue) {
        get().dsSetColor((float) red, (float) green, (float) blue);
    }

    /**
     * Set the color and transparency with which geometry is drawn.
     *
     * @param alpha Note that alpha transparency is a misnomer: it is alpha opacity.
     *              1.0 means fully opaque, and 0.0 means fully transparent.
     */

    public static void dsSetColorAlpha(float red, float green, float blue, float alpha) {
        get().dsSetColorAlpha(red, green, blue, alpha);
    }

    public static void dsSetColorAlpha(double red, double green, double blue, double alpha) {
        get().dsSetColorAlpha((float) red, (float) green, (float) blue, (float) alpha);
    }

    /**
     * Draw a box.
     *
     * @param pos   is the x,y,z of the center of the object.
     * @param R     is a 3x3 rotation matrix for the object, stored by row like this:
     *              [ R11 R12 R13 0 ]
     *              [ R21 R22 R23 0 ]
     *              [ R31 R32 R33 0 ]
     * @param sides is an array of x,y,z side lengths.
     */
    public static void dsDrawBox(final float[] pos, final float[] R, final float sides[]) {
        get().dsDrawBox(pos, R, sides);
    }

    public static void dsDrawBox(DVector3C pos, DMatrix3C R, DVector3C sides) {
        get().dsDrawBox(pos, R, sides);
    }

    /**
     * Draw a sphere.
     *
     * @param pos    Position of center.
     * @param R      orientation.
     * @param radius
     */
    public static void dsDrawSphere(final float[] pos, final float[] R,
                                    float radius) {
        get().dsDrawSphere(pos, R, radius);
    }

    public static void dsDrawSphere(DVector3C pos, DMatrix3C R,
                                    double radius) {
        get().dsDrawSphere(pos, R, (float) radius);
    }

    /**
     * Draw a triangle.
     *
     * @param pos   Position of center
     * @param R     orientation
     * @param v0    first vertex
     * @param v1    second
     * @param v2    third vertex
     * @param solid set to 0 for wireframe
     */
    public static void dsDrawTriangle(final float[] pos, final float[] R,
                                      final float[] v0, final float[] v1, final float[] v2, int solid) {
        throw new UnsupportedOperationException();
    }

    public static void dsDrawTriangle(DVector3C pos, DMatrix3C R,
                                      DVector3C v0, DVector3C v1, DVector3C v2, boolean solid) {
        get().dsDrawTriangle(pos, R, v0, v1, v2, solid);
    }

    public static void dsDrawTriangle(DVector3C pos, DMatrix3C rot,
                                      float[] v, int i, int j, int k, boolean solid) {
        get().dsDrawTriangle(pos, rot, v, i, j, k, solid);
    }

    public static void dsDrawTriangle(DVector3C pos, DMatrix3C rot,
                                      float[] v0, float[] v1, float[] v2, boolean solid) {
        get().dsDrawTriangle(pos, rot, v0, v1, v2, solid);
    }

    /**
     * Draw a z-aligned cylinder.
     */
    public static void dsDrawCylinder(final float[] pos, final float[] R,
                                      float length, float radius) {
        get().dsDrawCylinder(pos, R, length, radius);
    }

    public static void dsDrawCylinder(DVector3C pos, DMatrix3C R,
                                      double length, double radius) {
        get().dsDrawCylinder(pos, R, (float) length, (float) radius);
    }


    /**
     * Draw a z-aligned capsule.
     */
    public static void dsDrawCapsule(final float[] pos, final float[] R,
                                     float length, float radius) {
        get().dsDrawCapsule(pos, R, length, radius);
    }

    public static void dsDrawCapsule(DVector3C pos, DMatrix3C R,
                                     double length, double radius) {
        get().dsDrawCapsule(pos, R, (float) length, (float) radius);
    }


    /**
     * Draw a line.
     */
    public static void dsDrawLine(final float[] pos1, final float[] pos2) {
        get().dsDrawLine(pos1, pos2);
    }

    public static void dsDrawLine(DVector3C pos1, DVector3C pos2) {
        get().dsDrawLine(pos1, pos2);
    }


    /**
     * Draw a convex shape.
     */
    public static void dsDrawConvex(final float[] pos, final float[] R,
                                    float[] _planes,
                                    int _planecount,
                                    float[] _points,
                                    int _pointcount,
                                    int[] _polygons) {
        throw new UnsupportedOperationException();
    }

    public static void dsDrawConvex(DVector3C pos, DMatrix3C R,
                                    double[] _planes,
                                    int _planecount,
                                    double[] _points,
                                    int _pointcount,
                                    int[] _polygons) {
        get().dsDrawConvex(pos, R, _planes, _planecount, _points, _pointcount, _polygons);
    }

    public static void dsDrawSphere(DVector3C pos, DMatrix3C R,
                                    final float radius) {
        get().dsDrawSphere(pos, R, radius);
    }

    /**
     * Set the quality with which curved objects are rendered.
     * <p>
     * Higher numbers are higher quality, but slower to draw.
     * This must be set before the first objects are drawn to be effective.
     * Default sphere quality is 1, default capsule quality is 3.
     */
    public static void dsSetSphereQuality(int n) {        /* default = 1 */
        get().dsSetSphereQuality(n);
    }

    public static void dsSetCapsuleQuality(int n) {        /* default = 3 */
        throw new UnsupportedOperationException();
    }

    /**
     * Set Drawmode (0=Polygon Fill,1=Wireframe).
     * Use the DS_POLYFILL and DS_WIREFRAME macros.
     */
    public static void dsSetDrawMode(int mode) {
        get().dsSetDrawMode(mode);
    }

}
