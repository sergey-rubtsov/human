package deep.learning.human.humanoid.joints;

import deep.learning.human.HumanBone;
import deep.learning.human.JointType;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DJoint;

public class BallJoint extends Joint {

    public BallJoint(String name, HumanBone parent, HumanBone child, DJoint joint) {
        super(name, parent, child, joint);
    }

    @Override
    public JointType getType() {
        return JointType.CONSTRAINED_BALL;
    }

    /**
     * heading, rotating by Z axis of parent bone
     */
    public void yaw(double torque) {
        DVector3C front = new DVector3(getParent().getTop());
        DVector3C back = new DVector3(getParent().getBottom());
        DVector3 axis = back.sub(front);
        addTorque(torque, axis);
    }

    /**
     * heading, rotating by Z axis of child bone
     */
    public void yawChild(double torque) {
        DVector3C front = new DVector3(getChild().getTop());
        DVector3C back = new DVector3(getChild().getBottom());
        DVector3 axis = back.sub(front);
        addTorque(torque, axis);
    }

    /**
     * bank, rotating by X axis of parent bone
     */
    public void roll(double torque) {
        DVector3C front = new DVector3(getParent().getFront());
        DVector3C back = new DVector3(getParent().getBack());
        DVector3 axis = back.sub(front);
        addTorque(torque, axis);
    }

    /**
     * bank, rotating by X axis of child bone
     */
    public void rollChild(double torque) {
        DVector3C front = new DVector3(getChild().getFront());
        DVector3C back = new DVector3(getChild().getBack());
        DVector3 axis = back.sub(front);
        addTorque(torque, axis);
    }

    /**
     * altitude, rotating by Y axis of parent bone
     */
    public void pitch(double torque) {
        DVector3C front = new DVector3(getParent().getLeft());
        DVector3C back = new DVector3(getParent().getRight());
        DVector3 axis = back.sub(front);
        addTorque(torque, axis);
    }

    /**
     * altitude, rotating by Y axis of child bone
     */
    public void pitchChild(double torque) {
        DVector3C front = new DVector3(getChild().getLeft());
        DVector3C back = new DVector3(getChild().getRight());
        DVector3 axis = back.sub(front);
        addTorque(torque, axis);
    }

    private void addTorque(double torque, DVector3 axis) {
        axis.scale(torque);
        getParent().getBody().addTorque(axis);
        getChild().getBody().addTorque(axis.reScale(-1));
    }

}
