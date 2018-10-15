package deep.learning.human.utils.config;

import java.util.HashMap;
import java.util.Map;

public class HumanConfig {

    private Map<String, BoneConfig> bones = new HashMap<>();

    private Map<String, JointConfig> joints = new HashMap<>();

    public HumanConfig() {
    }

    public HumanConfig(Map<String, BoneConfig> bones, Map<String, JointConfig> joints) {
        this.bones = bones;
        this.joints = joints;
    }

    public Map<String, BoneConfig> getBones() {
        return bones;
    }

    public void setBones(Map<String, BoneConfig> bones) {
        this.bones = bones;
    }

    public Map<String, JointConfig> getJoints() {
        return joints;
    }

    public void setJoints(Map<String, JointConfig> joints) {
        this.joints = joints;
    }
}
