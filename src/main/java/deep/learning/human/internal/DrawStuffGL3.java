package deep.learning.human.internal;

import org.ode4j.math.DMatrix3C;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.OdeHelper;

import static org.ode4j.ode.internal.cpp4j.Cstdio.fprintf;
import static org.ode4j.ode.internal.cpp4j.Cstdio.stderr;

public class DrawStuffGL3 extends LwJGL3 implements DrawStuffApi {

    // drawing loop stuff

    // the current state:
    //    0 = uninitialized
    //    1 = dsSimulationLoop() called
    //    2 = dsDrawFrame() called
    private static int current_state = 0;

    // textures and shadows
    private static boolean use_textures = true;        // 1 if textures to be drawn
    private static boolean use_shadows = true;        // 1 if shadows to be drawn

    // current camera position and orientation
    private float[] view_xyz = new float[3];    // position x,y,z
    private float[] view_hpr = new float[3];    // heading, pitch, roll (degrees)
    private static final double DEG_TO_RAD = Math.PI / 180.0;
    private static volatile boolean _run = true;

    public void dsDrawBox(float[] pos, float[] R, float[] sides) {
        // Nothing
    }

    public void dsDrawCapsule(float[] pos, float[] R, float length, float radius) {
        // Nothing
    }

    public void dsDrawCylinder(float[] pos, float[] R, float length,
                               float radius) {
        // Nothing
    }

    public void dsDrawLine(float[] pos1, float[] pos2) {
        // Nothing
    }

    public void dsDrawSphere(float[] pos, float[] R, float radius) {
        // Nothing
    }

    public void dsSetColor(float red, float green, float blue) {
        // Nothing
    }

    public void dsSetTexture(DrawStuff.DS_TEXTURE_NUMBER texture_number) {
        // Nothing
    }

    public void dsSimulationLoop(String[] args, int window_width,
                                 int window_height, DrawStuff.dsFunctions fn) {
        if (current_state != 0) dsError("dsSimulationLoop() called more than once");
        current_state = 1;
        // look for flags that apply to us
        boolean initial_pause = false;
        for (int i = 0; i < args.length; i++) {
            //Ignore empty arguments
            if (args[i] == null || args[i].equals("")) continue;
            if (args[i].equals("-h")) {
                fn.dsPrintHelp();
                continue;
            }
            if (args[i].equals("-help")) {
                fn.dsPrintHelp();
                continue;
            }
            if (args[i].equals("-notex")) {
                use_textures = false;
                continue;
            }
            if (args[i].equals("-noshadow")) {
                use_shadows = false;
                continue;
            }
            if (args[i].equals("-noshadows")) {
                use_shadows = false;
                continue;
            }
            if (args[i].equals("-pause")) {
                initial_pause = true;
                continue;
            }
            if (args[i].equals("-texturepath"))
                if (++i < args.length) {
                    fn.dsSetPathToTextures(args[i]);
                    continue;
                }
            System.out.println("Argument not understood: \"" + args[i] + "\"");
            fn.dsPrintHelp();
            return;
        }

		initMotionModel();
        dsPlatformSimLoop(window_width, window_height, fn, initial_pause);

        current_state = 0;
    }

    private void initMotionModel() {
        view_xyz[0] = 2;
        view_xyz[1] = 0;
        view_xyz[2] = 1;
        view_hpr[0] = 180;
        view_hpr[1] = 0;
        view_hpr[2] = 0;
    }

    public float[] getView_xyz() {
        return view_xyz;
    }

    public float[] getView_hpr() {
        return view_hpr;
    }

    private static boolean firsttime = true;

    void dsPlatformSimLoop(int window_width, int window_height, DrawStuff.dsFunctions fn,
                           boolean initial_pause) {

        //TZ static bool firsttime=true;
        if (firsttime) {
            System.err.println();
            System.err.print("Using ode4j version: " + OdeHelper.getVersion());
            System.err.println("  [" + OdeHelper.getConfiguration() + "]");
            System.err.println();
            fprintf
                    (
                            stderr,
                            "\n" +
                                    "Simulation test environment v%d.%02d\n" +
                                    "   Ctrl-P : pause / unpause (or say `-pause' on command line).\n" +
                                    "   Ctrl-O : single step when paused.\n" +
                                    "   Ctrl-T : toggle textures (or say `-notex' on command line).\n" +
                                    "   Ctrl-S : toggle shadows (or say `-noshadow' on command line).\n" +
                                    "   Ctrl-V : print current viewpoint coordinates (x,y,z,h,p,r).\n" +
                                    "   Ctrl-W : write frames to ppm files: frame/frameNNN.ppm\n" +
                                    "   Ctrl-X : exit.\n" +
                                    "\n" +
                                    "Change the camera position by clicking + dragging in the window.\n" +
                                    "   Left button - pan and tilt.\n" +
                                    "   Right button - forward and sideways.\n" +
                                    "   Left + Right button (or middle button) - sideways and up.\n" +
                                    "\n", DrawStuff.DS_VERSION >> 8, DrawStuff.DS_VERSION & 0xff
                    );
            firsttime = false;
        }

        //if (fn.start)
        fn.start();

        long startTime = System.currentTimeMillis() + 5000;
        long fps = 0;
//		int loops = 0;
        _run = true;
        while (_run) {

            fn.step(false);

            if (startTime > System.currentTimeMillis()) {
                fps++;
            } else {
                long timeUsed = 5000 + (startTime - System.currentTimeMillis());
                startTime = System.currentTimeMillis() + 5000;
                System.out.println(fps + "  frames in " + (timeUsed / 1000f) + "  seconds = "
                        + (fps / (timeUsed / 1000f)));
                fps = 0;
            }

        }

        //if (fn.stop)
        fn.stop();
    }

