package ai.nets.samj.gui.last;


import javax.swing.BorderFactory;
import javax.swing.JScrollPane;

import ai.nets.samj.gui.HTMLPane;

public class HTMLPaneScroll extends JScrollPane {

    private static final long serialVersionUID = 1L;

    private final HTMLPane pane;

    public HTMLPaneScroll() {
        this(new HTMLPane());
    }

    public HTMLPaneScroll(HTMLPane pane) {
        super(pane, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.pane = pane;

        setBorder(BorderFactory.createEmptyBorder());
        setViewportBorder(BorderFactory.createEmptyBorder());
    }

    public HTMLPane getHtmlPane() {
        return pane;
    }

    // Convenience delegates (so callers don't need getHtmlPane())
    public void clear() { pane.clear(); }
    public void append(String html) { pane.append(html); }
    public void append(String tag, String content) { pane.append(tag, content); }
}