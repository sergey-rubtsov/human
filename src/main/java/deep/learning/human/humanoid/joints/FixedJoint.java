package deep.learning.human.humanoid.joints;

import deep.learning.human.HumanBone;
import deep.learning.human.JointType;
import org.ode4j.ode.DJoint;

public class FixedJoint extends Joint {

    public FixedJoint(String name, HumanBone parent, HumanBone child, DJoint joint) {
        super(name, parent, child, joint);
    }

    @Override
    public JointType getType() {
        return JointType.FIXED;
    }
}
