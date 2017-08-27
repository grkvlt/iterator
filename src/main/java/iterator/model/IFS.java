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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ForwardingList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

/**
 * IFS Model.
 * <p>
 * The system consists of {@link Function functions}, which can be either
 * {@link Transform transforms} or {@link Reflection reflections}. The
 * IFS can be used as a {@link List list} of both, or they can be
 * accessed with the {@link #getTransforms()} or {@link #getReflections()}
 * methods.
 * <p>
 * The IFS and its functions are serialisable as XML using JAXB, via th
 * {@link #save(IFS, java.io.File)} and {@link #load(java.io.File)}
 * methods.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "IFS")
public class IFS extends ForwardingList<Function> {

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

    public static final Comparator<Function> IDENTITY = new Comparator<Function>() {
        @Override
        public int compare(Function left, Function right) {
            return ComparisonChain.start()
                    .compare(left.getId(), right.getId())
                    .result();
        }
    };

    public static void save(IFS ifs, File file) {
        try (FileWriter writer = new FileWriter(file)) {
            JAXBContext context = JAXBContext.newInstance(IFS.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(ifs, writer);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static IFS load(File file) {
        try (FileReader reader = new FileReader(file)) {
            JAXBContext context = JAXBContext.newInstance(IFS.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            IFS ifs = (IFS) unmarshaller.unmarshal(reader);
            return ifs;
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @XmlAttribute
    private String name;
    @XmlElementWrapper(name = "Transforms")
    @XmlElement(name = "Transform")
    private List<Transform> transforms = Lists.newLinkedList();
    @XmlElementWrapper(name = "Reflections")
    @XmlElement(name = "Reflection")
    private List<Reflection> reflections = Lists.newLinkedList();

    public IFS() { }

    public boolean add(Transform element) {
        if (element.getId() < 0) {
            element.setId(transforms.isEmpty() ? 1 : Ordering.from(IDENTITY).max(transforms).getId() + 1);
            element.setZIndex(transforms.isEmpty() ? 0 : Ordering.from(Z_ORDER).max(transforms).getZIndex() + 1);
        }

        transforms.remove(element);
        return transforms.add(element);
    }

    public boolean add(Reflection element) {
        if (element.getId() < 0) {
            element.setId(reflections.isEmpty() ? 1 : Ordering.from(IDENTITY).max(reflections).getId() + 1);
        }

        reflections.remove(element);
        return reflections.add(element);
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

    public void setReflections(Collection<Reflection> reflections) {
        this.reflections.clear();
        this.reflections.addAll(reflections);
    }

    public List<Transform> getTransforms() {
        return transforms;
    }

    public List<Reflection> getReflections() {
        return reflections;
    }

    public void setSize(Dimension size) {
        for (Function f : this) {
            f.setSize(size);
        }
    }

    @Override
    protected List<Function> delegate() {
        return FluentIterable.from(Iterables.concat(transforms, reflections)).toList();
    }

    @Override
    public String toString() {
        StringBuilder td = new StringBuilder("[");
        if (!transforms.isEmpty()) {
            td.append("\n\t");
            Joiner.on(",\n\t").appendTo(td, transforms);
            td.append("\n");
        }
        td.append("]");

        StringBuilder rd = new StringBuilder("[");
        if (!reflections.isEmpty()) {
            rd.append("\n\t");
            Joiner.on(",\n\t").appendTo(rd, reflections);
            rd.append("\n");
        }
        rd.append("]");

        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("transforms", td.toString())
                .add("reflections", rd.toString())
                .omitNullValues()
                .toString();
    }
}
