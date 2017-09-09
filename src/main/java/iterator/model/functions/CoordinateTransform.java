package iterator.model.functions;

import java.awt.Dimension;

import com.google.common.base.CaseFormat;

import iterator.model.Function;

public enum CoordinateTransform {
    IDENTITY("id", Identity.create()),
    SPHERICAL("sph", Spherical.create()),
    SWIRL("sw", Swirl.create()),
    HORSESHOE("hrs", Horseshoe.create()),
    BENT("bent", Bent.create()),
    FISHEYE("fish", Fisheye.create()),
    EXPONENTIAL("exp", Exponential.create()),
    BUBBLE("bbl", Bubble.create()),
    CYLINDER("cyl", Cylinder.create()),
    TANGENT("tan", Tangent.create());

    private final String name;
    private final Function function;

    private CoordinateTransform(String name, Function function) {
        this.name = name;
        this.function = function;
    }

    public String getShortName() { return name; }

    public Function getFunction() { return function; }

    public static Function forName(String name, Dimension size) {
        Function f = valueOf(name).getFunction();
        f.setSize(size);
        return f;
    }

    @Override
    public String toString() {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, name());
    }
}
