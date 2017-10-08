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

import static iterator.Utils.DASHED_LINE_1;
import static iterator.Utils.DASHED_LINE_2;
import static iterator.Utils.DOTTED_LINE_2;
import static iterator.Utils.SOLID_LINE_2;
import static iterator.Utils.alpha;
import static iterator.Utils.calibri;
import static iterator.Utils.checkBoxItem;
import static iterator.Utils.context;
import static iterator.Utils.menuItem;
import static iterator.util.Messages.MENU_VIEWER_GRID;
import static iterator.util.Messages.MENU_VIEWER_INFO;
import static iterator.util.Messages.MENU_VIEWER_OVERLAY;
import static iterator.util.Messages.MENU_VIEWER_PAUSE;
import static iterator.util.Messages.MENU_VIEWER_RESUME;
import static iterator.util.Messages.MENU_VIEWER_ZOOM;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.MouseInputListener;

import com.google.common.eventbus.Subscribe;
import com.google.common.math.DoubleMath;

import iterator.Explorer;
import iterator.dialog.Zoom;
import iterator.model.IFS;
import iterator.model.Reflection;
import iterator.model.Transform;
import iterator.util.Config;
import iterator.util.Dialog;
import iterator.util.Messages;
import iterator.util.Output;
import iterator.util.Subscriber;

/**
 * Rendered IFS viewer.
 */
public class Viewer extends JPanel implements ActionListener, KeyListener, MouseInputListener, Printable, Subscriber {

    private final Explorer controller;
    private final Messages messages;
    private final Config config;
    private final Output out;
    private final Iterator iterator;

    private IFS ifs;
    private Timer timer;
    private float scale;
    private Point2D centre;
    private Dimension size;
    private Rectangle zoom;
    private boolean overlay, info, grid;
    private JPopupMenu viewer;
    private Zoom properties;
    private JCheckBoxMenuItem showGrid, showOverlay, showInfo;
    private JMenuItem pause, resume;

    public Viewer(Explorer controller) {
        super();

        this.controller = controller;
        this.messages = controller.getMessages();
        this.config = controller.getConfig();
        this.out = controller.getOutput();
        this.iterator = controller.getIterator();

        timer = new Timer(50, this);
        timer.setCoalesce(true);
        timer.start();

        size = getSize();

        properties = Zoom.dialog(controller);
        viewer = new JPopupMenu();
        viewer.add(menuItem(messages.getText(MENU_VIEWER_ZOOM), e -> {
            Dialog.show(properties, controller);
        }));
        pause = menuItem(messages.getText(MENU_VIEWER_PAUSE), e -> {
            stop();
        });
        viewer.add(pause);
        resume = menuItem(messages.getText(MENU_VIEWER_RESUME), e -> {
            start();
        });
        viewer.add(resume);
        JMenuItem separator = new JMenuItem("-");
        separator.setEnabled(false);
        viewer.add(separator);
        showGrid = checkBoxItem(messages.getText(MENU_VIEWER_GRID), e -> {
            setGrid(!grid);
        });
        viewer.add(showGrid);
        showOverlay = checkBoxItem(messages.getText(MENU_VIEWER_OVERLAY), e -> {
            setOverlay(!overlay);
        });
        viewer.add(showOverlay);
        showInfo = checkBoxItem(messages.getText(MENU_VIEWER_INFO), e -> {
            setInfo(!info);
        });
        viewer.add(showInfo);
        add(viewer);

        addMouseListener(this);
        addMouseMotionListener(this);

        controller.getEventBus().register(this);
    }

    public long getCount() { return iterator.getCount(); }

    /** @see Subscriber#updated(IFS) */
    @Override
    @Subscribe
    public void updated(IFS ifs) {
        this.ifs = ifs;
        if (!iterator.isRunning()) {
            setOverlay(config.isDebug());
            setInfo(config.isDebug());
            setGrid(config.isDebug());
            reset();
            rescale();
        }
    }

    /** @see Subscriber#resized(Dimension) */
    @Override
    @Subscribe
    public void resized(Dimension size) {
        stop();
        this.size = size;
        rescale();
        reset();
        if (isVisible()) {
            start();
        }
    }

