package deep.learning.human.humanoid;

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
import deep.learning.human.utils.Utils;
import deep.learning.human.utils.config.BoneConfig;

public class BoneBuilder {

    private static final double SCALE = 0.5;

    private static final int OCTAHEDRON_VERTEXES = 6;

    private static final int OCTAHEDRON_PLANES = 8;

    private DWorld world;

    private DSpace space;

    public BoneBuilder(DWorld world, DSpace space) {
        this.world = world;
        this.space = space;
    }

    public HumanBone buildBone(BoneConfig config) {
        switch (config.getType()) {
            case LONG:
                return buildLongBone(config.getName(),
                        config.getP1(),
                        config.getP2(),
                        config.getRadius(),
                        config.getDensity());
            case SHORT:
                return buildShortBone(config.getName(),
                        config.getP1(),
                        config.getP2(),
                        config.getRadius());
        }
        throw new UnsupportedOperationException();
    }

    private HumanBone buildShortBone(String name,
                                     DVector3 p1,
                                     DVector3 p2,
                                     double radius) {
        p1 = new DVector3(p1);
        p2 = new DVector3(p2);
        double length = p1.distance(p2);
        DBody body = OdeHelper.createBody(world);
        DMass m = OdeHelper.createMass();
        m.setCapsule(1, 3, radius, length);
        body.setMass(m);
        HumanBone bone = new ShortBone(space, name, radius, length);
        bone.setBody(body);
        DVector3 za = new DVector3(p2);
        (za.sub(p1)).safeNormalize();
        DVector3 xa;
        DVector3 ya;
        if (Math.abs(za.dot(new DVector3(1.0, 0.0, 0.0))) < 0.7) {
            xa = new DVector3(1.0, 0.0, 0.0);
            ya = new DVector3();
            ya.eqCross(za, xa);
            ya.safeNormalize();
            xa.eqCross(ya, za);
        } else {
            ya = new DVector3(0.0, 1.0, 0.0);
            xa = new DVector3();
            xa.eqCross(ya, za);
            xa.safeNormalize();
            ya.eqCross(za, xa);
        }
        body.setPosition(p1.add(p2).scale(SCALE));
        body.setRotation(new DMatrix3(xa.get0(),
                ya.get0(),
                za.get0(),
                xa.get1(),
                ya.get1(),
                za.get1(),
                xa.get2(),
                ya.get2(),
                za.get2()));
        return bone;
    }

    private HumanBone buildLongBone(String name,
                                    DVector3 p1,
                                    DVector3 p2,
                                    double radius,
                                    double density) {
        p1 = new DVector3(p1);
        p2 = new DVector3(p2);
        double length = p1.distance(p2);
        DBody body = OdeHelper.createBody(world);
        DMass m = OdeHelper.createMass();
        double[] points = Utils.octahedronPoints(length, radius);
        int[] polygons = Utils.octahedronPolygons();
        double[] planes = Utils.calculatePlanes(points, polygons, OCTAHEDRON_PLANES);
        HumanBone bone = new LongBone(space,
                planes,
                OCTAHEDRON_PLANES,
                points,
                OCTAHEDRON_VERTEXES,
                polygons,
                name,
                length,
                radius);
        /// Use equivalent TriMesh to set mass
        DTriMeshData triMeshData = OdeHelper.createTriMeshData();
        triMeshData.build(points, polygons);
        DTriMesh triMesh = OdeHelper.createTriMesh(space, triMeshData, null, null, null);
        m.setTrimesh(density, triMesh);
        triMesh.destroy();
        triMeshData.destroy();
        body.setMass(m);
        bone.setBody(body);
        DVector3 za = new DVector3(p2);
        (za.sub(p1)).safeNormalize();
        DVector3 xa;
        DVector3 ya;
        if (Math.abs(za.dot(new DVector3(1.0, 0.0, 0.0))) < 0.7) {
            xa = new DVector3(1.0, 0.0, 0.0);
            ya = new DVector3();
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
        body.setPosition(p1.add(p2).scale(SCALE));
        body.setRotation(new DMatrix3(xa.get0(),
                ya.get0(),
                za.get0(),
                xa.get1(),
                ya.get1(),
                za.get1(),
                xa.get2(),
                ya.get2(),
                za.get2()));
        return bone;
    }
}
