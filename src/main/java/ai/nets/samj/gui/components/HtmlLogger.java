/*-
 * #%L
 * Library to call models of the family of SAM (Segment Anything Model) from Java
 * %%
 * Copyright (C) 2024 SAMJ developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ai.nets.samj.gui.components;


import java.awt.Color;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import ai.nets.samj.gui.HTMLPane;

public class HtmlLogger {

    private final HTMLPane pane;

    private int waitingIter = 0;
    private int lastWorkingOffset = -1;
    private int lastWorkingLength = 0;

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    public HtmlLogger(HTMLPane pane) {
        this.pane = pane;
    }

    public void clear() {
        pane.clear();
        waitingIter = 0;
        lastWorkingOffset = -1;
        lastWorkingLength = 0;
    }

    public void log(String text, Color color) {
        if (text == null) return;

        final boolean empty = text.trim().isEmpty();
        final String msg = empty ? workingMessage() : text;

        if (!empty) {
            waitingIter = 0;
            lastWorkingOffset = -1;
            lastWorkingLength = 0;
        }

        final String fragment = toHtml(msg, color);

        SwingUtilities.invokeLater(() -> {
            try {
                HTMLDocument doc = (HTMLDocument) pane.getDocument();
                HTMLEditorKit kit = (HTMLEditorKit) pane.getEditorKit();

                if (empty && lastWorkingOffset >= 0) {
                    doc.remove(lastWorkingOffset, lastWorkingLength);
                }

                int before = doc.getLength();
                kit.insertHTML(doc, before, fragment, 0, 0, null);
                int after = doc.getLength();

                if (empty) {
                    lastWorkingOffset = before;
                    lastWorkingLength = after - before;
                }

                pane.setCaretPosition(doc.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /* ---------------- private helpers ---------------- */

    private String workingMessage() {
        String working = "Working, this might take several minutes";
        String dots;

        int mod = waitingIter % 3;
        if (mod == 0) dots = " .";
        else if (mod == 1) dots = " . .";
        else dots = " . . .";

        waitingIter++;

        return LocalDateTime.now().format(DATE) + " -- " + working + dots;
    }

    private static String toHtml(String text, Color color) {
        text = escape(text)
                .replace("\n", "<br>")
                .replace("  ", "&ensp;")
                .replace(" ", "&nbsp;");

        if (color != null) {
            text = "<span style='color:" + toHex(color) + "'>" + text + "</span>";
        }

        return text + "<br>";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private static String toHex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}