package deep.learning.human.statue;

import deep.learning.human.utils.config.BoneConfig;
import deep.learning.human.utils.config.HumanConfig;
import deep.learning.human.utils.config.JointConfig;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import deep.learning.human.Human;
import deep.learning.human.HumanBone;
import deep.learning.human.HumanJoint;
import deep.learning.human.bvh.Node;
import deep.learning.human.bvh.Skeleton;
import deep.learning.human.utils.Utils;

public class StatueBuilder {

    public static HumanConfig build(String file) {
        Skeleton skeleton = new Skeleton(file);
        skeleton.setPose(0);
        Node root = skeleton.getRootNode();
        processDuplications(root);
        processDuplications(root);
        Map<String, BoneConfig> bones = new HashMap<>();
        Map<String, JointConfig> joints = new HashMap<>();
        processBones(root, bones);
        processJoints(root, bones, joints);
        return new HumanConfig(bones, joints);
    }

    public static Human build(DWorld world, DSpace space, double height) {
        Skeleton skeleton = new Skeleton("1.bvh");
        skeleton.setPose(0);
        Node root = skeleton.getRootNode();
        processDuplications(root);
        processDuplications(root);
        Map<String, HumanBone> bones = new HashMap<>();
        Map<String, HumanJoint> joints = new HashMap<>();
        processBones(world, space, root, bones);
        Utils.adjustMass(bones.values(), 80);
        processJoints(world, root, bones, joints);
        Human human = new Statue(80, bones, joints);
        Utils.setAngularDamping(human, 0.1);
        return human;
    }

    private static void processDuplications(Node node) {
        node.getChildren().forEach(StatueBuilder::processDuplications);
        if (node.getType() == Node.Type.END) {
            return;
        }
        for (int i = 0; i < node.getChildren().size(); i++) {
            for (int k = i + 1; k < node.getChildren().size(); k++) {
                Node left = node.getChildren().get(i);
                Node right = node.getChildren().get(k);
                if (left.getType() == Node.Type.END ||
                        right.getType() == Node.Type.END) {
                    continue;
                }
                if (left.getPosition().equals(right.getPosition())) {
                    List<Node> collector = new ArrayList<>();
                    collector.addAll(left.getChildren());
                    collector.addAll(right.getChildren());
                    collector.forEach(child -> child.setParent(left));
                    right.getChildren().clear();
                    left.getChildren().clear();
                    left.setChildren(collector);
                    left.getNames().add(left.getName());
                    left.getNames().add(right.getName());
                }
            }
        }
        node.getChildren().forEach(Node::resetName);
        node.getChildren().removeIf(child -> child.getChildren().isEmpty() && child.getType() != Node.Type.END);
    }

    private static void processBones(Node node, Map<String, BoneConfig> bones) {
        node.getChildren().forEach(child -> processBones(child, bones));
        if(node.getType() == Node.Type.END || node.getType() == Node.Type.ROOT) {
            return;
        }
        Node begin = node.getParent();
        bones.put(node.getName(), new BoneConfig(node.getName(), Utils.translate(begin.getPosition()),
                Utils.translate(node.getPosition())));
    }

    private static void processBones(DWorld world, DSpace space, Node node, Map<String, HumanBone> bones) {
        node.getChildren().forEach(child -> processBones(world, space, child, bones));
        if(node.getType() == Node.Type.END || node.getType() == Node.Type.ROOT) {
            return;
        }
        Node begin = node.getParent();
        bones.put(node.getName(), createBone(world, space, begin, node));
    }

    private static HumanBone createBone(DWorld world,
                                        DSpace space,
                                        Node begin,
                                        Node end) {
        String name = end.getName();
        return BoneBuilder.buildBone(name,
                Utils.translate(begin.getPosition()),
                Utils.translate(end.getPosition()),
                0.2, world, space);
    }

    private static void processJoints(Node node, Map<String, BoneConfig> bones, Map<String, JointConfig> joints) {
        node.getChildren().forEach(child -> processJoints(child, bones, joints));
        if(node.getType() == Node.Type.END || node.getType() == Node.Type.ROOT) {
            return;
        }
        if(node.getParent().getType() == Node.Type.ROOT) {
            node.getParent().getChildren().forEach(child -> {
                BoneConfig begin = bones.get(node.getName());
                BoneConfig end = bones.get(child.getName());
                JointConfig joint = new JointConfig(child.getName(), begin.getName(), end.getName());
                joints.put(joint.getName(), joint);
            });
            return;
        }
        Node begin = node.getParent();
        JointConfig joint = new JointConfig(node.getName(), begin.getName(), node.getName());
        joints.put(joint.getName(), joint);
    }

    private static void processJoints(DWorld world, Node node, Map<String, HumanBone> bones, Map<String, HumanJoint> joints) {
        node.getChildren().forEach(child -> processJoints(world, child, bones, joints));
        if(node.getType() == Node.Type.END || node.getType() == Node.Type.ROOT) {
            return;
        }
        if(node.getParent().getType() == Node.Type.ROOT) {
            node.getParent().getChildren().forEach(child -> {
                HumanJoint joint = createJoint(world, node, child, bones);
                joints.put(joint.getName(), joint);
            });
            return;
        }
        Node begin = node.getParent();
        HumanJoint joint = createJoint(world, begin, node, bones);
        joints.put(joint.getName(), joint);
    }

    private static HumanJoint createJoint(DWorld world, Node begin, Node end, Map<String, HumanBone> bones) {
        HumanBone parent = bones.get(begin.getName());
        HumanBone child = bones.get(end.getName());
        return JointBuilder.buildJoint(parent, child, world);
    }

}