    /** @see javax.swing.JComponent#paintComponent(Graphics) */
    @Override
    protected void paintComponent(Graphics graphics) {
        context(controller, graphics, g -> {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (getImage() != null) {
                g.drawImage(getImage(), new AffineTransformOp(new AffineTransform(), AffineTransformOp.TYPE_BILINEAR), 0, 0);
            }

            if (zoom != null) {
                g.setPaint(config.getRender().getForeground());
                g.setStroke(DOTTED_LINE_2);
                g.draw(zoom);
            }

            if (overlay) {
                for (Transform t : ifs.getTransforms()) {
                    paintTransform(t, g);
                }
                for (Reflection r : ifs.getReflections()) {
                    paintReflection(r, g);
                }
            }

            if (info) {
                String countText = String.format("%,dK", getCount()).replaceAll("[^0-9K+]", " ");
                String infoText = getInfoText();

                g.setPaint(config.getRender().getForeground());
                FontRenderContext frc = g.getFontRenderContext();
                TextLayout infoLayout = new TextLayout(infoText, calibri(Font.BOLD, 16), frc);
                infoLayout.draw(g, 10f, size.height - 10f);
                TextLayout countLayout = new TextLayout(countText, calibri(Font.BOLD, 16), frc);
                countLayout.draw(g, size.width - 10f - (float) countLayout.getBounds().getWidth(), size.height - 10f);
            }

            if (grid) {
                paintGrid(g);
            }
        });
    }

    public void paintTransform(Transform t, Graphics2D graphics) {
        context(controller, graphics, g -> {
            // Transform unit square to view space
            double x = (size.getWidth() / 2d) - (centre.getX() * scale);
            double y = (size.getHeight() / 2d) - (centre.getY() * scale);
            Rectangle unit = new Rectangle(getSize());
            Shape rect = t.getTransform().createTransformedShape(unit);
            AffineTransform view = AffineTransform.getTranslateInstance(x, y);
            view.scale(scale, scale);
            rect = view.createTransformedShape(rect);

            // Draw the outline
            Color c = alpha(config.getRender().getForeground(), 128);
            g.setPaint(c);
            g.setStroke(SOLID_LINE_2);
            g.draw(rect);
            g.setPaint(alpha(Color.GRAY, 8));
            g.fill(rect);
        });
    }

    public void paintReflection(Reflection r, Graphics2D graphics) {
        context(controller, graphics, g -> {
            // Transform unit square to view space
            double x = (size.getWidth() / 2d) - (centre.getX() * scale);
            double y = (size.getHeight() / 2d) - (centre.getY() * scale);
            AffineTransform view = AffineTransform.getTranslateInstance(x, y);
            view.scale(scale, scale);

            // Draw the line
            Color c = alpha(config.getRender().getForeground(), 128);
            g.setPaint(c);
            g.setStroke(DASHED_LINE_2);
            Path2D line = new Path2D.Double(Path2D.WIND_NON_ZERO);
            if ((r.r < Math.toRadians(0.1d) && r.r > Math.toRadians(-0.1d)) ||
                    (r.r < Math.toRadians(180.1d) && r.r > Math.toRadians(179.9d))) {
                line.moveTo(r.x, -1d * (size.getHeight() / scale));
                line.lineTo(r.x, (size.getHeight() / scale));
            } else {
                line.moveTo(r.x - size.getWidth() - (size.getWidth() / scale),
                        r.y - (size.getWidth() / Math.tan(r.r)) - (size.getWidth() / (Math.tan(r.r) * scale)));
                line.lineTo(r.x + size.getWidth() + (size.getWidth() / scale),
                        r.y + (size.getWidth() / Math.tan(r.r)) + (size.getWidth() / (Math.tan(r.r) * scale)));
            }
            g.draw(view.createTransformedShape(line));
        });
    }

