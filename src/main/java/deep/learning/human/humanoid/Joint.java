package deep.learning.human.humanoid;

import deep.learning.human.HumanBone;
import deep.learning.human.HumanJoint;
import deep.learning.human.JointType;
import org.ode4j.ode.DJoint;

public class Joint implements HumanJoint {

    private final String name;
    private final JointType type;
    private final HumanBone parent;
    private final HumanBone child;
    private final DJoint joint;

    public Joint(String name,
                 JointType type,
                 HumanBone parent,
                 HumanBone child,
                 DJoint joint) {
        this.name = name;
        this.type = type;
        this.parent = parent;
        this.child = child;
        this.joint = joint;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public JointType getType() {
        return type;
    }

    @Override
    public HumanBone getParent() {
        return parent;
    }

    @Override
    public HumanBone getChild() {
        return child;
    }

    @Override
    public DJoint getJoint() {
        return joint;
    }
}
