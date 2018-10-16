package deep.learning.human.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import deep.learning.human.Human;
import deep.learning.human.HumanBone;
import deep.learning.human.bvh.Node;
import deep.learning.human.bvh.Vec4;
import deep.learning.human.utils.config.HumanConfig;
import org.ode4j.math.DMatrix3;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector4;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DContact;
import org.ode4j.ode.DContactBuffer;
import org.ode4j.ode.DContactJoint;
import org.ode4j.ode.DGeom;
import org.ode4j.ode.DJoint;
import org.ode4j.ode.DJointGroup;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.ode4j.ode.OdeConstants.dContactBounce;
import static org.ode4j.ode.OdeConstants.dContactSoftCFM;
import static org.ode4j.ode.OdeHelper.areConnectedExcluding;

public class Utils {

    private static final int MAX_CONTACTS = 128;        // maximum number of contact points per body

    private static final double MIN_VOLUME = 0.0001;

    public static void nearCallback(Object data, DGeom o1, DGeom o2, DWorld world, DJointGroup contactGroup) {
        // exit without doing anything if the two bodies are connected by a joint
        DBody b1 = o1.getBody();
        DBody b2 = o2.getBody();
        if (b1 != null && b2 != null && areConnectedExcluding(b1, b2, DContactJoint.class)) {
            return;
        }
        DContactBuffer contacts = new DContactBuffer(MAX_CONTACTS);   // up to MAX_CONTACTS contacts per box-box
        for (DContact contact : contacts) {
            contact.surface.mode = dContactBounce | dContactSoftCFM;
            contact.surface.mu = 100;
            contact.surface.mu2 = 0;
            contact.surface.bounce = 0.01;
            contact.surface.bounce_vel = 0.01;
            contact.surface.soft_cfm = 0.0001;
        }
        int collide = OdeHelper.collide(o1, o2, MAX_CONTACTS, contacts.getGeomBuffer());
        if (collide != 0) {
            DMatrix3 RI = new DMatrix3();
            RI.setIdentity();
            //final DVector3 ss = new DVector3(0.02, 0.02, 0.02);
            for (int i = 0; i < collide; i++) {
                DJoint c = OdeHelper.createContactJoint(world, contactGroup, contacts.get(i));
                c.attach(b1, b2);
            }
        }
    }

    public static void adjustMass(Collection<HumanBone> skeleton, double totalMass) {
        double volume = skeleton.stream().mapToDouble(el -> el.getLength() * el.getRadius()).sum();
        skeleton.forEach(el -> {
            double elVol = el.getLength() * el.getRadius();
            if (elVol < MIN_VOLUME) {
                elVol = MIN_VOLUME;
            }
            double newValue = (elVol / volume) * totalMass;
            DMass mass = ((DMass) el.getBody().getMass());
            mass.adjust(newValue);
            el.getBody().setMass(mass);
        });
    }

    public static void setAngularDamping(Human skeleton, double damping) {
        for (HumanBone bone : skeleton.getBones()) {
            bone.getBody().setAngularDamping(damping);
        }
    }

    public static DVector3 translate(Vec4 nodeBegin) {
        return new DVector3(nodeBegin.getX(), nodeBegin.getY() + 10, nodeBegin.getZ());
    }

    /**
     * A surface normal for a triangle can be calculated by taking the vector cross
     * product of two edges of that triangle. The order of the vertices used in the
     * calculation will affect the direction of the normal (in or out of the face w.r.t. winding).
     * So for a triangle p1, p2, p3, if the vector
     * U = p2 - p1
     * and the vector
     * V = p3 - p1
     * then the normal
     * N = U X V and can be calculated by:
     * Nx = UyVz - UzVy
     * Ny = UzVx - UxVz
     * Nz = UxVy - UyVx
     */
    public static DVector3 calculateNormal(DVector3 p1, DVector3 p2, DVector3 p3) {
        //Get the UV data
        DVector3 u = p2.clone().sub(p1.clone());
        DVector3 v = p3.clone().sub(p1.clone());
        //Calculate normals
        DVector3 normal = new DVector3();
        normal.set0((u.get1() * v.get2()) - (u.get2() * v.get1()));
        normal.set1((u.get2() * v.get0()) - (u.get0() * v.get2()));
        normal.set2((u.get0() * v.get1()) - (u.get1() * v.get0()));
        //Normalise them
        double nf = Math.sqrt(normal.get0() * normal.get0() +
                normal.get1() * normal.get1() +
                normal.get2() * normal.get2());
        if (nf > 0) {
            normal.set0(normal.get0() / nf);
            normal.set1(normal.get1() / nf);
            normal.set2(normal.get2() / nf);
        }
        return normal;
    }

