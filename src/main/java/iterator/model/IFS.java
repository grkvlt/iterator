/*
 * Copyright 2012 by adk; All Rights Reserved
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Iterator 
 *
 * @since Apr 1, 2012
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "IFS")
public class IFS {
    public static final Logger LOG = LoggerFactory.getLogger(IFS.class);
    
    public static final String UNTITLED = "Untitled";
    
    private static final Comparator<Transform> Z_ORDER = new Comparator<Transform>() {
        @Override
        public int compare(Transform left, Transform right) {
            return ComparisonChain.start()
                    .compare(left.getZIndex(), right.getZIndex())
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
        addTransform(transform);
        return transform;
    }
}
