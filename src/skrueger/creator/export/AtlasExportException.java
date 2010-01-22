/*******************************************************************************
 * Copyright (c) 2009 Stefan A. Krüger.
 * 
 * This file is part of the Geopublisher application - An authoring tool to facilitate the publication and distribution of geoproducts in form of online and/or offline end-user GIS.
 * http://www.geopublishing.org
 * 
 * Geopublisher is part of the Geopublishing Framework hosted at:
 * http://wald.intevation.org/projects/atlas-framework/
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License (license.txt)
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 * or try this link: http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Stefan A. Krüger - initial API and implementation
 ******************************************************************************/
package skrueger.creator.export;

import org.apache.log4j.Logger;

import skrueger.atlas.exceptions.AtlasException;

public class AtlasExportException extends AtlasException {
	public AtlasExportException() {
		super("Exception while exporting the atlas."); // i8n
	}

	public AtlasExportException(String message, Throwable cause) {
		super(message, cause);
	}

	public AtlasExportException(String message) {
		super(message);
	}

	public AtlasExportException(Throwable cause) {
		super(cause);
	}

	final static private Logger LOGGER = Logger
			.getLogger(AtlasExportException.class);
}
