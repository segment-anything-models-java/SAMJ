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
/*
 * Copyright 2010-2017 Biomedical Imaging Group at the EPFL.
 * 
 * This file is part of DeconvolutionLab2 (DL2).
 * 
 * DL2 is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * DL2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * DL2. If not, see <http://www.gnu.org/licenses/>.
 */
package ai.nets.samj.gui.components;

import java.awt.Desktop;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

/**
 * This class extends the Java JEditorPane to make a easy to use panel to
 * display HTML information.
 * 
 * @author Daniel Sage, Biomedical Imaging Group, EPFL, Lausanne, Switzerland.
 * 
 */
@SuppressWarnings("serial")
public class HTMLPane extends JEditorPane {

	private String		html		= "";
	private String		header		= "";
	private String		footer		= "";
	private Dimension	dim;
	private String		font		= "verdana";
	private String		color		= "#222222";
	private String		background	= "#f8f8f8";

	/**
	 * Create a default instance of the class
	 */
	public HTMLPane() {
		create();
	}

	/**
	 * Create an instance of the class in the specific font
	 * @param font
	 * 	the font that will be used on the panel
	 */
	public HTMLPane(String font) {
		this.font = font;
		create();
	}

	/**
	 * Create an instance of the class with the specific width and height
	 * 
	 * @param width
	 * 	width of the panel
	 * @param height
	 * 	height of the panel
	 */
	public HTMLPane(int width, int height) {
		this.dim = new Dimension(width, height);
		create();
	}

	/**
	 * Create an instance of the class with an specific font, specific width and height
	 * @param font
	 * 	the font that will be used on the panel
	 * @param width
	 * 	width of the panel
	 * @param height
	 * 	height of the panel
	 */
	public HTMLPane(String font, int width, int height) {
		this.font = font;
		this.dim = new Dimension(width, height);
		create();
	}

	/**
	 * Create an instance of the class with an specific font, specific color of the letters,
	 * specific color of the background of the panel, specific width and height
	 * @param font
	 * 	the font that will be used on the panel
	 * @param color
	 * 	color of the letters
	 * @param background
	 * 	color of the panel background
	 * @param width
	 * 	width of the panel
	 * @param height
	 * 	height of the panel
	 */
	public HTMLPane(String font, String color, String background, int width, int height) {
		this.font = font;
		this.dim = new Dimension(width, height);
		this.color = color;
		this.background = background;
		create();
	}

	@Override
	/**
	 * Retrieve the text written in the panel as a string
	 */
	public String getText() {
		Document doc = this.getDocument();
		try {
			return doc.getText(0, doc.getLength());
		}
		catch (BadLocationException e) {
			e.printStackTrace();
			return getText();
		}
	}
	
	/**
	 * Clear the text of the panel and left nothing written on it.
	 */
	public void clear() {
		html = "";
		append("");
	}

	private void create() {
		header += "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2//EN\">\n";
		header += "<html><head>\n";
		header += "<style>body {background-color:" + background + "; color:" + color + "; font-family: " + font + ";margin:4px}</style>\n";
		header += "<style>h1 {color:#555555; font-size:1.0em; font-weight:bold; padding:1px; margin:1px;}</style>\n";
		header += "<style>h2 {color:#333333; font-size:0.9em; font-weight:bold; padding:1px; margin:1px;}</style>\n";
		header += "<style>h3 {color:#000000; font-size:0.9em; font-weight:italic; padding:1px; margin:1px;}</style>\n";
		header += "<style>p  {color:" + color + "; font-size:0.9em; padding:1px; margin:0px;}</style>\n";
		header += "</head>\n";
		header += "<body>\n";
		footer += "</body></html>\n";
		setEditable(false);
		setContentType("text/html; charset=ISO-8859-1");
		addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } catch (IOException | URISyntaxException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
	}

	/**
	 * Add some text to the end of hte text of the panel
	 * @param content
	 * 	text string to add at the end of the panel
	 */
	public void append(String content) {
		html += content;
		setText(header + html + footer);
		if (dim != null) {
			setPreferredSize(dim);
		}
		setCaretPosition(0);
	}

	/**
	 * Appends specific text String to the end of the panel in the format specified by the html tag
	 *
	 * @param tag
	 *  the HTML tag to use for the content
	 * @param content
	 *  the String content to append
	 */
	public void append(String tag, String content) {
		html += "<" + tag + ">" + content + "</" + tag + ">";
		if (SwingUtilities.isEventDispatchThread()) {
			setText(header + html + footer);
			if (dim != null) {
				setPreferredSize(dim);
			}
			setCaretPosition(0);
			this.validate();
			this.repaint();
		} else {
			SwingUtilities.invokeLater(() -> {
				setText(header + html + footer);
				if (dim != null) {
					setPreferredSize(dim);
				}
				setCaretPosition(0);
				this.validate();
				this.repaint();
			});
		}
	}

	/**
	 * 
	 * @return a {@link JScrollPane} witht he dimensions of the {@link HTMLPane} instance
	 */
	private JScrollPane getPane() {
		JScrollPane scroll = new JScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setPreferredSize(dim);
		return scroll;
	}
}
