/*-
 * #%L
 * Library to call models of the family of SAM (Segment Anything Model) from Java
 * %%
 * Copyright (C) 2024 - 2026 SAMJ developers.
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
package ai.nets.samj.gui;

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * This class extends the Java JEditorPane to make an easy-to-use panel to
 * display HTML information.
 * 
 */
public class HTMLPane extends JEditorPane {

    private String html = "";
    private String header = "";
    private String footer = "";
    private Dimension dim;
    private String font = "Segoe UI";
    private String color = "#333333";
    private String background = "#FFFFFF";

    /**
     * Creates an HTML pane using the default font and no preferred size.
     */
    public HTMLPane() {
        create();
    }

    /**
     * Creates an HTML pane using the specified font family.
     *
     * @param font font family to use in the generated HTML
     */
    public HTMLPane(String font) {
        this.font = font;
        create();
    }

    /**
     * Creates an HTML pane with a preferred size.
     *
     * @param width preferred width in pixels
     * @param height preferred height in pixels
     */
    public HTMLPane(int width, int height) {
        this.dim = new Dimension(width, height);
        create();
    }

    /**
     * Creates an HTML pane with a custom font and preferred size.
     *
     * @param font font family to use in the generated HTML
     * @param width preferred width in pixels
     * @param height preferred height in pixels
     */
    public HTMLPane(String font, int width, int height) {
        this.font = font;
        this.dim = new Dimension(width, height);
        create();
    }

    /**
     * Creates an HTML pane with custom colors, font, and preferred size.
     *
     * @param font font family to use in the generated HTML
     * @param color text color as a CSS value
     * @param background background color as a CSS value
     * @param width preferred width in pixels
     * @param height preferred height in pixels
     */
    public HTMLPane(String font, String color, String background, int width, int height) {
        this.font = font;
        this.dim = new Dimension(width, height);
        this.color = color;
        this.background = background;
        create();
    }

    /**
     * Returns the plain text currently stored in the document.
     *
     * @return the document text
     */
    @Override
    public String getText() {
        Document doc = this.getDocument();
        try {
            return doc.getText(0, doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
            return getText();
        }
    }

    /**
     * Removes all HTML body content from the pane.
     */
    public void clear() {
        html = "";
        append("");
    }

    private void create() {
        header += "<!DOCTYPE HTML>\n";
        header += "<html><head>\n";
        header += "<style>\n";
        // CSS Reset
        header += "* {\n";
        header += "    margin: 0;\n";
        header += "    padding: 0;\n";
        header += "    box-sizing: border-box;\n";
        header += "}\n";
        header += "body {\n";
        header += "    background-color: " + background + ";\n";
        header += "    color: " + color + ";\n";
        header += "    font-family: '" + font + "', sans-serif;\n";
        header += "    font-size: 9px;\n"; // Base font size reduced
        header += "}\n";
        header += "h2 {\n";
        header += "    color: #333333;\n";
        header += "    font-size: 1.2em;\n"; // Adjust as needed
        // No margin or padding
        header += "}\n";
        header += "table {\n";
        header += "    width: 100%;\n";
        header += "    border-collapse: collapse;\n";
        // No margin or padding
        header += "}\n";
        header += "th, td {\n";
        header += "    text-align: left;\n";
        header += "    padding: 4px;\n"; // Adjust as needed
        header += "    font-size: 1.0em;\n";
        header += "}\n";
        // Additional styles...
        header += "</style>\n";
        header += "</head>\n";
        header += "<body>\n";
        footer += "</body></html>\n";
        setEditable(false);
        setContentType("text/html; charset=UTF-8");
        // Remove margins and borders from JEditorPane
        this.setMargin(new Insets(0, 0, 0, 0));
        this.setBorder(BorderFactory.createEmptyBorder());
    }

    /**
     * Appends raw HTML content to the document body.
     *
     * @param content HTML fragment to append
     */
    public void append(String content) {
        html += content;
        setText(header + html + footer);
        if (dim != null) {
            setPreferredSize(dim);
        }
        setCaretPosition(0);
    }

    /**
     * Appends content wrapped in the supplied HTML tag.
     *
     * @param tag HTML tag name to wrap around the content
     * @param content text or HTML fragment to append
     */
    public void append(String tag, String content) {
        html += "<" + tag + ">" + content + "</" + tag + ">";
        setText(header + html + footer);
        if (dim != null) {
            setPreferredSize(dim);
        }
        setCaretPosition(0);
    }

    /**
     * Returns this editor pane wrapped in a scroll pane.
     *
     * @return a scroll pane containing this HTML pane
     */
    public JScrollPane getPane() {
        JScrollPane scroll = new JScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setPreferredSize(dim);
        // Remove borders from JScrollPane
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setViewportBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }
}
