/*******************************************************************************
 * Copyright (c) 2010 Stefan A. Tzeggai.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan A. Tzeggai - initial API and implementation
 ******************************************************************************/
package org.geopublishing.atlasViewer.dp.layer;

import java.awt.Component;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.geopublishing.atlasViewer.AtlasConfig;
import org.geopublishing.atlasViewer.dp.DpEntryType;
import org.geopublishing.atlasViewer.swing.AVDialogManager;
import org.geopublishing.atlasViewer.swing.AVSwingUtil;
import org.geopublishing.atlasViewer.swing.internal.AtlasExportTask;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.data.DataUtilities;
import org.geotools.gce.arcgrid.ArcGridReader;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.image.WorldImageReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.parameter.Parameter;
import org.geotools.styling.Style;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;

import de.schmitzm.geotools.ZoomRestrictableGridInterface;
import de.schmitzm.geotools.data.rld.RasterLegendData;
import de.schmitzm.geotools.grid.GridUtil;
import de.schmitzm.geotools.io.GeoImportUtil;
import de.schmitzm.geotools.io.GeoImportUtil.ARCASCII_POSTFIXES;
import de.schmitzm.geotools.io.GeoImportUtil.GEOTIFF_POSTFIXES;
import de.schmitzm.geotools.io.GeoImportUtil.IMAGE_POSTFIXES;
import de.schmitzm.geotools.io.GeoImportUtil.WORLD_POSTFIXES;
import de.schmitzm.geotools.styling.StyledGridCoverageReaderInterface;
import de.schmitzm.geotools.styling.StyledLayerUtil;
import de.schmitzm.i18n.Translation;
import de.schmitzm.io.IOUtil;
import de.schmitzm.jfree.chart.style.ChartStyle;

/**
 * This class represents any {@link GridCoverage2D} that is read from one file.
 * 
 * @see DpLayerRasterPyramid
 * 
 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons Tzeggai</a>
 */
