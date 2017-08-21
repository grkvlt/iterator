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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

/**
 * IFS Model.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "IFS")
public class IFS extends ForwardingList<Transform> {
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
    private List<Transform> transforms = Lists.newArrayList();

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
            element.setId(isEmpty() ? 1 : Ordering.from(IDENTITY).max(this).getId() + 1);
            element.setZIndex(isEmpty() ? 0 : Ordering.from(Z_ORDER).max(this).getZIndex() + 1);
        }

        return super.add(element);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    protected List<Transform> delegate() {
        return transforms;
    }

    @Override
    public String toString() {
        StringBuilder data = new StringBuilder("[");
        if (!isEmpty()) {
            data.append("\n\t");
            Joiner.on(",\n\t").appendTo(data, transforms);
            data.append("\n");
        }
        data.append("]");

        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("transforms", data.toString())
                .omitNullValues()
                .toString();
    }
}
