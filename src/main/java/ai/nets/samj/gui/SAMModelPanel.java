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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Objects;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import ai.nets.samj.communication.model.EfficientSAM;
import ai.nets.samj.communication.model.EfficientViTSAML0;
import ai.nets.samj.communication.model.EfficientViTSAML1;
import ai.nets.samj.communication.model.EfficientViTSAML2;
import ai.nets.samj.communication.model.EfficientViTSAMXL0;
import ai.nets.samj.communication.model.EfficientViTSAMXL1;
import ai.nets.samj.communication.model.SAMModel;
import ai.nets.samj.communication.model.SAMModels;
import ai.nets.samj.gui.components.GridPanel;
import ai.nets.samj.gui.components.HTMLPane;
import ai.nets.samj.gui.tools.Files;
import io.bioimage.modelrunner.apposed.appose.Mamba;
import ai.nets.samj.SamEnvManager;

/**
 * Class that creates a subpanel in the main panel of SAMJ default GUI.
 * This panel handles model selection and installation.
 * @author Carlos Garcia
 * @author Daniel Sage
 * @author Vladimir Ulman
 */
public class SAMModelPanel extends JPanel implements ActionListener {
	/**
	 * Unique serial identifier
	 */
	private static final long serialVersionUID = 7623385356575804931L;
	/**
	 * HTML panel where all the info about models and installation is going to be shown
	 */
	private HTMLPane info = new HTMLPane(450, 135);
	/**
	 * Parameter used in the HTML panel during installation to know when to update
	 */
    private int waitingIter = 0;
	/**
	 * Button that when clicked installs the model selected
	 */
	private JButton bnInstall = new JButton("Install");
	/**
	 * Button that when clicked uninstalls the model selected
	 */
	private JButton bnUninstall = new JButton("Uninstall");
	/**
	 * Progress bar used during the model installation. If the model is already installed it
	 * is full, if it is not it is empty
	 */
	private JProgressBar progressInstallation = new JProgressBar();
	/**
	 * List of radio buttons that point to the models available
	 */
	private ArrayList<JRadioButton> rbModels = new ArrayList<JRadioButton>();
	/**
	 * Object contianing a list of the models available, and whether they are selected, installed...
	 */
	private SAMModels models;
	/**
	 * Object that manages the models and their installation
	 */
	private final SamEnvManager manager;
	/**
	 * Index of hte selected model in the list of available models
	 */
	private int selectedModel = 0;
	/**
	 * Whether the model selected has changed or not
	 */
	private boolean modelChanged = false;
	/**
	 * Time format using to update the installation information
	 */
	private static DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
	/**
	 * Interface implemented at {@link SAMJDialog} to tell the parent JPanel to update the
	 * interface
	 */
	interface CallParent {
		/**
		 * The implemented task to be done at {@link SAMJDialog}
		 * @param bool
		 * 	some helper parameter
		 */
		public void task(boolean bool);
	}
	/**
	 * Implementation of the interface {@link CallParent}
	 */
	private CallParent updateParent;
	
