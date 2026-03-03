package ai.nets.samj.gui;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * A lightweight HTML display pane with a modern default theme.
 * Keeps the original "append HTML then re-render" behavior, but fixes
 * recursion bugs, null sizing issues, and improves the HTML/CSS template.
 */
public class HTMLPane extends JEditorPane {

    private static final long serialVersionUID = 1729035616751891903L;

	private final StringBuilder html = new StringBuilder();

    private String header = "";
    private String footer = "";

    private Dimension dim;

    private String font = "Segoe UI";
    private String color = "#111827";       // slate-900-ish
    private String background = "#FFFFFF";
    private String accent = "#2563EB";      // blue-600-ish
    private int baseFontPx = 12;

    public enum AutoScroll { TOP, BOTTOM, NONE }
    private AutoScroll autoScroll = AutoScroll.TOP;

    public HTMLPane() {
        init();
    }

    public HTMLPane(String font) {
        this.font = font;
        init();
    }

    public HTMLPane(int width, int height) {
        this.dim = new Dimension(width, height);
        init();
    }

    public HTMLPane(String font, int width, int height) {
        this.font = font;
        this.dim = new Dimension(width, height);
        init();
    }

    public HTMLPane(String font, String color, String background, int width, int height) {
        this.font = font;
        this.dim = new Dimension(width, height);
        this.color = color;
        this.background = background;
        init();
    }

    /** Optional: adjust theme at runtime, then re-render current content. */
    public void setTheme(String font, String color, String background) {
        this.font = font != null ? font : this.font;
        this.color = color != null ? color : this.color;
        this.background = background != null ? background : this.background;
        rebuildTemplate();
        render();
    }

    /** Optional: accent color used for links/highlights. */
    public void setAccent(String accentHex) {
        if (accentHex != null && accentHex != "") {
            this.accent = accentHex;
            rebuildTemplate();
            render();
        }
    }

    /** Optional: base font size in px (default 12). */
    public void setBaseFontPx(int px) {
        if (px >= 8 && px <= 24) {
            this.baseFontPx = px;
            rebuildTemplate();
            render();
        }
    }

    /** Optional: control where the view jumps after append (default TOP to match old behavior). */
    public void setAutoScroll(AutoScroll mode) {
        this.autoScroll = (mode != null) ? mode : AutoScroll.TOP;
    }

    @Override
    public String getText() {
        // Return the raw document text (HTML), without recursion on exception.
        Document doc = this.getDocument();
        try {
            return doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
            return super.getText();
        }
    }

    public void clear() {
        runOnEdt(() -> {
            html.setLength(0);
            render();
        });
    }

    public void append(String content) {
        runOnEdt(() -> {
            html.append(content != null ? content : "");
            render();
        });
    }

    public void append(String tag, String content) {
        runOnEdt(() -> {
            String safeTag = (tag == null || tag == "") ? "div" : tag.trim();
            html.append("<").append(safeTag).append(">")
                .append(content != null ? content : "")
                .append("</").append(safeTag).append(">");
            render();
        });
    }

    public JScrollPane getPane() {
        JScrollPane scroll = new JScrollPane(
                this,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );

        if (dim != null) {
            scroll.setPreferredSize(dim);
        }

        // Remove borders for a clean look (matches original intent).
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }

    // -------------------- internals --------------------

    private void init() {
        setEditable(false);
        setContentType("text/html; charset=UTF-8");

        // Remove margins and borders from JEditorPane (original behavior).
        setMargin(new Insets(0, 0, 0, 0));
        setBorder(BorderFactory.createEmptyBorder());

        // Helps prevent odd background painting in some LAFs.
        setOpaque(true);

        rebuildTemplate();
        render();
    }

    private void rebuildTemplate() {
        // Keep CSS to what Swing's HTML/CSS parser reliably supports (very limited).
        // Avoid: :root, CSS variables (--x), nth-child, hover, complex selectors, and CSS comments.

        String fontStack =
                "'" + font + "', " +
                "SansSerif"; // Swing-safe fallback

        header =
            "<!doctype html>\n" +
            "<html>\n" +
            "<head>\n" +
            "  <meta charset=\"utf-8\" />\n" +
            "  <style type=\"text/css\">\n" +
            "    body {\n" +
            "      margin: 0;\n" +
            "      padding: 8px;\n" +
            "      background-color: " + background + ";\n" +
            "      color: " + color + ";\n" +
            "      font-family: " + fontStack + ";\n" +
            "      font-size: 12px;\n" +
            "      line-height: 1.4;\n" +
            "    }\n" +
            "    h1, h2, h3 {\n" +
            "      margin: 0 0 6px 0;\n" +
            "      padding: 0;\n" +
            "      font-weight: bold;\n" +
            "      color: " + color + ";\n" +
            "    }\n" +
            "    h2 { font-size: 14px; }\n" +
            "    p {\n" +
            "      margin: 0 0 6px 0;\n" +
            "    }\n" +
            "    a {\n" +
            "      color: #2563EB;\n" +
            "      text-decoration: none;\n" +
            "    }\n" +
            "    table {\n" +
            "      width: 100%;\n" +
            "      border-collapse: collapse;\n" +
            "      margin: 0 0 8px 0;\n" +
            "    }\n" +
            "    th {\n" +
            "      text-align: left;\n" +
            "      padding: 6px;\n" +
            "      font-weight: bold;\n" +
            "      background-color: #F3F4F6;\n" +
            "      border: 1px solid #E5E7EB;\n" +
            "      vertical-align: top;\n" +
            "      white-space: nowrap;\n" +
            "    }\n" +
            "    td {\n" +
            "      text-align: left;\n" +
            "      padding: 6px;\n" +
            "      border: 1px solid #E5E7EB;\n" +
            "      vertical-align: top;\n" +
            "    }\n" +
            "    .card {\n" +
            "      border: 1px solid #E5E7EB;\n" +
            "      background-color: #FAFAFA;\n" +
            "      padding: 8px;\n" +
            "      margin: 0 0 8px 0;\n" +
            "    }\n" +
            "    .muted { color: #6B7280; }\n" +
            "    .log-line {\n" +
            "      margin: 0;\n" +
            "      padding: 2px 0;\n" +
            "      border-bottom: 1px dotted #E5E7EB;\n" +
            "    }\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n";

        footer =
            "</body>\n" +
            "</html>\n";
    }

    private void render() {
        if (dim != null) {
            setPreferredSize(dim);
        }

        setText(header + html + footer);

        // Keep original behavior (TOP) unless changed.
        if (autoScroll == AutoScroll.TOP) {
            setCaretPosition(0);
        } else if (autoScroll == AutoScroll.BOTTOM) {
            int len = getDocument() != null ? getDocument().getLength() : 0;
            setCaretPosition(Math.max(0, len));
        }
    }

    private void runOnEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }
}