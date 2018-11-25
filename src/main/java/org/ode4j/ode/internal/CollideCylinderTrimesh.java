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
import org.ode4j.math.DVector4;
import org.ode4j.ode.DColliderFn;
import org.ode4j.ode.DContactGeom;
import org.ode4j.ode.DContactGeomBuffer;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.OdeConstants;
import org.ode4j.ode.OdeMath;
import org.ode4j.ode.internal.cpp4j.java.RefBoolean;
import org.ode4j.ode.internal.gimpact.GimDynArrayInt;
import org.ode4j.ode.internal.gimpact.GimGeometry.aabb3f;
import org.ode4j.ode.internal.gimpact.GimGeometry.vec3f;
import org.ode4j.ode.internal.gimpact.GimTrimesh;

import static org.ode4j.ode.internal.DxCollisionUtil.dClipEdgeToPlane;
import static org.ode4j.ode.internal.DxCollisionUtil.dClipPolyToPlane;
import static org.ode4j.ode.internal.DxCollisionUtil.dConstructPlane;
import static org.ode4j.ode.internal.DxCollisionUtil.dMat3GetCol;
import static org.ode4j.ode.internal.DxCollisionUtil.dPointPlaneDistance;
import static org.ode4j.ode.internal.DxCollisionUtil.dQuatInv;
import static org.ode4j.ode.internal.DxCollisionUtil.dQuatTransform;
import static org.ode4j.ode.internal.DxCollisionUtil.dVector3Copy;
import static org.ode4j.ode.internal.DxCollisionUtil.dVector3Cross;
import static org.ode4j.ode.internal.DxCollisionUtil.dVector3Inv;
import static org.ode4j.ode.internal.DxCollisionUtil.dVector3Subtract;

/**
 * Cylinder-trimesh collider by Alen Ladavac
 * Ported to ODE by Nguyen Binh
 * Ported to Java by Tilmann Zaeschke
 */
public class CollideCylinderTrimesh implements DColliderFn {

    @Override
    public int dColliderFn(DGeom o1, DGeom o2, int flags,
                           DContactGeomBuffer contacts) {
        return dCollideCylinderTrimesh((DxCylinder) o1, (DxGimpact) o2, flags, contacts, 1);
    }

    private static final double MAX_REAL = Common.dInfinity;
    private static final int nCYLINDER_AXIS = 2;
    private static final int nCYLINDER_CIRCLE_SEGMENTS = 8;
    private static final int nMAX_CYLINDER_TRIANGLE_CLIP_POINTS = 12;

    //#define OPTIMIZE_CONTACTS 1

    // Local contacts data
    //typedef struct _sLocalContactData
    private static class sLocalContactData {
        final DVector3 vPos = new DVector3();
        final DVector3 vNormal = new DVector3();
        double fDepth;
        int triIndex;
        int nFlags; // 0 = filtered out, 1 = OK
    }

    private static class sCylinderTrimeshColliderData {
        sCylinderTrimeshColliderData(int flags, int skip) {
            m_iFlags = flags;
            m_nContacts = 0;
            m_gLocalContacts = null;
        }

        // cylinder data
        private final DMatrix3 m_mCylinderRot = new DMatrix3();
        private final DQuaternion m_qCylinderRot = new DQuaternion();
        private final DQuaternion m_qInvCylinderRot = new DQuaternion();
        private final DVector3 m_vCylinderPos = new DVector3();
        private final DVector3 m_vCylinderAxis = new DVector3();
        private double m_fCylinderRadius;
        private double m_fCylinderSize;
        private final DVector3[] m_avCylinderNormals = new DVector3[nCYLINDER_CIRCLE_SEGMENTS];

        // mesh data
        //TODO remove/report ?
        @SuppressWarnings("unused")
        private DQuaternionC m_qTrimeshRot;
        //TODO remove/report ?
        @SuppressWarnings("unused")
        private DQuaternion m_qInvTrimeshRot;
        private final DMatrix3 m_mTrimeshRot = new DMatrix3();
        private final DVector3 m_vTrimeshPos = new DVector3();

        // global collider data
        private final DVector3 m_vBestPoint = new DVector3();
        private double m_fBestDepth;
        //TODO remove/report ?
        @SuppressWarnings("unused")
        private double m_fBestCenter;
        private double m_fBestrt;
        private int m_iBestAxis;
        private final DVector3 m_vContactNormal = new DVector3();
        private final DVector3 m_vNormal = new DVector3();
        private final DVector3 m_vE0 = new DVector3();
        private final DVector3 m_vE1 = new DVector3();
        private final DVector3 m_vE2 = new DVector3();

        // ODE stuff
        int m_iFlags;

        int m_nContacts;

        sLocalContactData[] m_gLocalContacts;

        // Use to classify contacts to be "near" in position
        private static final double fSameContactPositionEpsilon = (0.0001); // 1e-4
        // Use to classify contacts to be "near" in normal direction
        private static final double fSameContactNormalEpsilon = (0.0001); // 1e-4

