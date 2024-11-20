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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
/**
 * The GridPanel class is a custom JPanel that uses the GridBagLayout to arrange components in a grid.
 * It provides several constructors to customize the panel's border and spacing, as well as methods to place
 * components in specific cells of the grid.
 *
 * @author Daniel Sage
 */
public class GridPanel extends JPanel {

	private static final long	serialVersionUID	= 1L;
	private GridBagLayout		layout				= new GridBagLayout();
	private GridBagConstraints	constraint			= new GridBagConstraints();
	private int					defaultSpace		= 3;

	/**
	 * Creates a new GridPanel with default spacing and a border.
	 */
	public GridPanel() {
		super();
		setLayout(layout);
		setBorder(BorderFactory.createEtchedBorder());
	}

	/**
	 * Creates a new GridPanel with the specified default spacing and a border.
	 *
	 * @param defaultSpace
	 *  the default spacing between components
	 */
	public GridPanel(int defaultSpace) {
		super();
		setLayout(layout);
		this.defaultSpace = defaultSpace;
		setBorder(BorderFactory.createEtchedBorder());
	}

	/**
	 * Creates a new GridPanel with default spacing and the specified border.
	 *
	 * @param border
	 *  true if the panel should have a border, false otherwise
	 */
	public GridPanel(boolean border) {
		super();
		setLayout(layout);
		if (border) {
			setBorder(BorderFactory.createEtchedBorder());
		}
	}

	/**
	 * Creates a new GridPanel with default spacing and a titled border.
	 *
	 * @param title
	 *  the title of the border
	 */
	public GridPanel(String title) {
		super();
		setLayout(layout);
		setBorder(BorderFactory.createTitledBorder(title));
	}

	/**
	 * Creates a new GridPanel with the specified default spacing and a border.
	 *
	 * @param border
	 *  true if the panel should have a border, false otherwise
	 * @param defaultSpace
	 *  the default spacing between components
	 */
	public GridPanel(boolean border, int defaultSpace) {
		super();
		setLayout(layout);
		this.defaultSpace = defaultSpace;
		if (border) {
			setBorder(BorderFactory.createEtchedBorder());
		}
	}

	/**
	 * Creates a new GridPanel with the specified default spacing and a titled border.
	 *
	 * @param title
	 *  the title of the border
	 * @param defaultSpace
	 *  the default spacing between components
	 */
	public GridPanel(String title, int defaultSpace) {
		super();
		setLayout(layout);
		this.defaultSpace = defaultSpace;
		setBorder(BorderFactory.createTitledBorder(title));
	}

	/**
	 * Sets the default spacing between components in the GridPanel.
	 *
	 * @param defaultSpace
	 *  the default spacing between components
	 */
	public void setSpace(int defaultSpace) {
		this.defaultSpace = defaultSpace;
	}

	/**
	 * Adds a label to the GridPanel at the specified row and column with default spacing.
	 *
	 * @param row
	 *  the row index of the label
	 * @param col
	 *  the column index of the label
	 * @param label
	 *  the text of the label
	 */
	public void place(int row, int col, String label) {
		place(row, col, 1, 1, defaultSpace, new JLabel(label));
	}

	/**
	 * Adds a label to the GridPanel at the specified row and column with the specified spacing.
	 *
	 * @param row
	 *  the row index of the label
	 * @param col
	 *  the column index of the label
	 * @param space
	 *  the spacing between components
	 * @param label
	 *  the text of the label
	 */
	public void place(int row, int col, int space, String label) {
		place(row, col, 1, 1, space, new JLabel(label));
	}

	/**
	 * Adds a label to the GridPanel at the specified row and column with default spacing and the specified size.
	 *
	 * @param row
	 *  the row index of the label
	 * @param col
	 *  the column index of the label
	 * @param width
	 *  the width of the label in number of columns
	 * @param height
	 *  the height of the label in number of rows
	 * @param label
	 *  the text of the label
	 */
	public void place(int row, int col, int width, int height, String label) {
		place(row, col, width, height, defaultSpace, new JLabel(label));
	}

	/**
	 * Adds a component to the GridPanel at the specified row and column with default spacing.
	 *
	 * @param row
	 *  the row index of the component
	 * @param col
	 *  the column index of the component
	 * @param comp
	 *  the component to add
	 */
	public void place(int row, int col, JComponent comp) {
		place(row, col, 1, 1, defaultSpace, comp);
	}

	/**
	 * Adds a component to the GridPanel at the specified row and column with default spacing.
	 *
	 * @param row
	 *  the row index of the component
	 * @param col
	 *  the column index of the component
	 * @param space
	 *  the spacing between components
	 * @param comp
	 *  the component to add
	 */
	public void place(int row, int col, int space, JComponent comp) {
		place(row, col, 1, 1, space, comp);
	}

	/**
	 * Adds a component to the GridPanel at the specified row and column with the specified spacing.
	 *
	 * @param row
	 *  the row index of the component
	 * @param col
	 *  the column index of the component
	 * @param width
	 *  the width of the component in number of columns
	 * @param height
	 *  the height of the component in number of rows
	 * @param comp
	 *  the component to add
	 */
	public void place(int row, int col, int width, int height, JComponent comp) {
		place(row, col, width, height, defaultSpace, comp);
	}

	/**
	 * Adds a component to the GridPanel at the specified row and column with default spacing and the specified size.
	 *
	 * @param row
	 *  the row index of the component
	 * @param col
	 *  the column index of the component
	 * @param width
	 *  the width of the component in number of columns
	 * @param height
	 *  the height of the component in number of rows
	 * @param space
	 *  the spacing between components
	 * @param comp 
	 * the component to add
	 */
	public void place(int row, int col, int width, int height, int space, JComponent comp) {
		if (comp == null)
			return;
		constraint.gridx = col;
		constraint.gridy = row;
		constraint.gridwidth = width;
		constraint.gridheight = height;
		constraint.anchor = GridBagConstraints.NORTHWEST;
		constraint.insets = new Insets(space, space, space, space);
		constraint.fill = GridBagConstraints.HORIZONTAL;
		layout.setConstraints(comp, constraint);
		add(comp);
	}

	/**
	 * Adds a component to the GridPanel at the specified row and column with default spacing and the specified size.
	 *
	 * @param row
	 *  the row index of the component
	 * @param col
	 *  the column index of the component
	 * @param width
	 *  the width of the component in number of columns
	 * @param height
	 *  the height of the component in number of rows
	 * @param spaceHorizontal
	 *  the horizontal spacing between components
	 * @param spaceVertical
	 *  the vertical spacing between components
	 * @param comp 
	 * the component to add
	 */
	public void place(int row, int col, int width, int height, int spaceHorizontal, int spaceVertical, JComponent comp) {
		if (comp == null)
			return;
		constraint.gridx = col;
		constraint.gridy = row;
		constraint.gridwidth = width;
		constraint.gridheight = height;
		constraint.anchor = GridBagConstraints.NORTHWEST;
		constraint.insets = new Insets(spaceVertical, spaceHorizontal, spaceHorizontal, spaceVertical);
		constraint.fill = GridBagConstraints.HORIZONTAL;
		layout.setConstraints(comp, constraint);
		add(comp);
	}
}
