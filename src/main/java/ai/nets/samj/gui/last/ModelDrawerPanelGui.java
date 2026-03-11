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
package ai.nets.samj.gui.last;


import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 
 * @author Carlos Javier Garcia Lopez de Haro
 */
public class ModelDrawerPanelGui extends JPanel {

    
	private static final long serialVersionUID = -5258672339166051523L;
	private JLabel drawerTitle = new JLabel();
	protected JButton install;
	protected JButton uninstall;
	protected HTMLPane html = new HTMLPane("Segoe UI", "#333333", "#FFFFFF");
	HTMLPaneScroll htmlView = new HTMLPaneScroll(html);
	
    private static final String MODEL_TITLE = "<html><div style='text-align: center; font-size: 15px;'>%s</html>";
	
    private static final double TITLE_HRATIO = 0.15;

    protected static final String INSTALL_STRING = "Install";
    protected static final String UNINSTALL_STRING = "Uninstall";
    protected static final String STOP_STRING = "Stop";
	
	protected ModelDrawerPanelGui() {
		setLayout(null);
		setBorder(BorderFactory.createLineBorder(Color.black));
		install = new JButton(INSTALL_STRING);
		uninstall = new JButton(UNINSTALL_STRING);
        drawerTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        drawerTitle.setForeground(new Color(50, 50, 50)); 
        drawerTitle.setText(String.format(MODEL_TITLE, "&nbsp;"));
        drawerTitle.setHorizontalAlignment(JLabel.CENTER);
        
        add(drawerTitle);
        add(htmlView);
        add(install);
        add(uninstall);
	}
	
	@Override
	public void doLayout() {
	    super.doLayout();

	    final int pad = 2;   // outer padding + vertical spacing
	    final int gap = 2;   // gap between the two buttons

	    int x0 = 0;
	    int y0 = 0;
	    int w  = Math.max(0, getWidth());
	    int h  = Math.max(0, getHeight());

	    // Title: 15% height, full width
	    int titleH = Math.max(0, (int) Math.round(h * TITLE_HRATIO));
	    drawerTitle.setBounds(x0, y0, w, titleH);

	    // Bottom buttons row: height based on preferred height (plus padding)
	    int prefBtnH = Math.max(install.getPreferredSize().height, uninstall.getPreferredSize().height);
	    int btnRowH = Math.min(h - titleH, prefBtnH + 2 * pad); // clamp to available space

	    int btnRowY = y0 + h - btnRowH;

	    // Buttons: equal widths, with padding and a 2px gap
	    int innerX = x0 + pad;
	    int innerW = Math.max(0, w - 2 * pad);
	    int btnH   = Math.max(0, btnRowH - 2 * pad);

	    int btnW = Math.max(0, (innerW - gap) / 2);

	    install.setBounds(innerX, btnRowY + pad, btnW, btnH);
	    uninstall.setBounds(innerX + btnW + gap, btnRowY + pad, btnW, btnH);

	    // HTML: fills space between title and buttons row
	    int htmlX = x0 + pad;
	    int htmlY = y0 + titleH + pad;
	    int htmlW = Math.max(0, w - 2 * pad);
	    int htmlH = Math.max(0, (btnRowY - pad) - htmlY);

	    htmlView.setBounds(htmlX, htmlY, htmlW, htmlH);
	}
    
    protected void setTitle(String title) {
        drawerTitle.setText(String.format(MODEL_TITLE, title));
    }
}
