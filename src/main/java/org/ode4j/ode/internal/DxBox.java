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
import org.ode4j.ode.DBox;
import org.ode4j.ode.DContactGeom;
import org.ode4j.ode.DContactGeomBuffer;
import org.ode4j.ode.internal.cpp4j.java.RefDouble;
import org.ode4j.ode.internal.cpp4j.java.RefInt;

import static org.ode4j.ode.OdeConstants.CONTACTS_UNIMPORTANT;
import static org.ode4j.ode.OdeConstants.dInfinity;
import static org.ode4j.ode.OdeMath.dCalcVectorDot3;
import static org.ode4j.ode.OdeMath.dCalcVectorDot3_14;
import static org.ode4j.ode.OdeMath.dCalcVectorDot3_41;
import static org.ode4j.ode.OdeMath.dCalcVectorDot3_44;
import static org.ode4j.ode.OdeMath.dMultiply0_331;
import static org.ode4j.ode.OdeMath.dMultiply1_331;
import static org.ode4j.ode.internal.Common.M_PI;
import static org.ode4j.ode.internal.Common.dAASSERT;
import static org.ode4j.ode.internal.Common.dAtan2;
import static org.ode4j.ode.internal.Common.dFabs;
import static org.ode4j.ode.internal.Common.dIASSERT;
import static org.ode4j.ode.internal.Common.dNODEBUG;
import static org.ode4j.ode.internal.Common.dRecip;
import static org.ode4j.ode.internal.Common.dSqrt;
import static org.ode4j.ode.internal.cpp4j.Cstring.memcpy;


/**
 * standard ODE geometry primitives: public API and pairwise collision functions.
 * <p>
 * the rule is that only the low level primitive collision functions should set
 * dContactGeom::g1 and dContactGeom::g2.
 */
public class DxBox extends DxGeom implements DBox {

    DVector3 side = new DVector3();    // side lengths (x,y,z)

    DxBox(DxSpace space, double lx, double ly, double lz)
    {
        super(space, true);
        dAASSERT(lx >= 0 && ly >= 0 && lz >= 0);
        type = dBoxClass;
        side.set(lx, ly, lz);
        updateZeroSizedFlag(lx == 0 || ly == 0 || lz == 0);
    }


    @Override
    void computeAABB() {
        DMatrix3C R = final_posr().R();
        DVector3C pos = final_posr().pos();

        double xrange = 0.5 * (dFabs(R.get00() * side.get0()) +
                dFabs(R.get01() * side.get1()) + dFabs(R.get02() * side.get2()));
        double yrange = 0.5 * (dFabs(R.get10() * side.get0()) +
                dFabs(R.get11() * side.get1()) + dFabs(R.get12() * side.get2()));
        double zrange = 0.5 * (dFabs(R.get20() * side.get0()) +
                dFabs(R.get21() * side.get1()) + dFabs(R.get22() * side.get2()));
        _aabb.set(pos.get0() - xrange,
                pos.get0() + xrange,
                pos.get1() - yrange,
                pos.get1() + yrange,
                pos.get2() - zrange,
                pos.get2() + zrange);
    }


    public static DxBox dCreateBox(DxSpace space, double lx, double ly, double lz) {
        return new DxBox(space, lx, ly, lz);
    }


    public void dGeomBoxSetLengths(DVector3C l) {
        dAASSERT(l.get0() >= 0 && l.get1() >= 0 && l.get2() >= 0);
        side.set(l);
        updateZeroSizedFlag(l.get0() == 0 || l.get1() == 0 || l.get2() == 0);
        dGeomMoved();
    }


    /**
     * Get the side lengths of a box.
     *
     * @param result the returned side lengths
     * @see #dGeomBoxSetLengths(DVector3C)
     */
    public void dGeomBoxGetLengths(DVector3 result) {
        result.set(side);
    }


