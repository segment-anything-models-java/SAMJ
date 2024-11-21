package ai.nets.samj.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import ai.nets.samj.ui.UtilityMethods;
import ai.nets.samj.utils.Constants;
import io.bioimage.modelrunner.model.Model;
import ai.nets.samj.gui.ModelSelection.ModelSelectionListener;


public class Dialog extends JFrame implements ActionListener {

	private static final long serialVersionUID = -797293687195076077L;
	private int count = 1;
	private JCheckBox chkRoiManager = new JCheckBox("Add to RoiManager", true);
	private JSwitchButton chkInstant = new JSwitchButton("LIVE", "OFF");
	private GridPanel pn2;
	private GridPanel pn3;
	private JPanel drawerPanel;
    private boolean isDrawerOpen = false;

	private JButton go = new JButton("Go");
	private JButton close = new JButton("Close");
	private JButton help = new JButton("Help");
	private JButton export = new JButton("Export...");
	private final ModelSelection cmbModels;
	private final ImageSelection cmbImages;
	private JComboBox<String> cmbObjects = new JComboBox<String>();
	private JTabbedPane tab = new JTabbedPane();
	private JLabel drawerTitle = new JLabel();

	public Dialog(UtilityMethods consumerUtils) {
		super(Constants.JAR_NAME + "-" + Constants.SAMJ_VERSION);
		
		ModelSelectionListener modelListener = new ModelSelectionListener() {
		    @Override
		    public void modelChanged(Model selectedModel) {
		        // Perform the action needed when a new model is selected
		    }
		};

		cmbImages = ImageSelection.create(consumerUtils, null);
		cmbModels = ModelSelection.create(null, modelListener);

		cmbObjects.addItem("Only Largest Object");
		cmbObjects.addItem("All Objects");

		GridPanel pnm = new GridPanel(false, 2);
		pnm.place(0, 0, new JLabel("Instant Annotation"));
		pnm.place(1, 0, chkInstant);

		GridPanel pna = new GridPanel(false, 2);
		pna.place(0, 0, new JLabel("Selection from ROIManager"));
		pna.place(1, 0, new JButton("Batch SAMize"));

		GridPanel pn1 = new GridPanel(true, 2);
		pn1.place(0, 0, cmbModels);
		pn1.place(1, 0, cmbImages);
		pn1.place(2, 0, go);

		pn2 = new GridPanel(true, 2);
		tab.addTab("Manual", pnm);
		tab.addTab("Preset Prompts", pna);

		cmbObjects.setPreferredSize(new Dimension(200, 24));
		pn3 = new GridPanel(false, 2);
		pn3.place(1, 0, 1, 1, chkRoiManager);
		pn3.place(2, 0, 1, 1, cmbObjects);
		pn3.place(3, 0, 1, 1, export);

		JToolBar pnAction = new JToolBar();
		pnAction.setLayout(new GridLayout(1, 2));
		pnAction.setBorder(BorderFactory.createEtchedBorder());
		pnAction.setFloatable(false);
		pnAction.add(help);
		pnAction.add(close);

		GridPanel pn = new GridPanel(true, 5);
		pn.place(0, 0, 1, 1, pn1);
		pn.place(1, 0, 1, 1, tab);
		pn.place(2, 0, 1, 1, pn3);

		String text = "<html><div style='text-align: center; font-size: 15px;'>"
				+ "<span style='color: black;'>SAM</span>" + "<span style='color: red;'>J</span>";
		JLabel title = new JLabel(text, SwingConstants.CENTER);

		JPanel main = new JPanel(new BorderLayout());
		main.add(title, BorderLayout.NORTH);
		main.add(pn, BorderLayout.CENTER);
		main.add(pnAction, BorderLayout.SOUTH);

		setLayout(new BorderLayout());

		JToolBar pnInstallModel = new JToolBar();
		pnInstallModel.setLayout(new GridLayout(1, 2));
		pnInstallModel.setBorder(BorderFactory.createEtchedBorder());
		pnInstallModel.setFloatable(false);
		pnInstallModel.add(new JButton("Install"));
		pnInstallModel.add(new JButton("Uninstall"));

        drawerPanel = new JPanel();
        drawerPanel.setPreferredSize(new Dimension(200, 300));
        drawerPanel.setLayout(new BorderLayout());
        drawerPanel.setBorder(BorderFactory.createEtchedBorder());
        drawerTitle.setText("<html><div style='text-align: center; font-size: 15px;'>&nbsp;</html>");
        drawerPanel.add(drawerTitle, BorderLayout.NORTH);
        drawerPanel.add(pnInstallModel, BorderLayout.SOUTH);
        HTMLPane html = new HTMLPane("Arial", "#000", "#CCCCCC", 200, 200);
        //html.append("<span style='text-align: center; font-size: 20px;>SAM2 Tiny</span");
        html.append("Model description");
        html.append("Model description");
        html.append("Model description");
        html.append("");
        html.append("i", "Other information");
        html.append("i", "References");
        drawerPanel.add(html, BorderLayout.CENTER);
        drawerPanel.setVisible(false);
		add(main, BorderLayout.CENTER);
		pack();
		setVisible(true);

		this.cmbModels.getButton().addActionListener(this);
		go.addActionListener(this);
		help.addActionListener(this);
		close.addActionListener(this);
		chkRoiManager.setEnabled(false);
		chkInstant.setEnabled(false);
		pn2.setEnabled(false);
		pn3.setEnabled(false);
		tab.setEnabled(false);
		cmbObjects.setEnabled(false);
		export.setEnabled(false);
		chkInstant.setSelected(true);
	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		if (ev.getSource() == close) {
			dispose();
		}
		if (ev.getSource() == this.cmbModels.getButton()) {
			toggleDrawer();
		}
		if (ev.getSource() == go) {
			chkInstant.setEnabled(true);
			chkRoiManager.setEnabled(true);
			tab.setEnabled(true);
			pn2.setEnabled(true);
			pn3.setEnabled(true);
			cmbObjects.setEnabled(true);
			export.setEnabled(true);
		}
	}

	private void toggleDrawer() {
		if (isDrawerOpen) {
			drawerPanel.setVisible(false);
			remove(drawerPanel);
			setSize(getWidth() - 200, getHeight());
			this.cmbModels.getButton().setText("▶"); 
		} else {
			add(drawerPanel, BorderLayout.EAST);
			drawerPanel.setVisible(true);
			setSize(getWidth() + 200, getHeight());
			this.cmbModels.getButton().setText("◀");
		}
		isDrawerOpen = !isDrawerOpen;
		revalidate(); 
		repaint(); 
	}
	
	public static void main(String[] args) {
		Dialog dialog = new Dialog();
		dialog.setVisible(true);
	}
}
