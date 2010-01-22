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
package skrueger.atlas.dp.layer;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.styling.Style;
import org.opengis.coverage.Coverage;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import schmitzm.geotools.grid.GridUtil;
import schmitzm.geotools.io.GeoImportUtil;
import schmitzm.geotools.io.GeoImportUtil.ARCASCII_POSTFIXES;
import schmitzm.geotools.io.GeoImportUtil.GEOTIFF_POSTFIXES;
import schmitzm.geotools.io.GeoImportUtil.IMAGE_POSTFIXES;
import schmitzm.geotools.io.GeoImportUtil.WORLD_POSTFIXES;
import schmitzm.io.IOUtil;
import schmitzm.jfree.chart.style.ChartStyle;
import skrueger.RasterLegendData;
import skrueger.atlas.AVDialogManager;
import skrueger.atlas.AVUtil;
import skrueger.atlas.AtlasConfig;
import skrueger.atlas.dp.DpEntryType;
import skrueger.atlas.gui.internal.AtlasExportTask;
import skrueger.geotools.StyledGridCoverageInterface;
import skrueger.geotools.StyledLayerUtil;
import skrueger.geotools.ZoomRestrictableGridInterface;
import skrueger.geotools.io.GeoImportUtilURL;
import skrueger.i8n.Translation;

/**
 * This class represents any {@link GridCoverage2D} that is read from one file.
 * 
 * @see DpLayerRasterPyramid
 * 
 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons Kr&uuml;ger</a>
 */
