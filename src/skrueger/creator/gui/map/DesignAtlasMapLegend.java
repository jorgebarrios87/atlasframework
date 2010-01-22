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
package skrueger.creator.gui.map;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.log4j.Logger;
import org.geotools.map.MapLayer;

import schmitzm.geotools.gui.FeatureLayerFilterDialog;
import schmitzm.geotools.gui.GeoMapPane;
import schmitzm.geotools.map.event.FeatureSelectedEvent;
import schmitzm.geotools.map.event.JMapPaneListener;
import schmitzm.geotools.map.event.MapPaneEvent;
import schmitzm.jfree.chart.style.ChartStyle;
import skrueger.atlas.ExportableLayer;
import skrueger.atlas.dp.DpEntry;
import skrueger.atlas.dp.DataPool.EventTypes;
import skrueger.atlas.dp.layer.DpLayer;
import skrueger.atlas.dp.layer.DpLayerVectorFeatureSource;
import skrueger.atlas.gui.MapLayerLegend;
import skrueger.atlas.gui.MapLegend;
import skrueger.atlas.gui.map.AtlasMapLegend;
import skrueger.atlas.map.Map;
import skrueger.creator.AtlasConfigEditable;
import skrueger.creator.gui.DesignAtlasChartJDialog;
import skrueger.geotools.MapPaneToolBar;
import skrueger.geotools.StyledLayerInterface;
import skrueger.sld.ASUtil;

public class DesignAtlasMapLegend extends AtlasMapLegend {
	protected Logger LOGGER = ASUtil.createLogger(this);

	private final AtlasConfigEditable ace;

	/**
	 * This listener will update the whole legend whenever the DPE changed. The
	 * listener is added when the legend is created, and removed when the legend
	 * is disposed.
	 */
	private final PropertyChangeListener dataPoolChangesListener = new PropertyChangeListener() {

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if (evt.getPropertyName().equals(EventTypes.changeDpe.toString())
					|| evt.getPropertyName().equals(
							EventTypes.removeDpe.toString())) {
				// rememberLegend.clear();
				recreateLayerList();
			}
		}

	};

	@Override
	protected boolean selectionButtonShallAppearFor(
			final DpLayer<?, ? extends ChartStyle> dpl) {
		return super.selectionButtonShallAppearFor(dpl)
				|| dpl instanceof DpLayerVectorFeatureSource
				&& DesignAtlasChartJDialog
						.isOpenForLayer((DpLayerVectorFeatureSource) dpl);
	}

	public DesignAtlasMapLegend(final GeoMapPane geoMapPane, Map map,
			final AtlasConfigEditable ace, MapPaneToolBar mapPaneToolBar) {
		super(geoMapPane, map, ace, mapPaneToolBar);
		this.ace = ace;

		/** Listen to changes in the datapool **/
		ace.getDataPool().addChangeListener(dataPoolChangesListener);

		/***********************************************************************
		 * This Listener reacts to the FilterDialog. If a Filter is applied, it
		 * is saved in the corresponding DPLayerVector.
		 **********************************************************************/
		geoMapPane.getMapPane().addMapPaneListener(new JMapPaneListener() {

			public void performMapPaneEvent(MapPaneEvent e) {
				if (e instanceof FeatureSelectedEvent) {
					FeatureSelectedEvent fse = (FeatureSelectedEvent) e;
					if (fse.getSourceObject() instanceof FeatureLayerFilterDialog) {
						FeatureLayerFilterDialog fDialog = (FeatureLayerFilterDialog) fse
								.getSourceObject();

						String layerTitle = fse.getSourceLayer().getTitle();
						DpEntry dpe = ace.getDataPool().get(layerTitle);
						if (dpe instanceof DpLayerVectorFeatureSource) {
							DpLayerVectorFeatureSource dplv = (DpLayerVectorFeatureSource) dpe;
							dplv.setFilter(fDialog.getFilter());
						}
					}
				}

			}
		});
	}

	//
	// /**
	// * This is an overridden method. It calls the super method first and only
	// * add a second listener which saves the selected additional style.
	// *
	// *
	// * @param mapLayer
	// * The GeoTools {@link MapLayer} that will be affected by style
	// * changes.
	// * @param availableStyles
	// * A {@link List} of IDs of AdditionalStyles
	// * @param dpLayer
	// * The {@link DpLayer} that holds all the additional styles
	// *
	// * @return a new {@link JTabbedPane} that will represent all available
	// * Styles for a {@link DpLayer}.
	// */
	// @Override
	// protected Component createTabbedSylesPane(final MapLayer mapLayer,
	// final ArrayList<String> availableStyles, final DpLayer<?,ChartStyle>
	// dpLayer) {
	//
	// final Component legendPanel = super.createTabbedSylesPane(mapLayer,
	// availableStyles, dpLayer);
	//
	// return legendPanel;
	// }

	/**
	 * @param gmp
	 *            The {@link GeoMapPane} that the legend is working on
	 * @param mapLayer
	 *            The maplayer presented by this {@link MapLayerLegend}
	 * @param exportable
	 *            <code>null</code> or instance of {@link ExportableLayer} if
	 *            the layer can be exported
	 * @param styledObj
	 *            the {@link StyledLayerInterface} object that is presented by
	 *            this {@link MapLayerLegend}
	 * @param layerPanel
	 *            The parent {@link MapLegend} or {@link DesignAtlasMapLegend}
	 * 
	 * @return Generally constructs a {@link MapLayerLegend} with the given
	 *         paramters. This method is overridden by
	 *         {@link DesignAtlasMapLegend} to create DesignLayerPaneGroup
	 *         objects.
	 * 
	 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons
	 *         Kr&uuml;ger</a>
	 */
	@Override
	protected MapLayerLegend createMapLayerLegend(MapLayer mapLayer,
			ExportableLayer exportable, StyledLayerInterface<?> styledObj,
			MapLegend layerPanel) {

		String id = styledObj.getId();
		DpLayer<?, ChartStyle> dpLayer = (DpLayer<?, ChartStyle>) ace
				.getDataPool().get(id);

		if (dpLayer == null) {
			throw new RuntimeException("Can't find the layer id " + id
					+ " in the datapool");
		}

		DesignAtlasMapLayerLegend designLayerPaneGroup = new DesignAtlasMapLayerLegend(
				mapLayer, exportable, (DesignAtlasMapLegend) layerPanel,
				dpLayer, getMap());

		/**
		 * Check if the designLayerPaneGroup should come up minimized
		 */
		Boolean minimized = getMap().getMinimizedInLegendMap().get(
				dpLayer.getId());
		designLayerPaneGroup
				.setCollapsed(minimized != null ? minimized : false);

		/**
		 * Check if the designLayerPaneGroup is marked as "hideInLegend" in AV?
		 */
		Boolean hide = getMap().getHideInLegendMap().get(dpLayer.getId());
		if (hide != null && hide == true)
			designLayerPaneGroup.setBackground(Color.red);

		return designLayerPaneGroup;
	}

	public AtlasConfigEditable getAce() {
		return ace;
	}

	/**
	 * Helpt the GarbageCollection...
	 */
	public void dispose() {

		// In atlasMapView, the unneeded styledobjects are uncached if not
		// needed by the next map.. but for the DesignMapView we dispose them by
		// hand...
		for (StyledLayerInterface<?> styledObj : rememberId2StyledLayer
				.values()) {
			styledObj.uncache();
		}

		super.dispose();

		// The DataPool listeners are held in a WeakHashMap.. so no need to
		// remove
		// getAce().getDataPool().removeChangeListener(dataPoolChangesListener);
	}

}
