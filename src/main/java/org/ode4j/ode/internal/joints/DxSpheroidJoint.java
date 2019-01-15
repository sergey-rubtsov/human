package org.ode4j.ode.internal.joints;

import org.ode4j.math.DQuaternion;
import org.ode4j.math.DVector3;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.internal.DxWorld;

/**
 * The spheroid joint provides motion around an indefinite number of axes,
 * which have one common center. It enables the bone to move in many places
 * (nearly all directions).
 * This joint is better fit for hips and shoulders than DxJointConstrainedBall
 * because it has more constraint options.
 *
 * @author Sergey Rubtsov
 */
public class DxSpheroidJoint extends DxJoint implements DJoint {

    private DVector3 anchor1;               // anchor w.r.t first body
    private DVector3 anchor2;               // anchor w.r.t second body
    private DVector3 axis1;                 // axis w.r.t first body
    private DVector3 axis2;                 // axis w.r.t second body
    private DQuaternion qrel1;              // initial relative rotation body1 -> virtual cross piece
    private DQuaternion qrel2;              // initial relative rotation virtual cross piece -> body2
    private DxJointLimitMotor limot1;       // limit and motor information for axis1
    private DxJointLimitMotor limot2;       // limit and motor information for axis2

    private DVector3 twistUpAxis1;
    private DVector3 twistUpAxis2;
    private DxJointLimitMotor limotFlex;
    private DxJointLimitMotor limotTwist;

    protected DxSpheroidJoint(DxWorld w) {
        super(w);
        anchor1 = new DVector3();
        anchor2 = new DVector3();
        axis1 = new DVector3(1, 0, 0);
        axis2 = new DVector3(0, 1, 0);
        qrel1 = new DQuaternion();
        qrel2 = new DQuaternion();
        twistUpAxis1 = new DVector3();
        twistUpAxis2 = new DVector3();
        limotFlex = new DxJointLimitMotor();
        limotFlex.init(w);
        limotTwist = new DxJointLimitMotor();
        limotTwist.init(w);
        limot1 = new DxJointLimitMotor();
        limot1.init(w);
        limot2 = new DxJointLimitMotor();
        limot2.init(w);
    }

    @Override
    public void getInfo1(Info1 info) {

    }

    @Override
    public void getInfo2(double worldFPS, double worldERP, Info2Descr info) {
        // set the three ball-and-socket rows
        setBall(this, worldFPS, worldERP, info, anchor1, anchor2);




    }

    @Override
    void getSureMaxInfo(SureMaxInfo info) {

    }

    @Override
    public void setParam(PARAM_N parameter, double value) {

    }

    @Override
    public double getParam(PARAM_N parameter) {
        return 0;
    }

    public void setFlexLimits(double loStop, double hiStop) {
        limotFlex.set(PARAM.dParamLoStop, loStop);
        limotFlex.set(PARAM.dParamHiStop, hiStop);
    }

    public void setTwistLimits(double loStop, double hiStop) {
        limotTwist.set(PARAM.dParamLoStop, loStop);
        limotTwist.set(PARAM.dParamHiStop, hiStop);
    }




}
