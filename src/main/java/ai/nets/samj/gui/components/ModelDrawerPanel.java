package ai.nets.samj.gui.components;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import org.apache.commons.compress.archivers.ArchiveException;

import ai.nets.samj.communication.model.SAMModel;
import ai.nets.samj.gui.HTMLPane;
import io.bioimage.modelrunner.apposed.appose.MambaInstallException;

public class ModelDrawerPanel extends JPanel implements ActionListener {

    private JLabel drawerTitle = new JLabel();
    private JButton install = new JButton("Install");
    private JButton uninstall = new JButton("Uninstall");
    
    private SAMModel model;
    private ModelDrawerPanelListener listener;
    private int hSize;
    private Thread modelInstallThread;
    
    private static final String MODEL_TITLE = "<html><div style='text-align: center; font-size: 15px;'>%s</html>";
	
	
	private ModelDrawerPanel(int hSize, ModelDrawerPanelListener listener) {
		this.hSize = hSize;
		this.listener = listener;
		createDrawerPanel();
	}
	
	public static ModelDrawerPanel create(int hSize, ModelDrawerPanelListener listener) {
		return new ModelDrawerPanel(hSize, listener);
	}

    private void createDrawerPanel() {
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        drawerTitle.setText(String.format(MODEL_TITLE, "&nbsp;"));
        this.add(drawerTitle, BorderLayout.NORTH);
        this.add(createInstallModelComponent(), BorderLayout.SOUTH);
        HTMLPane html = new HTMLPane("Arial", "#000", "#CCCCCC", 200, 200);
        html.append("Model description");
        html.append("Model description");
        html.append("Model description");
        html.append("");
        html.append("i", "Other information");
        html.append("i", "References");
        this.add(html, BorderLayout.CENTER);
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
    }
    
    private void setTitle(String title) {
        drawerTitle.setText(String.format(MODEL_TITLE, title));
    }
    
    private void setInfo() {
    	// TODO
    }
    
    @Override
    public void setVisible(boolean visible) {
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
		modelInstallThread = new Thread(() ->{
			try {
				this.model.getInstallationManger().installEverything();
				SwingUtilities.invokeLater(() -> {
					listener.setGUIEnabled(true);
				});
			} catch (IOException | InterruptedException | ArchiveException | URISyntaxException
					| MambaInstallException e) {
				e.printStackTrace();
				SwingUtilities.invokeLater(() -> {
					listener.setGUIEnabled(true);
				});
			}
		});
		
	}
	
	private void uninstallModel() {
		SwingUtilities.invokeLater(() -> listener.setGUIEnabled(false));
		modelInstallThread = new Thread(() ->{
			this.model.getInstallationManger().uninstall();
			SwingUtilities.invokeLater(() -> {
				listener.setGUIEnabled(true);
			});
		});
	}
	
	public interface ModelDrawerPanelListener {
		
	    void setGUIEnabled(boolean enabled);
	}

}
