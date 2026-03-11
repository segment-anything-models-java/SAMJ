package ai.nets.samj.gui.roimanager;

import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import javax.swing.JButton;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import ai.nets.samj.annotation.Mask;
import ai.nets.samj.gui.components.PlusMinusButtonComp;
import ai.nets.samj.gui.roimanager.commands.Command;
import ai.nets.samj.gui.roimanager.commands.DeleteRoiCommand;
import ai.nets.samj.gui.roimanager.commands.ModifyRoiCommand;
import ai.nets.samj.gui.roimanager.utils.PolygonUtils;

public class RoiManager extends RoiManagerGUI implements MouseWheelListener, ListSelectionListener, MouseListener, ActionListener, ItemListener {

    private static final long serialVersionUID = -8405747451234902128L;

    private static final Pattern TRAILING_NUMBER = Pattern.compile("^(.*?)(\\d+)$");

    private Object image;

    private int prevIndex = -1;
    
    private HashMap<String, String> objectIdMap = new HashMap<String, String>();

    private RoiManagerConsumer consumer;

    private Consumer<List<Mask>> exportLMDCallback;

    private Consumer<Command> commandCallback;

    private final List<Mask> rois = new ArrayList<Mask>();
    private final List<String> roiClasses = new ArrayList<String>();

    private boolean justClickedDelete = false;

    private String nextClassPrefix = "instance ";
    private boolean nextClassNumbered = true;
    private int nextClassSeed = 0;

    public RoiManager(RoiManagerConsumer consumer) {
        this.consumer = consumer;

        BiConsumer<Integer, Polygon> mod = (ii, pol) -> {
            int n = rois.size();
            if (ii < 0 || ii >= n)
                return;
            rois.get(ii).clear();
            rois.get(ii).setContour(pol);
            consumer.setRois(rois);
            consumer.setSelected(rois.get(ii));
        };
        consumer.setModifyRoiCallback(mod);

        consumer.setSelectedCallback((i) -> list.setSelectedIndex(i));

        listModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() != TableModelEvent.UPDATE || e.getColumn() != 1)
                    return;

                int first = e.getFirstRow();
                int last = e.getLastRow();