public class DpLayerRaster extends DpLayer<GridCoverage2D, ChartStyle>
		implements StyledGridCoverageInterface, ZoomRestrictableGridInterface{
	static final private Logger LOGGER = Logger.getLogger(DpLayerRaster.class);

	/**
	 * caches the {@link GridCoverage2D} Can be un-cached by calling uncache()
	 */
	protected GridCoverage2D gc;

	private RasterLegendData legendMetaData;

	/**
	 * Creates an empty {@link DpLayerRaster}.
	 * 
	 * @param ac
	 *            {@link AtlasConfig}
	 */
	public DpLayerRaster(AtlasConfig ac) {
		super(ac);
		setType(DpEntryType.RASTER);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see skrueger.atlas.datapool.layer.DatapoolLayer#getGT()
	 */
	/**
	 * This method is caching the geotools object, and can be uncached by
	 * calling uncache()
	 */
	@Override
	public GridCoverage2D getGeoObject() {

		try {

			if (gc == null) {

				/**
				 * Can we define transparent colors here?
				 */
				GeneralParameterValue[] readParams = null;

				URL url = getUrl(); 

				final String filename = getFilename().toLowerCase();

				// ****************************************************************************
				// Check if the ending suggests a GeoTIFF
				// ****************************************************************************
				for (GEOTIFF_POSTFIXES ending : GeoImportUtil.GEOTIFF_POSTFIXES
						.values()) {
					if (filename.endsWith(ending.toString())) {
						/**
						 * GEOTiffReader fails for jar:file:....jar!bla.tif URLs
						 * :-(
						 */
						if (url.getProtocol().startsWith("jar")
								&& url.getPath().startsWith("file")) {

							/**
							 * We copy the Tiff to the local temp dir
							 */
							File inTemp = new File(IOUtil.getTempDir(),
									getFilename());
							LOGGER
									.info("Workaround for the GeoTiffReader bug, new source = "
											+ inTemp);

							if (!inTemp.exists()) {
								/**
								 * This is a work-around for GeoTiffReader
								 * problems for jar:// URLs We just all files to
								 * the local tempdir.
								 */
								LOGGER
										.info("The local copy doesn't exist, so we copy "
												+ url + " to " + inTemp);
								FileUtils.copyURLToFile(url, inTemp);

								// Try to copy pending world files...
								for (WORLD_POSTFIXES pf : GeoImportUtil.WORLD_POSTFIXES
										.values()) {
									final URL src = IOUtil.changeUrlExt(url, pf
											.toString());
									LOGGER.debug(src);
									AVUtil.copyURLNoException( src,
											IOUtil.getTempDir(), false);
									// clean = false, because we conly clean
									// filesnames on import
								}

								// Copy optional .prj file to data directory
								AVUtil.copyURLNoException( IOUtil
										.changeUrlExt(url, "prj"), IOUtil
										.getTempDir(), false);

								// Copy optional .sld file to data directory
								AVUtil.copyURLNoException( IOUtil
										.changeUrlExt(url, "sld"), IOUtil
										.getTempDir(), false);

							}

							gc = GeoImportUtilURL.readGridFromGeoTiff(inTemp,
									null, readParams);
						} else {
							gc = GeoImportUtilURL.readGridFromGeoTiff(url,
									null, readParams);
						}

						setType(DpEntryType.RASTER_GEOTIFF);
					}
				}
				// ****************************************************************************
				// Check if the ending suggests a Arc/Info ASCII Grid
				// ****************************************************************************
				for (ARCASCII_POSTFIXES ending : GeoImportUtil.ARCASCII_POSTFIXES
						.values()) {
					if (filename.endsWith(ending.toString())) {
						gc = GeoImportUtil.readGridFromArcInfoASCII(url, null);
						setType(DpEntryType.RASTER_ARCASCII);
					}
				}
				// ****************************************************************************
				// Check if the ending suggests normal image file with worldfile
				// ****************************************************************************
				for (IMAGE_POSTFIXES ending : GeoImportUtil.IMAGE_POSTFIXES
						.values()) {
					if (filename.endsWith(ending.toString())) {
						gc = GeoImportUtilURL.readGridFromImage(url);
						setType(DpEntryType.RASTER_IMAGEWORLD);
					}
				}

				if (gc == null)
					throw (new IllegalArgumentException(
							"File doesn't seem to be a GeoTIFF nor a GIF"));

				// Create an Envelope that contains all information of the
				// raster
				Envelope e = gc.getEnvelope();
				envelope = new com.vividsolutions.jts.geom.Envelope(e
						.getUpperCorner().getOrdinate(0), // X1
						e.getLowerCorner().getOrdinate(0), // X2
						e.getUpperCorner().getOrdinate(1), // Y1dddd
						e.getLowerCorner().getOrdinate(1) // Y2
				);

				crs = gc.getCoordinateReferenceSystem2D();
				//				
				// Object object = gc.getProperties().get("GC_NODATA");
				// gc.getSampleDimension(0).getNoDataValues();
				// System.out.println(object);
			}

			return gc;

		} catch (Exception e) {
			throw new RuntimeException(
					"Exception while accessing the GeoObject", e);
		}
	}

	/**
	 * Returns the cached {@link Style} for this Layer. Tries to load the
	 * {@link Style} from a file with the same URL but the ending
	 * <code>.sld</code>. If it doesn't exist, returns a default RasterStyle.
	 * 
	 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons
	 *         Kr&uuml;ger</a>
	 */
	@Override
	public Style getStyle() {
		if (StyledLayerUtil.isStyleable(this) && super.getStyle() == null) {
			setStyle(GridUtil.createDefaultStyle());
		}
		return super.getStyle();
	}

	/**
	 * Clears all memory-intensive cache objects
	 */
	@Override
	public void uncache() {
		LOGGER.debug("unchaching " + getId() + " aka " + getTitle());
		super.uncache();
		
		/** Close any open attribute table for this layer */
		AVDialogManager.dm_AtlasRasterStyler.disposeInstanceFor(this);

		if (gc != null) {
			gc.dispose(true);
			gc = null;
		}

		envelope = null;
	}

	/**
	 * This method returns the value/{@link Translation} pairs that will be
	 * shown in the Legend
	 */
	public RasterLegendData getLegendMetaData() {
		if (legendMetaData == null) {
			legendMetaData = new RasterLegendData(false);
		}
		return legendMetaData;
	}

	public void dispose() {
		if (isDisposed())
			return;
		disposed = true;
		uncache();
	}

	/**
	 * Calculates the width's resolution in it's
	 * {@link CoordinateReferenceSystem}:
	 * 
	 * @return width in CRS units divided by pixel width
	 */
	public Double getMaxResolution() {
		try {
			double pixelwidth = getGeoObject().getGridGeometry().getGridRange()
					.getHigh().getCoordinateValues()[0];
			double crswidth = getGeoObject().getEnvelope().getUpperCorner()
					.getDirectPosition().getCoordinate()[0]
					- getGeoObject().getEnvelope().getLowerCorner()
							.getDirectPosition().getCoordinate()[0];
//			LOGGER.debug("resolution of " + getTitle().toString() + " = "
//					+ crswidth / pixelwidth);
			return crswidth / pixelwidth;
		} catch (Exception e) {
			LOGGER.error(e);

			return 0.;
		}
	}

	@Override
	public Double getMinResolution() {
		// docme Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see skrueger.atlas.ExportableLayer#exportWithGUI(java.awt.Frame)
	 */
	/**
	 * Exports the raster file and all related files. Only failure on the main
	 * file produces an {@link IOException}
	 */
	public void exportWithGUI(Component owner) throws IOException {

		AtlasExportTask exportTask = new AtlasExportTask(owner, getTitle()
				.toString()) {

			@Override
			protected Boolean doInBackground() throws Exception {

				setPrefix("Exporting ");

				try {
					// waitDialog.setVisible(false);
					exportDir = selectExportDir(owner);
					// waitDialog.setVisible(true);

					if (exportDir == null) {
						// The fodler selection was cancelled.
						return false;
					}

					URL url = getUrl(owner);
					final File file = new File(exportDir, getFilename());

					// ****************************************************************************
					// Copy main file and possibly throw an Exception
					// ****************************************************************************
					publish(file.getAbsolutePath());
					FileUtils.copyURLToFile(getUrl(owner), file);

					// Try to copy pending world files...
					for (WORLD_POSTFIXES pf : GeoImportUtil.WORLD_POSTFIXES
							.values()) {
						final File changeFileExt = IOUtil.changeFileExt(file,
								pf.toString());
						publish(changeFileExt.getAbsolutePath());
						AtlasConfig.exportURLtoFileNoEx(IOUtil.changeUrlExt(
								url, pf.toString()), changeFileExt);
					}

					final File changeFileExt = IOUtil
							.changeFileExt(file, "prj");
					publish(changeFileExt.getAbsolutePath());
					AtlasConfig.exportURLtoFileNoEx(IOUtil.changeUrlExt(url,
							"prj"), changeFileExt);
					AtlasConfig.exportURLtoFileNoEx(IOUtil.changeUrlExt(url,
							"sld"), IOUtil.changeFileExt(file, "sld"));
					publish("done");
					success = true;
				} catch (Exception e) {
					done();
				}
				return success;
			}

		};

		exportTask.execute();

	}

	public void setLegendMetaData(RasterLegendData legendMetaData) {
		this.legendMetaData = legendMetaData;
	}

	/**
	 * Returns the number of bands contained in this {@link Coverage}
	 */
	public int getNumSampleDimensions() {
		return getGeoObject().getNumSampleDimensions();
	}
	
	public DpLayerRaster copy() {
		DpLayerRaster copy = new DpLayerRaster(ac);
		return (DpLayerRaster) copyTo(copy);
	}
	
	@Override
	public DpLayer<GridCoverage2D, ChartStyle> copyTo(
			DpLayer<GridCoverage2D, ChartStyle> target) {
		
		DpLayerRaster copy = (DpLayerRaster) super.copyTo(target);
		
		copy.setLegendMetaData(getLegendMetaData()); // TODO should be copied!
		
		return copy;
	}


}
