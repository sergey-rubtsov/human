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
import org.ode4j.ode.DAABB;
import org.ode4j.ode.DAABBC;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DColliderFn;
import org.ode4j.ode.DContactGeom;
import org.ode4j.ode.DContactGeomBuffer;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.OdeConfig;
import org.ode4j.ode.internal.DxQuadTreeSpace.Block;
import org.ode4j.ode.internal.Objects_H.DxPosR;
import org.ode4j.ode.internal.Objects_H.DxPosRC;
import org.ode4j.ode.internal.cpp4j.java.Ref;
import org.ode4j.ode.internal.cpp4j.java.RefInt;

import static org.ode4j.ode.DRotation.dRfromQ;
import static org.ode4j.ode.OdeMath.dMultiply0_331;
import static org.ode4j.ode.OdeMath.dMultiply0_333;
import static org.ode4j.ode.OdeMath.dMultiply1_331;
import static org.ode4j.ode.internal.CollisionLibccd.CollideBoxCylinderCCD;
import static org.ode4j.ode.internal.CollisionLibccd.CollideCapsuleCylinder;
import static org.ode4j.ode.internal.CollisionLibccd.CollideConvexBoxCCD;
import static org.ode4j.ode.internal.CollisionLibccd.CollideConvexCapsuleCCD;
import static org.ode4j.ode.internal.CollisionLibccd.CollideConvexConvexCCD;
import static org.ode4j.ode.internal.CollisionLibccd.CollideConvexCylinderCCD;
import static org.ode4j.ode.internal.CollisionLibccd.CollideConvexSphereCCD;
import static org.ode4j.ode.internal.CollisionLibccd.CollideCylinderCylinder;
import static org.ode4j.ode.internal.Common.dAASSERT;
import static org.ode4j.ode.internal.Common.dIASSERT;
import static org.ode4j.ode.internal.Common.dUASSERT;
import static org.ode4j.ode.internal.Rotation.dQfromR;

/**
 * geometry (collision object).
 * <p>
 * From collision_kernel.cpp.
 */
public abstract class DxGeom extends DBase implements DGeom {

    public static final int NUMC_MASK = 0xffff;

    //****************************************************************************
    // geometry object base class

    // geom flags.
    //
    // GEOM_DIRTY means that the space data structures for this geom are
    // potentially not up to date. NOTE THAT all space parents of a dirty geom
    // are themselves dirty. this is an invariant that must be enforced.
    //
    // GEOM_AABB_BAD means that the cached AABB for this geom is not up to date.
    // note that GEOM_DIRTY does not imply GEOM_AABB_BAD, as the geom might
    // recalculate its own AABB but does not know how to update the space data
    // structures for the space it is in. but GEOM_AABB_BAD implies GEOM_DIRTY.
    // the valid combinations are:
    //			0
    //			GEOM_DIRTY
    //			GEOM_DIRTY|GEOM_AABB_BAD
    //			GEOM_DIRTY|GEOM_AABB_BAD|GEOM_POSR_BAD

    protected final static int GEOM_DIRTY = 1;    // geom is 'dirty', i.e. position unknown
    protected final static int GEOM_POSR_BAD = 2;    // geom's final posr is not valid
    protected final static int GEOM_AABB_BAD = 4;    // geom's AABB is not valid
    protected final static int GEOM_PLACEABLE = 8;   // geom is placeable
    protected final static int GEOM_ENABLED = 16;    // geom is enabled
    protected final static int GEOM_ZERO_SIZED = 32; // geom is zero sized

    protected final static int GEOM_ENABLE_TEST_MASK = GEOM_ENABLED | GEOM_ZERO_SIZED;
    protected final static int GEOM_ENABLE_TEST_VALUE = GEOM_ENABLED;

    enum dxContactMergeOptions {
        DONT_MERGE_CONTACTS,
        MERGE_CONTACT_NORMALS,
        MERGE_CONTACTS_FULLY,
    }

    // geometry object base class. pos and R will either point to a separately
    // allocated buffer (if body is 0 - pos points to the dxPosR object) or to
    // the pos and R of the body (if body nonzero).
    // a dGeom is a pointer to this object.

    //	struct dxGeom : public dBase {
    public int type;        // geom type number, set by subclass constructor
    private int _gflags;        // flags used by geom and space
    Object _data;        // user-defined data pointer
    //	  dBody body;		// dynamics body associated with this object (if any)
    DxBody body;        // dynamics body associated with this object (if any)
    DxGeom body_next;    // next geom in body's linked list of associated geoms
    DxPosR _final_posr;    // final position of the geom in world coordinates
    private DxPosR offset_posr;    // offset from body in local coordinates

    // information used by spaces
    //TODO use linked list, seems simpler.
    //private List<DxGeom> _list = new LinkedList<DxGeom>();
    //dxGeom next;		// next geom in linked list of geoms
//	private final Ref<dxGeom> next = new Ref<dxGeom>();
    /**
     * 'tome' is pointer to a pointer to (this).
     * Usually it is a pointer to the (prev.next) field.
     * For the first element, it is a pointer to (container.first).
     */
//	private final Ref<dxGeom> tome = new Ref<dxGeom>();
    private DxGeom _next = null;// next geom in linked list of geoms
    private DxGeom _prev = null;
    //dxGeom tome;	// linked list backpointer
    private DxGeom _next_ex;    // next geom in extra linked list of geoms (for higher level structures)
    //private DxGeom _tome_ex;	// extra linked list backpointer (for higher level structures)
    DxSpace parent_space;// the space this geom is contained in, 0 if none
    int _sapIdxDirtyEx; // TZ: Used by SAP-Space.
    int _sapIdxGeomEx; // TZ: Used by SAP-Space.
    Block _qtIdxEx; // TZ: Used by QuadTree-Space.

    //double[] aabb = new double[6];	// cached AABB for this space
    DAABB _aabb = new DAABB();    // cached AABB for this space
    //TODO unsigned
    long category_bits, collide_bits;

