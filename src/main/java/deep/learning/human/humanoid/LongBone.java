package deep.learning.human.humanoid;

import deep.learning.human.BoneType;
import deep.learning.human.HumanBone;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.internal.DxConvex;

public class LongBone extends DxConvex implements HumanBone {

    private final String name;

    private final double length;

    private final double radius;

    public LongBone(DSpace space,
                    double[] planes,
                    int planeCount,
                    double[] points,
                    int pointCount,
                    int[] polygons,
                    String name,
                    double length,
                    double radius) {
        super(space, planes, planeCount, points, pointCount, polygons);
        this.name = name;
        this.length = length;
        this.radius = radius;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public BoneType getType() {
        return BoneType.LONG;
    }

    @Override
    public double getRadius() {
        return radius;
    }

    @Override
    public double getLength() {
        return length;
    }
}
