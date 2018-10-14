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

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.internal.Common;

import static deep.learning.human.internal.DrawStuff.dsFunctions;
import static org.ode4j.ode.internal.cpp4j.Cstdio.fflush;
import static org.ode4j.ode.internal.cpp4j.Cstdio.fprintf;
import static org.ode4j.ode.internal.cpp4j.Cstdio.printf;
import static org.ode4j.ode.internal.cpp4j.Cstdio.stderr;
import static org.ode4j.ode.internal.cpp4j.Cstdio.stdout;
import static org.ode4j.ode.internal.cpp4j.Cstdio.vfprintf;
import static org.ode4j.ode.internal.cpp4j.Cstdio.vprintf;

/**
 * Main window and event handling for LWJGL.
 * Ported from x11.cpp.
 */
abstract class LwJGL extends Internal implements DrawStuffApi {

    //Ensure that Display.destroy() is called (TZ)
    //Not sure this works, but it's an attempt at least.
    //-> This should avoid the Problem that a process keeps running with 99%CPU,
    //   even if the window is closed (clicking on the 'x'). The supposed
    //   problem is that when clicking 'x', Display.destroy() never gets called
    //   by dsPlatformSimLoop().
    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
//				Display.destroy();
            }
        });
    }

    private static void printMessage(String msg1, String fmt, Object... ap) {
        fflush(stderr);
        fflush(stdout);
        fprintf(stderr, "\n%s: ", msg1);
        vfprintf(stderr, fmt, ap);
        fprintf(stderr, "\n");
        fflush(stderr);
    }

    static void dsError(String msg, Object... ap) {
        printMessage("Error", msg, ap);
        throw new RuntimeException();
    }

    static void dsDebug(String msg, Object... ap) {
        printMessage("INTERNAL ERROR", msg, ap);
        throw new RuntimeException();
    }

    static void dsPrint(String msg, Object... ap) {
        vprintf(msg, ap);
    }

    private static int width = 0, height = 0;        // window size
    private static int last_key_pressed = 0;        // last key pressed in the window
    private static boolean run = true;            // 1 if simulation running
    private static boolean pause = false;            // 1 if in `pause' mode
    private static boolean singlestep = false;        // 1 if single step key pressed
    private static boolean writeframes = false;        // 1 if frame files to be written

    private static void createMainWindow(int _width, int _height) {
        // create Window of size 300x300
        try {
            Display.setLocation((Display.getDisplayMode().getWidth() - _width) / 2,
                    (Display.getDisplayMode().getHeight() - _height) / 2);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Missing lwjgl native libraries.");
            System.err.println("If you are using maven, make sure to use "
                    + "'-Djava.library.path=target/natives' as VM argument of your application.");
            System.err.println("For plain Eclipse, add the native library path to the included "
                    + "lwjgl.jar in the definition of the Referenced Libraries.");
            throw e;
        }
        try {
            Display.setDisplayMode(new DisplayMode(_width, _height));
            Display.setTitle("Simulation");
            Display.setVSyncEnabled(true);  //for VSync (TZ)
            Display.create();
        } catch (LWJGLException e) {
            throw new RuntimeException(e);
        }

        try {
            Keyboard.create();
            Mouse.create();
        } catch (LWJGLException e) {
            throw new RuntimeException(e);
        }

        if (firsttime) {
            System.err.println("GL_VENDOR:     " + GL11.glGetString(GL11.GL_VENDOR));
            System.err.println("GL_RENDERER:   " + GL11.glGetString(GL11.GL_RENDERER));
            System.err.println("GL_VERSION:    " + GL11.glGetString(GL11.GL_VERSION));
            System.err.println("LWJGL_VERSION: " + Sys.getVersion());
            System.err.println();
            System.err.println("glLoadTransposeMatrixfARB() supported: " +
                    GLContext.getCapabilities().GL_ARB_transpose_matrix);
        }
        width = _width;
        height = _height;
        last_key_pressed = 0;

        if (width < 1 || height < 1) dsDebug("", "bad window width or height");
    }

    private static void destroyMainWindow() {
        Keyboard.destroy();
        Mouse.destroy();
        Display.destroy();
    }

    private static void captureFrame(int num) {
        throw new UnsupportedOperationException();
    }

    /**
     * Handles the keyboard
     *
     * @param fn
     */
    private void handleKeyboard(dsFunctions fn) {
        Keyboard.poll();
        while (Keyboard.next()) {
            char key = (char) Keyboard.getEventKey();
            if (key == Keyboard.KEY_ESCAPE) {
                run = false;
            }

            if (!(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) ||
                    Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))) {
                char keyChar = Keyboard.getEventCharacter();
                if (keyChar >= ' ' && keyChar <= 126) fn.command(keyChar);
            } else {
                if (key == last_key_pressed) {
                    continue;
                }
                switch (key) {
                    case Keyboard.KEY_T:
                        dsSetTextures(!dsGetTextures());
                        break;
                    case Keyboard.KEY_S:
                        dsSetShadows(!dsGetShadows());
                        break;
                    case Keyboard.KEY_X:
                        run = false;
                        break;
                    case Keyboard.KEY_P:
                        pause = !pause;
                        singlestep = false;
                        break;
                    case Keyboard.KEY_O:
                        if (pause) singlestep = true;
                        break;
                    case Keyboard.KEY_V: {
                        float[] xyz = new float[3], hpr = new float[3];
                        dsGetViewpoint(xyz, hpr);
                        printf("Viewpoint = (%.4f,%.4f,%.4f,%.4f,%.4f,%.4f)\n",
                                xyz[0], xyz[1], xyz[2], hpr[0], hpr[1], hpr[2]);
                        break;
                    }
                    case Keyboard.KEY_W:
                        writeframes = !writeframes;
                        if (writeframes) printf("Now writing frames to PPM files\n");
                        break;
                }
            }
            last_key_pressed = key;        // a kludgy place to put this...
        }
    }

    /**
     * handles the mouse
     */
    private void handleMouse() {
        readBufferedMouse();
    }

    /**
     * reads a mouse in buffered mode
     */
    private void readBufferedMouse() {
        // iterate all events, use the last button down
        while (Mouse.next()) {
            if (Mouse.getEventButton() != -1) {
                if (Mouse.getEventButtonState()) {
                }
                //lastButton = Mouse.getEventButton();
            }
        }

        updateState();
    }

    /**
     * Updates our "model"
     */
    private void updateState() {
        int dx = Mouse.getDX();
        int dy = Mouse.getDY();
        int dw = Mouse.getDWheel();
        // get out if no movement
        if (dx == dy && dx == 0 && dw == 0) {
            return;
        }
        //LWJGL: 0=left 1=right 2=middle
        //GL: 0=left 1=middle 2=right
        int mode = 0;
        if (Mouse.isButtonDown(0)) mode |= 1;
        if (Mouse.isButtonDown(2)) mode |= 2;
        if (Mouse.isButtonDown(1)) mode |= 4;
        if (mode != 0) {
            //LWJGL has inverted dy wrt C++/GL
            dsMotion(mode, dx, -dy);
            float[] xyz = getView_xyz();
            float[] hpr = getView_hpr();
            System.out.println(xyz[0] + ", " + xyz[1] + ", " + xyz[2] + "\n" + hpr[0] + ", " + hpr[1] + ", " + hpr[2]);
        }
    }

    private static boolean firsttime = true;

    @Override
    void dsPlatformSimLoop(int window_width, int window_height, dsFunctions fn,
                           boolean initial_pause) {
        pause = initial_pause;
        createMainWindow(window_width, window_height);
        //glXMakeCurrent (display,win,glx_context);
        //TODO ?
        //GLContext.useContext(context);
        try {
            //Sets the context / by TZ
            Display.makeCurrent();
        } catch (LWJGLException e) {
            throw new RuntimeException(e);
        }

        dsStartGraphics(window_width, window_height, fn);

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
                                    "   Ctrl-V : print current viewpoint coordinates (x, y, z, h, p, r).\n" +
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

        int frame = 1;
        run = true;
        long startTime = System.currentTimeMillis() + 5000;
        long fps = 0;
        while (run && !Display.isCloseRequested()) {
            handleKeyboard(fn);
            handleMouse();

            //processDrawFrame: This was not move into separate method for convenience

            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            dsDrawFrame(width, height, fn, pause && !singlestep);
            singlestep = false;

            Display.update();
            if (startTime > System.currentTimeMillis()) {
                fps++;
            } else {
                long timeUsed = 5000 + (startTime - System.currentTimeMillis());
                startTime = System.currentTimeMillis() + 5000;
                System.out.println(fps + " frames in " + (timeUsed / 1000f) + " seconds = "
                        + (fps / (timeUsed / 1000f)));
                fps = 0;
            }

            // capture frames if necessary
            if (pause == false && writeframes) {
                captureFrame(frame);
                frame++;
            }
        }

        //if (fn.stop)
        fn.stop();
        dsStopGraphics();

        destroyMainWindow();
    }

    public void dsStop() {
        run = false;
    }

    private static double prev = System.nanoTime() / 1000000000.0;

    public double dsElapsedTime() {
        double curr = System.nanoTime() / 1000000000.0;
        double retval = curr - prev;
        prev = curr;
        if (retval > 1.0) retval = 1.0;
        if (retval < Common.dEpsilon) retval = Common.dEpsilon;
        return retval;
    }
}
