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
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

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
                    .compare(left.getId(), right.getId())
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

    public void setSize(Dimension size) {
        for (Transform t : transforms) {
            t.setSize(size);
        }
    }

    public IFS() { }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Transform> getTransforms() {
        Collections.sort(transforms, Z_ORDER);
        return transforms;
    }

    public void setTransforms(Collection<Transform> transforms) {
        this.transforms.clear();
        this.transforms.addAll(transforms);
    }

    public void addTransform(Transform transform) {
        this.transforms.add(transform);
    }

    public void deleteTransform(Transform transform) {
        this.transforms.remove(transform);
    }

    public Transform newTransform(Dimension size) {
        int id = Iterables.size(transforms) + 1;
        int zIndex = 0;
        if (!transforms.isEmpty()) {
            Transform last = Iterables.getLast(transforms);
            zIndex = last.getZIndex() + 1;
        }
        Transform transform = new Transform(id, zIndex, size);
        return transform;
    }

    @Override
    protected List<Transform> delegate() {
        return transforms;
    }
}
