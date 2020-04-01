package iterator.model.functions;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.CaseFormat;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.primitives.Ints;

import iterator.model.Function;

public abstract class CoordinateTransform implements Function {

    public enum Type {

        IDENTITY("id", Identity.create()),
        SPHERICAL("sph", Spherical.create()),
        SWIRL("sw", Swirl.create()),
        HORSESHOE("hrs", Horseshoe.create()),
        POLAR("pol", Polar.create()),
        HYPERBOLIC("hyp", Hyperbolic.create()),
        BENT("bent", Bent.create()),
        FISHEYE("fish", Fisheye.create()),
        EXPONENTIAL("exp", Exponential.create()),
        BUBBLE("bbl", Bubble.create()),
        EYEFISH("eye", Eyefish.create()),
        CYLINDER("cyl", Cylinder.create()),
        TANGENT("tan", Tangent.create());

        private final String name;
        private final CoordinateTransform function;

        Type(String name, CoordinateTransform function) {
            this.name = name;
            this.function = function;
        }

        public String getShortName() { return name; }

        public CoordinateTransform getFunction() { return function; }

        public static Type[] ordered() {
            List<Type> values = Arrays.asList(values());
            values.sort((a, b) -> Ints.compare(a.getFunction().id, b.getFunction().id));
            return values.toArray(new Type[0]);
        }

        @Override
        public String toString() {
            return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, name());
        }
    }

    protected int id;
    protected int sw;
    protected int sh;

    @Override
    public Dimension getSize() {
        return new Dimension(sw, sh);
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public void setId(int id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setSize(Dimension size) {
        sw = size.width;
        sh = size.height;
    }

    @Override
    public AffineTransform getTransform() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof CoordinateTransform)) return false;
        CoordinateTransform that = (CoordinateTransform) object;
        return Objects.equal(id, that.id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("id", id)
                .toString();
    }

}