        // If this two contact can be classified as "near"
        //inline int _IsNearContacts(sLocalContactData& c1,sLocalContactData& c2)
        private static final boolean _IsNearContacts(sLocalContactData c1, sLocalContactData c2) {
            boolean bPosNear = false;
            boolean bSameDir = false;
            DVector3 vDiff = new DVector3();

            // First check if they are "near" in position
            vDiff.eqDiff(c1.vPos, c2.vPos);//dVector3Subtract(c1.vPos,c2.vPos,vDiff);
            if ((Math.abs(vDiff.get0()) < fSameContactPositionEpsilon)
                    && (Math.abs(vDiff.get1()) < fSameContactPositionEpsilon)
                    && (Math.abs(vDiff.get2()) < fSameContactPositionEpsilon)) {
                bPosNear = true;
            }

            // Second check if they are "near" in normal direction
            vDiff.eqDiff(c1.vNormal, c2.vNormal);//dVector3Subtract(c1.vNormal,c2.vNormal,vDiff);
            if ((Math.abs(vDiff.get0()) < fSameContactNormalEpsilon)
                    && (Math.abs(vDiff.get1()) < fSameContactNormalEpsilon)
                    && (Math.abs(vDiff.get2()) < fSameContactNormalEpsilon)) {
                bSameDir = true;
            }

            // Will be "near" if position and normal direction are "near"
            return (bPosNear && bSameDir);
        }

        private static final boolean _IsBetter(sLocalContactData c1, sLocalContactData c2) {
            // The not better will be throw away
            // You can change the selection criteria here
            return (c1.fDepth > c2.fDepth);
        }

        // iterate through gLocalContacts and filtered out "near contact"
        void _OptimizeLocalContacts() {
            int nContacts = m_nContacts;

            for (int i = 0; i < nContacts - 1; i++) {
                for (int j = i + 1; j < nContacts; j++) {
                    if (_IsNearContacts(m_gLocalContacts[i], m_gLocalContacts[j])) {
                        // If they are seem to be the same then filtered
                        // out the least penetrate one
                        if (_IsBetter(m_gLocalContacts[j], m_gLocalContacts[i])) {
                            m_gLocalContacts[i].nFlags = 0; // filtered 1st contact
                        } else {
                            m_gLocalContacts[j].nFlags = 0; // filtered 2nd contact
                        }

                        // NOTE
                        // There is other way is to add two depth together but
                        // it not work so well. Why???
                    }
                }
            }
        }

        int _ProcessLocalContacts(DContactGeomBuffer contacts,
                                  DxCylinder Cylinder, DxTriMesh Trimesh) {
            if (m_nContacts > 1 && !((m_iFlags & OdeConstants.CONTACTS_UNIMPORTANT) != 0)) {
                // Can be optimized...
                _OptimizeLocalContacts();
            }

            int iContact = 0;
            DContactGeom Contact = null;

            int nFinalContact = 0;

            for (iContact = 0; iContact < m_nContacts; iContact++) {
                if (1 == m_gLocalContacts[iContact].nFlags) {
                    Contact = contacts.getSafe(m_iFlags, nFinalContact);
                    Contact.depth = m_gLocalContacts[iContact].fDepth;
                    Contact.normal.set(m_gLocalContacts[iContact].vNormal);
                    Contact.pos.set(m_gLocalContacts[iContact].vPos);
                    Contact.g1 = Cylinder;
                    Contact.g2 = Trimesh;
                    Contact.side1 = -1;
                    Contact.side2 = m_gLocalContacts[iContact].triIndex;
                    Contact.normal.scale(-1);

                    nFinalContact++;
                }
            }
            return nFinalContact;
        }

        boolean _cldTestAxis(
                final DVector3C v0,
                final DVector3C v1,
                final DVector3C v2,
                final DVector3 vAxis,
                final int iAxis,
                final boolean bNoFlip/* = false*/) {

            // calculate length of separating axis vector
            double fL = vAxis.length();//dVector3Length(vAxis);
            // if not long enough
            if (fL < (1e-5)) {
                // do nothing
                return true;
            }

            // otherwise normalize it
            vAxis.scale(1. / fL);

            double fdot1 = m_vCylinderAxis.dot(vAxis);//dVector3Dot(m_vCylinderAxis,vAxis);
            // project capsule on vAxis
            double frc;

            if (Math.abs(fdot1) > (1.0)) {
                frc = Math.abs(m_fCylinderSize * (0.5));
            } else {
                frc = Math.abs((m_fCylinderSize * (0.5)) * fdot1)
                        + m_fCylinderRadius * Math.sqrt((1.0) - (fdot1 * fdot1));
            }

            DVector3 vV0 = new DVector3();
            vV0.eqDiff(v0, m_vCylinderPos); //dVector3Subtract(v0,m_vCylinderPos,vV0);
            DVector3 vV1 = new DVector3();
            vV1.eqDiff(v1, m_vCylinderPos); //dVector3Subtract(v1,m_vCylinderPos,vV1);
            DVector3 vV2 = new DVector3();
            vV2.eqDiff(v2, m_vCylinderPos); //dVector3Subtract(v2,m_vCylinderPos,vV2);

            // project triangle on vAxis
            double[] afv = new double[3];
            afv[0] = vV0.dot(vAxis);//dVector3Dot( vV0 , vAxis );
            afv[1] = vV1.dot(vAxis);//dVector3Dot( vV1 , vAxis );
            afv[2] = vV2.dot(vAxis);//dVector3Dot( vV2 , vAxis );

            double fMin = MAX_REAL;
            double fMax = -MAX_REAL;

            // for each vertex
            for (int i = 0; i < 3; i++) {
                // find minimum
                if (afv[i] < fMin) {
                    fMin = afv[i];
                }
                // find maximum
                if (afv[i] > fMax) {
                    fMax = afv[i];
                }
            }

            // find capsule's center of interval on axis
            double fCenter = (fMin + fMax) * (0.5);
            // calculate triangles halfinterval
            double fTriangleRadius = (fMax - fMin) * (0.5);

            // if they do not overlap,
            if (Math.abs(fCenter) > (frc + fTriangleRadius)) {
                // exit, we have no intersection
                return false;
            }

            // calculate depth
            double fDepth = -(Math.abs(fCenter) - (frc + fTriangleRadius));

            // if greater then best found so far
            if (fDepth < m_fBestDepth) {
                // remember depth
                m_fBestDepth = fDepth;
                m_fBestCenter = fCenter;
                m_fBestrt = frc;
                dVector3Copy(vAxis, m_vContactNormal);
                m_iBestAxis = iAxis;

                // flip normal if interval is wrong faced
                if (fCenter < (0.0) && !bNoFlip) {
                    dVector3Inv(m_vContactNormal);
                    m_fBestCenter = -fCenter;
                }
            }

            return true;
        }

