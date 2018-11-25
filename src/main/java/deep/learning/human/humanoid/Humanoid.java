package deep.learning.human.humanoid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import deep.learning.human.Human;
import deep.learning.human.HumanBone;
import deep.learning.human.HumanJoint;

public class Humanoid implements Human {

    private double mass;

    private Map<String, HumanBone> bones;

    private Map<String, HumanJoint> joints;

    public Humanoid(double mass, Map<String, HumanBone> bones, Map<String, HumanJoint> joints) {
        this.mass = mass;
        this.bones = bones;
        this.joints = joints;
    }

    @Override
    public double getMass() {
        return mass;
    }

    @Override
    public Map<String, HumanBone> getBones() {
        return bones;
    }

    @Override
    public Map<String, HumanJoint> getJoints() {
        return joints;
    }

    public List<HumanBone> getBonesList() {
        return new ArrayList<>(bones.values());
    }

    public List<HumanJoint> getJointsList() {
        return new ArrayList<>(joints.values());
    }

    @Override
    public HumanBone getBone(String name) {
        return bones.get(name);
    }

    @Override
    public HumanJoint getJoint(String name) {
        return joints.get(name);
    }

    @Override
    public void destroy() {

    }
}
