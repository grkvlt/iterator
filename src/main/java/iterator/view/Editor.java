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
package iterator.view;

import static iterator.util.Messages.MENU_EDITOR_NEW_REFLECTION;
import static iterator.util.Messages.MENU_EDITOR_NEW_TRANSFORM;
import static iterator.util.Messages.MENU_TRANSFORM_BACK;
import static iterator.util.Messages.MENU_TRANSFORM_DELETE;
import static iterator.util.Messages.MENU_TRANSFORM_DUPLICATE;
import static iterator.util.Messages.MENU_TRANSFORM_FRONT;
import static iterator.util.Messages.MENU_TRANSFORM_LOWER;
import static iterator.util.Messages.MENU_TRANSFORM_MATRIX;
import static iterator.util.Messages.MENU_TRANSFORM_PROPERTIES;
import static iterator.util.Messages.MENU_TRANSFORM_RAISE;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import com.google.common.base.Throwables;
import com.google.common.collect.Ordering;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import iterator.Explorer;
import iterator.dialog.Matrix;
import iterator.dialog.Properties;
import iterator.model.IFS;
import iterator.model.Reflection;
import iterator.model.Transform;
import iterator.util.Messages;
import iterator.util.Subscriber;
import iterator.util.Utils;

/**
 * IFS Editor.
 */
public class Editor extends JPanel implements MouseInputListener, KeyListener, Subscriber {
    /** serialVersionUID */
    private static final long serialVersionUID = -1;

    private final EventBus bus;
    private final Explorer controller;
    private final Messages messages;

    private JPopupMenu transform, editor;
    private Action properties;

    private IFS ifs;
    private Reflection reflection;
    private Transform selected;
    private Transform resize, move, rotate;
    private Point start, end;
    private Cursor corner;