        // intersection test between edge and circle
        boolean _cldTestCircleToEdgeAxis(
                final DVector3C v0, final DVector3C v1, final DVector3C v2,
                final DVector3C vCenterPoint, final DVector3C vCylinderAxis1,
                final DVector3C vVx0, final DVector3C vVx1, final int iAxis) {
            // calculate direction of edge
            DVector3 vkl = new DVector3();
            dVector3Subtract(vVx1, vVx0, vkl);
            vkl.normalize();//dNormalize3(vkl);
            // starting point of edge
            DVector3 vol = new DVector3();
            dVector3Copy(vVx0, vol);

            // calculate angle cosine between cylinder axis and edge
            double fdot2 = vkl.dot(vCylinderAxis1);

            // if edge is perpendicular to cylinder axis
            if (Math.abs(fdot2) < (1e-5)) {
                // this can't be separating axis, because edge is parallel to circle plane
                return true;
            }

            // find point of intersection between edge line and circle plane
            DVector3 vTemp = new DVector3();
            dVector3Subtract(vCenterPoint, vol, vTemp);
            double fdot1 = vTemp.dot(vCylinderAxis1);
            DVector3 vpnt = new DVector3();
            vpnt.eqSum(vol, vkl, fdot1 / fdot2);

            // find tangent vector on circle with same center (vCenterPoint) that touches point of intersection (vpnt)
            DVector3 vTangent = new DVector3();
            dVector3Subtract(vCenterPoint, vpnt, vTemp);
            dVector3Cross(vTemp, vCylinderAxis1, vTangent);

            // find vector orthogonal both to tangent and edge direction
            DVector3 vAxis = new DVector3();
            dVector3Cross(vTangent, vkl, vAxis);

            // use that vector as separating axis
            return _cldTestAxis(v0, v1, v2, vAxis, iAxis, false);
        }

        // helper for less key strokes
        private static final void _CalculateAxis(final DVector3C v1,
                                                 final DVector3C v2,
                                                 final DVector3C v3,
                                                 final DVector3 r) {
            DVector3 t1 = new DVector3();
            DVector3 t2 = new DVector3();

            t1.eqDiff(v1, v2);//dVector3Subtract(v1,v2,t1);
            OdeMath.dCalcVectorCross3(t2, t1, v3);//dVector3Cross(t1,v3,t2);
            OdeMath.dCalcVectorCross3(r, t2, v3);//dVector3Cross(t2,v3,r);
        }

        boolean _cldTestSeparatingAxes(
                final DVector3C v0,
                final DVector3C v1,
                final DVector3C v2) {

            // calculate edge vectors
            dVector3Subtract(v1, v0, m_vE0);
            // m_vE1 has been calculated before -> so save some cycles here
            dVector3Subtract(v0, v2, m_vE2);

            // calculate caps centers in absolute space
            DVector3 vCp0 = new DVector3();
            vCp0.eqSum(m_vCylinderPos, m_vCylinderAxis, m_fCylinderSize * 0.5);

            // reset best axis
            m_iBestAxis = 0;
            DVector3 vAxis = new DVector3();

            // axis m_vNormal
            vAxis.set(m_vNormal).scale(-1);
            if (!_cldTestAxis(v0, v1, v2, vAxis, 1, true)) {
                return false;
            }

            // axis CxE0
            dVector3Cross(m_vCylinderAxis, m_vE0, vAxis);
            if (!_cldTestAxis(v0, v1, v2, vAxis, 2, false)) {
                return false;
            }

            // axis CxE1
            dVector3Cross(m_vCylinderAxis, m_vE1, vAxis);
            if (!_cldTestAxis(v0, v1, v2, vAxis, 3, false)) {
                return false;
            }

            // axis CxE2
            dVector3Cross(m_vCylinderAxis, m_vE2, vAxis);
            if (!_cldTestAxis(v0, v1, v2, vAxis, 4, false)) {
                return false;
            }

            // first vertex on triangle
            // axis ((V0-Cp0) x C) x C
            _CalculateAxis(v0, vCp0, m_vCylinderAxis, vAxis);
            if (!_cldTestAxis(v0, v1, v2, vAxis, 11, false)) {
                return false;
            }

            // second vertex on triangle
            // axis ((V1-Cp0) x C) x C
            _CalculateAxis(v1, vCp0, m_vCylinderAxis, vAxis);
            if (!_cldTestAxis(v0, v1, v2, vAxis, 12, false)) {
                return false;
            }

            // third vertex on triangle
            // axis ((V2-Cp0) x C) x C
            _CalculateAxis(v2, vCp0, m_vCylinderAxis, vAxis);
            if (!_cldTestAxis(v0, v1, v2, vAxis, 13, false)) {
                return false;
            }

            // test cylinder axis
            dVector3Copy(m_vCylinderAxis, vAxis);
            if (!_cldTestAxis(v0, v1, v2, vAxis, 14, false)) {
                return false;
            }

            // Test top and bottom circle ring of cylinder for separation
            DVector3 vccATop = new DVector3();
            vccATop.eqSum(m_vCylinderPos, m_vCylinderAxis, m_fCylinderSize * 0.5);

            DVector3 vccABottom = new DVector3();
            vccABottom.eqSum(m_vCylinderPos, m_vCylinderAxis, -m_fCylinderSize * (0.5));

            if (!_cldTestCircleToEdgeAxis(v0, v1, v2, vccATop, m_vCylinderAxis, v0, v1, 15)) {
                return false;
            }

            if (!_cldTestCircleToEdgeAxis(v0, v1, v2, vccATop, m_vCylinderAxis, v1, v2, 16)) {
                return false;
            }

            if (!_cldTestCircleToEdgeAxis(v0, v1, v2, vccATop, m_vCylinderAxis, v0, v2, 17)) {
                return false;
            }

            if (!_cldTestCircleToEdgeAxis(v0, v1, v2, vccABottom, m_vCylinderAxis, v0, v1, 18)) {
                return false;
            }

            if (!_cldTestCircleToEdgeAxis(v0, v1, v2, vccABottom, m_vCylinderAxis, v1, v2, 19)) {
                return false;
            }

            if (!_cldTestCircleToEdgeAxis(v0, v1, v2, vccABottom, m_vCylinderAxis, v0, v2, 20)) {
                return false;
            }

            return true;
        }

