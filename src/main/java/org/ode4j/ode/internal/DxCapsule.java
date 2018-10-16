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

import org.ode4j.math.DMatrix3C;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DCapsule;
import org.ode4j.ode.DColliderFn;
import org.ode4j.ode.DContactGeom;
import org.ode4j.ode.DContactGeomBuffer;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.internal.cpp4j.java.RefDouble;
import org.ode4j.ode.internal.cpp4j.java.RefInt;

import static org.ode4j.ode.OdeMath.dCalcVectorDot3;
import static org.ode4j.ode.OdeMath.dCalcVectorDot3_14;
import static org.ode4j.ode.internal.Common.dAASSERT;
import static org.ode4j.ode.internal.Common.dDOUBLE;
import static org.ode4j.ode.internal.Common.dFabs;
import static org.ode4j.ode.internal.Common.dIASSERT;
import static org.ode4j.ode.internal.Common.dSqrt;

/**
 * standard ODE geometry primitives: public API and pairwise collision functions.
 * <p>
 * the rule is that only the low level primitive collision functions should set
 * dContactGeom::g1 and dContactGeom::g2.
 */
public class DxCapsule extends DxGeom implements DCapsule {
    private double _radius, _lz;    // radius, length along z axis

    //****************************************************************************
    // capped cylinder public API

    public DxCapsule(DSpace space, double __radius, double __length)
    {
        super(space, true);
        dAASSERT(__radius >= 0 && __length >= 0);
        type = dCapsuleClass;
        _radius = __radius;
        _lz = __length;
        //updateZeroSizedFlag(!_radius/* || !_length -- zero length capsule is not a zero sized capsule*/);
        updateZeroSizedFlag(__radius == 0.0);
    }


    @Override
    public void computeAABB() {
        final DMatrix3C R = final_posr().R();
        final DVector3C pos = final_posr().pos();
        double xrange = dFabs(R.get02() * _lz) * (0.5) + _radius;
        double yrange = dFabs(R.get12() * _lz) * (0.5) + _radius;
        double zrange = dFabs(R.get22() * _lz) * (0.5) + _radius;
        _aabb.setMinMax(xrange, yrange, zrange);
        _aabb.shiftPos(pos);
    }


    public static DxCapsule dCreateCapsule(DxSpace space, double radius, double length) {
        return new DxCapsule(space, radius, length);
    }

    public void dGeomCapsuleSetParams(double radius, double length) {
        _radius = radius;
        _lz = length;
        updateZeroSizedFlag(_radius == 0.0);
        dGeomMoved();
    }

    public double dGeomCapsulePointDepth(double x, double y, double z) {
        recomputePosr();

        final DMatrix3C R = final_posr().R();
        final DVector3C pos = final_posr().pos();

        DVector3 a = new DVector3(x, y, z);
        a.sub(pos);
        double beta = dCalcVectorDot3_14(a, R, 2);
        double lz2 = _lz * (0.5);
        if (beta < -lz2) beta = -lz2;
        else if (beta > lz2) beta = lz2;
        a.eqSum(final_posr().pos(), R.columnAsNewVector(2), beta);
        return _radius -
                dSqrt((x - a.get0()) * (x - a.get0()) + (y - a.get1()) * (y - a.get1()) + (z - a.get2()) * (z - a.get2()));
    }