    //	  dxGeom (dSpace _space, int is_placeable);
    //	  virtual ~dxGeom();

    // Set or clear GEOM_ZERO_SIZED flag
    void updateZeroSizedFlag(boolean is_zero_sized) {
        _gflags = is_zero_sized ? (_gflags | GEOM_ZERO_SIZED) : (_gflags & ~GEOM_ZERO_SIZED);
    }

    // calculate our new final position from our offset and body
    //	  void computePosr();

    // recalculate our new final position if needed
    void recomputePosr() {
        if ((_gflags & GEOM_POSR_BAD) != 0) {
            computePosr();
            _gflags &= ~GEOM_POSR_BAD;
        }
    }

    /**
     * compute the AABB for this object and put it in aabb. this function
     * always performs a fresh computation, it does not inspect the
     * GEOM_AABB_BAD flag.
     */
    abstract void computeAABB();

    /**
     * Calculate oriented bounding box.
     *
     * @param R Orthogonalized rotation matrix.
     */
    void computeOBB(DMatrix3C R) {
        throw new UnsupportedOperationException();
    }

    // utility functions

    // compute the AABB only if it is not current. this function manipulates
    // the GEOM_AABB_BAD flag.
    void recomputeAABB() {
        if ((_gflags & GEOM_AABB_BAD) != 0) {
            // our aabb functions assume final_posr is up to date
            recomputePosr();
            computeAABB();
            _gflags &= ~GEOM_AABB_BAD;
        }
    }

    /**
     * _gflags |= (GEOM_DIRTY|GEOM_AABB_BAD);
     */
    final void setFlagDirtyAndBad() {
        _gflags |= (GEOM_DIRTY | GEOM_AABB_BAD);
    }

    /**
     * _gflags &= (~(GEOM_DIRTY|GEOM_AABB_BAD));
     */
    final void unsetFlagDirtyAndBad() {
        _gflags &= (~(GEOM_DIRTY | GEOM_AABB_BAD));
    }

    /**
     * (_gflags & GEOM_DIRTY)!=0;
     */
    final boolean hasFlagDirty() {
        return (_gflags & GEOM_DIRTY) != 0;
    }

    /**
     * (_gflags & GEOM_AABB_BAD)!=0;
     */
    final boolean hasFlagAabbBad() {
        return (_gflags & GEOM_AABB_BAD) != 0;
    }

    /**
     * (_gflags & GEOM_PLACEABLE)!=0;
     */
    final boolean hasFlagPlaceable() {
        return (_gflags & GEOM_PLACEABLE) != 0;
    }

    final int getFlags() {
        return _gflags;
    }

    final void setFlags(int flags) {
        _gflags = flags;
    }

    /**
     * _gflags |= customFlag;
     */
    final void setFlagCustom(int customFlag) {
        _gflags |= customFlag;
    }

    /**
     * _gflags &= ~customFlag;
     */
    final void unsetFlagCustom(int customFlag) {
        _gflags &= ~customFlag;
    }

    void spaceAdd(DxGeom next, DxSpace parent) {
        _next = next;
        if (_next != null) {
            _next._prev = this;
        }

        _prev = null;
        parent.setFirst(this);
    }

    void spaceRemove(DxSpace parent) {
        if (_next != null) {
            _next._prev = _prev;
        }
        if (_prev != null) {
            _prev._next = _next;
        } else {
            parent.setFirst(_next);
        }
    }

    /**
     * @return next dxGeom.
     * @author Tilmann Zaeschke
     */
    final DxGeom getNext() {
        return _next;
    }

    /**
     * @return next dxGeom.
     * @author Tilmann Zaeschke
     */
    final DxGeom getNextEx() {
        return _next_ex;
    }

    final void setNextEx(DxGeom g) {
        _next_ex = g;
    }

    // add and remove this geom from a linked list maintained by a body.

    private void bodyAdd(DxBody b) {
        body = b;
        body_next = b.geom;
        b.geom = this;
    }

    protected DxGeom(DSpace space, boolean isPlaceable) {
        // setup body vars. invalid type of -1 must be changed by the constructor.
        type = -1;
        _gflags = GEOM_DIRTY | GEOM_AABB_BAD | GEOM_ENABLED;
        if (isPlaceable) _gflags |= GEOM_PLACEABLE;
        _data = null;
        body = null;
        body_next = null;
        if (isPlaceable) {
            _final_posr = dAllocPosr();
            _final_posr.pos.setZero();
            _final_posr.Rw().setIdentity();
        } else {
            _final_posr = null;
        }
        offset_posr = null;

        // setup space vars
        //TODO remove.
        _next = null;
        _prev = null;//tome = null;
        _next_ex = null;
        //_tome_ex = null;
        parent_space = null;
        _aabb.setZero();
        category_bits = ~0;
        collide_bits = ~0;

        // put this geom in a space if required
        if (space != null) space.add(this);
    }

    @Override
    public void DESTRUCTOR() {
        if (parent_space != null) parent_space.dSpaceRemove(this);
        if (((_gflags & GEOM_PLACEABLE) != 0) && (body == null || (body != null && offset_posr != null)))
            dFreePosr(_final_posr);
        if (offset_posr != null) dFreePosr(offset_posr);
        bodyRemove();
    }


    /**
     * Get parent space TLS kind.
     */
    //unsigned
    int getParentSpaceTLSKind() {
        return parent_space != null ? parent_space.tls_kind : DxSpace.dSPACE_TLS_KIND_INIT_VALUE;
    }


    /**
     * test whether the given AABB object intersects with this object, return
     * 1=yes, 0=no. this is used as an early-exit test in the space collision
     * functions. the default implementation returns 1, which is the correct
     * behavior if no more detailed implementation can be provided.
     *
     * @param o
     * @return
     */
    boolean AABBTest(DxGeom o, DAABBC aabb) {
        return true;
    }

