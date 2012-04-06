/*
 * Copyright 2012 by Andrew Kennedy; All Rights Reserved
 */
package iterator.model;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transform 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "Transform")
public class Transform {
    public static final Logger LOG = LoggerFactory.getLogger(Transform.class);

    @XmlAttribute
    private int id;
    @XmlAttribute
    private int zIndex;
    @XmlAttribute
    private float weight;
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
    
    private Transform() { }
    
    public Transform(int id, int zIndex, Dimension size) {
        this.id = id;
        this.zIndex = zIndex;
        this.weight = 1f;
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

    public AffineTransform getTransform() {
        AffineTransform transform = new AffineTransform();
        transform.translate(x, y);
        transform.rotate(r);
        transform.scale((float) w / (float) sw, (float) h / (float) sh);
        return transform;
    }

    public int getZIndex() {
        return this.zIndex;
    }

    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }

    public float getWeight() {
        return this.weight;
    }
    
    public void setWeight(float weight) {
        this.weight = weight;
    }
    
    public void setSize(Dimension size) {
        this.w = (int) (((float) w / (float) sw) * size.getWidth());
        this.h = (int) (((float) h / (float) sh) * size.getHeight());
        this.sw = size.width;
        this.sh = size.height;
    } 
}