	/**
	 * Constructor of the class. Creates a panel that contains the selection of available models
	 * and an html panel with info about the models that also displays the installation progress
	 * when the model is being installed
	 * @param models
	 * 	list of models that are available
	 * @param updateParent
	 * 	interface implementation on {@link SAMJDialog} that allows making modifications in the parent GUI
	 */
	public SAMModelPanel(SAMModels models, CallParent updateParent) {
		super();
		this.updateParent = updateParent;
		manager = SamEnvManager.create((str) -> addHtml(str));
		this.models = models;
		JToolBar pnToolbarModel = new JToolBar();
		pnToolbarModel.setFloatable(false);
		pnToolbarModel.setLayout(new GridLayout(1, 2));
		pnToolbarModel.add(bnInstall);
		pnToolbarModel.add(bnUninstall);
		
		ButtonGroup group = new ButtonGroup();
		for(SAMModel model : models) {
			if (model.getName().equals(EfficientSAM.FULL_NAME)) 
				model.setInstalled(manager.checkEfficientSAMPythonInstalled() 
						&& manager.checkEfficientSAMSmallWeightsDownloaded() 
						&& manager.checkEfficientSAMPackageInstalled());
			else if (model.getName().equals(EfficientViTSAML0.FULL_NAME))
				model.setInstalled(manager.checkEfficientViTSAMPythonInstalled() 
						&& manager.checkEfficientViTSAMPackageInstalled() && manager.checkEfficientViTSAMWeightsDownloaded("l0"));
			else if (model.getName().equals(EfficientViTSAML1.FULL_NAME))
				model.setInstalled(manager.checkEfficientViTSAMPythonInstalled() 
						&& manager.checkEfficientViTSAMPackageInstalled() && manager.checkEfficientViTSAMWeightsDownloaded("l1"));
			else if (model.getName().equals(EfficientViTSAML2.FULL_NAME))
				model.setInstalled(manager.checkEfficientViTSAMPythonInstalled() 
						&& manager.checkEfficientViTSAMPackageInstalled() && manager.checkEfficientViTSAMWeightsDownloaded("l2"));
			else if (model.getName().equals(EfficientViTSAMXL0.FULL_NAME))
				model.setInstalled(manager.checkEfficientViTSAMPythonInstalled() 
						&& manager.checkEfficientViTSAMPackageInstalled() && manager.checkEfficientViTSAMWeightsDownloaded("xl0"));
			else if (model.getName().equals(EfficientViTSAMXL1.FULL_NAME))
				model.setInstalled(manager.checkEfficientViTSAMPythonInstalled() 
						&& manager.checkEfficientViTSAMPackageInstalled() && manager.checkEfficientViTSAMWeightsDownloaded("xl1"));
			JRadioButton rb = new JRadioButton(model.getName(), model.isInstalled());
			rbModels.add(rb);
			rb.addActionListener(this);
			group.add(rb);
		}
		rbModels.get(0).setSelected(true);
	
		JPanel pnManageModel = new JPanel(new BorderLayout());
		pnManageModel.add(pnToolbarModel, BorderLayout.NORTH);
		pnManageModel.add(progressInstallation, BorderLayout.SOUTH);
		pnManageModel.add(new JScrollPane(info), BorderLayout.CENTER);
		
		GridPanel pnModel = new GridPanel(true);
		int col = 1;
		for(JRadioButton rb : rbModels)
			pnModel.place(1, col++, 1, 1, rb);
		
		pnModel.place(2, 1, 6, 2, pnManageModel);
		
		add(pnModel);
		info.append("p", "Description of the model");
		info.append("p", "Link to source");
		bnInstall.addActionListener(this);
		bnUninstall.addActionListener(this);
		
		updateInterface();
	}
	
	private void updateInterface() {
		for(int i=0; i<rbModels.size(); i++) {
			if (!rbModels.get(i).isSelected()) continue;
			modelChanged = selectedModel != i;
			selectedModel = i;
			info.clear();
			info.append("p", models.get(i).getDescription());
			bnInstall.setEnabled(!models.get(i).isInstalled());
			bnUninstall.setEnabled(models.get(i).isInstalled());
			this.progressInstallation.setValue(models.get(i).isInstalled() ? 100 : 0);
			break;
		}
	}
	
	/**
	 * 
	 * @return whether the selected model is installed or not
	 */
	public boolean isSelectedModelInstalled() {
		for(int i=0; i<rbModels.size(); i++) {
			if (!rbModels.get(i).isSelected()) continue;
			return models.get(i).isInstalled();
		}
		return false;
	}
	
	/**
	 * 
	 * @return the selected model
	 */
	public SAMModel getSelectedModel() {
		for(int i=0; i<rbModels.size(); i++) {
			if (rbModels.get(i).isSelected())
				return models.get(i);
		}
		return null;
	}
	
	/**
	 * 
	 * @return the installation manager
	 */
	public SamEnvManager getInstallationManager() {
		return this.manager;
	}
	
	/**
	 * 
	 * @return true if the current model allows installation, thus it is not installed and false otherwise
	 */
	public boolean isInstallationEnabled() {
		return this.bnInstall.isEnabled();
	}
	
	/**
	 * Create the Thread that is used to install the selected model using {@link SamEnvManager}
	 * @return the thread where the models will be installed
	 */
	private Thread createInstallationThread() {
		Thread installThread = new Thread(() -> {
			try {
				SwingUtilities.invokeLater(() -> installationInProcess(true));
				if (getSelectedModel().getName().equals(EfficientSAM.FULL_NAME))
					this.manager.installEfficientSAMSmall();
				else if (getSelectedModel().getName().equals(EfficientViTSAML0.FULL_NAME))
					this.manager.installEfficientViTSAM("l0");
				else if (getSelectedModel().getName().equals(EfficientViTSAML1.FULL_NAME))
					this.manager.installEfficientViTSAM("l1");
				else if (getSelectedModel().getName().equals(EfficientViTSAML2.FULL_NAME))
					this.manager.installEfficientViTSAM("l2");
				else if (getSelectedModel().getName().equals(EfficientViTSAMXL0.FULL_NAME))
					this.manager.installEfficientViTSAM("xl0");
				else if (getSelectedModel().getName().equals(EfficientViTSAMXL1.FULL_NAME))
					this.manager.installEfficientViTSAM("xl1");
				else 
					throw new RuntimeException();
				getSelectedModel().setInstalled(true);
				SwingUtilities.invokeLater(() -> {
					installationInProcess(false);
					this.updateParent.task(false);});
			} catch (Exception e1) {
				e1.printStackTrace();
				SwingUtilities.invokeLater(() -> {installationInProcess(false); this.updateParent.task(false);});
			}
		});
		return installThread;
	}
	
