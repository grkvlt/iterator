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

import static iterator.Utils.DASHED_LINE_2;
import static iterator.Utils.DOTTED_LINE_2;
import static iterator.Utils.PATTERNED_LINE_2;
import static iterator.Utils.SOLID_LINE_1;
import static iterator.Utils.SOLID_LINE_2;
import static iterator.Utils.alpha;
import static iterator.Utils.calibri;
import static iterator.Utils.concatenate;
import static iterator.Utils.context;
import static iterator.Utils.menuItem;
import static iterator.Utils.weight;
import static iterator.util.Messages.MENU_EDITOR_NEW_IFS;
import static iterator.util.Messages.MENU_EDITOR_NEW_REFLECTION;
import static iterator.util.Messages.MENU_EDITOR_NEW_TRANSFORM;
import static iterator.util.Messages.MENU_REFLECTION_DELETE;
import static iterator.util.Messages.MENU_REFLECTION_PROPERTIES;
import static iterator.util.Messages.MENU_TRANSFORM_BACK;
import static iterator.util.Messages.MENU_TRANSFORM_DELETE;
import static iterator.util.Messages.MENU_TRANSFORM_DUPLICATE;
import static iterator.util.Messages.MENU_TRANSFORM_FRONT;
import static iterator.util.Messages.MENU_TRANSFORM_LOWER;
import static iterator.util.Messages.MENU_TRANSFORM_MATRIX;
import static iterator.util.Messages.MENU_TRANSFORM_PROPERTIES;
import static iterator.util.Messages.MENU_TRANSFORM_RAISE;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.MouseInputListener;

import com.google.common.base.Throwables;
import com.google.common.collect.Ordering;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.Ints;

import iterator.Explorer;
import iterator.dialog.Matrix;
import iterator.dialog.Properties;
import iterator.model.IFS;
import iterator.model.Reflection;
import iterator.model.Transform;
import iterator.model.functions.CoordinateTransform;
import iterator.util.Config.Render;
import iterator.util.Dialog;
import iterator.util.Formatter;
import iterator.util.Formatter.DoubleFormatter;
import iterator.util.Messages;
import iterator.util.Subscriber;

/**
 * IFS Editor.
 */
public class Editor extends JPanel implements MouseInputListener, KeyListener, ActionListener, Subscriber {

    private final EventBus bus;
    private final Explorer controller;
    private final Messages messages;

    private JPopupMenu transformMenu, reflectionMenu, editor;
    private JMenuItem properties;

    private Timer timer;
    private BufferedImage image;
    private IFS ifs;
    private Reflection reflection;
    private Transform selected;
    private Transform resize, move, rotate;
    private Point start, end;
    private Corner corner;

