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
package org.ode4j.ode.internal;

import org.ode4j.math.DMatrix3;
import org.ode4j.math.DMatrix3C;
import org.ode4j.math.DQuaternion;
import org.ode4j.math.DQuaternionC;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DMassC;
import org.ode4j.ode.internal.Objects_H.DxPosR;
import org.ode4j.ode.internal.Objects_H.DxPosRC;
import org.ode4j.ode.internal.Objects_H.dxAutoDisable;
import org.ode4j.ode.internal.Objects_H.dxDampingParameters;
import org.ode4j.ode.internal.cpp4j.java.Ref;
import org.ode4j.ode.internal.joints.DxJoint;
import org.ode4j.ode.internal.joints.DxJointNode;
import org.ode4j.ode.internal.joints.OdeJointsFactoryImpl;
import org.ode4j.ode.internal.processmem.DxWorldProcessContext;

import static org.ode4j.ode.OdeMath.dAddVectorCross3;
import static org.ode4j.ode.OdeMath.dCalcVectorDot3;
import static org.ode4j.ode.OdeMath.dMultiply0_331;
import static org.ode4j.ode.OdeMath.dMultiply1_331;
import static org.ode4j.ode.OdeMath.dNormalize3;
import static org.ode4j.ode.OdeMath.dNormalize4;
import static org.ode4j.ode.OdeMath.dOrthogonalizeR;
import static org.ode4j.ode.internal.Common.DBL_EPSILON;
import static org.ode4j.ode.internal.Common.dAASSERT;
import static org.ode4j.ode.internal.Common.dCos;
import static org.ode4j.ode.internal.Common.dDEBUGMSG;
import static org.ode4j.ode.internal.Common.dFabs;
import static org.ode4j.ode.internal.Common.dIASSERT;
import static org.ode4j.ode.internal.Common.dRecip;
import static org.ode4j.ode.internal.Common.dSin;
import static org.ode4j.ode.internal.Common.dSqrt;
import static org.ode4j.ode.internal.Common.dUASSERT;
import static org.ode4j.ode.internal.Matrix.dInvertPDMatrix;
import static org.ode4j.ode.internal.Rotation.dDQfromW;
import static org.ode4j.ode.internal.Rotation.dQMultiply0;
import static org.ode4j.ode.internal.Rotation.dQfromR;
import static org.ode4j.ode.internal.Rotation.dRfromQ;

/**
 * rigid body (dynamics object).
 */
public class DxBody extends DObject implements DBody, Cloneable {

    private static final int dxBodyFlagFiniteRotation = 1;          // use finite rotations
    private static final int dxBodyFlagFiniteRotationAxis = 2;      // use finite rotations only along axis
    static final int dxBodyDisabled = 4;                            // body is disabled
    static final int dxBodyNoGravity = 8;                           // body is not influenced by gravity
    static final int dxBodyAutoDisable = 16;                        // enable auto-disable on body
    static final int dxBodyLinearDamping = 32;                      // using linear damping
    static final int dxBodyAngularDamping = 64;                     // use angular damping
    static final int dxBodyMaxAngularSpeed = 128;                   // use maximum angular speed
    private static final int dxBodyGyroscopic = 256;                // use gyroscopic term

    public final Ref<DxJointNode> firstjoint = new Ref<>();         // list of attached joints

    int flags;                              // some dxBodyFlagXXX flags
    public DxGeom geom;                     // first collision geom associated with body
    DxMass mass;                            // mass parameters about POR
    DMatrix3 invI;                          // inverse of mass.I
    public double invMass;                  // 1 / mass.mass
    public DxPosR _posr;                    // position and orientation of point of reference
    public DQuaternion _q;                  // orientation quaternion
    public DVector3 lvel;                   // linear and angular velocity of POR
    public DVector3 avel;
    DVector3 facc, tacc;                    // force and torque accumulators
    DVector3 finite_rot_axis;               // finite rotation axis, unit length or 0=none

    // auto-disable information
    dxAutoDisable adis;                     // auto-disable parameters
    double adis_timeleft;                   // time left to be idle
    int adis_stepsleft;                     // steps left to be idle

    DVector3[] average_lvel_buffer;         // buffer for the linear average velocity calculation
    DVector3[] average_avel_buffer;         // buffer for the angular average velocity calculation
    //unsigned
    int average_counter;                    // counter/index to fill the average-buffers
    int average_ready;                      // indicates ( with = 1 ), if the Body's buffers are ready for average-calculations

    BodyMoveCallBack moved_callback;        // let the user know the body moved
    private dxDampingParameters dampingp;   // damping parameters, depends on flags
    double max_angular_speed;               // limit the angular velocity to this magnitude

    protected DxBody(DxWorld w) {
        super(w);
    }


    DxWorld dBodyGetWorld() {
        return world;
    }

