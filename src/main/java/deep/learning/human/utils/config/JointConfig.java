package deep.learning.human.utils.config;

import deep.learning.human.JointType;
import org.ode4j.math.DVector3;

public class JointConfig {

    private String name;
    private JointType type;
    private String parent;
    private String child;
    private DVector3 axis1;
    private DVector3 axis2;
    private double loStop1;
    private double hiStop1;
    private double loStop2;
    private double hiStop2;
    private DVector3 anchor;
    private DVector3 flexAxis;
    private DVector3 twistAxis;
    private double flexLimit;
    private double twistLimit;

    public JointConfig() {
    }

    public JointConfig(String name, String parent, String child) {
        this.name = name;
        this.parent = parent;
        this.child = child;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getChild() {
        return child;
    }

    public void setChild(String child) {
        this.child = child;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JointType getType() {
        return type;
    }

    public void setType(JointType type) {
        this.type = type;
    }

    public DVector3 getAxis1() {
        return axis1;
    }

    public void setAxis1(DVector3 axis1) {
        this.axis1 = axis1;
    }

    public DVector3 getAxis2() {
        return axis2;
    }

    public void setAxis2(DVector3 axis2) {
        this.axis2 = axis2;
    }

    public double getLoStop1() {
        return loStop1;
    }

    public void setLoStop1(double loStop1) {
        this.loStop1 = loStop1;
    }

    public double getHiStop1() {
        return hiStop1;
    }

    public void setHiStop1(double hiStop1) {
        this.hiStop1 = hiStop1;
    }

    public double getLoStop2() {
        return loStop2;
    }

    public void setLoStop2(double loStop2) {
        this.loStop2 = loStop2;
    }

    public double getHiStop2() {
        return hiStop2;
    }

    public void setHiStop2(double hiStop2) {
        this.hiStop2 = hiStop2;
    }

    public DVector3 getAnchor() {
        return anchor;
    }

    public void setAnchor(DVector3 anchor) {
        this.anchor = anchor;
    }

    public DVector3 getFlexAxis() {
        return flexAxis;
    }

    public void setFlexAxis(DVector3 flexAxis) {
        this.flexAxis = flexAxis;
    }

    public DVector3 getTwistAxis() {
        return twistAxis;
    }

    public void setTwistAxis(DVector3 twistAxis) {
        this.twistAxis = twistAxis;
    }

    public double getFlexLimit() {
        return flexLimit;
    }

    public void setFlexLimit(double flexLimit) {
        this.flexLimit = flexLimit;
    }

    public double getTwistLimit() {
        return twistLimit;
    }

    public void setTwistLimit(double twistLimit) {
        this.twistLimit = twistLimit;
    }
}
