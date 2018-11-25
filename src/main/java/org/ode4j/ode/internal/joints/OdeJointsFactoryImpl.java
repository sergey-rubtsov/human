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
package org.ode4j.ode.internal.joints;

import org.ode4j.ode.DBody;
import org.ode4j.ode.DContact;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.internal.DxBody;
import org.ode4j.ode.internal.DxWorld;

import java.util.LinkedList;
import java.util.List;

import static org.ode4j.ode.internal.Common.dAASSERT;

/**
 * Factory for Joints.
 */
public class OdeJointsFactoryImpl extends OdeHelper {

    //****************************************************************************
    // joints
    private <T extends DxJoint> T createJoint(T j, DJointGroup group) {
        if (group != null) {
            ((DxJointGroup) group).addJoint(j);
        }
        return j;
    }

    public DxJointBall dJointCreateBall(DWorld w, DJointGroup group) {
        dAASSERT(w);
        return createJoint(new DxJointBall((DxWorld) w), group);
    }


    public DxJointHinge dJointCreateHinge(DWorld w, DJointGroup group) {
        dAASSERT(w);
        return createJoint(new DxJointHinge((DxWorld) w), group);
    }


    public DxJointSlider dJointCreateSlider(DWorld w, DJointGroup group) {
        dAASSERT(w);
        return createJoint(new DxJointSlider((DxWorld) w), group);
    }

    public DxJointContact dJointCreateContact(DWorld w, DJointGroup group,
                                              final DContact c) {
        dAASSERT(w, c);
        DxJointContact j = createJoint(new DxJointContact((DxWorld) w), group);
        j.contact = c;
        return j;
    }

    public DxJointHinge2 dJointCreateHinge2(DWorld w, DJointGroup group) {
        dAASSERT(w);
        return createJoint(new DxJointHinge2((DxWorld) w), group);
    }

    public DxJointUniversal dJointCreateUniversal(DWorld w, DJointGroup group) {
        dAASSERT(w);
        return createJoint(new DxJointUniversal((DxWorld) w), group);
    }

    public DxJointPR dJointCreatePR(DWorld w, DJointGroup group) {
        dAASSERT(w);
        return createJoint(new DxJointPR((DxWorld) w), group);
    }

    public DxJointPU dJointCreatePU(DWorld w, DJointGroup group) {
        dAASSERT(w);
        return createJoint(new DxJointPU((DxWorld) w), group);
    }

    public DxJointPiston dJointCreatePiston(DWorld w, DJointGroup group) {
        dAASSERT(w);
        return createJoint(new DxJointPiston((DxWorld) w), group);
    }

    public DxJointFixed dJointCreateFixed(DWorld id, DJointGroup group) {
        dAASSERT(id);
        return createJoint(new DxJointFixed((DxWorld) id), group);
    }


    public DxJointNull dJointCreateNull(DWorld w, DJointGroup group) {
        dAASSERT(w);
        return createJoint(new DxJointNull((DxWorld) w), group);
    }


    public DxJointAMotor dJointCreateAMotor(DWorld w, DJointGroup group) {
        dAASSERT(w);
        return createJoint(new DxJointAMotor((DxWorld) w), group);
    }

    public DxJointLMotor dJointCreateLMotor(DWorld w, DJointGroup group) {
        dAASSERT(w);
        return createJoint(new DxJointLMotor((DxWorld) w), group);
    }

    public DxJointPlane2D dJointCreatePlane2D(DWorld w, DJointGroup group) {
        dAASSERT(w);
        return createJoint(new DxJointPlane2D((DxWorld) w), group);
    }

    public DxJointDBall dJointCreateDBall(DWorld w, DJointGroup group) {
        dAASSERT(w);
        return createJoint(new DxJointDBall((DxWorld) w), group);
    }

    public DxJointDHinge dJointCreateDHinge(DWorld w, DJointGroup group) {
        dAASSERT(w);
        return createJoint(new DxJointDHinge((DxWorld) w), group);
    }

    public DxJointTransmission dJointCreateTransmission(DWorld w, DJointGroup group) {
        dAASSERT(w);
        return createJoint(new DxJointTransmission((DxWorld) w), group);
    }

    protected static void dJointDestroy(DxJoint j) {
        if ((j.flags & DxJoint.dJOINT_INGROUP) == 0) {
            j.FinalizeAndDestroyJointInstance(true);
        }
    }

    public void dJointAttach(DxJoint joint, DxBody body1, DxBody body2) {
        joint.dJointAttach(body1, body2);
    }

    public static DJoint dConnectingJoint(DBody in_b1, DBody in_b2) {
        dAASSERT(in_b1 != null || in_b2 != null);

        DxBody b1, b2;

        if (in_b1 == null) {
            b1 = (DxBody) in_b2;
            b2 = (DxBody) in_b1;
        } else {
            b1 = (DxBody) in_b1;
            b2 = (DxBody) in_b2;
        }

        // look through b1's neighbour list for b2
        for (DxJointNode n = b1.firstjoint.get(); n != null; n = n.next) {
            if (n.body == b2) return n.joint;
        }

        return null;
    }

    public static List<DJoint> dConnectingJointList(DxBody in_b1, DxBody in_b2) {
        dAASSERT(in_b1 != null || in_b2 != null);

        List<DJoint> out_list = new LinkedList<DJoint>();

        DxBody b1, b2;

        if (in_b1 == null) {
            b1 = in_b2;
            b2 = in_b1;
        } else {
            b1 = in_b1;
            b2 = in_b2;
        }

        // look through b1's neighbour list for b2
        //int numConnectingJoints = 0;
        for (DxJointNode n = b1.firstjoint.get(); n != null; n = n.next) {
            if (n.body == b2)
                //out_list[numConnectingJoints++] = n.joint;
                out_list.add(n.joint);
        }

        return out_list;//numConnectingJoints;
    }


    public boolean _dAreConnected(DBody b1, DBody b2) {
        //dAASSERT (b1!=null);// b2 can be null
        // look through b1's neighbour list for b2
        for (DxJointNode n = ((DxBody) b1).firstjoint.get(); n != null; n = n.next) {
            if (n.body == b2) return true;
        }
        return false;
    }


    public boolean _dAreConnectedExcluding(DBody b1, DBody b2, Class<? extends DJoint>[] jointType) {
        //dAASSERT (b1!=null);// b2 can be null
        // look through b1's neighbour list for b2
        for (DxJointNode n = ((DxBody) b1).firstjoint.get(); n != null; n = n.next) {
            if (n.body == b2) {
                boolean found = false;
                Class<?> clsJoint = n.joint.getClass();
                for (Class<?> cls : jointType) {
                    if (cls == clsJoint) {
                        found = true;
                        break;
                    }
                }
                if (!found) return true;
            }
        }
        return false;
    }
}
