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

import javax.swing.JTextPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Detail display.
 */
public class Details extends JTextPane {
    /** serialVersionUID */
    private static final long serialVersionUID = -1;

    private IFS ifs;

    public Details(EventBus bus, Explorer controller) {
        super();

        setContentType("text/html");
        setText("<html><h1 id=\"title\">Iterated Function System</h1><html>");
        HTMLEditorKit kit = (HTMLEditorKit) getEditorKitForContentType("text/html");
        StyleSheet css = kit.getStyleSheet();
        css.addRule("h1 { font-family: Calibri, sans-serif; font-style: bold; font-size: 30px; margin-left: 10px; }");
        css.addRule("h2 { font-family: Calibri, sans-serif; font-style: bold; font-size: 20px; margin-left: 10px; }");
        css.addRule(".id { font-family: Calibri, sans-serif; font-style: bold; font-size: 10px; }");
        css.addRule(".matrix { font-family: Cambria, serif; font-style: italic; font-size: 10px; margin-left: 40px; }");
        css.addRule("#transforms { margin-left: 20px; border: 0px; }");

        bus.register(this);
    }

    @Subscribe
    public void update(IFS ifs) {
        this.ifs = ifs;

        setDetails();
    }

    public void setDetails() {
        StringBuilder html = new StringBuilder();
        try {
            html.append("<html><h1>Iterated Function System</h1>");
            html.append(String.format("<h2 id=\"name\">%s</h2>", ifs.getName() == null ? IFS.UNTITLED : ifs.getName()));
            if (ifs.getTransforms().isEmpty()) {
                html.append("<h2>No Transforms</h2>");
            } else {
                html.append("<h2>Transforms</h2>");
                html.append("<table id=\"transforms\">");
                for (Transform t : ifs.getTransforms()) {
                    double[] matrix = new double[6];
                    t.getTransform().getMatrix(matrix);
                    String data = String.format("<tr class=\"transform\"><td class=\"id\">%02d</td>" +
                            "<td class=\"matrix\">[ %f, %f, %f ]<br />[ %f, %f, %f ]</td>" +
                            "</tr>",
                            t.getId(), matrix[0], matrix[1], matrix[2], matrix[3], matrix[4], matrix[5]);
                    html.append(data);
                }
                html.append("</table>");
            }
            html.append("</html>");
            setText(html.toString());
        } catch (Exception e) {
            Throwables.propagate(e);
        }
        repaint();
    }
}
