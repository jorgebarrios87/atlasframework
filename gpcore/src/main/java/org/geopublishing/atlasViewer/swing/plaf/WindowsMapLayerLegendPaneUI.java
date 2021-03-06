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
/*
 * $Id$
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.geopublishing.atlasViewer.swing.plaf;

import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;

import org.geopublishing.atlasViewer.swing.MapLayerLegend;
import org.jdesktop.swingx.JXTaskPane;

/**
 * Windows implementation of the TaskPaneUI.
 * 
 * @author <a href="mailto:fred@L2FProd.com">Frederic Lavigne</a>
 */
public class WindowsMapLayerLegendPaneUI extends BasicMapLayerLegendPaneUI {

	public static ComponentUI createUI(JComponent c) {
		return new WindowsMapLayerLegendPaneUI();
	}

	@Override
	protected Border createPaneBorder() {
		return new XPPaneBorder();
	}

	/**
	 * Overriden to paint the background of the component but keeping the
	 * rounded corners.
	 */
	@Override
	public void update(Graphics g, JComponent c) {
		if (c.isOpaque()) {
			g.setColor(c.getParent().getBackground());
			g.fillRect(0, 0, c.getWidth(), c.getHeight());
			g.setColor(c.getBackground());
			g.fillRect(0, getRoundHeight(), c.getWidth(), c.getHeight()
					- getRoundHeight());
		}
		paint(g, c);
	}

	/**
	 * The border of the taskpane group paints the "text", the "icon", the
	 * "expanded" status and the "special" type.
	 * 
	 */
	class XPPaneBorder extends PaneBorder {

		@Override
		protected void paintTitleBackground(MapLayerLegend layerLegend,
				Graphics g) {
			if (layerLegend.isSpecial()) {
				g.setColor(specialTitleBackground);
				g.fillRoundRect(0, 0, layerLegend.getWidth(),
						getRoundHeight() * 2, getRoundHeight(),
						getRoundHeight());
				g.fillRect(0, getRoundHeight(), layerLegend.getWidth(),
						getTitleHeight(layerLegend) - getRoundHeight());
			} else {
				Paint oldPaint = ((Graphics2D) g).getPaint();
				GradientPaint gradient = new GradientPaint(
						0f,
						layerLegend.getWidth() / 2,
						layerLegend.getComponentOrientation().isLeftToRight() ? titleBackgroundGradientStart
								: titleBackgroundGradientEnd,
						layerLegend.getWidth(),
						getTitleHeight(layerLegend),
						layerLegend.getComponentOrientation().isLeftToRight() ? titleBackgroundGradientEnd
								: titleBackgroundGradientStart);

				((Graphics2D) g).setRenderingHint(
						RenderingHints.KEY_COLOR_RENDERING,
						RenderingHints.VALUE_COLOR_RENDER_QUALITY);
				((Graphics2D) g).setRenderingHint(
						RenderingHints.KEY_INTERPOLATION,
						RenderingHints.VALUE_INTERPOLATION_BILINEAR);
				((Graphics2D) g).setRenderingHint(RenderingHints.KEY_RENDERING,
						RenderingHints.VALUE_RENDER_QUALITY);
				((Graphics2D) g).setPaint(gradient);
				g.fillRoundRect(0, 0, layerLegend.getWidth(),
						getRoundHeight() * 2, getRoundHeight(),
						getRoundHeight());
				g.fillRect(0, getRoundHeight(), layerLegend.getWidth(),
						getTitleHeight(layerLegend) - getRoundHeight());
				((Graphics2D) g).setPaint(oldPaint);
			}
		}

		@Override
		protected void paintExpandedControls(JXTaskPane mapLayerLegend,
				Graphics g, int x, int y, int width, int height) {
			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);

			paintOvalAroundControls(mapLayerLegend, g, x, y, width, height);
			g.setColor(getPaintColor(mapLayerLegend));
			paintChevronControls(mapLayerLegend, g, x, y, width, height);

			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_OFF);
		}

		@Override
		protected boolean isMouseOverBorder() {
			return true;
		}

	}

}
