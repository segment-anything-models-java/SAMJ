package ai.nets.samj.gui;

import ai.nets.samj.annotation.Mask;
import ai.nets.samj.communication.model.EfficientSAM;
import ai.nets.samj.communication.model.EfficientViTSAML2;
import ai.nets.samj.communication.model.SAM2Large;
import ai.nets.samj.communication.model.SAM2Small;
import ai.nets.samj.communication.model.SAM2Tiny;
import ai.nets.samj.communication.model.SAMModel;
import ai.nets.samj.gui.ImageSelection.ImageSelectionListener;
import ai.nets.samj.gui.ModelSelection.ModelSelectionListener;
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

public class MainGUI extends JFrame {

    protected static final long serialVersionUID = -797293687195076077L;

    protected boolean isDrawerOpen = false;
    protected final List<SAMModel> modelList;
    protected ImageSelectionListener imageListener;
    protected ModelSelectionListener modelListener;
    protected ModelDrawerPanelListener modelDrawerListener;
    protected BatchCallback batchDrawerCallback;
    protected ConsumerCallback consumerCallback;
    
    protected ConsumerInterface consumer;
    protected boolean isValidPrompt = true;

    protected JCheckBox chkRoiManager = new JCheckBox("Add to RoiManager", true);
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
    protected ResizableButton stopProgressBtn = new ResizableButton("■", 10, 2, 2);
    protected final ModelSelection cmbModels;
    // TODO add information tab to cmbImages, same as cmbModels
    // TODO changes to just a combobox on december 2024
    // TODO private final ImageSelection cmbImages;
    protected final ImageSelectionOnlyComboBox cmbImages;
    protected ModelDrawerPanel drawerPanel;
    protected JPanel cardPanel;
    protected JPanel cardPanel1_2;
    protected JPanel cardPanel2_2;

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

    public static final List<SAMModel> DEFAULT_MODEL_LIST = new ArrayList<>();
    static {
        DEFAULT_MODEL_LIST.add(new SAM2Tiny());
        DEFAULT_MODEL_LIST.add(new SAM2Small());
        DEFAULT_MODEL_LIST.add(new SAM2Large());
        DEFAULT_MODEL_LIST.add(new EfficientSAM());
        DEFAULT_MODEL_LIST.add(new EfficientViTSAML2());
    }

    public MainGUI(ConsumerInterface consumer) {
        this(null, consumer);
    }