        boolean _cldClipCylinderEdgeToTriangle(
                final DVector3C v0, final DVector3C v1, final DVector3C v2) {
            // translate cylinder
            double fTemp = m_vCylinderAxis.dot(m_vContactNormal);
            DVector3 vN2 = new DVector3();
            vN2.eqSum(m_vContactNormal, m_vCylinderAxis, -fTemp);

            fTemp = vN2.length();
            if (fTemp < (1e-5)) {
                return false;
            }

            // Normalize it
            vN2.scale(1. / fTemp);

            // calculate caps centers in absolute space
            DVector3 vCposTrans = new DVector3();
            vCposTrans.eqSum(m_vCylinderPos, vN2, m_fCylinderRadius);

            DVector3 vCEdgePoint0 = new DVector3();
            vCEdgePoint0.eqSum(vCposTrans, m_vCylinderAxis, m_fCylinderSize * (0.5));

            DVector3 vCEdgePoint1 = new DVector3();
            vCEdgePoint1.eqSum(vCposTrans, m_vCylinderAxis, -m_fCylinderSize * 0.5);

            // transform cylinder edge points into triangle space
            vCEdgePoint0.sub(v0);

            vCEdgePoint1.sub(v0);

            DVector4 plPlane = new DVector4();
            DVector3 vPlaneNormal = new DVector3();

            // triangle plane
            vPlaneNormal.set(m_vNormal).scale(-1);
            dConstructPlane(vPlaneNormal, (0.0), plPlane);
            if (!dClipEdgeToPlane(vCEdgePoint0, vCEdgePoint1, plPlane)) {
                return false;
            }

            // plane with edge 0
            dVector3Cross(m_vNormal, m_vE0, vPlaneNormal);
            dConstructPlane(vPlaneNormal, (1e-5), plPlane);
            if (!dClipEdgeToPlane(vCEdgePoint0, vCEdgePoint1, plPlane)) {
                return false;
            }

            // plane with edge 1
            dVector3Cross(m_vNormal, m_vE1, vPlaneNormal);
            fTemp = m_vE0.dot(vPlaneNormal) - (1e-5);
            //plPlane = Plane4f( vTemp, -(( m_vE0 dot vTemp )-REAL(1e-5)));
            dConstructPlane(vPlaneNormal, -fTemp, plPlane);
            if (!dClipEdgeToPlane(vCEdgePoint0, vCEdgePoint1, plPlane)) {
                return false;
            }

            // plane with edge 2
            dVector3Cross(m_vNormal, m_vE2, vPlaneNormal);
            dConstructPlane(vPlaneNormal, (1e-5), plPlane);
            if (!dClipEdgeToPlane(vCEdgePoint0, vCEdgePoint1, plPlane)) {
                return false;
            }

            // return capsule edge points into absolute space
            vCEdgePoint0.add(v0);

            vCEdgePoint1.add(v0);

            // calculate depths for both contact points
            DVector3 vTemp = new DVector3();
            dVector3Subtract(vCEdgePoint0, m_vCylinderPos, vTemp);
            double fRestDepth0 = -vTemp.dot(m_vContactNormal) + m_fBestrt;
            dVector3Subtract(vCEdgePoint1, m_vCylinderPos, vTemp);
            double fRestDepth1 = -vTemp.dot(m_vContactNormal) + m_fBestrt;

            double fDepth0 = m_fBestDepth - (fRestDepth0);
            double fDepth1 = m_fBestDepth - (fRestDepth1);

            // clamp depths to zero
            if (fDepth0 < (0.0)) {
                fDepth0 = (0.0);
            }

            if (fDepth1 < (0.0)) {
                fDepth1 = (0.0);
            }

            // Generate contact 0
            {
                if (m_gLocalContacts[m_nContacts] == null)
                    m_gLocalContacts[m_nContacts] = new sLocalContactData();//TZ fix TODO
                m_gLocalContacts[m_nContacts].fDepth = fDepth0;
                dVector3Copy(m_vContactNormal, m_gLocalContacts[m_nContacts].vNormal);
                dVector3Copy(vCEdgePoint0, m_gLocalContacts[m_nContacts].vPos);
                m_gLocalContacts[m_nContacts].nFlags = 1;
                m_nContacts++;
                if (m_nContacts >= (m_iFlags & DxGeom.NUMC_MASK))
                    return true;
            }

            // Generate contact 1
            {
                // generate contacts
                m_gLocalContacts[m_nContacts].fDepth = fDepth1;
                dVector3Copy(m_vContactNormal, m_gLocalContacts[m_nContacts].vNormal);
                dVector3Copy(vCEdgePoint1, m_gLocalContacts[m_nContacts].vPos);
                m_gLocalContacts[m_nContacts].nFlags = 1;
                m_nContacts++;
            }

            return true;
        }