    /**
     *
     */
    private void bodyRemove() {
        if (body != null) {
            DxGeom last = null;
            DxGeom g = body.geom;
            while (g != null) {
                if (g == this) {
                    if (last == null) {
                        body.geom = g.body_next;
                    } else {
                        last.body_next = g.body_next;
                    }
                    break;
                }
                last = g;
                g = g.body_next;
            }
            body = null;
            body_next = null;
        }
    }


    private void matrixInvert(final DMatrix3C inMat, DMatrix3 outMat) {
        outMat.set(inMat);
        //TZ unused: dMatrix3 dummy = new dMatrix3();
        double x;
        // swap _12 and _21
        x = outMat.get10();
        outMat.set10(outMat.get01());
        outMat.set01(x);
        // swap _31 and _13
        x = outMat.get02();
        outMat.set02(outMat.get20());
        outMat.set20(x);
        // swap _23 and _32
        x = outMat.get21();
        outMat.set21(outMat.get12());
        outMat.set12(x);
    }

    void getBodyPosr(final DxPosR offset_posr, final DxPosR final_posr, DxPosR body_posr) {
        DMatrix3 inv_offset = new DMatrix3();
        matrixInvert(offset_posr.Rw(), inv_offset);

        dMultiply0_333(body_posr.Rw(), final_posr.R(), inv_offset);
        DVector3 world_offset = new DVector3();  //TZ
        dMultiply0_331(world_offset, body_posr.R(), offset_posr.pos());
        body_posr.pos.eqDiff(final_posr.pos(), world_offset);
    }

    void getWorldOffsetPosr(final DxPosRC body_posr, final DxPosRC world_posr,
                            DxPosR offset_posr) {
        DMatrix3 inv_body = new DMatrix3();
        matrixInvert(body_posr.R(), inv_body);

        dMultiply0_333(offset_posr.Rw(), inv_body, world_posr.R());
        DVector3 world_offset = new DVector3();
        world_offset.eqDiff(world_posr.pos(), body_posr.pos());
        dMultiply0_331(offset_posr.pos, inv_body, world_offset);
    }

    /**
     * calculate our new final position from our offset and body.
     */
    void computePosr() {
        dMultiply0_331(_final_posr.pos, body.posr().R(), offset_posr.pos());
        _final_posr.pos.add(body.posr().pos());
        dMultiply0_333(_final_posr.Rw(), body.posr().R(), offset_posr.R());
    }

    boolean checkControlValueSizeValidity(Ref<?> dataValue, RefInt dataSize,
                                          int iRequiresSize) {
        // Here it is the intent to return true for 0 required size in any case
        //return (*dataSize == iRequiresSize && dataValue != 0) ? true : !(*dataSize = iRequiresSize);
        if (dataSize.get() == iRequiresSize && dataValue.get() != null) {
            return true;
        }
        dataSize.set(iRequiresSize);
        return iRequiresSize == 0;
    }

    // ************ TZ from collision_kernel.cpp ********************

    //****************************************************************************
    // misc

    /**
     * private functions that must be implemented by the collision library:
     * (1) indicate that a geom has moved (TZ: -> dGeomMoved),
     * (2) get the next geom in a body list.
     * these functions are called whenever the position of geoms connected to a
     * body have changed, e.g. with dBodySetPosition(), dBodySetRotation(), or
     * when the ODE step function updates the body state.
     */
    DxGeom dGeomGetBodyNext() {
        return body_next;
    }

    //****************************************************************************
    // public API for geometry objects
    public void dGeomDestroy() {
        DESTRUCTOR();
    }

    public void dGeomSetData(Object data) {
        _data = data;
    }

    public Object dGeomGetData() {
        return _data;
    }


    //	public void dGeomSetBody (dxGeom g, dxBody b)
    public void dGeomSetBody(DxBody b) {
        dUASSERT(b == null || (_gflags & GEOM_PLACEABLE) != 0,
                "geom must be placeable");
        DxSpace.CHECK_NOT_LOCKED(parent_space);

        if (b != null) {
            if (body == null) dFreePosr(_final_posr);
            if (body != b) {
                if (offset_posr != null) {
                    dFreePosr(offset_posr);
                    offset_posr = null;
                }
                _final_posr = b._posr;
                bodyRemove();
                bodyAdd(b);
            }
            dGeomMoved();
        } else {
            if (body != null) {
                if (offset_posr != null) {
                    // if we're offset, we already have our own final position, make sure its updated
                    recomputePosr();
                    dFreePosr(offset_posr);
                    offset_posr = null;
                } else {
                    _final_posr = new DxPosR();
                    _final_posr.pos.set(body.posr().pos());
                    _final_posr.Rw().set(body.posr().R());
                }
                bodyRemove();
            }
            // dGeomMoved() should not be called if the body is being set to 0, as the
            // new position of the geom is set to the old position of the body, so the
            // effective position of the geom remains unchanged.
        }
    }

    private DxBody dGeomGetBody() {
        return body;
    }

    private void dGeomSetPosition(DVector3C xyz) {
        dUASSERT(_gflags & GEOM_PLACEABLE, "geom must be placeable");
        DxSpace.CHECK_NOT_LOCKED(parent_space);
        if (offset_posr != null) {
            // move body such that body+offset = position
            DVector3 world_offset = new DVector3();
            dMultiply0_331(world_offset, body.posr().R(), offset_posr.pos());
            //TZ body could be null...?
            world_offset.eqDiff(xyz, world_offset);
            body.dBodySetPosition(world_offset);
        } else if (body != null) {
            // this will call dGeomMoved (g), so we don't have to
            body.dBodySetPosition(xyz);
        } else {
            _final_posr.pos.set(xyz);
            dGeomMoved();
        }
    }

