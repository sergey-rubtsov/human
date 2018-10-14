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

import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DColliderFn;
import org.ode4j.ode.DContactGeom;
import org.ode4j.ode.DContactGeomBuffer;
import org.ode4j.ode.DConvex;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.internal.cpp4j.java.RefDouble;
import org.ode4j.ode.internal.cpp4j.java.RefInt;

import static org.ode4j.ode.OdeConstants.CONTACTS_UNIMPORTANT;
import static org.ode4j.ode.OdeConstants.dInfinity;
import static org.ode4j.ode.OdeMath.dCalcVectorCross3;
import static org.ode4j.ode.OdeMath.dCalcVectorDot3;
import static org.ode4j.ode.OdeMath.dMultiply0_331;
import static org.ode4j.ode.OdeMath.dMultiply1_331;
import static org.ode4j.ode.OdeMath.dNormalize3;
import static org.ode4j.ode.internal.Common.dAASSERT;
import static org.ode4j.ode.internal.Common.dEpsilon;
import static org.ode4j.ode.internal.Common.dFabs;
import static org.ode4j.ode.internal.Common.dIASSERT;
import static org.ode4j.ode.internal.Common.dNODEBUG;
import static org.ode4j.ode.internal.Common.dSqrt;
import static org.ode4j.ode.internal.cpp4j.Cmath.fabs;
import static org.ode4j.ode.internal.cpp4j.Cstdio.fprintf;
import static org.ode4j.ode.internal.cpp4j.Cstdio.stdout;

/**
 * Code for Convex Collision Detection
 * By Rodrigo Hernandez
 */
public class DxConvex extends DxGeom implements DConvex {

    /**
     * An array of planes in the form:
     * normal X, normal Y, normal Z, Distance
     */
    private DVector3[] planesV;
    private double[] planesD;

    /**
     * An array of points X,Y,Z.
     */
    private double[] points;

    /**
     * An array of indices to the points of each polygon, it should be the
     * number of vertices followed by that amount of indices to "points" in
     * counter clockwise order
     */
    private int[] polygons;

    /**
     * Amount of planes in planes.
     */
    private int planecount;

    /**
     * Amount of points in points.
     */
    private int pointcount;

    /**
     * Amount of edges in convex.
     */
    private int edgecount;

    private static class Edge {
        int first;
        int second;
    }

    private Edge[] edges;

    /**
     * A Support mapping function for convex shapes.
     *
     * @param dir [IN] direction to find the Support Point for
     * @return the index of the support vertex.
     */
    //inline unsigned int SupportIndex(dVector3 dir)
    private int SupportIndex(DVector3 dir) {
        DVector3 rdir = new DVector3();
        //unsigned
        int index = 0;
        dMultiply1_331(rdir, final_posr().R(), dir);
        double max = dCalcVectorDot3(points, 0, rdir);
        double tmp;
        for (int i = 1; i < pointcount; ++i) {
            tmp = dCalcVectorDot3(points, (i * 3), rdir);
            if (tmp > max) {
                index = i;
                max = tmp;
            }
        }
        return index;
    }

    private static final double dMIN(double a, double b) {
        return a > b ? b : a;
    }

    private static final double dMAX(double a, double b) {
        return a > b ? a : b;
    }

    private static final int dMIN(int a, int b) {
        return a > b ? b : a;
    }

    private static final int dMAX(int a, int b) {
        return a > b ? a : b;
    }

    public DxConvex(DSpace space,
                    double[] planes,
                    int planecount,
                    double[] points,
                    int pointcount,
                    int[] polygons) {
        super(space, true);
        dAASSERT(planes != null);
        dAASSERT(points != null);
        dAASSERT(polygons != null);
        type = dConvexClass;
        planesV = new DVector3[planecount];
        planesD = new double[planecount];
        for (int i = 0; i < planecount; i++) {
            planesV[i] = new DVector3(planes[i * 4], planes[i * 4 + 1], planes[i * 4 + 2]);
            planesD[i] = planes[i * 4 + 3];
        }
        this.planecount = planecount;
        // we need points as well
        this.points = points;
        this.pointcount = pointcount;
        this.polygons = polygons;
        edges = null;
        FillEdges();
        if (!dNODEBUG) {
            checkPolygonsBuild();
        }
    }

    private void checkPolygonsBuild() {
        // Check for properly build polygons by calculating the determinant
        // of the 3x3 matrix composed of the first 3 points in the polygon.
        int points_in_polyPos = 0;
        int indexPos = 1;

        for (int i = 0; i < this.planecount; ++i) {
            dAASSERT(this.polygons[points_in_polyPos] > 2);
            int index03 = this.polygons[indexPos] * 3;
            int index13 = this.polygons[indexPos + 1] * 3;
            int index23 = this.polygons[indexPos + 2] * 3;
            if ((
                            this.points[index03] * this.points[index13 + 1] * this.points[index23 + 2] +
                            this.points[index03 + 1] * this.points[index13 + 2] * this.points[index23] +
                            this.points[index03 + 2] * this.points[index13] * this.points[(index23) + 1] -
                            this.points[index03 + 2] * this.points[index13 + 1] * this.points[index23] -
                            this.points[index03 + 1] * this.points[index13] * this.points[index23 + 2] -
                            this.points[index03] * this.points[index13 + 2] * this.points[index23 + 1]) < 0) {
                fprintf(stdout, "WARNING: Polygon %d is not defined counterclockwise\n", i);
            }
            points_in_polyPos += this.polygons[points_in_polyPos] + 1;
            indexPos = points_in_polyPos + 1;
            if (planesD[i] < 0) fprintf(stdout, "WARNING: Plane %d does not contain the origin\n", i);
        }
    }


    @Override
    void computeAABB() {
        // this can, and should be optimized
        DVector3 point = new DVector3();
        dMultiply0_331(point, final_posr().R(), points, 0);
        _aabb.setMin(final_posr().pos());
        _aabb.setMax(final_posr().pos());
        _aabb.shiftPos(point);
        for (int i = 3; i < (pointcount * 3); i += 3) {
            dMultiply0_331(point, final_posr().R(), points, i);
            DVector3 tmp = new DVector3();
            tmp.eqSum(point, final_posr().pos());
            _aabb.expand(tmp);
        }
    }

    /**
     * Populates the edges set, should be called only once whenever
     * the polygon array gets updated.
     */
    void FillEdges() {
        int points_in_polyPos = 0;
        int indexPos = 1;
        if (edges != null) edges = null;
        edgecount = 0;
        Edge e = new Edge();
        boolean isinset;
        for (int i = 0; i < planecount; ++i) {
            for (int j = 0; j < polygons[points_in_polyPos]; ++j) {
                e.first = dMIN(polygons[indexPos + j], polygons[indexPos + (j + 1) % polygons[points_in_polyPos]]);
                e.second = dMAX(polygons[indexPos + j], polygons[indexPos + (j + 1) % polygons[points_in_polyPos]]);
                isinset = false;
                for (int k = 0; k < edgecount; ++k) {
                    if ((edges[k].first == e.first) && (edges[k].second == e.second)) {
                        isinset = true;
                        break;
                    }
                }
                if (!isinset) {
                    Edge[] tmp = new Edge[edgecount + 1];
                    if (edgecount != 0) {
                        for (int ii = 0; ii < edges.length; ii++) tmp[ii] = edges[ii];
                    }
                    tmp[edgecount] = new Edge();
                    tmp[edgecount].first = e.first;
                    tmp[edgecount].second = e.second;
                    edges = tmp;
                    ++edgecount;
                }
            }
            points_in_polyPos += polygons[points_in_polyPos] + 1;
            indexPos = points_in_polyPos + 1;
        }
    }