        void _cldClipCylinderToTriangle(
                final DVector3C v0, final DVector3C v1, final DVector3C v2) {
            int i = 0;
            DVector3[] avPoints = {new DVector3(), new DVector3(), new DVector3()};//[3];
            DVector3[] avTempArray1 = DVector3.newArray(nMAX_CYLINDER_TRIANGLE_CLIP_POINTS);
            DVector3[] avTempArray2 = DVector3.newArray(nMAX_CYLINDER_TRIANGLE_CLIP_POINTS);

            // setup array of triangle vertices
            dVector3Copy(v0, avPoints[0]);
            dVector3Copy(v1, avPoints[1]);
            dVector3Copy(v2, avPoints[2]);

            DVector3 vCylinderCirclePos = new DVector3(), vCylinderCircleNormal_Rel = new DVector3();
            vCylinderCircleNormal_Rel.setZero();
            // check which circle from cylinder we take for clipping
            if (m_vCylinderAxis.dot(m_vContactNormal) > (0.0)) {
                // get top circle
                vCylinderCirclePos.eqSum(m_vCylinderPos, m_vCylinderAxis, m_fCylinderSize * 0.5);

                vCylinderCircleNormal_Rel.set(nCYLINDER_AXIS, -1.0);
            } else {
                // get bottom circle
                vCylinderCirclePos.eqSum(m_vCylinderPos, m_vCylinderAxis, -m_fCylinderSize * 0.5);

                vCylinderCircleNormal_Rel.set(nCYLINDER_AXIS, 1.0);
            }

            DVector3 vTemp = new DVector3();
            dQuatInv(m_qCylinderRot, m_qInvCylinderRot);
            // transform triangle points to space of cylinder circle
            for (i = 0; i < 3; i++) {
                dVector3Subtract(avPoints[i], vCylinderCirclePos, vTemp);
                dQuatTransform(m_qInvCylinderRot, vTemp, avPoints[i]);
            }

            int iTmpCounter1 = 0;
            int iTmpCounter2 = 0;
            DVector4 plPlane = new DVector4();

            // plane of cylinder that contains circle for intersection
            dConstructPlane(vCylinderCircleNormal_Rel, (0.0), plPlane);
            iTmpCounter1 = dClipPolyToPlane(avPoints, 3, avTempArray1, plPlane);

            // Body of base circle of Cylinder
            int nCircleSegment = 0;
            for (nCircleSegment = 0; nCircleSegment < nCYLINDER_CIRCLE_SEGMENTS; nCircleSegment++) {
                dConstructPlane(m_avCylinderNormals[nCircleSegment], m_fCylinderRadius, plPlane);

                if (0 == (nCircleSegment % 2)) {
                    iTmpCounter2 = dClipPolyToPlane(avTempArray1, iTmpCounter1, avTempArray2, plPlane);
                } else {
                    iTmpCounter1 = dClipPolyToPlane(avTempArray2, iTmpCounter2, avTempArray1, plPlane);
                }

                Common.dIASSERT(iTmpCounter1 >= 0 && iTmpCounter1 <= nMAX_CYLINDER_TRIANGLE_CLIP_POINTS);
                Common.dIASSERT(iTmpCounter2 >= 0 && iTmpCounter2 <= nMAX_CYLINDER_TRIANGLE_CLIP_POINTS);
            }

            // back transform clipped points to absolute space
            double ftmpdot;
            double fTempDepth;
            DVector3 vPoint = new DVector3();

            if (nCircleSegment % 2 != 0) {
                for (i = 0; i < iTmpCounter2; i++) {
                    dQuatTransform(m_qCylinderRot, avTempArray2[i], vPoint);
                    vPoint.add(vCylinderCirclePos);

                    dVector3Subtract(vPoint, m_vCylinderPos, vTemp);
                    ftmpdot = Math.abs(vTemp.dot(m_vContactNormal));
                    fTempDepth = m_fBestrt - ftmpdot;
                    // Depth must be positive
                    if (fTempDepth > (0.0)) {
                        m_gLocalContacts[m_nContacts].fDepth = fTempDepth;
                        dVector3Copy(m_vContactNormal, m_gLocalContacts[m_nContacts].vNormal);
                        dVector3Copy(vPoint, m_gLocalContacts[m_nContacts].vPos);
                        m_gLocalContacts[m_nContacts].nFlags = 1;
                        m_nContacts++;
                        if (m_nContacts >= (m_iFlags & DxGeom.NUMC_MASK))
                            return;
                        ;
                    }
                }
            } else {
                for (i = 0; i < iTmpCounter1; i++) {
                    dQuatTransform(m_qCylinderRot, avTempArray1[i], vPoint);
                    vPoint.add(vCylinderCirclePos);

                    dVector3Subtract(vPoint, m_vCylinderPos, vTemp);
                    ftmpdot = Math.abs(vTemp.dot(m_vContactNormal));
                    fTempDepth = m_fBestrt - ftmpdot;
                    // Depth must be positive
                    if (fTempDepth > (0.0)) {
                        m_gLocalContacts[m_nContacts].fDepth = fTempDepth;
                        dVector3Copy(m_vContactNormal, m_gLocalContacts[m_nContacts].vNormal);
                        dVector3Copy(vPoint, m_gLocalContacts[m_nContacts].vPos);
                        m_gLocalContacts[m_nContacts].nFlags = 1;
                        m_nContacts++;
                        if (m_nContacts >= (m_iFlags & DxGeom.NUMC_MASK))
                            return;
                        ;
                    }
                }
            }
        }