    private void dGeomSetRotation(DMatrix3C R) {
        dUASSERT(_gflags & GEOM_PLACEABLE, "geom must be placeable");
        DxSpace.CHECK_NOT_LOCKED(parent_space);
        if (offset_posr != null) {
            recomputePosr();
            // move body such that body+offset = rotation
            DxPosR new_final_posr = new DxPosR();
            DxPosR new_body_posr = new DxPosR();
            new_final_posr.pos.set(_final_posr.pos());//, sizeof(dVector3));
            new_final_posr.Rw().set(R);//, sizeof(dMatrix3));
            getBodyPosr(offset_posr, new_final_posr, new_body_posr);
            body.dBodySetRotation(new_body_posr.R());
            body.dBodySetPosition(new_body_posr.pos());
        } else if (body != null) {
            // this will call dGeomMoved (g), so we don't have to
            body.dBodySetRotation(R);
        } else {
            _final_posr.Rw().set(R);
            dGeomMoved();
        }
    }


    private void dGeomSetQuaternion(DQuaternionC quat) {
        dUASSERT(_gflags & GEOM_PLACEABLE, "geom must be placeable");
        DxSpace.CHECK_NOT_LOCKED(parent_space);
        if (offset_posr != null) {
            recomputePosr();
            // move body such that body+offset = rotation
            DxPosR new_final_posr = new DxPosR();
            DxPosR new_body_posr = new DxPosR();
            dRfromQ(new_final_posr.Rw(), quat);
            new_final_posr.pos.set(_final_posr.pos());

            getBodyPosr(offset_posr, new_final_posr, new_body_posr);
            body.dBodySetRotation(new_body_posr.R());
            body.dBodySetPosition(new_body_posr.pos());
        }
        if (body != null) {
            // this will call dGeomMoved (g), so we don't have to
            body.dBodySetQuaternion(quat);
        } else {
            dRfromQ(_final_posr.Rw(), quat);
            dGeomMoved();
        }
    }

    DVector3C dGeomGetPosition() {
        dUASSERT(_gflags & GEOM_PLACEABLE, "geom must be placeable");
        recomputePosr();
        return _final_posr.pos();
    }

    private void dGeomCopyPosition(DVector3 pos) {
        pos.set(dGeomGetPosition());
    }

    DMatrix3C dGeomGetRotation() {
        dUASSERT(_gflags & GEOM_PLACEABLE, "geom must be placeable");
        recomputePosr();
        return _final_posr.R();
    }

    private void dGeomCopyRotation(DMatrix3 R) {
        R.set(dGeomGetRotation());
    }

    public void dGeomGetQuaternion(DQuaternion quat) {
        dUASSERT(_gflags & GEOM_PLACEABLE, "geom must be placeable");
        if (body != null && offset_posr == null) {
            quat.set(body.dBodyGetQuaternion());
        } else {
            recomputePosr();
            dQfromR(quat, _final_posr.R());
        }
    }

    @Override
    public DQuaternionC getQuaternion() {
        dUASSERT(_gflags & GEOM_PLACEABLE, "geom must be placeable");
        if (body != null && offset_posr == null) {
            return body.dBodyGetQuaternion();
        } else {
            recomputePosr();
            DQuaternion quat = new DQuaternion();
            dQfromR(quat, _final_posr.R());
            return quat;
        }
    }

    public void dGeomGetAABB(DAABB aabb) {
        recomputeAABB();
        aabb.set(_aabb);
    }

    @Override
    public DAABBC getAABB() {
        recomputeAABB();
        return _aabb;
    }

    private DxSpace dGeomGetSpace() {
        return parent_space;
    }

    private int dGeomGetClass() {
        return type;
    }

    private void dGeomSetCategoryBits(long bits) {
        DxSpace.CHECK_NOT_LOCKED(parent_space);
        category_bits = bits;
    }

    private void dGeomSetCollideBits(long bits) {
        DxSpace.CHECK_NOT_LOCKED(parent_space);
        collide_bits = bits;
    }

    private long dGeomGetCategoryBits() {
        return category_bits;
    }

    private long dGeomGetCollideBits() {
        return collide_bits;
    }

    private void dGeomEnable() {
        _gflags |= GEOM_ENABLED;
    }

    private void dGeomDisable() {
        _gflags &= ~GEOM_ENABLED;
    }

    private boolean dGeomIsEnabled() {
        return (_gflags & GEOM_ENABLED) != 0;// ? 1 : 0;
    }

    void dGeomGetRelPointPos(double px, double py, double pz,
                             DVector3 result) {
        if ((_gflags & GEOM_PLACEABLE) == 0) {
            result.set(px, py, pz);
            return;
        }
        recomputePosr();
        DVector3 prel, p = new DVector3();
        prel = new DVector3(px, py, pz);
        dMultiply0_331(p, _final_posr.R(), prel);
        result.eqSum(p, _final_posr.pos());
    }


    void dGeomGetPosRelPoint(double px, double py, double pz, DVector3 result) {
        if ((_gflags & GEOM_PLACEABLE) == 0) {
            result.set(px, py, pz);
            return;
        }

        recomputePosr();

        DVector3 prel = new DVector3(px, py, pz);
        prel.sub(_final_posr.pos());
        dMultiply1_331(result, _final_posr.R(), prel);
    }


    void dGeomVectorToWorld(double px, double py, double pz, DVector3 result) {
        if ((_gflags & GEOM_PLACEABLE) == 0) {
            result.set(px, py, pz);
            return;
        }

        recomputePosr();

        DVector3 p = new DVector3(px, py, pz);
        dMultiply0_331(result, _final_posr.R(), p);
    }


    void dGeomVectorFromWorld(double px, double py, double pz, DVector3 result) {
        if ((_gflags & GEOM_PLACEABLE) == 0) {
            result.set(px, py, pz);
            return;
        }

        recomputePosr();

        DVector3 p = new DVector3(px, py, pz);
        dMultiply1_331(result, _final_posr.R(), p);
    }

    /*extern */
    static void dFinitUserClasses() {
        //TODO num_user_classes = 0;
    }

    private void dGeomCreateOffset() {
        dUASSERT(_gflags & GEOM_PLACEABLE, "geom must be placeable");
        dUASSERT(body, "geom must be on a body");
        if (offset_posr != null) {
            return; // already created
        }
        dIASSERT(_final_posr == body._posr);

        _final_posr = dAllocPosr();
        offset_posr = dAllocPosr();
        offset_posr.pos.setZero();
        offset_posr.Rw().setIdentity();

        _gflags |= GEOM_POSR_BAD;
    }

