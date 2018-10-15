package deep.learning.human.utils.config;

import org.ode4j.math.DVector3;

import deep.learning.human.JointType;

public class JointConfig {

    private String name;
    private JointType type;
    private String parent;
    private String child;
    private DVector3 axis1;
    private DVector3 axis2;
    private Double loStop1;
    private Double hiStop1;
    private Double loStop2;
    private Double hiStop2;
    private DVector3 anchor;
    private DVector3 flexAxis;
    private DVector3 twistAxis;
    private Double flexLimit;
    private Double twistLimit;

    public JointConfig() {
    }

    public JointConfig(String name, String parent, String child) {
        this.name = name;
        this.parent = parent;
        this.child = child;
        this.type = JointType.FIXED;
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

    public Double getLoStop1() {
        return loStop1;
    }

    public void setLoStop1(Double loStop1) {
        this.loStop1 = loStop1;
    }

    public Double getHiStop1() {
        return hiStop1;
    }

    public void setHiStop1(Double hiStop1) {
        this.hiStop1 = hiStop1;
    }

    public Double getLoStop2() {
        return loStop2;
    }

    public void setLoStop2(Double loStop2) {
        this.loStop2 = loStop2;
    }

    public Double getHiStop2() {
        return hiStop2;
    }

    public void setHiStop2(Double hiStop2) {
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

    public Double getFlexLimit() {
        return flexLimit;
    }

    public void setFlexLimit(Double flexLimit) {
        this.flexLimit = flexLimit;
    }

    public Double getTwistLimit() {
        return twistLimit;
    }

    public void setTwistLimit(Double twistLimit) {
        this.twistLimit = twistLimit;
    }
}