    @SuppressWarnings("serial")
    public Editor(Explorer controller) {
        super();

        this.controller = controller;
        this.bus = controller.getEventBus();
        this.messages = controller.getMessages();

        transform = new JPopupMenu();
        properties = new AbstractAction(messages.getText(MENU_TRANSFORM_PROPERTIES)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Properties properties = new Properties(getSelected(), ifs, controller);
                properties.showDialog();
                properties.dispose();
            }
        };
        transform.add(properties);
        transform.add(new AbstractAction(messages.getText(MENU_TRANSFORM_MATRIX)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Matrix matrix = new Matrix(getSelected(), ifs, controller);
                matrix.showDialog();
                matrix.dispose();
            }
        });
        transform.add(new AbstractAction(messages.getText(MENU_TRANSFORM_DELETE)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                ifs.remove(selected);
                selected = null;
                bus.post(ifs);
            }
        });
        transform.add(new AbstractAction(messages.getText(MENU_TRANSFORM_DUPLICATE)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Transform copy = Transform.create(getSize());
                if (selected.isMatrix()) {
                    double[] matrix = new double[6];
                    AffineTransform tmp = selected.getTransform();
                    tmp.translate(controller.getMinGrid(), controller.getMinGrid());
                    tmp.getMatrix(matrix);
                    copy.setMatrix(matrix);
                } else {
                    copy.duplicate(selected);
                    copy.x += controller.getMinGrid();
                    copy.y += controller.getMinGrid();
                }
                ifs.add(copy);
                selected = copy;
                bus.post(ifs);
            }
        });
        JMenuItem separator = new JMenuItem("-");
        separator.setEnabled(false);
        transform.add(separator);
        transform.add(new AbstractAction(messages.getText(MENU_TRANSFORM_RAISE)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                selected.setZIndex(selected.getZIndex() + 1);
                bus.post(ifs);
            }
        });
        transform.add(new AbstractAction(messages.getText(MENU_TRANSFORM_LOWER)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                selected.setZIndex(selected.getZIndex() - 1);
                bus.post(ifs);
            }
        });
        transform.add(new AbstractAction(messages.getText(MENU_TRANSFORM_FRONT)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                selected.setZIndex(Ordering.from(IFS.Z_ORDER).max(ifs).getZIndex() + 1);
                bus.post(ifs);
            }
        });
        transform.add(new AbstractAction(messages.getText(MENU_TRANSFORM_BACK)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                selected.setZIndex(Ordering.from(IFS.Z_ORDER).min(ifs).getZIndex() - 1);
                bus.post(ifs);
            }
        });
        add(transform);

        editor = new JPopupMenu();
        editor.add(new AbstractAction(messages.getText(MENU_EDITOR_NEW_TRANSFORM)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Transform t = Transform.create(getSize());
                double side = getSize().getWidth() / 4d;
                double origin = (getSize().getWidth() - side) / 2d;
                t.x = origin;
                t.y = origin;
                t.w = side;
                t.h = side;
                t.r = 0d;
                ifs.add(t);
                selected = t;
                bus.post(ifs);
            }
        });
        editor.add(new AbstractAction(messages.getText(MENU_EDITOR_NEW_REFLECTION)) {
            @Override
            public void actionPerformed(ActionEvent e) {
                Reflection r = Reflection.create(getSize());
                double origin = getSize().getWidth() / 2d;
                r.x = origin;
                r.y = origin;
                r.r = 0d;
                ifs.getReflections().add(r);
                reflection = r;
                bus.post(ifs);
            }
        });
        add(editor);

        addMouseListener(this);
        addMouseMotionListener(this);

        bus.register(this);
    }

    public Transform getSelected() { return selected; }

    public Point getStart() { return start; }

    public Point getEnd() { return end; }

    public Transform getAnts() {
        Transform ants = null;
        Point start = controller.getEditor().getStart();
        Point end = controller.getEditor().getEnd();
        if (selected == null && start != null && end != null) {
            double x = Math.min(start.x, end.x);
            double y = Math.min(start.y, end.y);
            double w = Math.max(start.x, end.x) - x;
            double h = Math.max(start.y, end.y) - y;

            int grid = controller.getMinGrid();
            w = Math.max(grid, w);
            h = Math.max(grid, h);

            ants = Transform.create(Integer.MIN_VALUE, 0, getSize());
            ants.x = x;
            ants.y = y;
            ants.w = w;
            ants.h = h;
        }
        return ants;
    }

    public List<Transform> getTransforms() {
        Transform selected = controller.getEditor().getSelected();
        Transform ants = getAnts();

        if (ifs.contains(selected)) { selected = null; }
        List<Transform> transforms = Utils.concatenate(ifs, selected, ants);
        Collections.sort(transforms, IFS.IDENTITY);
        return transforms;
    }

    public double getWeight(List<Transform> transforms) {
        return transforms.stream().map(Transform::getWeight).reduce(0d, Double::sum);
    }

    public double getArea(List<Transform> transforms) {
        return transforms.stream().map(t -> t.getWidth() * t.getHeight()).reduce(0d, Double::sum);
    }

    public double getWidth(List<Transform> transforms) {
        double min = transforms.stream().map(Transform::getTranslateX).reduce(0d, Double::min);
        double max = transforms.stream().map(Transform::getTranslateX).reduce(0d, Double::max);
        return max - min;
    }

    public double getHeight(List<Transform> transforms) {
        double min = transforms.stream().map(Transform::getTranslateY).reduce(0d, Double::min);
        double max = transforms.stream().map(Transform::getTranslateY).reduce(0d, Double::max);
        return max - min;
    }

    /** @see Subscriber#updated(IFS) */
    @Override
    @Subscribe
    public void updated(IFS ifs) {
        this.ifs = ifs;
        start = null;
        end = null;
        resize = null;
        move = null;
        rotate = null;
        if (!ifs.contains(selected)) {
            selected = null;
        }
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        repaint();
    }

    /** @see Subscriber#resized(Dimension) */
    @Override
    @Subscribe
    public void resized(Dimension size) {
        ifs.setSize(size);
        bus.post(ifs);
    }

    /** @see javax.swing.JComponent#paintComponent(Graphics) */
    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            paintGrid(g);

            if (ifs != null) {
                for (Transform t : ifs) {
                    if (!t.equals(selected)) {
                        paintTransform(t, false, g);
                    }
                }
                if (selected != null) {
                    paintTransform(selected, true, g);
                }

                for (Reflection r : ifs.getReflections()) {
                    if (!r.equals(reflection)) {
                        paintReflection(r, false, g);
                    }
                }
                if (reflection != null) {
                    paintReflection(reflection, true, g);
                }

                if (selected == null && start != null && end != null) {
                    g.setPaint(Color.BLACK);
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 5f, 5f }, 0f));
                    Transform ants = getAnts();
                    g.draw(new Rectangle(ants.x.intValue(), ants.y.intValue(), ants.w.intValue(), ants.h.intValue()));
                }

                if (!ifs.isEmpty()) {
                    Viewer viewer = controller.getViewer();
                    viewer.reset();
                    List<Transform> transforms = controller.getEditor().getTransforms();
                    double areaRatio = controller.getEditor().getArea(transforms) / (getWidth() * getHeight());
                    double sizeRatio = (controller.getEditor().getWidth(transforms) * controller.getEditor().getHeight(transforms)) / (getWidth() * getHeight());
                    int k = (int) (1_000_000 * areaRatio * sizeRatio);
                    viewer.iterate(k, 1.0f, new Point2D.Double(getWidth() / 2d, getHeight() / 2d));

                    g.setComposite(AlphaComposite.SrcOver.derive(0.8f));
                    g.drawImage(viewer.getImage(), new AffineTransformOp(new AffineTransform(), AffineTransformOp.TYPE_BILINEAR), 0, 0);
                }
            }
        } catch (Exception e) {
            controller.error(e, "Failure painting IFS editor");
        } finally {
            g.dispose();
        }
