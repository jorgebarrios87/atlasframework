package skrueger.creator.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

import schmitzm.swing.ExceptionDialog;
import skrueger.atlas.dp.DpEntry;
import skrueger.creator.AtlasConfigEditable;
import skrueger.creator.AtlasCreator;

public class GPExportCSVAction extends AbstractAction {

	private final AtlasConfigEditable ace;
	private final Component owner;

	public GPExportCSVAction(String label, AtlasConfigEditable ace,
			Component owner) {
		super(label);
		this.ace = ace;
		this.owner = owner;
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		File exportFile = askUserForFolder();

		if (exportFile == null)
			return;

		FileWriter fw;
		try {
			fw = new FileWriter(exportFile);

			fw.write("Name;Beschreibung;Dateiname\n");

			for (DpEntry dpe : ace.getDataPool().values()) {
				fw.write(dpe.getTitle() + ";" + dpe.getDesc() + ";"
						+ dpe.getFilename() + "\n");
			}

			fw.flush();
			fw.close();

		} catch (IOException e1) {
			ExceptionDialog.show(owner, e1);
		}
	}

	private File askUserForFolder() {
		File exportFile;
		/**
		 * Ask the user to select a save position 
		 * TODO Remember this position in the .properties
		 */

		File startWithDir = new File(System.getProperty("user.home"),
				"datapool.csv");
		JFileChooser dc = new JFileChooser(startWithDir);
		dc.setDialogType(JFileChooser.SAVE_DIALOG);
		dc.setDialogTitle(AtlasCreator.R("ExportCSV.SaveCSVDialog.Title"));
		dc.setSelectedFile(startWithDir);

		if ((dc.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION)
				|| (dc.getSelectedFile() == null))
			return null;

		exportFile = dc.getSelectedFile();
		exportFile.delete();
		return exportFile;
	}

}