    public double dGeomBoxPointDepth(double x, double y, double z) {
        recomputePosr();


        // Set p = (x,y,z) relative to box center
        //
        // This will be (0,0,0) if the point is at (side[0]/2,side[1]/2,side[2]/2)
        DVector3 p = new DVector3(), q = new DVector3();

        p.set(x, y, z).sub(final_posr().pos());
        // Rotate p into box's coordinate frame, so we can
        // treat the OBB as an AABB
        dMultiply1_331(q, final_posr().R(), p);

        // Record distance from point to each successive box side, and see
        // if the point is inside all six sides

        double[] dist = new double[6];
        int i;

        boolean inside = true;

        for (i = 0; i < 3; i++) {
            double sideX = side.get(i) * (0.5);

            dist[i] = sideX - q.get(i);
            dist[i + 3] = sideX + q.get(i);

            if ((dist[i] < 0) || (dist[i + 3] < 0)) {
                inside = false;
            }
        }

        // If point is inside the box, the depth is the smallest positive distance
        // to any side

        if (inside) {
            //TZ double smallest_dist = (double) (unsigned) -1;
            double smallest_dist = -1;

            for (i = 0; i < 6; i++) {
                if (dist[i] < smallest_dist) smallest_dist = dist[i];
            }

            return smallest_dist;
        }

        // Otherwise, if point is outside the box, the depth is the largest
        // distance to any side.  This is an approximation to the 'proper'
        // solution (the proper solution may be larger in some cases).

        double largest_dist = 0;

        for (i = 0; i < 6; i++) {
            if (dist[i] > largest_dist) largest_dist = dist[i];
        }

        return -largest_dist;
    }

    //****************************************************************************
    // box-box collision utility


    // find all the intersection points between the 2D rectangle with vertices
    // at (+/-h[0],+/-h[1]) and the 2D quadrilateral with vertices (p[0],p[1]),
    // (p[2],p[3]),(p[4],p[5]),(p[6],p[7]).
    //
    // the intersection points are returned as x,y pairs in the 'ret' array.
    // the number of intersection points is returned by the function (this will
    // be in the range 0 to 8).

    //static int intersectRectQuad (double h[2], double p[8], double ret[16])
    static int intersectRectQuad(double h[], double p[], double ret[]) {
        // q (and r) contain nq (and nr) coordinate points for the current (and
        // chopped) polygons
        int nq = 4, nr = 0;
        double[] buffer = new double[16];
        double[] r = ret;
        double[] q = p;
        int pos_q = 0; // p
        int pos_r = 0; // ret;
        for (int dir = 0; dir <= 1; dir++) {
            // direction notation: xy[0] = x axis, xy[1] = y axis
            for (int sign = -1; sign <= 1; sign += 2) {
                // chop q along the line xy[dir] = sign*h[dir]
                int pos_pq = pos_q;
                int pos_pr = pos_r;
                nr = 0;
                for (int i = nq; i > 0; i--) {
                    // go through all points in q and all lines between adjacent points
                    if (sign * q[pos_pq + dir] < h[dir]) {
                        // this point is inside the chopping line
                        r[pos_pr] = q[pos_pq];
                        r[pos_pr + 1] = q[pos_pq + 1];
                        pos_pr += 2;
                        nr++;
                        if ((nr & 8) != 0) {
                            q = r;  //TZ
                            pos_q = pos_r;
                            //TZ goto done;
                            if (q != ret) memcpy(ret, 0, q, pos_q, nr * 2);
                            return nr;
                        }
                    }

                    int next_q = (i > 1) ? pos_pq + 2 : 0;

                    if ((sign * q[pos_pq + dir] < h[dir]) ^ (sign * q[next_q + dir] < h[dir])) {
                        // this line crosses the chopping line
                        r[pos_pr + 1 - dir] = q[pos_pq + 1 - dir] +
                                (q[next_q + 1 - dir] - q[pos_pq + 1 - dir]) /
                                        (q[next_q + dir] - q[pos_pq + dir]) * (sign * h[dir] - q[pos_pq + dir]);
                        r[pos_pr + dir] = sign * h[dir];
                        pos_pr += 2;
                        nr++;
                        if ((nr & 8) != 0) {
                            q = r;
                            pos_q = pos_r; //TZ
                            //TZ goto done;
                            if (q != ret) memcpy(ret, 0, q, pos_q, nr * 2);
                            return nr;
                        }
                    }
                    pos_pq += 2;
                }
                q = r;
                pos_q = pos_r;  //TZ
                r = (q == ret) ? buffer : ret;
                pos_r = 0; //TZ
                nq = nr;
            }
        }
        //TZ	done:
        if (q != ret) memcpy(ret, 0, q, pos_q, nr * 2);
        return nr;
    }


