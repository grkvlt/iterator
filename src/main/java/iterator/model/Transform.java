/*
 * Copyright 2012 by Andrew Kennedy; All Rights Reserved
 */
package iterator.model;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transform 
 */
public class Transform {
    public static final Logger LOG = LoggerFactory.getLogger(Transform.class);

    private int id;
    private int zIndex;
    private float weight;
    private Dimension size;
    public int x, y;
    public int w, h;
    public double r;
    
    public Transform(int id, int zIndex, Dimension size) {
        this.id = id;
        this.zIndex = zIndex;
        this.weight = 1f;
        this.size = size;
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
        transform.scale(w / size.getWidth(), h / size.getHeight());
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
        this.w = (int) ((w / this.size.getWidth()) * size.getWidth());
        this.h = (int) ((h / this.size.getHeight()) * size.getHeight());
        this.size = size;
    } 
}