    public void paintGrid(Graphics2D graphics) {
        context(controller, graphics, g -> {
            // Transform unit square to view space
            double x0 = centre.getX() - (size.getWidth() / 2d / scale);
            double y0 = centre.getY() - (size.getHeight() / 2d / scale);
            AffineTransform view = AffineTransform.getScaleInstance(scale, scale);
            view.translate(-x0, -y0);

            // Calculate grid position
            double spacing = config.getMaxGrid() / scale;
            double mx = centre.getX() - (size.getWidth() / 2d / scale) - (size.getWidth() / 2d);
            double my = centre.getY() - (size.getHeight() / 2d / scale) - (size.getHeight() / 2d);
            double rx = Math.IEEEremainder(mx, spacing);
            double ry = Math.IEEEremainder(my, spacing);
            double sx = mx - rx + (size.getWidth() / 2d);
            double sy = my - ry + (size.getHeight() / 2d);
            int xn = (int) ((sx - x0) / spacing);
            int yn = (int) ((sy - y0) / spacing);
            double x1 = sx + (xn - 2) * spacing;
            double y1 = sy + (yn - 2) * spacing;
            int n = (size.width / config.getMaxGrid()) + 4;

            // Draw grid lines
            g.setStroke(DASHED_LINE_1);
            for (int i = 0; i < n; i++) {
                double x = x1 + i * spacing;
                if (DoubleMath.fuzzyEquals(x, size.getWidth() / 2d, spacing / 100d)) {
                    g.setPaint(alpha(config.getRender().getForeground(), 128));
                } else {
                    g.setPaint(alpha(config.getRender().getForeground(), 64));
                }
                double pts[] = { x, y1, x, y1 + n * spacing };
                view.transform(pts, 0, pts, 0, 2);
                if (pts[0] > 0d && pts[0] < size.getWidth()) {
                    Line2D line = new Line2D.Double(pts[0], pts[1], pts[2], pts[3]);
                    g.draw(line);
                }
            }
            for (int i = 0; i < n; i++) {
                double y = y1 + i * spacing;
                if (DoubleMath.fuzzyEquals(y, size.getHeight() / 2d, spacing / 100d)) {
                    g.setPaint(alpha(config.getRender().getForeground(), 128));
                } else {
                    g.setPaint(alpha(config.getRender().getForeground(), 64));
                }
                double pts[] = { x1, y, x1 + n * spacing, y };
                view.transform(pts, 0, pts, 0, 2);
                if (pts[1] > 0d && pts[1] < size.getWidth()) {
                    Line2D line = new Line2D.Double(pts[0], pts[1], pts[2], pts[3]);
                    g.draw(line);
                }
            }
        });
    }

    public void rescale() {
        scale = config.getDisplayScale();
        centre = new Point2D.Double(config.getDisplayCentreX() * size.getWidth(), config.getDisplayCentreY() * size.getHeight());
    }

    public void resetScale() {
        config.setDisplayScale(Config.DEFAULT_DISPLAY_SCALE);
        config.setDisplayCentreX(Config.DEFAULT_DISPLAY_CENTRE_X);
        config.setDisplayCentreY(Config.DEFAULT_DISPLAY_CENTRE_Y);

        rescale();
    }

    public void reset() {
        if (size.getWidth() <= 0 && size.getHeight() <= 0) return;

        iterator.reset(size);
    }

    public String getInfoText() {
        String text = String.format("%s [%s/%d]",
                iterator.getInfo(), iterator.getTaskSet().isEmpty() ? "-" : Integer.toString(iterator.getTaskSet().size()), config.getThreads());
        return text;
    }

    public BufferedImage getImage() {
        return iterator.getImage();
    }

    /** @see java.awt.print.Printable#print(Graphics, PageFormat, int) */
    @Override
    public int print(Graphics graphics, PageFormat pf, int page) throws PrinterException {
        if (page > 0) return NO_SUCH_PAGE;

        context(controller, graphics, g -> {
            g.translate(pf.getImageableX(), pf.getImageableY());
            double scale = pf.getImageableWidth() / size.getWidth();
            if ((scale * getHeight()) > pf.getImageableHeight()) {
                scale = pf.getImageableHeight() / size.getHeight();
            }
            g.scale(scale, scale);
            g.setClip(0, 0, size.width, size.height);
            printAll(g);
        });

        return PAGE_EXISTS;
    }


