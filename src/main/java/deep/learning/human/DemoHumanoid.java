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

import deep.learning.human.humanoid.HumanoidBuilder;
import deep.learning.human.humanoid.LongBone;
import deep.learning.human.humanoid.ShortBone;
import deep.learning.human.humanoid.joints.BallJoint;
import deep.learning.human.humanoid.joints.UniversalJoint;
import deep.learning.human.utils.Utils;
import deep.learning.human.utils.config.HumanConfig;
import org.ode4j.math.DMatrix3;
import org.ode4j.math.DQuaternion;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DCapsule;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.OdeMath;
import org.ode4j.ode.internal.Rotation;

import java.util.Random;

import static deep.learning.human.internal.DrawStuff.dsDrawCapsule;
import static deep.learning.human.internal.DrawStuff.dsDrawConvex;
import static deep.learning.human.internal.DrawStuff.dsDrawLine;
import static deep.learning.human.internal.DrawStuff.dsFunctions;
import static deep.learning.human.internal.DrawStuff.dsSetColor;
import static deep.learning.human.internal.DrawStuff.dsSetViewpoint;
import static deep.learning.human.internal.DrawStuff.dsSimulationLoop;

public class DemoHumanoid extends dsFunctions {

    private DWorld world;
    private DSpace space;
    private Human human;
    private static double[] xyz = {14.051105, -14.9596, 4.8200006};
    private static double[] hpr = {135.5, 17.5, 0.0};
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
        HumanConfig preprocessed = HumanoidBuilder.build("1.bvh",
                //, "-__LThumb", "-__RThumb"
                //"+Neck1"
                //"+Spine1"//,
                "+__LeftShoulder",
                "+__RightShoulder",
                "!2"
                //"+RightForeArm",
                //"+LeftForeArm",
                //"-Neck"
        );
        //human = HumanoidBuilder.build(world, space, preprocessed);
        human = HumanoidBuilder.build(world, space, "bvh/edited.json");
        DQuaternion q = new DQuaternion(1, 0, 0, 0);
        Rotation.dQFromAxisAndAngle(q, new DVector3(1, 0, 0), -0.5 * Math.PI);
        for (int i = 0; i < human.getBonesList().size(); i++) {
            HumanBone bone = human.getBonesList().get(i);
            DBody body = bone.getBody();
            DQuaternion qq = new DQuaternion();
            OdeMath.dQMultiply1(qq, q, body.getQuaternion());
            body.setQuaternion(qq);
            DMatrix3 R = new DMatrix3();
            OdeMath.dRfromQ(R, q);
            DVector3 v = new DVector3();
            OdeMath.dMultiply0_133(v, body.getPosition(), R);
            body.setPosition(v.get0(), v.get1(), v.get2());
        }
        // initial camera position
        dsSetViewpoint(xyz, hpr);
        //Utils.postProcessSelfColliding(space, world, human);
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
            dsSetColor (1,1,1);
            DCapsule cap = (DCapsule) g;
            dsDrawCapsule(g.getPosition(),
                    g.getRotation(),
                    cap.getLength(),
                    cap.getRadius());
        } else if (g instanceof LongBone) {
            dsSetColor (0.8,0.8,0.8);
            dsDrawConvex(
                    g.getPosition(),
                    g.getRotation(),
                    ((LongBone) g).getPlanes(),
                    ((LongBone) g).getPlaneCount(),
                    ((LongBone) g).getPoints(),
                    ((LongBone) g).getPointCount(),
                    ((LongBone) g).getPolygons());
            /*
            dsSetColor (1,0,0);
            dsDrawLine(g.getBody().getPosition(), ((LongBone) g).getLeft());
            dsDrawLine(g.getBody().getPosition(), ((LongBone) g).getTop());
            dsSetColor (0,1,0);
            dsDrawLine(g.getBody().getPosition(), ((LongBone) g).getRight());
            dsDrawLine(g.getBody().getPosition(), ((LongBone) g).getBack());
            dsDrawLine(g.getBody().getPosition(), ((LongBone) g).getBottom());
            DVector3 force = new DVector3(g.getBody().getPosition()).add(g.getBody().getForce());
            dsDrawLine(g.getBody().getPosition(), force);
            dsSetColor (0,1,0);
            DVector3 torque = new DVector3(g.getBody().getPosition()).add(g.getBody().getTorque());
            dsDrawLine(g.getBody().getPosition(), torque);*/
        }
        if (g instanceof  HumanBone) {
            dsSetColor (1,0,0);
            DVector3 front = new DVector3(((HumanBone) g).getFront());
            front.sub(g.getBody().getPosition());
            front.scale(1.1);
            front.add(g.getBody().getPosition());
            dsDrawLine(g.getBody().getPosition(), front);
        }
    }

    private DGeom.DNearCallback nearCallback = new DGeom.DNearCallback() {
        public void call(Object data, DGeom o1, DGeom o2) {
            Utils.nearCallback(data, o1, o2, world, contactGroup);
        }
    };

    private double t;

    @Override
    public void step(boolean pause) {
        space.collide(null, nearCallback);
        if (!pause) {
            t = t + STEP;
            double f0 = Math.sin(t * 2) * 1000;
            double f1 = Math.sin(t) * 5000;
            HumanJoint rightKnee = human.getJoint("RightLegRightFoot");
            HumanJoint leftKnee = human.getJoint("LeftLegLeftFoot");
            HumanJoint leftElbow = human.getJoint("LeftForeArmLeftHand");
            HumanJoint rightElbow = human.getJoint("RightForeArmRightHand");
            HumanJoint rightHip = human.getJoint("RightUpLegRightLeg");
            HumanJoint head = human.getJoint("Neck1Head");
            HumanJoint LeftUpLegLeftLeg = human.getJoint("LeftUpLegLeftLeg");
            //((BallJoint) LeftUpLegLeftLeg).pitch(f1);
            //((UniversalJoint) rightHip).addTorque(f0, f1);
            //((BallJoint) rightHip).twist(f1);
            //((BallJoint) head).roll(f1);
            //((BallJoint) head).pitch(f1);
            //((BallJoint) head).yaw(f1);
            //((DHingeJoint) rightElbow.getJoint()).addTorque(f1);
            //((HingeJoint) leftElbow).addTorque(f0);
            //HumanJoint neck0 = human.getJoint("Neck1Head");
            //
            //((DBallJoint) neck0.getJoint()).
            //((DHingeJoint) rightKnee.getJoint()).addTorque(f0);
            //((DHingeJoint) leftKnee.getJoint()).addTorque(f1);
            for (DGeom g : space.getGeoms()) {
                drawGeom(g);
            }
            world.quickStep(STEP);
        }
        contactGroup.empty();
        // now we draw everything

    }

    public static void main(String[] args) {
        new DemoHumanoid().demo(args);
    }

    private void demo(String[] args) {
        // create world
        OdeHelper.initODE();
        // run demo
        dsSimulationLoop(args, 300, 150, this);
        OdeHelper.closeODE();
    }

    @Override
    public void command(char cmd) {
        cmd = Character.toLowerCase(cmd);
        if (cmd == ' ') {
            Random r = new Random();
            int bone = r.nextInt(human.getBonesList().size());
            human.getBonesList().get(bone).getBody().setLinearVel(0, 0, 80);
            //human.getBonesList().get(DHumanImpl.PELVIS).getBody().setLinearVel(0, 0, 80);
            //human.getBonesList().get(DHumanImpl.PELVIS).getBody().addForce(0, 0, 8000);
        }
    }

}