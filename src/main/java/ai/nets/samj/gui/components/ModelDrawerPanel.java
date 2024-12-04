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
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.apache.commons.compress.archivers.ArchiveException;

import ai.nets.samj.communication.model.SAMModel;
import ai.nets.samj.gui.HTMLPane;
import io.bioimage.modelrunner.apposed.appose.Mamba;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;

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
    
    private SAMModel model;
    private ModelDrawerPanelListener listener;
    private int hSize;
    private Thread modelInstallThread;
    private Thread infoThread;
    private Thread installedThread;
    private Thread loadingAnimationThread;
    private volatile boolean isLoading = false;
	/**
	 * Parameter used in the HTML panel during installation to know when to update
	 */
    private int waitingIter = 0;
    
    private static final String MODEL_TITLE = "<html><div style='text-align: center; font-size: 15px;'>%s</html>";

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
	
	
	private ModelDrawerPanel(int hSize, ModelDrawerPanelListener listener) {
		this.hSize = hSize;
		this.listener = listener;
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
		html.clear();
	    startLoadingAnimation("Loading info");
		infoThread =new Thread(() -> {
			String description = model.getDescription();
			stopLoadingAnimation();
			SwingUtilities.invokeLater(() -> {
				html.clear();
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
		SwingUtilities.invokeLater(() -> listener.setGUIEnabled(false));
		SwingUtilities.invokeLater(() -> install.setEnabled(false));
		modelInstallThread = new Thread(() ->{
			try {
				this.html.clear();
				this.model.getInstallationManger().setConsumer(str -> addHtml(str));
				this.model.getInstallationManger().installEverything();
				SwingUtilities.invokeLater(() -> {
					this.setInfo();
			    	setButtons();
					listener.setGUIEnabled(true);
				});
			} catch (IOException | InterruptedException | ArchiveException | URISyntaxException
					| MambaInstallException e) {
				e.printStackTrace();
				SwingUtilities.invokeLater(() -> {
					this.setInfo();
			    	setButtons();
					listener.setGUIEnabled(true);
				});
			}
		});
		modelInstallThread.start();
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
                HTMLDocument doc = (HTMLDocument) this.html.getDocument();
                HTMLEditorKit editorKit = (HTMLEditorKit) this.html.getEditorKit();
            	editorKit.insertHTML(doc, doc.getLength(), nContent, 0, 0, null);
            	this.html.setCaretPosition(doc.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Check if a message is empty, thus no information is comming. If the message is not empty, nothing is done.
     * If it is, the html panel is updated so a changing installation in progress message appears
     * @param html
     * 	the message sent by the installation thread
     * @return the message to be print in the html panel
     */
    private String manageEmptyMessage(String html) {
    	String working = "Working, this might take several minutes";
    	if (html.trim().isEmpty() && waitingIter == 0) {
        	html = LocalDateTime.now().format(DATE_FORMAT).toString() + " -- " + working + " .";
        	waitingIter += 1;
        } else if (html.trim().isEmpty() && waitingIter % 3 == 1) {
        	html = LocalDateTime.now().format(DATE_FORMAT).toString() + " -- " + working + " . .";
        	int len = html.length() - (" .").length() + System.lineSeparator().length();
        	SwingUtilities.invokeLater(() -> {
        		HTMLDocument doc = (HTMLDocument) this.html.getDocument();
        		try {doc.remove(doc.getLength() - len, len);} catch (BadLocationException e) {}
        	});
        	waitingIter += 1;
        } else if (html.trim().isEmpty() && waitingIter % 3 == 2) {
        	html = LocalDateTime.now().format(DATE_FORMAT).toString() + " -- " + working + " . . .";
        	int len = html.length() - (" .").length() + System.lineSeparator().length();
        	SwingUtilities.invokeLater(() -> {
        		HTMLDocument doc = (HTMLDocument) this.html.getDocument();
        		try {doc.remove(doc.getLength() - len, len);} catch (BadLocationException e) {}
        	});
        	waitingIter += 1;
        } else if (html.trim().isEmpty() && waitingIter % 3 == 0) {
        	html = LocalDateTime.now().format(DATE_FORMAT).toString() + " -- " + working + " .";
        	int len = html.length() + (" . .").length() + System.lineSeparator().length();
        	SwingUtilities.invokeLater(() -> {
        		HTMLDocument doc = (HTMLDocument) this.html.getDocument();
        		try {doc.remove(doc.getLength() - len, len);} catch (BadLocationException e) {}
        	});
        	waitingIter += 1;
        }
    	return html;
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
	
	private void uninstallModel() {
		SwingUtilities.invokeLater(() -> listener.setGUIEnabled(false));
		SwingUtilities.invokeLater(() -> uninstall.setEnabled(false));
	    startLoadingAnimation("Uninstalling model");
		modelInstallThread = new Thread(() ->{
			this.model.getInstallationManger().uninstall();
			stopLoadingAnimation();
			SwingUtilities.invokeLater(() -> {
				this.setInfo();
		    	setButtons();
				listener.setGUIEnabled(true);
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
	                html.clear();
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

}
