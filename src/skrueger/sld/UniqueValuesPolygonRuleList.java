/*******************************************************************************
 * Copyright (c) 2010 Stefan A. Krüger (soon changing to Stefan A. Tzeggai).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Stefan A. Krüger (soon changing to Stefan A. Tzeggai) - initial API and implementation
 ******************************************************************************/
package skrueger.sld;

import org.apache.log4j.Logger;
import org.geotools.styling.FeatureTypeStyle;

import skrueger.geotools.StyledFeaturesInterface;

public class UniqueValuesPolygonRuleList extends UniqueValuesRuleList {
	private Logger LOGGER = Logger.getLogger(UniqueValuesPolygonRuleList.class);

	public UniqueValuesPolygonRuleList(StyledFeaturesInterface<?> styledFeatures) {
		super(styledFeatures);
	}

	@Override
	public SingleRuleList getDefaultTemplate() {
		return ASUtil.getDefaultPolygonTemplate();
	}

	@Override
	public void importTemplate(FeatureTypeStyle importFTS) {
		setTemplate(ASUtil.importPolygonTemplateFromFirstRule(importFTS));
	}

	@Override
	public RulesListType getTypeID() {
		return RulesListType.UNIQUE_VALUE_POLYGON;
	}
}