    // given n points in the plane (array p, of size 2*n), generate m points that
    // best represent the whole set. the definition of 'best' here is not
    // predetermined - the idea is to select points that give good box-box
    // collision detection behavior. the chosen point indexes are returned in the
    // array iret (of size m). 'i0' is always the first entry in the array.
    // n must be in the range [1..8]. m must be in the range [1..n]. i0 must be
    // in the range [0..n-1].
    static void cullPoints(int n, double p[], int m, int i0, int iretA[]) {
        int iretP = 0;
        // compute the centroid of the polygon in cx,cy
        int i, j;
        double a, cx, cy, q;
        if (n == 1) {
            cx = p[0];
            cy = p[1];
        } else if (n == 2) {
            cx = (0.5) * (p[0] + p[2]);
            cy = (0.5) * (p[1] + p[3]);
        } else {
            a = 0;
            cx = 0;
            cy = 0;
            for (i = 0; i < (n - 1); i++) {
                q = p[i * 2] * p[i * 2 + 3] - p[i * 2 + 2] * p[i * 2 + 1];
                a += q;
                cx += q * (p[i * 2] + p[i * 2 + 2]);
                cy += q * (p[i * 2 + 1] + p[i * 2 + 3]);
            }
            q = p[n * 2 - 2] * p[1] - p[0] * p[n * 2 - 1];
            a = dRecip((3.0) * (a + q));
            cx = a * (cx + q * (p[n * 2 - 2] + p[0]));
            cy = a * (cy + q * (p[n * 2 - 1] + p[1]));
        }

        // compute the angle of each point w.r.t. the centroid
        double[] A = new double[8];
        for (i = 0; i < n; i++) A[i] = dAtan2(p[i * 2 + 1] - cy, p[i * 2] - cx);

        // search for points that have angles closest to A[i0] + i*(2*pi/m).
        int[] avail = new int[8];
        for (i = 0; i < n; i++) avail[i] = 1;
        avail[i0] = 0;
        iretA[iretP] = i0;
        iretP++;
        for (j = 1; j < m; j++) {
            a = j * (2. * M_PI / m) + A[i0];
            if (a > M_PI) a -= 2. * M_PI;
            double maxdiff = 1e9, diff;
            if (!dNODEBUG) {
                iretA[iretP] = i0;// iret is not allowed to keep this value
            }
            for (i = 0; i < n; i++) {
                if (avail[i] != 0) {
                    diff = dFabs(A[i] - a);
                    if (diff > M_PI) diff = 2. * M_PI - diff;
                    if (diff < maxdiff) {
                        maxdiff = diff;
                        iretA[iretP] = i;//*iret = i;
                    }
                }
            }
            if (!dNODEBUG) {
                dIASSERT(iretA[iretP] != i0);    // ensure iret got set
            }
            avail[iretA[iretP]] = 0;
            iretP++;
        }
    }

    private static boolean TST1(double expr1, double expr2, DMatrix3C norm_A, int norm_O, int cc,
                                TstClass tst1) {
        double expr1_val = (expr1); /* Avoid duplicate evaluation of expr1 */
        double s2 = dFabs(expr1_val) - (expr2);
        if (s2 > 0) return false;
        if (s2 > tst1._s) {
            tst1._s = s2;
            tst1._normalR_M = norm_A;
            tst1._normalR_col = norm_O;
            tst1._invert_normal = ((expr1_val) < 0);
            tst1._code = (cc);
            if ((tst1._flags & CONTACTS_UNIMPORTANT) != 0) {
                tst1._break = true;
                return true;
            }
        }
        return true;
    }

