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
package deep.learning.human;

import org.ode4j.math.DMatrix3;
import org.ode4j.math.DMatrix3C;
import org.ode4j.math.DQuaternion;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DAABBC;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DBox;
import org.ode4j.ode.DCapsule;
import org.ode4j.ode.DCylinder;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DSphere;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.OdeMath;
import org.ode4j.ode.internal.Rotation;

import deep.learning.human.statue.BoneImpl;
import deep.learning.human.statue.StatueBuilder;
import deep.learning.human.utils.Utils;

import static deep.learning.human.internal.DrawStuff.dsDrawBox;
import static deep.learning.human.internal.DrawStuff.dsDrawCapsule;
import static deep.learning.human.internal.DrawStuff.dsDrawConvex;
import static deep.learning.human.internal.DrawStuff.dsDrawCylinder;
import static deep.learning.human.internal.DrawStuff.dsDrawSphere;
import static deep.learning.human.internal.DrawStuff.dsFunctions;
import static deep.learning.human.internal.DrawStuff.dsSetColorAlpha;
import static deep.learning.human.internal.DrawStuff.dsSetViewpoint;
import static deep.learning.human.internal.DrawStuff.dsSimulationLoop;

public class DemoStatue extends dsFunctions {

    private DWorld world;
    private DSpace space;
    private Human human;
    private static double[] xyz = {14.051105, -14.9596, 4.8200006};
    private static double[] hpr = {135.5, 10.5, 0.0};
    private DJointGroup contactGroup;
    private static final double STEP = 0.01;

    @Override
    public void start() {
        dsSetViewpoint(xyz, hpr);
        world = OdeHelper.createWorld();
        world.setGravity(0, 0, -9.8);
        world.setDamping(1e-4, 1e-5);

        space = OdeHelper.createSimpleSpace();
        contactGroup = OdeHelper.createJointGroup();
        OdeHelper.createPlane(space, 0, 0, 1, 0);
        double height = 180;
        human = StatueBuilder.build(world, space, height);

        DQuaternion q = new DQuaternion(1, 0, 0, 0);
        Rotation.dQFromAxisAndAngle(q, new DVector3(1, 0, 0), -0.5 * Math.PI);
        for (int i = 0; i < human.getBones().size(); i++) {
            HumanBone bone = human.getBones().get(i);
            //DGeom g = OdeHelper.createCapsule(space, bone.getRadius(), bone.getLength());
            DBody body = bone.getBody();
            DQuaternion qq = new DQuaternion();
            OdeMath.dQMultiply1(qq, q, body.getQuaternion());
            body.setQuaternion(qq);
            DMatrix3 R = new DMatrix3();
            OdeMath.dRfromQ(R, q);
            DVector3 v = new DVector3();
            OdeMath.dMultiply0_133(v, body.getPosition(), R);
            body.setPosition(v.get0(), v.get1(), v.get2());
            //g.setBody(body);
        }
        // initial camera position
        dsSetViewpoint(xyz, hpr);
    }

    @Override
    public void stop() {
        human.destroy();
        contactGroup.destroy();
        space.destroy();
        world.destroy();
    }

    private void drawGeom(DGeom g) {
        if (g instanceof DCapsule) {
            DCapsule cap = (DCapsule) g;
            dsDrawCapsule(g.getPosition(), g.getRotation(), cap.getLength(), cap.getRadius());
        } else if (g instanceof BoneImpl) {
            dsDrawConvex(
            g.getPosition(),
            g.getRotation(),
            ((BoneImpl) g).getPlanes(),
                    ((BoneImpl) g).getPlanesNumber(),
                    ((BoneImpl) g).getPoints(),
                    ((BoneImpl) g).getPointsNumber(),
                    ((BoneImpl) g).getPolygons());
        }
    }

    private void drawBoneGeom(DGeom g, DVector3C pos, DMatrix3C R, boolean show_aabb) {
        if (g == null) return;
        if (pos == null) pos = g.getPosition();
        if (R == null) R = g.getRotation();

        if (g instanceof DBox) {
            DVector3C sides = ((DBox) g).getLengths();
            dsDrawBox(pos, R, sides);
        } else if (g instanceof DSphere) {
            dsDrawSphere(pos, R, ((DSphere) g).getRadius());
        } else if (g instanceof DCylinder) {
            double radius = ((DCylinder) g).getRadius();
            double length = ((DCylinder) g).getLength();
            dsDrawCylinder(pos, R, length, radius);
        } else if (g instanceof DCapsule) {
            double radius = ((DCapsule) g).getRadius();
            double length = ((DCapsule) g).getLength();
            dsDrawCapsule(pos, R, length, radius);
        }


        if (show_aabb) {
            // draw the bounding box for this geom
            DAABBC aabb = g.getAABB();
            DVector3 bbpos = aabb.getCenter();
            DVector3 bbsides = aabb.getLengths();
            DMatrix3 RI = new DMatrix3();
            RI.setIdentity();
            dsSetColorAlpha(1f, 0f, 0f, 0.5f);
            dsDrawBox(bbpos, RI, bbsides);
        }
    }

    private DGeom.DNearCallback nearCallback = new DGeom.DNearCallback() {

        public void call(Object data, DGeom o1, DGeom o2) {
            Utils.nearCallback(data, o1, o2, world, contactGroup);
        }
    };

    @Override
    public void step(boolean pause) {
        space.collide(null, nearCallback);
        if (!pause) {
            world.quickStep(STEP);
        }
        contactGroup.empty();
        // now we draw everything
        for (DGeom g : space.getGeoms()) {
            drawGeom(g);
        }
    }

    public static void main(String[] args) {
        new DemoStatue().demo(args);
    }

    private void demo(String[] args) {
        // create world
        OdeHelper.initODE();
        // run demo
        dsSimulationLoop(args, 800, 600, this);
        OdeHelper.closeODE();
    }

    @Override
    public void command(char cmd) {
        cmd = Character.toLowerCase(cmd);
        if (cmd == ' ') {
            //human.getBones().get(DHumanImpl.PELVIS).getBody().setLinearVel(0, 0, 80);
            //human.getBones().get(DHumanImpl.PELVIS).getBody().addForce(0, 0, 8000);
        }
    }

}