    public static DConvex dCreateConvex(DxSpace space, double[] planes, int planecount,
                                        double[] points, int pointcount, int[] polygons) {
        return new DxConvex(space, planes, planecount,
                points,
                pointcount,
                polygons);
    }

    void dGeomSetConvex(double[] planes, int planecount,
                        double[] points, int pointcount, int[] polygons) {
        planesV = new DVector3[planecount];
        planesD = new double[planecount];
        for (int i = 0; i < planecount; i++) {
            planesV[i] = new DVector3(planes[i * 4], planes[i * 4 + 1], planes[i * 4 + 2]);
            planesD[i] = planes[i * 4 + 3];
        }
        this.planecount = planecount;
        this.points = points;
        this.pointcount = pointcount;
        this.polygons = polygons;
    }

    //****************************************************************************
    // Helper Inlines
    //

    /**
     * Returns Whether or not the segment ab intersects plane p.
     *
     * @param a origin of the segment
     * @param b segment destination
     * @param p plane to test for intersection
     * @param t returns the time "t" in the segment ray that gives us the intersecting
     *          point
     * @param q returns the intersection point
     * @return true if there is an intersection, otherwise false.
     */
    private static boolean IntersectSegmentPlane(DVector3 a,
                                                 DVector3 b,
                                                 DVector3 pV, double pD,
                                                 RefDouble t,
                                                 DVector3 q) {
        // Compute the t value for the directed line ab intersecting the plane
        DVector3 ab = new DVector3();
        ab.eqDiff(b, a);
        t.set((pD - dCalcVectorDot3(pV, a)) / dCalcVectorDot3(pV, ab));
        // If t in [0..1] compute and return intersection point
        if (t.get() >= 0.0 && t.get() <= 1.0) {
            q.eqSum(a, ab, t.get());
            return true;
        }
        // Else no intersection
        return false;
    }

    /**
     * Returns the Closest Point in Ray 1 to Ray 2.
     * @param Origin1 The origin of Ray 1
     * @param Direction1 The direction of Ray 1
     * @param Origin1 The origin of Ray 2
     * @param Direction1 The direction of Ray 3
     * @param t the time "t" in Ray 1 that gives us the closest point
     * (closest_point=Origin1+(Direction1*t).
     * @return true if there is a closest point, false if the rays are paralell.
     */
    //inline bool ClosestPointInRay(final dVector3 Origin1,
    //	      final dVector3 Direction1,
    //	      final dVector3 Origin2,
    //	      final dVector3 Direction2,
    //	      dReal& t)
//	private boolean ClosestPointInRay(final DVector3 Origin1,
//			final DVector3 Direction1,
//			final DVector3 Origin2,
//			final DVector3 Direction2,
//			RefDouble t)
//	{
//		//  dVector3 w = {Origin1[0]-Origin2[0],
//		//				Origin1[1]-Origin2[1],
//		//				Origin1[2]-Origin2[2]};
//		DVector3 w = new DVector3();
//		w.eqDiff(Origin1, Origin2);
//		double a = dCalcVectorDot3(Direction1 , Direction1);
//		double b = dCalcVectorDot3(Direction1 , Direction2);
//		double c = dCalcVectorDot3(Direction2 , Direction2);
//		double d = dCalcVectorDot3(Direction1 , w);
//		double e = dCalcVectorDot3(Direction2 , w);
//		double denominator = (a*c)-(b*b);
//		if(denominator==0.0f)
//		{
//			return false;
//		}
//		t.set( ((a*e)-(b*d))/denominator );
//		return true;
//	}

    /**
     * Clamp n to lie within the range [min, max].
     */
    private static double Clamp(double n, double min, double max) {
        if (n < min) return min;
        if (n > max) return max;
        return n;
    }

    /**
     * Returns the Closest Points from Segment 1 to Segment 2.
     * <p>NOTE: Adapted from Christer Ericson's Real Time Collision Detection Book.
     *
     * @param p1 start of segment 1
     * @param q1 end of segment 1
     * @param p2 start of segment 2
     * @param q2 end of segment 2
     * @param t  the time "t" in Ray 1 that gives us the closest point
     *           (closest_point=Origin1+(Direction1*t).
     * @return true if there is a closest point, false if the rays are paralell.
     */
    private static double ClosestPointBetweenSegments(DVector3 p1, DVector3 q1,
                                                      DVector3 p2, DVector3 q2, DVector3 c1, DVector3 c2) {
        // s & t were originaly part of the output args, but since
        // we don't really need them, we'll just declare them in here
        double s;
        double t;
        DVector3 d1 = q1.reSub(p1);
        DVector3 d2 = q2.reSub(p2);

        DVector3 r = p1.reSub(p2);
        double a = d1.lengthSquared();
        double e = d2.lengthSquared();
        double f = d2.dot(r);//dDOT(d2, r);
        // Check if either or both segments degenerate into points
        if (a <= dEpsilon && e <= dEpsilon) {
            // Both segments degenerate into points
            c1.set(p1);
            c2.set(p2);
            return c1.reSub(c2).lengthSquared();
        }
        if (a <= dEpsilon) {
            // First segment degenerates into a point
            s = 0.0f;
            t = f / e;
            t = Clamp(t, 0.0f, 1.0f);
        } else {
            double c = dCalcVectorDot3(d1, r);
            if (e <= dEpsilon) {
                // Second segment degenerates into a point
                t = 0.0f;
                s = Clamp(-c / a, 0.0f, 1.0f);
            } else {
                // The general non degenerate case starts here
                double b = dCalcVectorDot3(d1, d2);
                double denom = a * e - b * b; // Always nonnegative

                // If segments not parallel, compute closest point on L1 to L2, and
                // clamp to segment S1. Else pick arbitrary s (here 0)
                if (denom != 0.0f) {
                    s = Clamp((b * f - c * e) / denom, 0.0f, 1.0f);
                } else s = 0.0f;
                double tnom = b * s + f;
                if (tnom < 0.0f) {
                    t = 0.0f;
                    s = Clamp(-c / a, 0.0f, 1.0f);
                } else if (tnom > e) {
                    t = 1.0f;
                    s = Clamp((b - c) / a, 0.0f, 1.0f);
                } else {
                    t = tnom / e;
                }
            }
        }
        c1.eqSum(p1, d1, s);
        c2.eqSum(p2, d2, t);
        return (c1.get0() - c2.get0()) * (c1.get0() - c2.get0()) +
                (c1.get1() - c2.get1()) * (c1.get1() - c2.get1()) +
                (c1.get2() - c2.get2()) * (c1.get2() - c2.get2());
    }

//	/** 
//	 * Returns the Ray on which 2 planes intersect if they do.
//	 * @param p1 Plane 1
//	 * @param p2 Plane 2
//	 * @param p Contains the origin of the ray upon returning if planes intersect
//	 * @param d Contains the direction of the ray upon returning if planes intersect
//	 * @return true if the planes intersect, false if paralell.
//	 */
//	private boolean IntersectPlanes(final DVector3 p1, double p13, final DVector3 p2, double p23,DVector3 p, DVector3 d)
//	{
//		// Compute direction of intersection line
//	    dCalcVectorCross3(d,p1,p2);  
//		// If d is (near) zero, the planes are parallel (and separated)
//		// or coincident, so they're not considered intersecting
//		double denom = d.dot(d);
//		if (denom < dEpsilon) return false;
//		DVector3 n=new DVector3();
//		//  n[0]=p1[3]*p2[0] - p2[3]*p1[0];
//		//  n[1]=p1[3]*p2[1] - p2[3]*p1[1];
//		//  n[2]=p1[3]*p2[2] - p2[3]*p1[2];
//		n.eqSum(p2, p13, p1, -p23);
//		// Compute point on intersection line
//		dCalcVectorCross3(p,n,d);
//		//  p[0]/=denom;
//		//  p[1]/=denom;
//		//  p[2]/=denom;
//		p.scale(1/denom);
//		return true;
//	}


