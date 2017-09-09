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
package iterator.model.functions;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

import iterator.model.Function;

/**
 * Identity Co-ordinate Transform.
 */
public class Identity implements Function {

    private int id;
    private int sw;
    private int sh;

    private Identity() {
        this(-1);
    }

    private Identity(int id) {
        this.id = id;
    }

    public static Identity create() {
        return new Identity();
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
        sw = size.width;
        sh = size.height;
    }

    @Override
    public AffineTransform getTransform() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Point2D apply(Point2D src) {
        return src;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Identity)) return false;
        Identity that = (Identity) object;
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