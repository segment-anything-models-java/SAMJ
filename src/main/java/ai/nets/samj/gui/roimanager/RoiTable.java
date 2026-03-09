package ai.nets.samj.gui.roimanager;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

public class RoiTable extends JTable {

    private static final long serialVersionUID = 1L;

    private final RoiTableModel roiModel = new RoiTableModel();

    private static final Color ROW_LINE = new Color(215, 215, 215);
    private static final Color COL_LINE = new Color(235, 235, 235);
    private static final Color HEADER_BG = new Color(245, 245, 245);
    private static final Color CLASS_BG = new Color(248, 250, 255);
    private static final Color CLASS_FG = new Color(70, 90, 130);
    private static final Color SELECTION_BG = new Color(25, 64, 140);
    private static final Color SELECTION_FG = new Color(245, 248, 255);

    private final Font roiFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private final Font classFont = new Font(Font.SANS_SERIF, Font.ITALIC, 12);
    private final Font headerFont = new Font(Font.SANS_SERIF, Font.BOLD, 12);

    public RoiTable() {
        setModel(roiModel);

        setBackground(Color.WHITE);
        setForeground(new Color(30, 30, 30));
        setSelectionBackground(SELECTION_BG);
        setSelectionForeground(SELECTION_FG);
        setFillsViewportHeight(true);
        setRowHeight(24);

        setRowSelectionAllowed(true);
        setColumnSelectionAllowed(false);
        setCellSelectionEnabled(false);
        setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        getSelectionModel().addListSelectionListener(e -> repaint());

        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setSurrendersFocusOnKeystroke(true);
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        JTableHeader header = getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);
        header.setDefaultRenderer(new HeaderRenderer());

        getColumnModel().getColumn(0).setCellRenderer(new CellRenderer(0));
        getColumnModel().getColumn(1).setCellRenderer(new CellRenderer(1));

        JTextField editorField = new JTextField();
        editorField.setFont(classFont);
        editorField.setBackground(SELECTION_BG);
        editorField.setForeground(SELECTION_FG);
        editorField.setCaretColor(SELECTION_FG);
        editorField.setBorder(new CompoundBorder(
                new MatteBorder(1, 1, 1, 1, new Color(180, 200, 230)),
                new EmptyBorder(0, 4, 0, 4)
        ));

        DefaultCellEditor editor = new DefaultCellEditor(editorField);
        editor.setClickCountToStart(2); // double click on second column starts editing
        getColumnModel().getColumn(1).setCellEditor(editor);

        TableColumnModel cols = getColumnModel();
        cols.getColumn(0).setPreferredWidth(200);
        cols.getColumn(1).setPreferredWidth(140);
    }

    public RoiTableModel getRoiTableModel() {
        return roiModel;
    }

    public void addListSelectionListener(ListSelectionListener l) {
        getSelectionModel().addListSelectionListener(l);
    }

    public void removeListSelectionListener(ListSelectionListener l) {
        getSelectionModel().removeListSelectionListener(l);
    }

    public void setSelectedIndex(int index) {
        clearSelection();
        if (index < 0 || index >= roiModel.getRowCount()) return;
        int view = convertRowIndexToView(index);
        getSelectionModel().setSelectionInterval(view, view);
        scrollRectToVisible(getCellRect(view, 0, true));
    }

    public int getSelectedIndex() {
        int view = getSelectedRow();
        return view < 0 ? -1 : convertRowIndexToModel(view);
    }

    public int[] getSelectedIndices() {
        int[] viewRows = getSelectedRows();
        int[] modelRows = new int[viewRows.length];
        for (int i = 0; i < viewRows.length; i++) {
            modelRows[i] = convertRowIndexToModel(viewRows[i]);
        }
        return modelRows;
    }

    public void setSelectedIndices(int[] indices) {
        clearSelection();
        if (indices == null || indices.length == 0) return;

        ListSelectionModel sm = getSelectionModel();
        for (int index : indices) {
            if (index < 0 || index >= roiModel.getRowCount()) continue;
            int view = convertRowIndexToView(index);
            sm.addSelectionInterval(view, view);
        }
    }

    public void setValueIsAdjusting(boolean adjusting) {
        getSelectionModel().setValueIsAdjusting(adjusting);
    }

    public int locationToIndex(Point p) {
        int view = rowAtPoint(p);
        return view < 0 ? -1 : convertRowIndexToModel(view);
    }

    public int getLeadSelectionIndex() {
        int view = getSelectionModel().getLeadSelectionIndex();
        return view < 0 ? -1 : convertRowIndexToModel(view);
    }

    public String getSelectedLeft() {
        int row = getSelectedIndex();
        return row < 0 ? null : roiModel.getLeftAt(row);
    }

    public String getSelectedRight() {
        int row = getSelectedIndex();
        return row < 0 ? null : roiModel.getRightAt(row);
    }

    @Override
    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        super.changeSelection(rowIndex, columnIndex, toggle, extend);
        repaint();
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        if (row >= 0) {
            // Always keep the whole row selected when editing starts
            setRowSelectionInterval(row, row);
        }

        boolean editing = super.editCellAt(row, column, e);

        if (editing) {
            Component editorComp = getEditorComponent();
            if (editorComp != null) {
                editorComp.setBackground(SELECTION_BG);
                editorComp.setForeground(SELECTION_FG);
            }
            repaint();
        }

        return editing;
    }

    @Override
    public void removeEditor() {
        super.removeEditor();
        repaint();
    }

    private class HeaderRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                    table, value, false, false, row, column);

            lbl.setFont(headerFont);
            lbl.setOpaque(true);
            lbl.setBackground(HEADER_BG);
            lbl.setForeground(new Color(40, 40, 40));
            lbl.setBorder(new CompoundBorder(
                    new MatteBorder(0, 0, 1, column == 0 ? 1 : 0, column == 0 ? COL_LINE : ROW_LINE),
                    new EmptyBorder(4, 6, 4, 6)
            ));
            return lbl;
        }
    }

    private class CellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private final int column;

        CellRenderer(int column) {
            this.column = column;
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {

            boolean selectedRow = table.isRowSelected(row);

            JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                    table, value, selectedRow, hasFocus, row, col);

            lbl.setText(value == null ? "" : value.toString());
            lbl.setBorder(createCellBorder(column));

            if (column == 0) {
                lbl.setFont(roiFont);
                lbl.setToolTipText(lbl.getText());
            } else {
                lbl.setFont(classFont);
                lbl.setToolTipText("Editable");
            }

            if (selectedRow) {
                lbl.setBackground(SELECTION_BG);
                lbl.setForeground(SELECTION_FG);
            } else {
                lbl.setBackground(column == 1 ? CLASS_BG : Color.WHITE);
                lbl.setForeground(column == 1 ? CLASS_FG : table.getForeground());
            }

            return lbl;
        }

        private Border createCellBorder(int column) {
            Border bottom = new MatteBorder(0, 0, 1, 0, ROW_LINE);
            Border right = new MatteBorder(0, 0, 0, column == 0 ? 1 : 0, COL_LINE);
            return new CompoundBorder(
                    new CompoundBorder(bottom, right),
                    new EmptyBorder(2, 6, 2, 6)
            );
        }
    }
}