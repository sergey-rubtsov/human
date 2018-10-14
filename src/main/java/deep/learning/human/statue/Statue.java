package deep.learning.human.statue;

import deep.learning.human.HumanJoint;
import deep.learning.human.Human;
import deep.learning.human.HumanBone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Statue implements Human {

    private double mass;

    private Map<String, HumanBone> bones;

    private Map<String, HumanJoint> joints;

    public Statue(double mass, Map<String, HumanBone> bones, Map<String, HumanJoint> joints) {
        this.mass = mass;
        this.bones = bones;
        this.joints = joints;
    }

    public double getMass() {
        return 0;
    }

    public List<HumanBone> getBones() {
        return new ArrayList<HumanBone>(bones.values());
    }

    public List<HumanJoint> getJoints() {
        return new ArrayList<HumanJoint>(joints.values());
    }

    public HumanBone getBone(String name) {
        return bones.get(name);
    }

    public HumanJoint getJoint(String name) {
        return joints.get(name);
    }

    public void destroy() {

    }
}