    public void dGeomSetOffsetPosition(double x, double y, double z) {
        //dAASSERT (g);
        dUASSERT(_gflags & GEOM_PLACEABLE, "geom must be placeable");
        dUASSERT(body, "geom must be on a body");
        DxSpace.CHECK_NOT_LOCKED(parent_space);
        if (offset_posr == null) {
            dGeomCreateOffset();
        }
        offset_posr.pos.set(x, y, z);
        dGeomMoved();
    }

    public void dGeomSetOffsetRotation(final DMatrix3C R) {
        dUASSERT(_gflags & GEOM_PLACEABLE, "geom must be placeable");
        dUASSERT(body, "geom must be on a body");
        DxSpace.CHECK_NOT_LOCKED(parent_space);
        if (offset_posr == null) {
            dGeomCreateOffset();
        }
        offset_posr.Rw().set(R);
        dGeomMoved();
    }

    void dGeomSetOffsetQuaternion(DQuaternionC quat) {
        dUASSERT(_gflags & GEOM_PLACEABLE, "geom must be placeable");
        dUASSERT(body, "geom must be on a body");
        DxSpace.CHECK_NOT_LOCKED(parent_space);
        if (offset_posr == null) {
            dGeomCreateOffset();
        }
        dRfromQ(offset_posr.Rw(), quat);
        dGeomMoved();
    }

    void dGeomSetOffsetWorldPosition(double x, double y, double z) {
        dUASSERT(_gflags & GEOM_PLACEABLE, "geom must be placeable");
        dUASSERT(body, "geom must be on a body");
        DxSpace.CHECK_NOT_LOCKED(parent_space);
        if (offset_posr == null) {
            dGeomCreateOffset();
        }
        body.dBodyGetPosRelPoint(new DVector3(x, y, z), offset_posr.pos);
        dGeomMoved();
    }

    void dGeomSetOffsetWorldRotation(DMatrix3C R) {
        dUASSERT(_gflags & GEOM_PLACEABLE, "geom must be placeable");
        dUASSERT(body, "geom must be on a body");
        DxSpace.CHECK_NOT_LOCKED(parent_space);
        if (offset_posr == null) {
            dGeomCreateOffset();
        }
        recomputePosr();
        DxPosR new_final_posr = new DxPosR();
        new_final_posr.pos.set(_final_posr.pos());
        new_final_posr.Rw().set(R);
        getWorldOffsetPosr(body.posr(), new_final_posr, offset_posr);
        dGeomMoved();
    }

    void dGeomSetOffsetWorldQuaternion(DQuaternionC quat) {
        dUASSERT(_gflags & GEOM_PLACEABLE, "geom must be placeable");
        dUASSERT(body, "geom must be on a body");
        DxSpace.CHECK_NOT_LOCKED(parent_space);
        if (offset_posr == null) {
            dGeomCreateOffset();
        }

        recomputePosr();

        DxPosR new_final_posr = new DxPosR();
        new_final_posr.pos.set(_final_posr.pos());
        dRfromQ(new_final_posr.Rw(), quat);

        getWorldOffsetPosr(body.posr(), new_final_posr, offset_posr);
        dGeomMoved();
    }

    void dGeomClearOffset() {
        dUASSERT(_gflags & GEOM_PLACEABLE, "geom must be placeable");
        if (offset_posr != null) {
            // no longer need an offset posr
            dFreePosr(offset_posr);
            offset_posr = null;
            // the geom will now share the position of the body
            dFreePosr(_final_posr);
            _final_posr = body._posr;
            // geom has moved
            _gflags &= ~GEOM_POSR_BAD;
            dGeomMoved();
        }
    }

    boolean dGeomIsOffset() {
        return null != offset_posr;
    }

    private static final DVector3C OFFSET_POSITION_ZERO = new DVector3(0.0f, 0.0f, 0.0f);

    //	final double * dGeomGetOffsetPosition (dxGeom g)
    private DVector3C dGeomGetOffsetPosition() {
        if (offset_posr != null) {
            return offset_posr.pos();
        }
        return OFFSET_POSITION_ZERO;
    }

    private void dGeomCopyOffsetPosition(DVector3 pos) {
        pos.set(dGeomGetOffsetPosition());
    }

    private static final DMatrix3C OFFSET_ROTATION_ZERO = new DMatrix3(
            1.0, 0.0, 0.0,
            0.0, 1.0, 0.0,
            0.0, 0.0, 1.0);

    //double *
    private DMatrix3C dGeomGetOffsetRotation() {
        if (offset_posr != null) {
            return offset_posr.R();
        }
        return OFFSET_ROTATION_ZERO;
    }

    private void dGeomCopyOffsetRotation(DMatrix3 R) {
        R.set(dGeomGetOffsetRotation());
    }

    void dGeomGetOffsetQuaternion(DQuaternion result) {
        if (offset_posr != null) {
            dQfromR(result, offset_posr.R());
        } else {
            result.set(1, 0, 0, 0);
        }
    }


    private DxPosR dAllocPosr() {
        return new DxPosR();
    }

    /**
     * Frees memory ?
     *
     * @deprecated
     */
    @Deprecated
    private void dFreePosr(DxPosR oldPosR) {
    }

