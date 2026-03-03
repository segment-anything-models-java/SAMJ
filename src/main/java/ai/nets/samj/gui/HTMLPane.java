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
        // Build a modern HTML template and keep the "append content into body" behavior.
        // We wrap user content inside a .container for nicer spacing and readable layout.
        String systemFontStack =
                "'" + font + "', " +
                "system-ui, -apple-system, 'Segoe UI', Roboto, Helvetica, Arial, " +
                "'Apple Color Emoji', 'Segoe UI Emoji'";

        header =
            "<!doctype html>\n" +
            "<html>\n" +
            "<head>\n" +
            "  <meta charset=\"utf-8\" />\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n" +
            "  <style>\n" +
            "    :root {\n" +
            "      --bg: " + background + ";\n" +
            "      --fg: " + color + ";\n" +
            "      --muted: #6B7280;\n" +          // gray-500-ish
            "      --border: #E5E7EB;\n" +         // gray-200-ish
            "      --card: #F9FAFB;\n" +           // gray-50-ish
            "      --accent: " + accent + ";\n" +
            "      --radius: 10px;\n" +
            "      --shadow: 0 1px 2px rgba(0,0,0,.06), 0 6px 18px rgba(0,0,0,.06);\n" +
            "    }\n" +
            "\n" +
            "    * { box-sizing: border-box; }\n" +
            "    html, body { height: 100%; }\n" +
            "    body {\n" +
            "      margin: 0;\n" +
            "      padding: 0;\n" +
            "      background: var(--bg);\n" +
            "      color: var(--fg);\n" +
            "      font-family: " + systemFontStack + ";\n" +
            "      font-size: " + baseFontPx + "px;\n" +
            "      line-height: 1.45;\n" +
            "      -webkit-font-smoothing: antialiased;\n" +
            "      -moz-osx-font-smoothing: grayscale;\n" +
            "    }\n" +
            "\n" +
            "    .container {\n" +
            "      padding: 10px 12px;\n" +
            "    }\n" +
            "\n" +
            "    h1, h2, h3 {\n" +
            "      margin: 0 0 8px 0;\n" +
            "      line-height: 1.2;\n" +
            "      letter-spacing: -0.01em;\n" +
            "    }\n" +
            "    h1 { font-size: 1.35em; }\n" +
            "    h2 { font-size: 1.15em; }\n" +
            "    h3 { font-size: 1.05em; }\n" +
            "\n" +
            "    p { margin: 0 0 8px 0; }\n" +
            "    small, .muted { color: var(--muted); }\n" +
            "\n" +
            "    a {\n" +
            "      color: var(--accent);\n" +
            "      text-decoration: none;\n" +
            "    }\n" +
            "    a:hover { text-decoration: underline; }\n" +
            "\n" +
            "    hr {\n" +
            "      border: 0;\n" +
            "      border-top: 1px solid var(--border);\n" +
            "      margin: 10px 0;\n" +
            "    }\n" +
            "\n" +
            "    /* Card utility (optional): wrap sections in <div class=\"card\"> ... */\n" +
            "    .card {\n" +
            "      background: var(--card);\n" +
            "      border: 1px solid var(--border);\n" +
            "      border-radius: var(--radius);\n" +
            "      padding: 10px 12px;\n" +
            "      box-shadow: var(--shadow);\n" +
            "      margin: 0 0 10px 0;\n" +
            "    }\n" +
            "\n" +
            "    /* Badges (optional): <span class=\"badge\">text</span> */\n" +
            "    .badge {\n" +
            "      display: inline-block;\n" +
            "      padding: 2px 8px;\n" +
            "      border: 1px solid var(--border);\n" +
            "      border-radius: 999px;\n" +
            "      background: #fff;\n" +
            "      color: var(--muted);\n" +
            "      font-size: 0.9em;\n" +
            "      margin-left: 6px;\n" +
            "      vertical-align: middle;\n" +
            "    }\n" +
            "\n" +
            "    /* Tables */\n" +
            "    table {\n" +
            "      width: 100%;\n" +
            "      border-collapse: separate;\n" +
            "      border-spacing: 0;\n" +
            "      border: 1px solid var(--border);\n" +
            "      border-radius: var(--radius);\n" +
            "      overflow: hidden;\n" +
            "      background: #fff;\n" +
            "      margin: 0 0 10px 0;\n" +
            "    }\n" +
            "    thead th {\n" +
            "      background: var(--card);\n" +
            "      color: var(--fg);\n" +
            "      font-weight: 600;\n" +
            "      padding: 8px 10px;\n" +
            "      border-bottom: 1px solid var(--border);\n" +
            "      text-align: left;\n" +
            "      white-space: nowrap;\n" +
            "    }\n" +
            "    tbody td {\n" +
            "      padding: 7px 10px;\n" +
            "      border-bottom: 1px solid var(--border);\n" +
            "      vertical-align: top;\n" +
            "    }\n" +
            "    tbody tr:last-child td { border-bottom: 0; }\n" +
            "    tbody tr:nth-child(even) td { background: #FCFCFD; }\n" +
            "    tbody tr:hover td { background: #F3F4F6; }\n" +
            "\n" +
            "    /* Code blocks */\n" +
            "    code {\n" +
            "      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;\n" +
            "      font-size: 0.95em;\n" +
            "      background: #F3F4F6;\n" +
            "      border: 1px solid var(--border);\n" +
            "      border-radius: 6px;\n" +
            "      padding: 1px 5px;\n" +
            "    }\n" +
            "    pre {\n" +
            "      margin: 0 0 10px 0;\n" +
            "      padding: 10px 12px;\n" +
            "      border-radius: var(--radius);\n" +
            "      background: #0B1220;\n" +
            "      color: #E5E7EB;\n" +
            "      overflow-x: auto;\n" +
            "      border: 1px solid rgba(255,255,255,.08);\n" +
            "    }\n" +
            "    pre code {\n" +
            "      background: transparent;\n" +
            "      border: 0;\n" +
            "      padding: 0;\n" +
            "      color: inherit;\n" +
            "    }\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "  <div class=\"container\">\n";

        footer =
            "  </div>\n" +
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