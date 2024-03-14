/*-
 * #%L
 * Library to call models of the family of SAM (Segment Anything Model) from Java
 * %%
 * Copyright (C) 2024 SAMJ developers.
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package ai.nets.samj.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import ai.nets.samj.communication.model.SAMModel;
import ai.nets.samj.communication.model.SAMModels;
import ai.nets.samj.gui.components.ComboBoxItem;
import ai.nets.samj.gui.components.GridPanel;
import ai.nets.samj.gui.icons.ButtonIcon;
import ai.nets.samj.gui.icons.LoadingButton;
import ai.nets.samj.gui.tools.Tools;
import ai.nets.samj.ui.PromptsResultsDisplay;
import ai.nets.samj.ui.PromptsResultsDisplay.SAMJException;
import ai.nets.samj.ui.SAMJLogger;
import ai.nets.samj.ui.UtilityMethods;
import ai.nets.samj.SamEnvManager;

/**
 * Class that creates a default graphical user interface to interact with SAMJ models
 * @author Carlos Garcia
 * @author Daniel Sage
 * @author Vladimir Ulman
 */
public class SAMJDialog extends JPanel implements ActionListener, PopupMenuListener {

	/**
	 * Unique serial version identifier
	 */
	private static final long serialVersionUID = -4362794696325316195L;
	/**
	 * Name of the folder where the icon images for the dialog buttons are within the resources folder
	 */
	private static final String RESOURCES_FOLDER = "icons_samj/";
	/**
	 * Button that closes the GUI
	 */
	private JButton bnClose = new JButton("Close");
	/**
	 * Button that opens the help button
	 */
	private JButton bnHelp = new JButton("Help");
	/**
	 * Button that when pressed runs the SAMJ model encoder on the image selected
	 */
	
	private LoadingButton bnStart;
	/** TODO will this ever happen?
	 * Button for the auto-complete function, it is not ready yet
	 */
	private JButton bnComplete = new JButton("Coming soon...");
	/**
	 * Button to export the ROIs of an image to a instance segmentation labeling
	 */
	private JButton bnRoi2Labeling = new JButton("Export as Label");
	/**
	 * Text field containing copyrigth info
	 */
	private JTextField txtStatus = new JTextField("(c) SAMJ team 2024 - EARLY RELEASE");
	/**
	 * Button that activates the annotation with SAMJ models using bounding box prompts
	 */
	private ButtonIcon bnRect = new ButtonIcon("Rect", RESOURCES_FOLDER, "rect_samj.png");
	/**
	 * Button that activates the annotation with SAMJ models using point or multiple points prompts
	 */
	private ButtonIcon bnPoints = new ButtonIcon("Points", RESOURCES_FOLDER, "points_samj.png");
	/**
	 * Button that activates the annotation with SAMJ models using freeline prompts drawn with a freeline
	 */
	private ButtonIcon bnBrush = new ButtonIcon("Brush", RESOURCES_FOLDER, "brush_samj.png");
	/**
	 * Button that allows selecting a mask as a prompt for the image
	 */
	private ButtonIcon bnMask = new ButtonIcon("Mask", RESOURCES_FOLDER, "help_samj.png");
	/**
	 * Key that specifies that the user only wants the model to return the largest ROI
	 */
	private static final String ONLY_BIGGEST = "Only return largest ROI";
	/**
	 * Key that specifies that the user wants to return every ROI produced by the model
	 */
	private static final String ALL = "Return all ROIs";
	/**
	 * Combobox to choose whether to return only the biggest ROI produced by a SAM-based model
	 * or to return all what is outputed by the model
	 */
	private JComboBox<String> cmbROIs = new JComboBox<String>(new String[] {ALL, ONLY_BIGGEST});
	/**
	 * Checkbox that defines whether to add the segmentations to the roi manager or not
	 */
	private JCheckBox chkROIManager = new JCheckBox("Add to ROI Manager", true);
	/**
	 * Combo box containing the images that are currently open
	 */
	private JComboBox<ComboBoxItem> cmbImage = new JComboBox<ComboBoxItem>();
	/**
	 * Panel containing the model choice and HTML panel with info about the models or installation
	 */
	private final SAMModelPanel panelModel;
	/**
	 * Logger for the GUI actions
	 */
	private final SAMJLogger GUIsOwnLog;
	/**
	 * Logger for the SAMJ models
	 */
	private final SAMJLogger logForNetworks;
	/**
	 * Interface implementation that contains methods specific to each consumer software
	 */
	private final UtilityMethods consumerMethods;
	/**
	 * Unique identifier of the selected image
	 */
	private Integer selectedID = null;
	/**
	 * Functional interface that allows creating different instances of SAMJ models for different images
	 */
	public interface PromptsFunctionalInterface { PromptsResultsDisplay getPrompts(Object image); }
	/**
	 * Implementation of the {@link PromptsFunctionalInterface} in the consumer software that allows the consumer
	 * software to provide prompts to the image of interest
	 */
	private PromptsFunctionalInterface displayInterface;
	/**
	 * Implementation of the {@link PromptsResultsDisplay} in the consumer software that allows the consumer
	 * software to provide prompts to the image of interest
	 */
	private PromptsResultsDisplay display;
	/**
	 * Whether the encodings for the current image are available or the image encoder needs to be
	 * run to provide annotations
	 */
	private boolean encodingsDone = false;
	
