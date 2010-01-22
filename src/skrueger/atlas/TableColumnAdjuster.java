/*******************************************************************************
 * Copyright (c) 2009 Stefan A. Krüger.
 * 
 * This file is part of the AtlasViewer application - A GIS viewer application targeting at end-users with no GIS-experience. Its main purpose is to present the atlases created with the Geopublisher application.
 * http://www.geopublishing.org
 * 
 * AtlasViewer is part of the Geopublishing Framework hosted at:
 * http://wald.intevation.org/projects/atlas-framework/
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License (license.txt)
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * or try this link: http://www.gnu.org/licenses/lgpl.html
 * 
 * Contributors:
 *     Stefan A. Krüger - initial API and implementation
 ******************************************************************************/
package skrueger.atlas;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/*
 *	Class to manage the widths of colunmns in a table.
 *
 *  Various properties control how the width of the column is calculated.
 *  Another property controls whether column width calculation should be dynamic.
 *  Finally, various Actions will be added to the table to allow the user
 *  to customize the functionality.
 *
 *  This class was designed to be used with tables that use an auto resize mode
 *  of AUTO_RESIZE_OFF. With all other modes you are constrained as the width
 *  of the columns must fit inside the table. So if you increase one column, one
 *  or more of the other columns must decrease. Because of this the resize mode
 *  of RESIZE_ALL_COLUMNS will work the best.
 */