public class DpLayerRaster_Reader extends
	DpLayerRaster<AbstractGridCoverage2DReader, ChartStyle> implements
	StyledGridCoverageReaderInterface, ZoomRestrictableGridInterface {

    static final private Logger LOGGER = Logger
	    .getLogger(DpLayerRaster_Reader.class);
    /**
     * 
     * caches the {@link GridCoverage2D} Can be un-cached by calling uncache()
     */
    protected AbstractGridCoverage2DReader gc;

    private RasterLegendData legendMetaData;

    /**
     * 0 wenn noch nicht bestimmt.
     */
    private int bandCount = 0;

    /**
     * Creates an empty {@link DpLayerRaster_Reader}.
     * 
     * @param ac
     *            {@link AtlasConfig}
     */
    public DpLayerRaster_Reader(AtlasConfig ac) {
	super(ac);
	setType(DpEntryType.RASTER);
    }

    /**
     * Exports the raster file and all related files. Only failure on the main
     * file produces an {@link IOException}
     */
    @Override
    public void exportWithGUI(Component owner) throws IOException {

	AtlasExportTask exportTask = new AtlasExportTask(owner, getTitle()
		.toString()) {

	    @Override
	    protected Boolean doInBackground() throws Exception {

		setPrefix("Exporting ");

		try {
		    // waitDialog.setVisible(false);
		    exportDir = AVSwingUtil.selectExportDir(owner,
			    getAtlasConfig());
		    // waitDialog.setVisible(true);

		    if (exportDir == null) {
			// The fodler selection was cancelled.
			return false;
		    }

		    URL url = AVSwingUtil.getUrl(DpLayerRaster_Reader.this,
			    owner);
		    final File file = new File(exportDir, getFilename());

		    // ****************************************************************************
		    // Copy main file and possibly throw an Exception
		    // ****************************************************************************
		    publish(file.getAbsolutePath());
		    FileUtils.copyURLToFile(AVSwingUtil.getUrl(
			    DpLayerRaster_Reader.this, owner), file);

		    // Try to copy pending world files...
		    for (WORLD_POSTFIXES pf : GeoImportUtil.WORLD_POSTFIXES
			    .values()) {
			final File changeFileExt = IOUtil.changeFileExt(file,
				pf.toString());
			publish(changeFileExt.getAbsolutePath());
			AtlasConfig.exportURLtoFileNoEx(
				IOUtil.changeUrlExt(url, pf.toString()),
				changeFileExt);
		    }

		    final File changeFileExt = IOUtil
			    .changeFileExt(file, "prj");
		    publish(changeFileExt.getAbsolutePath());
		    AtlasConfig.exportURLtoFileNoEx(
			    IOUtil.changeUrlExt(url, "prj"), changeFileExt);
		    AtlasConfig.exportURLtoFileNoEx(
			    IOUtil.changeUrlExt(url, "sld"),
			    IOUtil.changeFileExt(file, "sld"));
		    // publish("done");
		    success = true;
		} catch (Exception e) {
		    done();
		}
		return success;
	    }

	};
	exportTask.execute();
    }

    /**
     * This method is caching the geotools object, and can be uncached by
     * calling uncache()
     */
    @Override
    public AbstractGridCoverage2DReader getGeoObject() {

	try {

	    if (gc == null) {

		/**
		 * Can we define transparent colors here?
		 */
		GeneralParameterValue[] readParams = null;

		URL url;
		url = getUrl();

		/**
		 * GEOTiffReader fails for jar:file:....jar!bla.tif URLs :-(
		 */
		if (url.getProtocol().startsWith("jar")
		// && url.getPath().startsWith("file")
		) {
		    // We copy the Tiff to the local temp dir
		    File inTemp = new File(IOUtil.getTempDir(), getFilename());
		    LOGGER.debug("Workaround for the GeoTiffReader bug, new source = "
			    + inTemp);
		    if (!inTemp.exists()) {
			/**
			 * This is a work-around for GeoTiffReader problems for
			 * jar:// URLs We just all files to the local tempdir.
			 */
			LOGGER.debug("Local copy does not exist, so we copy "
				+ url + " to " + inTemp);
			FileUtils.copyURLToFile(url, inTemp);

			// Try to copy pending world files...
			for (WORLD_POSTFIXES pf : GeoImportUtil.WORLD_POSTFIXES
				.values()) {
			    final URL src = IOUtil.changeUrlExt(url,
				    pf.toString());

			    // clean = false, because we only clean
			    // filenames on import
			    IOUtil.copyURLNoException(src, IOUtil.getTempDir(),
				    false);
			}

			// Copy optional .prj file to data directory
			IOUtil.copyURLNoException(
				IOUtil.changeUrlExt(url, "prj"),
				IOUtil.getTempDir(), false);

			// Copy optional .prj file to data directory
			IOUtil.copyURLNoException(
				IOUtil.changeUrlExt(url, "tfw"),
				IOUtil.getTempDir(), false);

			// Copy optional .sld file to data directory
			IOUtil.copyURLNoException(
				IOUtil.changeUrlExt(url, "sld"),
				IOUtil.getTempDir(), false);

		    }

		    LOGGER.info("Changed the URL from " + url + " to " + inTemp);
		    url = DataUtilities.fileToURL(inTemp);
		}

		final String filename = getFilename().toLowerCase();

		// ****************************************************************************
		// Check if the ending suggests a GeoTIFF
		// ****************************************************************************
		for (GEOTIFF_POSTFIXES ending : GeoImportUtil.GEOTIFF_POSTFIXES
			.values()) {
		    if (filename.endsWith(ending.toString())) {

			gc = new GeoTiffReader(url);
			setType(DpEntryType.RASTER_GEOTIFF);
		    }
		}

		// ****************************************************************************
		// Check if the ending suggests a Arc/Info ASCII Grid
		// ****************************************************************************
		for (ARCASCII_POSTFIXES ending : GeoImportUtil.ARCASCII_POSTFIXES
			.values()) {
		    if (filename.endsWith(ending.toString())) {
			gc = new ArcGridReader(url);
			setType(DpEntryType.RASTER_ARCASCII);
		    }
		}
		// ****************************************************************************
		// Check if the ending suggests normal image file with worldfile
		// ****************************************************************************
		for (IMAGE_POSTFIXES ending : GeoImportUtil.IMAGE_POSTFIXES
			.values()) {
		    if (filename.endsWith(ending.toString())) {
			gc = new WorldImageReader(url);
			setType(DpEntryType.RASTER_IMAGEWORLD);
		    }
		}

		if (gc == null)
		    throw (new IllegalArgumentException(
			    "File doesn't seem to be a GeoTIFF nor a GIF"));

		// Create an Envelope that contains all information of the
		// raster
		// MS-01.sc
		// Envelope e = gc.getEnvelope();
		// envelope = new com.vividsolutions.jts.geom.Envelope(e
		// .getUpperCorner().getOrdinate(0), // X1
		// e.getLowerCorner().getOrdinate(0), // X2
		// e.getUpperCorner().getOrdinate(1), // Y1dddd
		// e.getLowerCorner().getOrdinate(1) // Y2
		// );

		// GridEnvelope e = gc.getOriginalGridRange();
		// envelope = new com.vividsolutions.jts.geom.Envelope(
		// e.getHigh(0), // X1
		// e.getLow(0), // X2
		// e.getHigh(1), // Y1
		// e.getLow(1) // Y2
		//
		// ServiceInfo info = gc.getInfo();
		//
		// Object source = gc.getSource();
		//
		// LOGGER.debug(info);
		// LOGGER.debug(source);
		// envelope = new com.vividsolutions.jts.geom.Envelope(
		// e.getHigh(0), // X1
		// e.getLow(0), // X2
		// e.getHigh(1), // Y1
		// e.getLow(1) // Y2
		// );

		double[] lower = gc.getOriginalEnvelope().getLowerCorner()
			.getCoordinate();
		double[] upper = gc.getOriginalEnvelope().getUpperCorner()
			.getCoordinate();
		envelope = new Envelope(lower[0], lower[1], upper[0], upper[1]);

		// LOGGER.info("Evaluated the following Enveloper for GeoTiffReader "
		// + envelope);

		crs = gc.getCrs();

		// Object object = gc.getProperties().get("GC_NODATA");
		// gc.getSampleDimension(0).getNoDataValues();
		// log.debug(object);
	    }

	    return gc;

	} catch (Exception e) {
	    throw new RuntimeException(
		    "Exception while accessing the GeoObject for " + getId()
			    + " " + this, e);
	}
    }

    /**
     * Returns the cached {@link Style} for this Layer. Tries to load the
     * {@link Style} from a file with the same URL but the ending
     * <code>.sld</code>. If it doesn't exist, returns a default RasterStyle.
     * 
     * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons Tzeggai</a>
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
	// LOGGER.debug("unchaching " + getId() + " aka " + getTitle());
	super.uncache();

	/** Close any open attribute table for this layer */
	AVDialogManager.dm_AtlasStyler.disposeInstanceFor(this);

	if (gc != null) {
	    gc.dispose();
	    gc = null;
	}

	envelope = null;
    }

    /**
     * This method returns the value/{@link Translation} pairs that will be
     * shown in the Legend
     */
    @Override
    public RasterLegendData getLegendMetaData() {
	if (legendMetaData == null) {
	    legendMetaData = new RasterLegendData(false);
	}
	return legendMetaData;
    }

    @Override
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
    @Override
    public Double getMaxResolution() {
	// MS-01.sc
	// try {
	// double pixelwidth = getGeoObject().getGridGeometry().getGridRange()
	// .getHigh().getCoordinateValues()[0];
	// double crswidth = getGeoObject().getEnvelope().getUpperCorner()
	// .getDirectPosition().getCoordinate()[0]
	// - getGeoObject().getEnvelope().getLowerCorner()
	// .getDirectPosition().getCoordinate()[0];
	// // LOGGER.debug("resolution of " + getTitle().toString() + " = "
	// // + crswidth / pixelwidth);
	// return crswidth / pixelwidth;
	// } catch (Exception e) {
	// LOGGER.error(e);
	//
	// return 0.;
	// }
//	LOGGER.warn("DpLayerRaster.getMaxResolution() not yet implemented for AbstractGridCoverage2DReader!");
	return null;
	// MS-01.ec
    }

    @Override
    public Double getMinResolution() {
	// docme Auto-generated method stub
	return null;
    }

    @Override
    public void setLegendMetaData(RasterLegendData legendMetaData) {
	this.legendMetaData = legendMetaData;
    }

    // /**
    // * Returns the number of bands contained in this {@link Coverage}
    // */
    // public int getNumSampleDimensions() {
    // // TODO: Determine the sample dimensions from
    // AbstractGridCoverage2DReader
    // LOGGER.error("DpLayerRaster.getNumSampleDimensions() not yet implemented correctly for AbstractGridCoverage2DReader!");
    // // return getGeoObject().getNumSampleDimensions();
    // return 1;
    // }

    @Override
    public DpLayerRaster_Reader copy() {
	DpLayerRaster_Reader copy = new DpLayerRaster_Reader(ac);
	return (DpLayerRaster_Reader) copyTo(copy);
    }

    @Override
    public DpLayer<AbstractGridCoverage2DReader, ChartStyle> copyTo(
	    DpLayer<AbstractGridCoverage2DReader, ChartStyle> target) {

	DpLayerRaster_Reader copy = (DpLayerRaster_Reader) super.copyTo(target);

	copy.setLegendMetaData(getLegendMetaData()); // TODO should be copied!

	return copy;
    }

    @Override
    public ReferencedEnvelope getReferencedEnvelope() {
	if (getEnvelope() == null)
	    return null;
	return new ReferencedEnvelope(getEnvelope(), getCrs());
    }

    @Override
    public int getBandCount() {
	if (bandCount == 0) {
	    Parameter readGG = new Parameter(
		    AbstractGridFormat.READ_GRIDGEOMETRY2D);

	    ReferencedEnvelope mapExtend = new org.geotools.geometry.jts.ReferencedEnvelope(
		    getEnvelope(), getCrs());
	    //
	    // readGG.setValue(new GridGeometry2D(new GeneralGridEnvelope(
	    // getBounds()), mapExtend));
	    readGG.setValue(new GridGeometry2D(new GeneralGridEnvelope(
		    new Rectangle(1, 1)), mapExtend));

	    try {
		GridCoverage2D sourceGrid = getGeoObject().read(
			new GeneralParameterValue[] { readGG });
		bandCount = sourceGrid.getNumSampleDimensions();
	    } catch (Exception e) {
		LOGGER.error("read(new GeneralParameterValue[] { readGG })", e);
	    }
	}
	return bandCount;
    }

    @Override
    public Translation[] getBandNames() {
	if (bandNames.length == 0) {
	    for (int i = 0; i < getBandCount(); i++) {
		setBandNames(new Translation(""));
	    }
	}
	return bandNames;
    }

}
