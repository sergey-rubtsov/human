package deep.learning.human.bvh;

import java.util.Objects;

/**
 * Vec4 class.
 *
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class Vec4 {

    double x;
    double y;
    double z;
    double w;

    public Vec4() {
    }

    public Vec4(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vec4(Vec4 v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = v.w;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public double getW() {
        return w;
    }

    public void setW(double w) {
        this.w = w;
    }

    public void set(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public void set(Vec4 v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = v.w;
    }

    public void add(Vec4 v) {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        this.w += v.w;
    }

    public void sub(Vec4 v) {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        this.w -= v.w;
    }

    @Override
    public String toString() {
        return "Vec4{" + "x=" + x + ", y=" + y + ", z=" + z + ", w=" + w + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vec4 vec4 = (Vec4) o;
        return Double.compare(vec4.x, x) == 0 &&
                Double.compare(vec4.y, y) == 0 &&
                Double.compare(vec4.z, z) == 0 &&
                Double.compare(vec4.w, w) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, w);
    }
}