    /**
     * private functions that must be implemented by the collision library:
     * (1) indicate that a geom has moved, (2) get the next geom in a body list
     * (TZ: -> dGeomGetBodyNext() ).
     * these functions are called whenever the position of geoms connected to a
     * body have changed, e.g. with dBodySetPosition(), dBodySetRotation(), or
     * when the ODE step function updates the body state.
     * <p>
     * make the geom dirty by setting the GEOM_DIRTY and GEOM_BAD_AABB flags
     * and moving it to the front of the space's list. all the parents of a
     * dirty geom also become dirty.
     */
    void dGeomMoved() {
        // if geom is offset, mark it as needing a calculate
        if (offset_posr != null) {
            _gflags |= GEOM_POSR_BAD;
        }

        // from the bottom of the space heirarchy up, process all clean geoms
        // turning them into dirty geoms.
        DxSpace parent = parent_space;

        DxGeom geom = this;
        while (parent != null && (geom._gflags & GEOM_DIRTY) == 0) {
            parent.CHECK_NOT_LOCKED();
            geom._gflags |= GEOM_DIRTY | GEOM_AABB_BAD;
            parent.dirty(geom);
            geom = parent;
            parent = parent.parent_space;
        }

        // all the remaining dirty geoms must have their AABB_BAD flags set, to
        // ensure that their AABBs get recomputed
        while (geom != null) {
            geom._gflags |= GEOM_DIRTY | GEOM_AABB_BAD;
            DxSpace.CHECK_NOT_LOCKED(geom.parent_space);
            geom = geom.parent_space;
        }
    }

    protected boolean GEOM_ENABLED(DxGeom g) {
        return ((g._gflags & GEOM_ENABLE_TEST_MASK) == GEOM_ENABLE_TEST_VALUE);
    }

    private static class dColliderEntry {
        DColliderFn fn;    // collider function, 0 = no function available
        boolean reverse;        // 1 = reverse o1 and o2
    }

    private static final dColliderEntry[][] colliders = new dColliderEntry[dGeomNumClasses][dGeomNumClasses];
    private static boolean colliders_initialized = false;

    private static final boolean LIBCCD = OdeConfig.isLibCCDEndabled();
    private static final boolean dLIBCCD_BOX_CYL = LIBCCD;

    private static final boolean dLIBCCD_CYL_CYL = LIBCCD;

    private static final boolean dLIBCCD_CAP_CYL = LIBCCD;

    private static final boolean dLIBCCD_CONVEX_BOX = LIBCCD;

    private static final boolean dLIBCCD_CONVEX_CAP = LIBCCD;

    private static final boolean dLIBCCD_CONVEX_CYL = LIBCCD;

    private static final boolean dLIBCCD_CONVEX_SPHERE = LIBCCD;

    private static final boolean dLIBCCD_CONVEX_CONVEX = LIBCCD;

    private static void setCollider(int i, int j, DColliderFn fn) {
        //TZ
        if (colliders[i][j] == null) colliders[i][j] = new dColliderEntry();
        if (colliders[j][i] == null) colliders[j][i] = new dColliderEntry();
        if (colliders[i][j].fn == null) {
            colliders[i][j].fn = fn;
            colliders[i][j].reverse = false;
        }
        if (colliders[j][i].fn == null) {
            colliders[j][i].fn = fn;
            colliders[j][i].reverse = true;
        }
    }

    //TODO put into a different class?
    /*extern */
    public static void dInitColliders() {
        dIASSERT(!colliders_initialized);
        colliders_initialized = true;

        for (int i = 0; i < colliders.length; i++) {
            //TZ
            colliders[i] = new dColliderEntry[dGeomNumClasses];
            for (int j = 0; j < colliders[i].length; j++) {
                colliders[i][j] = new dColliderEntry();
            }
        }

        // setup space colliders
        for (int i = dFirstSpaceClass; i <= dLastSpaceClass; i++) {
            for (int j = 0; j < dGeomNumClasses; j++) {
                setCollider(i, j, new CollideSpaceGeom());
            }
        }

        setCollider(dSphereClass, dSphereClass, new DxSphere.CollideSphereSphere());
        setCollider(dSphereClass, dBoxClass, new DxSphere.CollideSphereBox());
        setCollider(dSphereClass, dPlaneClass, new DxSphere.CollideSpherePlane());
        setCollider(dBoxClass, dBoxClass, new CollideBoxBox());//dCollideBoxBox);
        setCollider(dBoxClass, dPlaneClass, new CollideBoxPlane());//dCollideBoxPlane);
        setCollider(dCapsuleClass, dSphereClass, new DxCapsule.CollideCapsuleSphere());//dCollideCapsuleSphere);
        setCollider(dCapsuleClass, dBoxClass, new DxCapsule.CollideCapsuleBox());//dCollideCapsuleBox);
        setCollider(dCapsuleClass, dCapsuleClass, new DxCapsule.CollideCapsuleCapsule());//dCollideCapsuleCapsule);
        setCollider(dCapsuleClass, dPlaneClass, new DxCapsule.CollideCapsulePlane());//dCollideCapsulePlane);
        setCollider(dRayClass, dSphereClass, new DxRay.CollideRaySphere());//dCollideRaySphere);
        setCollider(dRayClass, dBoxClass, new DxRay.CollideRayBox());//dCollideRayBox);
        setCollider(dRayClass, dCapsuleClass, new DxRay.CollideRayCapsule());//dCollideRayCapsule);
        setCollider(dRayClass, dPlaneClass, new DxRay.CollideRayPlane());//dCollideRayPlane);
        setCollider(dRayClass, dCylinderClass, new DxRay.CollideRayCylinder());//dCollideRayCylinder);

        setCollider(dTriMeshClass, dSphereClass, new CollideTrimeshSphere());//.  dCollideSTL);
        setCollider(dTriMeshClass, dBoxClass, new CollideTrimeshBox());// dCollideBTL);
        setCollider(dTriMeshClass, dRayClass, new CollideTrimeshRay());// dCollideRTL);
        setCollider(dTriMeshClass, dTriMeshClass, new CollideTrimeshTrimesh());// dCollideTTL);
        setCollider(dTriMeshClass, dCapsuleClass, new CollideTrimeshCCylinder());// dCollideCCTL);
        setCollider(dTriMeshClass, dPlaneClass, new CollideTrimeshPlane());// dCollideTrimeshPlane);
        setCollider(dCylinderClass, dTriMeshClass, new CollideCylinderTrimesh());// dCollideCylinderTrimesh);
        setCollider(dConvexClass, dTriMeshClass, new CollideConvexTrimesh());

        if (dLIBCCD_BOX_CYL) {
            setCollider(dBoxClass, dCylinderClass, new CollideBoxCylinderCCD());
        } else {
            setCollider(dCylinderClass, dBoxClass, new CollideCylinderBox());//dCollideCylinderBox);
        }
        setCollider(dCylinderClass, dSphereClass, new CollideCylinderSphere());//dCollideCylinderSphere);
        setCollider(dCylinderClass, dPlaneClass, new CollideCylinderPlane());//dCollideCylinderPlane);

        if (dLIBCCD_CYL_CYL) {
            setCollider(dCylinderClass, dCylinderClass, new CollideCylinderCylinder());
        }
        if (dLIBCCD_CAP_CYL) {
            setCollider(dCapsuleClass, dCylinderClass, new CollideCapsuleCylinder());
        }

        //--> Convex Collision
        if (dLIBCCD_CONVEX_BOX) {
            setCollider(dConvexClass, dBoxClass, new CollideConvexBoxCCD());
        } else {
            setCollider(dConvexClass, dBoxClass, new DxConvex.CollideConvexBox());
        }

        if (dLIBCCD_CONVEX_CAP) {
            setCollider(dConvexClass, dCapsuleClass, new CollideConvexCapsuleCCD());
        } else {
            setCollider(dConvexClass, dCapsuleClass, new DxConvex.CollideConvexCapsule());
        }

        if (dLIBCCD_CONVEX_CYL) {
            setCollider(dConvexClass, dCylinderClass, new CollideConvexCylinderCCD());
        }

        if (dLIBCCD_CONVEX_SPHERE) {
            setCollider(dConvexClass, dSphereClass, new CollideConvexSphereCCD());
        } else {
            setCollider(dSphereClass, dConvexClass, new DxConvex.CollideSphereConvex());
        }

        if (dLIBCCD_CONVEX_CONVEX) {
            setCollider(dConvexClass, dConvexClass, new CollideConvexConvexCCD());
        } else {
            setCollider(dConvexClass, dConvexClass, new DxConvex.CollideConvexConvex());
        }

        setCollider(dConvexClass, dPlaneClass, new DxConvex.CollideConvexPlane());
        setCollider(dRayClass, dConvexClass, new DxConvex.CollideRayConvex());
        setCollider(dHeightfieldClass, dRayClass, new DxHeightfield.CollideHeightfield());
        setCollider(dHeightfieldClass, dSphereClass, new DxHeightfield.CollideHeightfield());
        setCollider(dHeightfieldClass, dBoxClass, new DxHeightfield.CollideHeightfield());
        setCollider(dHeightfieldClass, dCapsuleClass, new DxHeightfield.CollideHeightfield());
        setCollider(dHeightfieldClass, dCylinderClass, new DxHeightfield.CollideHeightfield());
        setCollider(dHeightfieldClass, dConvexClass, new DxHeightfield.CollideHeightfield());
        setCollider(dHeightfieldClass, dTriMeshClass, new DxHeightfield.CollideHeightfield());
    }