    public static DxBody dBodyCreate(DxWorld w) {
        dAASSERT(w);
        DxBody b = new DxBody(w);
        b.firstjoint.set(null);
        b.flags = 0;
        b.geom = null;
        b.average_lvel_buffer = null;
        b.average_avel_buffer = null;
        //TZ
        b.mass = new DxMass();
        b.mass.dMassSetParameters(1, 0, 0, 0, 1, 1, 1, 0, 0, 0);
        b.invI = new DMatrix3();

        b.invI.set00(1);
        b.invI.set11(1);
        b.invI.set22(1);
        b.invMass = 1;
        b._posr = new DxPosR();

        b._q = new DQuaternion();

        b._q.set(0, 1);
        b._posr.Rw().setIdentity();
        b.lvel = new DVector3();

        b.avel = new DVector3();

        b.facc = new DVector3();

        b.tacc = new DVector3();

        b.finite_rot_axis = new DVector3();

        addObjectToList(b, w.firstbody);
        w.nb++;

        // set auto-disable parameters
        b.average_avel_buffer = b.average_lvel_buffer = null; // no buffer at beginning
        b.dBodySetAutoDisableDefaults();    // must do this after adding to world
        b.adis_stepsleft = b.adis.idle_steps;
        b.adis_timeleft = b.adis.idle_time;
        b.average_counter = 0;
        b.average_ready = 0; // average buffer not filled on the beginning
        b.dBodySetAutoDisableAverageSamplesCount(b.adis.average_samples);

        b.moved_callback = null;

        b.dBodySetDampingDefaults();    // must do this after adding to world

        b.flags |= w.body_flags & dxBodyMaxAngularSpeed;
        b.max_angular_speed = w.max_angular_speed;

        b.flags |= dxBodyGyroscopic;

        return b;
    }

    public void dBodyDestroy() {
        //dAASSERT (b);

        // all geoms that link to this body must be notified that the body is about
        // to disappear. note that the call to dGeomSetBody(geom,0) will result in
        // dGeomGetBodyNext() returning 0 for the body, so we must get the next body
        // before setting the body to 0.
        DxGeom next_geom = null;
        for (DxGeom geom2 = geom; geom2 != null; geom2 = next_geom) {
            next_geom = geom2.dGeomGetBodyNext();
            geom2.dGeomSetBody(null);
        }

        // detach all neighbouring joints, then delete this body.
        DxJointNode n = firstjoint.get();
        while (n != null) {
            // sneaky trick to speed up removal of joint references (black magic)
            //TODO use equals?
            n.joint.node[(n == n.joint.node[0]) ? 1 : 0].body = null;

            DxJointNode next = n.next;
            n.next = null;
            n.joint.removeJointReferencesFromAttachedBodies();
            n = next;
        }
        removeObjectFromList();
        world.nb--;

        // delete the average buffers
        //TZ nothing to do
        DESTRUCTOR();
    }

    public void dBodySetData(Object data) {
        userdata = data;
    }

    public Object dBodyGetData() {
        return userdata;
    }

    public void dBodySetPosition(double x, double y, double z) {
        _posr.pos.set(x, y, z);

        // notify all attached geoms that this body has moved
        for (DxGeom geom2 = geom; geom2 != null; geom2 = geom2.dGeomGetBodyNext())
            geom2.dGeomMoved();
    }

    //TZ
    public void dBodySetPosition(DVector3C xyz) {
        _posr.pos.set(xyz);

        // notify all attached geoms that this body has moved
        for (DxGeom geom2 = geom; geom2 != null; geom2 = geom2.dGeomGetBodyNext())
            geom2.dGeomMoved();
    }

    public void dBodySetRotation(DMatrix3C R) {
        _posr.Rw().set(R);
        dOrthogonalizeR(_posr.Rw());
        dQfromR(_q, R);
        dNormalize4(_q);

        // notify all attached geoms that this body has moved
        for (DxGeom geom2 = geom; geom2 != null; geom2 = geom2.dGeomGetBodyNext()) {
            geom2.dGeomMoved();
        }
    }

    public void dBodySetQuaternion(DQuaternionC q) {
        //dAASSERT (q);
        _q.set(q);
        dNormalize4(_q);
        dRfromQ(_posr.Rw(), _q);

        // notify all attached geoms that this body has moved
        for (DxGeom geom2 = geom; geom2 != null; geom2 = geom2.dGeomGetBodyNext())
            geom2.dGeomMoved();
    }

    public void dBodySetLinearVel(double x, double y, double z) {
        lvel.set(x, y, z);
    }

    public void dBodySetLinearVel(DVector3C xyz) {
        lvel.set(xyz);
    }


    public void dBodySetAngularVel(double x, double y, double z) {
        avel.set(x, y, z);
    }

    public void dBodySetAngularVel(DVector3C xyz) {
        avel.set(xyz);
    }

    public DVector3C dBodyGetPosition() {
        return _posr.pos();
    }

    void dBodyCopyPosition(DxBody b, DVector3 pos) {
        // dAASSERT (b);
        pos.set(b._posr.pos());
    }

    public DMatrix3C dBodyGetRotation() {
        return _posr.R();
    }

    void dBodyCopyRotation(DxBody b, DMatrix3 R) {
        // dAASSERT (b);
        //TODO clean up commented code and other TODO below (add(f), e.t.c)
        R.set(b._posr.R());
    }