        void TestOneTriangleVsCylinder(
                final DVector3C v0,
                final DVector3C v1,
                final DVector3C v2,
                final boolean bDoubleSided) {
            // calculate triangle normal
            dVector3Subtract(v2, v1, m_vE1);
            DVector3 vTemp = new DVector3();
            dVector3Subtract(v0, v1, vTemp);
            dVector3Cross(m_vE1, vTemp, m_vNormal);

            // Even though all triangles might be initially valid,
            // a triangle may degenerate into a segment after applying
            // space transformation.
            if (!OdeMath.dSafeNormalize3(m_vNormal)) {
                return;
            }

            // create plane from triangle
            //Plane4f plTrianglePlane = Plane4f( vPolyNormal, v0 );
            double plDistance = -v0.dot(m_vNormal);
            DVector4 plTrianglePlane = new DVector4();
            dConstructPlane(m_vNormal, plDistance, plTrianglePlane);

            // calculate sphere distance to plane
            double fDistanceCylinderCenterToPlane = dPointPlaneDistance(m_vCylinderPos, plTrianglePlane);

            // Sphere must be over positive side of triangle
            if (fDistanceCylinderCenterToPlane < 0 && !bDoubleSided) {
                // if not don't generate contacts
                return;
            }

            DVector3 vPnt0 = new DVector3();
            DVector3 vPnt1 = new DVector3();
            DVector3 vPnt2 = new DVector3();

            if (fDistanceCylinderCenterToPlane < (0.0)) {
                // flip it
                dVector3Copy(v0, vPnt0);
                dVector3Copy(v1, vPnt2);
                dVector3Copy(v2, vPnt1);
            } else {
                dVector3Copy(v0, vPnt0);
                dVector3Copy(v1, vPnt1);
                dVector3Copy(v2, vPnt2);
            }

            m_fBestDepth = MAX_REAL;

            // do intersection test and find best separating axis
            if (!_cldTestSeparatingAxes(vPnt0, vPnt1, vPnt2)) {
                // if not found do nothing
                return;
            }

            // if best separation axis is not found
            if (m_iBestAxis == 0) {
                // this should not happen (we should already exit in that case)
                Common.dIASSERT(false);
                // do nothing
                return;
            }

            double fdot = m_vContactNormal.dot(m_vCylinderAxis);

            // choose which clipping method are we going to apply
            if (Math.abs(fdot) < (0.9)) {
                if (!_cldClipCylinderEdgeToTriangle(vPnt0, vPnt1, vPnt2)) {
                    return;
                }
            } else {
                _cldClipCylinderToTriangle(vPnt0, vPnt1, vPnt2);
            }
        }

        //	void sCylinderTrimeshColliderData::_InitCylinderTrimeshData(dxGeom *Cylinder, dxTriMesh *Trimesh)
        void _InitCylinderTrimeshData(DxCylinder Cylinder, DxTriMesh Trimesh) {
            // get cylinder information
            // Rotation
            DMatrix3C pRotCyc = Cylinder.getRotation();
            m_mCylinderRot.set(pRotCyc);
            m_qCylinderRot.set(Cylinder.getQuaternion());

            // Position
            DVector3C pPosCyc = Cylinder.getPosition();
            dVector3Copy(pPosCyc, m_vCylinderPos);
            // Cylinder axis
            dMat3GetCol(m_mCylinderRot, nCYLINDER_AXIS, m_vCylinderAxis);
            // get cylinder radius and size
            //dGeomCylinderGetParams(Cylinder,m_fCylinderRadius,m_fCylinderSize);
            m_fCylinderRadius = Cylinder.getRadius();
            m_fCylinderSize = Cylinder.getLength();

            // get trimesh position and orientation
            DMatrix3C pRotTris = Trimesh.getRotation();
            m_mTrimeshRot.set(pRotTris);
            m_qTrimeshRot = Trimesh.getQuaternion();  //TODO TZ copy instead?

            // Position
            DVector3C pPosTris = Trimesh.getPosition();
            dVector3Copy(pPosTris, m_vTrimeshPos);


            // calculate basic angle for 8-gon
            double fAngle = OdeMath.M_PI / nCYLINDER_CIRCLE_SEGMENTS;
            // calculate angle increment
            double fAngleIncrement = fAngle * (2.0);

            // calculate plane normals
            // axis dependant code
            for (int i = 0; i < nCYLINDER_CIRCLE_SEGMENTS; i++) {
                m_avCylinderNormals[i] = new DVector3();
                m_avCylinderNormals[i].set0(-Math.cos(fAngle));
                m_avCylinderNormals[i].set1(-Math.sin(fAngle));
                m_avCylinderNormals[i].set2(0.0);

                fAngle += fAngleIncrement;
            }

            m_vBestPoint.setZero();
            // reset best depth
            m_fBestCenter = (0.0);
        }

