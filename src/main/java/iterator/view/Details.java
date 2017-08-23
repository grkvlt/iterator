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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import javax.swing.JTextPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import iterator.Explorer;
import iterator.model.IFS;
import iterator.model.Reflection;
import iterator.model.Transform;
import iterator.util.Subscriber;
import iterator.util.Utils;

/**
 * Detail display.
 */
public class Details extends JTextPane implements Printable, Subscriber {
    /** serialVersionUID */
    private static final long serialVersionUID = -1279626785145420083L;

    private final Explorer controller;
    private final EventBus bus;

    private IFS ifs;

    public static final String HTML_MIME_TYPE = "text/html";
    public static final String INITIAL_CONTENT_HTML = "<html><h1 id=\"title\">Iterated Function System</h1><html>";

    public static final List<String> CSS_RULES = Arrays.asList(
        "h1 { font-family: Calibri, sans-serif; font-style: bold; font-size: 30px; margin-left: 10px; }",
        "h2 { font-family: Calibri, sans-serif; font-style: bold; font-size: 20px; margin-left: 10px; }",
        ".id { font-family: Calibri, sans-serif; font-style: bold; font-size: 15px; padding: 5px 0 0 0; margin: 0; }",
        ".info { font-family: Calibri, sans-serif; font-style: italic; font-size: 12px; padding: 0 0 5px 0; margin: 0; }",
        ".matrixr1 { font-family: Cambria, serif; font-style: italic; font-size: 12px; padding: 5px 5px 0 -5px; margin: 0; width: 60px; }",
        ".matrixr2 { font-family: Cambria, serif; font-style: italic; font-size: 12px; padding: 0 5px -5px -5px; margin: 0 0 -5px 0; width: 60px; }",
        ".reflect { font-family: Cambria, serif; font-style: italic; font-size: 12px; padding: 5px 5px 0 -5px; margin: 0; width: 60px; }",
        ".ifs { margin-left: 20px; border: 0; }"
    );
    public static final List<String> CSS_BRACKET_RULES = Arrays.asList(
        ".bracketl { width: 5px; padding: 0; margin-left: 2px; border-left: 2px solid black; border-top: 2px solid black; border-bottom: 2px solid black; }",
        ".bracketr { width: 5px; padding: 0; margin-right: 2px; border-right: 2px solid black; border-top: 2px solid black; border-bottom: 2px solid black; }"
    );

    public Details(Explorer controller) {
        super();

        this.controller = controller;
        this.bus = controller.getEventBus();

        setEditable(false);
        setContentType(HTML_MIME_TYPE);
        setText(INITIAL_CONTENT_HTML);
        HTMLEditorKit kit = (HTMLEditorKit) getEditorKitForContentType(HTML_MIME_TYPE);
        StyleSheet css = kit.getStyleSheet();
        for (String rule : Iterables.concat(CSS_RULES, CSS_BRACKET_RULES)) {
            css.addRule(rule);
        }

        bus.register(this);
    }

    /** @see Subscriber#resized(Dimension) */
    @Override
    @Subscribe
    public void resized(Dimension size) {
        setSize(size.width, getHeight());

        controller.getScroll().getViewport().setViewSize(size);

        setDetails();
    }

    /** @see Subscriber#updated(IFS) */
    @Override
    @Subscribe
    public void updated(IFS ifs) {
        this.ifs = ifs;

        setDetails();
    }

    public void setDetails() {
        StringBuilder html = new StringBuilder("<html>");
        String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, Optional.fromNullable(ifs.getName()).or(IFS.UNTITLED));
        String words = CharMatcher.JAVA_LETTER_OR_DIGIT.negate().replaceFrom(name, ' ');
        html.append("<a name=\"top\"></a>")
            .append(String.format("<h1 id=\"title\">IFS %s</h1>", words));