                for (int row = first; row <= last && row < roiClasses.size(); row++) {
                    Object value = listModel.getValueAt(row, 1);
                    String className = value == null ? "" : value.toString();
                    roiClasses.set(row, className);
                    updateNextClassRule(className);
                }
            }
        });
    }
    
    @Override
    protected void addPointsControl() {
        pointsComp = new PlusMinusButtonComp("Points", this::handlePointsAction);
        panel.add(pointsComp);
    }
    
    private void handlePointsAction(String command) {
        if ("Points+".equals(command)) {
            complicate();
        } else if ("Points-".equals(command)) {
        	simplify();
        }
    }

    protected void addButton(String label) {
        JButton b = new JButton(label);
        btns.add(b);
        b.addActionListener(this);
        panel.add(b);
    }

    public void setImage(Object image) {
        consumer.setImage(image);
        this.image = image;
    }

    public void addCommandCallback(Consumer<Command> commandCallback) {
        this.commandCallback = commandCallback;
    }

    public void setExportLMDcallback(Consumer<List<Mask>> exportLMDCallback) {
        this.exportLMDCallback = exportLMDCallback;
    }

    public Object getCurrentImage() {
        return this.image;
    }

    public synchronized Mask[] getRoisAsArray() {
        Mask[] array = new Mask[rois.size()];
        return (Mask[]) rois.toArray(array);
    }

    public int getROIsNumber() {
        return listModel != null ? listModel.getRowCount() : 0;
    }

    public void addRoi(Mask roi) {
        rois.add(roi);
        if (objectIdMap.get(roi.getObjectId()) != null)
            roiClasses.add(objectIdMap.get(roi.getObjectId()));
        else {
        	String name = buildNextClassName();
        	roiClasses.add(name);
        	objectIdMap.put(roi.getObjectId(), name);
        }
        updateShowAll();
    }

    public void deleteAll() {
        rois.clear();
        roiClasses.clear();
        listModel.clear();
        updateShowAll();
    }

    private void deleteAllBtn() {
        this.commandCallback.accept(new DeleteRoiCommand(this, rois));
        deleteAll();
    }

    public List<Mask> delete(int... indeces) {
        int count = this.getROIsNumber();
        List<Mask> deleted = new ArrayList<Mask>();
        for (int i = indeces.length - 1; i >= 0; i--) {
            int index = indeces[i];
            if (count == 0 || index >= count || index < 0)
                continue;
            deleted.add(rois.get(index));
            rois.remove(index);
            roiClasses.remove(index);
            listModel.remove(index);
        }
        updateShowAll();
        return deleted;
    }

    private void deleteBtn(int... indeces) {
        this.commandCallback.accept(new DeleteRoiCommand(this, delete(indeces)));
    }

    public void updateShowAll() {
        listModel.clear();
        if (showAllCheckbox.isSelected())
            consumer.setRois(rois);
        else
            consumer.deleteAllRois();
        consumer.setSelected(null);

        for (int i = 0; i < rois.size(); i++) {
            String className = i < roiClasses.size() ? roiClasses.get(i) : buildNextClassName();
            if (i >= roiClasses.size())
                roiClasses.add(className);
            listModel.addRow(rois.get(i).getName(), className);
        }
        updateButtonsEnabled();
    }

    private void addRoiFromGUI() {

    }

    private void simplify() {
        if (list.getRowCount() == 0)
            return;
        int[] indices = list.getSelectedIndices();
        if (indices.length == 0)
        	indices = IntStream.range(0, list.getRowCount()).toArray();
        ModifyRoiCommand command = new ModifyRoiCommand(this, rois);
        Mask mask = null;
        for (int ind : indices) {
            mask = rois.get(ind);
            command.setOldContour(mask.getUUID(), mask.getContour());
            mask.simplify();
            command.setNewContour(mask.getUUID(), mask.getContour());
            consumer.setRois(rois);
        }
        consumer.setSelected(mask);
        this.commandCallback.accept(command);
        list.setSelectedIndices(indices);
    }

    private void complicate() {
        if (list.getRowCount() == 0)
            return;
        int[] indices = list.getSelectedIndices();
        if (indices.length == 0)
        	indices = IntStream.range(0, list.getRowCount()).toArray();
        ModifyRoiCommand command = new ModifyRoiCommand(this, rois);
        Mask mask = null;
        for (int ind : indices) {
            mask = rois.get(ind);
            command.setOldContour(mask.getUUID(), mask.getContour());
            mask.complicate();
            command.setNewContour(mask.getUUID(), mask.getContour());
            consumer.setRois(rois);
        }
        consumer.setSelected(mask);
        this.commandCallback.accept(command);
        list.setSelectedIndices(indices);
    }

    private void merge() {
        if (list.getSelectedIndex() == -1 || list.getSelectedIndices().length <= 1)
            return;
        Mask roi = null;
        ModifyRoiCommand command = new ModifyRoiCommand(this, rois);
        for (int i = list.getSelectedIndices().length - 1; i >= 0; i--) {
            int ii = list.getSelectedIndices()[i];
            command.setOldContour(rois.get(ii).getUUID(), rois.get(ii).getContour());
            for (int j = i - 1; j >= 0; j--) {
                int jj = list.getSelectedIndices()[j];
                if (!PolygonUtils.overlaps(rois.get(ii).getContour(), rois.get(jj).getContour())) {
                    command.setNewContour(rois.get(ii).getUUID(), rois.get(ii).getContour());
                    command.setNewContour(rois.get(jj).getUUID(), rois.get(jj).getContour());
                    continue;
                }
                roi = rois.get(jj);
                command.setOldContour(roi.getUUID(), roi.getContour());
                roi.setContour(PolygonUtils.merge(roi.getContour(), rois.get(ii).getContour()));
                command.setNewContour(roi.getUUID(), roi.getContour());
                command.setNewContour(rois.get(ii).getUUID(), null);
                rois.remove(ii);
                roiClasses.remove(ii);
                listModel.remove(ii);
                break;
            }
        }
        commandCallback.accept(command);
        consumer.setRois(rois);
        consumer.setSelected(roi);
    }

    private void dilate() {
        if (list.getRowCount() == 0)
            return;
        int[] indices = list.getSelectedIndices();
        if (indices.length == 0)
        	indices = IntStream.range(0, list.getRowCount()).toArray();
        ModifyRoiCommand command = new ModifyRoiCommand(this, rois);
        Mask mask = null;
        for (int ind : indices) {
            mask = rois.get(ind);
            command.setOldContour(mask.getUUID(), mask.getContour());
            Polygon newPol = PolygonUtils.dilate(mask.getContour(), 1);
            command.setNewContour(mask.getUUID(), newPol);
            mask.setContour(newPol);
            consumer.setRois(rois);
        }
        consumer.setSelected(mask);
        this.commandCallback.accept(command);
        list.setSelectedIndices(indices);
    }

    private void erode() {
        if (list.getRowCount() == 0)
            return;
        int[] indices = list.getSelectedIndices();
        if (indices.length == 0)
        	indices = IntStream.range(0, list.getRowCount()).toArray();
        ModifyRoiCommand command = new ModifyRoiCommand(this, rois);
        Mask mask = null;
        for (int ind : indices) {
            mask = rois.get(ind);
            command.setOldContour(mask.getUUID(), mask.getContour());
            Polygon newPol = PolygonUtils.erode(mask.getContour(), 1);
            command.setNewContour(mask.getUUID(), newPol);
            mask.setContour(newPol);
            consumer.setRois(rois);
        }
        commandCallback.accept(command);
        consumer.setSelected(mask);
        list.setSelectedIndices(indices);
    }

    private void exportMask() {
        consumer.exportMask();
    }

    private void exportLMD() {
        exportLMDCallback.accept(rois);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String label = e.getActionCommand();
        if (label == null)
            return;
        list.setValueIsAdjusting(true);
        String command = label;
        if (command.equals("Add"))
            addRoiFromGUI();
        else if (command.equals("Simplify"))
            simplify();
        else if (command.equals("Delete") && list.getSelectedIndex() != -1 && list.getSelectedIndices().length == 1)
            deleteBtn(list.getSelectedIndex());
        else if (command.equals("Delete") && list.getSelectedIndex() != -1 && list.getSelectedIndices().length != 1)
            deleteBtn(list.getSelectedIndices());
        else if (command.equals("Delete") && list.getSelectedIndex() == -1 && justClickedDelete)
            deleteAllBtn();
        else if (command.equals("Delete") && list.getSelectedIndex() == -1) {
            justClickedDelete = true;
            return;
        } else if (command.equals("Complicate"))
            complicate();
        else if (command.equals("Export LMD"))
            exportLMD();
        else if (command.equals("Export mask"))
            exportMask();
        else if (command.equals("Dilate"))
            dilate();
        else if (command.equals("Erode"))
            erode();
        else if (command.equals("Merge"))
            merge();
        //updateButtonsEnabled();
        list.setValueIsAdjusting(false);

        justClickedDelete = false;
    }

    public void select(int ind) {
        consumer.setSelected(this.rois.get(ind));
    }

    public void select(Mask mm) {
        consumer.setSelected(mm);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getComponent().equals(this.list)) {
            int ind = this.list.locationToIndex(e.getPoint());
            if (ind < 0 || ind == prevIndex || ind > this.rois.size())
                return;
            consumer.setSelected(this.rois.get(ind));
            prevIndex = ind;
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        for (JButton btn : this.btns) {
            if (btn.getText().equals("Dilate") || btn.getText().equals("Erode")
                    || btn.getText().equals("Simplify") || btn.getText().equals("Complicate"))
                btn.setEnabled(list.getSelectedIndex() != -1 && rois.size() > 0);
            else if (btn.getText().equals("Merge"))
                btn.setEnabled(list.getSelectedIndex() != -1 && list.getSelectedIndices().length > 1);
        }

        if (e.getSource() != list.getSelectionModel())
            return;

        if (!e.getValueIsAdjusting()) {
            int lastIndex = list.getLeadSelectionIndex();
            if (lastIndex == prevIndex || lastIndex < 0 || lastIndex >= this.rois.size())
                return;
            consumer.setSelected(this.rois.get(lastIndex));
            prevIndex = lastIndex;
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        Object source = e.getSource();
        if (source == showAllCheckbox && showAllCheckbox.isSelected()) {
            consumer.setRois(rois);
        } else if (source == labelsCheckbox) {
            consumer.deleteAllRois();
        } else if (source == labelsCheckbox && this.labelsCheckbox.isSelected()) {
        } else if (source == labelsCheckbox) {
        }
    }

    private String buildNextClassName() {
        if (!nextClassNumbered)
            return nextClassPrefix;

        int next = nextClassSeed;
        for (String className : roiClasses) {
            Integer index = extractIndexForPrefix(className, nextClassPrefix);
            if (index != null)
                next = Math.max(next, index + 1);
        }
        return nextClassPrefix + next;
    }

    private Integer extractIndexForPrefix(String text, String prefix) {
        if (text == null)
            return null;
        Matcher m = TRAILING_NUMBER.matcher(text);
        if (!m.matches())
            return null;
        if (!m.group(1).equals(prefix))
            return null;
        return Integer.valueOf(m.group(2));
    }

    private void updateNextClassRule(String className) {
        if (className == null)
            className = "";

        Matcher m = TRAILING_NUMBER.matcher(className);
        if (m.matches()) {
            nextClassPrefix = m.group(1);
            nextClassNumbered = true;
            nextClassSeed = Integer.parseInt(m.group(2)) + 1;
        } else {
            nextClassPrefix = className;
            nextClassNumbered = false;
            nextClassSeed = 0;
        }
    }
}