    private static boolean TST2(double expr1, double expr2, double n1, double n2, double n3, int cc,
                                TstClass tst2) {
        double expr1_val = (expr1); /* Avoid duplicate evaluation of expr1 */
        double s2 = dFabs(expr1_val) - (expr2);
        if (s2 > 0) return false;
        double l = dSqrt((n1) * (n1) + (n2) * (n2) + (n3) * (n3));
        if (l > 0) {
            s2 /= l;
            if (s2 * tst2._fudge_factor > tst2._s) {
                tst2._s = s2;
                tst2._normalR_M = null;
                tst2._normalR_col = 0;
                tst2._normalC.set((n1) / l, (n2) / l, (n3) / l);
                tst2._invert_normal = ((expr1_val) < 0);
                tst2._code = (cc);
                if ((tst2._flags & CONTACTS_UNIMPORTANT) != 0) {
                    tst2._break = true;
                    return true;
                }
            }
        }
        return true;
    }

    private static class TstClass {
        int _code;
        //TODO use RefInt?
        DMatrix3C _normalR_M;
        int _normalR_col;
        DVector3 _normalC = new DVector3();
        double _s;
        double _fudge_factor;
        final int _flags;
        boolean _invert_normal;

        TstClass(int flags, double fudge_factor) {
            _flags = flags;
            _fudge_factor = fudge_factor;
        }
        boolean _break = false;
    }