    public MainGUI(List<SAMModel> modelList, ConsumerInterface consumer) {
        super(Constants.JAR_NAME + "-" + Constants.SAMJ_VERSION);

        createListeners();
        this.consumer = consumer;
        cmbImages = ImageSelectionOnlyComboBox.create(this.consumer, imageListener);
        if (modelList == null) this.modelList = DEFAULT_MODEL_LIST;
        else this.modelList = modelList;
        cmbModels = ModelSelection.create(this.modelList, modelListener);
        
        this.consumer.setGuiCallback(() -> {
	            setTwoThirdsEnabled(false);
	            cmbImages.updateList();
	            go.setEnabled(false);
	            go.showAnimation(true);
	            new Thread(() -> {
	                go.setEnabled(cmbModels.getSelectedModel().isInstalled());
	                go.showAnimation(false);
	            }).start();
			});
        this.consumer.setCallback(consumerCallback);

        

        drawerPanel = ModelDrawerPanel.create(DRAWER_HORIZONTAL_SIZE, this.modelDrawerListener);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        cmbModels.getButton().addActionListener(e -> toggleDrawer());
        go.addActionListener(e -> loadModel());
        export.addActionListener(e -> consumer.exportImageLabeling());
        chkInstant.addActionListener(e -> setInstantPromptsEnabled(this.chkInstant.isSelected() && this.isValidPrompt));
        chkRoiManager.addActionListener(e -> consumer.enableAddingToRoiManager(chkRoiManager.isSelected()));
        retunLargest.addActionListener(e -> cmbModels.getSelectedModel().setReturnOnlyBiggest(retunLargest.isSelected()));
        btnBatchSAMize.addActionListener(e -> batchSAMize());
        stopProgressBtn.addActionListener(e -> {
        	// TODO stopProgress();
        });
        close.addActionListener(e -> dispose());
        help.addActionListener(e -> consumer.exportImageLabeling());

        // Use BorderLayout for the main frame
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;

        // Add the title panel at the top
        gbc.gridy = 0;
        gbc.weighty = 0.1;
        mainPanel.add(createTitlePanel(), gbc);

        // Add the main center panel
        gbc.gridy = 2;
        gbc.weighty = 0.87;
        mainPanel.add(createCenterPanel(), gbc);

        // Add the bottom panel with buttons
        gbc.gridy = 3;
        gbc.weighty = 0.03;
        mainPanel.add(createBottomPanel(), gbc);

        // Add the mainPanel and drawerPanel using BorderLayout
        add(mainPanel, BorderLayout.CENTER);
        add(drawerPanel, BorderLayout.EAST);

        // Set the initial size of the frame
        setSize(MAIN_HORIZONTAL_SIZE, MAIN_VERTICAL_SIZE); // Width x Height
        
        consumerCallback.validPromptChosen(consumer.isValidPromptSelected());

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                close();
            }
        });

        // Initially hide the drawerPanel
        drawerPanel.setVisible(false);

        this.setTwoThirdsEnabled(false);
    	go.setEnabled(false);
        new Thread(() -> {
            if (this.cmbModels.getSelectedModel().isInstalled() && cmbImages.getSelectedObject() != null)
            	SwingUtilities.invokeLater(() -> go.setEnabled(true));
        }).start();
        // Make the frame visible
        makeVisibleOnInstantiation();
    }

    /**
     * The purpose of this method is two-fold: To make this JFrame visible,
     * and to be called from default constructor of this class. So the default
     * behaviour is that this JFrame is immediately displayed. However, if
     * a class derived from this one needs a different behaviour, that class
     * can override this method...
     */
    protected void makeVisibleOnInstantiation() {
        setVisible(true);
    }

    protected void setInstantPromptsEnabled(boolean enabled) {
        if (enabled) {
        	consumer.enableAddingToRoiManager(this.chkRoiManager.isSelected());
            consumer.activateListeners();
        } else {
            consumer.deactivateListeners();
        }
    }

    protected void setTwoThirdsEnabled(boolean enabled) {
        this.chkInstant.setEnabled(enabled);
        this.retunLargest.setEnabled(enabled);
        this.chkRoiManager.setEnabled(enabled);
        this.btnBatchSAMize.setEnabled(enabled);
        this.export.setEnabled(enabled);
        this.radioButton1.setEnabled(enabled);
        this.radioButton2.setEnabled(enabled);
        this.batchProgress.setEnabled(enabled);
        if (!enabled)
        	this.stopProgressBtn.setEnabled(enabled);
    }

    protected void loadModel() {
        SwingUtilities.invokeLater(() -> {
            go.setEnabled(false);
            setTwoThirdsEnabled(false);
        });
        new Thread(() -> {
            try {
                // TODO try removing Cast
                cmbModels.loadModel(Cast.unchecked(cmbImages.getSelectedRai()));
            	consumer.enableAddingToRoiManager(this.chkRoiManager.isSelected());
                consumer.setFocusedImage(cmbImages.getSelectedObject());
                consumer.setModel(cmbModels.getSelectedModel());
                setInstantPromptsEnabled(this.chkInstant.isSelected() && this.isValidPrompt);
                cmbModels.getSelectedModel().setReturnOnlyBiggest(retunLargest.isSelected());
                setTwoThirdsEnabled(true);
            } catch (IOException | RuntimeException | InterruptedException ex) {
                ex.printStackTrace();
            }
            this.go.showAnimation(false);
        }).start();
    }

    protected void close() {
        cmbModels.unLoadModel();
        this.drawerPanel.interruptThreads();
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

    // Method to create the center panel
    protected JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Insets around components
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        // First component: Rectangular area with line border
        gbc.gridy = 0;
        gbc.weighty = 0.25;
        centerPanel.add(createFirstComponent(), gbc);

        // Second component: Radio button with changing panel
        gbc.gridy = 1;
        gbc.weighty = 0.4;
        centerPanel.add(createSecondComponent(), gbc);

        // Third component: Two checkboxes and a button
        gbc.gridy = 2;
        gbc.weighty = 0.35;
        centerPanel.add(createThirdComponent(), gbc);

        return centerPanel;
    }

    // Method to create the bottom panel
    protected JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridBagLayout());
        bottomPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        GridBagConstraints gbcb = new GridBagConstraints();
        gbcb.gridy = 0;
        gbcb.weightx = 1;
        gbcb.weighty = 1;
        gbcb.fill = GridBagConstraints.BOTH;

        gbcb.gridx = 0;
        bottomPanel.add(help, gbcb);
        gbcb.gridx = 1;
        bottomPanel.add(close, gbcb);

        return bottomPanel;
    }

    // Method to create the first component
    protected JPanel createFirstComponent() {
        JPanel firstComponent = new JPanel();
        firstComponent.setLayout(new GridBagLayout());
        firstComponent.setBorder(new LineBorder(Color.BLACK));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 5, 2); // Adjust insets as needed
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        // Add the first component
        gbc.gridy = 0;
        gbc.weighty = 0.33; // Adjust weighty if necessary
        firstComponent.add(this.cmbModels, gbc);

        // Add the second component
        gbc.gridy = 1;
        firstComponent.add(this.cmbImages, gbc);

        // Add the button
        gbc.gridy = 2;
        gbc.insets = new Insets(2, 2, 2, 2); // Adjust insets as needed
        firstComponent.add(go, gbc);
        firstComponent.setPreferredSize(new Dimension(0, (int) (MAIN_VERTICAL_SIZE * 0.2)));

        return firstComponent;
    }

    // Method to create the second component
    protected JPanel createSecondComponent() {
        JPanel secondComponent = new JPanel();
        secondComponent.setLayout(new GridBagLayout());
        secondComponent.setBorder(new LineBorder(Color.BLACK));

        // Radio buttons
        JPanel radioPanel = new JPanel();
        radioButton1 = new JRadioButton(MANUAL_STR, true);
        radioButton2 = new JRadioButton(PRESET_STR);

        ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(radioButton1);
        radioGroup.add(radioButton2);

        radioPanel.add(radioButton1);
        radioPanel.add(radioButton2);

        // Panel below radio buttons with CardLayout
        cardPanel = new JPanel(new CardLayout());
        cardPanel.setBorder(new LineBorder(Color.BLACK));

        // First card
        JPanel card1 = new JPanel(new GridBagLayout());
        GridBagConstraints gbc0 = new GridBagConstraints();
        gbc0.gridx = 0;
        gbc0.gridy = 0;
        gbc0.weighty = 0.2;
        gbc0.anchor = GridBagConstraints.NORTH;
        cardPanel1_2 = new JPanel(new CardLayout());
        cardPanel1_2.add(new JPanel() {{ setOpaque(false); }}, INVISIBLE_STR);
        cardPanel1_2.add(new JLabel("<html><font color='orange'>&#9888; Only rect and points!</font></html>"), VISIBLE_STR);
        card1.add(cardPanel1_2, gbc0);

        gbc0.gridy = 1;
        gbc0.anchor = GridBagConstraints.CENTER;
        gbc0.fill = GridBagConstraints.BOTH;
        gbc0.weighty = 0.8;
        gbc0.insets = new Insets(0, 5, 10, 5);
        card1.add(chkInstant, gbc0);

        JPanel card2 = new JPanel(new GridBagLayout());
        gbc0.gridy = 0;
        gbc0.anchor = GridBagConstraints.NORTH;
        gbc0.fill = GridBagConstraints.NONE;
        gbc0.weighty = 0.1;
        gbc0.insets = new Insets(0, 2, 5, 2);
        gbc0.weightx = 1;
        cardPanel2_2 = new JPanel(new CardLayout());
        cardPanel2_2.add(new JPanel() {{ setOpaque(false); }}, INVISIBLE_STR);
        cardPanel2_2.add(new JLabel("<html><font color='orange'>&#9888; No prompt was provided!</font></html>"), VISIBLE_STR);
        card2.add(cardPanel2_2, gbc0);

        gbc0.gridy = 1;
        gbc0.anchor = GridBagConstraints.CENTER;
        gbc0.fill = GridBagConstraints.BOTH;
        gbc0.weighty = 0.8;
        JPanel wrapperBtn = new JPanel(new GridBagLayout());
        GridBagConstraints gbcBtn = new GridBagConstraints();
        gbcBtn.weightx = 0.05;
        gbcBtn.weighty = 1;
        gbcBtn.fill = GridBagConstraints.NONE;
        gbcBtn.insets = new Insets(0, 0, 0, 0);
        gbcBtn.gridy = 0;
        gbcBtn.gridx = 1;
        Icon questionIcon = UIManager.getIcon("OptionPane.questionIcon");
        if (questionIcon == null) {
            questionIcon = UIManager.getIcon("OptionPane.informationIcon");
        }
        CustomInsetsJLabel questionMark = new CustomInsetsJLabel(questionIcon, 4, 2, 0, 0);
        //questionMark.setBorder(LineBorder.createBlackLineBorder());
        questionMark.setToolTipText(Constants.HELP_MSG);
        wrapperBtn.add(questionMark, gbcBtn);
        gbcBtn.gridx = 0;
        gbcBtn.fill = GridBagConstraints.BOTH;
        gbcBtn.weightx = 0.95;
        wrapperBtn.add(btnBatchSAMize, gbcBtn);
        gbc0.insets = new Insets(0, 5, 5, 0);
        card2.add(wrapperBtn, gbc0);

        gbc0.gridy = 2;
        gbc0.insets = new Insets(0, 2, 5, 2);
        gbc0.weighty = 0.1;
        gbc0.anchor = GridBagConstraints.CENTER;
        gbc0.fill = GridBagConstraints.BOTH;
        JPanel wrapper = new JPanel(new GridBagLayout());
        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.insets = new Insets(0, 0, 0, 0);
        gbc1.gridy = 0;
        gbc1.gridx = 0;
        gbc1.anchor = GridBagConstraints.CENTER;
        gbc1.fill = GridBagConstraints.BOTH;
        gbc1.weighty = 1;
        gbc1.weightx = 0.9;
        wrapper.add(this.batchProgress, gbc1);
        gbc1.gridx = 1;
        gbc1.weightx = 0.1;
        wrapper.add(stopProgressBtn, gbc1);
        card2.add(wrapper, gbc0);

        cardPanel.add(card1, MANUAL_STR);
        cardPanel.add(card2, PRESET_STR);

        // Add action listeners to radio buttons
        radioButton1.addActionListener(e -> {
            CardLayout cl = (CardLayout) (cardPanel.getLayout());
            cl.show(cardPanel, MANUAL_STR);
        	this.chkInstant.setEnabled(this.isValidPrompt);
        });

        radioButton2.addActionListener(e -> {
        	CardLayout cl = (CardLayout) (cardPanel.getLayout());
        	cl.show(cardPanel, PRESET_STR);
        	this.chkInstant.setSelected(false);
        	setInstantPromptsEnabled(false);
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;

        // Add the title panel at the top
        gbc.gridy = 0;
        gbc.weighty = 0.1;
        // Assemble the second component
        secondComponent.add(radioPanel, gbc);
        gbc.gridy = 1;
        gbc.weighty = 0.9;
        secondComponent.add(cardPanel, gbc);
        secondComponent.setPreferredSize(new Dimension(0, (int) (MAIN_VERTICAL_SIZE * 0.30)));

        return secondComponent;
    }

    // Method to create the third component
    protected JPanel createThirdComponent() {
        JPanel thirdComponent = new JPanel();
        thirdComponent.setLayout(new GridBagLayout());
        thirdComponent.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));
        thirdComponent.setBorder(new LineBorder(Color.BLACK));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;

        // First checkbox
        gbc.gridy = 0;
        thirdComponent.add(chkRoiManager, gbc);

        // Second checkbox
        gbc.gridy = 1;
        thirdComponent.add(this.retunLargest, gbc);

        // Button
        gbc.gridy = 2;
        thirdComponent.add(this.export, gbc);
        thirdComponent.setPreferredSize(new Dimension(0, (int) (MAIN_VERTICAL_SIZE * 0.2)));

        return thirdComponent;
    }

    protected void toggleDrawer() {
        if (drawerPanel.isVisible()) {
            drawerPanel.setVisible(false);
            this.cmbModels.getButton().setText("▶");
            setSize(getWidth() - drawerPanel.getPreferredSize().width, getHeight());
        } else {
            drawerPanel.setVisible(true);
            drawerPanel.setSelectedModel(this.cmbModels.getSelectedModel());
            this.cmbModels.getButton().setText("◀");
            setSize(getWidth() + drawerPanel.getPreferredSize().width, getHeight());
        }
        isDrawerOpen = !isDrawerOpen;
        revalidate();
        repaint();
    }
    
    protected < T extends RealType< T > & NativeType< T > > void batchSAMize() {
    	RandomAccessibleInterval<T> rai;
    	if (this.consumer.getFocusedImage() != this.cmbImages.getSelectedObject())
    		rai = this.consumer.getFocusedImageAsRai();
    	else
    		rai = null;
    	List<int[]> pointPrompts = this.consumer.getPointRoisOnFocusImage();
    	List<Rectangle> rectPrompts = this.consumer.getRectRoisOnFocusImage();
		CardLayout lyt = (CardLayout) cardPanel2_2.getLayout();
    	if (pointPrompts.size() == 0 && rectPrompts.size() == 0 && rai == null) {
        	lyt.show(cardPanel2_2, VISIBLE_STR);
    		return;
    	} else if (pointPrompts.size() == 0 && rectPrompts.size() == 0 && !(Util.getTypeFromInterval(rai) instanceof IntegerType)){
        	lyt.show(cardPanel2_2, VISIBLE_STR);
    		return;
    	}
    	lyt.show(cardPanel2_2, INVISIBLE_STR);
    	this.stopProgressBtn.setEnabled(true);
    	consumer.setFocusedImage(this.cmbImages.getSelectedObject());
    	new Thread(() -> {
    		try {
    			consumer.notifyBatchSamize(this.cmbModels.getSelectedModel().getName(), 
    					rai == null ? null : consumer.getFocusedImageName() );
				cmbModels.getSelectedModel().processBatchOfPrompts(pointPrompts, rectPrompts, rai, batchDrawerCallback);
			} catch (IOException | RuntimeException | InterruptedException e) {
				e.printStackTrace();
			}
    		SwingUtilities.invokeLater(() -> stopProgressBtn.setEnabled(false));
    	}).start();;
    	pointPrompts.stream().forEach(pp -> consumer.deletePointRoi(pp));
    	rectPrompts.stream().forEach(pp -> consumer.deleteRectRoi(pp));
    }

    protected void createListeners() {
        imageListener = new ImageSelectionListener() {
            @Override
            public void modelActionsOnImageChanged() {
                cmbModels.getSelectedModel().closeProcess();
            }

            @Override
            public void imageActionsOnImageChanged() {
                consumer.deactivateListeners();
                consumer.deselectImage();
            	setTwoThirdsEnabled(false);
                if (go.isEnabled())
                	return;
                go.showAnimation(true);
                new Thread(() -> {
                    go.setEnabled(cmbModels.getSelectedModel().isInstalled());
                    go.showAnimation(false);
                }).start();
            }
        };
        modelListener = new ModelSelectionListener() {

			@Override
			public void changeDrawerPanel() {
				if (drawerPanel.isVisible())
					drawerPanel.setSelectedModel(cmbModels.getSelectedModel());
				
			}
			
			@Override
			public void changeGUI() {
                setTwoThirdsEnabled(false);
                go.setEnabled(cmbImages.getSelectedObject() != null);
                go.setEnabled(false);
                go.showAnimation(true);
                new Thread(() -> {
                    go.setEnabled(cmbModels.getSelectedModel().isInstalled());
                    go.showAnimation(false);
                }).start();
			}
        };
        modelDrawerListener = new ModelDrawerPanelListener() {

			@Override
			public void setGUIEnabled(boolean enabled) {
				cmbModels.setEnabled(enabled);
				cmbImages.setEnabled(enabled);
				if (!enabled) {
					setTwoThirdsEnabled(enabled);
				} else if (enabled && cmbImages.getSelectedObject() != null) {
			        new Thread(() -> {
			            if (cmbModels.getSelectedModel().isInstalled())
			            	SwingUtilities.invokeLater(() -> go.setEnabled(true));
			        }).start();
				}
			}
        };
        
        batchDrawerCallback = new BatchCallback() {
        	private int nRois;

			@Override
			public void setTotalNumberOfRois(int nRois) {
				this.nRois = nRois;
				SwingUtilities.invokeLater(() -> {
					batchProgress.setValue(0);
				});
			}

			@Override
			public void updateProgress(int n) {
				SwingUtilities.invokeLater(() -> batchProgress.setValue((int) Math.round(100 * n / (double) nRois) ));
			}

			@Override
			public void drawRoi(List<Mask> masks) {
				SwingUtilities.invokeLater(() -> consumer.addPolygonsFromGUI(masks));
				
			}

			@Override
			public void deletePointPrompt(List<int[]> promptList) {
				SwingUtilities.invokeLater(() -> promptList.forEach(proi -> consumer.deletePointRoi(proi)));
			}

			@Override
			public void deleteRectPrompt(List<int[]> promptList) {
				SwingUtilities.invokeLater(() -> promptList.stream()
						.map(rect -> new Rectangle(rect[0], rect[1], rect[2] - rect[0], rect[3] - rect[1]))
						.forEach(roi -> consumer.deleteRectRoi(roi)));
			}
        	
        };
        
        consumerCallback = new ConsumerCallback() {

			@Override
			public void validPromptChosen(boolean isValid) {
				if (isValid && !isValidPrompt) {
					CardLayout lyt = (CardLayout) cardPanel1_2.getLayout();
		        	lyt.show(cardPanel1_2, INVISIBLE_STR);
		        	isValidPrompt = true;
		        	if (cmbModels.getSelectedModel().isLoaded())
		        		MainGUI.this.chkInstant.setEnabled(true);
				} else if (!isValid && isValidPrompt) {
					CardLayout lyt = (CardLayout) cardPanel1_2.getLayout();
		        	lyt.show(cardPanel1_2, VISIBLE_STR);
		        	isValidPrompt = false;
		        	MainGUI.this.chkInstant.setSelected(false);
		        	MainGUI.this.chkInstant.setEnabled(false);
		        	MainGUI.this.setInstantPromptsEnabled(false);;
				}
			}
        	
        };
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainGUI(null, null));
    }
}
