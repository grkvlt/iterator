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
    @XmlAttribute
    public int x;
    @XmlAttribute
    public int y;
    @XmlAttribute
    public int w;
    @XmlAttribute
    public int h;
    @XmlAttribute
    public double r;
    @XmlAttribute
    private int sw;
    @XmlAttribute
    private int sh;

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
        this.sw = size.width;
        this.sh = size.height;
        this.x = 0;
        this.y = 0;
        this.w = 0;
        this.h = 0;
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

    public double getDeterminant() {
        return this.getTransform().getDeterminant();
    }

    public void setSize(Dimension size) {
        this.w = (int) ((double) w * (size.getWidth() / (double) sw));
        this.h = (int) ((double) h * (size.getHeight()/ (double) sh));
        this.x = (int) ((double) x * (size.getWidth() / (double) sw));
        this.y = (int) ((double) y * (size.getHeight() / (double) sh));
        this.sw = size.width;
        this.sh = size.height;
    }

    public AffineTransform getTransform() {
        AffineTransform transform = new AffineTransform();
        transform.translate(x, y);
        transform.rotate(r);
        transform.scale((double) w / (double) sw, (double) h / (double) sh);
        return transform;
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
}
