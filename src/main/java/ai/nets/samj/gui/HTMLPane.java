package ai.nets.samj.gui;

import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * HTML pane with Swing-safe CSS + adaptive font sizing.
 * - Font size is derived from component height, clamped to [minFontPx..baseFontPx]
 * - If the computed size would be below minFontPx, we clamp to minFontPx and force scrollbars
 *   (when inside a JScrollPane).
 */
public class HTMLPane extends JEditorPane {

    private static final long serialVersionUID = 1729035616751891903L;

    private final StringBuilder html = new StringBuilder();

    private String header = "";
    private String footer = "";

    private String font = "Segoe UI";
    private String color = "#111827";
    private String background = "#FFFFFF";
    private String accent = "#2563EB";

    /** Max font size (also your "normal" font size). */
    private int baseFontPx = 12;

    /** Minimum font size; if scaling wants below this, we keep this and force scrollbars. */
    private int minFontPx = 10;

    /** Current applied CSS font size. */
    private int currentFontPx = baseFontPx;

    /** Controls scaling: desiredFont = floor(height / fontScaleDivisor). */
    private int fontScaleDivisor = 34;

    /** Whether scaling is enabled. */
    private boolean autoFontScalingEnabled = true;

    /** If true, we force scrollbars in the wrapper/ancestor JScrollPane. */
    private boolean forceScrollbars = false;
    
    private boolean initialRenderDone = false;

    /** Debounce resize recalculations. */
    private final Timer resizeDebounce = new Timer(120, e -> updateAdaptiveFontFromSize());

    public enum AutoScroll { TOP, BOTTOM, NONE }
    private AutoScroll autoScroll = AutoScroll.TOP;

    public HTMLPane() {
        init();
    }

    public HTMLPane(String font) {
        this.font = font;
        init();
    }

    public HTMLPane(String font, String color, String background) {
        this.font = font;
        this.background = background;
        init();
    }

    /** Optional: adjust theme at runtime, then re-render current content. */
    public void setTheme(String font, String color, String background) {
        this.font = (font != null && !font.trim().isEmpty()) ? font : this.font;
        this.color = (color != null && !color.trim().isEmpty()) ? color : this.color;
        this.background = (background != null && !background.trim().isEmpty()) ? background : this.background;
        rebuildTemplate();
        render();
        updateAdaptiveFontFromSize();
    }

    /** Optional: accent color used for links. */
    public void setAccent(String accentHex) {
        if (accentHex != null && !accentHex.trim().isEmpty()) {
            this.accent = accentHex;
            rebuildTemplate();
            render();
        }
    }

    /** Optional: max/base font size in px (default 12). */
    public void setBaseFontPx(int px) {
        if (px >= 8 && px <= 24) {
            this.baseFontPx = px;
            if (currentFontPx > baseFontPx) currentFontPx = baseFontPx;
            updateAdaptiveFontFromSize();
        }
    }

    /** Optional: minimum font size in px (default 8). */
    public void setMinFontPx(int px) {
        if (px >= 6 && px <= 16) {
            this.minFontPx = px;
            updateAdaptiveFontFromSize();
        }
    }

    /** Optional: tweak scaling aggressiveness (bigger divisor => smaller text). Default 18. */
    public void setFontScaleDivisor(int divisor) {
        if (divisor >= 10 && divisor <= 60) {
            this.fontScaleDivisor = divisor;
            updateAdaptiveFontFromSize();
        }
    }

    /** Optional: enable/disable adaptive font scaling. */
    public void setAutoFontScalingEnabled(boolean enabled) {
        this.autoFontScalingEnabled = enabled;
        updateAdaptiveFontFromSize();
    }

    /** Optional: control where the view jumps after append (default TOP). */
    public void setAutoScroll(AutoScroll mode) {
        this.autoScroll = (mode != null) ? mode : AutoScroll.TOP;
    }