    @Override
    void dsStartGraphics(int width, int height, DrawStuff.dsFunctions fn) {

    }

    @Override
    void dsDrawFrame(int width, int height, DrawStuff.dsFunctions fn, boolean pause) {
        if (current_state < 1) dsDebug("internal error");
        current_state = 2;

        // snapshot camera position (in MS Windows it is changed by the GUI thread)
        float[] view2_xyz = view_xyz.clone();
        float[] view2_hpr = view_hpr.clone();

        setCamera(view2_xyz[0], view2_xyz[1], view2_xyz[2],
                view2_hpr[0], view2_hpr[1], view2_hpr[2]);

        drawScene(view2_xyz);

        fn.step(pause);
    }

    private void drawScene(float[] view_xyz) {}

    private void setCamera(float x, float y, float z, float h, float p, float r) {
    }

        @Override
    void dsStopGraphics() {

    }

    /**
     * Call this to update the current camera position. the bits in `mode' say
     * if the left (1), middle (2) or right (4) mouse button is pressed, and
     * (deltax,deltay) is the amount by which the mouse pointer has moved.
     */
    @Override
    void dsMotion(int mode, int deltax, int deltay) {
        float side = 0.01f * deltax;
        float fwd = (mode == 4) ? (0.01f * deltay) : 0.0f;
        float s = (float) Math.sin(view_hpr[0] * DEG_TO_RAD);
        float c = (float) Math.cos(view_hpr[0] * DEG_TO_RAD);

        if (mode == 1) {
            view_hpr[0] += deltax * 0.5f;
            view_hpr[1] += deltay * 0.5f;
        } else {
            view_xyz[0] += -s * side + c * fwd;
            view_xyz[1] += c * side + s * fwd;
            if (mode == 2 || mode == 5) view_xyz[2] += 0.01f * deltay;
        }
        wrapCameraAngles();
    }

    private void wrapCameraAngles() {
        for (int i = 0; i < 3; i++) {
            while (view_hpr[i] > 180) view_hpr[i] -= 360;
            while (view_hpr[i] < -180) view_hpr[i] += 360;
        }
    }

    @Override
    boolean dsGetShadows() {
        return false;
    }

    @Override
    void dsSetShadows(boolean a) {

    }

    @Override
    boolean dsGetTextures() {
        return false;
    }

    @Override
    void dsSetTextures(boolean a) {

    }


    public double dsElapsedTime() {
        // Nothing
        return 0;
    }


    public void dsGetViewpoint(float[] xyz, float[] hpr) {
        if (current_state < 1) dsError("dsGetViewpoint() called before simulation started");
        if (xyz != null) {
            xyz[0] = view_xyz[0];
            xyz[1] = view_xyz[1];
            xyz[2] = view_xyz[2];
        }
        if (hpr != null) {
            hpr[0] = view_hpr[0];
            hpr[1] = view_hpr[1];
            hpr[2] = view_hpr[2];
        }

    }

    public void dsSetViewpoint(float[] xyz, float[] hpr) {
        if (current_state < 1) dsError("dsSetViewpoint() called before simulation started");
        if (xyz != null) {
            view_xyz[0] = xyz[0];
            view_xyz[1] = xyz[1];
            view_xyz[2] = xyz[2];
        }
        if (hpr != null) {
            view_hpr[0] = hpr[0];
            view_hpr[1] = hpr[1];
            view_hpr[2] = hpr[2];
            wrapCameraAngles();
        }
    }


    public void dsStop() {
        _run = false;
    }


    public void dsDrawBox(DVector3C pos, DMatrix3C R, DVector3C sides) {
        // Nothing
    }


    public void dsDrawCapsule(DVector3C pos, DMatrix3C R, float length,
                              float radius) {
        // Nothing
    }


    public void dsDrawConvex(DVector3C pos, DMatrix3C R, double[] _planes,
                             int _planecount, double[] _points, int _pointcount, int[] _polygons) {
        // Nothing
    }


    public void dsDrawCylinder(DVector3C pos, DMatrix3C R, float length,
                               float radius) {
        // Nothing
    }


    public void dsDrawLine(DVector3C pos1, DVector3C pos2) {
        // Nothing
    }


    public void dsDrawSphere(DVector3C pos, DMatrix3C R, float radius) {
        // Nothing
    }


    public void dsSetColorAlpha(float red, float green, float blue, float alpha) {
        // Nothing
    }


    public void dsSetDrawMode(int mode) {
        // Nothing
    }

    public void dsDrawTriangle(DVector3C pos, DMatrix3C rot, float[] v, int i,
                               int j, int k, boolean solid) {
        // Nothing
    }

    public void dsDrawTriangle(DVector3C pos, DMatrix3C r, DVector3C v0,
                               DVector3C v1, DVector3C v2, boolean solid) {
        // Nothing
    }

    public void dsDrawTriangle(DVector3C pos, DMatrix3C R, float[] v0,
                               float[] v1, float[] v2, boolean solid) {
        // Nothing
    }

    public void dsSetSphereQuality(int n) {
        // Nothing
    }

}