public class TableColumnAdjuster implements PropertyChangeListener,
		TableModelListener {
	private JTable table;
	private int spacing;
	private boolean isColumnHeaderIncluded;
	private boolean isColumnDataIncluded;
	private boolean isOnlyAdjustLarger;
	private boolean isDynamicAdjustment;
	private Map<TableColumn, Integer> columnSizes = new HashMap<TableColumn, Integer>();

	// Returns the preferred height of a row.
	// The result is equal to the tallest cell in the row.
	public int getPreferredRowHeight(JTable table, int rowIndex, int margin) {
		// Get the current default height for all rows
		int height = table.getRowHeight();

		// Determine highest cell in the row
		for (int c = 0; c < table.getColumnCount(); c++) {
			TableCellRenderer renderer = table.getCellRenderer(rowIndex, c);
			Component comp = table.prepareRenderer(renderer, rowIndex, c);
			int h = comp.getPreferredSize().height + 2 * margin;
			height = Math.max(height, h);
		}
		return height;
	}

	// The height of each row is set to the preferred height of the
	// tallest cell in that row.
	public void packRows(JTable table, int margin) {
		packRows(table, 0, table.getRowCount(), margin);
	}

	// For each row >= start and < end, the height of a
	// row is set to the preferred height of the tallest cell
	// in that row.
	public void packRows(JTable table, int start, int end, int margin) {
		for (int r = 0; r < table.getRowCount(); r++) {
			// Get the preferred height
			int h = getPreferredRowHeight(table, r, margin);

			// Now set the row height using the preferred height
			if (table.getRowHeight(r) != h) {
				table.setRowHeight(r, h);
			}
		}
	}

	/*
	 * Specify the table and use default spacing
	 */
	public TableColumnAdjuster(JTable table) {
		this(table, 6);
	}

	/*
	 * Specify the table and spacing
	 */
	public TableColumnAdjuster(JTable table, int spacing) {
		this.table = table;
		this.spacing = spacing;
		setColumnHeaderIncluded(false);
		setColumnDataIncluded(true);
		setOnlyAdjustLarger(true);
		setDynamicAdjustment(false);
		// installActions();
	}

	/*
	 * Adjust the widths of all the columns in the table
	 */
	public void adjustColumns() {

		TableColumnModel tcm = table.getColumnModel();

		for (int i = 0; i < tcm.getColumnCount(); i++) {
			adjustColumn(i);
		}

		// packRows(table, 2);
	}

	/*
	 * Adjust the width of the specified column in the table
	 */
	public void adjustColumn(final int column) {
		TableColumn tableColumn = table.getColumnModel().getColumn(column);

		if (!tableColumn.getResizable())
			return;

		int columnHeaderWidth = getColumnHeaderWidth(column);
		int columnDataWidth = getColumnDataWidth(column);
		int preferredWidth = Math.max(columnHeaderWidth, columnDataWidth);

		updateTableColumn(column, preferredWidth);
	}

	/*
	 * Calculated the width based on the column name
	 */
	private int getColumnHeaderWidth(int column) {
		if (!isColumnHeaderIncluded)
			return 0;

		TableColumn tableColumn = table.getColumnModel().getColumn(column);
		Object value = tableColumn.getHeaderValue();
		TableCellRenderer renderer = tableColumn.getHeaderRenderer();

		if (renderer == null) {
			renderer = table.getTableHeader().getDefaultRenderer();
		}

		Component c = renderer.getTableCellRendererComponent(table, value,
				false, false, -1, column);
		return c.getPreferredSize().width;
	}

	/*
	 * Calculate the width based on the widest cell renderer for the given
	 * column.
	 */
	private int getColumnDataWidth(int column) {
		if (!isColumnDataIncluded)
			return 0;

		int preferredWidth = 0;
		int maxWidth = table.getColumnModel().getColumn(column).getMaxWidth();

		for (int row = 0; row < table.getRowCount(); row++) {
			preferredWidth = Math.max(preferredWidth, getCellDataWidth(row,
					column));

			// We've exceeded the maximum width, no need to check other rows

			if (preferredWidth >= maxWidth)
				break;
		}

		return preferredWidth;
	}

	/*
	 * Get the preferred width for the specified cell
	 */
	private int getCellDataWidth(int row, int column) {
		// Inovke the renderer for the cell to calculate the preferred width

		TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
		Object value = table.getValueAt(row, column);
		Component c = cellRenderer.getTableCellRendererComponent(table, value,
				false, false, row, column);
		int width = c.getPreferredSize().width
				+ table.getIntercellSpacing().width;

		return width;
	}

	/*
	 * Update the TableColumn with the newly calculated width
	 */
	private void updateTableColumn(int column, int width) {
		final TableColumn tableColumn = table.getColumnModel()
				.getColumn(column);

		if (!tableColumn.getResizable())
			return;

		width += spacing;

		// Don't shrink the column width

		if (isOnlyAdjustLarger) {
			width = Math.max(width, tableColumn.getPreferredWidth());
		}

		columnSizes.put(tableColumn, new Integer(tableColumn.getWidth()));
		table.getTableHeader().setResizingColumn(tableColumn);
		tableColumn.setWidth(width);
	}

	/*
	 * Restore the widths of the columns in the table to its previous width
	 */
	public void restoreColumns() {
		TableColumnModel tcm = table.getColumnModel();

		for (int i = 0; i < tcm.getColumnCount(); i++) {
			restoreColumn(i);
		}
	}

	/*
	 * Restore the width of the specified column to its previous width
	 */
	private void restoreColumn(int column) {
		TableColumn tableColumn = table.getColumnModel().getColumn(column);
		Integer width = columnSizes.get(tableColumn);

		if (width != null) {
			table.getTableHeader().setResizingColumn(tableColumn);
			tableColumn.setWidth(width.intValue());
		}
	}

	/*
	 * Indicates whether to include the header in the width calculation
	 */
	public void setColumnHeaderIncluded(boolean isColumnHeaderIncluded) {
		this.isColumnHeaderIncluded = isColumnHeaderIncluded;
	}

	/*
	 * Indicates whether to include the model data in the width calculation
	 */
	public void setColumnDataIncluded(boolean isColumnDataIncluded) {
		this.isColumnDataIncluded = isColumnDataIncluded;
	}

	/*
	 * Indicates whether columns can only be increased in size
	 */
	public void setOnlyAdjustLarger(boolean isOnlyAdjustLarger) {
		this.isOnlyAdjustLarger = isOnlyAdjustLarger;
	}

	/*
	 * Indicate whether changes to the model should cause the width to be
	 * dynamically recalculated.
	 */
	public void setDynamicAdjustment(boolean isDynamicAdjustment) {
		// May need to add or remove the TableModelListener when changed

		if (this.isDynamicAdjustment != isDynamicAdjustment) {
			if (isDynamicAdjustment) {
				table.addPropertyChangeListener(this);
				table.getModel().addTableModelListener(this);
			} else {
				table.removePropertyChangeListener(this);
				table.getModel().removeTableModelListener(this);
			}
		}

		this.isDynamicAdjustment = isDynamicAdjustment;
	}

	//
	// Implement the PropertyChangeListener
	//
	public void propertyChange(PropertyChangeEvent e) {
		// When the TableModel changes we need to update the listeners
		// and column widths

		if ("model".equals(e.getPropertyName())) {
			TableModel model = (TableModel) e.getOldValue();
			model.removeTableModelListener(this);

			model = (TableModel) e.getNewValue();
			model.addTableModelListener(this);
			adjustColumns();
		}
	}

	//
	// Implement the TableModelListener
	//
	public void tableChanged(TableModelEvent e) {
		// A cell has been updated

		if (e.getType() == TableModelEvent.UPDATE) {
			int column = table.convertColumnIndexToView(e.getColumn());

			// Only need to worry about an increase in width for this cell

			if (isOnlyAdjustLarger) {
				int row = e.getFirstRow();
				TableColumn tableColumn = table.getColumnModel().getColumn(
						column);

				if (tableColumn.getResizable()) {
					int width = getCellDataWidth(row, column);
					updateTableColumn(column, width);
				}
			}

			// Could be an increase of decrease so check all rows

			else {
				adjustColumn(column);
			}
		}

		// The update affected more than one column so adjust all columns

		else {
			adjustColumns();
		}
	}

	/*
	 * Install Actions to give user control of certain functionality.
	 */
	private void installActions() {
		installColumnAction(true, true, "adjustColumn", "control ADD");
		installColumnAction(false, true, "adjustColumns", "control shift ADD");
		installColumnAction(true, false, "restoreColumn", "control SUBTRACT");
		installColumnAction(false, false, "restoreColumns",
				"control shift SUBTRACT");

		installToggleAction(true, false, "toggleDynamic", "control MULTIPLY");
		installToggleAction(false, true, "toggleLarger", "control DIVIDE");
	}

	/*
	 * Update the input and action maps with a new ColumnAction
	 */
	private void installColumnAction(boolean isSelectedColumn,
			boolean isAdjust, String key, String keyStroke) {
		Action action = new ColumnAction(isSelectedColumn, isAdjust);
		KeyStroke ks = KeyStroke.getKeyStroke(keyStroke);
		table.getInputMap().put(ks, key);
		table.getActionMap().put(key, action);
	}

	/*
	 * Update the input and action maps with new ToggleAction
	 */
	private void installToggleAction(boolean isToggleDynamic,
			boolean isToggleLarger, String key, String keyStroke) {
		Action action = new ToggleAction(isToggleDynamic, isToggleLarger);
		KeyStroke ks = KeyStroke.getKeyStroke(keyStroke);
		table.getInputMap().put(ks, key);
		table.getActionMap().put(key, action);
	}

	/*
	 * Action to adjust or restore the width of a single column or all columns
	 */
	class ColumnAction extends AbstractAction {
		private boolean isSelectedColumn;
		private boolean isAdjust;

		public ColumnAction(boolean isSelectedColumn, boolean isAdjust) {
			this.isSelectedColumn = isSelectedColumn;
			this.isAdjust = isAdjust;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// Handle selected column(s) width change actions

			if (isSelectedColumn) {
				int[] columns = table.getSelectedColumns();

				for (int i = 0; i < columns.length; i++) {
					if (isAdjust)
						adjustColumn(columns[i]);
					else
						restoreColumn(columns[i]);
				}
			} else {
				if (isAdjust)
					adjustColumns();
				else
					restoreColumns();
			}
		}
	}

	/*
	 * Toggle properties of the TableColumnAdjuster so the user can customize
	 * the functionality to their preferences
	 */
	class ToggleAction extends AbstractAction {
		private boolean isToggleDynamic;
		private boolean isToggleLarger;

		public ToggleAction(boolean isToggleDynamic, boolean isToggleLarger) {
			this.isToggleDynamic = isToggleDynamic;
			this.isToggleLarger = isToggleLarger;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (isToggleDynamic) {
				setDynamicAdjustment(!isDynamicAdjustment);
				return;
			}

			if (isToggleLarger) {
				setOnlyAdjustLarger(!isOnlyAdjustLarger);
				return;
			}
		}
	}
}
