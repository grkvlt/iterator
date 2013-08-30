/*
 * Copyright 2012 by Andrew Kennedy; All Rights Reserved
 */
package iterator.view;

import iterator.Explorer;
import iterator.model.IFS;
import iterator.model.Transform;

import javax.swing.JTextPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

/**
 * Detail display.
 */
public class Details extends JTextPane {
    /** serialVersionUID */
    private static final long serialVersionUID = -1;

    public static final Logger LOG = LoggerFactory.getLogger(Details.class);

    private IFS ifs;

    public Details(EventBus bus, Explorer controller) {
        super();

        setContentType("text/html");
        setText("<html><h1 id=\"title\">Iterated Function System</h1><html>");
        HTMLEditorKit kit = (HTMLEditorKit) getEditorKitForContentType("text/html");
        StyleSheet css = kit.getStyleSheet();
        css.addRule("h1 { font-family: Calibri, sans-serif; font-style: bold; font-size: 30pt; margin-left: 10px; }");
        css.addRule("h2 { font-family: Calibri, sans-serif; font-style: bold; font-size: 20pt; margin-left: 10px; }");
        css.addRule("span.id { font-family: Calibri, sans-serif; font-style: bold; font-size: 15pt; }");
        css.addRule("div.matrix { font-family: Cambria, serif; font-style: italic; font-size: 15pt; margin-left: 40px;}");
        css.addRule("div#transforms { margin-left: 20px; }");

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
                html.append("<div id=\"transforms\">");
                for (Transform t : ifs.getTransforms()) {
                    double[] matrix = new double[6];
                    t.getTransform().getMatrix(matrix);
                    String data = String.format("<div class=\"transform\"><span class=\"id\">%02d</span>" +
                            "<div class=\"matrix\">[ %f, %f, %f ]<br />[ %f, %f, %f ]</div>" +
                            "</div>",
                            t.getId(), matrix[0], matrix[1], matrix[2], matrix[3], matrix[4], matrix[5]);
                    html.append(data);
                }
                html.append("</div>");
            }
            html.append("</html>");
            setText(html.toString());
        } catch (Exception e) {
            Throwables.propagate(e);
        }
        repaint();
    }
}
