package ai.nets.samj.gui.last;


import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

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

    private void runOnEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    // Convenience delegates (so callers don't need getHtmlPane())
    public void clear() {
        runOnEdt(() -> {
            // replaces pane.clear()
            pane.setBodyHtml("");
        });
    }
    public void append(String html) { pane.append(html); }
}