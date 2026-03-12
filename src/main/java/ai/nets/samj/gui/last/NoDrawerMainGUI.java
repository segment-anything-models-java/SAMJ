package ai.nets.samj.gui.last;

import javax.swing.JButton;
import javax.swing.JPanel;

public class NoDrawerMainGUI extends JPanel {

    private static final long serialVersionUID = 5067309009579235529L;

    protected JButton close = new JButton("Close");
    protected JButton help = new JButton("Help");
    protected TitleGUI titleGui;
    protected SelectionPanel selectionPanel;
    protected Center centerPanel;
    protected BottomPanel bottomPanel;

    // Tune these
    private static final double TITLE_H_PCT     = 0.10;
    private static final double SELECTION_H_PCT = 0.225;
    private static final double CENTER_H_PCT    = 0.425;
    private static final double BOTTOM_H_PCT    = 0.20;

    private static final int BUTTONS_GAP_PX = 4;

    public NoDrawerMainGUI() {
        setLayout(null);

        titleGui = new TitleGUI();
        selectionPanel = new SelectionPanel();
        centerPanel = new Center();
        bottomPanel = new BottomPanel();

        add(titleGui);
        add(selectionPanel);
        add(centerPanel);
        add(bottomPanel);
        add(close);
        add(help);

        this.setTwoThirdsEnabled(false);
    }

    @Override
    public void doLayout() {
        final int gap = BUTTONS_GAP_PX;

        final int wTit = getWidth();
        final int w = Math.max(0, wTit - gap * 2);
        final int h = getHeight();

        final int hTitle     = (int) Math.round(h * TITLE_H_PCT);
        final int hSelection = Math.max(0, -gap * 2 + (int) Math.round(h * SELECTION_H_PCT));
        final int hCenter    = Math.max(0, -gap * 2 + (int) Math.round(h * CENTER_H_PCT));
        final int hBottom    = Math.max(0, -gap * 2 + (int) Math.round(h * BOTTOM_H_PCT));

        int y = 0;

        // Row 1
        titleGui.setBounds(0, y, wTit, hTitle);
        y += hTitle + gap;

        // Row 2
        selectionPanel.setBounds(gap, y, w, hSelection);
        y += hSelection + gap;

        // Row 3
        centerPanel.setBounds(gap, y, w, hCenter);
        y += hCenter + gap;

        // Row 4
        bottomPanel.setBounds(gap, y, w, hBottom);
        y += hBottom + gap;

        // Row 5 (remaining)
        int hButtons = h - y - gap * 2;
        if (hButtons < 0) hButtons = 0;

        int W = w / 2;
        close.setBounds(gap, y, W, hButtons);
        help.setBounds(gap + W, y, W, hButtons);
    }

    protected void setTwoThirdsEnabled(boolean enabled) {
        // keep as in your original (empty or implement)
    }
}