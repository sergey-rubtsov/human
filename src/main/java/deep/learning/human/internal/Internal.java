package deep.learning.human.internal;

import static deep.learning.human.internal.DrawStuff.dsFunctions;

/**
 * functions supplied and used by the platform specific code.
 */
abstract class Internal {

    // supplied by platform specific code

    abstract void dsPlatformSimLoop(int window_width, int window_height,
                                    dsFunctions fn, boolean initial_pause);


    // used by platform specific code

    abstract void dsStartGraphics(int width, int height, dsFunctions fn);

    abstract void dsDrawFrame(int width, int height, dsFunctions fn, boolean pause);

    abstract void dsStopGraphics();

    abstract void dsMotion(int mode, int deltax, int deltay);

    abstract boolean dsGetShadows();

    abstract void dsSetShadows(boolean a);

    abstract boolean dsGetTextures();

    abstract void dsSetTextures(boolean a);
}
