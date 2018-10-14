package deep.learning.human;

import java.util.List;

public interface Human {

    double getMass();

    List<HumanBone> getBones();

    List<HumanJoint> getJoints();

    HumanBone getBone(String name);

    HumanJoint getJoint(String name);

    void destroy();

}