        if (ifs.getTransforms().isEmpty()) {
            html.append("<h2>Empty</h2>");
        } else {
            int i = 0;
            int columns = controller.getWidth() / 380;
            html.append("<table>")
                .append("<tr><td><h2>Transforms</h2></td></tr>");
            for (Transform t : Ordering.from(IFS.IDENTITY).immutableSortedCopy(ifs.getTransforms())) {
                if (i % columns == 0) html.append("<tr>");
                html.append("<td>")
                    .append("<table class=\"ifs\">");

                double[] matrix = new double[6];
                t.getTransform().getMatrix(matrix);

                Color c = Color.WHITE;
                if (controller.isColour()) {
                    if (controller.hasPalette()) {
                        c = Iterables.get(controller.getColours(), i % controller.getPaletteSize());
                    } else {
                        c = Color.getHSBColor((float) i / (float) ifs.size(), 0.8f, 0.8f);
                    }
                }

                String transform = String.format(
                        "<tr class=\"transform\">" +
                            "<td class=\"id\" width=\"50px\">%02d</td>" +
                            "<td class=\"bracketl\" rowspan=\"2\">&nbsp;</td>" +
                            "<td class=\"matrixr1\" align=\"right\">%.3f</td>" +
                            "<td class=\"matrixr1\" align=\"right\">%.3f</td>" +
                            "<td class=\"matrixr1\" align=\"right\">%.3f</td>" +
                            "<td class=\"bracketr\" rowspan=\"2\">&nbsp;</td>" +
                        "</tr>" +
                        "<tr class=\"transform\">" +
                            "<td class=\"info\" width=\"50px\">%.1f%%" +
                                "<div style=\"width: 15px; height: 10px; border: 1px solid %s; " +
                                "background: #%02x%02x%02x; padding: 0; margin: 0;\">&nbsp;</div>" +
                            "</td>" +
                            "<td class=\"matrixr2\" align=\"right\">%.3f</td>" +
                            "<td class=\"matrixr2\" align=\"right\">%.0f</td>" +
                            "<td class=\"matrixr2\" align=\"right\">%.0f</td>" +
                        "</tr>" +
                        "<tr class=\"space\"><td colspan=\"6\">&nbsp;</td></tr>",
                        t.getId(),
                        matrix[0], matrix[1], matrix[2],
                        100d * t.getWeight() / controller.getEditor().getWeight(ifs.getTransforms()),
                        controller.isColour() ? "black" : "white",
                        c.getRed(), c.getGreen(), c.getBlue(),
                        matrix[3], matrix[4], matrix[5]);
                html.append(transform)
                    .append("</table>")
                    .append("</td>");
                i++;
                if (i % columns == 0) html.append("</tr>");
            }

            if (ifs.getReflections().size() > 0) {
                if (i % columns != 0) html.append("</tr>");
                html.append("<tr><td><h2>Reflections</h2></td></tr>");
                for (Reflection r : ifs.getReflections()) {
                    if (i % columns == 0) html.append("<tr>");
                    html.append("<td>")
                        .append("<table class=\"ifs\">");

                    Color c = Color.WHITE;
                    if (controller.isColour()) {
                        if (controller.hasPalette()) {
                            c = Iterables.get(controller.getColours(), i % controller.getPaletteSize());
                        } else {
                            c = Color.getHSBColor((float) i / (float) ifs.size(), 0.8f, 0.8f);
                        }
                    }

                    String reflection = String.format(
                            "<tr class=\"reflection\">" +
                                "<td class=\"id\" width=\"50px\">%02d" +
                                    "<div style=\"width: 15px; height: 10px; border: 1px solid %s; " +
                                    "background: #%02x%02x%02x; padding: 0; margin: 0;\">&nbsp;</div>" +
                                "</td>" +
                                "<td class=\"reflect\" align=\"right\" colspan=\"3\">(%.0f, %.0f)</td>" +
                                "<td class=\"reflect\" align=\"right\" colspan=\"2\">%.0f °</td>" +
                            "</tr>" +
                            "<tr class=\"space\"><td colspan=\"6\">&nbsp;</td></tr>",
                            r.getId(),
                            controller.isColour() ? "black" : "white",
                            c.getRed(), c.getGreen(), c.getBlue(),
                            r.x, r.y, Math.toDegrees(r.r));
                    html.append(reflection)
                        .append("</table>")
                        .append("</td>");
                    i++;
                    if (i % columns == 0) html.append("</tr>");
                }
            }
            html.append("</tr>")
                .append("</table>");
        }

        html.append("</html>");
        setText(html.toString());

        repaint();

        scrollToReference("top");
    }

    /** @see java.awt.print.Printable#print(Graphics, PageFormat, int) */
    @Override
    public int print(Graphics graphics, PageFormat pf, int page) throws PrinterException {
        if (page > 0) return NO_SUCH_PAGE;

        Graphics2D g = (Graphics2D) graphics.create();

        try {
            g.translate(pf.getImageableX(), pf.getImageableY());
            double scale = pf.getImageableWidth() / (double) getWidth();
            if ((scale * getHeight()) > pf.getImageableHeight()) {
                scale = pf.getImageableHeight() / (double) getHeight();
            }
            g.scale(scale, scale);
            printAll(g);
        } catch (Exception e) {
            controller.error(e, "Failure printing details");
        } finally {
            g.dispose();
        }

        return PAGE_EXISTS;
    }

}