	/**
	 * Constructor that creates the default GUI for SAMJ. This GUI lets the user decide between
	 * several SAM based models. It also lets the user install all the models with just one click.
	 * Once installed the user can use any of the models to annotate providing the needed prompts
	 * in the way of the consumer software.
	 * @param availableModel
	 * 	all SAM-based models available to be used
	 * @param consumerMethods
	 * 	interface implementing methods on the consumer software
	 */
	public SAMJDialog(final SAMModels availableModel, UtilityMethods consumerMethods) {
		this(availableModel, consumerMethods, null, null);
	}

	/**
	 * Constructor that creates the default GUI for SAMJ. This GUI lets the user decide between
	 * several SAM based models. It also lets the user install all the models with just one click.
	 * Once installed the user can use any of the models to annotate providing the needed prompts
	 * in the way of the consumer software.
	 * @param availableModel
	 * 	all SAM-based models available to be used
	 * @param consumerMethods
	 * 	interface implementing methods on the consumer software
	 * @param guilogger
	 * 	interface implemented on the consumer software to log the events on the GUI
	 */
	public SAMJDialog(final SAMModels availableModel,
						UtilityMethods consumerMethods,
	                    final SAMJLogger guilogger) {
		this(availableModel, consumerMethods, guilogger, null);
	}

