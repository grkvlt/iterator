/*
 * Copyright 2012-2013 by Andrew Kennedy.
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
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Objects;

/**
 * Transform Model.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Transform")
public class Transform {
    @XmlAttribute
    private int id;
    @XmlAttribute
    private int zIndex;
    @XmlAttribute(required = false)
    public Double x;
    @XmlAttribute(required = false)
    public Double y;
    @XmlAttribute(required = false)
    public Double w;
    @XmlAttribute(required = false)
    public Double h;
    @XmlAttribute(required = false)
    public Double r;
    @XmlAttribute
    private double sw;
    @XmlAttribute
    private double sh;
    @XmlAttribute(required = false)
    private double matrix[] = null;

    @SuppressWarnings("unused")
    private Transform() {
        // JAXB
    }

    public Transform(Dimension size) {
        this(-1, 0, size);
    }

    public Transform(int id, int zIndex, Dimension size) {
        this.id = id;
        this.zIndex = zIndex;
        this.sw = size.getWidth();
        this.sh = size.getHeight();
        this.x = 0d;
        this.y = 0d;
        this.w = 0d;
        this.h = 0d;
        this.r = 0d;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getZIndex() {
        return this.zIndex;
    }

    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }

    public void setMatrix(double[] matrix) {
        this.matrix = matrix;
        this.x = null;
        this.y = null;
        this.w = null;
        this.h = null;
        this.r = null;
    }

    public boolean isMatrix() { return matrix != null; }

    public double getDeterminant() {
        return this.getTransform().getDeterminant();
    }

    public void setSize(Dimension size) {
        Point2D scale = new Point2D.Double(size.getWidth() / sw, size.getHeight()/ sh);
        sw = size.getWidth();
        sh = size.getHeight();
        
        if (isMatrix()) {
            AffineTransform transform = getTransform();
            transform.scale(scale.getX(), scale.getY());
            double scaled[] = new double[6];
            transform.getMatrix(scaled);
            matrix = scaled;
        } else {
            w *= scale.getX();
            h *= scale.getY();
            x *= scale.getX();
            y *= scale.getY();
        }
    }

    public AffineTransform getTransform() {
        AffineTransform transform = new AffineTransform();
        if (isMatrix()) {
            transform.setTransform(matrix[0], matrix[1], matrix[2], matrix[3], matrix[4], matrix[5]);
        } else {
            transform.translate(x, y);
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

    public double[] applyTransform(double xin, double yin) {
        AffineTransform transform = getTransform();
        double src[] = new double[] { xin, yin };
        double dst[] = new double[2];
        transform.transform(src, 0, dst, 0, 1);
        return dst;
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
        return Objects.toStringHelper(this)
                .omitNullValues()
                .add("id", id)
                .add("zIndex", zIndex)
                .add("sw", sw)
                .add("sh", sh)
                .add("x", x)
                .add("y", y)
                .add("w", w)
                .add("h", h)
                .add("r", r)
                .add("matrix", matrix)
                .toString();
    }
}