    public static DVector4 calculateNormalAndDistance(DVector3 p1, DVector3 p2, DVector3 p3) {
        //Get the UV data
        DVector3 u = p2.clone().sub(p1.clone());
        DVector3 v = p3.clone().sub(p1.clone());
        //Calculate normals
        DVector3 normal = new DVector3();
        normal.set0((u.get1() * v.get2()) - (u.get2() * v.get1()));
        normal.set1((u.get2() * v.get0()) - (u.get0() * v.get2()));
        normal.set2((u.get0() * v.get1()) - (u.get1() * v.get0()));
        //Normalise them
        double nf = Math.sqrt(normal.get0() * normal.get0() +
                normal.get1() * normal.get1() +
                normal.get2() * normal.get2());
        if (nf > 0) {
            normal.set0(normal.get0() / nf);
            normal.set1(normal.get1() / nf);
            normal.set2(normal.get2() / nf);
        }
        DVector4 result = new DVector4();
        double a = normal.get0();
        if (a == -0.0) {
            a = 0;
        }
        result.set0(a);
        double b = normal.get1();
        if (b == -0.0) {
            b = 0;
        }
        result.set1(b);
        double c = normal.get2();
        if (c == -0.0) {
            c = 0;
        }
        result.set2(c);
        result.set3(nf);
        return result;
    }

    public static double[] calculatePlanes(double[] points, int[] polygons, int planeCount) {
        Set<DVector4> normals = calculateNormals(points, polygons, planeCount);
        double[] planes = new double[normals.size() * 4];
        int i = 0;
        for (DVector4 vector : normals) {
            planes[i * 4] = vector.get0();
            planes[i * 4 + 1] = vector.get1();
            planes[i * 4 + 2] = vector.get2();
            planes[i * 4 + 3] = vector.get3();
            i++;
        }
        return planes;
    }

    private static Set<DVector4> calculateNormals(double[] points, int[] polygons, int planeCount) {
        Set<DVector4> normals = new LinkedHashSet<>();
        int face = (int) Math.ceil(polygons.length / (double) planeCount);
        int[][] faces = new int[planeCount][face];
        for (int i = 0; i < planeCount; i++) {
            for (int j = 0; j < face; j++) {
                faces[i][j] = polygons[i * face + j];
            }
        }
        for (int i = 0; i < faces.length; i++) {
            int a = faces[i][1];
            int b = faces[i][2];
            int c = faces[i][3];
            DVector3 p1 = new DVector3(points[a * 3], points[a * 3 + 1], points[a * 3 + 2]);
            DVector3 p2 = new DVector3(points[b * 3], points[b * 3 + 1], points[b * 3 + 2]);
            DVector3 p3 = new DVector3(points[c * 3], points[c * 3 + 1], points[c * 3 + 2]);
            normals.add(calculateNormalAndDistance(p1, p2, p3));
        }
        return normals;
    }

    public static double[] octahedronPoints(double length, double radius) {
        //coordinates of vertexes
        return new double[]{
                -radius, -radius, 0, //0
                radius, -radius, 0,  //1
                radius, radius, 0,   //2
                -radius, radius, 0,  //3
                0, 0, -length / 2,       //4
                0, 0, length / 2         //5
        };
    }

    public static int[] octahedronPolygons() {
        //first number in row is count of vertexes on face
        //count of rows is count of faces
        return new int[]{
                3, 1, 5, 0,
                3, 2, 5, 1,
                3, 3, 5, 2,
                3, 0, 5, 3,
                3, 4, 3, 2,
                3, 4, 2, 1,
                3, 4, 1, 0,
                3, 4, 0, 3
        };
    }

    public static HumanConfig readHumanConfig(String fileName) {
        try {
            ClassLoader classLoader = Utils.class.getClassLoader();
            return new ObjectMapper().readValue(classLoader.getResourceAsStream(fileName),
                    new TypeReference<HumanConfig>() {
                    });
        } catch (IOException iox) {
            throw new UnsupportedOperationException(iox);
        }
    }

    public static void processDuplications(Node node) {
        node.getChildren().forEach(Utils::processDuplications);
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

}
