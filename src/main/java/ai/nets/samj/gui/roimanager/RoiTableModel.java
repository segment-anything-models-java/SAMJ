package ai.nets.samj.gui.roimanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.table.AbstractTableModel;

public class RoiTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    public static class Row {
        private final String left;
        private String right;

        public Row(String left, String right) {
            this.left = left;
            this.right = right;
        }

        public String getLeft() {
            return left;
        }

        public String getRight() {
            return right;
        }

        public void setRight(String right) {
            this.right = right;
        }
    }

    private final List<Row> rows = new ArrayList<Row>();

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int column) {
        return column == 0 ? "ROI Name" : "Class Name";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Row row = rows.get(rowIndex);
        return columnIndex == 0 ? row.getLeft() : row.getRight();
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex != 1) return;
        rows.get(rowIndex).setRight(value == null ? "" : value.toString());
        fireTableCellUpdated(rowIndex, columnIndex);
    }

    public void clear() {
        if (rows.isEmpty()) return;
        rows.clear();
        fireTableDataChanged();
    }

    public void addRow(String left, String right) {
        int i = rows.size();
        rows.add(new Row(left, right));
        fireTableRowsInserted(i, i);
    }

    public void setRows(Map<String, String> map) {
        rows.clear();
        for (Map.Entry<String, String> e : map.entrySet()) {
            rows.add(new Row(e.getKey(), e.getValue()));
        }
        fireTableDataChanged();
    }

    public void remove(int index) {
        if (index < 0 || index >= rows.size()) return;
        rows.remove(index);
        fireTableRowsDeleted(index, index);
    }

    public String getLeftAt(int row) {
        return rows.get(row).getLeft();
    }

    public String getRightAt(int row) {
        return rows.get(row).getRight();
    }

    public void setRightAt(int row, String value) {
        if (row < 0 || row >= rows.size()) return;
        rows.get(row).setRight(value);
        fireTableCellUpdated(row, 1);
    }
}