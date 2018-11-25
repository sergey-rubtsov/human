package deep.learning.human.humanoid;

import org.ode4j.math.DVector3;
import org.ode4j.ode.DSpace;
import org.ode4j.ode.internal.DxCapsule;

import deep.learning.human.BoneType;
import deep.learning.human.HumanBone;

public class ShortBone extends DxCapsule implements HumanBone {

    private final String name;

    private final DVector3 left;

    private final DVector3 right;

    private final DVector3 front;

    private final DVector3 back;

    private final DVector3 top;

    private final DVector3 bottom;

    public ShortBone(DSpace space, String name, double radius, double length) {
        super(space, radius, length);
        this.name = name;
        this.left = new DVector3(radius, 0, 0);
        this.right = new DVector3(-radius, 0, 0);
        this.front = new DVector3(0, radius, 0);
        this.back = new DVector3(0, -radius, 0);
        this.top = new DVector3( 0, 0, length / 2);
        this.bottom = new DVector3(0, 0, -length / 2);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public BoneType getType() {
        return BoneType.SHORT;
    }

    public DVector3 getLeft() {
        return calculatePoint(new DVector3(left));
    }

    public DVector3 getRight() {
        return calculatePoint(new DVector3(right));
    }

    public DVector3 getFront() {
        return calculatePoint(new DVector3(front));
    }

    public DVector3 getBack() {
        return calculatePoint(new DVector3(back));
    }

    public DVector3 getTop() {
        return calculatePoint(new DVector3(top));
    }

    public DVector3 getBottom() {
        return calculatePoint(new DVector3(bottom));
    }

    private DVector3 calculatePoint(DVector3 point) {
        return getBody().getQuaternion().rotate(point).add(getBody().getPosition());
    }
}