    /*extern */
    static void dFinitColliders() {
        colliders_initialized = false;
    }

    public static void dSetColliderOverride(int i, int j, DColliderFn fn) {
        dIASSERT(colliders_initialized);
        dAASSERT(i < dGeomNumClasses);
        dAASSERT(j < dGeomNumClasses);

        colliders[i][j].fn = fn;
        colliders[i][j].reverse = false;
        colliders[j][i].fn = fn;
        colliders[j][i].reverse = true;
    }

    /**
     * NOTE!
     * If it is necessary to add special processing mode without contact generation
     * use NULL contact parameter value as indicator, not zero in flags.
     */
    public static int dCollide(DxGeom o1, DxGeom o2, int flags,
                               DContactGeomBuffer contacts, int skip) {
        dAASSERT(contacts);
        dUASSERT(colliders_initialized,
                "Please call ODE initialization (dInitODE() or similar) before using the library");
        dUASSERT(o1.type >= 0 && o1.type < dGeomNumClasses, "bad o1 class number");
        dUASSERT(o2.type >= 0 && o2.type < dGeomNumClasses, "bad o2 class number");
        // Even though comparison for greater or equal to one is used in all the
        // other places, here it is more logical to check for greater than zero
        // because function does not require any specific number of contact slots -
        // it must be just a positive.
        dUASSERT((flags & NUMC_MASK) > 0, "no contacts requested");

        // Extra precaution for zero contact count in parameters
        if ((flags & NUMC_MASK) == 0) return 0;
        // no contacts if both geoms are the same
        if (o1 == o2) return 0;

        // no contacts if both geoms on the same body, and the body is not 0
        if (o1.body == o2.body && o1.body != null) return 0;

        o1.recomputePosr();
        o2.recomputePosr();

        dColliderEntry ce = colliders[o1.type][o2.type];
        int count = 0;
        if (ce.fn != null) {
            if (ce.reverse) {
                count = ce.fn.dColliderFn(o2, o1, flags, contacts);
                for (int i = 0; i < count; i++) {
                    DContactGeom c = contacts.get(i);
                    c.normal.scale(-1);
                    DGeom tmp = c.g1;
                    c.g1 = c.g2;
                    c.g2 = tmp;
                    int tmpint = c.side1;
                    c.side1 = c.side2;
                    c.side2 = tmpint;
                }
            } else {
                count = ce.fn.dColliderFn(o1, o2, flags, contacts);
            }
        }

        return count;
    }


