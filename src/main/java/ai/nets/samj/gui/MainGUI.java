package ai.nets.samj.gui;


import ai.nets.samj.communication.model.EfficientSAM;
import ai.nets.samj.communication.model.EfficientViTSAML2;
import ai.nets.samj.communication.model.SAM2Large;
import ai.nets.samj.communication.model.SAM2Small;
import ai.nets.samj.communication.model.SAM2Tiny;
import ai.nets.samj.communication.model.SAMModel;
import ai.nets.samj.gui.ModelSelection.ModelSelectionListener;
import ai.nets.samj.gui.components.ComboBoxButtonComp;
import ai.nets.samj.gui.components.ComboBoxItem;
import ai.nets.samj.utils.Constants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class MainGUI extends JFrame {

	private static final long serialVersionUID = -797293687195076077L;
	

	private JPanel drawerPanel;
    private boolean isDrawerOpen = false;

	private JCheckBox chkRoiManager = new JCheckBox("Add to RoiManager", true);
	private JSwitchButton chkInstant = new JSwitchButton("LIVE", "OFF");
	private JButton go = new JButton("Go");
	private JButton close = new JButton("Close");
	private JButton help = new JButton("Help");
	private JButton export = new JButton("Export...");
	private final ModelSelection cmbModels;
	private final ImageSelection cmbImages;
	private JComboBox<String> cmbObjects = new JComboBox<String>();
	private JTabbedPane tab = new JTabbedPane();
	private JLabel drawerTitle = new JLabel();
	
	
	private static final List<SAMModel> DEFAULT_MODEL_LIST = new ArrayList<>();
	static {
		DEFAULT_MODEL_LIST.add(new SAM2Tiny());
		DEFAULT_MODEL_LIST.add(new SAM2Small());
		DEFAULT_MODEL_LIST.add(new SAM2Large());
		DEFAULT_MODEL_LIST.add(new EfficientSAM());
		DEFAULT_MODEL_LIST.add(new EfficientViTSAML2());
	}
	
	private static double HEADER_VERTIACAL_RATIO = 0.1;

	private static int MAIN_VERTICAL_SIZE = 500;
	private static int MAIN_HORIZONTAL_SIZE = 250;

    public MainGUI() {
		super(Constants.JAR_NAME + "-" + Constants.SAMJ_VERSION);
		
		ModelSelectionListener modelListener = new ModelSelectionListener() {
		    @Override
		    public void modelChanged(SAMModel selectedModel) {
		        // Perform the action needed when a new model is selected
		    }
		};

		cmbImages = ImageSelection.create(null, null);
		
		cmbModels = ModelSelection.create(DEFAULT_MODEL_LIST, modelListener);

		cmbObjects.addItem("Only Largest Object");
		cmbObjects.addItem("All Objects");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Use BorderLayout for the main frame
        setLayout(new BorderLayout());
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;

        // Add the title panel at the top
        gbc.gridy = 0;
        gbc.weighty = 0.1;
        add(createTitlePanel(), gbc);

        // Add the main center panel
        gbc.gridy = 2;
        gbc.weighty = 0.87;
        add(createCenterPanel(), gbc);

        // Add the bottom panel with buttons
        gbc.gridy = 3;
        gbc.weighty = 0.03;
        add(createBottomPanel(), gbc);

        // Set the initial size of the frame
        setSize(MAIN_HORIZONTAL_SIZE, MAIN_VERTICAL_SIZE); // Width x Height

        // Make the frame visible
        setVisible(true);
    }

    // Method to create the title panel
    private JPanel createTitlePanel() {
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(Color.LIGHT_GRAY);
        int height = (int) (HEADER_VERTIACAL_RATIO * MAIN_VERTICAL_SIZE);
        titlePanel.setPreferredSize(new Dimension(0, height)); // Fixed height

        JLabel titleLabel = new JLabel("Application Title", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));

        titlePanel.setLayout(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        return titlePanel;
    }

    // Method to create the center panel
    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 2, 5, 2); // Insets around components
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        // First component: Rectangular area with line border
        gbc.gridy = 0;
        gbc.weighty = 0.45;
        centerPanel.add(createFirstComponent(), gbc);

        // Second component: Radio button with changing panel
        gbc.gridy = 1;
        gbc.weighty = 0.45;
        centerPanel.add(createSecondComponent(), gbc);

        // Third component: Two checkboxes and a button
        gbc.gridy = 2;
        gbc.weighty = 0.1;
        centerPanel.add(createThirdComponent(), gbc);

        return centerPanel;
    }

    // Method to create the bottom panel
    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new GridBagLayout());
        bottomPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
        
        GridBagConstraints gbcb = new GridBagConstraints();
        gbcb.gridy = 0;
        gbcb.weightx = 1;
        gbcb.weighty = 1;
        gbcb.fill = GridBagConstraints.BOTH;
        
        JButton button1 = new JButton("OK");
        JButton button2 = new JButton("Cancel");

        gbcb.gridx = 0;
        bottomPanel.add(button1, gbcb);
        gbcb.gridx = 1;
        bottomPanel.add(button2, gbcb);

        return bottomPanel;
    }

    // Method to create the first component
    private JPanel createFirstComponent() {
         JPanel firstComponent = new JPanel();
        firstComponent.setLayout(new GridBagLayout());
        firstComponent.setBorder(new LineBorder(Color.BLACK));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2); // Insets around components
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        // Add the first component
        gbc.gridy = 0;
        firstComponent.add(this.cmbModels, gbc);
        // Add the second component
        gbc.gridy = 1;
        firstComponent.add(this.cmbImages, gbc);

        // Add the button
        gbc.gridy = 2;
        firstComponent.add(go, gbc);

        //cmbModels.setPreferredSize(new Dimension(MAIN_HORIZONTAL_SIZE, (int) (MAIN_VERTICAL_SIZE * 0.05)));
        //cmbImages.setPreferredSize(new Dimension(MAIN_HORIZONTAL_SIZE, (int) (MAIN_VERTICAL_SIZE * 0.05)));

        return firstComponent;
    }

    // Method to create the second component
    private JPanel createSecondComponent() {
        JPanel secondComponent = new JPanel();
        secondComponent.setLayout(new GridBagLayout());
        secondComponent.setBorder(new LineBorder(Color.BLACK));

        // Radio buttons
        JPanel radioPanel = new JPanel();
        JRadioButton radioButton1 = new JRadioButton("Option 1", true);
        JRadioButton radioButton2 = new JRadioButton("Option 2");

        ButtonGroup radioGroup = new ButtonGroup();
        radioGroup.add(radioButton1);
        radioGroup.add(radioButton2);

        radioPanel.add(radioButton1);
        radioPanel.add(radioButton2);

        // Panel below radio buttons with CardLayout
        JPanel cardPanel = new JPanel(new CardLayout());
        cardPanel.setBorder(new LineBorder(Color.BLACK));

        // First card
        JPanel card1 = new JPanel();
        card1.add(new JLabel("Content for Option 1"));

        // Second card
        JPanel card2 = new JPanel();
        card2.add(new JLabel("Content for Option 2"));

        cardPanel.add(card1, "Option 1");
        cardPanel.add(card2, "Option 2");

        // Add action listeners to radio buttons
        radioButton1.addActionListener(e -> {
            CardLayout cl = (CardLayout) (cardPanel.getLayout());
            cl.show(cardPanel, "Option 1");
        });

        radioButton2.addActionListener(e -> {
            CardLayout cl = (CardLayout) (cardPanel.getLayout());
            cl.show(cardPanel, "Option 2");
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

        return secondComponent;
    }

    // Method to create the third component
    private JPanel createThirdComponent() {
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
        JCheckBox checkBox1 = new JCheckBox("Option A");
        gbc.gridy = 0;
        thirdComponent.add(checkBox1, gbc);

        // Second checkbox
        JCheckBox checkBox2 = new JCheckBox("Option B");
        gbc.gridy = 1;
        thirdComponent.add(checkBox2, gbc);

        // Button
        JButton processButton = new JButton("Process");
        gbc.gridy = 2;
        //gbc.weighty = 1.0; // Allows the button to move with resizing
        //gbc.anchor = GridBagConstraints.NORTH;
        thirdComponent.add(processButton, gbc);
        thirdComponent.setPreferredSize(new Dimension(0, 0));

        return thirdComponent;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainGUI());
    }
}
