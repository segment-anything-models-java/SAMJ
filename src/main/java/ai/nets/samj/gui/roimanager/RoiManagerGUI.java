package ai.nets.samj.gui.roimanager;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ItemListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionListener;

public abstract class RoiManagerGUI extends JPanel implements ListSelectionListener, MouseListener, MouseWheelListener, ItemListener {

    private static final long serialVersionUID = -8405747451234902128L;

    protected boolean calibrationReady = false;

    protected RoiTableModel listModel;
    protected RoiTable list;
    protected JPanel panel;
    protected JCheckBox showAllCheckbox = new JCheckBox("Show All", true);
    protected JCheckBox labelsCheckbox = new JCheckBox("Labels", true);

    protected List<JButton> btns = new ArrayList<JButton>();

    private static final int BUTTONS = 11;

    public RoiManagerGUI() {
        setLayout(new BorderLayout());

        list = new RoiTable();
        listModel = list.getRoiTableModel();

        JScrollPane scrollPane = new JScrollPane(
                list,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        add(BorderLayout.CENTER, scrollPane);

        panel = new JPanel();
        panel.setLayout(new GridLayout(BUTTONS, 1, 5, 0));

        addButton("Add");
        addButton("Delete");
        addButton("Points");
        addButton("Dilate");
        addButton("Erode");
        addButton("IJManager");
        addButton("YOLO");
        addButton("IJ");
        addButton("GeoJson");

        panel.add(showAllCheckbox);
        panel.add(labelsCheckbox);
        add(BorderLayout.EAST, panel);

        list.addListSelectionListener(this);
        list.addMouseListener(this);
        list.addMouseWheelListener(this);
        labelsCheckbox.addItemListener(this);
        showAllCheckbox.addItemListener(this);
    }
    
    public void updateButtonsEnabled() {
    	int nRois = this.listModel.getRowCount();
    	int selectedInd = list.getSelectedIndex();
    	for (JButton btn : this.btns) {
    		if (btn.getText().equals("Add") || btn.getText().equals("Merge")) {
    			btn.setEnabled(true);
    			continue;
    		}
    		btn.setEnabled(nRois > 0);
    	}
    }

    public void block(boolean block) {
        for (JButton b : btns) {
            if (!block && !calibrationReady && b.getText().equals("Export LMD"))
                continue;
            b.setEnabled(!block);
        }
        list.setEnabled(!block);
        showAllCheckbox.setEnabled(!block);
        labelsCheckbox.setEnabled(!block);
    }

    public void readyToExport(boolean isCalibrated) {
        calibrationReady = isCalibrated;
        if (!list.isEnabled())
            return;

        for (JButton b : btns) {
            if (b.getText().equals("Export LMD")) {
                b.setEnabled(isCalibrated);
                break;
            }
        }
    }

    public RoiTable getList() {
        return list;
    }

    protected abstract void addButton(String label);
}