	/**
	 * Create a thread that is used to monitor the installation and provide some feedback to the user
	 * @param installThread
	 * 	the thread where the installation is being done
	 * @return the thread used to control the installation thread
	 */
	private Thread createControlThread(Thread installThread) {
		Thread currentThread = Thread.currentThread();
		Thread controlThread = new Thread(() -> {
			while (currentThread.isAlive() && installThread.isAlive()) {
				try{ Thread.sleep(50); } catch(Exception ex) {}
			}
			if (!currentThread.isAlive()) installThread.interrupt();
		});
		return controlThread;
	}
	
	@Override
	/**
	 * Mange the interface actions and logic
	 */
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource() == bnInstall) {
			Thread installThread = createInstallationThread();
			installThread.start();
			Thread controlThread = createControlThread(installThread);
			controlThread.start();
		} else if (e.getSource() == bnUninstall) {
			uninstallModel();
		}
		
		updateInterface();
		this.updateParent.task(modelChanged);
		modelChanged = false;
	}
	
	/**
	 * Uninstall the model from the user computer. Delete the required files
	 */
	private void uninstallModel() {
		if (getSelectedModel().getName().equals(EfficientSAM.FULL_NAME)) {
			Files.deleteFolder(new File(manager.getEfficientSAMPythonEnv()));
			Files.deleteFolder(new File(manager.getEfficientSamEnv()));
			getSelectedModel().setInstalled(false);
		} else if (getSelectedModel().getName().equals(EfficientViTSAML0.FULL_NAME)
				&& new File(manager.getEfficientViTSAMWeightsPath("l0")).getParentFile().list().length != 1) {
			Files.deleteFolder(new File(manager.getEfficientViTSAMWeightsPath("l0")));
			getSelectedModel().setInstalled(false);
		} else if (getSelectedModel().getName().equals(EfficientViTSAML1.FULL_NAME)
				&& new File(manager.getEfficientViTSAMWeightsPath("l1")).getParentFile().list().length != 1) {
			Files.deleteFolder(new File(manager.getEfficientViTSAMWeightsPath("l1")));
			getSelectedModel().setInstalled(false);
		} else if (getSelectedModel().getName().equals(EfficientViTSAML2.FULL_NAME)
				&& new File(manager.getEfficientViTSAMWeightsPath("l2")).getParentFile().list().length != 1) {
			Files.deleteFolder(new File(manager.getEfficientViTSAMWeightsPath("l2")));
			getSelectedModel().setInstalled(false);
		} else if (getSelectedModel().getName().equals(EfficientViTSAMXL0.FULL_NAME)
				&& new File(manager.getEfficientViTSAMWeightsPath("xl0")).getParentFile().list().length != 1) {
			Files.deleteFolder(new File(manager.getEfficientViTSAMWeightsPath("xl0")));
			getSelectedModel().setInstalled(false);
		} else if (getSelectedModel().getName().equals(EfficientViTSAMXL1.FULL_NAME)
				&& new File(manager.getEfficientViTSAMWeightsPath("xl1")).getParentFile().list().length != 1) {
			Files.deleteFolder(new File(manager.getEfficientViTSAMWeightsPath("xl1")));
			getSelectedModel().setInstalled(false);
		} else if (getSelectedModel().getName().equals(EfficientViTSAML0.FULL_NAME) 
				|| getSelectedModel().getName().equals(EfficientViTSAML1.FULL_NAME)
				|| getSelectedModel().getName().equals(EfficientViTSAML2.FULL_NAME)
				|| getSelectedModel().getName().equals(EfficientViTSAMXL0.FULL_NAME)
				|| getSelectedModel().getName().equals(EfficientViTSAMXL1.FULL_NAME)) {
			Files.deleteFolder(new File(manager.getEfficientViTSamEnv()));
			getSelectedModel().setInstalled(false);
		}
	}
	
	/**
	 * Update the interface accordingly once the installation starts or finishes
	 * @param inProcess
	 * 	whether the installation is happening or it has finished already
	 */
	private void installationInProcess(boolean inProcess) {
		this.bnUninstall.setEnabled(inProcess ? false : getSelectedModel().isInstalled());
		this.bnInstall.setEnabled(inProcess ? false : !getSelectedModel().isInstalled());
		this.rbModels.stream().forEach(btn -> btn.setEnabled(!inProcess));
		this.progressInstallation.setIndeterminate(inProcess);
		if (!inProcess) {
			this.progressInstallation.setValue(this.getSelectedModel().isInstalled() ? 100 : 0);
			this.updateInterface();
		}
	}

    /**
     * Sets the HTML text to be displayed.
     * 
     * @param html
     *        HTML text.
     * @throws NullPointerException
     *         If the HTML text is null.
     */
    public void setHtml(String html) throws NullPointerException
    {
        Objects.requireNonNull(html, "HTML text is null");
        info.setText(formatHTML(html));
        info.setCaretPosition(0);
    }

    /**
     * Sets the HTML text to be displayed and moves the caret until the end of the text
     * 
     * @param html
     *        HTML text.
     * @throws NullPointerException
     *         If the HTML text is null.
     */
    public void setHtmlAndDontMoveCaret(String html) throws NullPointerException {
        Objects.requireNonNull(html, "HTML text is null");
        HTMLDocument doc = (HTMLDocument) info.getDocument();
        HTMLEditorKit editorKit = (HTMLEditorKit) info.getEditorKit();
        try {
            doc.remove(0, doc.getLength());
            editorKit.insertHTML(doc, doc.getLength(), formatHTML(html), 0, 0, null);
        } catch (BadLocationException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return HTML text shown in this component.
     */
    public String getHtml()
    {
        return info.getText();
    }

    /**
     * Add a String to the html pane in the correct format
     * @param html
     * 	the String to be converted into HTML and added to the HTML panel
     */
    public void addHtml(String html) {
        if (html == null) return;
        if (html.trim().isEmpty()) {
        	html = manageEmptyMessage(html);
        } else {
        	waitingIter = 0;
        }
        String nContent = formatHTML(html);
        
        SwingUtilities.invokeLater(() -> {
            try {
                HTMLDocument doc = (HTMLDocument) info.getDocument();
                HTMLEditorKit editorKit = (HTMLEditorKit) info.getEditorKit();
            	editorKit.insertHTML(doc, doc.getLength(), nContent, 0, 0, null);
            	info.setCaretPosition(doc.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Convert the input String into the correct HTML string for the HTML panel
     * @param html
     * 	the input Stirng to be formatted
     * @return the String formatted into the correct HTML string
     */
    private static String formatHTML(String html) {
	    html = html.replace(System.lineSeparator(), "<br>")
	            .replace("    ", "&emsp;")
	            .replace("  ", "&ensp;")
	            .replace(" ", "&nbsp;");
	
	    if (html.startsWith(Mamba.ERR_STREAM_UUUID)) {
	    	html = "<span style=\"color: red;\">" + html.replace(Mamba.ERR_STREAM_UUUID, "") + "</span>";
	    } else {
	    	html = "<span style=\"color: black;\">" + html + "</span>";
	    }
	    return html;
    }
    
    /**
     * Check if a message is empty, thus no information is comming. If the message is not empty, nothing is done.
     * If it is, the html panel is updated so a changing installation in progress message appears
     * @param html
     * 	the message sent by the installation thread
     * @return the message to be print in the html panel
     */
    private String manageEmptyMessage(String html) {
    	if (html.trim().isEmpty() && waitingIter == 0) {
        	html = LocalDateTime.now().format(DATE_FORMAT).toString() + " -- Working, this migh take several minutes .";
        	waitingIter += 1;
        } else if (html.trim().isEmpty() && waitingIter % 3 == 1) {
        	html = LocalDateTime.now().format(DATE_FORMAT).toString() + " -- Working, this migh take several minutes . .";
        	int len = html.length() - (" .").length() + System.lineSeparator().length();
        	SwingUtilities.invokeLater(() -> {
        		HTMLDocument doc = (HTMLDocument) info.getDocument();
        		try {doc.remove(doc.getLength() - len, len);} catch (BadLocationException e) {}
        	});
        	waitingIter += 1;
        } else if (html.trim().isEmpty() && waitingIter % 3 == 2) {
        	html = LocalDateTime.now().format(DATE_FORMAT).toString() + " -- Working, this migh take several minutes . . .";
        	int len = html.length() - (" .").length() + System.lineSeparator().length();
        	SwingUtilities.invokeLater(() -> {
        		HTMLDocument doc = (HTMLDocument) info.getDocument();
        		try {doc.remove(doc.getLength() - len, len);} catch (BadLocationException e) {}
        	});
        	waitingIter += 1;
        } else if (html.trim().isEmpty() && waitingIter % 3 == 0) {
        	html = LocalDateTime.now().format(DATE_FORMAT).toString() + " -- Working, this migh take several minutes .";
        	int len = html.length() + (" . .").length() + System.lineSeparator().length();
        	SwingUtilities.invokeLater(() -> {
        		HTMLDocument doc = (HTMLDocument) info.getDocument();
        		try {doc.remove(doc.getLength() - len, len);} catch (BadLocationException e) {}
        	});
        	waitingIter += 1;
        }
    	return html;
    }
}


