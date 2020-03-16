/*************************************************************************
 *                                                                       *
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
 *       the file ODE4J-LICENSE-BSD.TXT.                                 *
 *                                                                       *
 * This library is distributed in the hope that it will be useful,       *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the files    *
 * LICENSE.TXT and ODE4J-LICENSE-BSD.TXT for more details.               *
 *                                                                       *
 *************************************************************************/
package org.lwjgl;

import deep.learning.human.internal.DrawStuffApi;
import org.joml.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;
import org.ode4j.math.DMatrix3C;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.OdeHelper;

import java.lang.Math;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static deep.learning.human.internal.DrawStuff.DS_TEXTURE_NUMBER;
import static deep.learning.human.internal.DrawStuff.dsFunctions;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.ode4j.ode.internal.cpp4j.Cstdio.printf;

public class DrawStuffFromScratch implements DrawStuffApi {

    private static boolean firsttime = true;
    private static volatile boolean _run = true;

    private long window;
    private int width = 800;
    private int height = 600;
    private int fbWidth = 800;
    private int fbHeight = 600;
    private boolean windowed = true;

    private GLCapabilities caps;
    private GLFWKeyCallback keyCallback;
    private GLFWCursorPosCallback cpCallback;
    private GLFWMouseButtonCallback mbCallback;
    private GLFWScrollCallback scrollCallback;
    private GLFWFramebufferSizeCallback fbCallback;
    private GLFWWindowSizeCallback wsCallback;
    private Callback debugProc;

    //apply these to camera
    private float mouseX = 0.0f;
    private float mouseY = 0.0f;
    private float mouseDx = 0.0f;
    private float mouseDy = 0.0f;
    private boolean[] keyDown = new boolean[GLFW.GLFW_KEY_LAST];
    private boolean leftMouseDown = false;
    private boolean rightMouseDown = false;
    private static boolean run = true;            // 1 if simulation running
    private static boolean pause = false;            // 1 if in `pause' mode
    private static boolean singlestep = false;        // 1 if single step key pressed
    private static boolean writeframes = false;        // 1 if frame files to be written
    //

    private Camera camera = new Camera();
    private long lastTime = System.nanoTime();
    private Vector3d tmp = new Vector3d();
    private Vector3d newPosition = new Vector3d();
    private Vector3f tmp2 = new Vector3f();
    private Vector3f tmp3 = new Vector3f();
    private Vector3f tmp4 = new Vector3f();
    private Matrix4f projMatrix = new Matrix4f();
    private Matrix4f viewMatrix = new Matrix4f();
    private Matrix4f modelMatrix = new Matrix4f();
    private Matrix4f viewProjMatrix = new Matrix4f();
    private Matrix4f invViewMatrix = new Matrix4f();
    private Matrix4f invViewProjMatrix = new Matrix4f();
    private FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
    private FrustumIntersection frustumIntersection = new FrustumIntersection();

    // current camera position and orientation
    private float[] view_xyz = new float[3];    // position x,y,z
    private float[] view_hpr = new float[3];    // heading, pitch, roll (degrees)

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

    public void dsSetTexture(DS_TEXTURE_NUMBER texture_number) {
        // Nothing
    }

