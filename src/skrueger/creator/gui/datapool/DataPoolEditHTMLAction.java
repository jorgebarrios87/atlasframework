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
package skrueger.creator.gui.datapool;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;

import schmitzm.jfree.chart.style.ChartStyle;
import skrueger.atlas.dp.DataPool;
import skrueger.atlas.dp.DpEntry;
import skrueger.atlas.dp.layer.DpLayer;
import skrueger.creator.AtlasConfigEditable;
import skrueger.creator.AtlasCreator;
import skrueger.creator.gui.SimplyHTMLUtil;
import skrueger.i8n.I8NUtil;

public class DataPoolEditHTMLAction extends AbstractAction {

	static final Logger LOGGER = Logger.getLogger(DataPoolDeleteAction.class);

	private final DataPoolJTable dpTable;

	public DataPoolEditHTMLAction(DataPoolJTable dpTable) {
		super(AtlasCreator.R("DataPoolWindow_Action_EditDPEHTML_label"));

		this.dpTable = dpTable;
	}

	@Override
	/*
	 * Opens a dialog to edit the HTML for this layer
	 */
	public void actionPerformed(ActionEvent e) {

		// Determine which DPEntry is selected
		if (dpTable.getSelectedRow() == -1)
			return;
		DataPool dataPool = dpTable.getDataPool();
		DpEntry dpe = dataPool.get(dpTable.convertRowIndexToModel(dpTable
				.getSelectedRow()));

		if (!(dpe instanceof DpLayer))
			return;

		DpLayer<?, ChartStyle> dpl = (DpLayer<?, ChartStyle>) dpe;
		java.util.List<File> infoFiles = dpTable.getAce().getHTMLFilesFor(dpl);

		java.util.List<String> tabTitles = new ArrayList<String>();
		AtlasConfigEditable ace = dpTable.getAce();
		for (String l : ace.getLanguages()) {
			tabTitles.add(AtlasCreator.R("DPLayer.HTMLInfo.LanguageTabTitle",
					I8NUtil.getLocaleFor(l).getDisplayLanguage()));
		}

		SimplyHTMLUtil.openHTMLEditors(dpTable, ace, infoFiles, tabTitles,
				AtlasCreator.R("EditLayerHTML.Dialog.Title", dpl.getTitle()
						.toString()));

		/**
		 * Try to reset a few cached values for the TODO nicer! // Expect that
		 * the number of HTML files, and with it the QM have changed. // TODO
		 * replace this events?! Should uncache fire the events?
		 */
		dpl.uncache();
		((AbstractTableModel) dpTable.getModel()).fireTableDataChanged();
		dpTable.setTableCellRenderers();

	}

}