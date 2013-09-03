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
package iterator.view;

import iterator.Explorer;
import iterator.model.IFS;
import iterator.model.Transform;

import java.awt.Dimension;

import javax.swing.JTextPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.google.common.base.CaseFormat;
import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Detail display.
 */
public class Details extends JTextPane {
    /** serialVersionUID */
    private static final long serialVersionUID = -1279626785145420083L;

    private final Explorer controller;

    private IFS ifs;

    private static final String HTML_MIME_TYPE = "text/html";
    private static final String INITIAL_CONTENT_HTML = "<html><h1 id=\"title\">Iterated Function System</h1><html>";

    private static final String[] CSS_RULES = new String[] {
        "h1 { font-family: Calibri, sans-serif; font-style: bold; font-size: 30px; margin-left: 10px; }",
        "h2 { font-family: Calibri, sans-serif; font-style: bold; font-size: 20px; margin-left: 10px; }",
        ".id { font-family: Calibri, sans-serif; font-style: bold; font-size: 13px; padding-top: 24px; }",
        ".matrixr1 { font-family: Cambria, serif; font-style: italic; font-size: 11px; padding: 20px 10px -5px 10px; }",
        ".matrixr2 { font-family: Cambria, serif; font-style: italic; font-size: 11px; padding: -5px 10px -5px 10px; }",
        ".brackets { font-family: Cambria, serif; font-weight: 100; font-size: 28px; padding: 14px -5px -5px 0px; }",
        ".ifs { margin-left: 20px; border: 0px; }",
    };

    public Details(EventBus bus, Explorer controller) {
        super();

        this.controller = controller;

        setContentType(HTML_MIME_TYPE);
        setText(INITIAL_CONTENT_HTML);
        HTMLEditorKit kit = (HTMLEditorKit) getEditorKitForContentType(HTML_MIME_TYPE);
        StyleSheet css = kit.getStyleSheet();
        for (String rule : CSS_RULES) {
            css.addRule(rule);
        }
 
        bus.register(this);
    }

    @Subscribe
    public void size(Dimension size) {
        setSize(size.width, getHeight());

        controller.getScroll().getViewport().setViewSize(size);

        setDetails();
    }

    @Subscribe
    public void update(IFS ifs) {
        this.ifs = ifs;

        setDetails();
    }

    public void setDetails() {
        StringBuilder html = new StringBuilder();
        html.append("<html>");
        String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, Optional.fromNullable(ifs.getName()).or(IFS.UNTITLED));
        html.append(String.format("<a name=\"top\"></a><h1 id=\"title\">IFS - %s</h1>", name));
        if (ifs.isEmpty()) {
            html.append("<h2>Empty</h2>");
        } else {
            int i = 0;
            int columns = (int) Math.floor((float) getWidth() / 350f);
            html.append("<table>");
            for (Transform t : ifs) {
                if (i % columns == 0 && i != 0) { html.append("</tr>"); }
                if (i % columns == 0) { html.append("<tr>"); }
                html.append("<td>");
                html.append("<table class=\"ifs\" width=\"250px\">");
                double[] matrix = new double[6];
                t.getTransform().getMatrix(matrix);
                String data = String.format(
                        "<tr class=\"transform\">" +
                        "<td class=\"id\" rowspan=\"2\">%02d</td>" +
                        "<td class=\"brackets\" rowspan=\"2\">[</td>" +
                        "<td class=\"matrixr1\" align=\"right\">%.3f</td>" +
                        "<td class=\"matrixr1\" align=\"right\">%.3f</td>" +
                        "<td class=\"matrixr1\" align=\"right\">%.3f</td>" +
                        "<td class=\"brackets\" rowspan=\"2\">]</td>" +
                        "</tr>" +
                        "<tr class=\"transform\">" +
                        "<td class=\"matrixr2\" align=\"right\">%.3f</td>" +
                        "<td class=\"matrixr2\" align=\"right\">%.3f</td>" +
                        "<td class=\"matrixr2\" align=\"right\">%.3f</td>" +
                        "</tr>",
                        t.getId(), matrix[0], matrix[1], matrix[2], matrix[3], matrix[4], matrix[5]);
                html.append(data);
                html.append("</table>");
                html.append("</td>");
                i++;
            }
            html.append("</tr>");
            html.append("</table>");
        }
        html.append("</html>");

        setText(html.toString());

        repaint();

        scrollToReference("top");
    }
}