	/**
	 * Constructor that creates the default GUI for SAMJ. This GUI lets the user decide between
	 * several SAM based models. It also lets the user install all the models with just one click.
	 * Once installed the user can use any of the models to annotate providing the needed prompts
	 * in the way of the consumer software.
	 * @param availableModel
	 * 	all SAM-based models available to be used
	 * @param consumerMethods
	 * 	interface implementing methods on the consumer software
	 * @param guilogger
	 * 	interface implemented on the consumer software to log the events on the GUI
	 * @param networkLogger
	 * 	interface implemented on the consumer software to log the SAMJ models events
	 */
	public SAMJDialog(final SAMModels availableModel, UtilityMethods consumerMethods,
	                  final SAMJLogger guilogger,
	                  final SAMJLogger networkLogger) {
		// TODO super(new JFrame(), "SAMJ Annotator");
		if (guilogger == null) {
			this.GUIsOwnLog = new SAMJLogger () {
				@Override
				public void info(String text) {}
				@Override
				public void warn(String text) {}
				@Override
				public void error(String text) {}
			};
		} else {
			this.GUIsOwnLog = guilogger;
		}
		if (networkLogger == null) {
			this.logForNetworks = new SAMJLogger () {
				@Override
				public void info(String text) {}
				@Override
				public void warn(String text) {}
				@Override
				public void error(String text) {}
			};
		} else {
			this.logForNetworks = networkLogger;
		}
		this.consumerMethods = consumerMethods;

		panelModel = new SAMModelPanel(availableModel, (boolean bol) -> this.updateInterface(bol));
		// Buttons
		JPanel pnButtons = new JPanel(new FlowLayout());
		pnButtons.add(bnRect);
		pnButtons.add(bnPoints);
		pnButtons.add(bnBrush);
		// TODO decide what to do pnButtons.add(bnMask);
		
		// Status
		JToolBar pnStatus = new JToolBar();
		pnStatus.setFloatable(false);
		pnStatus.setLayout(new BorderLayout());
		pnStatus.add(bnHelp, BorderLayout.EAST);
		txtStatus.setEditable(false);
		pnStatus.add(txtStatus, BorderLayout.CENTER);
		pnStatus.add(bnClose, BorderLayout.WEST);

		JPanel pnActions = new JPanel(new FlowLayout());
		pnActions.add(bnRoi2Labeling);
		pnActions.add(bnComplete);
		pnActions.add(cmbROIs);
		pnActions.add(chkROIManager);
		
		List<ComboBoxItem> listImages = this.consumerMethods.getListOfOpenImages();
		for(ComboBoxItem item : listImages)
			cmbImage.addItem(item);
		cmbImage.addPopupMenuListener(this);
		
		GridBagLayout middleLayout = new GridBagLayout();
		//middleLayout.columnWidths = new int[] {1, 5};
		middleLayout.columnWeights = new double[] {1.0, 10.0};
		middleLayout.rowHeights = new int[] {1};
		JPanel panelImage = new JPanel(middleLayout);
		panelImage.setBorder(BorderFactory.createEtchedBorder());
		
		GridBagConstraints gbc0 = new GridBagConstraints();
		gbc0.fill = GridBagConstraints.HORIZONTAL;
		gbc0.gridx = 0;
		gbc0.insets = new Insets(5, 5, 5, 0);
		bnStart = new LoadingButton("Go!", RESOURCES_FOLDER, "loading_animation_samj.gif", 20);
		panelImage.add(bnStart, gbc0);
		GridBagConstraints gbc1 = new GridBagConstraints();
		gbc1.fill = GridBagConstraints.HORIZONTAL;
		gbc1.gridx = 1;
		gbc1.insets = new Insets(5, 5, 5, 6);
		panelImage.add(cmbImage, gbc1);
		cmbImage.setPreferredSize(new Dimension(panelModel.getWidth(), 25));
		
		GridPanel pn = new GridPanel();
		pn.place(1, 1, panelModel);
		pn.place(2, 1, panelImage);
		pn.place(3, 1, pnButtons);
		pn.place(4, 1, pnActions);
		
		setLayout(new BorderLayout());
		add(pn, BorderLayout.NORTH);		
		add(pnStatus, BorderLayout.SOUTH);		

		bnRoi2Labeling.addActionListener(this);
		// TODO not ready yet bnComplete.addActionListener(this);
		bnRoi2Labeling.setEnabled(false);
		bnComplete.setEnabled(false);
		bnClose.addActionListener(this);
		bnHelp.addActionListener(this);
		chkROIManager.addActionListener(this);
		cmbROIs.addPopupMenuListener(this);
		
		bnStart.addActionListener(this);
		bnRect.addActionListener(this);
		bnPoints.addActionListener(this);
		bnBrush.addActionListener(this);
		// TODO decide what to do bnMask.addActionListener(this);
		// TODO decide what to do bnMask.setDropTarget(new LocalDropTarget());
		
		add(pn);
		updateInterface();
	}
	
	/**
	 * Set the implementation of {@link PromptsFunctionalInterface} in the consumer software that allows working with
	 * image objects native to the consumer software
	 * @param pp
	 * 	the implementation of {@link PromptsFunctionalInterface} in the consumer software 
	 */
	public void setPromptsProvider(PromptsFunctionalInterface pp) {
		this.displayInterface = pp;
	}
	
	/**
	 * 
	 * @return the {@link SamEnvManager} used to install and mange the models
	 */
	public SamEnvManager getModelInstallationManager() {
		return this.panelModel.getInstallationManager();
	}

