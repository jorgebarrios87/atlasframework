/*******************************************************************************
 * Copyright (c) 2010 Stefan A. Tzeggai.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan A. Tzeggai - initial API and implementation
 ******************************************************************************/
package org.geopublishing.geopublisher.gui.map;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.geopublishing.atlasViewer.map.Map;
import org.geopublishing.atlasViewer.map.MapPool;
import org.geopublishing.atlasViewer.map.MapRef;
import org.geopublishing.geopublisher.AtlasConfigEditable;
import org.geopublishing.geopublisher.gui.internal.GPDialogManager;
import org.geopublishing.geopublisher.swing.GeopublisherGUI;
import org.geopublishing.geopublisher.swing.GpSwingUtil;

import de.schmitzm.swing.SmallButton;
import de.schmitzm.swing.event.FilterTableKeyListener;

/**
 * This {@link JPanel} allows editing
 * 
 * @author Stefan Alfons Tzeggai
 * 
 */
public class EditMapPoolPanel extends JPanel {
	final static private Logger LOGGER = Logger
			.getLogger(EditMapPoolPanel.class);

	private DraggableMapPoolJTable mapPoolJTable;

	private final MapPool mapPool;

	private final AtlasConfigEditable ace;

	private JTextField filterTextField;

	public JTextField getFilterTextField() {
		if (filterTextField == null) {
			filterTextField = new JTextField();
			filterTextField.setToolTipText(GpSwingUtil
					.R("DataPoolWindow.FilterTable.TT"));
		}
		return filterTextField;
	}

	/**
	 * Constructs a {@link EditMapPoolPanel} with the features to delete, edit
	 * an dcreate new {@link Map}s,
	 * 
	 * @param draggable
	 *            Shall the {@link Map}s be allowed to dragged by Drag'n'Drop
	 *            (as {@link MapRef}s)?
	 */
	public EditMapPoolPanel(AtlasConfigEditable atlasConfig_) {
		super(new MigLayout("wrap 1", "[grow]", "[shrink][grow][shrink]"));
		this.mapPool = atlasConfig_.getMapPool();
		this.ace = atlasConfig_;

		// A row to enter a filter:
		JLabel filterLabel = new JLabel("Filter:");
		add(filterLabel, "split 2, top");
		filterLabel.setToolTipText(GpSwingUtil
				.R("DataPoolWindow.FilterTable.TT"));
		add(getFilterTextField(), "growx, top");

		// The constructor adds itself to the textfield
		new FilterTableKeyListener(getMapPoolJTable(), getFilterTextField(), 1, 2, 3);

		add(new JScrollPane(getMapPoolJTable()), "grow 2000");
		add(getBottomPanel(), "shrinky, growx 1000");

		// Add a listener for mouse click
		// Double-click on mapPoolJList opens its the DesignMapView to edit.
		getMapPoolJTable().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() == 2
						&& !SwingUtilities.isRightMouseButton(evt)) {
					MapPoolJTable mpTable = (MapPoolJTable) evt.getSource();

					final Map map = mapPool.get(mpTable
							.convertRowIndexToModel(mpTable.rowAtPoint(evt
									.getPoint())));

					GPDialogManager.dm_MapComposer.getInstanceFor(map,
							EditMapPoolPanel.this, map);
				}
			}
		});
	}

	private JPanel getBottomPanel() {
		JPanel bottom = new JPanel(new MigLayout("gap 0, inset 0", "[grow]"));

		bottom.add(
				new JLabel(GeopublisherGUI.R("EditMappoolPanel.Explanation")),
				"growx 200");

		JButton addButton = new SmallButton(new MapPoolAddAction(
				getMapPoolJTable()),
				GeopublisherGUI.R("MapPoolWindow.Button_AddMap_tt"));

		bottom.add(addButton, "top, align right");

		return bottom;
	}

	/**
	 * @return The {@link MapPoolJTable} representing the {@link MapPool} in
	 *         this {@link JPanel}
	 */
	public DraggableMapPoolJTable getMapPoolJTable() {
		if (mapPoolJTable == null) {
			mapPoolJTable = new DraggableMapPoolJTable(ace);
		}
		return mapPoolJTable;
	}

}
