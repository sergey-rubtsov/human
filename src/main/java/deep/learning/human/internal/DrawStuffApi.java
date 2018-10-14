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
package deep.learning.human.internal;

import org.ode4j.math.DMatrix3C;
import org.ode4j.math.DVector3C;

import static deep.learning.human.internal.DrawStuff.DS_TEXTURE_NUMBER;
import static deep.learning.human.internal.DrawStuff.dsFunctions;

public interface DrawStuffApi {

    void dsSimulationLoop(String[] args, int window_width,
                          int window_height, dsFunctions fn);

    void dsStop();

    void dsSetTexture(DS_TEXTURE_NUMBER texture_number);

    void dsSetColor(float red, float green, float blue);

    void dsDrawBox(final float[] pos, final float[] R,
                   final float[] sides);

    void dsDrawSphere(final float[] pos, final float[] R,
                      float radius);

    void dsDrawCylinder(final float[] pos, final float[] R,
                        float length, float radius);

    void dsDrawCapsule(final float[] pos, final float[] R,
                       float length, float radius);

    void dsDrawLine(final float[] pos1, final float[] pos2);

    void dsDrawBox(final DVector3C pos, final DMatrix3C R,
                   final DVector3C sides);

    void dsDrawConvex(final DVector3C pos, final DMatrix3C R,
                      double[] _planes, int _planecount, double[] _points,
                      int _pointcount, int[] _polygons);

    void dsDrawSphere(final DVector3C pos, final DMatrix3C R,
                      float radius);

    void dsDrawCylinder(final DVector3C pos, final DMatrix3C R,
                        float length, float radius);

    void dsDrawCapsule(final DVector3C pos, final DMatrix3C R,
                       float length, float radius);

    void dsDrawLine(final DVector3C pos1, final DVector3C pos2);

    void dsSetViewpoint(float[] xyz, float[] hpr);

    void dsGetViewpoint(float[] xyz, float[] hpr);

    double dsElapsedTime();

    void dsSetColorAlpha(float red, float green, float blue,
                         float alpha);

    void dsSetDrawMode(int mode);

    void dsDrawTriangle(DVector3C pos, DMatrix3C rot,
                        float[] v, int i, int j, int k, boolean solid);

    void dsDrawTriangle(DVector3C pos, DMatrix3C r,
                        DVector3C v0, DVector3C v1, DVector3C v2, boolean solid);

    void dsDrawTriangle(final DVector3C pos, final DMatrix3C R,
                        final float[] v0, final float[] v1, final float[] v2, boolean solid);

    void dsSetSphereQuality(int n);

    // position x,y,z
    float[] getView_xyz();

    // heading, pitch, roll (degrees)
    float[] getView_hpr();
}