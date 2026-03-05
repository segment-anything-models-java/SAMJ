package ai.nets.samj.gui.last;

import java.awt.Color;
import java.awt.Desktop;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Position;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import ai.nets.samj.gui.HTMLPane;

/**
 * Appends small HTML fragments to an {@link HTMLPane} using the underlying {@link HTMLDocument}
 * (fast, avoids rebuilding the entire HTML each time).
 *
 * Behavior:
 *  - log(null, ...) => no-op
 *  - log(blank/whitespace, ...) => writes/updates an animated "Working..." line
 *  - log(non-empty, ...) => appends the message as a new line
 *
 * Note: Avoid mixing this logger with HTMLPane.append()/setText() elsewhere on the same pane.
 */
public final class HtmlLogger {

    private final HTMLPane pane;

    private int workingTick = 0;
    private Position workingStart = null;
    private int workingLength = 0;
    private AtomicLong startMillis;
    private AtomicLong lastMessageMillis = new AtomicLong(0);

    private String workingBaseText = "Working, this might take several minutes";

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    private static final long MINIMUM_INTERVAL = 1200;

    public HtmlLogger(HTMLPane pane) {
        this.pane = Objects.requireNonNull(pane, "pane");
        installDefaultLinkHandler();
    }

    /** Clears pane + resets internal working animation state. */
    public void clear() {
        runOnEdt(() -> {
            // replaces pane.clear()
            pane.setBodyHtml("");
            resetWorkingState();
        });
    }

    /** Convenience: no color. */
    public void log(String text) {
        log(text, null);
    }

    /**
     * Logs text with an optional color.
     * If text is blank/whitespace, an animated "Working..." line is written/updated.
     */
    public void log(String text, Color color) {
        if (text == null) return;

        final boolean isWorkingTick = text.trim().isEmpty();
        if (isWorkingTick && (System.currentTimeMillis() - this.lastMessageMillis.get()) < MINIMUM_INTERVAL)
        	return;
        else if (!isWorkingTick)
        	lastMessageMillis.set(System.currentTimeMillis());
        final String msg = isWorkingTick ? buildWorkingMessage() : text;

        // Only reset "working offsets" when a real message arrives
        if (!isWorkingTick) {
            resetWorkingState();
        }

        final String fragment = buildHtmlFragment(msg, color);

        runOnEdt(new Runnable() {
            @Override
            public void run() {
                try {
                    HTMLDocument doc = requireHtmlDocument();
                    HTMLEditorKit kit = requireHtmlEditorKit();                    

                    if (isWorkingTick && startMillis == null) {
                        startMillis = new AtomicLong(System.currentTimeMillis());
                    } else if (!isWorkingTick) {
                    	startMillis = null;
                    }
                    // If we're updating the working line, remove the previous working fragment first.
                    if (isWorkingTick && workingStart != null && workingLength > 0) {
                        safeRemove(doc, workingStart.getOffset(), workingLength);
                        workingStart = null;
                        workingLength = 0;
                    }

                    // Append at end.
                    int before = doc.getLength();
                    Position insertedAt = doc.createPosition(before);

                    kit.insertHTML(doc, before, fragment, 0, 0, null);

                    int after = doc.getLength();

                    // Track working fragment so we can replace it on the next tick.
                    if (isWorkingTick) {
                        workingStart = insertedAt;
                        workingLength = Math.max(0, after - before);
                    }

                    // Auto-scroll to bottom for logs.
                    pane.setCaretPosition(doc.getLength());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /** Optional: set the base "working" message text (dots are appended automatically). */
    public void setWorkingBaseText(String baseText) {
        if (baseText != null && baseText.trim().length() > 0) { // Java 8: no isBlank()
            this.workingBaseText = baseText;
        }
    }

    /* ---------------- internals ---------------- */

    private void resetWorkingState() {
        workingTick = 0;
        workingStart = null;
        workingLength = 0;
    }

    private String buildWorkingMessage() {
        String dots;
        int mod = workingTick % 3;
        if (mod == 0) dots = " .     ";
        else if (mod == 1) dots = " . .   ";
        else dots = " . . .";
        String elapsedTime;
        if (this.startMillis == null) {
        	elapsedTime = "[0s]";
        } else {
        	elapsedTime = "[" + (Math.round((System.currentTimeMillis() - startMillis.get()) / 100) / 10d) + "s]";
        }
        workingTick++;

        return LocalDateTime.now().format(TIME_FMT) + " -- " + workingBaseText + dots + elapsedTime;
    }

    private static String buildHtmlFragment(String text, Color color) {
        String safe = escapeHtml(text);
        safe = preserveNewlinesAndIndentation(safe);

        StringBuilder sb = new StringBuilder(128);
        sb.append("<div class='log-line'>");

        if (color != null) {
            sb.append("<span style='color:").append(toHex(color)).append(";'>")
              .append(safe)
              .append("</span>");
        } else {
            sb.append(safe);
        }

        sb.append("</div>");
        return sb.toString();
    }

    private static String preserveNewlinesAndIndentation(String escapedText) {
        // Java 8: no \R and no String.repeat()
        String normalized = escapedText.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1); // keep empty lines
        StringBuilder out = new StringBuilder(escapedText.length() + 32);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            int lead = 0;
            while (lead < line.length() && line.charAt(lead) == ' ') lead++;

            if (lead > 0) {
                for (int k = 0; k < lead; k++) out.append("&nbsp;");
            }

            // Preserve double spaces inside line minimally (still allows wrapping somewhat).
            String rest = line.substring(lead).replace("  ", "&nbsp; ");
            out.append(rest);

            if (i < lines.length - 1) out.append("<br>");
        }
        return out.toString();
    }

    private static String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

    private HTMLDocument requireHtmlDocument() {
        if (!(pane.getDocument() instanceof HTMLDocument)) {
            throw new IllegalStateException(
                    "HTMLPane document is not an HTMLDocument. Content type must be text/html.");
        }
        return (HTMLDocument) pane.getDocument();
    }

    private HTMLEditorKit requireHtmlEditorKit() {
        if (!(pane.getEditorKit() instanceof HTMLEditorKit)) {
            throw new IllegalStateException(
                    "HTMLPane editor kit is not an HTMLEditorKit. Content type must be text/html.");
        }
        return (HTMLEditorKit) pane.getEditorKit();
    }

    private static void safeRemove(HTMLDocument doc, int offset, int length) throws BadLocationException {
        if (length > 0) doc.remove(offset - length, length);
    }

    private void runOnEdt(Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) r.run();
        else SwingUtilities.invokeLater(r);
    }

    /**
     * Optional: make links clickable (opens system browser).
     * If you already add your own HyperlinkListener elsewhere, remove this.
     */
    private void installDefaultLinkHandler() {
        pane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                try {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED
                            && Desktop.isDesktopSupported()
                            && e.getURL() != null) {
                        URI uri = e.getURL().toURI();
                        Desktop.getDesktop().browse(uri);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}