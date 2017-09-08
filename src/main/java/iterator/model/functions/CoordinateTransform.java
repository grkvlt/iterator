package iterator.model.functions;

import java.awt.Dimension;

import com.google.common.base.CaseFormat;

import iterator.model.Function;

public enum CoordinateTransform {
    IDENTITY("id") {
        @Override
        public Function getFunction(Dimension size) {
            return Identity.create(size);
        }
    },
    SPHERICAL("sph") {
        @Override
        public Function getFunction(Dimension size) {
            return Spherical.create(size);
        }
    },
    EXPONENTIAL("exp") {
        @Override
        public Function getFunction(Dimension size) {
            return Exponential.create(size);
        }
    },
    CYLINDER("cyl") {
        @Override
        public Function getFunction(Dimension size) {
            return Cylinder.create(size);
        }
    },
    TANGENT("tan") {
        @Override
        public Function getFunction(Dimension size) {
            return Tangent.create(size);
        }
    };

    public abstract Function getFunction(Dimension size);

    private final String name;

    private CoordinateTransform(String name) {
        this.name = name;
    }

    public String getShortName() { return name; }

    public static Function forName(String name, Dimension size) {
        return valueOf(name).getFunction(size);
    }

    @Override
    public String toString() {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_HYPHEN, name());
    }
}
