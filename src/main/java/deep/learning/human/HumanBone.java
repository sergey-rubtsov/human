package deep.learning.human;

import org.ode4j.math.DVector3;
import org.ode4j.ode.DBody;

public interface HumanBone {

    String getName();

    BoneType getType();

    double getRadius();

    double getLength();

    DBody getBody();

    void setBody(DBody body);

    DVector3 getLeft();

    DVector3 getRight();

    DVector3 getFront();

    DVector3 getBack();

    DVector3 getTop();

    DVector3 getBottom();

}
