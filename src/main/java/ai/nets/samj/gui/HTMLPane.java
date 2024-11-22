package ai.nets.samj.gui;

import java.awt.Dimension;

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

    public HTMLPane() {
        create();
    }

    public HTMLPane(String font) {
        this.font = font;
        create();
    }

    public HTMLPane(int width, int height) {
        this.dim = new Dimension(width, height);
        create();
    }

    public HTMLPane(String font, int width, int height) {
        this.font = font;
        this.dim = new Dimension(width, height);
        create();
    }

    public HTMLPane(String font, String color, String background, int width, int height) {
        this.font = font;
        this.dim = new Dimension(width, height);
        this.color = color;
        this.background = background;
        create();
    }

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

    public void clear() {
        html = "";
        append("");
    }

    private void create() {
        header += "<!DOCTYPE HTML>\n";
        header += "<html><head>\n";
        header += "<style>\n";
        header += "body {\n";
        header += "    background-color: " + background + ";\n";
        header += "    color: " + color + ";\n";
        header += "    font-family: '" + font + "', sans-serif;\n";
        header += "    margin: 5px;\n";
        header += "    font-size: 9px;\n"; // Base font size reduced
        header += "}\n";
        header += "h2 {\n";
        header += "    color: #333333;\n";
        header += "    font-size: 1.2em;\n"; // Reduced font size
        header += "    margin-bottom: 5px;\n";
        header += "    border-bottom: 1px solid #e0e0e0;\n";
        header += "    padding-bottom: 5px;\n";
        header += "}\n";
        header += "table {\n";
        header += "    width: 100%;\n";
        header += "    border-collapse: collapse;\n";
        header += "    margin-bottom: 5px;\n";
        header += "}\n";
        header += "th, td {\n";
        header += "    text-align: left;\n";
        header += "    padding: 4px;\n"; // Reduced padding
        header += "    font-size: 1.0em;\n"; // Reduced font size
        header += "}\n";
        header += "th {\n";
        header += "    background-color: #f2f2f2;\n";
        header += "    width: 40%;\n"; // Adjusted width
        header += "}\n";
        header += "tr:nth-child(even) td {\n";
        header += "    background-color: #fafafa;\n";
        header += "}\n";
        header += "a {\n";
        header += "    color: #1a0dab;\n";
        header += "    text-decoration: none;\n";
        header += "}\n";
        header += "a:hover {\n";
        header += "    text-decoration: underline;\n";
        header += "}\n";
        header += "</style>\n";
        header += "</head>\n";
        header += "<body>\n";
        footer += "</body></html>\n";
        setEditable(false);
        setContentType("text/html; charset=UTF-8");
    }

    public void append(String content) {
        html += content;
        setText(header + html + footer);
        if (dim != null) {
            setPreferredSize(dim);
        }
        setCaretPosition(0);
    }

    public void append(String tag, String content) {
        html += "<" + tag + ">" + content + "</" + tag + ">";
        setText(header + html + footer);
        if (dim != null) {
            setPreferredSize(dim);
        }
        setCaretPosition(0);
    }

    public JScrollPane getPane() {
        JScrollPane scroll = new JScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setPreferredSize(dim);
        return scroll;
    }
}
