/*
 * Copyright 2012-2017 by Andrew Kennedy.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package iterator.model;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Transform Model.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Transform")
public class Transform implements Function {

    @XmlAttribute
    private int id;
    @XmlAttribute
    private int zIndex;
    @XmlAttribute(required = false)
    public Integer x;
    @XmlAttribute(required = false)
    public Integer y;
    @XmlAttribute(required = false)
    public Double w;
    @XmlAttribute(required = false)
    public Double h;
    @XmlAttribute(required = false)
    public Double r;
    @XmlAttribute(required = false)
    public Double shx = 0d;
    @XmlAttribute(required = false)
    public Double shy = 0d;
    @XmlAttribute(required = false)
    public Double weight;
    @XmlAttribute
    private int sw;
    @XmlAttribute
    private int sh;
    @XmlAttribute(required = false)
    private double matrix[] = null;

    private Transform() {
        // JAXB
    }

    private Transform(Dimension size) {
        this(-1, 0, size);
    }

    private Transform(int id, int zIndex, Dimension size) {
        this.id = id;
        this.zIndex = zIndex;
        this.sw = size.width;
        this.sh = size.height;
        this.x = 0;
        this.y = 0;
        this.w = 0d;
        this.h = 0d;
        this.r = 0d;
        this.shx = 0d;
        this.shy = 0d;
    }

    public static Transform create(Dimension size) {
        return new Transform(size);
    }

    public static Transform create(int id, int zIndex, Dimension size) {
        return new Transform(id, zIndex, size);
    }

    public static Transform copy(Transform original) {
        Transform copy = new Transform(original.getSize());
        copy.duplicate(original);
        return copy;
    }

    public static Transform clone(Transform original) {
        Transform copy = new Transform(original.id, original.zIndex, original.getSize());
        copy.duplicate(original);
        return copy;
    }

    public void duplicate(Transform original) {
        if (original.isMatrix()) {
            this.matrix = Arrays.copyOf(original.matrix, original.matrix.length);
        } else {
            this.x = original.x;
            this.y = original.y;
            this.w = original.w;
            this.h = original.h;
            this.r = original.r;
            this.shx = original.shx;
            this.shy = original.shy;
        }
        this.weight = original.weight;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public int getZIndex() {
        return this.zIndex;
    }

    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }

    @Override
    public Dimension getSize() {
        return new Dimension(sw, sh);
    }

    public void setMatrix(double[] matrix) {
        this.matrix = Arrays.copyOf(matrix, matrix.length);
        this.x = null;
        this.y = null;
        this.w = null;
        this.h = null;
        this.r = null;
        this.shx = null;
        this.shy = null;
    }

    public boolean isMatrix() { return matrix != null; }

    public double getDeterminant() {
        return getTransform().getDeterminant();
    }

    public Double getWeight() {
        return weight != null ? weight : getDeterminant();
    }

    @Override
    public void setSize(Dimension size) {
        Point2D scale = new Point2D.Double(size.getWidth() / sw, size.getHeight() / sh);
        sw = size.width;
        sh = size.height;

        if (isMatrix()) {
            AffineTransform transform = getTransform();
            double tx = transform.getTranslateX();
            double ty = transform.getTranslateY();
            transform.translate(-tx, -ty);
            transform.scale(scale.getX(), scale.getY());
            transform.translate(tx * scale.getX(), ty * scale.getY());
            double scaled[] = new double[6];
            transform.getMatrix(scaled);
            matrix = scaled;
        } else {
            w *= scale.getX();
            h *= scale.getY();
            x = (int) (scale.getX() * x);
            y = (int) (scale.getY() * y);
        }
    }

    @Override
    public AffineTransform getTransform() {
        AffineTransform transform = new AffineTransform();
        if (isMatrix()) {
            transform.setTransform(matrix[0], matrix[1], matrix[2], matrix[3], matrix[4], matrix[5]);
        } else {
            transform.translate(x, y);
            transform.shear(shx, shy);
            transform.rotate(r);
            transform.scale(w / sw, h / sh);
        }
        return transform;
    }

    public double getTranslateX() {
        return getTransform().getTranslateX();
    }

    public double getTranslateY() {
        return getTransform().getTranslateY();
    }

    public double getScaleX() {
        return getTransform().getScaleX();
    }

    public double getScaleY() {
        return getTransform().getScaleY();
    }

    public double getShearX() {
        return getTransform().getShearX();
    }

    public double getShearY() {
        return getTransform().getShearY();
    }

    public double getWidth() {
        return getTransform().getScaleX() * sw;
    }

    public double getHeight() {
        return getTransform().getScaleY() * sh;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Transform)) return false;
        Transform that = (Transform) object;
        return Objects.equal(id, that.id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("id", id)
                .add("zIndex", zIndex)
                .add("x", x)
                .add("y", y)
                .add("w", w)
                .add("h", h)
                .add("r", r)
                .add("shx", shx)
                .add("shy", shy)
                .add("weight", weight)
                .add("matrix", matrix)
                .toString();
    }
}