    public void dsSimulationLoop(String[] args, int window_width,
                                 int window_height, dsFunctions fn) {
        boolean initial_pause = false;
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-pause")) initial_pause = true;
        }
        initMotionModel();
        try {
            dsPlatformSimLoop(window_width, window_height, fn, initial_pause);
        } finally {
            glfwTerminate();
        }
    }

    void dsPlatformSimLoop(int window_width, int window_height, dsFunctions fn,
                           boolean initial_pause) {
        if (firsttime) {
            System.err.println();
            System.err.print("Using ode4j version: " + OdeHelper.getVersion());
            System.err.println("  [" + OdeHelper.getConfiguration() + "]");
            System.err.println();
            firsttime = false;
            this.width = window_width;
            this.height = window_height;
            init();
        }

        fn.start();
        long startTime = System.currentTimeMillis() + 5000;
        long fps = 0;
        _run = true;
        while (_run && !glfwWindowShouldClose(window)) {
            fn.step(initial_pause);
            update();
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
        fn.stop();
        if (debugProc != null)
            debugProc.free();
        keyCallback.free();
        cpCallback.free();
        mbCallback.free();
        fbCallback.free();
        wsCallback.free();
        glfwDestroyWindow(window);
    }

    private void update() {
        glfwPollEvents();
        glViewport(0, 0, fbWidth, fbHeight);
        long thisTime = System.nanoTime();
        float dt = (thisTime - lastTime) / 1E9f;
        lastTime = thisTime;
        //update particles here
        camera.update(dt);

        projMatrix.setPerspective((float) Math.toRadians(40.0f), (float) width / height, 0.1f, 5000.0f);
        viewMatrix.set(camera.rotation).invert(invViewMatrix);
        viewProjMatrix.set(projMatrix).mul(viewMatrix).invert(invViewProjMatrix);
        frustumIntersection.set(viewProjMatrix);

        // update all objects

        updateControls();
        render();
        glfwSwapBuffers(window);
    }

    private void init() {
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_SAMPLES, 4);

        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode vidmode = glfwGetVideoMode(monitor);
        if (!windowed) {
            this.width = vidmode.width();
            this.height = vidmode.height();
            fbWidth = width;
            fbHeight = height;
        }
        window = glfwCreateWindow(width, height, "Little Space Shooter Game", !windowed ? monitor : 0L, NULL);
        if (window == NULL) {
            throw new AssertionError("Failed to create the GLFW window");
        }
        glfwSetCursor(window, glfwCreateStandardCursor(GLFW_CROSSHAIR_CURSOR));

        glfwSetFramebufferSizeCallback(window, fbCallback = new GLFWFramebufferSizeCallback() {
            public void invoke(long window, int width, int height) {
                if (width > 0 && height > 0 && (DrawStuffFromScratch.this.fbWidth != width || DrawStuffFromScratch.this.fbHeight != height)) {
                    DrawStuffFromScratch.this.fbWidth = width;
                    DrawStuffFromScratch.this.fbHeight = height;
                }
            }
        });
        glfwSetWindowSizeCallback(window, wsCallback = new GLFWWindowSizeCallback() {
            public void invoke(long window, int width, int height) {
                if (width > 0 && height > 0 && (DrawStuffFromScratch.this.width != width || DrawStuffFromScratch.this.height != height)) {
                    DrawStuffFromScratch.this.width = width;
                    DrawStuffFromScratch.this.height = height;
                }
            }
        });

        //Init controls here
        glfwSetKeyCallback(window, keyCallback = new GLFWKeyCallback() {
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_UNKNOWN)
                    return;
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                    glfwSetWindowShouldClose(window, true);
                }
                if (action == GLFW_PRESS || action == GLFW_REPEAT) {
                    keyDown[key] = true;
                } else {
                    keyDown[key] = false;
                }
                switch (key) {
                    case GLFW_KEY_T:
                        //dsSetTextures(!dsGetTextures());
                        break;
                    case GLFW_KEY_S:
                        //dsSetShadows(!dsGetShadows());
                        break;
                    case GLFW_KEY_X:
                        run = false;
                        break;
                    case GLFW_KEY_P:
                        pause = !pause;
                        singlestep = false;
                        break;
                    case GLFW_KEY_O:
                        if (pause) singlestep = true;
                        break;
                    case GLFW_KEY_V: {
                        float[] xyz = new float[3], hpr = new float[3];
                        dsGetViewpoint(xyz, hpr);
                        printf("Viewpoint = (%.4f,%.4f,%.4f,%.4f,%.4f,%.4f)\n",
                                xyz[0], xyz[1], xyz[2], hpr[0], hpr[1], hpr[2]);
                        break;
                    }
                    case GLFW_KEY_W:
                        writeframes = !writeframes;
                        if (writeframes) printf("Now writing frames to PPM files\n");
                        break;
                }
            }
        });
        glfwSetCursorPosCallback(window, cpCallback = new GLFWCursorPosCallback() {
            public void invoke(long window, double xpos, double ypos) {
                float normX = (float) ((xpos - width/2.0) / width * 2.0);
                float normY = (float) ((ypos - height/2.0) / height * 2.0);
                float currentX = Math.max(-width/2.0f, Math.min(width/2.0f, normX));
                float currentY = Math.max(-height/2.0f, Math.min(height/2.0f, normY));
                DrawStuffFromScratch.this.mouseDx = currentX - mouseX;
                DrawStuffFromScratch.this.mouseX = currentX;
                DrawStuffFromScratch.this.mouseDy = currentY - mouseY;
                DrawStuffFromScratch.this.mouseY = currentY;
            }
        });
        glfwSetMouseButtonCallback(window, mbCallback = new GLFWMouseButtonCallback() {
            public void invoke(long window, int button, int action, int mods) {
                if (button == GLFW_MOUSE_BUTTON_LEFT) {
                    if (action == GLFW_PRESS)
                        leftMouseDown = true;
                    else if (action == GLFW_RELEASE)
                        leftMouseDown = false;
                } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                    if (action == GLFW_PRESS)
                        rightMouseDown = true;
                    else if (action == GLFW_RELEASE)
                        rightMouseDown = false;
                }
            }
        });
        glfwSetScrollCallback(window, scrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long l, double v, double v1) {
                System.out.println("l " + l + " v " + v + " v1 " + v1);
            }
        });
        //

        glfwMakeContextCurrent(window);
        glfwSwapInterval(0);
        glfwShowWindow(window);

        IntBuffer framebufferSize = BufferUtils.createIntBuffer(2);
        nglfwGetFramebufferSize(window, memAddress(framebufferSize), memAddress(framebufferSize) + 4);
        fbWidth = framebufferSize.get(0);
        fbHeight = framebufferSize.get(1);
        caps = GL.createCapabilities();
        if (!caps.OpenGL20) {
            throw new AssertionError("This demo requires OpenGL 2.0.");
        }
        debugProc = GLUtil.setupDebugMessageCallback();

        //Init resources here (textures, images, etc)

        glEnableClientState(GL_VERTEX_ARRAY);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
    }

    private void render() {
        glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);