    //#if 0
    ///*! \brief Finds out if a point lies inside a convex
    //  \param p Point to test
    //  \param convex a pointer to convex to test against
    //  \return true if the point lies inside the convex, false if not.
    //*/
    //inline bool IsPointInConvex(dVector3 p,
    //			    dxConvex *convex)
    //{
    //  dVector3 lp,tmp;
    //  // move point into convex space to avoid plane local to world calculations
    //  tmp[0] = p[0] - convex->final_posr->pos[0];
    //  tmp[1] = p[1] - convex->final_posr->pos[1];
    //  tmp[2] = p[2] - convex->final_posr->pos[2];
    //  dMULTIPLY1_331 (lp,convex->final_posr->R,tmp);
    //  for(unsigned int i=0;i<convex->planecount;++i)
    //  {
    //    if((
    //	  ((convex->planes+(i*4))[0]*lp[0])+
    //	  ((convex->planes+(i*4))[1]*lp[1])+
    //	  ((convex->planes+(i*4))[2]*lp[2])+
    //	  -(convex->planes+(i*4))[3]
    //	  )>0)
    //	  {
    //	    return false;
    //	  }
    //  }
    //  return true;
    //}
    //#endif

    /**
     * Finds out if a point lies inside a 2D polygon.
     *
     * @param p       Point to test
     * @param polygonA a pointer to the start of the convex polygon index buffer
     * @param out     the closest point in the polygon if the point is not inside
     * @return true if the point lies inside of the polygon, false if not.
     */
    private static final boolean IsPointInPolygon(DVector3C p,
                                                  int[] polygonA, int polyPos,
                                                  DVector3C plane,
                                                  DxConvex convex,
                                                  DVector3 out) {
        // p is the point we want to check,
        // polygon is a pointer to the polygon we
        // are checking against, remember it goes
        // number of vertices then that many indexes
        // out returns the closest point on the border of the
        // polygon if the point is not inside it.
        DVector3 a = new DVector3();
        DVector3 b = new DVector3();
        DVector3 ab = new DVector3();
        DVector3 ap = new DVector3();
        DVector3 v = new DVector3();

        int pointcount = polygonA[0 + polyPos];
        Common.dIASSERT(pointcount != 0);
        polyPos++;//skip past pointcount

        dMultiply0_331(b, convex.final_posr().R(),
                convex.points, (polygonA[polyPos + pointcount - 1] * 3));
        b.eqSum(convex.final_posr().pos(), b);

        for (int i = 0; i < pointcount; ++i) {
            a.set(b);

            dMultiply0_331(b, convex.final_posr().R(),
                    convex.points, (polygonA[polyPos + i] * 3));
            b.eqSum(convex.final_posr().pos(), b);
            ab.eqDiff(b, a);
            ap.eqDiff(p, a);

            dCalcVectorCross3(v, ab, plane);

            if (dCalcVectorDot3(ap, v) > 0.0) {
                double ab_m2 = dCalcVectorDot3(ab, ab);
                double s = ab_m2 != 0.0 ? dCalcVectorDot3(ab, ap) / ab_m2 : 0.0;

                if (s <= 0.0) {
                    out.set(a);
                } else if (s >= 1.0) {
                    out.set(b);
                } else {
                    out.eqSum(a, ab, s);
                }
                return false;
            }
        }
        return true;
    }