    /**
     * Invoked when the timer fires, to refresh the image when rendering.
     *
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (isVisible()) {
            repaint();
        }
    }

    public void start() {
        iterator.setTransforms(ifs);
        iterator.start();
        pause.setEnabled(true);
        resume.setEnabled(false);
    }

    public boolean stop() {
        pause.setEnabled(false);
        resume.setEnabled(true);
        return iterator.stop();
    }

    public boolean isRunning() {
        return iterator.isRunning();
    }

    /** @see java.awt.event.KeyListener#keyPressed(java.awt.event.KeyEvent) */
    @Override
    public void keyPressed(KeyEvent e) {
        if (isVisible()) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_C:
                    if (e.isControlDown() || e.isMetaDown()) {
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        Transferable text = new StringSelection(iterator.getInfo());
                        clipboard.setContents(text, null);
                    }
                    break;
                case KeyEvent.VK_SPACE:
                    if (isRunning()) {
                        stop();
                    } else {
                        start();
                    }
                    break;
                case KeyEvent.VK_ESCAPE:
                    if (zoom != null) {
                        zoom = null;
                        repaint();
                    }
                    break;
                case KeyEvent.VK_MINUS:
                    stop();
                    config.setDisplayScale(scale / 2f);
                    rescale();
                    reset();
                    start();
                    break;
                case KeyEvent.VK_EQUALS:
                    stop();
                    if (e.isShiftDown()) {
                        config.setDisplayScale(scale * 2f);
                        rescale();
                    } else {
                        resetScale();
                    }
                    reset();
                    start();
                    break;
                case KeyEvent.VK_I:
                    setInfo(!info);
                    break;
                case KeyEvent.VK_O:
                    setOverlay(!overlay);
                    break;
                case KeyEvent.VK_G:
                    setGrid(!grid);
                    break;
                case KeyEvent.VK_T:
                    out.timestamp("Thread dump");
                    System.err.println(iterator.getThreadDump());
                    break;
                case KeyEvent.VK_UP:
                    config.setThreads(config.getThreads() + 1);
                    iterator.updateTasks();
                    repaint();
                    break;
                case KeyEvent.VK_DOWN:
                    config.setThreads(config.getThreads() - 1);
                    iterator.updateTasks();
                    repaint();
                    break;
            }
        }
    }

    /** @see java.awt.event.KeyListener#keyTyped(java.awt.event.KeyEvent) */
    @Override
    public void keyTyped(KeyEvent e) { }

    /** @see java.awt.event.KeyListener#keyReleased(java.awt.event.KeyEvent) */
    @Override
    public void keyReleased(KeyEvent e) { }

    private void setInfo(boolean state) {
        info = state;
        showInfo.setSelected(state);
        repaint();
    }

    private void setOverlay(boolean state) {
        overlay = state;
        showOverlay.setSelected(state);
        repaint();
    }

    private void setGrid(boolean state) {
        grid = state;
        showGrid.setSelected(state);
        repaint();
    }

    /** @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent) */
    @Override
    public void mousePressed(MouseEvent e) {
        if (!isContextMenu(e) && SwingUtilities.isLeftMouseButton(e)) {
            zoom = new Rectangle(e.getX(), e.getY(), 0, 0);
            repaint();
        }
    }

    /** @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent) */
    @Override public void mouseDragged(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && zoom != null) {
            Point end = e.getPoint();
            int x1 = Math.min(end.x, zoom.x);
            int x2 = Math.max(end.x, zoom.x);
            int y1 = Math.min(end.y, zoom.y);
            int y2 = Math.max(end.y, zoom.y);
            int side = Math.min(x2 - x1,  y2 - y1);
            if (side < config.getSnapGrid()) {
                side = 0;
            }
            zoom.setSize(new Dimension(side, side));
            repaint();
        }
    }

    /** @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent) */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (!isContextMenu(e) && SwingUtilities.isLeftMouseButton(e)) {
            if (zoom != null) {
                stop();

                // Calculate new centre point and scale
                Point2D origin = new Point2D.Double((centre.getX() * scale) - (size.getWidth() / 2d), (centre.getY() * scale) - (size.getHeight() / 2d));
                Point2D updated = new Point2D.Double((zoom.x + (zoom.width / 2d) + origin.getX()) / scale, (zoom.y + (zoom.height / 2d) + origin.getY()) / scale);
                if (zoom.width == 0) {
                    config.setDisplayScale(scale * 2f);
                } else {
                    config.setDisplayScale(scale * ((float) size.getWidth() / (float) zoom.width));
                }
                config.setDisplayCentreX(updated.getX() / size.getWidth());
                config.setDisplayCentreY(updated.getY() / size.getHeight());
                rescale();

                if (config.isDebug()) {
                    out.debug("Zoom: %.1fx scale, centre (%.1f, %.1f) via click at (%d, %d)",
                            scale, centre.getX(), centre.getY(), (int) (zoom.x + (zoom.width / 2d)), (int) (zoom.y + (zoom.height / 2d)));
                }

                zoom = null;
                reset();
                start();
            }
        }
    }

    public boolean isContextMenu(MouseEvent e) {
        if (e.isPopupTrigger()) {
            viewer.show(e.getComponent(), e.getX(), e.getY());
            return true;
        } else {
            return false;
        }
    }

    /** @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent) */
    @Override
    public void mouseEntered(MouseEvent e) { }

    /** @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent) */
    @Override
    public void mouseExited(MouseEvent e) { }

    /** @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent) */
    @Override
    public void mouseMoved(MouseEvent e) { }

    /** @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent) */
    @Override
    public void mouseClicked(MouseEvent e) { }

}