        //	int sCylinderTrimeshColliderData::TestCollisionForSingleTriangle(int ctContacts0,
//		int Triint, dVector3 dv[3], bool &bOutFinishSearching)
        int TestCollisionForSingleTriangle(int ctContacts0,
                                           final int Triint, final DVector3[] dv, final RefBoolean bOutFinishSearching) {
            // test this triangle
            TestOneTriangleVsCylinder(dv[0], dv[1], dv[2], false);

            // fill-in tri index for generated contacts
            for (; ctContacts0 < m_nContacts; ctContacts0++)
                m_gLocalContacts[ctContacts0].triIndex = Triint;

            // Putting "break" at the end of loop prevents unnecessary checks on first pass and "continue"
            bOutFinishSearching.b = (m_nContacts >= (m_iFlags & DxGeom.NUMC_MASK));

            return ctContacts0;
        }

    }

//	// OPCODE version of cylinder to mesh collider
//	#if dTRIMESH_OPCODE
//	static void dQueryCTLPotentialCollisionTriangles(OBBCollider &Collider, 
//		sCylinderTrimeshColliderData &cData, dxGeom *Cylinder, dxTriMesh *Trimesh,
//		OBBCache &BoxCache)
//	{
//		const dVector3 &vCylinderPos = cData.m_vCylinderPos;
//
//		Point cCenter(vCylinderPos[0],vCylinderPos[1],vCylinderPos[2]);
//
//		Point cExtents(cData.m_fCylinderRadius,cData.m_fCylinderRadius,cData.m_fCylinderRadius);
//		cExtents[nCYLINDER_AXIS] = cData.m_fCylinderSize * REAL(0.5);
//
//		Matrix3x3 obbRot;
//
//		const dMatrix3 &mCylinderRot = cData.m_mCylinderRot;
//
//		// It is a potential issue to explicitly cast to float 
//		// if custom width floating point type is introduced in OPCODE.
//		// It is necessary to make a typedef and cast to it
//		// (e.g. typedef float opc_float;)
//		// However I'm not sure in what header it should be added.
//
//		obbRot[0][0] = /*(float)*/mCylinderRot[0];
//		obbRot[1][0] = /*(float)*/mCylinderRot[1];
//		obbRot[2][0] = /*(float)*/mCylinderRot[2];
//
//		obbRot[0][1] = /*(float)*/mCylinderRot[4];
//		obbRot[1][1] = /*(float)*/mCylinderRot[5];
//		obbRot[2][1] = /*(float)*/mCylinderRot[6];
//
//		obbRot[0][2] = /*(float)*/mCylinderRot[8];
//		obbRot[1][2] = /*(float)*/mCylinderRot[9];
//		obbRot[2][2] = /*(float)*/mCylinderRot[10];
//
//		OBB obbCapsule(cCenter,cExtents,obbRot);
//
//		Matrix4x4 CapsuleMatrix;
//		MakeMatrix(vCylinderPos, mCylinderRot, CapsuleMatrix);
//
//		Matrix4x4 MeshMatrix;
//		MakeMatrix(cData.m_vTrimeshPos, cData.m_mTrimeshRot, MeshMatrix);
//
//		// TC results
//		if (Trimesh->doBoxTC) 
//		{
//			dxTriMesh::BoxTC* BoxTC = 0;
//			for (int i = 0; i < Trimesh->BoxTCCache.size(); i++)
//			{
//				if (Trimesh->BoxTCCache[i].Geom == Cylinder)
//				{
//					BoxTC = &Trimesh->BoxTCCache[i];
//					break;
//				}
//			}
//			if (!BoxTC)
//			{
//				Trimesh->BoxTCCache.push(dxTriMesh::BoxTC());
//
//				BoxTC = &Trimesh->BoxTCCache[Trimesh->BoxTCCache.size() - 1];
//				BoxTC->Geom = Cylinder;
//				BoxTC->FatCoeff = REAL(1.0);
//			}
//
//			// Intersect
//			Collider.SetTemporalCoherence(true);
//			Collider.Collide(*BoxTC, obbCapsule, Trimesh->Data->BVTree, null, &MeshMatrix);
//		}
//		else 
//		{
//			Collider.SetTemporalCoherence(false);
//			Collider.Collide(BoxCache, obbCapsule, Trimesh->Data->BVTree, null,&MeshMatrix);
//		}
//	}
//
//	int dCollideCylinderTrimesh(dxGeom *o1, dxGeom *o2, int flags, dContactGeom *contact, int skip)
//	{
//		dIASSERT( skip >= (int)sizeof( dContactGeom ) );
//		dIASSERT( o1->type == dCylinderClass );
//		dIASSERT( o2->type == dTriMeshClass );
//		dIASSERT ((flags & NUMC_MASK) >= 1);
//
//		int nContactCount = 0;
//
//		dxGeom *Cylinder = o1;
//		dxTriMesh *Trimesh = (dxTriMesh *)o2;
//
//		// Main data holder
//		sCylinderTrimeshColliderData cData(flags, skip);
//		cData._InitCylinderTrimeshData(Cylinder, Trimesh);
//
//		const unsigned uiTLSKind = Trimesh->getParentSpaceTLSKind();
//		dIASSERT(uiTLSKind == Cylinder->getParentSpaceTLSKind()); // The colliding spaces must use matching cleanup method
//		TrimeshCollidersCache *pccColliderCache = GetTrimeshCollidersCache(uiTLSKind);
//		OBBCollider& Collider = pccColliderCache->_OBBCollider;
//
//		dQueryCTLPotentialCollisionTriangles(Collider, cData, Cylinder, Trimesh, pccColliderCache->defaultBoxCache);
//
//		// Retrieve data
//		int TriCount = Collider.GetNbTouchedPrimitives();
//
//		if (TriCount != 0)
//		{
//			const int* Triangles = (const int*)Collider.GetTouchedPrimitives();
//
//			if (Trimesh->ArrayCallback != null)
//			{
//				Trimesh->ArrayCallback(Trimesh, Cylinder, Triangles, TriCount);
//			}
//
//			// allocate buffer for local contacts on stack
//			cData.m_gLocalContacts = (sLocalContactData*)dALLOCA16(sizeof(sLocalContactData)*(cData.m_iFlags & NUMC_MASK));
//
//		    int ctContacts0 = 0;
//
//			// loop through all intersecting triangles
//			for (int i = 0; i < TriCount; i++)
//			{
//				const int Triint = Triangles[i];
//				if (!Callback(Trimesh, Cylinder, Triint)) continue;
//
//
//				dVector3 dv[3];
//				FetchTriangle(Trimesh, Triint, cData.m_vTrimeshPos, cData.m_mTrimeshRot, dv);
//
//				bool bFinishSearching;
//				ctContacts0 = cData.TestCollisionForSingleTriangle(ctContacts0, Triint, dv, bFinishSearching);
//
//				if (bFinishSearching) 
//				{
//					break;
//				}
//			}
//
//			if (cData.m_nContacts != 0)
//			{
//				nContactCount = cData._ProcessLocalContacts(contact, Cylinder, Trimesh);
//			}
//		}
//
//		return nContactCount;
//	}
//	#endif  // OPCODE

