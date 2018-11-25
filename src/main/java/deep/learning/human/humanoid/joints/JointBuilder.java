package deep.learning.human.humanoid.joints;

import org.ode4j.math.DVector3;
import org.ode4j.ode.DFixedJoint;
import org.ode4j.ode.DHinge2Joint;
import org.ode4j.ode.DHingeJoint;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DUniversalJoint;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;
import org.ode4j.ode.internal.joints.DxJointConstrainedBall;

import java.util.Map;
import java.util.Optional;

import deep.learning.human.HumanBone;
import deep.learning.human.utils.config.BoneConfig;
import deep.learning.human.utils.config.JointConfig;

public class JointBuilder {

    private DWorld world;

    private Map<String, BoneConfig> boneConfig;

    private double coefficient = Math.PI;

    public JointBuilder(DWorld world, Map<String, BoneConfig> boneConfig) {
        this.world = world;
        this.boneConfig = boneConfig;
    }

    public Joint buildJointFromConfig(JointConfig config, Map<String, HumanBone> bones) {
        switch (config.getType()) {
            case CONSTRAINED_BALL:
                return getConstrainedBallJoint(config, bones);
            case FIXED:
                return new FixedJoint(config.getName(),
                        bones.get(config.getParent()),
                        bones.get(config.getChild()),
                        buildFixedJoint(
                                bones.get(config.getParent()),
                                bones.get(config.getChild())
                        ));
            case HINGE:
                return getHingeJoint(config, bones);
            case UNIVERSAL:
                return getUniversalJoint(config, bones);
        }
        throw new UnsupportedOperationException();
    }

    private Joint getConstrainedBallJoint(JointConfig config, Map<String, HumanBone> bones) {
        DVector3 anchor = Optional.ofNullable(config.getAnchor()).orElse(boneConfig.get(config.getParent()).getP2());
        return new BallJoint(config.getName(),
                bones.get(config.getParent()),
                bones.get(config.getChild()),
                buildConstrainedBallJoint(
                        bones.get(config.getParent()),
                        bones.get(config.getChild()),
                        anchor,
                        config.getFlexAxis(),
                        config.getTwistAxis(),
                        coefficient * config.getFlexLimit(),
                        coefficient * config.getTwistLimit()
                ));
    }

    private Joint getUniversalJoint(JointConfig config, Map<String, HumanBone> bones) {
        DVector3 anchor = Optional.ofNullable(config.getAnchor()).orElse(boneConfig.get(config.getParent()).getP2());
        return new UniversalJoint(config.getName(),
                bones.get(config.getParent()),
                bones.get(config.getChild()),
                buildUniversalJoint(
                        bones.get(config.getParent()),
                        bones.get(config.getChild()),
                        anchor,
                        config.getAxis1(),
                        config.getAxis2(),
                        coefficient * config.getLoStop1(),
                        coefficient * config.getHiStop1(),
                        coefficient * config.getLoStop2(),
                        coefficient * config.getHiStop2()
                ));
    }

    private Joint getHingeJoint(JointConfig config, Map<String, HumanBone> bones) {
        DVector3 anchor = Optional.ofNullable(config.getAnchor()).orElse(boneConfig.get(config.getParent()).getP2());
        return new HingeJoint(config.getName(),
                bones.get(config.getParent()),
                bones.get(config.getChild()),
                buildHingeJoint(
                        bones.get(config.getParent()),
                        bones.get(config.getChild()),
                        anchor,
                        config.getAxis(),
                        coefficient * config.getLoStop1(),
                        coefficient * config.getHiStop1()
                ));
    }

    private DJoint buildFixedJoint(HumanBone body1,
                                  HumanBone body2) {
        DFixedJoint joint = OdeHelper.createFixedJoint(world);
        joint.attach(body1.getBody(), body2.getBody());
        joint.setFixed();
        return joint;
    }

    private DJoint buildHingeJoint(HumanBone body1,
                                  HumanBone body2,
                                  DVector3 anchor,
                                  DVector3 axis,
                                  double loStop1,
                                  double hiStop1) {
        DHingeJoint joint = OdeHelper.createHingeJoint(world);
        joint.attach(body1.getBody(), body2.getBody());
        joint.setAnchor(anchor);
        joint.setAxis(axis);
        joint.setParam(DJoint.PARAM_N.dParamLoStop1, loStop1);
        joint.setParam(DJoint.PARAM_N.dParamHiStop1, hiStop1);
        return joint;
    }

    private DJoint buildHinge2Joint(HumanBone body1,
                                   HumanBone body2,
                                   DVector3 anchor,
                                   DVector3 axis1,
                                   DVector3 axis2,
                                   double loStop1,
                                   double hiStop1) {
        DHinge2Joint joint = OdeHelper.createHinge2Joint(world);
        joint.attach(body1.getBody(), body2.getBody());
        joint.setAnchor(anchor);
        joint.setAxis1(axis1);
        joint.setAxis1(axis2);
        joint.setParam(DJoint.PARAM_N.dParamLoStop1, loStop1);
        joint.setParam(DJoint.PARAM_N.dParamHiStop1, hiStop1);
        return joint;
    }

    private DJoint buildUniversalJoint(HumanBone body1,
                                      HumanBone body2,
                                      DVector3 anchor,
                                      DVector3 axis1,
                                      DVector3 axis2,
                                      double loStop1,
                                      double hiStop1,
                                      double loStop2,
                                      double hiStop2) {
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

    private DJoint buildConstrainedBallJoint(HumanBone body1,
                                            HumanBone body2,
                                            DVector3 anchor,
                                            DVector3 flexAxis,
                                            DVector3 twistAxis,
                                            double flexLimit,
                                            double twistLimit) {
        DxJointConstrainedBall joint = new DxJointConstrainedBall(world);
        joint.attach(body1.getBody(), body2.getBody());
        joint.setAnchor(anchor);
        joint.setAxes(flexAxis, twistAxis);
        joint.setLimits(flexLimit, twistLimit);
        return joint;
    }

    public double getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(double coefficient) {
        this.coefficient = coefficient;
    }
}
