package deep.learning.human.ragdoll;

import org.ode4j.ode.DSpace;
import org.ode4j.ode.DWorld;

public class DxHumanBuilder {

    public static DxHuman build(DWorld world, DSpace space) {
        return new DxHuman(world, space);
    }

}
