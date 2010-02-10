/*******************************************************************************
 * Copyright (c) 2010 Stefan A. Krüger (soon changing to Stefan A. Tzeggai).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan A. Krüger (soon changing to Stefan A. Tzeggai) - initial API and implementation
 ******************************************************************************/
package skrueger.creator.gui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JSplitPane;

import org.apache.log4j.Logger;

import schmitzm.swing.SwingUtil;
import skrueger.atlas.dp.DataPool;
import skrueger.creator.AtlasConfigEditable;
import skrueger.creator.AtlasCreator;
import skrueger.creator.GPProps;
import skrueger.creator.gui.datapool.DataPoolJTable;
import skrueger.creator.gui.datapool.DraggableDatapoolJTable;
import skrueger.creator.gui.datapool.EditDataPoolPanel;
import skrueger.creator.gui.group.EditGroupsDnDJTreePanel;
import skrueger.creator.gui.map.EditMapPoolPanel;
import skrueger.creator.gui.map.MapPoolJTable;

/**
 * A {@link JSplitPane} that represents the {@link AtlasConfigEditable}. It is
 * the main panel of Geopublisher. If automatically sets values of its size and
 * state from {@link GPProps} properties.
 * 
 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons Kr&uuml;ger</a>
 */
public class GpJSplitPane extends JSplitPane {
	private Logger LOGGER = Logger.getLogger(GpJSplitPane.class);

	/** This {@link GpJSplitPane} visualizes this {@link AtlasConfigEditable} */
	private AtlasConfigEditable ace;

	private EditDataPoolPanel editDatapoolPanel;

	private JSplitPane rightSide;

	private EditGroupsDnDJTreePanel editGroups;

	private EditMapPoolPanel editMapPoolPanel;

	/**
	 * Creates a {@link GpJSplitPane} which is the main interface to edit an
	 * Atlas
	 * 
	 * @param ace
	 *            {@link AtlasConfigEditable} this is working on
	 */
	public GpJSplitPane(AtlasConfigEditable ace) {
		super(JSplitPane.HORIZONTAL_SPLIT);
		this.ace = ace;

		if (ace == null)
			add(new JLabel(AtlasCreator.R("NoAtlasPanelText")));
		else
			initialize();

	}

	/**
	 * This method initializes this AtlasInternalFrame GUI Should be called
	 * after the supported languages have changed, because the menu is language
	 * sensitive.
	 * 
	 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons
	 *         Kr&uuml;ger</a>
	 */
	private void initialize() {
		setOneTouchExpandable(true);

		// ****************************************************************************
		// The left side contains the datapool,
		// ****************************************************************************
		editDatapoolPanel = new EditDataPoolPanel(ace);
		editDatapoolPanel.setBorder(BorderFactory
				.createTitledBorder(AtlasCreator
						.R("DataPoolJTable.Border.Title")));
		setLeftComponent(editDatapoolPanel);

		// ****************************************************************************
		// right side contains the map-pool and the groups
		// ****************************************************************************
		rightSide = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		rightSide.setOneTouchExpandable(true);

		editMapPoolPanel = new EditMapPoolPanel(ace);
		editMapPoolPanel.setBorder(BorderFactory
				.createTitledBorder(AtlasCreator
						.R("MapPoolJTable.Border.Title")));
		rightSide.setTopComponent(editMapPoolPanel);

		editGroups = new EditGroupsDnDJTreePanel(ace.getFirstGroup());
		editGroups.setBorder(BorderFactory.createTitledBorder(AtlasCreator
				.R("EditGroupsDnDJTreePanel.Border.Title")));
		rightSide.setBottomComponent(editGroups);

		setRightComponent(rightSide);

		SwingUtil.setMinimumWidth(editDatapoolPanel, 300);
		SwingUtil.setPreferredWidth(editDatapoolPanel, 400);
		SwingUtil.setMinimumWidth(rightSide, 100);
		SwingUtil.setPreferredWidth(rightSide, 300);

		setLeftDividerLocation(GPProps.getInt(
				GPProps.Keys.gpWindowLeftDividerLocation, 400));
		setRightDividerLocation(GPProps.getInt(
				GPProps.Keys.gpWindowRightDividerLocation, 350));
	}

	public int getLeftDividerLocation() {
		return getDividerLocation();
	}

	public void setLeftDividerLocation(int location) {
		setDividerLocation(location);
	}

	public int getRightDividerLocation() {
		return rightSide.getDividerLocation();
	}

	public void setRightDividerLocation(int location) {
		rightSide.setDividerLocation(location);
	}

	/**
	 * @return and caches the {@link DraggableDatapoolJTable} that represents
	 *         the {@link DataPool}
	 */
	public DataPoolJTable getDatapoolJTable() {
		return editDatapoolPanel.getDatapoolJTable();
	}

	/**
	 * @return and caches the {@link DraggableDatapoolJTable} that represents
	 *         the {@link DataPool}
	 */
	public MapPoolJTable getMappoolJTable() {
		return editMapPoolPanel.getMapPoolJTable();
	}

	public void dispose() {
		if (editDatapoolPanel != null && editDatapoolPanel.getDatapoolJTable() != null) {
			editDatapoolPanel.getDatapoolJTable().dispose();
		}
		if (editMapPoolPanel != null && editMapPoolPanel.getMapPoolJTable() != null) {
			editMapPoolPanel.getMapPoolJTable().dispose();
		}
	}

}
