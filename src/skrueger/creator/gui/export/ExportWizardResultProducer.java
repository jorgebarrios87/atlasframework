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
package skrueger.creator.gui.export;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.netbeans.spi.wizard.DeferredWizardResult;
import org.netbeans.spi.wizard.ResultProgressHandle;
import org.netbeans.spi.wizard.Summary;
import org.netbeans.spi.wizard.WizardException;
import org.netbeans.spi.wizard.WizardPage.WizardResultProducer;

import schmitzm.swing.ExceptionDialog;
import skrueger.atlas.AVUtil;
import skrueger.atlas.AtlasCancelException;
import skrueger.atlas.AVUtil.OSfamiliy;
import skrueger.creator.AtlasConfigEditable;
import skrueger.creator.AtlasCreator;
import skrueger.creator.GPProps;
import skrueger.creator.GPProps.Keys;
import skrueger.creator.export.JarExportUtil;

/**
 * This class is using the values collected during the {@link ExportWizard} to
 * export the {@link AtlasConfigEditable}.
 * 
 * 
 * 
 * @author Stefan A. Krüger
 */
public class ExportWizardResultProducer implements WizardResultProducer {

	private static final Logger LOGGER = Logger
			.getLogger(ExportWizardResultProducer.class);

	@Override
	public boolean cancel(Map settings) {
		return true;
	}

	@Override
	public Object finish(Map wizardData) throws WizardException {

		final AtlasConfigEditable ace = (AtlasConfigEditable) wizardData
				.get(ExportWizard.ACE);
		if (!ace.save(AtlasCreator.getInstance().getJFrame(), false))
			return null; // TODO what should be return here?

		final Boolean isJws = (Boolean) wizardData
				.get(ExportWizard.JWS_CHECKBOX);
		final Boolean isDisk = (Boolean) wizardData
				.get(ExportWizard.DISK_CHECKBOX);
		final String exportDir = (String) wizardData
				.get(ExportWizard.EXPORTFOLDER);
		final Boolean copyJRE = (Boolean) wizardData.get(ExportWizard.COPYJRE);

		/**
		 * Store stuff to the geopublisher.properties
		 */
		{
			if (isJws) {
				GPProps.set(GPProps.Keys.jnlpURL, (String) wizardData
						.get(ExportWizard.JNLPURL));
			}

			GPProps.set(Keys.LastExportFolder, exportDir);

			GPProps.set(Keys.LastExportDisk, isDisk);
			GPProps.set(Keys.LastExportJWS, isJws);

			GPProps.store();
		}

		/**
		 * Start the export as a DeferredWizardResult
		 */
		DeferredWizardResult result = new DeferredWizardResult(true) {

			private JarExportUtil jarExportUtil;
			private ResultProgressHandle progress;

			/**
			 * If the user aborts the export, we tell it to JarExportUtil
			 * instance
			 */
			@Override
			public void abort() {
				jarExportUtil.abort();
				progress.finished(getAbortSummary());
			};

			@Override
			public void start(Map wizardData, ResultProgressHandle progress) {
				this.progress = progress;

				jarExportUtil = new JarExportUtil(ace, new File(exportDir),
						isDisk, isJws, copyJRE);
				try {
					jarExportUtil.export(progress);
				} catch (AtlasCancelException e) {
					LOGGER.info("Export aborted by user:", e);
					progress.finished(getAbortSummary());
					return;
				} catch (Exception e) {
					LOGGER.error("Export failed!", e);
					progress.failed(e.getMessage(), false);
					progress.finished(getErrorPanel(e));
					ExceptionDialog.show(null, e);
					return;
				}

				/*
				 * Only gets here if the Export was successful
				 */
				Summary summary = Summary.create(getSummaryJPanel(), new File(
						exportDir));

				progress.finished(summary);

			}

			private JPanel getErrorPanel(Exception e) {
				JPanel panel = new JPanel(new MigLayout("wrap 1"));

				panel.add(new JTextArea(e.getLocalizedMessage()));

				return panel;
			}

			/**
			 * Generates the last WizardPage
			 */
			private JPanel getSummaryJPanel() {
				JPanel panel = new JPanel(new MigLayout("wrap 1"));

				String exportJWSandDISKdirRepresentation = exportDir;
				if (AVUtil.getOSType() == OSfamiliy.windows) {
					// Otherwise all Windows paths are missing the slashes
					exportJWSandDISKdirRepresentation = exportJWSandDISKdirRepresentation
							.replace("\\", "\\\\");
				}

				panel.add(new JLabel(AtlasCreator.R(
						"Export.Dialog.Finished.Msg",
						exportJWSandDISKdirRepresentation)));

				final JButton openFolderButton = new JButton(AtlasCreator
						.R("ExportWizard.Result.OpenFolderButton.Label"));
				openFolderButton.addActionListener(new ActionListener() {

					@Override
					public void actionPerformed(ActionEvent e) {
						AVUtil.openOSFolder(new File(exportDir));
						openFolderButton.setEnabled(false);

						// TODO Here it would be nice to close the Wizard... but
						// how??
					}

				});

				panel.add(openFolderButton, "align center");

				return panel;
			}
		};

		return result;
	}

	protected Summary getAbortSummary() {
		JPanel aborted = new JPanel(new MigLayout());
		aborted
				.add(new JLabel(
						"The export has been aborted by the user. The temporary folder have been deleted."));

		return Summary.create(aborted, "abort");
	}
}