    public DQuaternionC dBodyGetQuaternion() {
        return _q;
    }

    void dBodyCopyQuaternion(DxBody b, DQuaternion quat) {
        quat.set(b._q);
    }

    public DVector3C dBodyGetLinearVel() {
        return lvel;
    }

    public DVector3C dBodyGetAngularVel() {
        return avel;
    }

    public void dBodySetMass(DMassC mass2) {
        //dAASSERT (mass2 );
        dIASSERT(mass2.check());

        // The centre of mass must be at the origin.
        // Use dMassTranslate( mass, -mass->c[0], -mass->c[1], -mass->c[2] )
        // to correct it.
        DVector3C mass2c = mass2.getC();
        dUASSERT(Math.abs(mass2c.get0()) <= DBL_EPSILON &&
                        Math.abs(mass2c.get1()) <= DBL_EPSILON &&
                        Math.abs(mass2c.get2()) <= DBL_EPSILON,
                "The centre of mass must be at the origin.");

        //memcpy (b.mass,mass,sizeof(dMass));
        mass.set(mass2);
        if (!dInvertPDMatrix(mass._I, invI)) {
            dDEBUGMSG("inertia must be positive definite!");
            invI.setIdentity();
        }
        invMass = dRecip(mass._mass);
    }

    public void dBodyGetMass(DxMass mass2)
    {
        mass2.set(mass);
    }

    public void dBodyAddForce(double fx, double fy, double fz) {
        facc.add(fx, fy, fz);
    }

    public void dBodyAddForce(DVector3C f) {
        facc.add(f);
    }

    public void dBodyAddTorque(double fx, double fy, double fz) {
        tacc.add(fx, fy, fz);
    }

    public void dBodyAddTorque(DVector3C f) {
        tacc.add(f);
    }

    void dBodyAddRelForce(DVector3C f) {
        DVector3 t2 = new DVector3();
        dMultiply0_331(t2, _posr.R(), f);
        facc.add(t2);
    }

    public void dBodyAddRelTorque(DVector3C f) {
        DVector3 t2 = new DVector3();
        dMultiply0_331(t2, _posr.R(), f);
        tacc.add(t2);
    }

    void dBodyAddForceAtPos(DVector3C f, DVector3C p) {
        facc.add(f);
        DVector3 q = p.reSub(_posr.pos());
        dAddVectorCross3(tacc, q, f);
    }

    void dBodyAddForceAtRelPos(DVector3C f, DVector3C prel) {
        DVector3 p = new DVector3();
        dMultiply0_331(p, _posr.R(), prel);
        facc.add(f);
        dAddVectorCross3(tacc, p, f);
    }


    void dBodyAddRelForceAtPos(DVector3C frel, DVector3C p) {
        DVector3 f = new DVector3();
        dMultiply0_331(f, _posr.R(), frel);
        facc.add(f);
        DVector3 q = p.reSub(_posr.pos());
        dAddVectorCross3(tacc, q, f);
    }

    void dBodyAddRelForceAtRelPos(DVector3C fRel, DVector3C pRel) {
        DVector3 f = new DVector3();
        DVector3 p = new DVector3();
        dMultiply0_331(f, _posr.R(), fRel);
        dMultiply0_331(p, _posr.R(), pRel);
        facc.add(f);
        dAddVectorCross3(tacc, p, f);
    }

    DVector3C dBodyGetForce() {
        //dAASSERT (b);
        return facc;
    }

    DVector3C dBodyGetTorque() {
        return tacc;
    }

    void dBodySetForce(double x, double y, double z) {
        facc.set(x, y, z);
    }

    void dBodySetForce(DVector3C xyz) {
        facc.set(xyz);
    }

    void dBodySetTorque(double x, double y, double z) {
        tacc.set(x, y, z);
    }

    void dBodySetTorque(DVector3C t) {
        tacc.set(t);
    }

    void dBodyGetRelPointPos(DVector3C prel, DVector3 result) {
        dMultiply0_331(result, _posr.R(), prel);
        result.add(_posr.pos());
    }

    public void dBodyGetRelPointVel(DVector3C prel, DVector3 result) {
        DVector3 p = new DVector3();
        dMultiply0_331(p, _posr.R(), prel);
        result.set(lvel);
        dAddVectorCross3(result, avel, p);
    }

    void dBodyGetPointVel(DVector3C prel, DVector3 result) {
        DVector3 p = new DVector3(prel).sub(_posr.pos());
        result.set(lvel);
        dAddVectorCross3(result, avel, p);
    }

    void dBodyGetPosRelPoint(DVector3C p, DVector3 result) {
        DVector3 prel = p.reSub(_posr.pos());
        dMultiply1_331(result, _posr.R(), prel);
    }

    void dBodyVectorToWorld(DVector3C p, DVector3 result) {
        dMultiply0_331(result, _posr.R(), p);
    }

    void dBodyVectorFromWorld(DVector3C p, DVector3 result) {
        dMultiply1_331(result, _posr.R(), p);
    }

