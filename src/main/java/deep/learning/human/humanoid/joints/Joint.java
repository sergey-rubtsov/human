package deep.learning.human.humanoid.joints;

import deep.learning.human.HumanBone;
import deep.learning.human.HumanJoint;
import deep.learning.human.JointType;
import org.ode4j.ode.DHingeJoint;
import org.ode4j.ode.DJoint;

public abstract class Joint implements HumanJoint {

    private final String name;
    private final HumanBone parent;
    private final HumanBone child;
    private final DJoint joint;

    public Joint(String name,
                 HumanBone parent,
                 HumanBone child,
                 DJoint joint) {
        this.name = name;
        this.parent = parent;
        this.child = child;
        this.joint = joint;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public abstract JointType getType();

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
