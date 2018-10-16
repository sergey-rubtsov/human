package deep.learning.human.humanoid;

import org.ode4j.ode.DSpace;
import org.ode4j.ode.internal.DxCapsule;

import deep.learning.human.BoneType;
import deep.learning.human.HumanBone;

public class ShortBone extends DxCapsule implements HumanBone {

    private final String name;

    public ShortBone(DSpace space, String name, double radius, double length) {
        super(space, radius, length);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public BoneType getType() {
        return BoneType.SHORT;
    }
}
