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
package skrueger.creator;

import java.io.File;
import java.io.IOException;

import javax.swing.SwingWorker;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import rachel.http.loader.WebResourceManager;
import skrueger.atlas.dp.AMLImport;
import skrueger.atlas.exceptions.AtlasException;
import skrueger.atlas.gui.internal.AtlasStatusDialog;
import skrueger.atlas.http.FileWebResourceLoader;
import skrueger.atlas.http.Webserver;

public class AMLImportEd extends AMLImport {
	final static private Logger LOGGER = Logger.getLogger(AMLImportEd.class);

	/**
	 * Creates a {@link AtlasConfigEditable} from AtlasML XML code <br>
	 * The sequence of the elements is strictly defined by the XSD <br>
	 * Also sets up the {@link Webserver} to serve from the filesystem:
	 * [atlasDir/ad/html]
	 * 
	 * <br>
	 * This should only be called by the AtlasCreator!
	 * 
	 * @param atlasDir
	 *            The atlas directory always has a folder ad and ad/atlas.xml
	 *            must exists!
	 * @param aceLoader
	 *            A {@link SwingWorker} who's publish method is called with some
	 *            info. May be null.
	 */
	public final static AtlasConfigEditable parseAtlasConfig(AtlasStatusDialog statusDialog, File atlasDir) throws AtlasException, SAXException, IOException,
			ParserConfigurationException {
		LOGGER.info("Opening Atlas from Folder " + atlasDir + "...");

		// Create virgin AtlasConfigEditable
		AtlasConfigEditable ace = new AtlasConfigEditable();

		ace.setAtlasDir(atlasDir);

		parseAtlasConfig(statusDialog,  ace, true);

		// The AtlasConfig was loaded from a folder in the file system.
		// Adding the folder as a WebResource for the internal WebServer
		LOGGER.debug("Adding folder " + ace.getAtlasDir()
				+ " as a WebServer resource");

		WebResourceManager.addResourceLoader(new FileWebResourceLoader(ace
				.getAtlasDir()));
		return ace;
	}

	/**
	 * Determines if this is an atlas dir by looking if a ./ad/atlas.xml file
	 * exists
	 * 
	 * @param atlasDir
	 *            A {@link File} folder to check
	 * @return true if this looks like an atlasDir
	 */
	public static boolean isAtlasDir(File atlasDir) {
		if (!new File(atlasDir, "ad" + File.separator + "atlas.xml").exists())
			return false;
		return true;
	}

}
