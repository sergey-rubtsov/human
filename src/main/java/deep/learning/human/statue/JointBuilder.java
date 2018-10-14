package deep.learning.human.statue;

import deep.learning.human.HumanBone;
import deep.learning.human.HumanJoint;
import deep.learning.human.JointType;
import org.ode4j.math.DVector3;
import org.ode4j.ode.DFixedJoint;
import org.ode4j.ode.DHingeJoint;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DUniversalJoint;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.internal.joints.DxJointConstrainedBall;

public class JointBuilder {

    public static HumanJoint buildJoint(HumanBone parent, HumanBone child, DWorld world) {
        DJoint joint = JointBuilder.buildFixedJoint(parent, child, world);
        return new JointImpl(parent.getName() + child.getName(),
                JointType.FIXED,
                parent,
                child,
                joint);
    }

    public static DJoint buildFixedJoint(HumanBone body1, HumanBone body2, DWorld world) {
        DFixedJoint joint = OdeHelper.createFixedJoint(world);
        joint.attach(body1.getBody(), body2.getBody());
        joint.setFixed();
        return joint;
    }

    public static DJoint buildHingeJoint(HumanBone body1,
                                 HumanBone body2,
                                 DVector3 anchor,
                                 DVector3 axis,
                                 double loStop,
                                 double hiStop,
                                 DWorld world) {
        DHingeJoint joint = OdeHelper.createHingeJoint(world);
        joint.attach(body1.getBody(), body2.getBody());
        joint.setAnchor(anchor);
        joint.setAxis(axis);
        joint.setParam(DJoint.PARAM_N.dParamLoStop1, loStop);
        joint.setParam(DJoint.PARAM_N.dParamHiStop1, hiStop);
        return joint;
    }

    public static DJoint buildUniversalJoint(HumanBone body1,
                                     HumanBone body2,
                                     DVector3 anchor,
                                     DVector3 axis1,
                                     DVector3 axis2,
                                     double loStop1,
                                     double hiStop1,
                                     double loStop2,
                                     double hiStop2,
                                     DWorld world) {
        DUniversalJoint joint = OdeHelper.createUniversalJoint(world);
        joint.attach(body1.getBody(), body2.getBody());
        joint.setAnchor(anchor);
        joint.setAxis1(axis1);
        joint.setAxis2(axis2);
        joint.setParam(DJoint.PARAM_N.dParamLoStop1, loStop1);
        joint.setParam(DJoint.PARAM_N.dParamHiStop1, hiStop1);
        joint.setParam(DJoint.PARAM_N.dParamLoStop2, loStop2);
        joint.setParam(DJoint.PARAM_N.dParamHiStop2, hiStop2);
        return joint;
    }

    public static DJoint buildConstrainedBallJoint(HumanBone body1,
                                           HumanBone body2,
                                           DVector3 anchor,
                                           DVector3 flexAxis,
                                           DVector3 twistAxis,
                                           double flexLimit,
                                           double twistLimit,
                                           DWorld world) {
        DxJointConstrainedBall joint = new DxJointConstrainedBall(world);
        joint.attach(body1.getBody(), body2.getBody());
        joint.setAnchor(anchor);
        joint.setAxes(flexAxis, twistAxis);
        joint.setLimits(flexLimit, twistLimit);
        return joint;
    }

}