    static class CollideCapsuleSphere implements DColliderFn {
        int dCollideCapsuleSphere(DxCapsule o1, DxSphere o2, int flags,
                                  DContactGeomBuffer contacts, int skip) {
            dIASSERT(skip == 1);
            dIASSERT((flags & NUMC_MASK) >= 1);

            DxCapsule ccyl = o1;
            DxSphere sphere = o2;

            contacts.get(0).g1 = o1;
            contacts.get(0).g2 = o2;
            contacts.get(0).side1 = -1;
            contacts.get(0).side2 = -1;

            // find the point on the cylinder axis that is closest to the sphere
            double alpha =
                    o1.final_posr().R().get02() * (o2.final_posr().pos().get0() - o1.final_posr().pos().get0()) +
                            o1.final_posr().R().get12() * (o2.final_posr().pos().get1() - o1.final_posr().pos().get1()) +
                            o1.final_posr().R().get22() * (o2.final_posr().pos().get2() - o1.final_posr().pos().get2());
            double lz2 = ccyl._lz * (0.5);
            if (alpha > lz2) alpha = lz2;
            if (alpha < -lz2) alpha = -lz2;

            // collide the spheres
            DVector3 p = new DVector3();
            p.eqSum(o1.final_posr().pos(), o1.final_posr().R().columnAsNewVector(2), alpha);
            return DxCollisionUtil.dCollideSpheres(p, ccyl._radius, o2.final_posr().pos(), sphere.getRadius(), contacts);
        }

        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags,
                               DContactGeomBuffer contacts) {
            return dCollideCapsuleSphere((DxCapsule) o1, (DxSphere) o2, flags, contacts, 1);
        }
    }

    static class CollideCapsuleBox implements DColliderFn {
        int dCollideCapsuleBox(DxCapsule o1, DxBox o2, int flags,
                               DContactGeomBuffer contacts, int skip) {
            dIASSERT(skip == 1);
            dIASSERT((flags & NUMC_MASK) >= 1);

            DxCapsule cyl = o1;
            DxBox box = o2;

            contacts.get(0).g1 = o1;
            contacts.get(0).g2 = o2;
            contacts.get(0).side1 = -1;
            contacts.get(0).side2 = -1;

            DVector3 p1 = new DVector3(), p2 = new DVector3();
            double clen = cyl._lz * (0.5);
            p1.eqSum(o1.final_posr().pos(), o1.final_posr().R().columnAsNewVector(2), clen);
            p2.eqSum(o1.final_posr().pos(), o1.final_posr().R().columnAsNewVector(2), -clen);
            double radius = cyl._radius;

            // copy out box center, rotation matrix, and side array
            DVector3C c = o2.final_posr().pos();
            DMatrix3C R = o2.final_posr().R();
            final DVector3 side = box.side;

            // get the closest point between the cylinder axis and the box
            DVector3 pl = new DVector3(), pb = new DVector3();
            DxCollisionUtil.dClosestLineBoxPoints(p1, p2, c, R, side, pl, pb);

            // if the capsule is penetrated further than radius
            // then pl and pb are equal (up to mindist) -> unknown normal
            // use normal vector of closest box surface
            double mindist;
            if (!dDOUBLE) {
                mindist = 1e-6;
            } else {
                mindist = 1e-15;
            }
            if (pl.distance(pb) < mindist) {
                // consider capsule as box
                DVector3 normal = new DVector3();
                RefDouble depth = new RefDouble();
                RefInt code = new RefInt();
                double radiusMul2 = radius * 2.0;
                DVector3C capboxside = new DVector3(radiusMul2, radiusMul2, cyl._lz + radiusMul2);
                int num = DxBox.dBoxBox(c, R, side,
                        o1.final_posr().pos(), o1.final_posr().R(), capboxside,
                        normal, depth, code, flags, contacts, skip);

                for (int i = 0; i < num; i++) {
                    DContactGeom cC = contacts.get(i * skip);
                    cC.normal.set(normal);
                    cC.g1 = o1;
                    cC.g2 = o2;
                    cC.side1 = -1;
                    cC.side2 = -1;
                }
                return num;
            } else {
                // generate contact point
                return DxCollisionUtil.dCollideSpheres(pl, radius, pb, 0., contacts);
            }
        }

        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags,
                               DContactGeomBuffer contacts) {
            return dCollideCapsuleBox((DxCapsule) o1, (DxBox) o2, flags, contacts, 1);
        }
    }

    static class CollideCapsuleCapsule implements DColliderFn {
        int dCollideCapsuleCapsule(DxCapsule cyl1, DxCapsule cyl2,
                                   int flags, DContactGeomBuffer contacts, int skip) {
            dIASSERT(skip == 1);
            dIASSERT((flags & NUMC_MASK) >= 1);

            final double tolerance = (1e-5);

            contacts.get(0).g1 = cyl1;
            contacts.get(0).g2 = cyl2;
            contacts.get(0).side1 = -1;
            contacts.get(0).side2 = -1;

            // copy out some variables, for convenience
            double lz1 = cyl1._lz * (0.5);
            double lz2 = cyl2._lz * (0.5);
            DVector3C pos1 = cyl1.final_posr().pos();
            DVector3C pos2 = cyl2.final_posr().pos();
            DVector3C axis1 = cyl1.final_posr().R().columnAsNewVector(2);
            DVector3 axis2 = cyl2.final_posr().R().columnAsNewVector(2);

            // if the cylinder axes are close to parallel, we'll try to detect up to
            // two contact points along the body of the cylinder. if we can't find any
            // points then we'll fall back to the closest-points algorithm. note that
            // we are not treating this special case for reasons of degeneracy, but
            // because we want two contact points in some situations. the closet-points
            // algorithm is robust in all casts, but it can return only one contact.

            DVector3 sphere1 = new DVector3(), sphere2 = new DVector3();
            double a1a2 = dCalcVectorDot3(axis1, axis2);
            double det = (1.0) - a1a2 * a1a2;
            if (det < tolerance) {
                // the cylinder axes (almost) parallel, so we will generate up to two
                // contacts. alpha1 and alpha2 (line position parameters) are related by:
                //       alpha2 =   alpha1 + (pos1-pos2)'*axis1   (if axis1==axis2)
                //    or alpha2 = -(alpha1 + (pos1-pos2)'*axis1)  (if axis1==-axis2)
                // first compute where the two cylinders overlap in alpha1 space:
                if (a1a2 < 0) {
                    axis2.scale(-1);
                }
                DVector3 q = new DVector3();
                q.set(pos1).sub(pos2);
                double k = dCalcVectorDot3(axis1, q);
                double a1lo = -lz1;
                double a1hi = lz1;
                double a2lo = -lz2 - k;
                double a2hi = lz2 - k;
                double lo = (a1lo > a2lo) ? a1lo : a2lo;
                double hi = (a1hi < a2hi) ? a1hi : a2hi;
                if (lo <= hi) {
                    int num_contacts = flags & NUMC_MASK;
                    if (num_contacts >= 2 && lo < hi) {
                        // generate up to two contacts. if one of those contacts is
                        // not made, fall back on the one-contact strategy.
                        sphere1.set(axis1).scale(lo).add(pos1);
                        sphere2.set(axis2).scale(lo + k).add(pos2);
                        int n1 = DxCollisionUtil.dCollideSpheres(sphere1, cyl1._radius,
                                sphere2, cyl2._radius, contacts);
                        if (n1 != 0) {
                            sphere1.set(axis1).scale(hi).add(pos1);
                            sphere2.set(axis2).scale(hi + k).add(pos2);
                            DContactGeomBuffer c2 = contacts.createView(skip);
                            int n2 = DxCollisionUtil.dCollideSpheres(sphere1, cyl1._radius,
                                    sphere2, cyl2._radius, c2);
                            if (n2 != 0) {
                                c2.get().g1 = cyl1;
                                c2.get().g2 = cyl2;
                                c2.get().side1 = -1;
                                c2.get().side2 = -1;
                                return 2;
                            }
                        }
                    }

                    // just one contact to generate, so put it in the middle of
                    // the range
                    double alpha1 = (lo + hi) * (0.5);
                    double alpha2 = alpha1 + k;
                    sphere1.set(axis1).scale(alpha1).add(pos1);
                    sphere2.set(axis2).scale(alpha2).add(pos2);
                    return DxCollisionUtil.dCollideSpheres(sphere1, cyl1._radius,
                            sphere2, cyl2._radius, contacts);
                }
            }

            // use the closest point algorithm
            DVector3 a1 = new DVector3(), a2 = new DVector3();
            DVector3 b1 = new DVector3(), b2 = new DVector3();
            a1.set(axis1).scale(lz1).add(cyl1.final_posr().pos());
            a2.set(axis1).scale(-lz1).add(cyl1.final_posr().pos());
            b1.set(axis2).scale(lz2).add(cyl2.final_posr().pos());
            b2.set(axis2).scale(-lz2).add(cyl2.final_posr().pos());

            DxCollisionUtil.dClosestLineSegmentPoints(a1, a2, b1, b2, sphere1, sphere2);
            return DxCollisionUtil.dCollideSpheres(sphere1, cyl1._radius, sphere2, cyl2._radius, contacts);
        }

        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags,
                               DContactGeomBuffer contacts) {
            return dCollideCapsuleCapsule((DxCapsule) o1, (DxCapsule) o2, flags, contacts, 1);
        }
    }

    static class CollideCapsulePlane implements DColliderFn {
        int dCollideCapsulePlane(DxCapsule o1, DxPlane o2, int flags,
                                 DContactGeomBuffer contacts, int skip) {
            dIASSERT(skip == 1);
            dIASSERT((flags & NUMC_MASK) >= 1);

            DxCapsule ccyl = o1;
            DxPlane plane = o2;
            DVector3C planePos = plane.getNormal();

            // collide the deepest capping sphere with the plane
            double sign = (planePos.dot(o1.final_posr().R().viewCol(2)) > 0) ? (-1.0) : (1.0);
            DVector3 p = o1.final_posr().R().columnAsNewVector(2);
            p.scale(ccyl._lz * 0.5 * sign).add(o1.final_posr().pos());

            double k = p.dot(planePos);//dDOT (p.v,plane._p);
            double depth = plane.getDepth() - k + ccyl._radius;
            if (depth < 0) return 0;
            DContactGeom contact = contacts.get(0);
            contact.normal.set(planePos);
            contact.pos.set(contact.normal).scale(-ccyl._radius).add(p);
            contact.depth = depth;

            int ncontacts = 1;
            if ((flags & NUMC_MASK) >= 2) {
                // collide the other capping sphere with the plane
                p = o1.final_posr().R().columnAsNewVector(2);
                p.scale(-ccyl._lz * 0.5 * sign).add(o1.final_posr().pos());

                k = p.dot(planePos);
                depth = plane.getDepth() - k + ccyl._radius;
                if (depth >= 0) {
                    DContactGeom c2 = contacts.get(skip);
                    c2.normal.set(planePos);
                    c2.pos.eqSum(p, planePos, -ccyl._radius);
                    c2.depth = depth;
                    ncontacts = 2;
                }
            }

            for (int i = 0; i < ncontacts; i++) {
                DContactGeom currContact = contacts.get(i * skip);
                currContact.g1 = o1;
                currContact.g2 = o2;
                currContact.side1 = -1;
                currContact.side2 = -1;
            }
            return ncontacts;
        }

        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags,
                               DContactGeomBuffer contacts) {
            return dCollideCapsulePlane((DxCapsule) o1, (DxPlane) o2, flags, contacts, 1);
        }
    }

    // *****************************************
    // API dCapsule
    // *****************************************

    @Override
    public void setParams(double radius, double length) {
        dGeomCapsuleSetParams(radius, length);
    }

    @Override
    public double getRadius() {
        return _radius;
    }

    @Override
    public double getLength() {
        return _lz;
    }

    @Override
    public double getPointDepth(DVector3C p) {
        return dGeomCapsulePointDepth(p.get0(), p.get1(), p.get2());
    }

}