	@Override
	/**
	 * Method that controls that the logic of the buttons is correct
	 */
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == bnRect && !bnRect.isSelected()) {
			display.switchToUsingRectangles();
			bnRect.setPressed(true); bnPoints.setPressed(false); bnBrush.setPressed(false);
		} else if (e.getSource() == bnPoints && !bnPoints.isSelected()) {
			display.switchToUsingPoints();
			bnRect.setPressed(false); bnPoints.setPressed(true); bnBrush.setPressed(false);
		} else if (e.getSource() == bnBrush && !bnBrush.isSelected()) {
			display.switchToUsingBrush();
			bnRect.setPressed(false); bnPoints.setPressed(false); bnBrush.setPressed(true);
		} else if (e.getSource() == bnRect || e.getSource() == bnPoints || e.getSource() == bnBrush) {
			display.switchToNone();
			bnRect.setPressed(false); bnPoints.setPressed(false); bnBrush.setPressed(false);
		} else if (e.getSource() == bnHelp) {
			Tools.help();
		} else if (e.getSource() == bnClose) {
			this.close();
		} else if (e.getSource() == bnComplete) {
			GUIsOwnLog.warn("TO DO call Auto-complete");
		} else if (e.getSource() == bnStart) {
			if (!panelModel.getSelectedModel().isInstalled())
				GUIsOwnLog.warn("Not starting encoding as the selected model is not installed.");

			GUIsOwnLog.warn("Start the encoding");
			if (display == null || !display.getFocusedImage().equals(((ComboBoxItem) this.cmbImage.getSelectedItem()).getValue()))
				display = displayInterface.getPrompts(((ComboBoxItem) this.cmbImage.getSelectedItem()).getValue());
			SAMModel selectedSAMModel = this.panelModel.getSelectedModel();
			this.bnStart.setPressed(true);
			new Thread(() -> {
				SAMModel netAdapter = null;
				SwingUtilities.invokeLater(() -> {
					panelModel.bnUninstall.setEnabled(false);
					panelModel.bnInstall.setEnabled(false);
					panelModel.rbModels.stream().forEach(btn -> btn.setEnabled(false));
					panelModel.progressInstallation.setIndeterminate(true);
				});
				try {
					netAdapter = panelModel
						.getSelectedModel()
						.instantiate(display.giveProcessedSubImage(selectedSAMModel), logForNetworks);
				} catch (Exception ex) {
					display.notifyException(SAMJException.ENCODING, ex);
				}
				SwingUtilities.invokeLater(() -> {
					this.bnStart.setPressed(false);
					panelModel.bnUninstall.setEnabled(panelModel.getSelectedModel().isInstalled());
					panelModel.bnInstall.setEnabled(!panelModel.getSelectedModel().isInstalled());
					panelModel.rbModels.stream().forEach(btn -> btn.setEnabled(true));
					panelModel.progressInstallation.setIndeterminate(false);
					panelModel.progressInstallation.setValue(100);
				});
				if (netAdapter == null) return;
				display.switchToThisNet(netAdapter);
				GUIsOwnLog.warn("Finished the encoding");
				SwingUtilities.invokeLater(() -> {
					//TODO: encoding should be a property of a model
					this.setEncodingsDone(true);
					updateInterface();
				});
			}).start();
			return;
		} else if (e.getSource() == chkROIManager) {
			if (display != null)
				display.enableAddingToRoiManager(chkROIManager.isSelected());
		} else if (e.getSource() == this.bnMask) {
			JFileChooser fileChooser = new JFileChooser();
            int returnValue = fileChooser.showOpenDialog(null);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                this.display.improveExistingMask(fileChooser.getSelectedFile());
            }
		} else if (e.getSource() == this.bnRoi2Labeling) {
			this.display.exportImageLabeling();
		}

		updateInterface();
	}

	/**
	 * Update the interface
	 */
	public void updateInterface() {
		updateInterface(false);
	}

	/**
	 * Update the interface deleting the encodings and current Python process if needed
	 * @param deleteEncodings
	 * 	whether to delete or not the current encodings and the current Python process
	 */
	public void updateInterface(boolean deleteEncodings) {
		if (deleteEncodings) this.setEncodingsDone(false);
		if (!this.panelModel.isSelectedModelInstalled()) {
			this.bnStart.setEnabled(false);
			this.cmbImage.setEnabled(false);
			setEncodingsDone(false);
		} else if (this.panelModel.isSelectedModelInstalled()
				&& !this.encodingsDone
				&& this.cmbImage.getSelectedItem() != null 
				&& ((ComboBoxItem) this.cmbImage.getSelectedItem()).getId() != -1) {
			this.bnStart.setEnabled(true);
			this.cmbImage.setEnabled(true);
			this.chkROIManager.setEnabled(true);
			this.cmbROIs.setEnabled(true);
		} else if (this.panelModel.isSelectedModelInstalled()
				&& this.cmbImage.getSelectedItem() != null 
				&& ((ComboBoxItem) this.cmbImage.getSelectedItem()).getId() != -1) {
			this.bnStart.setEnabled(false);
			this.cmbImage.setEnabled(true);
		} else if (this.panelModel.isSelectedModelInstalled()) {
			this.bnStart.setEnabled(false);
			this.cmbImage.setEnabled(true);
		}
		// TODO not ready yet bnComplete.setEnabled(this.encodingsDone);
		bnRoi2Labeling.setEnabled(this.encodingsDone);
		chkROIManager.setEnabled(this.encodingsDone);
		cmbROIs.setEnabled(this.encodingsDone);
		bnRect.setEnabled(this.encodingsDone);
		bnPoints.setEnabled(this.encodingsDone);
		bnBrush.setEnabled(this.encodingsDone);
		bnMask.setEnabled(this.encodingsDone);
		if (!encodingsDone) {
			this.bnRect.setPressed(false);
			this.bnPoints.setPressed(false);
			this.bnBrush.setPressed(false);
			this.bnMask.setPressed(false);
		}
	}
	
	/**
	 * Set whether the encodings for the current image have been already calculated or not.
	 * If true, the encodings for the selected image do not need to be calculated until the iuser
	 * selects another image
	 * @param isDone
	 * 	whether the encodings for the current image have been calculated or not
	 */
	private void setEncodingsDone(boolean isDone) {
		this.encodingsDone = isDone;
		if (!isDone) {
			if (display != null) {
				display.notifyNetToClose();
				display.switchToNone();
			}
			this.bnRect.setPressed(false);
			this.bnPoints.setPressed(false);
			this.bnBrush.setPressed(false);
			this.bnMask.setPressed(false);
		}
	}

	/**
	 * Class that implements drag and drop for the mask prompt
	 */
	public class LocalDropTarget extends DropTarget {

		private static final long serialVersionUID = 286813958463411816L;

		@Override
		/**
		 * Enable drag and drop on the mask prompt button and calculate the annotation once the 
		 * mask is dropped
		 */
		public void drop(DropTargetDropEvent e) {
			e.acceptDrop(DnDConstants.ACTION_COPY);
			e.getTransferable().getTransferDataFlavors();
			Transferable transferable = e.getTransferable();
			DataFlavor[] flavors = transferable.getTransferDataFlavors();
			for (DataFlavor flavor : flavors) {
				if (flavor.isFlavorJavaFileListType()) {
					try {
						List<File> files = (List<File>) transferable.getTransferData(flavor);
						for (File file : files) {
							GUIsOwnLog.info("Taking mask from file " + file.getAbsolutePath());
							display.improveExistingMask(file);
						}
					}
					catch (UnsupportedFlavorException ex) {
						ex.printStackTrace();
					}
					catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
			e.dropComplete(true);
			super.drop(e);
		}
	}
	
	/**
	 * Close the panel and the plugin, deleting the python process if it exists
	 */
	public void close() {
		if (display != null) {
			display.notifyNetToClose();
			display.switchToNone();
		}
		display = null;
		this.panelModel.interrupExistingThreads();
		if (SwingUtilities.windowForComponent(this).isDisplayable())
			SwingUtilities.windowForComponent(this).dispose();
	}

	@Override
	/**
	 * Update the list of images opened once the combobox pop up is open
	 */
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		if (e.getSource() == this.cmbROIs) return;
		Object item = this.cmbImage.getSelectedItem();
        List<ComboBoxItem> openSeqs = consumerMethods.getListOfOpenImages();
        ComboBoxItem[] objects = new ComboBoxItem[openSeqs.size()];
        for (int i = 0; i < objects.length; i ++) objects[i] = openSeqs.get(i);
        DefaultComboBoxModel<ComboBoxItem> comboBoxModel = new DefaultComboBoxModel<ComboBoxItem>(objects);
        this.cmbImage.setModel(comboBoxModel);
        if (item != null && objects.length != 0) 
        	this.cmbImage.setSelectedIndex(
        			IntStream.range(0, objects.length).filter(i -> objects[i].getId() == ((ComboBoxItem) item).getId()).findFirst().orElse(0)
        			);
	}

	@Override
	/**
	 * Check if the image selected has been changed once the combobox pop up is closed
	 */
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		if (e.getSource() == this.cmbROIs) {
			if (display != null) 
				display.getNetBeingUsed().setReturnOnlyBiggest(cmbROIs.getSelectedItem().equals(ONLY_BIGGEST));
			return;
		}
		ComboBoxItem item = (ComboBoxItem) this.cmbImage.getSelectedItem();
		if ( item == null || (item != null && item.getId() == -1) ) {
			setEncodingsDone(false);
			selectedID = null;
		} else if (selectedID == null || (selectedID != null && selectedID != item.getId())) {
			setEncodingsDone(false);
        	selectedID = item.getId();
		}
    	this.updateInterface();
	}

	@Override
	/**
	 * Nothing
	 */
	public void popupMenuCanceled(PopupMenuEvent e) {
	}
}