    static class CollideConvexPlane implements DColliderFn {
        int dCollideConvexPlane(DxConvex Convex, DxPlane Plane, int flags,
                                DContactGeomBuffer contactBuf, int skip) {
            dIASSERT(skip >= 1);
            dIASSERT((flags & NUMC_MASK) >= 1);
            int contacts = 0;
            int maxc = flags & NUMC_MASK;
            DVector3 v2 = new DVector3();
            final int LTEQ_ZERO = 0x10000000;
            final int GTEQ_ZERO = 0x20000000;
            final int BOTH_SIGNS = (LTEQ_ZERO | GTEQ_ZERO);
            int totalsign = 0;
            for (int i = 0; i < Convex.pointcount; ++i) {
                dMultiply0_331(v2, Convex.final_posr().R(), Convex.points, i * 3);
                v2.add(Convex.final_posr().pos());
                int distance2sign = GTEQ_ZERO;
                double distance2 = Plane.getNormal().dot(v2) - Plane.getDepth();
                if ((distance2 <= (0.0))) {
                    distance2sign = distance2 != (0.0) ? LTEQ_ZERO : BOTH_SIGNS;
                    if (contacts != maxc) {
                        DContactGeom target = contactBuf.getSafe(flags, contacts);
                        target.normal.set(Plane.getNormal());
                        target.pos.set(v2);
                        target.depth = -distance2;
                        target.g1 = Convex;
                        target.g2 = Plane;
                        target.side1 = -1; // TODO: set plane index?
                        target.side2 = -1;
                        contacts++;
                    }
                }

                // Take new sign into account
                totalsign |= distance2sign;
                // Check if contacts are full and both signs have been already found
                if (((contacts ^ maxc) | totalsign) == BOTH_SIGNS) // harder to comprehend but requires one register less
                {
                    break; // Nothing can be changed any more
                }
            }
            if (totalsign == BOTH_SIGNS) return contacts;
            return 0;

        }

        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags,
                               DContactGeomBuffer contacts) {
            return dCollideConvexPlane((DxConvex) o1, (DxPlane) o2, flags, contacts, 1);
        }
    }

    static class CollideSphereConvex implements DColliderFn {
        int dCollideSphereConvex(DxSphere sphere, DxConvex convex, int flags,
                                 DContactGeomBuffer contacts, int skip) {
            dIASSERT(skip >= 1);
            dIASSERT((flags & NUMC_MASK) >= 1);
            double dist, closestdist = dInfinity;
            DVector3 planeV = new DVector3();
            double planeD;
            DVector3 offsetpos = new DVector3(), out = new DVector3(), temp = new DVector3();
            int[] pPolyV = convex.polygons;
            int pPolyPos = 0;
            int closestplane = -1;
            boolean sphereinside = true;
            DContactGeom contact = contacts.get(0);
/*            Do a good old sphere vs plane check first,
            if a collision is found then check if the contact point
            is within the polygon*/
            offsetpos.eqDiff(sphere.final_posr().pos(), convex.final_posr().pos());
            for (int i = 0; i < convex.planecount; ++i) {
                // apply rotation to the plane
                dMultiply0_331(planeV, convex.final_posr().R(), convex.planesV[i]);//convex.planes[(i*4)]);
                planeD = convex.planesD[i];//(convex.planes[(i*4)])[3];
                // Get the distance from the sphere origin to the plane
                dist = planeV.dot(offsetpos) - planeD; // Ax + By + Cz - D
                if (dist > 0) {
                    // if we get here, we know the center of the sphere is
                    // outside of the convex hull.
                    if (dist < sphere.getRadius()) {
                        // if we get here we know the sphere surface penetrates
                        // the plane
                        if (IsPointInPolygon(sphere.final_posr().pos(), pPolyV, pPolyPos, planeV, convex, out)) {
                            // finally if we get here we know that the
                            // sphere is directly touching the inside of the polyhedron
                            contact.normal.set(planeV);
                            contact.pos.eqSum(sphere.final_posr().pos(),
                                    contact.normal, -sphere.getRadius());
                            contact.depth = sphere.getRadius() - dist;
                            contact.g1 = sphere;
                            contact.g2 = convex;
                            contact.side1 = -1;
                            contact.side2 = -1; // TODO: set plane index?
                            return 1;
                        } else {
                            // the sphere may not be directly touching
                            // the polyhedron, but it may be touching
                            // a point or an edge, if the distance between
                            // the closest point on the poly (out) and the
                            // center of the sphere is less than the sphere
                            // radius we have a hit.
                            temp.eqDiff(sphere.final_posr().pos(), out);
                            dist = temp.lengthSquared();//(temp[0]*temp[0])+(temp[1]*temp[1])+(temp[2]*temp[2]);
                            // avoid the sqrt unless really necesary
                            if (dist < (sphere.getRadius() * sphere.getRadius())) {
                                // We got an indirect hit
                                dist = dSqrt(dist);
                                contact.normal.set(temp).scale(1. / dist);
                                contact.pos.eqSum(sphere.final_posr().pos(),
                                        contact.normal, -sphere.getRadius());
                                contact.depth = sphere.getRadius() - dist;
                                contact.g1 = sphere;
                                contact.g2 = convex;
                                contact.side1 = -1;
                                contact.side2 = -1; // TODO: set plane index?
                                return 1;
                            }
                        }
                    }
                    sphereinside = false;
                }
                if (sphereinside) {
                    if (closestdist > dFabs(dist)) {
                        closestdist = dFabs(dist);
                        closestplane = i;
                    }
                }
                pPolyPos += pPolyV[pPolyPos] + 1;
            }
            if (sphereinside) {
                // if the center of the sphere is inside
                // the Convex, we need to pop it out
                dMultiply0_331(contact.normal,
                        convex.final_posr().R(),
                        convex.planesV[closestplane]);
                contact.pos.set(sphere.final_posr().pos());
                contact.depth = closestdist + sphere.getRadius();
                contact.g1 = sphere;
                contact.g2 = convex;
                contact.side1 = -1;
                contact.side2 = -1; // TODO: set plane index?
                return 1;
            }
            return 0;
        }

        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags,
                               DContactGeomBuffer contacts) {
            return dCollideSphereConvex((DxSphere) o1, (DxConvex) o2, flags, contacts, 1);
        }
    }

    static class CollideConvexBox implements DColliderFn {

        int dCollideConvexBox(DxConvex Convex, DxBox box, int flags,
                              DContactGeomBuffer contacts, int skip) {
            dIASSERT(skip >= 1);
            dIASSERT((flags & NUMC_MASK) >= 1);
            return 0;
        }

        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags,
                               DContactGeomBuffer contacts) {
            return dCollideConvexBox((DxConvex) o1, (DxBox) o2, flags, contacts, 1);
        }
    }

    static class CollideConvexCapsule implements DColliderFn {
        int dCollideConvexCapsule(DxConvex Convex, DxCapsule Capsule,
                                  int flags, DContactGeomBuffer contacts, int skip) {
            dIASSERT(skip >= 1);
            dIASSERT((flags & NUMC_MASK) >= 1);
            return 0;
        }

        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags,
                               DContactGeomBuffer contacts) {
            return dCollideConvexCapsule((DxConvex) o1, (DxCapsule) o2, flags, contacts, 1);
        }
    }

    private static void ComputeInterval(DxConvex cvx, DVector3 axis, double axisD, RefDouble min, RefDouble max) {
        /* TODO: Use Support points here */
        DVector3 point = new DVector3();
        double value;
        dMultiply0_331(point, cvx.final_posr().R(), cvx.points, 0);
        point.add(cvx.final_posr().pos());
        min.set(dCalcVectorDot3(point, axis) - axisD);
        max.set(min.get());
        for (int i = 1; i < cvx.pointcount; ++i) {
            dMultiply0_331(point, cvx.final_posr().R(), cvx.points, (i * 3));
            point.add(cvx.final_posr().pos());
            value = dCalcVectorDot3(point, axis) - axisD;
            if (value < min.get()) {
                min.set(value);
            } else if (value > max.get()) {
                max.set(value);
            }
        }
        // *: usually using the distance part of the plane (axis) is
        // not necesary, however, here we need it here in order to know
        // which face to pick when there are 2 parallel sides.
    }

    boolean CheckEdgeIntersection(DxConvex cvx1, DxConvex cvx2, int flags, RefInt curc,
                                  DContactGeomBuffer contacts, int skip) {
        int maxc = flags & NUMC_MASK;
        dIASSERT(maxc != 0);
        DVector3 e1 = new DVector3(), e2 = new DVector3(), q = new DVector3();
        //dVector4 plane,depthplane;
        DVector3 planeV = new DVector3(), depthplaneV = new DVector3();
        double planeD, depthplaneD;
        RefDouble t = new RefDouble();  //TZ TODO Why?
        for (int i = 0; i < cvx1.edgecount; ++i) {
            // Rotate
            dMultiply0_331(e1, cvx1.final_posr().R(), cvx1.points, (cvx1.edges[i].first * 3));
            // translate
            e1.add(cvx1.final_posr().pos());
            // Rotate
            dMultiply0_331(e2, cvx1.final_posr().R(), cvx1.points, (cvx1.edges[i].second * 3));
            // translate
            e2.add(cvx1.final_posr().pos());
            int[] pPolyV = cvx2.polygons;
            int pPolyPos = 0;
            for (int j = 0; j < cvx2.planecount; ++j) {
                // Rotate
                dMultiply0_331(planeV, cvx2.final_posr().R(), cvx2.planesV[i]);//+(j*4));
                dNormalize3(planeV);
                // Translate
                planeD =
                        cvx2.planesD[j] +

                                planeV.dot(cvx2.final_posr().pos());

                DContactGeom target = contacts.getSafe(flags, curc.get());

                target.g1 = cvx1;// g1 is the one pushed
                target.g2 = cvx2;
                if (IntersectSegmentPlane(e1, e2, planeV, planeD, t, target.pos)) {
                    if (IsPointInPolygon(target.pos, pPolyV, pPolyPos, planeV, cvx2, q)) {
                        target.depth = dInfinity;
                        for (int k = 0; k < cvx2.planecount; ++k) {
                            if (k == j) continue; // we're already at 0 depth on this plane
                            // Rotate
                            dMultiply0_331(depthplaneV, cvx2.final_posr().R(), cvx2.planesV[k]);
                            dNormalize3(depthplaneV);
                            // Translate
                            depthplaneD = cvx2.planesD[k] + planeV.dot(cvx2.final_posr().pos());
                            double depth = depthplaneV.dot(target.pos) - depthplaneD;
                            if ((fabs(depth) < fabs(target.depth)) && ((depth < -dEpsilon) || (depth > dEpsilon))) {
                                target.depth = depth;
                                target.normal.set(depthplaneV);
                            }
                        }
                        curc.inc();
                        if (curc.get() == maxc)
                            return true;
                    }
                }
                pPolyPos += pPolyV[pPolyPos] + 1;
            }
        }
        return false;
    }

	/*
Helper struct
	 */

    private static class ConvexConvexSATOutput {
        double min_depth;
        int depth_type;
        DVector3 dist = new DVector3(); // distance from center to center, from cvx1 to cvx2
        DVector3 e1a = new DVector3(), e1b = new DVector3();
        DVector3 e2a = new DVector3(), e2b = new DVector3();
    }

    /**
     * Does an axis separation test using cvx1 planes on cvx1 and cvx2,
     * returns true for a collision false for no collision.
     *
     * @param cvx1      [IN] First Convex object, its planes are used to do the tests
     * @param cvx2      [IN] Second Convex object
     * @param min_depth [IN/OUT] Used to input as well as output the minimum
     *                  depth so far, must be set to a huge value such as dInfinity for initialization.
     * @param g1        [OUT] Pointer to the convex which should be used in the returned contact as g1
     * @param g2        [OUT] Pointer to the convex which should be used in the returned contact as g2
     */
    private static boolean CheckSATConvexFaces(DxConvex cvx1,
                                               DxConvex cvx2,
                                               ConvexConvexSATOutput ccso) {
        //double min,max,min1,max1,min2,max2,depth;
        double min, max, depth;
        RefDouble min1 = new RefDouble(), max1 = new RefDouble();
        RefDouble min2 = new RefDouble(), max2 = new RefDouble();
        //dVector4 plane;
        DVector3 planeV = new DVector3();
        double planeD;
        for (int i = 0; i < cvx1.planecount; ++i) {
            // -- Apply Transforms --
            // Rotate
            dMultiply0_331(planeV, cvx1.final_posr().R(), cvx1.planesV[i]);
            dNormalize3(planeV);
            // Translate
            planeD = cvx1.planesD[i] + planeV.dot(cvx1.final_posr().pos());
            ComputeInterval(cvx1, planeV, planeD, min1, max1);
            ComputeInterval(cvx2, planeV, planeD, min2, max2);
            if (max2.get() < min1.get() || max1.get() < min2.get()) return false;
            min = dMAX(min1.get(), min2.get());
            max = dMIN(max1.get(), max2.get());
            depth = max - min;
            /*
            Take only into account the faces that penetrate cvx1 to determine
            minimum depth ((max2*min2)<=0) = different sign, or one is zero and thus
            cvx2 barelly touches cvx1
            */
            if (((max2.get() * min2.get()) <= 0) && (dFabs(depth) < dFabs(ccso.min_depth))) {
                // Flip plane because the contact normal must point INTO g1,
                // plus the integrator seems to like positive depths better than negative ones
                ccso.min_depth = -depth;
                ccso.depth_type = 1; // 1 = face-something
            }
        }
        return true;
    }


    /**
     * Does an axis separation test using cvx1 and cvx2 edges,
     * returns true for a collision false for no collision.
     *
     * @param cvx1      [IN] First Convex object
     * @param cvx2      [IN] Second Convex object
     * @param min_depth [IN/OUT] Used to input as well as output the minimum
     *                  depth so far, must be set to a huge value such as dInfinity for initialization.
     * @param g1        [OUT] Pointer to the convex which should be used in the returned contact as g1
     * @param g2        [OUT] Pointer to the convex which should be used in the returned contact as g2
     */
    private static boolean CheckSATConvexEdges(DxConvex cvx1,
                                               DxConvex cvx2,
                                               ConvexConvexSATOutput ccso) {
        // Test cross products of pairs of edges
        double depth, min, max;
        RefDouble min1 = new RefDouble(), max1 = new RefDouble();
        RefDouble min2 = new RefDouble(), max2 = new RefDouble();
        //dVector4 plane;
        DVector3 planeV = new DVector3();
        double planeD;
        DVector3 e1 = new DVector3(), e2 = new DVector3(), e1a = new DVector3();
        DVector3 e1b = new DVector3(), e2a = new DVector3(), e2b = new DVector3();
        DVector3 dist = new DVector3(ccso.dist);
        int s1 = cvx1.SupportIndex(dist);
        // invert direction
        dist.scale(-1);
        int s2 = cvx2.SupportIndex(dist);
        for (int i = 0; i < cvx1.edgecount; ++i) {
            // Skip edge if it doesn't contain the extremal vertex
            if ((cvx1.edges[i].first != s1) && (cvx1.edges[i].second != s1)) continue;
            // we only need to apply rotation here
            dMultiply0_331(e1a, cvx1.final_posr().R(), cvx1.points, (cvx1.edges[i].first * 3));
            dMultiply0_331(e1b, cvx1.final_posr().R(), cvx1.points, (cvx1.edges[i].second * 3));
            e1.eqDiff(e1b, e1a);
            for (int j = 0; j < cvx2.edgecount; ++j) {
                // Skip edge if it doesn't contain the extremal vertex
                if ((cvx2.edges[j].first != s2) && (cvx2.edges[j].second != s2)) continue;
                // we only need to apply rotation here
                dMultiply0_331(e2a, cvx2.final_posr().R(), cvx2.points, (cvx2.edges[j].first * 3));
                dMultiply0_331(e2b, cvx2.final_posr().R(), cvx2.points, (cvx2.edges[j].second * 3));
                e2.eqDiff(e2b, e2a);
                dCalcVectorCross3(planeV, e1, e2);
                if (planeV.dot(planeV) < dEpsilon) /* edges are parallel */ continue;
                dNormalize3(planeV);
                planeD = 0;//plane[3]=0;
                ComputeInterval(cvx1, planeV, planeD, min1, max1);
                ComputeInterval(cvx2, planeV, planeD, min2, max2);
                if (max2.get() < min1.get() || max1.get() < min2.get()) return false;
                min = dMAX(min1.get(), min2.get());
                max = dMIN(max1.get(), max2.get());
                depth = max - min;
                if (((dFabs(depth) + dEpsilon) < dFabs(ccso.min_depth))) {
                    ccso.min_depth = depth;
                    ccso.depth_type = 2; // 2 = edge-edge
                    // use cached values, add position
                    ccso.e1a.set(e1a);
                    ccso.e1b.set(e1b);
                    ccso.e1a.add(cvx1.final_posr().pos());
                    ccso.e1b.add(cvx1.final_posr().pos());
                    ccso.e2a.set(e2a);
                    ccso.e2b.set(e2b);
                    ccso.e2a.add(cvx2.final_posr().pos());
                    ccso.e2b.add(cvx2.final_posr().pos());
                }
            }
        }
        return true;
    }

    //#if 0
    ///*! \brief Returns the index of the plane/side of the incident convex (ccso.g2)
    //      which is closer to the reference convex (ccso.g1) side
    //
    //      This function just looks for the incident face that is facing the reference face
    //      and is the closest to being parallel to it, which sometimes is.
    //*/
    //inline unsigned int GetIncidentSide(ConvexConvexSATOutput& ccso)
    //{
    //  dVector3 nis; // (N)ormal in (I)ncident convex (S)pace
    //  dReal SavedDot;
    //  dReal Dot;
    //  unsigned int incident_side=0;
    //  // Rotate the plane normal into incident convex space
    //  // (things like this should be done all over this file,
    //  //  will look into that)
    //  dMULTIPLY1_331(nis,ccso.g2.final_posr.R,ccso.plane);
    //  SavedDot = dDOT(nis,ccso.g2.planes);
    //  for(unsigned int i=1;i<ccso.g2.planecount;++i)
    //  {
    //    Dot = dDOT(nis,ccso.g2.planes+(i*4));
    //    if(Dot>SavedDot)
    //    {
    //      SavedDot=Dot;
    //      incident_side=i;
    //    }
    //  }
    //  return incident_side;
    //}
    //#endif

    //inline unsigned int GetSupportSide(dVector3& dir,dxConvex& cvx)
    private static int GetSupportSide(DVector3 dir, DxConvex cvx) {
        DVector3 dics = new DVector3(), tmp = new DVector3(); // Direction in convex space
        double SavedDot;
        double Dot;
        //unsigned
        int side = 0;
        //dVector3Copy(dir,tmp);
        tmp.set(dir);
        dNormalize3(tmp);
        dMultiply1_331(dics, cvx.final_posr().R(), tmp);
        SavedDot = dics.dot(cvx.planesV[0]);
        for (int i = 1; i < cvx.planecount; ++i) {
            Dot = dCalcVectorDot3(dics, cvx.planesV[i]);//+(i*4));
            if (Dot > SavedDot) {
                SavedDot = Dot;
                side = i;
            }
        }
        return side;
    }

    /**
     * Does an axis separation test between the 2 convex shapes
     * using faces and edges.
     */
    private static int TestConvexIntersection(DxConvex cvx1, DxConvex cvx2, int flags,
                                              DContactGeomBuffer contactBuf, int skip) {
        ConvexConvexSATOutput ccso = new ConvexConvexSATOutput();
        ccso.min_depth = dInfinity; // Min not min at all
        ccso.depth_type = 0; // no type
        // precompute distance vector
        ccso.dist.eqDiff(cvx2.final_posr().pos(), cvx1.final_posr().pos());
        int maxc = flags & NUMC_MASK;
        dIASSERT(maxc != 0);
        DVector3 i1 = new DVector3(), i2 = new DVector3(),
                r1 = new DVector3(), r2 = new DVector3(); // edges of incident and reference faces respectively
        int contacts = 0;
        if (!CheckSATConvexFaces(cvx1, cvx2, ccso)) {
            return 0;
        } else if (!CheckSATConvexFaces(cvx2, cvx1, ccso)) {
            return 0;
        } else if (!CheckSATConvexEdges(cvx1, cvx2, ccso)) {
            return 0;
        }
        // If we get here, there was a collision
        if (ccso.depth_type == 1) // face-face
        {
            // cvx1 MUST always be in contact->g1 and cvx2 in contact->g2
            // This was learned the hard way :(
            //unsigned
            int incident_side;
            //unsigned int* pIncidentPoly;
            int pIncidentPolyPos;
            //unsigned int* pIncidentPoints;
            int pIncidentPointsPos;
            //unsigned int reference_side;
            int reference_side;
            //unsigned int* pReferencePoly;
            int pReferencePolyPos;
            //unsigned int* pReferencePoints;
            int pReferencePointsPos;
            //dVector4 plane,rplane,iplane;
            DVector3 planeV = new DVector3(), rplaneV = new DVector3(), iplaneV = new DVector3();
            double planeD, rplaneD, iplaneD;
            DVector3 tmp = new DVector3();
            DVector3 dist, p = new DVector3();
            RefDouble t = new RefDouble();
            double d, d1, d2;
            boolean outside, out;
            dist = new DVector3(ccso.dist);
            reference_side = GetSupportSide(dist, cvx1);
            dist.scale(-1);
            incident_side = GetSupportSide(dist, cvx2);

            pReferencePolyPos = 0;
            pIncidentPolyPos = 0;
            int[] refPolys = cvx1.polygons;
            int[] incPolys = cvx2.polygons;
            // Get Reference plane (We may not have to apply transforms Optimization Oportunity)
            // Rotate
            dMultiply0_331(rplaneV, cvx1.final_posr().R(), cvx1.planesV[reference_side]);
            dNormalize3(rplaneV);
            // Translate
            rplaneD = (cvx1.planesD[reference_side]) + rplaneV.dot(cvx1.final_posr().pos());
            // flip
            rplaneV.scale(-1);
            rplaneD = -rplaneD;
            for (int i = 0; i < incident_side; ++i) {
                pIncidentPolyPos += incPolys[pIncidentPolyPos] + 1;
            }
            pIncidentPointsPos = pIncidentPolyPos + 1;
            // Get the first point of the incident face
            dMultiply0_331(i2, cvx2.final_posr().R(), cvx2.points, (incPolys[pIncidentPointsPos] * 3));
            i2.add(cvx2.final_posr().pos());
            // Get the same point in the reference convex space
            r2.set(i2);
            r2.sub(cvx1.final_posr().pos());
            tmp.set(r2);
            dMultiply1_331(r2, cvx1.final_posr().R(), tmp);
            for (int i = 0; i < cvx2.polygons[pIncidentPolyPos]; ++i) {
                // Move i2 to i1, r2 to r1
                i1.set(i2);
                r1.set(r2);
                dMultiply0_331(i2, cvx2.final_posr().R(),
                        cvx2.points, incPolys[pIncidentPointsPos + (i + 1) % incPolys[pIncidentPolyPos]] * 3);
                i2.add(cvx2.final_posr().pos());
                // Get the same point in the reference convex space
                r2.set(i1);
                r2.sub(cvx1.final_posr().pos());
                tmp.set(r2);
                dMultiply1_331(r2, cvx1.final_posr().R(), tmp);
                outside = false;
                for (int j = 0; j < cvx1.planecount; ++j) {
                    planeV.set(cvx1.planesV[j]);
                    planeD = cvx1.planesD[j];
                    // Get the distance from the points to the plane
                    d1 = r1.dot(planeV) - planeD;
                    d2 = r2.dot(planeV) - planeD;
                    if (d1 * d2 < 0) {
                        // Edge intersects plane
                        IntersectSegmentPlane(r1, r2, planeV, planeD, t, p);
                        // Check the resulting point again to make sure it is inside the reference convex
                        out = false;
                        for (int k = 0; k < cvx1.planecount; ++k) {
                            d = p.dot(cvx1.planesV[k]) - cvx1.planesD[k];
                            if (d > 0) {
                                out = true;
                                break;
                            }
                            ;
                        }
                        if (!out) {
                            // Apply reference convex transformations to p
                            // The commented out piece of code is likelly to
                            // produce less operations than this one, but
                            // this way we know we are getting the right data
                            dMultiply0_331(tmp, cvx1.final_posr().R(), p);
                            p.eqSum(tmp, cvx1.final_posr().pos());
                            d = p.dot(rplaneV) - rplaneD;
                            if (d > 0) {
                                DContactGeom contact = contactBuf.getSafe(flags, contacts);
                                contact.pos.set(p);
                                contact.normal.set(rplaneV);
                                contact.depth = d;
                                contact.g2 = cvx2;
                                contact.g1 = cvx1;
                                ++contacts;
                                if (contacts == maxc) return contacts;
                            }
                        }
                    }
                    if (d1 > 0) {
                        outside = true;
                    }
                }
                if (outside) continue;
                d = i1.dot(rplaneV) - rplaneD;
                if (d > 0) {
                    DContactGeom contact = contactBuf.getSafe(flags, contacts);
                    contact.pos.set(i1);
                    contact.normal.set(rplaneV);
                    contact.depth = d;
                    contact.g1 = cvx1;
                    contact.g2 = cvx2;
                    ++contacts;
                    if (contacts == maxc) return contacts;
                }
            }
            // IF we get here, we got the easiest contacts to calculate,
            // but there is still space in the contacts array for more.
            // So, project the Reference's face points onto the Incident face
            // plane and test them for inclusion in the reference plane as well.
            // We already have computed intersections so, skip those.

            // Get Incident plane, we need it for projection
            // Rotate
            dMultiply0_331(iplaneV, cvx2.final_posr().R(), cvx2.planesV[incident_side]);//+(incident_side*4));
            dNormalize3(iplaneV);
            // Translate
            iplaneD = cvx2.planesD[incident_side] + iplaneV.dot(cvx2.final_posr().pos());
            // get reference face
            for (int i = 0; i < reference_side; ++i) {

                pReferencePolyPos += refPolys[pReferencePolyPos] + 1;
            }
            pReferencePointsPos = pReferencePolyPos + 1;
            for (int i = 0; i < refPolys[pReferencePolyPos]; ++i) {
                dMultiply0_331(i1, cvx1.final_posr().R(), cvx1.points, refPolys[pReferencePointsPos + i] * 3);
                i1.add(cvx1.final_posr().pos());
                // Project onto Incident face plane
                t.set(-(i1.dot(iplaneV) - iplaneD));
                i1.eqSum(i1, iplaneV, t.get());
                // Get the same point in the incident convex space
                r1.set(i1);
                r1.sub(cvx2.final_posr().pos());
                tmp.set(r1);
                dMultiply1_331(r1, cvx2.final_posr().R(), tmp);
                // Check if it is outside the incident convex
                out = false;
                for (int j = 0; j < cvx2.planecount; ++j) {
                    d = r1.dot(cvx2.planesV[j]) - cvx2.planesD[j];
                    if (d >= 0) {
                        out = true;
                        break;
                    }
                }
                if (!out) {
                    // check that the point is not a duplicate
                    outside = false;
                    for (int j = 0; j < contacts; ++j) {
                        if (contactBuf.getSafe(flags, j).pos.isEq(i1)) {
                            outside = true;
                        }
                    }
                    if (!outside) {
                        d = i1.dot(rplaneV) - rplaneD;
                        if (d > 0) {
                            DContactGeom contact = contactBuf.getSafe(flags, contacts);
                            contact.pos.set(i1);
                            contact.normal.set(rplaneV);
                            contact.g1 = cvx1;
                            contact.g2 = cvx2;
                            contact.depth = d;
                            ++contacts;
                            if (contacts == maxc) return contacts;
                        }
                    }
                }
            }
        } else if (ccso.depth_type == 2) // edge-edge
        {
//			// Some parts borrowed from dBoxBox
//			DVector3 ua = new DVector3(),ub = new DVector3(),pa = new DVector3(),pb = new DVector3();
//			RefDouble alpha=new RefDouble(),beta=new RefDouble();
//			// Get direction of first edge
//			//for (i=0; i<3; i++) ua[i] = ccso.e1b[i]-ccso.e1a[i];
//			ua.eqDiff(ccso.e1b, ccso.e1a);
//			dNormalize3(ua); // normalization shouldn't be necesary but dLineClosestApproach requires it
//			// Get direction of second edge
//			//for (i=0; i<3; i++) ub[i] = ccso.e2b[i]-ccso.e2a[i];
//			ub.eqDiff(ccso.e2b, ccso.e2a);
//			dNormalize3(ub); // same as with ua normalization
//			// Get closest points between edges (one at each)
//			DxCollisionUtil.dLineClosestApproach (ccso.e1a,ua,ccso.e2a,ub,alpha,beta);
//			//for (i=0; i<3; i++) pa[i] = ccso.e1a[i]+(ua[i]*alpha.get());
//			pa.eqSum(ccso.e1a, ua, alpha.get());
//			//for (i=0; i<3; i++) pb[i] = ccso.e2a[i]+(ub[i]*beta.get());
//			pb.eqSum(ccso.e2a, ub, beta.get());
//			// Set the contact point as halfway between the 2 closest points
////			for (i=0; i<3; i++) SAFECONTACT(flags, contact, contacts, skip).pos[i] = REAL(0.5)*(pa[i]+pb[i]);
////			SAFECONTACT(flags, contact, contacts, skip).g1=cvx1;//&cvx1;
////			SAFECONTACT(flags, contact, contacts, skip).g2=cvx2;//&cvx2;
////			dVector3Copy(ccso.plane,SAFECONTACT(flags, contact, contacts, skip).normal);
////			SAFECONTACT(flags, contact, contacts, skip).depth=ccso.min_depth;
//			DContactGeom contact = contactBuf.getSafe(flags, contacts);
//			contact.pos.eqSum(pa, 0.5, pb, 0.5);
//			contact.g1 = cvx1;
//			contact.g2 = cvx2;
//			//TODO TZ optimize with dVector3!
//			contact.normal.set(ccso.plane.get0(), ccso.plane.get1(), ccso.plane.get2());
//			contact.depth = ccso.min_depth;
//			++contacts;

            DVector3 c1 = new DVector3(), c2 = new DVector3();
            //float s,t;
            DContactGeom contact = contactBuf.getSafe(flags, contacts);
            contact.depth = dSqrt(ClosestPointBetweenSegments(ccso.e1a, ccso.e1b, ccso.e2a, ccso.e2b, c1, c2));
            contact.g1 = cvx1;
            contact.g2 = cvx2;
            contact.pos.set(c1);
            contact.normal.eqDiff(c2, c1);
            contact.normal.normalize();
            contacts++;
        }
        return contacts;
    }

    public static class CollideConvexConvex implements DColliderFn {
        int dCollideConvexConvex(DxConvex Convex1, DxConvex Convex2, int flags,
                                 DContactGeomBuffer contacts, int skip) {
            dIASSERT(skip >= 1);
            dIASSERT((flags & NUMC_MASK) >= 1);
            //TODO? Passing actual objects/clone????
            return TestConvexIntersection(Convex1, Convex2, flags,
                    contacts, skip);
        }

        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags,
                               DContactGeomBuffer contacts) {
            return dCollideConvexConvex((DxConvex) o1, (DxConvex) o2, flags, contacts, 1);
        }
    }

    //#if 0
    //int dCollideRayConvex (dxGeom *o1, dxGeom *o2, int flags,
    //		       dContactGeom *contact, int skip)
    //{
    //  dIASSERT (skip >= (int)sizeof(dContactGeom));
    //  dIASSERT( o1->type == dRayClass );
    //  dIASSERT( o2->type == dConvexClass );
    //  dIASSERT ((flags & NUMC_MASK) >= 1);
    //  dxRay* ray = (dxRay*) o1;
    //  dxConvex* convex = (dxConvex*) o2;
    //  dVector3 origin,destination,contactpoint,out;
    //  dReal depth;
    //  dVector4 plane;
    //  unsigned int *pPoly=convex->polygons;
    //  // Calculate ray origin and destination
    //  destination[0]=0;
    //  destination[1]=0;
    //  destination[2]= ray->length;
    //  // -- Rotate --
    //  dMULTIPLY0_331(destination,ray->final_posr->R,destination);
    //  origin[0]=ray->final_posr->pos[0];
    //  origin[1]=ray->final_posr->pos[1];
    //  origin[2]=ray->final_posr->pos[2];
    //  destination[0]+=origin[0];
    //  destination[1]+=origin[1];
    //  destination[2]+=origin[2];
    //  for(int i=0;i<convex->planecount;++i)
    //    {
    //      // Rotate
    //      dMULTIPLY0_331(plane,convex->final_posr->R,convex->planes+(i*4));
    //      // Translate
    //      plane[3]=
    //	(convex->planes[(i*4)+3])+
    //	((plane[0] * convex->final_posr->pos[0]) +
    //	 (plane[1] * convex->final_posr->pos[1]) +
    //	 (plane[2] * convex->final_posr->pos[2]));
    //      if(IntersectSegmentPlane(origin,
    //			       destination,
    //			       plane,
    //			       depth,
    //			       contactpoint))
    //	{
    //	  if(IsPointInPolygon(contactpoint,pPoly,planeV,convex,out))
    //	    {
    //	      contact->pos[0]=contactpoint[0];
    //	      contact->pos[1]=contactpoint[1];
    //	      contact->pos[2]=contactpoint[2];
    //	      contact->normal[0]=plane[0];
    //	      contact->normal[1]=plane[1];
    //	      contact->normal[2]=plane[2];
    //	      contact->depth=depth;
    //	      contact->g1 = ray;
    //	      contact->g2 = convex;
    //  contact->side1 = -1;
    //  contact->side2 = -1; // TODO: set plane index?
    //	      return 1;
    //	    }
    //	}
    //      pPoly+=pPoly[0]+1;
    //    }
    //  return 0;
    //}
    //#else

    static class CollideRayConvex implements DColliderFn {
        // Ray - Convex collider by David Walters, June 2006
        int dCollideRayConvex(DxRay ray, DxConvex convex,
                              int flags, DContactGeomBuffer contacts, int skip) {
            dIASSERT(skip >= 1);
            dIASSERT((flags & NUMC_MASK) >= 1);


            DContactGeom contact = contacts.get(0);
            contact.g1 = ray;
            contact.g2 = convex;
            contact.side1 = -1;
            contact.side2 = -1; // TODO: set plane index?
            double alpha, beta, nsign;
            boolean flag;
            //
            // Compute some useful info
            //
            flag = false;    // Assume start point is behind all planes.

            for (int i = 0; i < convex.planecount; ++i) {
                // Alias this plane.
                int planePos = i;
                // If alpha >= 0 then start point is outside of plane.
                alpha = dCalcVectorDot3(convex.planesV[planePos], ray.final_posr().pos()) - convex.planesD[planePos];

                // If any alpha is positive, then
                // the ray start is _outside_ of the hull
                if (alpha >= 0) {
                    flag = true;
                    break;
                }
            }
            // If the ray starts inside the convex hull, then everything is flipped.
            nsign = (flag) ? (1.0) : (-1.0);
            //
            // Find closest contact point
            //
            // Assume no contacts.
            contact.depth = dInfinity;

            for (int i = 0; i < convex.planecount; ++i) {
                // Alias this plane.
                int planePos = i;
                // If alpha >= 0 then point is outside of plane.
                alpha = nsign * (dCalcVectorDot3(convex.planesV[planePos], ray.final_posr().pos()) - convex.planesD[planePos]);

                // Compute [ plane-normal DOT ray-normal ], (/flip)
                beta = convex.planesV[planePos].dot(ray.final_posr().R().viewCol(2)) * nsign;

                // Ray is pointing at the plane? ( beta < 0 )
                // Ray start to plane is within maximum ray length?
                // Ray start to plane is closer than the current best distance?
                if (beta < -dEpsilon &&
                        alpha >= 0 && alpha <= ray.getLength() &&
                        alpha < contact.depth) {
                    // Compute contact point on convex hull surface.
                    contact.pos.eqSum(ray.final_posr().pos(), 0, ray.final_posr().R().columnAsNewVector(2), alpha);

                    flag = false;

                    // For all _other_ planes.
                    for (int j = 0; j < convex.planecount; ++j) {
                        if (i == j)
                            continue;    // Skip self.

                        // Alias this plane.
                        //double* planej = convex.planes + ( j * 4 );
                        int planePosJ = j;//*4;

                        // If beta >= 0 then start is outside of plane.
                        //beta = dDOT( planej, contact.pos ) - plane[3];
                        //TODO use planePos+3 or planePosJ+3 ???
                        //beta = dDOT( convex.planesV[planePosJ], contact.pos) - convex.planesD[planePosJ];
                        beta = dCalcVectorDot3(convex.planesV[planePosJ], contact.pos) - convex.planesD[planePos];

                        // If any beta is positive, then the contact point
                        // is not on the surface of the convex hull - it's just
                        // intersecting some part of its infinite extent.
                        if (beta > dEpsilon) {
                            flag = true;
                            break;
                        }
                    }

                    // Contact point isn't outside hull's surface? then it's a good contact!
                    if (flag == false) {
                        // Store the contact normal, possibly flipped.
                        contact.normal.set(convex.planesV[planePos]).scale(nsign);

                        // Store depth
                        contact.depth = alpha;

                        if ((flags & CONTACTS_UNIMPORTANT) != 0 && contact.depth <= ray.getLength()) {
                            // Break on any contact if contacts are not important
                            break;
                        }
                    }
                }
            }
            // Contact?
            return (contact.depth <= ray.getLength() ? 1 : 0);
        }

        @Override
        public int dColliderFn(DGeom o1, DGeom o2, int flags,
                               DContactGeomBuffer contacts) {
            return dCollideRayConvex((DxRay) o1, (DxConvex) o2, flags, contacts, 1);
        }
    }

    @Override
    public void setConvex(double[] planes, int planeCount, double[] points,
                          int pointCount, int[] polygons) {
        dGeomSetConvex(planes, planeCount, points, pointCount, polygons);
    }

    public double[] getPoints() {
        return points;
    }

    public int getPointcount() {
        return pointcount;
    }
}