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
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

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
@XmlRootElement(name = "Reflection")
public class Reflection implements Function {

    @XmlAttribute(required = true)
    private int id;
    @XmlAttribute
    public Integer x;
    @XmlAttribute
    public Integer y;
    @XmlAttribute
    public Double r;
    @XmlAttribute(required = true)
    private int sw;
    @XmlAttribute(required = true)
    private int sh;

    private Reflection() {
        // JAXB
    }

    private Reflection(Dimension size) {
        this(-1, size);
    }

    private Reflection(int id, Dimension size) {
        this.id = id;
        this.sw = size.width;
        this.sh = size.height;
        this.x = 0;
        this.y = 0;
        this.r = 0d;
    }

    public static Reflection create(Dimension size) {
        return new Reflection(size);
    }

    public static Reflection copy(Reflection original) {
        Reflection copy = new Reflection(original.getSize());
        copy.duplicate(original);
        return copy;
    }

    public void duplicate(Reflection original) {
        this.x = original.x;
        this.y = original.y;
        this.r = original.r;
    }

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
        this.id = id;
    }

    @Override
    public void setSize(Dimension size) {
        Point2D scale = new Point2D.Double(size.getWidth() / sw, size.getHeight() / sh);
        sw = size.width;
        sh = size.height;

        x = (int) (scale.getX() * x);
        y = (int) (scale.getY() * y);
    }

    @Override
    public AffineTransform getTransform() {
        AffineTransform transform = AffineTransform.getTranslateInstance(x, y);
        transform.rotate(-r);
        transform.scale(-1d, 1d);
        transform.rotate(r);
        transform.translate(-x, -y);

        return transform;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Reflection)) return false;
        Reflection that = (Reflection) object;
        return Objects.equal(id, that.id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("id", id)
                .add("x", x)
                .add("y", y)
                .add("r", r)
                .toString();
    }
}
