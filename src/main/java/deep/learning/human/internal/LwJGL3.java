package deep.learning.human.internal;

import org.lwjgl.BufferUtils;
import org.lwjgl.SpaceGame;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.system.Callback;
import org.ode4j.math.DMatrix3C;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.internal.Common;

import java.io.IOException;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.GLFW_SAMPLES;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.ode4j.ode.internal.cpp4j.Cstdio.*;
import static org.ode4j.ode.internal.cpp4j.Cstdio.stderr;

abstract class LwJGL3 extends Internal implements DrawStuffApi {

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

    private int width = 0, height = 0;              // window size
    private int fbWidth = 0, fbHeight = 0;          // frame buffer size
    private static int last_key_pressed = 0;        // last key pressed in the window
    private static boolean run = true;            // 1 if simulation running
    private static boolean pause = false;            // 1 if in `pause' mode
    private static boolean singlestep = false;        // 1 if single step key pressed
    private static boolean writeframes = false;        // 1 if frame files to be written

    private boolean windowed = true;
    private long window;
    private GLCapabilities caps;
    private GLFWKeyCallback keyCallback;
    private GLFWCursorPosCallback cpCallback;
    private GLFWMouseButtonCallback mbCallback;
    private GLFWFramebufferSizeCallback fbCallback;
    private GLFWWindowSizeCallback wsCallback;
    private Callback debugProc;
    private float mouseX = 0.0f;
    private float mouseY = 0.0f;
    private float mouseDx = 0.0f;
    private float mouseDy = 0.0f;
    private boolean[] keyDown = new boolean[GLFW.GLFW_KEY_LAST];
    private boolean leftMouseDown = false;
    private boolean rightMouseDown = false;


    /**
     * Updates our "model"
     */
    private void updateState() {
        int dw = 0;
        // get out if no movement
        if (mouseDx == mouseDy && mouseDx == 0 && dw == 0) {
            return;
        }
        //LWJGL: 0=left 1=right 2=middle
        //GL: 0=left 1=middle 2=right
        int mode = 0;
        if (leftMouseDown) mode |= 1;
        if (rightMouseDown) mode |= 2;
//        if (Mouse.isButtonDown(1)) mode |= 4;
        if (mode != 0) {
            //LWJGL has inverted dy wrt C++/GL
            dsMotion(mode, Math.round(mouseDx), Math.round(-mouseDy));
            mouseDx = 0;
            mouseDy = 0;
            float[] xyz = getView_xyz();
            float[] hpr = getView_hpr();
            System.out.println(xyz[0] + ", " + xyz[1] + ", " + xyz[2] + "\n" + hpr[0] + ", " + hpr[1] + ", " + hpr[2]);
        }
    }

    private static boolean firsttime = true;

    private void init(int width, int height) throws IOException {
        if (width < 1 || height < 1) dsDebug("", "bad window width or height");
        if (firsttime) {
//            System.err.println("GL_VENDOR:     " + GL11.glGetString(GL11.GL_VENDOR));
//            System.err.println("GL_RENDERER:   " + GL11.glGetString(GL11.GL_RENDERER));
//            System.err.println("GL_VERSION:    " + GL11.glGetString(GL11.GL_VERSION));
//            System.err.println("LWJGL_VERSION: " + Sys.getVersion());
//            System.err.println();
//            System.err.println("glLoadTransposeMatrixfARB() supported: " +
//                    GLContext.getCapabilities().GL_ARB_transpose_matrix);
        }
        this.width = width;
        this.height = height;
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_SAMPLES, 4);
        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode vidmode = glfwGetVideoMode(monitor);
        if (!windowed) {
            this.width = vidmode.width();
            this.height = vidmode.height();
            fbWidth = width;
            fbHeight = height;
        }
        window = glfwCreateWindow(width, height, "ODE port for Java", !windowed ? monitor : 0L, NULL);
        if (window == NULL) {
            throw new AssertionError("Failed to create the GLFW window");
        }
        glfwSetCursor(window, glfwCreateStandardCursor(GLFW_CROSSHAIR_CURSOR));

        glfwSetFramebufferSizeCallback(window, fbCallback = new GLFWFramebufferSizeCallback() {
            public void invoke(long window, int width, int height) {
                if (width > 0 && height > 0 && (LwJGL3.this.fbWidth != width || LwJGL3.this.fbHeight != height)) {
                    LwJGL3.this.fbWidth = width;
                    LwJGL3.this.fbHeight = height;
                }
            }
        });
        glfwSetWindowSizeCallback(window, wsCallback = new GLFWWindowSizeCallback() {
            public void invoke(long window, int width, int height) {
                if (width > 0 && height > 0 && (LwJGL3.this.width != width || LwJGL3.this.height != height)) {
                    LwJGL3.this.width = width;
                    LwJGL3.this.height = height;
                }
            }
        });
        System.out.println("Press W/S to move forward/backward");
        System.out.println("Press L.Ctrl/Spacebar to move down/up");
        System.out.println("Press A/D to strafe left/right");
        System.out.println("Press Q/E to roll left/right");
        System.out.println("Hold the left mouse button to shoot");
        System.out.println("Hold the right mouse button to rotate towards the mouse cursor");
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
                        dsSetTextures(!dsGetTextures());
                        break;
                    case GLFW_KEY_S:
                        dsSetShadows(!dsGetShadows());
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
                LwJGL3.this.mouseDx = currentX - mouseX;
                LwJGL3.this.mouseX = currentX;
                LwJGL3.this.mouseDy = currentY - mouseY;
                LwJGL3.this.mouseY = currentY;
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
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
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

        /* Create all needed GL resources */

        glEnableClientState(GL_VERTEX_ARRAY);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);
    }

    @Override
    void dsPlatformSimLoop(int window_width, int window_height, DrawStuff.dsFunctions fn,
                           boolean initial_pause) {
        pause = initial_pause;
        try {
            init(window_width, window_height);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        dsStartGraphics(window_width, window_height, fn);

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
        while (run) {
            updateState();
            //processDrawFrame: This was not move into separate method for convenience
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

            dsDrawFrame(width, height, fn, pause && !singlestep);
            singlestep = false;

            if (startTime > System.currentTimeMillis()) {
                fps++;
            } else {
                long timeUsed = 5000 + (startTime - System.currentTimeMillis());
                startTime = System.currentTimeMillis() + 5000;
                System.out.println(fps + " frames in " + (timeUsed / 1000f) + " seconds = "
                        + (fps / (timeUsed / 1000f)));
                fps = 0;
            }
        }
        //if (fn.stop)
        fn.stop();
        dsStopGraphics();
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
