package deep.learning.human.statue;

import org.ode4j.math.DMatrix3;
import org.ode4j.math.DVector3;
import org.ode4j.ode.DBody;
import org.ode4j.ode.DMass;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.DTriMesh;
import org.ode4j.ode.DTriMeshData;
import org.ode4j.ode.DWorld;
import org.ode4j.ode.OdeHelper;

import deep.learning.human.HumanBone;

public class BoneBuilder {

    private final String file;

    private static final double DENSITY = (5.0);        // density of all objects

    public BoneBuilder(String file) {
        this.file = file;
    }

    public HumanBone buildBone(String name,
                               DVector3 p1,
                               DVector3 p2,
                               DWorld world,
                               DSpace space) {
        double length = p1.distance(p2);
        return BoneImpl.createBone(space, name, length, 4);
    }

    public static HumanBone buildBone(String name,
                                      DVector3 p1,
                                      DVector3 p2,
                                      double radius,
                                      DWorld world,
                                      DSpace space) {
        p1 = new DVector3(p1);
        p2 = new DVector3(p2);
        double length = p1.distance(p2);
        DBody body = OdeHelper.createBody(world);
        DMass m = OdeHelper.createMass();

        BoneImpl bone = BoneImpl.createBone(space, name, length, radius);
        /// Use equivalent TriMesh to set mass
        DTriMeshData triMeshData = OdeHelper.createTriMeshData();
        triMeshData.build(bone.getPoints(), bone.getPolygons());
        DTriMesh triMesh = OdeHelper.createTriMesh(space, triMeshData, null, null, null);
        m.setTrimesh(DENSITY, triMesh);
        triMesh.destroy();
        triMeshData.destroy();

        //m.setCapsule(1, 3, radius, length);
        body.setMass(m);

        bone.setBody(body);
        DVector3 za = new DVector3(p2);
        (za.sub(p1)).safeNormalize();
        DVector3 xa;
        DVector3 ya;
        if (Math.abs(za.dot(new DVector3(1.0, 0.0, 0.0))) < 0.7) {
            ya = new DVector3();
            xa = new DVector3(1.0, 0.0, 0.0);
            ya.eqCross(za, xa);
            ya.safeNormalize();
            xa.eqCross(ya, za);
        } else {
            xa = new DVector3();
            ya = new DVector3(0.0, 1.0, 0.0);
            xa.eqCross(ya, za);
            xa.safeNormalize();
            ya.eqCross(za, xa);
        }
        body.setPosition(p1.add(p2).scale(0.9));
        body.setRotation(new DMatrix3(xa.get0(), ya.get0(), za.get0(), xa.get1(), ya.get1(), za.get1(), xa.get2(), ya.get2(), za.get2()));
        return bone;
    }

}
