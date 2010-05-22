/*******************************************************************************
 * Copyright (c) 2010 Stefan A. Tzeggai (soon changing to Stefan A. Tzeggai).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan A. Tzeggai (soon changing to Stefan A. Tzeggai) - initial API and implementation
 ******************************************************************************/
package org.geopublishing.atlasStyler;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.geotools.brewer.color.BrewerPalette;
import org.geotools.brewer.color.ColorBrewer;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import skrueger.geotools.StyledFS;

public class GraduatedColorRuleListTest  {

	private FeatureSource<SimpleFeatureType, SimpleFeature> featureSource_polygon;

	@Test
	/**
	 * Pretty nonsense :-(
	 */
	public void testGraduatedColorRuleList() throws IOException {
		URL shpURL = AtlasStylerTest.class.getResource(
				"/data/shp countries/country.shp");
		assertNotNull(shpURL);
		Map<Object, Object> params = new HashMap<Object, Object>();
		params.put("url", shpURL);
		DataStore dataStore = DataStoreFinder.getDataStore(params);
		featureSource_polygon = dataStore.getFeatureSource(dataStore
				.getTypeNames()[0]);

		GraduatedColorPolygonRuleList polyRL = new GraduatedColorPolygonRuleList(
				new StyledFS(featureSource_polygon ));
		
		polyRL.pushQuite();
		
		BrewerPalette[] palettes = ColorBrewer
				.instance(ColorBrewer.QUALITATIVE).getPalettes();
		for (BrewerPalette bp : palettes) {
			polyRL.setBrewerPalette(bp);
			assertTrue(bp.getMaxColors() <= bp.getPaletteSuitability()
					.getMaxColors());
		}

		polyRL.popQuite();
	}
}