;
    }

    public void paintTransform(Transform t, boolean highlight, Graphics2D graphics) {
        Rectangle unit = new Rectangle(getSize());
        Shape rect = t.getTransform().createTransformedShape(unit);
        Graphics2D g = (Graphics2D) graphics.create();

        try {
            // Draw the outline
            g.setPaint(Color.BLACK);
            if (highlight) {
                g.setStroke(new BasicStroke(2f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER, 10.0f, new float[] { 10f, 10f }, 0f));
            } else {
                g.setStroke(new BasicStroke(2f));
            }
            g.draw(rect);

            // Fill the rectangle
            if (highlight) {
                if (t.isMatrix()) {
                    g.setPaint(new Color(Color.GREEN.getRed(), Color.GREEN.getGreen(), Color.GREEN.getBlue(), 16));
                } else {
                    g.setPaint(new Color(Color.BLUE.getRed(), Color.BLUE.getGreen(), Color.BLUE.getBlue(), 16));
                }
            } else {
                g.setPaint(new Color(Color.GRAY.getRed(), Color.GRAY.getGreen(), Color.GRAY.getBlue(), 8));
            }
            g.fill(rect);

            if (!t.isMatrix()) {
                // Draw the resize handles
                g.setStroke(new BasicStroke(2f));
                g.setPaint(Color.BLACK);
                int[] cornerX = new int[] { 0, 0, getWidth(), getWidth() };
                int[] cornerY = new int[] { 0, getHeight(), getHeight(), 0 };
                for (int i = 0; i < 4; i++) {
                    Point center = new Point();
                    t.getTransform().transform(new Point(cornerX[i], cornerY[i]), center);
                    Rectangle corner = new Rectangle(center.x - 4, center.y - 4, 8, 8);
                    g.fill(corner);
                }

                // And rotate handle
                int rotateX = getWidth() / 2, rotateY = 0;
                Point center = new Point();
                t.getTransform().transform(new Point(rotateX, rotateY), center);
                Arc2D handle = new Arc2D.Double(center.getX() - 6d, center.getY() - 6d, 12d, 12d, 0d, 360d, Arc2D.OPEN);
                g.draw(handle);
            }

            // Draw the number
            paintTransformNumber(t, highlight, g);
        } catch (Exception e) {
            controller.error(e, "Failure painting transform");
        } finally {
            g.dispose();
        }
    }

    public void paintTransformNumber(Transform t, boolean highlight, Graphics2D graphics) {
        Graphics2D g = (Graphics2D) graphics.create();

        try {
            if (highlight) {
                g.setPaint(Color.BLACK);
            } else {
                g.setPaint(new Color(Color.BLACK.getRed(), Color.BLACK.getGreen(), Color.BLACK.getBlue(), 128));
            }
            g.setFont(new Font("Calibri", Font.BOLD, 25));
            Point text = new Point();
            t.getTransform().transform(new Point(0, 0), text);
            AffineTransform rotation = new AffineTransform();
            if (t.isMatrix()) {
                rotation = t.getTransform();
                rotation.scale(1 / t.getScaleX(), 1 / t.getScaleY());
                rotation.translate(-t.getTranslateX(), -t.getTranslateY());
            } else {
                rotation.translate(text.x, text.y);
                rotation.shear(t.shx, t.shy);
                rotation.rotate(t.r);
                rotation.translate(-text.x, -text.y);
            }
            g.setTransform(rotation);
            String id = String.format("TR%s %.1f%% %s",
                    (t.getId() == -1 ? "--" : String.format("%02d", t.getId())),
                    100d * t.getWeight() / getWeight(Utils.concatenate(ifs, ifs.contains(selected) ? null : selected)),
                    ((highlight && rotate != null) ? String.format("(%+d)", (int) Math.toDegrees(t.r)) : ""));
            g.drawString(id, text.x + 5, text.y + 25);
        } catch (Exception e) {
            controller.error(e, "Failure painting transform number");
        } finally {
            g.dispose();
        }
    }

    public void paintReflection(Reflection r, boolean highlight, Graphics2D graphics) {
        
    }

    public void paintGrid(Graphics2D graphics) {
        Graphics2D g = (Graphics2D) graphics.create();

        try {
            int min = controller.getMinGrid();
            int max = controller.getMaxGrid();
            Rectangle s = new Rectangle(getSize());
            g.setPaint(Color.WHITE);
            g.fill(s);
            g.setPaint(Color.LIGHT_GRAY);
            g.setStroke(new BasicStroke(1f));
            for (int x = 0; x < getWidth(); x += min) {
                g.drawLine(x, 0, x, getHeight());
            }
            for (int y = 0; y < getHeight(); y += min) {
                g.drawLine(0, y, getWidth(), y);
            }
            g.setPaint(Color.GRAY);
            g.setStroke(new BasicStroke(2f));
            for (int x = 0; x < getWidth(); x += max) {
                g.drawLine(x, 0, x, getHeight());
            }
            for (int y = 0; y < getHeight(); y += max) {
                g.drawLine(0, y, getWidth(), y);
            }
        } catch (Exception e) {
            controller.error(e, "Failure painting grid");
        } finally {
            g.dispose();
        }
    }

    public Transform getTransformAt(int x, int y) {
        Point point = new Point(x, y);
        return getTransformAt(point);
    }

    public Transform getTransformAt(Point point) {
        for (Transform t : Ordering.from(IFS.Z_ORDER).reverse().immutableSortedCopy(ifs)) {
            Shape box = t.getTransform().createTransformedShape(new Rectangle(0, 0, getWidth(), getHeight()));
            if (box.contains(point) || isResize(t, point) || isRotate(t, point)) {
                return t;
            }
        }
        return null;
    }

    public boolean isRotate(Transform t, Point point) {
        int rotateX = getWidth() / 2, rotateY = 0;
        Point center = new Point();
        t.getTransform().transform(new Point(rotateX, rotateY), center);
        Arc2D handle = new Arc2D.Double(center.getX() - 6d, center.getY() - 6d, 12d, 12d, 0d, 360d, Arc2D.OPEN);
        return handle.contains(point);
    }

    public boolean isResize(Transform t, Point point) {
        return getCorner(t, point) != null;
    }

    public Cursor getCorner(Transform t, Point point) {
        int[] cornerX = new int[] { 0, 0, getWidth(), getWidth() };
        int[] cornerY = new int[] { 0, getHeight(), getHeight(), 0 };
        int[] cursors = new int[] { Cursor.NW_RESIZE_CURSOR, Cursor.SW_RESIZE_CURSOR, Cursor.SE_RESIZE_CURSOR, Cursor.NE_RESIZE_CURSOR };
        for (int i = 0; i < 4; i++) {
            Point center = new Point(cornerX[i], cornerY[i]);
            t.getTransform().transform(center, center);
            Rectangle corner = new Rectangle(center.x - 5, center.y - 5, 10, 10);
            if (corner.contains(point)) {
                return new Cursor(cursors[i]);
            }
        }
        return null;
    }

    /** @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent) */
    @Override
    public void mouseClicked(MouseEvent e) {
    }

    /** @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent) */
    @Override
    public void mousePressed(MouseEvent e) {
        Transform clicked = getTransformAt(e.getPoint());
        if (SwingUtilities.isLeftMouseButton(e)) {
            resize = null;
            for (Transform t : ifs) {
                if (t.isMatrix()) continue;
                if (isResize(t, e.getPoint())) {
                    resize = t;
                    break;
                }
            }
            if (resize != null) {
                corner = getCorner(resize, e.getPoint());
                setCursor(corner);
                Point nw = new Point(0, 0);
                Point ne = new Point(getWidth(), 0);
                Point sw = new Point(0, getHeight());
                Point se = new Point(getWidth(), getHeight());
                resize.getTransform().transform(nw, nw);
                resize.getTransform().transform(ne, ne);
                resize.getTransform().transform(sw, sw);
                resize.getTransform().transform(se, se);
                switch (corner.getType()) {
                    case Cursor.NW_RESIZE_CURSOR: start = nw; break;
                    case Cursor.NE_RESIZE_CURSOR: start = ne; break;
                    case Cursor.SW_RESIZE_CURSOR: start = sw; break;
                    case Cursor.SE_RESIZE_CURSOR: start = se; break;
                }
                ifs.remove(resize);
                selected = resize;
            } else if (clicked != null) {
                if (!clicked.isMatrix() && isRotate(clicked, e.getPoint())) {
                    selected = clicked;
                    rotate = selected;
                    ifs.remove(rotate);
                    start = snap(e.getPoint());
                    setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
                } else {
                    selected = clicked;
                    if (e.isAltDown()) {
                        Transform copy = Transform.copy(selected);
                        selected = copy;
                    }
                    move = selected;
                    setCursor(new Cursor(Cursor.MOVE_CURSOR));
                    start = snap(e.getPoint());
                    ifs.remove(selected);
                }
            } else {
                start = snap(e.getPoint());
                selected = null;
                setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
            }
            end = new Point(start.x, start.y);
        } else if (SwingUtilities.isRightMouseButton(e)) {
            if (clicked != null) {
                selected = clicked;
                properties.setEnabled(!selected.isMatrix());
                transform.show(e.getComponent(), e.getX(), e.getY());
            } else {
                editor.show(e.getComponent(), e.getX(), e.getY());
            }
        }
        repaint();
    }

    private Point snap(Point point) {
        int grid = controller.getSnapGrid();
        return new Point(grid * (point.x / grid), grid * (point.y / grid));
    }

    /** @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent) */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (selected == null && start != null && end != null) {
                double x = Math.min(start.x, end.x);
                double y = Math.min(start.y, end.y);
                double w = Math.max(start.x, end.x) - x;
                double h = Math.max(start.y, end.y) - y;

                start = null;
                end = null;

                int grid = controller.getSnapGrid();
                if (w <= grid && h <= grid) {
                    repaint();
                    return;
                }

                selected = Transform.create(getSize());
                selected.x = x;
                selected.y = y;
                selected.w = w;
                selected.h = h;
                ifs.add(selected);
                bus.post(ifs);
            } else if (selected != null  && start != null && end != null) {
                ifs.add(selected);
                bus.post(ifs);
            }
        }
    }

    /** @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent) */
    @Override
    public void mouseEntered(MouseEvent e) {
    }

    /** @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent) */
    @Override
    public void mouseExited(MouseEvent e) {
    }

    /** @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent) */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (start != null) {
            end = snap(e.getPoint());
            if (selected  == null) {
                if (e.isShiftDown()) {
                    int dx = end.x - start.x;
                    int dy = end.y - start.y;
                    end.y = start.y + (int) Math.copySign(dx, dy);
                }
            } else if (selected != null && resize != null) {
                double w = resize.w;
                double h = resize.h;
                int dx = end.x - start.x;
                int dy = end.y - start.y;

                Point delta = new Point(dx, dy);
                AffineTransform reverse = new AffineTransform();
                reverse.rotate(-resize.r);
                reverse.shear(-resize.shx, -resize.shy);
                reverse.transform(delta, delta);

                if (e.isShiftDown()) {
                    delta.x = (int) (delta.y * (w / h));
                }
                Point inverseX = new Point(delta.x, 0);
                Point inverseY = new Point(0, delta.y);
                try {
                    reverse.inverseTransform(inverseX, inverseX);
                    reverse.inverseTransform(inverseY, inverseY);
                } catch (NoninvertibleTransformException e1) {
                    Throwables.propagate(e1);
                }

                double x = resize.x;
                double y = resize.y;

                switch(corner.getType()) {
                    case Cursor.NW_RESIZE_CURSOR:
                        x += (inverseX.x + inverseY.x);
                        y += (inverseX.y + inverseY.y);
                        w -= delta.x;
                        h -= delta.y;
                        break;
                    case Cursor.NE_RESIZE_CURSOR:
                        x += inverseY.x;
                        y += inverseY.y;
                        if (e.isShiftDown()) {
                            w -= delta.x;
                        } else {
                            w += delta.x;
                        }
                        h -= delta.y;
                        break;
                    case Cursor.SW_RESIZE_CURSOR:
                        if (e.isShiftDown()) {
                            x -= inverseX.x;
                            y -= inverseX.y;
                            w += delta.x;
                        } else {
                            x += inverseX.x;
                            y += inverseX.y;
                            w -= delta.x;
                        }
                        h += delta.y;
                        break;
                    case Cursor.SE_RESIZE_CURSOR:
                        w += delta.x;
                        h += delta.y;
                        break;
                }

                int grid = controller.getSnapGrid();
                w = Math.max(grid, w);
                h = Math.max(grid, h);

                selected = Transform.clone(selected);
                selected.duplicate(resize);
                selected.x = x;
                selected.y = y;
                selected.w = w;
                selected.h = h;
            } else if (selected != null && move != null) {
                int dx = end.x - start.x;
                int dy = end.y - start.y;

                selected = Transform.clone(selected);
                if (move.isMatrix()) {
                    AffineTransform moved = AffineTransform.getTranslateInstance(dx, dy);
                    moved.concatenate(move.getTransform());
                    double matrix[] = new double[6];
                    moved.getMatrix(matrix);
                    selected.setMatrix(matrix);
                } else {
                    double x = move.x + dx;
                    double y = move.y + dy;

                    selected.duplicate(move);
                    selected.x = x;
                    selected.y = y;
                }
            } else if (selected != null && rotate != null) {
                Point origin = new Point();
                rotate.getTransform().transform(new Point(0, 0), origin);
                int dx = end.x - origin.x;
                int dy = end.y - origin.y;
                double r = Math.atan2(dy - (selected.shy * dx), dx - (selected.shx * dy));

                selected = Transform.clone(selected);
                selected.duplicate(rotate);
                selected.r = r;
            }
            repaint();
        }
    }

    /** @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent) */
    @Override
    public void mouseMoved(MouseEvent e) {
        if (start == null) {
            for (Transform t : ifs) {
                Cursor corner = getCorner(t, e.getPoint());
                if (corner != null) {
                    setCursor(corner);
                    return;
                }
            }
            if (getTransformAt(e.getPoint()) != null) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            } else {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    /** @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent) */
    @Override
    public void keyTyped(KeyEvent e) {
    }

    /** @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent) */
    @Override
    public void keyPressed(KeyEvent e) {
        if (isVisible()) {
            if (selected != null) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DELETE:
                    case KeyEvent.VK_BACK_SPACE:
                        ifs.remove(selected);
                        selected = null;
                        bus.post(ifs);
                        break;
                    case KeyEvent.VK_RIGHT:
                        if (!selected.isMatrix()) {
                            selected.r += Math.PI / 2d;
                            bus.post(ifs);
                        }
                        break;
                    case KeyEvent.VK_LEFT:
                        if (!selected.isMatrix()) {
                            selected.r -= Math.PI / 2d;
                            bus.post(ifs);
                        }
                        break;
                }
            } else {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ESCAPE:
                        if (start != null && end != null) {
                            start = null;
                            end = null;
                            repaint();
                        }
                        break;
                }
            }
        }
    }

    /** @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent) */
    @Override
    public void keyReleased(KeyEvent e) {
    }
}
