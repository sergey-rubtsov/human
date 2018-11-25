package deep.learning.human.humanoid.joints;

import deep.learning.human.HumanBone;
import deep.learning.human.JointType;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DUniversalJoint;

public class UniversalJoint extends Joint {

    public UniversalJoint(String name, HumanBone parent, HumanBone child, DJoint joint) {
        super(name, parent, child, joint);
    }

    @Override
    public JointType getType() {
        return JointType.UNIVERSAL;
    }

    public void addTorque(double torque1, double torque2) {
        ((DUniversalJoint)getJoint()).addTorques(torque1, torque2);
    }
}
