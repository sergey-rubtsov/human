package deep.learning.human;

import org.ode4j.ode.DBody;

public interface HumanBone {

    String getName();

    double getRadius();

    double getLength();

    DBody getBody();

}
