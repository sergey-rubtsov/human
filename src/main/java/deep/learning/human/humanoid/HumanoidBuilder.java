package deep.learning.human.humanoid;

import org.ode4j.math.DVector3;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;

import java.util.HashMap;
import java.util.Map;

import deep.learning.human.Human;
import deep.learning.human.HumanBone;
import deep.learning.human.HumanJoint;
import deep.learning.human.bvh.Node;
import deep.learning.human.bvh.Skeleton;
import deep.learning.human.humanoid.joints.JointBuilder;
import deep.learning.human.utils.Utils;
import deep.learning.human.utils.config.BoneConfig;
import deep.learning.human.utils.config.HumanConfig;
import deep.learning.human.utils.config.JointConfig;

public class HumanoidBuilder {

    private static final double BONE_SHORTENING = 0.99;

    public static Human build(DWorld world, DSpace space, String configFile) {
        HumanConfig config = Utils.readHumanConfig(configFile);
        return build(world, space, config);
    }

    public static Human build(DWorld world, DSpace space, HumanConfig config) {
        BoneBuilder boneBuilder = new BoneBuilder(world, space);
        Map<String, HumanBone> bones = new HashMap<>();
        Map<String, HumanJoint> joints = new HashMap<>();
        config.getBones().forEach((key, value) -> bones.put(key, boneBuilder.buildBone(value)));
        Utils.adjustMass(bones.values(), 80);
        JointBuilder jointBuilder = new JointBuilder(world, config.getBones());
        config.getJoints().forEach((key, value) -> joints.put(key, jointBuilder.buildJointFromConfig(value, bones)));
        Human human = new Humanoid(80, bones, joints);
        Utils.setAngularDamping(human, 0.1);
        return human;
    }

    public static HumanConfig build(String bvhFile, String... parameter) {
        Skeleton skeleton = new Skeleton(bvhFile);
        skeleton.setPose(0);
        Node root = skeleton.getRootNode();

        Utils.preProcessBones(root, parameter);

        Map<String, BoneConfig> bones = new HashMap<>();
        Map<String, JointConfig> joints = new HashMap<>();
        processBones(root, bones);
        processJoints(root, bones, joints);
        //set default anchors
        joints.forEach((key, value) -> value.setAnchor(bones.get(value.getChild()).getP1()));
        shortenBones(bones);
        return new HumanConfig(bones, joints);
    }

    private static void shortenBones(Map<String, BoneConfig> bones) {
        bones.forEach((key, value) -> {
            DVector3 begin = new DVector3(value.getP1());
            DVector3 end = new DVector3(value.getP2());
            value.setP2(end.sub(begin).scale(BONE_SHORTENING).add(begin));
        });
    }

    private static void processBones(Node node, Map<String, BoneConfig> bones) {
        node.getChildren().forEach(child -> processBones(child, bones));
        if (node.getType() == Node.Type.END || node.getType() == Node.Type.ROOT) {
            return;
        }
        Node begin = node.getParent();
        bones.put(node.getName(), new BoneConfig(node.getName(), Utils.translate(begin.getPosition()),
                Utils.translate(node.getPosition())));
    }

    private static void processJoints(Node node, Map<String, BoneConfig> bones, Map<String, JointConfig> joints) {
        node.getChildren().forEach(child -> processJoints(child, bones, joints));
        if (node.getType() == Node.Type.END || node.getType() == Node.Type.ROOT) {
            return;
        }
        if (node.getParent().getType() == Node.Type.ROOT) {
            node.getParent().getChildren().forEach(child -> {
                BoneConfig begin = bones.get(node.getName());
                BoneConfig end = bones.get(child.getName());
                JointConfig joint = new JointConfig(begin.getName() + end.getName(),
                        begin.getName(),
                        end.getName());
                joints.put(joint.getName(), joint);
            });
            return;
        }
        Node begin = node.getParent();
        JointConfig joint = new JointConfig(begin.getName() + node.getName(), begin.getName(), node.getName());
        joints.put(joint.getName(), joint);
    }

}
