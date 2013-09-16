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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.SortedSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ForwardingSortedSet;
import com.google.common.collect.Sets;

/**
 * IFS Model.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "IFS")
public class IFS extends ForwardingSortedSet<Transform> {
    public static final String UNTITLED = "Untitled";

    public static final Comparator<Transform> Z_ORDER = new Comparator<Transform>() {
        @Override
        public int compare(Transform left, Transform right) {
            return ComparisonChain.start()
                    .compare(left.getZIndex(), right.getZIndex())
                    .compare(left, right, IDENTITY)
                    .result();
        }
    };

    public static final Comparator<Transform> REVERSE_Z_ORDER = Collections.reverseOrder(Z_ORDER);

    public static final Comparator<Transform> IDENTITY = new Comparator<Transform>() {
        @Override
        public int compare(Transform left, Transform right) {
            return ComparisonChain.start()
                    .compare(left.getId(), right.getId())
                    .result();
        }
    };

    @XmlAttribute
    private String name;
    @XmlElementWrapper(name = "Transforms")
    @XmlElement(name = "Transform")
    private SortedSet<Transform> transforms = Sets.newTreeSet(IDENTITY);

    public IFS() { }

    @Override
    public boolean contains(Object object) {
        if (object == null) {
            return false;
        } else {
            return super.contains(object);
        }
    }

    @Override
    public boolean add(Transform element) {
        if (element.getId() < 0) {
            element.setId(size() + 1);
            element.setZIndex(isEmpty() ? 0 : last().getZIndex() + 1);
        }
        return super.add(element);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SortedSet<Transform> getTransforms() {
        return transforms;
    }

    public void setTransforms(Collection<Transform> transforms) {
        this.transforms.clear();
        this.transforms.addAll(transforms);
    }

    public void setSize(Dimension size) {
        for (Transform t : transforms) {
            t.setSize(size);
        }
    }

    public double getWeight() {
        double weight = 0;
        for (Transform t : transforms) weight += t.getDeterminant();
        return weight;
    }

    @Override
    protected SortedSet<Transform> delegate() {
        return transforms;
    }
}
