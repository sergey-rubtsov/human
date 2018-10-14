package deep.learning.human.ragdoll;

import org.ode4j.math.DQuaternion;
import org.ode4j.math.DQuaternionC;
import org.ode4j.math.DVector3;
import org.ode4j.math.DVector3C;
import org.ode4j.ode.DBody;

public class DxHumanBone {

    public final DBody body;
    public final double length;
    public final double radius;
    public final DVector3 position;
    public final DQuaternion quaternion;
    public DVector3[] pBuffer;
    public DQuaternion[] qBuffer;

    public DxHumanBone(DBody body, double length, double radius, DVector3C pinitial, DQuaternionC qinitial) {
        this.body = body;
        this.length = length;
        this.radius = radius;
        this.position = new DVector3(pinitial);
        this.quaternion = new DQuaternion(qinitial.get0(), qinitial.get1(), qinitial.get2(), qinitial.get3());
    }

    public double getLength() {
        return length;
    }

    public double getRadius() {
        return radius;
    }

    public DVector3 getPosition() {
        return position;
    }

    public DQuaternion getQuaternion() {
        return quaternion;
    }

    public DBody getBody() {
        return body;
    }

}