    // GIMPACT version of cylinder to mesh collider
//	#if dTRIMESH_GIMPACT
//	int dCollideCylinderTrimesh(DxGeom *o1, dxGeom *o2, int flags, dContactGeom *contact, int skip)
    int dCollideCylinderTrimesh(DxCylinder o1, DxGimpact o2, final int flags, DContactGeomBuffer contacts, int skip) {
        Common.dIASSERT(skip == 1);//(int)sizeof( dContactGeom ) );
        //dIASSERT( o1->type == dCylinderClass );
        //dIASSERT( o2->type == dTriMeshClass );
        Common.dIASSERT((flags & DxGeom.NUMC_MASK) >= 1);

        int nContactCount = 0;

        DxCylinder Cylinder = o1;
        DxGimpact Trimesh = o2;

        // Main data holder
        sCylinderTrimeshColliderData cData = new sCylinderTrimeshColliderData(flags, skip);
        cData._InitCylinderTrimeshData(Cylinder, Trimesh);

        //*****at first , collide box aabb******//

        aabb3f test_aabb = new aabb3f();

        test_aabb.minX = (float) o1._aabb.getMin0();
        test_aabb.maxX = (float) o1._aabb.getMax0();
        test_aabb.minY = (float) o1._aabb.getMin1();
        test_aabb.maxY = (float) o1._aabb.getMax1();
        test_aabb.minZ = (float) o1._aabb.getMin2();
        test_aabb.maxZ = (float) o1._aabb.getMax2();


        GimDynArrayInt collision_result = GimDynArrayInt.GIM_CREATE_BOXQUERY_LIST();

        Trimesh.m_collision_trimesh.getAabbSet().gim_aabbset_box_collision(test_aabb, collision_result);

        if (collision_result.size() != 0) {
            //*****Set globals for box collision******//

            int ctContacts0 = 0;
            //cData.m_gLocalContacts = null;// TODO TZ (sLocalContactData*)dALLOCA16(sizeof(sLocalContactData)*(cData.m_iFlags & NUMC_MASK));
            cData.m_gLocalContacts = new sLocalContactData[cData.m_iFlags & DxGeom.NUMC_MASK];
            for (int i = 0; i < cData.m_gLocalContacts.length; i++) {
                cData.m_gLocalContacts[i] = new sLocalContactData();
            }

            int[] boxesresult = collision_result.GIM_DYNARRAY_POINTER();
            GimTrimesh ptrimesh = Trimesh.m_collision_trimesh;

            ptrimesh.gim_trimesh_locks_work_data();

            for (int i = 0; i < collision_result.size(); i++) {
                final int Triint = boxesresult[i];

                vec3f[] dvf = {new vec3f(), new vec3f(), new vec3f()};
                ptrimesh.gim_trimesh_get_triangle_vertices(Triint, dvf[0], dvf[1], dvf[2]);

                DVector3[] dv = {new DVector3(), new DVector3(), new DVector3()};
                dv[0].set(dvf[0].f);
                dv[1].set(dvf[1].f);
                dv[2].set(dvf[2].f);
                RefBoolean bFinishSearching = new RefBoolean(false);
                ctContacts0 = cData.TestCollisionForSingleTriangle(ctContacts0, Triint, dv, bFinishSearching);

                if (bFinishSearching.b) {
                    break;
                }
            }

            ptrimesh.gim_trimesh_unlocks_work_data();

            if (cData.m_nContacts != 0) {
                nContactCount = cData._ProcessLocalContacts(contacts, Cylinder, Trimesh);
            }
        }

        collision_result.GIM_DYNARRAY_DESTROY();

        return nContactCount;
    }
//	#endif
//
//	#endif // dTRIMESH_ENABLED
}
