package ai.nets.samj.gui.last;

import ai.nets.samj.annotation.Mask;
import ai.nets.samj.communication.model.DummyModel;
import ai.nets.samj.communication.model.EfficientTAMSmall;
import ai.nets.samj.communication.model.EfficientTAMTiny;
import ai.nets.samj.communication.model.SAM2Large;
import ai.nets.samj.communication.model.SAM2Small;
import ai.nets.samj.communication.model.SAM2Tiny;
import ai.nets.samj.communication.model.SAMModel;
import ai.nets.samj.gui.CustomInsetsJLabel;
import ai.nets.samj.gui.ImageSelection.ImageSelectionListener;
import ai.nets.samj.gui.ImageSelectionCombo;
import ai.nets.samj.gui.JSwitchButton;
import ai.nets.samj.gui.LoadingButton;
import ai.nets.samj.gui.ModelSelection;
import ai.nets.samj.gui.ModelSelection.ModelSelectionListener;
import ai.nets.samj.gui.components.ImageDrawerPanel;
import ai.nets.samj.gui.components.ModelDrawerPanel;
import ai.nets.samj.gui.components.ModelDrawerPanel.ModelDrawerPanelListener;
import ai.nets.samj.models.AbstractSamJ.BatchCallback;
import ai.nets.samj.ui.ConsumerInterface;
import ai.nets.samj.ui.ConsumerInterface.ConsumerCallback;
import ai.nets.samj.utils.Constants;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.IntegerType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Cast;
import net.imglib2.util.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apposed.appose.BuildException;
import org.apposed.appose.TaskException;

public class MainGUI extends JPanel {

    protected static final long serialVersionUID = -797293687195076077L;

    protected boolean isModelDrawerOpen = false;
    protected boolean isImageDrawerOpen = false;
    

    protected JCheckBox propagate3D = new JCheckBox("Propagate in 3D/time", false);
    protected JCheckBox retunLargest = new JCheckBox("Only return largest ROI", true);
    protected JSwitchButton chkInstant = new JSwitchButton("LIVE", "OFF");
    protected LoadingButton go = new LoadingButton("Go!", RESOURCES_FOLDER, "loading_animation_samj.gif", 20);;
    protected JButton btnBatchSAMize = new JButton("Batch SAMize");
    protected JButton close = new JButton("Close");
    protected JButton help = new JButton("Help");
    protected JButton export = new JButton("Export...");
    protected JRadioButton radioButton1;
    protected JRadioButton radioButton2;
    protected JProgressBar batchProgress = new JProgressBar();
    protected JButton stopProgressBtn = new JButton("■");
    protected TitleGUI titleGui;
    protected final ModelSelection cmbModels;
    protected final ImageSelectionCombo cmbImages;
    protected ModelDrawerPanel modelDrawerPanel;
    protected ImageDrawerPanel imageDrawerPanel;
    protected JPanel cardPanel;
    protected JPanel cardPanel1_2;
    protected JPanel cardPanel2_2;
    protected JPanel drawerContainer;

    protected static double HEADER_VERTICAL_RATIO = 0.1;

    protected static int MAIN_VERTICAL_SIZE = 400;
    protected static int MAIN_HORIZONTAL_SIZE = 250;
    protected static int DRAWER_HORIZONTAL_SIZE = 450;

    protected static String MANUAL_STR = "Manual";
    protected static String PRESET_STR = "Preset prompts";
    protected static String VISIBLE_STR = "visible";
    protected static String INVISIBLE_STR = "invisible";
	/**
	 * Name of the folder where the icon images for the dialog buttons are within the resources folder
	 */
	protected static final String RESOURCES_FOLDER = "icons_samj/";