    /**
     * given two boxes (p1,R1,side1) and (p2,R2,side2), collide them together and
     * generate contact points. this returns 0 if there is no contact otherwise
     * it returns the number of contacts generated.		<br>
     * `normal' returns the contact normal.				<br>
     * `depth' returns the maximum penetration depth along that normal.	<br>
     * `return_code' returns a number indicating the type of contact that was
     * detected:<br>
     * 1,2,3 = box 2 intersects with a face of box 1		<br>
     * 4,5,6 = box 1 intersects with a face of box 2		<br>
     * 7..15 = edge-edge contact							<br>
     * `maxc' is the maximum number of contacts allowed to be generated, i.e.
     * the size of the `contact' array.		<br>
     * `contact' and `skip' are the contact array information provided to the
     * collision functions. this function only fills in the position and depth
     * fields.
     */
    public static int dBoxBox(final DVector3C p1, final DMatrix3C R1,
                              final DVector3C side1, final DVector3C p2,
                              final DMatrix3C R2, final DVector3C side2,
                              DVector3 normal, RefDouble depth, RefInt return_code,
                              int flags, DContactGeomBuffer contacts, int skip) {
        //TZ final double fudge_factor = (1.05);
        DVector3 p = new DVector3(), pp = new DVector3();
        DVector3 A = new DVector3(), B = new DVector3();
        double R11, R12, R13, R21, R22, R23, R31, R32, R33,
                Q11, Q12, Q13, Q21, Q22, Q23, Q31, Q32, Q33;
        int i, j;//,invert_normal;//,code;

        // get vector from centers of box 1 to box 2, relative to box 1
        p.eqDiff(p2, p1);
        dMultiply1_331(pp, R1, p);        // get pp = p relative to body 1

        // get side lengths / 2
        A.set(side1).scale(0.5);
        B.set(side2).scale(0.5);

        // Rij is R1'*R2, i.e. the relative rotation between R1 and R2
        R11 = dCalcVectorDot3_44(R1, 0, R2, 0);
        R12 = dCalcVectorDot3_44(R1, 0, R2, 1);
        R13 = dCalcVectorDot3_44(R1, 0, R2, 2);
        R21 = dCalcVectorDot3_44(R1, 1, R2, 0);
        R22 = dCalcVectorDot3_44(R1, 1, R2, 1);
        R23 = dCalcVectorDot3_44(R1, 1, R2, 2);
        R31 = dCalcVectorDot3_44(R1, 2, R2, 0);
        R32 = dCalcVectorDot3_44(R1, 2, R2, 1);
        R33 = dCalcVectorDot3_44(R1, 2, R2, 2);

        Q11 = dFabs(R11);
        Q12 = dFabs(R12);
        Q13 = dFabs(R13);
        Q21 = dFabs(R21);
        Q22 = dFabs(R22);
        Q23 = dFabs(R23);
        Q31 = dFabs(R31);
        Q32 = dFabs(R32);
        Q33 = dFabs(R33);


        // for all 15 possible separating axes:
        //   * see if the axis separates the boxes. if so, return 0.
        //   * find the depth of the penetration along the separating axis (s2)
        //   * if this is the largest depth so far, record it.
        // the normal vector will be set to the separating axis with the smallest
        // depth. note: normalR is set to point to a column of R1 or R2 if that is
        // the smallest depth normal so far. otherwise normalR is 0 and normalC is
        // set to a vector relative to body 1. invert_normal is 1 if the sign of
        // the normal should be flipped.
        TstClass tst = new TstClass(flags, 1.05);//fudge_factor);
        do {
            tst._s = -dInfinity;
            tst._invert_normal = false;
            tst._code = 0;

            // separating axis = u1,u2,u3
            if (!TST1(pp.get0(), (A.get0() + B.get0() * Q11 + B.get1() * Q12 + B.get2() * Q13), R1, 0, 1, tst))
                return 0;
            if (tst._break) break;
            if (!TST1(pp.get1(), (A.get1() + B.get0() * Q21 + B.get1() * Q22 + B.get2() * Q23), R1, 1, 2, tst))
                return 0;
            if (tst._break) break;
            if (!TST1(pp.get2(), (A.get2() + B.get0() * Q31 + B.get1() * Q32 + B.get2() * Q33), R1, 2, 3, tst))
                return 0;
            if (tst._break) break;

            // separating axis = v1,v2,v3
            if (!TST1(dCalcVectorDot3_41(R2, 0, p), (A.get0() * Q11 + A.get1() * Q21 + A.get2() * Q31 + B.get0()), R2, 0, 4, tst))
                return 0;
            if (tst._break) break;
            if (!TST1(dCalcVectorDot3_41(R2, 1, p), (A.get0() * Q12 + A.get1() * Q22 + A.get2() * Q32 + B.get1()), R2, 1, 5, tst))
                return 0;
            if (tst._break) break;
            if (!TST1(dCalcVectorDot3_41(R2, 2, p), (A.get0() * Q13 + A.get1() * Q23 + A.get2() * Q33 + B.get2()), R2, 2, 6, tst))
                return 0;
            if (tst._break) break;

            // note: cross product axes need to be scaled when s is computed.
            // normal (n1,n2,n3) is relative to box 1.
            // We only need to check 3 edges per box
            // since parallel edges are equivalent.

            // separating axis = u1 x (v1,v2,v3)
            if (!TST2(pp.get2() * R21 - pp.get1() * R31, (A.get1() * Q31 + A.get2() * Q21 + B.get1() * Q13 + B.get2() * Q12), 0, -R31, R21, 7, tst))
                return 0;
            if (tst._break) break;
            if (!TST2(pp.get2() * R22 - pp.get1() * R32, (A.get1() * Q32 + A.get2() * Q22 + B.get0() * Q13 + B.get2() * Q11), 0, -R32, R22, 8, tst))
                return 0;
            if (tst._break) break;
            if (!TST2(pp.get2() * R23 - pp.get1() * R33, (A.get1() * Q33 + A.get2() * Q23 + B.get0() * Q12 + B.get1() * Q11), 0, -R33, R23, 9, tst))
                return 0;
            if (tst._break) break;

            // separating axis = u2 x (v1,v2,v3)
            if (!TST2(pp.get0() * R31 - pp.get2() * R11, (A.get0() * Q31 + A.get2() * Q11 + B.get1() * Q23 + B.get2() * Q22), R31, 0, -R11, 10, tst))
                return 0;
            if (tst._break) break;
            if (!TST2(pp.get0() * R32 - pp.get2() * R12, (A.get0() * Q32 + A.get2() * Q12 + B.get0() * Q23 + B.get2() * Q21), R32, 0, -R12, 11, tst))
                return 0;
            if (tst._break) break;
            if (!TST2(pp.get0() * R33 - pp.get2() * R13, (A.get0() * Q33 + A.get2() * Q13 + B.get0() * Q22 + B.get1() * Q21), R33, 0, -R13, 12, tst))
                return 0;
            if (tst._break) break;

            // separating axis = u3 x (v1,v2,v3)
            if (!TST2(pp.get1() * R11 - pp.get0() * R21, (A.get0() * Q21 + A.get1() * Q11 + B.get1() * Q33 + B.get2() * Q32), -R21, R11, 0, 13, tst))
                return 0;
            if (tst._break) break;
            if (!TST2(pp.get1() * R12 - pp.get0() * R22, (A.get0() * Q22 + A.get1() * Q12 + B.get0() * Q33 + B.get2() * Q31), -R22, R12, 0, 14, tst))
                return 0;
            if (tst._break) break;
            if (!TST2(pp.get1() * R13 - pp.get0() * R23, (A.get0() * Q23 + A.get1() * Q13 + B.get0() * Q32 + B.get1() * Q31), -R23, R13, 0, 15, tst))
                return 0;
            if (tst._break) break;
            //	#undef TST
        } while (false);//(0);

        if (tst._code == 0) return 0;//(!code) return 0;

        // if we get to this point, the boxes interpenetrate. compute the normal
        // in global coordinates.
        if (tst._normalR_M != null) {
            normal.set(tst._normalR_M.viewCol(tst._normalR_col));
        } else {
            dMultiply0_331(normal, R1, tst._normalC);
        }
        if (tst._invert_normal) {
            normal.scale(-1);
        }
        depth.set(-tst._s);

        // compute contact point(s)
        if (tst._code > 6) {
            // An edge from box 1 touches an edge from box 2.
            // find a point pa on the intersecting edge of box 1
            double sign;
            // Copy p1 into pa
            // why no memcpy?
            DVector3 pa = new DVector3(p1);
            // Get world position of p2 into pa
            for (j = 0; j < 3; j++) {
                sign = (dCalcVectorDot3_14(normal, R1, j) > 0) ? (1.0) : (-1.0);
                for (i = 0; i < 3; i++) pa.add(i, sign * A.get(j) * R1.get(i, j));
            }

            // find a point pb on the intersecting edge of box 2
            // Copy p2 into pb
            // why no memcpy?
            DVector3 pb = new DVector3(p2);
            // Get world position of p2 into pb
            for (j = 0; j < 3; j++) {
                sign = (dCalcVectorDot3_14(normal, R2, j) > 0) ? (-1.0) : (1.0);

                for (i = 0; i < 3; i++) pb.add(i, sign * B.get(j) * R2.get(i, j));
            }
            RefDouble alpha = new RefDouble(0), beta = new RefDouble(0);
            DVector3 ua = new DVector3(), ub = new DVector3();
            // Get direction of first edge
            for (i = 0; i < 3; i++) ua.set(i, R1.get(i, (tst._code - 7) / 3));
            // Get direction of second edge
            for (i = 0; i < 3; i++) ub.set(i, R2.get(i, (tst._code - 7) % 3));
            // Get closest points between edges (one at each)
            DxCollisionUtil.dLineClosestApproach(pa, ua, pb, ub, alpha, beta);
            pa.eqSum(pa, ua, alpha.get());
            pb.eqSum(pb, ub, beta.get());
            // Set the contact point as halfway between the 2 closest points
            contacts.get(0).pos.eqSum(pa, pb).scale(0.5);
            contacts.get(0).depth = depth.get();
            return_code.set(tst._code);
            return 1;
        }

        // okay, we have a face-something intersection (because the separating
        // axis is perpendicular to a face). define face 'a' to be the reference
        // face (i.e. the normal vector is perpendicular to this) and face 'b' to be
        // the incident face (the closest face of the other box).
        // Note: Unmodified parameter values are being used here
        //final double *Ra,*Rb,*pa,*pb,*Sa,*Sb;
        DMatrix3C Ra, Rb;
        DVector3C pa, pb, Sa, Sb;
        if (tst._code <= 3) { // One of the faces of box 1 is the reference face
            Ra = R1; // Rotation of 'a'
            Rb = R2; // Rotation of 'b'
            pa = p1; // Center (location) of 'a'
            pb = p2; // Center (location) of 'b'
            Sa = A;  // Side Lenght of 'a'
            Sb = B;  // Side Lenght of 'b'
        } else { // One of the faces of box 2 is the reference face
            Ra = R2; // Rotation of 'a'
            Rb = R1; // Rotation of 'b'
            pa = p2; // Center (location) of 'a'
            pb = p1; // Center (location) of 'b'
            Sa = B;  // Side Lenght of 'a'
            Sb = A;  // Side Lenght of 'b'
        }

        // nr = normal vector of reference face dotted with axes of incident box.
        // anr = absolute values of nr.
        /* The normal is flipped if necessary so it always points outward from box 'a',
          box 'b' is thus always the incident box
         */

        DVector3 normal2 = new DVector3(), nr = new DVector3(), anr = new DVector3();
        if (tst._code <= 3) {
            normal2.set(normal);
        } else {
            normal2.set(normal).scale(-1);
        }
        // Rotate normal2 in incident box opposite direction
        dMultiply1_331(nr, Rb, normal2);
        anr.set(nr).eqAbs();

        // find the largest component of anr: this corresponds to the normal
        // for the incident face. the other axis numbers of the incident face
        // are stored in a1,a2.
        int lanr, a1, a2;
        if (anr.get1() > anr.get0()) {
            if (anr.get1() > anr.get2()) {
                a1 = 0;
                lanr = 1;
                a2 = 2;
            } else {
                a1 = 0;
                a2 = 1;
                lanr = 2;
            }
        } else {
            if (anr.get0() > anr.get2()) {
                lanr = 0;
                a1 = 1;
                a2 = 2;
            } else {
                a1 = 0;
                a2 = 1;
                lanr = 2;
            }
        }

        // compute center point of incident face, in reference-face coordinates
        DVector3 center = new DVector3();
        if (nr.get(lanr) < 0) {
            for (i = 0; i < 3; i++) center.set(i, pb.get(i) - pa.get(i) + Sb.get(lanr) * Rb.get(i, lanr));
        } else {
            for (i = 0; i < 3; i++) center.set(i, pb.get(i) - pa.get(i) - Sb.get(lanr) * Rb.get(i, lanr));
        }

        // find the normal and non-normal axis numbers of the reference box
        int codeN, code1, code2;
        if (tst._code <= 3) codeN = tst._code - 1;
        else codeN = tst._code - 4;
        if (codeN == 0) {
            code1 = 1;
            code2 = 2;
        } else if (codeN == 1) {
            code1 = 0;
            code2 = 2;
        } else {
            code1 = 0;
            code2 = 1;
        }

        // find the four corners of the incident face, in reference-face coordinates
        double[] quad = new double[8];    // 2D coordinate of incident face (x,y pairs)
        double c1, c2, m11, m12, m21, m22;
        c1 = dCalcVectorDot3_14(center, Ra, code1);
        c2 = dCalcVectorDot3_14(center, Ra, code2);
        // optimize this? - we have already computed this data above, but it is not
        // stored in an easy-to-index format. for now it's quicker just to recompute
        // the four dot products.
        m11 = dCalcVectorDot3_44(Ra, code1, Rb, a1);
        m12 = dCalcVectorDot3_44(Ra, code1, Rb, a2);
        m21 = dCalcVectorDot3_44(Ra, code2, Rb, a1);
        m22 = dCalcVectorDot3_44(Ra, code2, Rb, a2);
        {
            double k1 = m11 * Sb.get(a1);
            double k2 = m21 * Sb.get(a1);
            double k3 = m12 * Sb.get(a2);
            double k4 = m22 * Sb.get(a2);
            quad[0] = c1 - k1 - k3;
            quad[1] = c2 - k2 - k4;
            quad[2] = c1 - k1 + k3;
            quad[3] = c2 - k2 + k4;
            quad[4] = c1 + k1 + k3;
            quad[5] = c2 + k2 + k4;
            quad[6] = c1 + k1 - k3;
            quad[7] = c2 + k2 - k4;
        }

        // find the size of the reference face
        double[] rect = new double[2];
        rect[0] = Sa.get(code1);
        rect[1] = Sa.get(code2);

        // intersect the incident and reference faces
        double[] ret = new double[16];
        int n = intersectRectQuad(rect, quad, ret);
        if (n < 1) return 0;        // this should never happen

        // convert the intersection points into reference-face coordinates,
        // and compute the contact position and depth for each point. only keep
        // those points that have a positive (penetrating) depth. delete points in
        // the 'ret' array as necessary so that 'point' and 'ret' correspond.
        double[] point = new double[3 * 8];        // penetrating contact points
        double[] dep = new double[8];            // depths for those points
        double det1 = dRecip(m11 * m22 - m12 * m21);
        m11 *= det1;
        m12 *= det1;
        m21 *= det1;
        m22 *= det1;
        int cnum = 0; // number of penetrating contact points found
        for (j = 0; j < n; j++) {
            double k1 = m22 * (ret[j * 2] - c1) - m12 * (ret[j * 2 + 1] - c2);
            double k2 = -m21 * (ret[j * 2] - c1) + m11 * (ret[j * 2 + 1] - c2);
            for (i = 0; i < 3; i++)
                point[cnum * 3 + i] =
                        center.get(i) + k1 * Rb.get(i, a1) + k2 * Rb.get(i, a2);
            dep[cnum] = Sa.get(codeN) - dCalcVectorDot3(normal2, point, cnum * 3);
            if (dep[cnum] >= 0) {
                ret[cnum * 2] = ret[j * 2];
                ret[cnum * 2 + 1] = ret[j * 2 + 1];
                cnum++;
                if ((cnum | CONTACTS_UNIMPORTANT) == (flags & (NUMC_MASK | CONTACTS_UNIMPORTANT))) {
                    break;
                }
            }
        }
        if (cnum < 1) {
            return 0;    // this should not happen, yet does at times (demo_plane2d single precision).
        }

        // we can't generate more contacts than we actually have
        int maxc = flags & NUMC_MASK;
        if (maxc > cnum) maxc = cnum;
        // Even though max count must not be zero this check is kept for backward
        // compatibility as this is a public function
        if (maxc < 1) maxc = 1;

        if (cnum <= maxc) {
            // we have less contacts than we need, so we use them all
            for (j = 0; j < cnum; j++) {
                DContactGeom con = contacts.get(skip * j);
                for (i = 0; i < 3; i++) con.pos.set(i, point[j * 3 + i] + pa.get(i));
                con.depth = dep[j];
            }
        } else {
            // cnum should be generated not greater than maxc so that "then" clause is executed
            dIASSERT(0 == (flags & CONTACTS_UNIMPORTANT));
            // we have more contacts than are wanted, some of them must be culled.
            // find the deepest point, it is always the first contact.
            int i1 = 0;
            double maxdepth = dep[0];
            for (i = 1; i < cnum; i++) {
                if (dep[i] > maxdepth) {
                    maxdepth = dep[i];
                    i1 = i;
                }
            }

            int[] iret = new int[8];
            cullPoints(cnum, ret, maxc, i1, iret);

            for (j = 0; j < maxc; j++) {
                DContactGeom con = contacts.get(skip * j);
                for (i = 0; i < 3; i++) con.pos.set(i, point[iret[j] * 3 + i] + pa.get(i));
                con.depth = dep[iret[j]];
            }
            cnum = maxc;
        }

        return_code.set(tst._code);
        return cnum;
    }

    // ****************************************
    // dBox API
    // ****************************************

    @Override
    public void setLengths(double lx, double ly, double lz) {
        dGeomBoxSetLengths(new DVector3(lx, ly, lz));
    }

    @Override
    public void getLengths(DVector3 result) {
        dGeomBoxGetLengths(result);
    }

    @Override
    public void setLengths(DVector3C sides) {
        dGeomBoxSetLengths(sides);
    }

    @Override
    public DVector3C getLengths() {
        return side;
    }


    @Override
    public double getPointDepth(DVector3C p) {
        return dGeomBoxPointDepth(p.get0(), p.get1(), p.get2());
    }

}