    // **************** from collision_space_internal.h TZ
    // collide two geoms together. for the hash table space, this is
    // called if the two AABBs inhabit the same hash table cells.
    // this only calls the callback function if the AABBs actually
    // intersect. if a geom has an AABB test function, that is called to
    // provide a further refinement of the intersection.
    //
    // NOTE: this assumes that the geom AABBs are valid on entry
    // and that both geoms are enabled.
    static void collideAABBs(DxGeom g1, DxGeom g2,
                             Object data, DNearCallback callback) {
        dIASSERT((g1._gflags & GEOM_AABB_BAD) == 0);
        dIASSERT((g2._gflags & GEOM_AABB_BAD) == 0);

        // no contacts if both geoms on the same body, and the body is not 0
        if (g1.body == g2.body && g1.body != null) return;

        // test if the category and collide bitfields match
        if (!((g1.category_bits & g2.collide_bits) != 0 ||
                (g2.category_bits & g1.collide_bits) != 0)) {
            return;
        }

        // if the bounding boxes are disjoint then don't do anything
        DAABB bounds1 = g1._aabb;
        DAABB bounds2 = g2._aabb;
        if (bounds1.isDisjoint(bounds2)) {
            return;
        }
        // check if either object is able to prove that it doesn't intersect the
        // AABB of the other
        if (!g1.AABBTest(g2, bounds2)) return;
        if (!g2.AABBTest(g1, bounds1)) return;
        // the objects might actually intersect - call the space callback function
        callback.call(data, g1, g2);
    }

    @Override
    public String toString() {
        return super.toString() + " body=" + body;
    }

    void setNextPrevNull() {
        _next = null;
        _prev = null;
    }

    DxPosRC final_posr() {
        return _final_posr;
    }

    DxPosRC offset_posr() {
        return offset_posr;
    }

    // *********************************************
    // dGeom API
    // *********************************************

    @Override
    public void destroy() {
        dGeomDestroy();
    }

    @Override
    public int getClassID()// const
    {
        return dGeomGetClass();
    }

    @Override
    public DSpace getSpace() //const
    {
        return dGeomGetSpace();
    }

    @Override
    public void setData(Object data) {
        dGeomSetData(data);
    }

    @Override
    public Object getData() //const
    {
        return dGeomGetData();
    }

    @Override
    public void setBody(DBody b) {
        dGeomSetBody((DxBody) b);
    }

    @Override
    public DBody getBody() //const
    {
        return dGeomGetBody();
    }

    @Override
    public void setPosition(double x, double y, double z) {
        dGeomSetPosition(new DVector3(x, y, z));
    }

    @Override
    public void setPosition(DVector3C xyz) {
        dGeomSetPosition(xyz);
    }

    @Override
    public DVector3C getPosition() {
        return dGeomGetPosition();
    }

    @Override
    public void setRotation(DMatrix3C R) {
        dGeomSetRotation(R);
    }

    @Override
    public DMatrix3C getRotation() {
        return dGeomGetRotation();
    }

    @Override
    public void setQuaternion(DQuaternionC quat) {
        dGeomSetQuaternion(quat);
    }

    @Override
    public void setCategoryBits(long bits)
    {
        dGeomSetCategoryBits(bits);
    }

    @Override
    public void setCollideBits(long bits)
    {
        dGeomSetCollideBits(bits);
    }

    //unsigned
    @Override
    public long getCategoryBits() {
        return dGeomGetCategoryBits();
    }

    //unsigned
    @Override
    public long getCollideBits() {
        return dGeomGetCollideBits();
    }

    @Override
    public void enable() {
        dGeomEnable();
    }

    @Override
    public void disable() {
        dGeomDisable();
    }

    @Override
    public boolean isEnabled() {
        return dGeomIsEnabled();
    }

    @Override
    public void collide2(DGeom g, Object data, DNearCallback callback) {
        DxSpace.dSpaceCollide2(this, (DxGeom) g, data, callback);
    }

    @Override
    public void setOffsetPosition(double x, double y, double z) {
        dGeomSetOffsetPosition(x, y, z);
    }

    @Override
    public void setOffsetPosition(DVector3C xyz) {
        dGeomSetOffsetPosition(xyz.get0(), xyz.get1(), xyz.get2());
    }

    @Override
    public void setOffsetRotation(DMatrix3C R) {
        dGeomSetOffsetRotation(R);
    }


    @Override
    public void clearOffset() {
        dGeomClearOffset();
    }


    @Override
    public DVector3C getOffsetPosition() {
        return dGeomGetOffsetPosition();
    }


    @Override
    public void getOffsetQuaternion(DQuaternion result) {
        dGeomGetOffsetQuaternion(result);
    }


    @Override
    public DMatrix3C getOffsetRotation() {
        return dGeomGetOffsetRotation();
    }


    @Override
    public boolean isOffset() {
        return dGeomIsOffset();
    }


    @Override
    public void setOffsetQuaternion(DQuaternionC q) {
        dGeomSetOffsetQuaternion(q);
    }


    @Override
    public void setOffsetWorldPosition(double x, double y, double z) {
        dGeomSetOffsetWorldPosition(x, y, z);
    }


    @Override
    public void setOffsetWorldQuaternion(DQuaternionC q) {
        dGeomSetOffsetWorldQuaternion(q);
    }


    @Override
    public void setOffsetWorldRotation(DMatrix3C R) {
        dGeomSetOffsetRotation(R);
    }

    @Override
    public void copyOffsetPosition(DVector3 pos) {
        dGeomCopyOffsetPosition(pos);
    }

    @Override
    public void copyOffsetRotation(DMatrix3 R) {
        dGeomCopyOffsetRotation(R);
    }

    @Override
    public void copyPosition(DVector3 pos) {
        dGeomCopyPosition(pos);
    }

    @Override
    public void copyRotation(DMatrix3 R) {
        dGeomCopyRotation(R);
    }

    @Override
    public void getRelPointPos(double px, double py, double pz, DVector3 result) {
        dGeomGetRelPointPos(px, py, pz, result);
    }

    @Override
    public void getPosRelPoint(double px, double py, double pz, DVector3 result) {
        dGeomGetPosRelPoint(px, py, pz, result);
    }

    @Override
    public void vectorToWorld(double px, double py, double pz, DVector3 result) {
        dGeomVectorToWorld(px, py, pz, result);
    }

    @Override
    public void vectorFromWorld(double px, double py, double pz, DVector3 result) {
        dGeomVectorFromWorld(px, py, pz, result);
    }

}