    void dBodySetFiniteRotationMode(boolean mode) {
        flags &= ~(dxBodyFlagFiniteRotation | dxBodyFlagFiniteRotationAxis);
        if (mode) {
            flags |= dxBodyFlagFiniteRotation;
            if (finite_rot_axis.get0() != 0 || finite_rot_axis.get1() != 0 ||
                    finite_rot_axis.get2() != 0) {
                flags |= dxBodyFlagFiniteRotationAxis;
            }
        }
    }

    void dBodySetFiniteRotationAxis(DVector3C xyz) {
        finite_rot_axis.set(xyz);
        if (xyz.get0() != 0 || xyz.get1() != 0 || xyz.get2() != 0) {
            dNormalize3(finite_rot_axis);
            flags |= dxBodyFlagFiniteRotationAxis;
        } else {
            flags &= ~dxBodyFlagFiniteRotationAxis;
        }
    }

    boolean dBodyGetFiniteRotationMode() {
        return (flags & dxBodyFlagFiniteRotation) != 0;
    }

    void dBodyGetFiniteRotationAxis(DVector3 result) {
        result.set(finite_rot_axis);
    }

    int dBodyGetNumJoints() {
        int count = 0;
        //TODO return array size
        for (DxJointNode n = firstjoint.get(); n != null; n = n.next, count++) ;
        return count;
    }

    DxJoint dBodyGetJoint(int index) {
        int i = 0;
        //TODO return array size
        for (DxJointNode n = firstjoint.get(); n != null; n = n.next, i++) {
            if (i == index) return n.joint;
        }
        return null;
    }

    void dBodySetDynamic() {
        dBodySetMass(mass);
    }

    void dBodySetKinematic() {
        invI.setZero();
        invMass = 0;
    }

    boolean dBodyIsKinematic() {
        return invMass == 0;
    }

    public void dBodyEnable() {
        flags &= ~dxBodyDisabled;
        adis_stepsleft = adis.idle_steps;
        adis_timeleft = adis.idle_time;
        // no code for average-processing needed here
    }

    /**
     * flags &= ~dxBodyDisabled.
     */
    //(TZ)
    public void dBodyEnable_noAdis() {
        flags &= ~dxBodyDisabled;
    }

    public void dBodyDisable() {
        flags |= dxBodyDisabled;
    }


    /**
     * @return (flags & dxBodyDisabled) == 0
     */
    public boolean dBodyIsEnabled() {
        return ((flags & dxBodyDisabled) == 0);
    }

    void dBodySetGravityMode(boolean mode) {
        if (mode) flags &= ~dxBodyNoGravity;
        else flags |= dxBodyNoGravity;
    }

    /**
     * @return (flags & dxBodyNoGravity) == 0
     */
    boolean dBodyGetGravityMode() {
        return (flags & dxBodyNoGravity) == 0;
    }


    // body auto-disable functions

    double dBodyGetAutoDisableLinearThreshold() {
        return dSqrt(adis.linear_average_threshold);
    }


    void dBodySetAutoDisableLinearThreshold(double linear_average_threshold) {
        adis.linear_average_threshold = linear_average_threshold * linear_average_threshold;
    }

    double dBodyGetAutoDisableAngularThreshold() {
        return dSqrt(adis.angular_average_threshold);
    }

    void dBodySetAutoDisableAngularThreshold(double angular_average_threshold) {
        adis.angular_average_threshold = angular_average_threshold * angular_average_threshold;
    }

    int dBodyGetAutoDisableAverageSamplesCount() {
        return adis.average_samples;
    }

    void dBodySetAutoDisableAverageSamplesCount(int average_samples_count) {
        adis.average_samples = average_samples_count;
        // update the average buffers
        if (average_lvel_buffer != null) {
            average_lvel_buffer = null;
        }
        if (average_avel_buffer != null) {
            average_avel_buffer = null;
        }
        if (adis.average_samples > 0) {
            average_lvel_buffer = DVector3.newArray(adis.average_samples);
            average_avel_buffer = DVector3.newArray(adis.average_samples);
        } else {
            average_lvel_buffer = null;
            average_avel_buffer = null;
        }
        // new buffer is empty
        average_counter = 0;
        average_ready = 0;
    }


    int dBodyGetAutoDisableSteps() {
        return adis.idle_steps;
    }


    void dBodySetAutoDisableSteps(int steps) {
        adis.idle_steps = steps;
    }


    double dBodyGetAutoDisableTime() {
        return adis.idle_time;
    }


    void dBodySetAutoDisableTime(double time) {
        adis.idle_time = time;
    }


    boolean dBodyGetAutoDisableFlag() {
        return ((flags & dxBodyAutoDisable) != 0);
    }

    void dBodySetAutoDisableFlag(boolean do_auto_disable) {
        if (!do_auto_disable) {
            flags &= ~dxBodyAutoDisable;
            // (mg) we should also reset the IsDisabled state to correspond to the DoDisabling flag
            flags &= ~dxBodyDisabled;
            adis.idle_steps = world.getAutoDisableSteps();
            adis.idle_time = world.getAutoDisableTime();
            // resetting the average calculations too
            dBodySetAutoDisableAverageSamplesCount(
                    world.getAutoDisableAverageSamplesCount());
        } else {
            flags |= dxBodyAutoDisable;
        }
    }