//        drawShips();
//        drawAsteroids();
//        drawCubemap();
//        drawShots();
//        drawParticles();
//        drawHudShotDirection();
//        drawHudShip();
        drawVelocityCompass();
    }

    private void drawVelocityCompass() {
        glUseProgram(0);
        glEnable(GL_BLEND);
        glEnableClientState(GL_NORMAL_ARRAY);
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadMatrixf(projMatrix.get(matrixBuffer));
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glTranslatef(0, -1, -4);
        glMultMatrixf(viewMatrix.get(matrixBuffer));
        glScalef(0.3f, 0.3f, 0.3f);
        glColor4f(0.1f, 0.1f, 0.1f, 0.2f);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_DEPTH_TEST);
        glBegin(GL_LINES);
        glColor4f(1, 0, 0, 1);
        glVertex3f(0, 0, 0);
        glVertex3f(1, 0, 0);
        glColor4f(0, 1, 0, 1);
        glVertex3f(0, 0, 0);
        glVertex3f(0, 1, 0);
        glColor4f(0, 0, 1, 1);
        glVertex3f(0, 0, 0);
        glVertex3f(0, 0, 1);
        glColor4f(1, 1, 1, 1);
        glVertex3f(0, 0, 0);
        glEnd();
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
        glDisableClientState(GL_NORMAL_ARRAY);
        glDisable(GL_BLEND);
    }

    public double dsElapsedTime() {
        // Nothing
        return 0;
    }


    public void dsGetViewpoint(float[] xyz, float[] hpr) {
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

    private void wrapCameraAngles() {
        for (int i = 0; i < 3; i++) {
            while (view_hpr[i] > 180) view_hpr[i] -= 360;
            while (view_hpr[i] < -180) view_hpr[i] += 360;
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

    public float[] getView_xyz() {
        return view_xyz;
    }

    public float[] getView_hpr() {
        return view_hpr;
    }

    private void initMotionModel() {
        view_xyz[0] = 2;
        view_xyz[1] = 0;
        view_xyz[2] = 1;
        view_hpr[0] = 180;
        view_hpr[1] = 0;
        view_hpr[2] = 0;
    }

    private void updateControls() {
        if (rightMouseDown)
            camera.rotation.rotateXYZ(mouseY / 1000, mouseX / 1000, 0);
    }
}
