package deep.learning.human;

import org.ode4j.ode.DJoint;

public interface HumanJoint {

    String getName();

    JointType getType();

    HumanBone getParent();

    HumanBone getChild();

    DJoint getJoint();

}