    void dBodySetAutoDisableDefaults() {
        DxWorld w = world;
        adis = w.adis.clone();
        dBodySetAutoDisableFlag((w.body_flags & dxBodyAutoDisable) != 0);
    }


    // body damping functions

    double dBodyGetLinearDamping() {
        return dampingp.linear_scale;
    }

    void dBodySetLinearDamping(double scale) {
        if (scale != 0)
            flags |= dxBodyLinearDamping;
        else
            flags &= ~dxBodyLinearDamping;
        dampingp.linear_scale = scale;
    }

    double dBodyGetAngularDamping() {
        return dampingp.angular_scale;
    }

    void dBodySetAngularDamping(double scale) {
        if (scale != 0)
            flags |= dxBodyAngularDamping;
        else
            flags &= ~dxBodyAngularDamping;
        dampingp.angular_scale = scale;
    }

    void dBodySetDamping(double linear_scale, double angular_scale) {
        dBodySetLinearDamping(linear_scale);
        dBodySetAngularDamping(angular_scale);
    }

    double dBodyGetLinearDampingThreshold() {
        return dSqrt(dampingp.linear_threshold);
    }

    void dBodySetLinearDampingThreshold(double threshold) {
        dampingp.linear_threshold = threshold * threshold;
    }


    double dBodyGetAngularDampingThreshold() {
        return dSqrt(dampingp.angular_threshold);
    }

    void dBodySetAngularDampingThreshold(double threshold) {
        dampingp.angular_threshold = threshold * threshold;
    }

    void dBodySetDampingDefaults() {
        DxWorld w = world;
        dampingp = w.dampingp.clone();
        //unsigned
        final int mask = dxBodyLinearDamping | dxBodyAngularDamping;
        flags &= ~mask; // zero them
        flags |= w.body_flags & mask;
    }

    double dBodyGetMaxAngularSpeed() {
        return max_angular_speed;
    }

    void dBodySetMaxAngularSpeed(double max_speed) {
        if (max_speed < Double.MAX_VALUE)
            flags |= dxBodyMaxAngularSpeed;
        else
            flags &= ~dxBodyMaxAngularSpeed;
        max_angular_speed = max_speed;
    }

    public void dBodySetMovedCallback(BodyMoveCallBack callback) {
        moved_callback = callback;
    }

    DxGeom dBodyGetFirstGeom() {
        //			dAASSERT(b);
        return geom;
    }

    //TODO check calls for invalid geom (not of this body)
    DxGeom dBodyGetNextGeom(DxGeom geom) {
        //dAASSERT(geom);
        return geom.dGeomGetBodyNext();
    }

    boolean dBodyGetGyroscopicMode() {
        return (flags & dxBodyGyroscopic) != 0;
    }

    void dBodySetGyroscopicMode(boolean enabled) {
        if (enabled)
            flags |= dxBodyGyroscopic;
        else
            flags &= ~dxBodyGyroscopic;
    }


    //****************************************************************************
    // body rotation

    // return sin(x)/x. this has a singularity at 0 so special handling is needed
    // for small arguments.

    private static double sinc(double x) {
        // if |x| < 1e-4 then use a taylor series expansion. this two term expansion
        // is actually accurate to one LS bit within this range if double precision
        // is being used - so don't worry!
        if (dFabs(x) < 1.0e-4) return 1.0 - x * x * 0.166666666666666666667;
        else return dSin(x) / x;
    }