    public Editor(Explorer controller) {
        super();

        this.controller = controller;
        this.bus = controller.getEventBus();
        this.messages = controller.getMessages();

        timer = new Timer(100, this);
        timer.setCoalesce(true);

        transformMenu = new JPopupMenu();
        properties = menuItem(messages.getText(MENU_TRANSFORM_PROPERTIES), e -> {
            Dialog.show(Properties.dialog(controller, selected, ifs), controller);
        });
        transformMenu.add(properties);
        transformMenu.add(menuItem(messages.getText(MENU_TRANSFORM_MATRIX), e -> {
            Dialog.show(Matrix.dialog(controller, selected, ifs), controller);
        }));
        transformMenu.add(menuItem(messages.getText(MENU_TRANSFORM_DELETE), e -> {
            ifs.getTransforms().remove(selected);
            selected = null;
            bus.post(ifs);
        }));
        transformMenu.add(menuItem(messages.getText(MENU_TRANSFORM_DUPLICATE), e -> {
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
        }));
        JMenuItem separator = new JMenuItem("-");
        separator.setEnabled(false);
        transformMenu.add(separator);
        transformMenu.add(menuItem(messages.getText(MENU_TRANSFORM_RAISE), e -> {
            selected.setZIndex(selected.getZIndex() + 1);
            bus.post(ifs);
        }));
        transformMenu.add(menuItem(messages.getText(MENU_TRANSFORM_LOWER), e -> {
            selected.setZIndex(selected.getZIndex() - 1);
            bus.post(ifs);
        }));
        transformMenu.add(menuItem(messages.getText(MENU_TRANSFORM_FRONT), e -> {
            selected.setZIndex(Ordering.from(IFS.Z_ORDER).max(ifs.getTransforms()).getZIndex() + 1);
            bus.post(ifs);
        }));
        transformMenu.add(menuItem(messages.getText(MENU_TRANSFORM_BACK), e -> {
            selected.setZIndex(Ordering.from(IFS.Z_ORDER).min(ifs.getTransforms()).getZIndex() - 1);
            bus.post(ifs);
        }));
        add(transformMenu);

        reflectionMenu = new JPopupMenu();
        reflectionMenu.add(menuItem(messages.getText(MENU_REFLECTION_PROPERTIES), e -> {
            Dialog.show(Properties.dialog(controller, reflection, ifs), controller);
        }));
        reflectionMenu.add(menuItem(messages.getText(MENU_REFLECTION_DELETE), e -> {
            ifs.getReflections().remove(reflection);
            reflection = null;
            bus.post(ifs);
        }));
        add(reflectionMenu);

        editor = new JPopupMenu();
        editor.add(menuItem(messages.getText(MENU_EDITOR_NEW_IFS), e -> {
            IFS untitled = new IFS();
            bus.post(untitled);
            resetImage();
        }));
        editor.add(menuItem(messages.getText(MENU_EDITOR_NEW_TRANSFORM), e -> {
            Transform t = Transform.create(getSize());
            double side = getSize().getWidth() / 4d;
            int origin = (getSize().width - (int) side) / 2;
            t.x = origin;
            t.y = origin;
            t.w = side;
            t.h = side;
            t.r = 0d;
            ifs.add(t);
            selected = t;
            bus.post(ifs);
        }));
        editor.add(menuItem(messages.getText(MENU_EDITOR_NEW_REFLECTION), e -> {
            Reflection r = Reflection.create(getSize());
            int origin = getSize().width / 2;
            r.x = origin;
            r.y = origin;
            r.r = 0d;
            ifs.add(r);
            bus.post(ifs);
        }));
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
        if (reflection == null && selected == null && start != null && end != null) {
            int x = Math.min(start.x, end.x);
            int y = Math.min(start.y, end.y);
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

    public List<Reflection> getReflections() {
        return concatenate(ifs.getReflections(), reflection);
    }

    public List<Transform> getTransforms() {
        List<Transform> transforms = concatenate(ifs.getTransforms(), selected, getAnts());
        Collections.sort(transforms, IFS.IDENTITY);
        return transforms;
    }

    /** @see Subscriber#updated(IFS) */
    @Override
    @Subscribe
    public void updated(IFS ifs) {
        this.ifs = ifs;
        this.start = null;
        this.end = null;
        this.resize = null;
        this.move = null;
        this.rotate = null;
        if (!ifs.contains(selected)) {
            this.selected = null;
        }
        this.reflection = null;

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

    /**
     * Invoked when the timer fires, to refresh the image when rendering.
     *
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (isVisible() && ifs.size() > 0) {
            Viewer viewer = controller.getViewer();
            long n = getTransforms().size();
            long m = getReflections().size() + 1;
            long k = Math.min(1_000_000, 50_000 * (long) Math.pow(2d, n * m));
            k *= (controller.getCoordinateTransformType() == CoordinateTransform.Type.IDENTITY ? 1 : 2);
            resetImage();
            viewer.reset();
            viewer.iterate(image, 2, k, 1.0f, new Point2D.Double(getWidth() / 2d, getHeight() / 2d),
                    Render.STANDARD, controller.getMode(), controller.getCoordinateTransform());
            repaint();
        }
    }

    public void start() {
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    public void stop() {
        if (timer.isRunning()) {
            timer.stop();
        }
    }

    public void resetImage() {
        image = new BufferedImage(getSize().width, getSize().height, BufferedImage.TYPE_INT_ARGB);
        context(controller, image.getGraphics(), g -> {
            g.setColor(new Color(1f, 1f, 1f, 0f));
            g.fillRect(0, 0, getSize().width, getSize().height);
        });
    }

    /** @see javax.swing.JComponent#paintComponent(Graphics) */
    @Override
    protected void paintComponent(Graphics graphics) {
        context(controller, graphics, g -> {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            paintGrid(g);

            if (ifs != null) {
                if (ifs.size() > 0) {
                    g.setComposite(AlphaComposite.SrcOver.derive(0.8f));
                    g.drawImage(image, new AffineTransformOp(new AffineTransform(), AffineTransformOp.TYPE_BILINEAR), 0, 0);
                }

                for (Transform t : ifs.getTransforms()) {
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

                Transform ants = getAnts();
                if (ants != null) {
                    g.setPaint(Color.BLACK);
                    g.setStroke(DOTTED_LINE_2);
                    g.draw(new Rectangle(ants.x.intValue(), ants.y.intValue(), ants.w.intValue(), ants.h.intValue()));
                }
            }
        });
    }

    public void paintTransform(Transform t, boolean highlight, Graphics graphics) {
        Rectangle unit = new Rectangle(getSize());
        Shape rect = t.getTransform().createTransformedShape(unit);

        context(controller, graphics, g -> {
            // Draw the outline
            g.setPaint(Color.BLACK);
            g.setStroke(highlight ? DASHED_LINE_2 : SOLID_LINE_2);
            g.draw(rect);

            // Fill the rectangle
            if (highlight) {
                if (t.isMatrix()) {
                    g.setPaint(alpha(Color.GREEN, 16));
                } else {
                    g.setPaint(alpha(Color.BLUE, 16));
                }
            } else {
                g.setPaint(alpha(Color.GRAY, 16));
            }
            g.fill(rect);

            if (!t.isMatrix()) {
                // Draw the resize handles
                g.setStroke(SOLID_LINE_2);
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
        });
    }

    public void paintTransformNumber(Transform t, boolean highlight, Graphics graphics) {
        context(controller, graphics, g -> {
            // Set the position and angle
            Point2D text = new Point2D.Double(t.getTranslateX(), t.getTranslateY());
            AffineTransform rotation = new AffineTransform();
            rotation.translate(text.getX(), text.getY());
            if (t.isMatrix()) {
                Point2D nw = t.getTransform().transform(Corner.NW.getPoint2D(unit()), null);
                Point2D ne = t.getTransform().transform(Corner.NE.getPoint2D(unit()), null);
                double r = Math.atan2(ne.getY() - nw.getY(), ne.getX() - nw.getX());
                rotation.rotate(r); // FIXME Ignores shearing
            } else {
                rotation.shear(t.shx, t.shy);
                rotation.rotate(t.r);
            }
            rotation.translate(-text.getX(), -text.getY());
            g.setTransform(rotation);

            // Draw the label
            DoubleFormatter one = Formatter.doubles(1);
            String id = String.format("T%s %s%% %s",
                    (t.getId() == -1 ? "--" : String.format("%02d", t.getId())),
                    one.toString(100d * t.getWeight() / weight(concatenate(ifs.getTransforms(), selected))),
                    ((highlight && rotate != null) ? String.format("(%s)", one.toString(Math.toDegrees(t.r))) : ""));
            g.setPaint(Color.BLACK);
            g.setFont(calibri(Font.BOLD, 25));
            g.drawString(id, (int) text.getX() + 5, (int) text.getY() + 25);
        });
    }

    public void paintReflection(Reflection r, boolean highlight, Graphics graphics) {
        context(controller, graphics, g -> {
            // Set the line pattern
            g.setStroke(highlight ? DASHED_LINE_2 : PATTERNED_LINE_2);
            g.setPaint(Color.BLACK);

            // Draw the line
            Path2D line = new Path2D.Double(Path2D.WIND_NON_ZERO);
            if ((r.r < Math.toRadians(0.1d) && r.r > Math.toRadians(-0.1d)) ||
                    (r.r < Math.toRadians(180.1d) && r.r > Math.toRadians(179.9d))) {
                line.moveTo(r.x, 0d);
                line.lineTo(r.x, getHeight());
            } else {
                line.moveTo(r.x - 2d * getWidth(), r.y - 2d * (getWidth() / Math.tan(r.r)));
                line.lineTo(r.x + 2d * getWidth(), r.y + 2d * (getWidth() / Math.tan(r.r)));
            }
            g.draw(line);

            // Draw the label
            DoubleFormatter one = Formatter.doubles(1);
            String id = String.format("R%s %s",
                    (r.getId() == -1 ? "--" : String.format("%02d", r.getId())),
                    highlight ? String.format("(%s)", one.toString(Math.toDegrees(r.r))) : "");
            g.setFont(calibri(Font.BOLD, 25));
            g.drawString(id, (int) (r.x + 10), (int) (r.y + 10));

            // Add the select handle
            g.setStroke(SOLID_LINE_2);
            Rectangle handle = new Rectangle((int) (r.x - 4), (int) (r.y - 4), 8, 8);
            g.fill(handle);
        });
    }

    public void paintGrid(Graphics graphics) {
        context(controller, graphics, g -> {
            int min = controller.getMinGrid();
            int max = controller.getMaxGrid();
            Rectangle s = new Rectangle(getSize());
            g.setPaint(Color.WHITE);
            g.fill(s);
            g.setPaint(Color.LIGHT_GRAY);
            g.setStroke(SOLID_LINE_1);
            for (int x = 0; x < getWidth(); x += min) {
                g.drawLine(x, 0, x, getHeight());
            }
            for (int y = 0; y < getHeight(); y += min) {
                g.drawLine(0, y, getWidth(), y);
            }
            g.setPaint(Color.GRAY);
            g.setStroke(SOLID_LINE_2);
            for (int x = 0; x < getWidth(); x += max) {
                g.drawLine(x, 0, x, getHeight());
            }
            for (int y = 0; y < getHeight(); y += max) {
                g.drawLine(0, y, getWidth(), y);
            }
        });
    }

    public Transform getTransformAt(int x, int y) {
        Point point = new Point(x, y);
        return getTransformAt(point);
    }

    public Transform getTransformAt(Point point) {
        for (Transform t : Ordering.from(IFS.Z_ORDER).reverse().immutableSortedCopy(ifs.getTransforms())) {
            Shape box = t.getTransform().createTransformedShape(unit());
            if (box.contains(point)) {
                return t;
            }
        }
        return null;
    }

    public Reflection getReflectionAt(Point point) {
        for (Reflection r : ifs.getReflections()) {
            Shape box = new Rectangle((int) (r.x - 5), (int) (r.y - 5), 10, 10);
            if (box.contains(point)) {
                return r;
            }
        }
        return null;
    }

    public boolean isRotateHandle(Transform t, Point point) {
        int rotateX = getWidth() / 2, rotateY = 0;
        Point center = new Point();
        t.getTransform().transform(new Point(rotateX, rotateY), center);
        Arc2D handle = new Arc2D.Double(center.getX() - 6d, center.getY() - 6d, 12d, 12d, 0d, 360d, Arc2D.OPEN);
        return handle.contains(point);
    }

    public boolean isResizeHandle(Transform t, Point point) {
        return getCorner(t, point) != null;
    }

    public static enum Corner {
        NW(Cursor.NW_RESIZE_CURSOR, 0, 0),
        SW(Cursor.SW_RESIZE_CURSOR, 0, 1),
        SE(Cursor.SE_RESIZE_CURSOR, 1, 1),
        NE(Cursor.NE_RESIZE_CURSOR, 1, 0);

        private final Cursor cursor;
        private final int x, y;

        private Corner(int cursor, int x, int y) {
            this.cursor = new Cursor(cursor);
            this.x = x;
            this.y = y;
        }

        public Cursor getCursor() { return cursor; }
        public int getX() { return x; }
        public int getY() { return y; }

        public Point getPoint(Rectangle rect) {
            return new Point(rect.x + x * rect.width, rect.y + y * rect.height);
        }

        public Point2D getPoint2D(Rectangle2D rect) {
            return new Point2D.Double(rect.getX() + x * rect.getWidth(), rect.getY() + y * rect.getHeight());
        }
    }

    public Corner getCorner(Transform t, Point point) {
        for (Corner corner : Corner.values()) {
            Point center = corner.getPoint(unit());
            t.getTransform().transform(center, center);
            Rectangle handle = new Rectangle(center.x - 5, center.y - 5, 10, 10);
            if (handle.contains(point)) {
                return corner;
            }
        }
        return null;
    }

    public void setCornerCursor(Transform t, Point point) {
        Corner c = getCorner(t, point);

        List<Corner> we = Arrays.asList(Corner.values());
        Collections.sort(we, (a, b) -> {
            Point pa = a.getPoint(unit());
            Point pb = b.getPoint(unit());
            t.getTransform().transform(pa, pa);
            t.getTransform().transform(pb, pb);
            return Ints.compare(pa.x, pb.x);
        });
        boolean west = (we.get(0) == c || we.get(1) == c);

        List<Corner> ns = west ? Arrays.asList(we.get(0), we.get(1)) : Arrays.asList(we.get(2), we.get(3));
        Collections.sort(ns, (a, b) -> {
            Point pa = a.getPoint(unit());
            Point pb = b.getPoint(unit());
            t.getTransform().transform(pa, pa);
            t.getTransform().transform(pb, pb);
            return Ints.compare(pa.y, pb.y);
        });
        boolean north = (ns.get(0) == c);

        Corner actual = north ? (west ? Corner.NW : Corner.NE) : (west ? Corner.SW : Corner.SE);
        setCursor(actual.getCursor());
    }

    public Rectangle unit() {
        return new Rectangle(new Point(0, 0), getSize());
    }

    /** @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent) */
    @Override
    public void mouseClicked(MouseEvent e) { }

    /** @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent) */
    @Override
    public void mousePressed(MouseEvent e) {
        Transform clickTransform = getTransformAt(e.getPoint());
        Reflection clickReflection = getReflectionAt(e.getPoint());

        if (SwingUtilities.isLeftMouseButton(e)) {
            if (clickReflection != null) {
                selected = null;
                clickTransform = null;
                if (e.isAltDown()) {
                    Reflection copy = Reflection.copy(clickReflection);
                    reflection = copy;
                } else {
                    reflection = clickReflection;
                }
                start = snap(e.getPoint());
                setCursor(new Cursor(Cursor.MOVE_CURSOR));
            } else {
                resize = null;
                ifs.getTransforms().stream()
                        .filter(t -> !t.isMatrix() && isResizeHandle(t, e.getPoint()))
                        .findFirst()
                        .ifPresent(t -> { resize = t; });
                if (resize != null) {
                    corner = getCorner(resize, e.getPoint());
                    if (corner != null) {
                        Point cp = corner.getPoint(unit());
                        resize.getTransform().transform(cp, cp);
                        start = cp;
                        setCornerCursor(resize, e.getPoint());
                    }
                    ifs.getTransforms().remove(resize);
                    selected = resize;
                } else if (clickTransform != null) {
                    if (!clickTransform.isMatrix() && isRotateHandle(clickTransform, e.getPoint())) {
                        selected = clickTransform;
                        rotate = selected;
                        ifs.getTransforms().remove(rotate);
                        start = snap(e.getPoint());
                        setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
                    } else {
                        selected = clickTransform;
                        if (e.isAltDown()) {
                            Transform copy = Transform.copy(selected);
                            selected = copy;
                        }
                        move = selected;
                        setCursor(new Cursor(Cursor.MOVE_CURSOR));
                        start = snap(e.getPoint());
                        ifs.getTransforms().remove(selected);
                    }
                } else {
                    start = snap(e.getPoint());
                    selected = null;
                    setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
                    if (e.isControlDown() && reflection == null) {
                        reflection = Reflection.create(getSize());
                        reflection.x = start.x;
                        reflection.y = start.y;
                    }
                }
            }
            end = new Point(start.x, start.y);
        } else if (SwingUtilities.isRightMouseButton(e)) {
            if (clickTransform != null) {
                selected = clickTransform;
                properties.setEnabled(!selected.isMatrix());
                transformMenu.show(e.getComponent(), e.getX(), e.getY());
            } else if (clickReflection != null) {
                reflection = clickReflection;
                reflectionMenu.show(e.getComponent(), e.getX(), e.getY());
            } else  {
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
                int x = Math.min(start.x, end.x);
                int y = Math.min(start.y, end.y);
                double w = Math.max(start.x, end.x) - x;
                double h = Math.max(start.y, end.y) - y;
                int dx = end.x - start.x;
                int dy = end.y - start.y;

                start = null;
                end = null;

                int grid = controller.getSnapGrid();
                if (w <= grid && h <= grid) {
                    reflection = null;
                    repaint();
                    return;
                }

                if (reflection != null) {
                    if (e.isControlDown()) {
                        reflection.r = Math.atan2(dx, dy);
                    }
                    ifs.add(reflection);
                    reflection = null;
                } else {
                    selected = Transform.create(getSize());
                    selected.x = x;
                    selected.y = y;
                    selected.w = w;
                    selected.h = h;
                    ifs.add(selected);
                }
                bus.post(ifs);
            } else if (selected != null  && start != null && end != null) {
                ifs.add(selected);
                bus.post(ifs);
            } else if (reflection != null  && start != null && end != null) {
                ifs.add(reflection);
                bus.post(ifs);
            }
        }
    }

    /** @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent) */
    @Override
    public void mouseEntered(MouseEvent e) { }

    /** @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent) */
    @Override
    public void mouseExited(MouseEvent e) { }

    /** @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent) */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (start != null) {
            end = snap(e.getPoint());
            if (reflection != null) {
                if (e.isControlDown()) {
                    int dx = end.x - start.x;
                    int dy = end.y - start.y;
                    reflection.r = Math.IEEEremainder(Math.atan2(dx, dy), Math.PI * 2d);
                } else {
                    reflection.x = end.x;
                    reflection.y = end.y;
                }
            } else {
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

                    int x = resize.x;
                    int y = resize.y;

                    switch (corner) {
                        case NW:
                            x += (inverseX.x + inverseY.x);
                            y += (inverseX.y + inverseY.y);
                            w -= delta.x;
                            h -= delta.y;
                            break;
                        case NE:
                            x += inverseY.x;
                            y += inverseY.y;
                            if (e.isShiftDown()) {
                                w -= delta.x;
                            } else {
                                w += delta.x;
                            }
                            h -= delta.y;
                            break;
                        case SW:
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
                        case SE:
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
                        int x = move.x + dx;
                        int y = move.y + dy;

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
            }
            repaint();
        }
    }

    /** @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent) */
    @Override
    public void mouseMoved(MouseEvent e) {
        if (start == null) {
            Optional<Transform> corner = ifs.getTransforms().stream()
                    .filter(t -> !t.isMatrix() && isResizeHandle(t, e.getPoint()))
                    .findFirst();
            Optional<Transform> rotate = ifs.getTransforms().stream()
                    .filter(t -> !t.isMatrix() && isRotateHandle(t, e.getPoint()))
                    .findFirst();
            if (corner.isPresent()) {
                setCornerCursor(corner.get(), e.getPoint());
            } else if (rotate.isPresent()) {
                setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
            } else if (getTransformAt(e.getPoint()) != null) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            } else if (getReflectionAt(e.getPoint()) != null) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            } else {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }

    /** @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent) */
    @Override
    public void keyPressed(KeyEvent e) {
        if (isVisible()) {
            if (selected != null) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_DELETE:
                    case KeyEvent.VK_BACK_SPACE:
                        ifs.getTransforms().remove(selected);
                        selected = null;
                        bus.post(ifs);
                        break;
                    case KeyEvent.VK_EQUALS:
                        if (!e.isShiftDown()) break;
                    case KeyEvent.VK_MINUS:
                        if (selected.isMatrix()) {
                            AffineTransform t = selected.getTransform();
                            t.concatenate(AffineTransform.getQuadrantRotateInstance(e.getKeyCode() == KeyEvent.VK_EQUALS ? 1 : -1));
                            double matrix[] = new double[6];
                            t.getMatrix(matrix);
                            selected.setMatrix(matrix);
                        } else {
                            if (e.getKeyCode() == KeyEvent.VK_EQUALS) {
                                selected.r += Math.PI / 2d;
                            } else {
                                selected.r -= Math.PI / 2d;
                            }
                        }
                        repaint();
                        break;
                    case KeyEvent.VK_LEFT:
                    case KeyEvent.VK_RIGHT:
                    case KeyEvent.VK_UP:
                    case KeyEvent.VK_DOWN:
                        int dx = 0, dy = 0;
                        int delta = e.isShiftDown() ? 1 : controller.getSnapGrid();
                        switch (e.getKeyCode()) {
                            case KeyEvent.VK_LEFT:
                                dx -= delta; break;
                            case KeyEvent.VK_RIGHT:
                                dx += delta; break;
                            case KeyEvent.VK_UP:
                                dy -= delta; break;
                            case KeyEvent.VK_DOWN:
                                dy += delta; break;
                        }
                        if (selected.isMatrix()) {
                            AffineTransform t = AffineTransform.getTranslateInstance(dx, dy);
                            t.concatenate(selected.getTransform());
                            double matrix[] = new double[6];
                            t.getMatrix(matrix);
                            selected.setMatrix(matrix);
                        } else {
                            selected.x += dx;
                            selected.y += dy;
                        }
                        repaint();
                        break;
                }
            } else {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ESCAPE:
                        if (start != null && end != null) {
                            start = null;
                            end = null;
                        }
                        if (reflection != null) {
                            reflection = null;
                        }
                        repaint();
                        break;
                }
            }
        }
    }

    /** @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent) */
    @Override
    public void keyTyped(KeyEvent e) { }

    /** @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent) */
    @Override
    public void keyReleased(KeyEvent e) { }

}