    public MainGUI() {
        setLayout(null);

        cmbImages = ImageSelectionCombo.create(this.consumer, imageListener);
        cmbModels = ModelSelection.create(this.modelList, modelListener);
        

        modelDrawerPanel = ModelDrawerPanel.create(DRAWER_HORIZONTAL_SIZE, this.modelDrawerListener);
        imageDrawerPanel = ImageDrawerPanel.create();

        titleGui = new TitleGUI();
        drawerContainer = new JPanel(new CardLayout());
        drawerContainer.add(modelDrawerPanel, "MODEL");
        drawerContainer.add(imageDrawerPanel, "IMAGE");
        drawerContainer.setVisible(false);

        setSize(MAIN_HORIZONTAL_SIZE, MAIN_VERTICAL_SIZE);

        add(titleGui);
        add(propagate3D);
        add(retunLargest);
        add(chkInstant);
        add(go);
        add(btnBatchSAMize);
        add(close);
        add(help);
        add(export);
        add(radioButton1);
        add(radioButton2);
        add(batchProgress);
        add(stopProgressBtn);
        add(cmbImages);
        add(cmbModels);
        add(cardPanel);
        add(cardPanel1_2);
        add(cardPanel2_2);
        add(drawerContainer);

        setSize(MAIN_HORIZONTAL_SIZE, MAIN_VERTICAL_SIZE);

        modelDrawerPanel.setVisible(false);
        imageDrawerPanel.setVisible(false);

        this.setTwoThirdsEnabled(false);
    }
    
    @Override
    private void doLayout() {
        int rawW = getWidth();
        int rawH = getHeight();
        int inset = 2;
    }
    
    // Method to create the title panel
    protected JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(Color.LIGHT_GRAY);
        int height = (int) (HEADER_VERTICAL_RATIO * MAIN_VERTICAL_SIZE);
        titlePanel.setPreferredSize(new Dimension(0, height)); // Fixed height
        String text = "<html><div style='text-align: center; font-size: 15px;'>"
                + "<span style='color: black;'>SAM</span>" + "<span style='color: red;'>J</span>";
        JLabel titleLabel = new JLabel(text, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));

        titlePanel.setLayout(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        return titlePanel;
    }

    protected void setTwoThirdsEnabled(boolean enabled) {
        this.chkInstant.setEnabled(enabled);
        this.retunLargest.setEnabled(enabled);
        this.propagate3D.setEnabled(enabled);
        this.btnBatchSAMize.setEnabled(enabled);
        this.export.setEnabled(enabled);
        this.radioButton1.setEnabled(enabled);
        this.radioButton2.setEnabled(enabled);
        this.batchProgress.setEnabled(enabled);
        if (!enabled)
        	this.stopProgressBtn.setEnabled(enabled);
    }

    protected void toggleModelDrawer() {
        CardLayout cl = (CardLayout) drawerContainer.getLayout();

        if (drawerContainer.isVisible() && isModelDrawerOpen) {
            drawerContainer.setVisible(false);
            cmbModels.getButton().setText("▶");
            setSize(getWidth() - DRAWER_HORIZONTAL_SIZE, getHeight());
            isModelDrawerOpen = false;
        } else if (drawerContainer.isVisible()) {
            cl.show(drawerContainer, "MODEL");
            cmbModels.getButton().setText("◀");
            cmbImages.getButton().setText("▶");
            isImageDrawerOpen = false;
            modelDrawerPanel.setSelectedModel(cmbModels.getSelectedModel());
            isModelDrawerOpen = true;
        } else {
            drawerContainer.setVisible(true);
            cl.show(drawerContainer, "MODEL");
            cmbModels.getButton().setText("◀");
            setSize(getWidth() + DRAWER_HORIZONTAL_SIZE, getHeight());
            modelDrawerPanel.setSelectedModel(cmbModels.getSelectedModel());
            isModelDrawerOpen = true;
        }
        revalidate();
        repaint();
    }

    protected void toggleImageDrawer() {
        CardLayout cl = (CardLayout) drawerContainer.getLayout();

        if (drawerContainer.isVisible() && isImageDrawerOpen) {
            drawerContainer.setVisible(false);
            cmbImages.getButton().setText("▶");
            setSize(getWidth() - DRAWER_HORIZONTAL_SIZE, getHeight());
            isImageDrawerOpen = false;
        } else if (drawerContainer.isVisible()) {
            cl.show(drawerContainer, "IMAGE");
            cmbImages.getButton().setText("◀");
            cmbModels.getButton().setText("▶");
            isImageDrawerOpen = true;
            isModelDrawerOpen = false;
        } else {
            drawerContainer.setVisible(true);
            cl.show(drawerContainer, "IMAGE");
            cmbImages.getButton().setText("◀");
            setSize(getWidth() + DRAWER_HORIZONTAL_SIZE, getHeight());
            isImageDrawerOpen = true;
        }
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainGUI(null, null));
    }
}