    // given a body b, apply its linear and angular rotation over the time
    // interval h, thereby adjusting its position and orientation.
    void dxStepBody(double h) {
        // cap the angular velocity
        if ((flags & dxBodyMaxAngularSpeed) != 0) {
            final double max_ang_speed = max_angular_speed;
            final double aspeed = dCalcVectorDot3(avel, avel);
            if (aspeed > max_ang_speed * max_ang_speed) {
                final double coef = max_ang_speed / dSqrt(aspeed);
                avel.scale(coef);//dOPEC(avel.v, OP.MUL_EQ /* *= */, coef);
            }
        }
        // end of angular velocity cap

        // handle linear velocity
        //for (j=0; j<3; j++) _posr.pos.v[j] += h * lvel.v[j];
        _posr.pos.eqSum(_posr.pos(), lvel, h);

        if ((flags & dxBodyFlagFiniteRotation) != 0) {
            DVector3 irv = new DVector3();    // infitesimal rotation vector
            DQuaternion q = new DQuaternion();    // quaternion for finite rotation

            if ((flags & dxBodyFlagFiniteRotationAxis) != 0) {
                // split the angular velocity vector into a component along the finite
                // rotation axis, and a component orthogonal to it.
                DVector3 frv = new DVector3();        // finite rotation vector
                double k = dCalcVectorDot3(finite_rot_axis, avel);
                frv.set(finite_rot_axis).scale(k);
                irv.eqDiff(avel, frv);

                // make a rotation quaternion q that corresponds to frv * h.
                // compare this with the full-finite-rotation case below.
                h *= 0.5;
                double theta = k * h;
                double s = sinc(theta) * h;
                q.set(dCos(theta), frv.get0() * s, frv.get1() * s, frv.get2() * s);
            } else {
                // make a rotation quaternion q that corresponds to w * h
                double wlen = avel.length();
                h *= 0.5;
                double theta = wlen * h;
                double s = sinc(theta) * h;
                q.set(dCos(theta), avel.get0() * s, avel.get1() * s, avel.get2() * s);
            }

            // do the finite rotation
            DQuaternion q2 = new DQuaternion();
            dQMultiply0(q2, q, _q);

            _q.set(q2);

            // do the infitesimal rotation if required
            if ((flags & dxBodyFlagFiniteRotationAxis) != 0) {
                DQuaternion dq = new DQuaternion();
                dDQfromW(dq, irv, _q);
                _q.sum(_q, dq, h);
            }
        } else {
            // the normal way - do an infitesimal rotation
            DQuaternion dq = new DQuaternion();
            dDQfromW(dq, avel, _q);
            _q.sum(_q, dq, h);
        }
        // normalize the quaternion and convert it to a rotation matrix
        dNormalize4(_q);
        dRfromQ(_posr.Rw(), _q);
        // notify all attached geoms that this body has moved
        DxWorldProcessContext world_process_context = world.UnsafeGetWorldProcessingContext();
        for (DxGeom geom2 = geom; geom2 != null; geom2 = geom2.dGeomGetBodyNext()) {
            world_process_context.LockForStepbodySerialization();
            geom2.dGeomMoved();
            world_process_context.UnlockForStepbodySerialization();
        }
        // notify the user
        if (moved_callback != null)
            moved_callback.run(this);
        // damping
        if ((flags & dxBodyLinearDamping) != 0) {
            final double lin_threshold = dampingp.linear_threshold;
            final double lin_speed = dCalcVectorDot3(lvel, lvel);
            if (lin_speed > lin_threshold) {
                final double k = 1 - dampingp.linear_scale;
                lvel.scale(k);
            }
        }
        if ((flags & dxBodyAngularDamping) != 0) {
            final double ang_threshold = dampingp.angular_threshold;
            final double ang_speed = dCalcVectorDot3(avel, avel);
            if (ang_speed > ang_threshold) {
                final double k = 1 - dampingp.angular_scale;
                avel.scale(k);
            }
        }
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public String toString() {
        return super.toString();
    }

    public DxPosRC posr() {
        return _posr;
    }


    // ******************************************************
    // dBody API
    // ******************************************************

    //~dBody()
    @Override
    public void DESTRUCTOR() {
        super.DESTRUCTOR();
    }

    @Override
    public void setData(Object data) {
        dBodySetData(data);
    }

    @Override
    public Object getData() {
        return dBodyGetData();
    }

    @Override
    public void setPosition(double x, double y, double z) {
        dBodySetPosition(x, y, z);
    }

    @Override
    public void setPosition(DVector3C p) {
        dBodySetPosition(p);
    }

    @Override
    public void setRotation(DMatrix3C R) {
        dBodySetRotation(R);
    }

    @Override
    public void setQuaternion(DQuaternionC q) {
        dBodySetQuaternion(q);
    }

    @Override
    public void setLinearVel(double x, double y, double z) {
        dBodySetLinearVel(x, y, z);
    }

    @Override
    public void setLinearVel(DVector3C v) {
        dBodySetLinearVel(v);
    }

    @Override
    public void setAngularVel(double x, double y, double z) {
        dBodySetAngularVel(x, y, z);
    }

    @Override
    public void setAngularVel(DVector3C v) {
        dBodySetAngularVel(v);
    }

    @Override
    public DVector3C getPosition() {
        return dBodyGetPosition();
    }

    @Override
    public DMatrix3C getRotation() //const
    {
        return dBodyGetRotation();
    }

    @Override
    public DQuaternionC getQuaternion() //const
    {
        return dBodyGetQuaternion();
    }

    @Override
    public DVector3C getLinearVel() //const
    {
        return dBodyGetLinearVel();
    }

    @Override
    public DVector3C getAngularVel() //const
    {
        return dBodyGetAngularVel();
    }

    @Override
    public void setMass(DMassC mass) {
        dBodySetMass(mass);
    }

    @Override
    public DMass getMass() //const
    {
        DMass mass = new DxMass();
        dBodyGetMass((DxMass) mass);
        return mass;
    }

    @Override
    public void addForce(double fx, double fy, double fz) {
        dBodyAddForce(fx, fy, fz);
    }

    @Override
    public void addForce(DVector3C f) {
        dBodyAddForce(f);
    }

    @Override
    public void addTorque(double fx, double fy, double fz) {
        dBodyAddTorque(fx, fy, fz);
    }

    @Override
    public void addTorque(DVector3C t) {
        dBodyAddTorque(t);
    }

    @Override
    public void addRelForce(double fx, double fy, double fz) {
        dBodyAddRelForce(new DVector3(fx, fy, fz));
    }

    @Override
    public void addRelForce(DVector3C f) {
        dBodyAddRelForce(f);
    }

    @Override
    public void addRelTorque(double fx, double fy, double fz) {
        dBodyAddRelTorque(new DVector3(fx, fy, fz));
    }

    @Override
    public void addRelTorque(DVector3C t) {
        dBodyAddRelTorque(t);
    }

    @Override
    public void addForceAtPos(double fx, double fy, double fz,
                              double px, double py, double pz) {
        dBodyAddForceAtPos(new DVector3(fx, fy, fz), new DVector3(px, py, pz));
    }

    @Override
    public void addForceAtPos(DVector3C f, DVector3C p) {
        dBodyAddForceAtPos(f, p);
    }

    @Override
    public void addForceAtRelPos(double fx, double fy, double fz,
                                 double px, double py, double pz) {
        dBodyAddForceAtRelPos(new DVector3(fx, fy, fz), new DVector3(px, py, pz));
    }

    @Override
    public void addForceAtRelPos(DVector3C f, DVector3C p) {
        dBodyAddForceAtRelPos(f, p);
    }

    @Override
    public void addRelForceAtPos(double fx, double fy, double fz,
                                 double px, double py, double pz) {
        dBodyAddRelForceAtPos(new DVector3(fx, fy, fz), new DVector3(px, py, pz));
    }

    @Override
    public void addRelForceAtPos(DVector3C f, DVector3C p) {
        dBodyAddRelForceAtPos(f, p);
    }

    @Override
    public void addRelForceAtRelPos(double fx, double fy, double fz,
                                    double px, double py, double pz) {
        dBodyAddRelForceAtRelPos(new DVector3(fx, fy, fz), new DVector3(px, py, pz));
    }

    @Override
    public void addRelForceAtRelPos(DVector3C f, DVector3C p) {
        dBodyAddRelForceAtRelPos(f, p);
    }

    @Override
    public DVector3C getForce() //const
    {
        return dBodyGetForce();
    }

    @Override
    public DVector3C getTorque() //const
    {
        return dBodyGetTorque();
    }

    @Override
    public void setForce(double x, double y, double z) {
        dBodySetForce(x, y, z);
    }

    @Override
    public void setForce(DVector3C f) {
        dBodySetForce(f);
    }

    @Override
    public void setTorque(double x, double y, double z) {
        dBodySetTorque(x, y, z);
    }

    @Override
    public void setTorque(DVector3C t) {
        dBodySetTorque(t);
    }

    @Override
    public void setDynamic() {
        dBodySetDynamic();
    }

    @Override
    public void setKinematic() {
        dBodySetKinematic();
    }

    @Override
    public boolean isKinematic() {
        return dBodyIsKinematic();
    }

    @Override
    public void enable() {
        dBodyEnable();
    }

    @Override
    public void disable() {
        dBodyDisable();
    }

    @Override
    public boolean isEnabled() //const
    {
        return dBodyIsEnabled();
    }

    //TZ
    public boolean isFlagsGyroscopic() {
        return (flags & dxBodyGyroscopic) != 0;
    }

    @Override
    public void getRelPointPos(double px, double py, double pz, DVector3 result) //const
    {
        dBodyGetRelPointPos(new DVector3(px, py, pz), result);
    }

    @Override
    public void getRelPointPos(DVector3C p, DVector3 result) //const
    {
        dBodyGetRelPointPos(p, result);
    }

    @Override
    public void getRelPointVel(double px, double py, double pz, DVector3 result) //const
    {
        dBodyGetRelPointVel(new DVector3(px, py, pz), result);
    }

    @Override
    public void getRelPointVel(DVector3C p, DVector3 result) //const
    {
        dBodyGetRelPointVel(p, result);
    }

    @Override
    public void getPointVel(double px, double py, double pz, DVector3 result) //const
    {
        dBodyGetPointVel(new DVector3(px, py, pz), result);
    }

    @Override
    public void getPointVel(DVector3C p, DVector3 result) //const
    {
        dBodyGetPointVel(p, result);
    }

    @Override
    public void getPosRelPoint(double px, double py, double pz, DVector3 result) //const
    {
        dBodyGetPosRelPoint(new DVector3(px, py, pz), result);
    }

    @Override
    public void getPosRelPoint(DVector3C p, DVector3 result) //const
    {
        dBodyGetPosRelPoint(p, result);
    }

    @Override
    public void vectorToWorld(double px, double py, double pz, DVector3 result) //const
    {
        dBodyVectorToWorld(new DVector3(px, py, pz), result);
    }

    @Override
    public void vectorToWorld(DVector3C p, DVector3 result) //const
    {
        dBodyVectorToWorld(p, result);
    }

    @Override
    public void vectorFromWorld(double px, double py, double pz, DVector3 result) //const
    {
        dBodyVectorFromWorld(new DVector3(px, py, pz), result);
    }

    @Override
    public void vectorFromWorld(DVector3C p, DVector3 result) //const
    {
        dBodyVectorFromWorld(p, result);
    }

    @Override
    public void setFiniteRotationMode(boolean mode) {
        dBodySetFiniteRotationMode(mode);
    }

    @Override
    public void setFiniteRotationAxis(double x, double y, double z) {
        dBodySetFiniteRotationAxis(new DVector3(x, y, z));
    }

    @Override
    public void setFiniteRotationAxis(DVector3C a) {
        dBodySetFiniteRotationAxis(a);
    }

    @Override
    public boolean getFiniteRotationMode() //const
    {
        return dBodyGetFiniteRotationMode();
    }

    @Override
    public void getFiniteRotationAxis(DVector3 result) //const
    {
        dBodyGetFiniteRotationAxis(result);
    }

    @Override
    public int getNumJoints() //const
    {
        return dBodyGetNumJoints();
    }

    @Override
    public DJoint getJoint(int index) //const
    {
        return dBodyGetJoint(index);
    }

    @Override
    public void setGravityMode(boolean mode) {
        dBodySetGravityMode(mode);
    }

    /**
     * @see DxBody#dBodyGetGravityMode()
     */
    @Override
    public boolean getGravityMode() {
        return dBodyGetGravityMode();
    }

    @Override
    public void setGyroscopicMode(boolean mode) {
        dBodySetGyroscopicMode(mode);
    }

    @Override
    public boolean getGyroscopicMode() {
        return dBodyGetGyroscopicMode();
    }

    @Override
    public boolean isConnectedTo(DBody body) //const
    {
        return OdeJointsFactoryImpl.areConnected(this, body);
    }

    @Override
    public void setAutoDisableLinearThreshold(double threshold) {
        dBodySetAutoDisableLinearThreshold(threshold);
    }

    @Override
    public double getAutoDisableLinearThreshold() //const
    {
        return dBodyGetAutoDisableLinearThreshold();
    }

    @Override
    public void setAutoDisableAngularThreshold(double threshold) {
        dBodySetAutoDisableAngularThreshold(threshold);
    }

    @Override
    public double getAutoDisableAngularThreshold() //const
    {
        return dBodyGetAutoDisableAngularThreshold();
    }

    @Override
    public void setAutoDisableSteps(int steps) {
        dBodySetAutoDisableSteps(steps);
    }

    @Override
    public int getAutoDisableSteps() {
        return dBodyGetAutoDisableSteps();
    }

    @Override
    public void setAutoDisableTime(double time) {
        dBodySetAutoDisableTime(time);
    }

    @Override
    public double getAutoDisableTime() {
        return dBodyGetAutoDisableTime();
    }

    @Override
    public void setAutoDisableFlag(boolean do_auto_disable) {
        dBodySetAutoDisableFlag(do_auto_disable);
    }

    @Override
    public boolean getAutoDisableFlag() {
        return dBodyGetAutoDisableFlag();
    }

    @Override
    public double getLinearDamping() {
        return dBodyGetLinearDamping();
    }

    @Override
    public void setLinearDamping(double scale) {
        dBodySetLinearDamping(scale);
    }

    @Override
    public double getAngularDamping() {
        return dBodyGetAngularDamping();
    }

    @Override
    public void setAngularDamping(double scale) {
        dBodySetAngularDamping(scale);
    }

    @Override
    public void setDamping(double linear_scale, double angular_scale) {
        dBodySetDamping(linear_scale, angular_scale);
    }

    @Override
    public double getLinearDampingThreshold() {
        return dBodyGetLinearDampingThreshold();
    }

    @Override
    public void setLinearDampingThreshold(double threshold) {
        dBodySetLinearDampingThreshold(threshold);
    }

    @Override
    public double getAngularDampingThreshold() {
        return dBodyGetAngularDampingThreshold();
    }

    @Override
    public void setAngularDampingThreshold(double threshold) {
        dBodySetAngularDampingThreshold(threshold);
    }

    @Override
    public void setDampingDefaults() {
        dBodySetDampingDefaults();
    }

    @Override
    public double getMaxAngularSpeed() {
        return dBodyGetMaxAngularSpeed();
    }

    @Override
    public void setMaxAngularSpeed(double max_speed) {
        dBodySetMaxAngularSpeed(max_speed);
    }

    @Override
    public void destroy() {
        dBodyDestroy();
    }


    @Override
    public int getAutoDisableAverageSamplesCount() {
        return dBodyGetAutoDisableAverageSamplesCount();
    }


    @Override
    public void setAutoDisableAverageSamplesCount(int average_samples_count) {
        dBodySetAutoDisableAverageSamplesCount(average_samples_count);
    }


    @Override
    public void setAutoDisableDefaults() {
        dBodySetAutoDisableDefaults();
    }

    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public DGeom getFirstGeom() {
        return dBodyGetFirstGeom();
    }


    /**
     * @deprecated
     */
    @Deprecated
    @Override
    public DGeom getNextGeom(DGeom geom) {
        return dBodyGetNextGeom((DxGeom) geom);
    }

    @Override
    public void setMovedCallback(BodyMoveCallBack callback) {
        dBodySetMovedCallback(callback);
    }

}
