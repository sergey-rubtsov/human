package deep.learning.human;

import java.util.List;
import java.util.Map;

public interface Human {

    double getMass();

    Map<String, HumanBone> getBones();

    Map<String, HumanJoint> getJoints();

    List<HumanBone> getBonesList();

    List<HumanJoint> getJointsList();

    HumanBone getBone(String name);

    HumanJoint getJoint(String name);

    void destroy();

}
