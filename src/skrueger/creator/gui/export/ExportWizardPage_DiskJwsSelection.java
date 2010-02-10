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

import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.netbeans.spi.wizard.WizardPage;

import skrueger.creator.AtlasCreator;
import skrueger.creator.GPProps;
import skrueger.creator.GPProps.Keys;

public class ExportWizardPage_DiskJwsSelection extends WizardPage {
	/*
	 * The short description label that appears on the left side of the wizard
	 */
	static final String desc = AtlasCreator.R("ExportWizard.JwsOrDisk");

	private static final String validationJwsOrDiskFailedMsg = AtlasCreator
			.R("ExportWizard.JwsOrDisk.ValidationError");

	JLabel explanationJLabel = new JLabel(AtlasCreator
			.R("ExportWizard.JwsOrDisk.Explanation"));
	JLabel explanationJwsJLabel = new JLabel(AtlasCreator
			.R("ExportWizard.JwsOrDisk.Explanation.Jws"));
	JLabel explanationDiskJLabel = new JLabel(AtlasCreator
			.R("ExportWizard.JwsOrDisk.Explanation.Disk"));
	JCheckBox diskJCheckbox;
	JCheckBox jwsJCheckbox;

	public static String getDescription() {
		return desc;
	}

	public ExportWizardPage_DiskJwsSelection() {
		initGui();
	}

	@Override
	protected String validateContents(final Component component,
			final Object event) {
		if (!getJwsJCheckbox().isSelected() && !getDiskJCheckbox().isSelected())
			return validationJwsOrDiskFailedMsg;
		return null;
	}

	private void initGui() {
		setSize(ExportWizard.DEFAULT_WPANEL_SIZE);
		setPreferredSize(ExportWizard.DEFAULT_WPANEL_SIZE);

		setLayout(new MigLayout("wrap 1"));
		add(explanationJLabel);
		add(getDiskJCheckbox(), "gapy unrelated");
		add(explanationDiskJLabel);
		add(getJwsJCheckbox(), "gapy unrelated");
		add(explanationJwsJLabel);

	}

	private JCheckBox getJwsJCheckbox() {
		if (jwsJCheckbox == null) {
			jwsJCheckbox = new JCheckBox(AtlasCreator
					.R("ExportWizard.JwsOrDisk.JwsCheckbox"));
			jwsJCheckbox.setName(ExportWizard.JWS_CHECKBOX);
			jwsJCheckbox.setSelected(GPProps.getBoolean(Keys.LastExportJWS));

		}
		return jwsJCheckbox;
	}

	private JCheckBox getDiskJCheckbox() {
		if (diskJCheckbox == null) {
			diskJCheckbox = new JCheckBox(AtlasCreator
					.R("ExportWizard.JwsOrDisk.DiskCheckbox"));
			diskJCheckbox.setName(ExportWizard.DISK_CHECKBOX);
			diskJCheckbox.setSelected(GPProps.getBoolean(Keys.LastExportDisk));
		}
		return diskJCheckbox;
	}

}
