package deep.learning.human.humanoid.joints;

import deep.learning.human.HumanBone;
import deep.learning.human.JointType;
import org.ode4j.ode.DHingeJoint;
import org.ode4j.ode.DJoint;

public class HingeJoint extends Joint {

    public HingeJoint(String name, HumanBone parent, HumanBone child, DJoint joint) {
        super(name, parent, child, joint);
    }

    @Override
    public JointType getType() {
        return JointType.HINGE;
    }

    public void addTorque(double torque) {
        ((DHingeJoint)getJoint()).addTorque(torque);
    }
}
