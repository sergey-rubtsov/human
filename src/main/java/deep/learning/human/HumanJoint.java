package deep.learning.human;

import deep.learning.human.bvh.Node;
import org.ode4j.ode.DJoint;

public interface HumanJoint {

    String getName();

    JointType getType();

    HumanBone getParent();

    HumanBone getChild();

    DJoint getJoint();

}
