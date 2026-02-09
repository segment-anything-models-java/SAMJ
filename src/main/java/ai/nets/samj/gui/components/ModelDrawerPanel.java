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
package ai.nets.samj.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import ai.nets.samj.communication.model.SAMModel;
import ai.nets.samj.gui.HTMLPane;

import org.apposed.appose.BuildException;
import org.apposed.appose.Builder.ProgressConsumer;

/**
 * TODO improve the way the installation is logged
 * TODO improve the way the installation is logged
 * TODO improve the way the installation is logged
 * TODO improve the way the installation is logged
 * TODO improve the way the installation is logged
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 */
public class ModelDrawerPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 6708853280844731445L;
    
	private JLabel drawerTitle = new JLabel();
    private JButton install = new JButton("Install");
    private JButton uninstall = new JButton("Uninstall");
    HTMLPane html = new HTMLPane("Segoe UI", "#333333", "#FFFFFF", 200, 200);
    private HtmlLogger logger = new HtmlLogger(html);
    
    private SAMModel model;
    private final List<ModelDrawerPanelListener> listeners;
    private int hSize;
    private Thread modelInstallThread;
    private Thread infoThread;
    private Thread installedThread;
    private Thread loadingAnimationThread;
    private volatile boolean isLoading = false;
    private static final String MODEL_TITLE = "<html><div style='text-align: center; font-size: 15px;'>%s</html>";
	
	
	private ModelDrawerPanel(int hSize, ModelDrawerPanelListener listener) {
		this.hSize = hSize;
		this.listeners = new ArrayList<>(5);
		this.listeners.add(listener);
		createDrawerPanel();
		this.install.addActionListener(this);
		this.uninstall.addActionListener(this);
	}
	
	public static ModelDrawerPanel create(int hSize, ModelDrawerPanelListener listener) {
		return new ModelDrawerPanel(hSize, listener);
	}

    private void createDrawerPanel() {
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEtchedBorder());
        drawerTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        drawerTitle.setForeground(new Color(50, 50, 50)); 
        drawerTitle.setText(String.format(MODEL_TITLE, "&nbsp;"));
        drawerTitle.setHorizontalAlignment(JLabel.CENTER); // Center the title
        //drawerTitle.setFont(new Font("Arial", Font.BOLD, 18)); // Set a modern font and size
        drawerTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); // Add padding
        this.add(drawerTitle, BorderLayout.NORTH);
        this.add(createInstallModelComponent(), BorderLayout.SOUTH);
        html.append("Model description");
        html.append("Model description");
        html.append("Model description");
        html.append("");
        html.append("i", "Other information");
        html.append("i", "References");
        this.add(html.getPane(), BorderLayout.CENTER);
        this.setPreferredSize(new Dimension(hSize, 0)); // Set preferred width
    }


    // Method to create the install model component
    private JPanel createInstallModelComponent() {
        JPanel thirdComponent = new JPanel();
        thirdComponent.setLayout(new GridBagLayout());
        thirdComponent.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));
        thirdComponent.setBorder(new LineBorder(Color.BLACK));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;

        // Install button
        gbc.gridx = 0;
        thirdComponent.add(this.install, gbc);

        // Uninstall button
        gbc.gridx = 1;
        thirdComponent.add(this.uninstall, gbc);

        return thirdComponent;
    }
    
    public void setSelectedModel(SAMModel model) {
    	this.model = model;
    	setTitle(model.getName());
    	setInfo();
    	setButtons();
    }
    
    private void setButtons() {
        install.setEnabled(false);
        uninstall.setEnabled(false);
        installedThread = new Thread(() -> {
        	boolean installed = model.isInstalled();
        	SwingUtilities.invokeLater(() -> {
        		if (installed) this.uninstall.setEnabled(true);
        		else this.install.setEnabled(true);
        	});
        });
        installedThread.start();
    }
    
    private void setTitle(String title) {
        drawerTitle.setText(String.format(MODEL_TITLE, title));
    }
    
    private void setInfo() {
		logger.clear();
	    startLoadingAnimation("Loading info");
		infoThread =new Thread(() -> {
			String description = model.getDescription();
			stopLoadingAnimation();
			SwingUtilities.invokeLater(() -> {
				logger.clear();
				html.append("p", description);
			});
		});
		infoThread.start();
    }
    
    @Override
    public void setVisible(boolean visible) {
    	if (!visible)
    		interruptThreads();
    	super.setVisible(visible);
    }

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == this.install) {
			installModel();
		} else if (e.getSource() == this.uninstall) {
			uninstallModel();
		}
	}
	
	private void installModel() {
		SwingUtilities.invokeLater(() -> listeners.forEach(l -> l.setGUIEnabled(false)));
		SwingUtilities.invokeLater(() -> install.setEnabled(false));
		if (this.model.getInstallationManger() == null) {
			SwingUtilities.invokeLater(() -> {
				this.setInfo();
		    	setButtons();
				listeners.forEach(l -> l.setGUIEnabled(true));
			});
		}
		modelInstallThread = new Thread(() ->{
			try {
				this.logger.clear();
				ProgressConsumer progress = (title, current, maximum) -> {
				    double pct = (maximum <= 0) ? 0.0 : (100.0 * current / maximum);
				    logger.log("Downloading Pixi: " + pct + "%", Color.blue);
				};
				this.model.getInstallationManger().setOutputConsumer(str -> logger.log(str, Color.black));
				this.model.getInstallationManger().setErrorConsumer(str -> logger.log(str, Color.green));
				this.model.getInstallationManger().setProgressConsumer(progress);
				this.model.getInstallationManger().installEverything();
				SwingUtilities.invokeLater(() -> {
					this.setInfo();
			    	setButtons();
					listeners.forEach(l -> l.setGUIEnabled(true));
				});
			} catch (IOException | InterruptedException | BuildException e) {
				e.printStackTrace();
				SwingUtilities.invokeLater(() -> {
					this.setInfo();
			    	setButtons();
					listeners.forEach(l -> l.setGUIEnabled(true));
				});
			}
		});
		modelInstallThread.start();
	}
	
	private void uninstallModel() {
		SwingUtilities.invokeLater(() -> listeners.forEach(l -> l.setGUIEnabled(false)));
		SwingUtilities.invokeLater(() -> uninstall.setEnabled(false));
	    startLoadingAnimation("Uninstalling model");
		modelInstallThread = new Thread(() ->{
			this.model.getInstallationManger().uninstall();
			stopLoadingAnimation();
			SwingUtilities.invokeLater(() -> {
				this.setInfo();
		    	setButtons();
				listeners.forEach(l -> l.setGUIEnabled(true));
			});
		});
		modelInstallThread.start();
	}
	
	private void startLoadingAnimation(String message) {
	    stopLoadingAnimation(); // Ensure any previous animation is stopped
	    isLoading = true;
	    loadingAnimationThread = new Thread(() -> {
	        int dotCount = 0;
	        String[] dots = {".", "..", "...", ""};
	        while (isLoading) {
	            String currentTime = java.time.LocalTime.now()
	                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
	            String displayMessage = String.format("%s -- %s%s", currentTime, message, dots[dotCount % dots.length]);
	            dotCount++;
	            SwingUtilities.invokeLater(() -> {
	                logger.clear();
	                html.append("p", displayMessage);
	            });
	            try {
	                Thread.sleep(300); // Update every half second
	            } catch (InterruptedException e) {
	                // Thread was interrupted
	                break;
	            }
	        }
	    });
	    loadingAnimationThread.start();
	}

	private void stopLoadingAnimation() {
	    isLoading = false;
	    if (loadingAnimationThread != null && loadingAnimationThread.isAlive()) {
	        loadingAnimationThread.interrupt();
	        try {
	        	loadingAnimationThread.join();
	        } catch (InterruptedException e) {}
	    }
	}

	
	public void interruptThreads() {
		if (infoThread != null)
			this.infoThread.interrupt();
		if (installedThread != null)
			this.installedThread.interrupt();
		if (modelInstallThread != null)
			this.modelInstallThread.interrupt();
	}
	
	public interface ModelDrawerPanelListener {
		
	    void setGUIEnabled(boolean enabled);
	}

	public void addModelDrawerPanelListener(ModelDrawerPanelListener listener) {
		 this.listeners.add(listener);
	}
	public void removeModelDrawerPanelListener(ModelDrawerPanelListener listener) {
		this.listeners.remove(listener);
	}
}