    @Override
    public String getText() {
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
            String safeTag = (tag == null || tag.trim().isEmpty()) ? "div" : tag.trim();
            html.append("<").append(safeTag).append(">")
                .append(content != null ? content : "")
                .append("</").append(safeTag).append(">");
            render();
        });
    }

    // -------------------- internals --------------------

    private void init() {
        setEditable(false);
        setContentType("text/html; charset=UTF-8");

        setMargin(new Insets(0, 0, 0, 0));
        setBorder(BorderFactory.createEmptyBorder());
        setOpaque(true);

        resizeDebounce.setRepeats(false);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (!autoFontScalingEnabled) return;
                resizeDebounce.restart();
            }
        });

        rebuildTemplate(); // builds with currentFontPx (baseFontPx initially)
        setText(header + footer); // tiny empty doc, optional

        runOnEdt(new Runnable() {
            @Override
            public void run() {
                ensureInitializedOnce();
            }
        });
    }

    private void rebuildTemplate() {
        String fontStack = "'" + font + "', SansSerif";

        int h2Size = Math.max(currentFontPx + 2, currentFontPx);

        header =
            "<!doctype html>\n" +
            "<html>\n" +
            "<head>\n" +
            "  <meta charset=\"utf-8\" />\n" +
            "  <style type=\"text/css\">\n" +
            "    body {\n" +
            "      margin: 0;\n" +
            "      padding: 6px 6px 6px 6px;\n" +
            "      background-color: " + background + ";\n" +
            "      color: " + color + ";\n" +
            "      font-family: " + fontStack + ";\n" +
            "      font-size: " + currentFontPx + "px;\n" +
            "      line-height: 1.4;\n" +
            "    }\n" +
            "    h1, h2, h3 {\n" +
            "      margin: 0;\n" +
            "      padding: 0;\n" +
            "      font-weight: bold;\n" +
            "      color: " + color + ";\n" +
            "    }\n" +
            "    h2 { font-size: " + h2Size + "px; margin: 0 0 4px 0; }\n" +
            "    p { margin: 0 0 6px 0; }\n" +
            "    a { color: " + accent + "; text-decoration: none; }\n" +
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
            "      padding: 6px;\n" +
            "      margin: 0 0 6px 0;\n" +
            "    }\n" +
            "    .notice {\n" +
            "      border: 1px solid #FCA5A5;\n" +
            "      background-color: #FEF2F2;\n" +
            "      color: #B91C1C;\n" +
            "      padding: 6px;\n" +
            "      margin: 0 0 6px 0;\n" +
            "      text-align: center;\n" +     // ✅ center
            "      font-weight: bold;\n" +      // ✅ emphasis
            "    }\n" +
            "    .caution {\n" +
            "      border: 1px solid #A7F3D0;\n" +
            "      background-color: #ECFDF5;\n" +
            "      color: #065F46;\n" +
            "      padding: 6px;\n" +
            "      margin: 0 0 6px 0;\n" +
            "      text-align: center;\n" +
            "      font-weight: bold;\n" +
            "    }\n" +
            "    .caution .icon {\n" +
            "      padding-right: 6px;\n" +
            "    }\n" +
            "    .notice .icon {\n" +
            "      padding-right: 6px;\n" +
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

        setText(header + html + footer);

        if (autoScroll == AutoScroll.TOP) {
            setCaretPosition(0);
        } else if (autoScroll == AutoScroll.BOTTOM) {
            int len = getDocument() != null ? getDocument().getLength() : 0;
            setCaretPosition(Math.max(0, len));
        }
    }
    
    private void ensureInitializedOnce() {
        if (initialRenderDone) return;

        int h = getVisibleRect().height;
        if (h <= 0) {
            // Not laid out yet, try again shortly (still on EDT)
            SwingUtilities.invokeLater(new Runnable() {
                @Override public void run() { ensureInitializedOnce(); }
            });
            return;
        }

        // Now we have a real size: compute font once, then render once.
        recomputeFontFromHeight(h);
        rebuildTemplate();
        render();

        initialRenderDone = true;
    }

    private void recomputeFontFromHeight(int h) {
        int desired = baseFontPx;
        if (h > 0) {
            desired = (int) Math.floor(h / (double) fontScaleDivisor);
            if (desired > baseFontPx) desired = baseFontPx;
        }

        forceScrollbars = desired < minFontPx;
        if (forceScrollbars) desired = minFontPx;

        currentFontPx = desired;
    }

    private void updateAdaptiveFontFromSize() {
        if (!initialRenderDone) return; // avoid “jump” before first render is finalized

        int h = getVisibleRect().height;
        if (h <= 0) return;

        int old = currentFontPx;

        recomputeFontFromHeight(h);

        if (currentFontPx != old) {
            rebuildTemplate();
            render();
        }
        // if you also want scroll policy changes, do it here (in your scroll wrapper)
    }

    private void runOnEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }
}