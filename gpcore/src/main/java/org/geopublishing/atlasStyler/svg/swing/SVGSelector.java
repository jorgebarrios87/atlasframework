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
package org.geopublishing.atlasStyler.svg.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.geopublishing.atlasStyler.ASProps;
import org.geopublishing.atlasStyler.ASProps.Keys;
import org.geopublishing.atlasStyler.ASUtil;
import org.geopublishing.atlasStyler.AtlasStylerVector;
import org.geopublishing.atlasStyler.FreeMapSymbols;
import org.geopublishing.atlasStyler.rulesLists.SingleLineSymbolRuleList;
import org.geopublishing.atlasStyler.rulesLists.SinglePointSymbolRuleList;
import org.geopublishing.atlasStyler.rulesLists.SinglePolygonSymbolRuleList;
import org.geopublishing.atlasStyler.rulesLists.SingleRuleList;
import org.geopublishing.atlasStyler.swing.GraphicEditGUI;
import org.geopublishing.atlasStyler.swing.JScrollPaneSymbols;
import org.geopublishing.atlasViewer.swing.Icons;
import org.geopublishing.geopublisher.GpUtil;
import org.geotools.data.DataUtilities;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.GraphicImpl;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;

import de.schmitzm.geotools.feature.FeatureUtil.GeometryForm;
import de.schmitzm.geotools.styling.StylingUtil;
import de.schmitzm.io.IOUtil;
import de.schmitzm.lang.LangUtil;
import de.schmitzm.swing.CancelButton;
import de.schmitzm.swing.CancellableDialogAdapter;
import de.schmitzm.swing.ExceptionDialog;
import de.schmitzm.swing.FileExtensionFilter;
import de.schmitzm.swing.OkButton;
import de.schmitzm.swing.SwingUtil;
import de.schmitzm.swing.swingworker.AtlasSwingWorker;

/**
 * Swing GUI dialog to choos an SVG graphic from freemapsymbols.org
 */
public class SVGSelector extends CancellableDialogAdapter {
	static private final Logger LOGGER = LangUtil
			.createLogger(SVGSelector.class);

	protected static final Dimension SVGICON_SIZE = AtlasStylerVector.DEFAULT_SYMBOL_PREVIEW_SIZE;

	/**
	 * A property changed event with this ID is fired, when the ExternalGraphic
	 * has been changed.
	 */
	public static final String PROPERTY_UPDATED = "Property Updated event ID";

	private JPanel jContentPane = null;

	private JPanel jPanelButtons = null;

	private JPanel jPanelBrowser = null;

	private JPanel jPanel = null;

	private JButton jButtonOk = null;

	private JButton jButtonCancel = null;

	private JLabel jLabelExplanation = null;

	private JTextField jTextFieldURL = null;

	private JButton jButtonSelfURL = null;

	private JScrollPane jScrollPane1 = null;

	protected URL url;

	private JList jList = null;

	private final GeometryForm geometryForm;

	private JButton jButtonUp = null;

	/**
	 * String KEY is folderUrl.getFile() + rl.getStyleName()
	 */
	final static protected WeakHashMap<String, BufferedImage> weakImageCache = new WeakHashMap<String, BufferedImage>();

	private final ExternalGraphic[] backup;

	protected JLabel notOnlineLabel = new JLabel(
			ASUtil.R("SVGSelector.notOnlineErrorLabel"));

	/**
	 * The String KEY is URL.toString
	 */
	final static Map<String, List<Object>> cachedRuleLists = new ConcurrentHashMap<String, List<Object>>();

	/**
	 * @throws MalformedURLException
	 * 
	 * @param preSelection
	 *            Which SVG is preselected. May be <code>null</code>
	 */
	public SVGSelector(Window owner, GeometryForm geomForm,
			ExternalGraphic[] preSelection) throws MalformedURLException {
		super(owner, ASUtil.R("SVGSelector.window.title"));
		this.geometryForm = geomForm;
		backup = preSelection;

		url = null;

		try {
			if (preSelection != null && preSelection.length > 0) {
				URL location = preSelection[0].getLocation();
				String preselectedFolderUrl = location.toExternalForm()
						.substring(0,
								location.toExternalForm().lastIndexOf("/") + 1);
				if ((preselectedFolderUrl.startsWith(FreeMapSymbols.SVG_URL) && (preselectedFolderUrl
						.endsWith("/")))) {
					url = new URL(preselectedFolderUrl);
				} else {
					LOGGER.info(SVGSelector.class.getSimpleName()
							+ " ignores the preselected URL = "
							+ preSelection[0].getLocation());
				}
			}
		} finally {
			// URL to start with by default
			if (url == null) {
				url = new URL(FreeMapSymbols.SVG_URL);
			}
		}

		initialize();

		rescan(false);

	}

	@Override
	public void cancel() {
		// Reset any changes by promoting the backed-up symbol 
		firePropertyChange(SVGSelector.PROPERTY_UPDATED, null, backup);
	}

	/**
	 * This method initializes a panel with OK and Close buttons
	 */
	private JPanel getJPanelButtons() {
		if (jPanelButtons == null) {
			jPanelButtons = new JPanel(new MigLayout());
			jPanelButtons.add(getJButtonOk(), "tag ok");
			jPanelButtons.add(getJButtonCancel(), "tag cancel");
		}
		return jPanelButtons;
	}

	/**
	 * This method initializes jPanel
	 * 
	 * @return javax.swing jPanelBrowser.add(getJList(), gridBagConstraints9);
	 *         .JPanel
	 */
	private JPanel getJPanelBrowser() {
		if (jPanelBrowser == null) {
			GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
			gridBagConstraints8.fill = GridBagConstraints.BOTH;
			gridBagConstraints8.gridy = 1;
			gridBagConstraints8.weightx = 1.0;
			gridBagConstraints8.weighty = 1.0;
			gridBagConstraints8.gridx = 0;
			jPanelBrowser = new JPanel();
			jPanelBrowser.setLayout(new GridBagLayout());
			jPanelBrowser.add(getJScrollPane1(), gridBagConstraints8);
		}
		return jPanelBrowser;
	}

	/**
	 * This method initializes jPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanel() {
		if (jPanel == null) {
			GridBagConstraints gridBagConstraints9 = new GridBagConstraints();
			gridBagConstraints9.gridx = 0;
			gridBagConstraints9.insets = new Insets(5, 5, 5, 5);
			gridBagConstraints9.gridy = 1;
			GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
			gridBagConstraints7.gridx = 2;
			gridBagConstraints7.insets = new Insets(0, 5, 0, 5);
			gridBagConstraints7.gridy = 1;
			GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
			gridBagConstraints6.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints6.gridy = 1;
			gridBagConstraints6.weightx = 1.0;
			gridBagConstraints6.gridwidth = 1;
			gridBagConstraints6.gridx = 1;
			GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.gridx = 0;
			gridBagConstraints5.gridwidth = 3;
			gridBagConstraints5.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints5.gridy = 0;
			jLabelExplanation = new JLabel();
			jLabelExplanation.setText(ASUtil.R("SVGSelector.Heading.HTML"));
			jPanel = new JPanel();
			jPanel.setLayout(new GridBagLayout());
			jPanel.add(jLabelExplanation, gridBagConstraints5);
			jPanel.add(getJTextFieldURL(), gridBagConstraints6);
			jPanel.add(getJButtonSelfURL(), gridBagConstraints7);
			jPanel.add(getJButtonUp(), gridBagConstraints9);
		}
		return jPanel;
	}

	/**
	 * This method initializes jButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonOk() {
		if (jButtonOk == null) {
			jButtonOk = new OkButton();
			jButtonOk.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					setVisible(false);
				}

			});
		}
		return jButtonOk;
	}

	/**
	 * This method initializes jButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonCancel() {
		if (jButtonCancel == null) {
			jButtonCancel = new CancelButton();
			jButtonCancel.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					cancel();
					setVisible(false);
				}

			});
		}
		return jButtonCancel;
	}

	/**
	 * This method initializes jTextField
	 * 
	 * @return javax.swing.JTextField
	 */
	private JTextField getJTextFieldURL() {
		if (jTextFieldURL == null) {
			jTextFieldURL = new JTextField();
			jTextFieldURL.setEditable(false);
			jTextFieldURL.setText(url.getFile());
		}
		return jTextFieldURL;
	}

	/**
	 * This method initializes jButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonSelfURL() {
		if (jButtonSelfURL == null) {
			jButtonSelfURL = new JButton();
			jButtonSelfURL.setToolTipText(ASUtil
					.R("ExternalGraphicsSelector.button.manual_URL_ToolTip"));

			jButtonSelfURL.setAction(new AbstractAction(ASUtil
					.R("SymbolSelector.Tabs.LocalSymbols")) {

				@Override
				public void actionPerformed(ActionEvent e) {

					File startFolder = ASProps.get(Keys.lastLocalSvgSelected) == null ? new File(
							System.getProperty("user.home")) : new File(ASProps
							.get(Keys.lastLocalSvgSelected));

					// try to use an HTML view based on Lobo/Cobra
					File graphicFile = null;
					try {
						Class<?> clazz = Class
								.forName("org.geopublishing.atlasStyler.AsSwingUtil");
						for (Method m : clazz.getMethods()) {
							if (m.getName().equals("chooseFileOpen")) {
								graphicFile = (File) m.invoke(
										null,
										SVGSelector.this,
										startFolder,
										ASUtil.R("SelectLocalSVGFileAction.window.title"),
										new FileExtensionFilter[] {
												new FileExtensionFilter(
														ASUtil.FILTER_SVG),
												new FileExtensionFilter(
														ASUtil.FILTER_PNG) });

							}
						}
					} catch (Exception e1) {
						graphicFile = GpUtil.chooseFileOpenFallback(
								SVGSelector.this,
								startFolder,
								ASUtil.R("SelectLocalSVGFileAction.window.title"),
								new FileExtensionFilter(
										ASUtil.FILTER_EXTERNALGRAPHIC_FORMATS));
					}

					// File graphicFile =
					// GpUtil.chooseFileOpenFallback(SVGSelector.this,
					// startFolder, ASUtil
					// .R("SelectLocalSVGFileAction.window.title"), new
					// FileExtensionFilter(ASUtil.FILTER_SVG),
					// new FileExtensionFilter(ASUtil.FILTER_PNG));

					if (graphicFile != null) {
						// Remember the last SVG file location
						ASProps.set(Keys.lastLocalSvgSelected,
								graphicFile.getAbsolutePath());
					} else
						return;

					URL imageUrl = IOUtil.fileToURL(graphicFile);

					LOGGER.info(imageUrl);

					String mimetype = GraphicEditGUI.PNG_MIMETYPE;
					if (imageUrl.getFile().toLowerCase().endsWith("svg")) {
						mimetype = GraphicEditGUI.SVG_MIMETYPE;
					}

					ExternalGraphic eg = StylingUtil.STYLE_BUILDER
							.createExternalGraphic(imageUrl, mimetype);
					final ExternalGraphic[] egs = new ExternalGraphic[] { eg };

					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							SVGSelector.this.firePropertyChange(
									SVGSelector.PROPERTY_UPDATED, null, egs);
							SVGSelector.this.okClose();
						}
					});

				}
			});
		}
		return jButtonSelfURL;
	}

	/**
	 * This method initializes jScrollPane1
	 * 
	 * @return javax.swing.JScrollPane
	 */
	private JScrollPane getJScrollPane1() {
		if (jScrollPane1 == null) {
			jScrollPane1 = new JScrollPane();
			jScrollPane1.setViewportView(getJList());
		}
		return jScrollPane1;
	}

	/**
	 * This method initializes jList
	 * 
	 * @return javax.swing.JList
	 */
	private JList getJList() {
		if (jList == null) {
			jList = new JList();
			jList.setModel(new DefaultListModel());

			jList.setToolTipText(ASUtil.R("SVGSelector.list.tooltip"));//

			// The JList has to react on click
			jList.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					int i = jList.locationToIndex(e.getPoint());

					if ((e.getClickCount() == 2)
							&& (e.getButton() == MouseEvent.BUTTON1)) {

						Object value = jList.getModel().getElementAt(i);

						final ExternalGraphic[] egs;

						if (value instanceof SinglePolygonSymbolRuleList) {
							/***************************************************
							 * A Symbol has been selected. Throws an event.
							 */

							final SinglePolygonSymbolRuleList rl = (SinglePolygonSymbolRuleList) value;
							PolygonSymbolizer symbolizer = rl.getSymbolizers()
									.get(0);
							egs = symbolizer.getFill().getGraphicFill()
									.getExternalGraphics();
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									SVGSelector.this.firePropertyChange(
											SVGSelector.PROPERTY_UPDATED, null,
											egs);
								}
							});

						} else if (value instanceof SinglePointSymbolRuleList) {
							/***************************************************
							 * A Symbol has been selected. Throws an event.
							 */

							if (geometryForm == GeometryForm.POINT) {
								final SinglePointSymbolRuleList rl = (SinglePointSymbolRuleList) value;
								PointSymbolizer symbolizer = rl
										.getSymbolizers().get(0);
								egs = symbolizer.getGraphic()
										.getExternalGraphics();
								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										SVGSelector.this.firePropertyChange(
												SVGSelector.PROPERTY_UPDATED,
												null, egs);
									}
								});
							}
						} else if (value instanceof URL) {
							/***************************************************
							 * A folder has been selected... Change the
							 * directory!
							 */
							changeURL((URL) value);

						}

					}

				}

			});

			jList.setCellRenderer(new ListCellRenderer() {

				@Override
				public Component getListCellRendererComponent(JList list,
						Object value, int index, boolean isSelected,
						boolean cellHasFocus) {

					JPanel fullCell = new JPanel(new BorderLayout());
					fullCell.setSize(JScrollPaneSymbols.size);
					fullCell.setBorder(BorderFactory.createEmptyBorder(2, 4, 2,
							5));

					fullCell.setBackground(Color.white);
					JPanel infos = new JPanel(new BorderLayout());
					JPanel nameAuthor = new JPanel(new BorderLayout());
					JLabel styleName = new JLabel();
					nameAuthor.add(styleName, BorderLayout.WEST);
					JLabel styleAuthor = new JLabel();
					nameAuthor.add(styleAuthor, BorderLayout.EAST);
					infos.add(nameAuthor, BorderLayout.NORTH);
					JLabel description = new JLabel();
					infos.add(description, BorderLayout.CENTER);
					description.setFont(description.getFont().deriveFont(8f));
					styleAuthor.setFont(styleAuthor.getFont().deriveFont(9f)
							.deriveFont(Font.ITALIC));
					fullCell.add(infos, BorderLayout.CENTER);

					if (value instanceof SingleRuleList<?>) {
						/**
						 * We have a SVG Symbol
						 */

						SingleRuleList rl = (SingleRuleList) value;

						BufferedImage symbolImage;

						symbolImage = weakImageCache.get(getKey(rl));
						// LOGGER.debug("Looking for key =
						// "+folderUrl.getFile()+rl.getStyleName() );
						if (symbolImage == null) {
							// We should not have to render it often, as we
							// render it parallel to downloading. But the cache
							// is weak, so...
							symbolImage = rl.getImage(SVGICON_SIZE);
							// LOGGER.debug("Rendering an Image on the EDT. key
							// = "+folderUrl.getFile()+rl.getStyleName() );
							weakImageCache.put(getKey(rl), symbolImage);
						}

						fullCell.add(new JLabel(new ImageIcon(symbolImage)),
								BorderLayout.WEST);

						styleName.setText(rl.getStyleName());
						styleAuthor.setText(rl.getStyleTitle());
						description.setText(rl.getStyleAbstract());

						fullCell.setToolTipText("Double-click to use ExternalGraphic "
								+ IOUtil.escapePath(url.toString()
										+ rl.getStyleName() + ".svg"));

					} else if (value instanceof URL) {
						/**
						 * We have a folder
						 */
						JLabel icon = new JLabel("DIR"); // i8n TODO icon
						// better than text ?

						icon.setPreferredSize(SVGICON_SIZE);
						fullCell.add(icon, BorderLayout.WEST);

						URL dirUrl = (URL) value;
						String string = dirUrl.getFile();
						String dirName = string.substring(0,
								string.lastIndexOf("/"));
						dirName = dirName.substring(
								dirName.lastIndexOf("/") + 1, dirName.length());

						styleName.setText("Folder: " + dirName); // i8n
						styleAuthor.setText("");
						description.setText(ASUtil
								.R("SVGSelector.list.tooltip"));
					}

					if (isSelected) {
						fullCell.setBorder(BorderFactory.createEtchedBorder(
								Color.YELLOW, Color.BLACK));
					} else {
						fullCell.setBorder(BorderFactory.createEtchedBorder(
								Color.WHITE, Color.GRAY));
					}

					return fullCell;
				}

			});

			// The JList has to react to movement
			jList.addMouseMotionListener(new MouseMotionAdapter() {

				@Override
				public void mouseMoved(MouseEvent me) {
					Point p = new Point(me.getPoint());
					jList.setSelectedIndex(jList.locationToIndex(p));
				}
			});
		}
		return jList;
	}

	/**
	 * Generates a String key under which the SVG-Component are cached for
	 * faster GUI.
	 */
	String getKey(SingleRuleList rl) {
		return url.getFile() + rl.getStyleName();
	}

	public void changeURL(URL newUrl) {
		url = newUrl;

		LOGGER.debug("Changing " + this.getClass().getSimpleName() + " URL to "
				+ url.getFile());

		jList = null;
		rescan(false);
		jScrollPane1.setViewportView(getJList());
		jTextFieldURL.setText(url.getFile());

		jButtonUp.setEnabled(!url.toString().equals(
				FreeMapSymbols.SVG_URL + "/"));

	}

	/**
	 * This method initializes jButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonUp() {
		if (jButtonUp == null) {
			jButtonUp = new JButton();
			jButtonUp.setIcon(Icons.ICON_DIR_UP_SMALL);
			jButtonUp.setMargin(new Insets(0, 0, 0, 0));
			jButtonUp.setEnabled(!url.toString().equals(
					FreeMapSymbols.SVG_URL + "/"));

			// This button is only enabled if we are not on the root level
			jButtonUp.addActionListener(new AbstractAction() {

				private static final long serialVersionUID = 6536632895051071149L;

				@Override
				public void actionPerformed(ActionEvent e) {
					// Transform
					// http://http://freemapsymbols.org/svg/health/
					// To: http://http://freemapsymbols.org/svg/
					String string = url.toExternalForm();
					String newUrl = string.substring(0, string.lastIndexOf("/"));
					newUrl = newUrl.substring(0, newUrl.lastIndexOf("/") + 1);
					try {
						URL url2 = new URL(newUrl);
						changeURL(url2);
					} catch (MalformedURLException e1) {
						ExceptionDialog.show(SVGSelector.this, e1);
					}
				}
			});

			jButtonUp.setToolTipText(ASUtil
					.R("ExternalGraphicsSelector.button.directory_up_ToolTip"));

		}
		return jButtonUp;
	}

	/**
	 * @return A SwingWorker that adds the Online-Symbols in a background task.
	 * 
	 * @author <a href="mailto:skpublic@wikisquare.de">Stefan Alfons Tzeggai</a>
	 */
	private AtlasSwingWorker<List<Object>> getWorker() {
		AtlasSwingWorker<List<Object>> swingWorker = new AtlasSwingWorker<List<Object>>(
				SVGSelector.this) {

			@Override
			protected List<Object> doInBackground() throws Exception {

				List<Object> entriesForTheList = new ArrayList<Object>();
				// i18n
				LOGGER.debug("Seaching for online Symbols");

				URL index = url;

				BufferedReader in = null;
				try {
					in = new BufferedReader(new InputStreamReader(
							index.openStream()));

					// Parsing the APACHE OUTPUT

					// Sorting them alphabetically by using a set
					SortedSet<URL> sortedSymbolUrls = new TreeSet(
							new Comparator<URL>() {

								@Override
								public int compare(URL o1, URL o2) {
									return o1.toExternalForm().compareTo(
											o2.toExternalForm());
								}
							});
					String line;

					while ((line = in.readLine()) != null) {

						parseSldFileanmeFromApacheOutput(entriesForTheList,
								sortedSymbolUrls, line);
					}

					for (final URL url : sortedSymbolUrls) {

						String path = url.getFile();

						/*******************************************************
						 * Checking if a Style with the same name allready
						 * exists
						 */
						// Name without .sld
						String newNameWithOutEnding = path.substring(0,
								path.length() - 4);
						newNameWithOutEnding = newNameWithOutEnding
								.substring(newNameWithOutEnding
										.lastIndexOf("/") + 1);

						/** ExternalGraphic to online Graphic * */
						ExternalGraphic eg = ASUtil.SB.createExternalGraphic(
								url, GraphicEditGUI.SVG_MIMETYPE);

						final SingleRuleList symbolRuleList;

						GraphicImpl g = (GraphicImpl) ASUtil
								.createDefaultGraphic();
						g.setSize(ASUtil.ff.literal(SVGICON_SIZE.height));
						g.graphicalSymbols().clear();
						g.graphicalSymbols().add(eg);

						if (geometryForm == GeometryForm.POINT) {
							symbolRuleList = new SinglePointSymbolRuleList("");
							symbolRuleList.addNewDefaultLayer();
							((PointSymbolizer) symbolRuleList.getSymbolizers()
									.get(0)).setGraphic(g);

						} else if (geometryForm == GeometryForm.LINE) {
							symbolRuleList = new SingleLineSymbolRuleList("");
							symbolRuleList.addNewDefaultLayer();
							((LineSymbolizer) symbolRuleList.getSymbolizers()
									.get(0)).getStroke().setGraphicStroke(g);

						} else {
							symbolRuleList = new SinglePolygonSymbolRuleList("");
							symbolRuleList.addNewDefaultLayer();
							((PolygonSymbolizer) symbolRuleList
									.getSymbolizers().get(0)).getFill()
									.setGraphicFill(g);
						}

						symbolRuleList.setStyleName(newNameWithOutEnding);

						String key = url.getFile()
								+ symbolRuleList.getStyleName();
						if (weakImageCache.get(key) == null) {
							/**
							 * Render the image now
							 */
							// LOGGER.debug("Rendering an Image for the cache.
							// key = "+key );
							try {
								weakImageCache.put(key,
										symbolRuleList.getImage());
							} catch (Exception e) {
								ExceptionDialog.show(SVGSelector.this, e);
							}
						}

						entriesForTheList.add(symbolRuleList);
					}

				} catch (java.net.UnknownHostException e) {
					// This is handled in the done() method.
				} catch (Exception e) {
					ExceptionDialog.show(SwingUtil
							.getParentWindowComponent(SVGSelector.this), e);
				} finally {
					if (in != null)
						in.close();
				}

				return entriesForTheList;
			}

		};
		return swingWorker;
	}

	/**
	 * @param reset
	 *            if <code>false</code> uses cached values when possible
	 */
	private void rescan(boolean reset) {
		// getWorker().execute();

		final String key = url.toString();

		if (reset) {
			getJList().setModel(new DefaultListModel());
			cachedRuleLists.remove(key);
		}

		if (cachedRuleLists.get(key) == null) {
			cachedRuleLists.put(key, getWorker().executeModalNoEx());
		}

		// Add new or cached RuleLists to the GUI model
		addNewRuleListsToModel();

	}

	private void addNewRuleListsToModel() {
		final DefaultListModel model = (DefaultListModel) getJList().getModel();
		model.clear();

		List<Object> entriesForTheList;
		entriesForTheList = cachedRuleLists.get(url.toString());

		try {
			if ((entriesForTheList.size() == 0) && (model.size() == 0)) {
				/*
				 * We are not rescanning and we are probably not online
				 */
				getJScrollPane1().setViewportView(notOnlineLabel);
			} else {
				/*
				 * Add the RulesLists to the GUI
				 */
				for (Object o : entriesForTheList) {
					model.addElement(o);
				}

				getJScrollPane1().setViewportView(SVGSelector.this.getJList());

			}
			SVGSelector.this.doLayout();
			SVGSelector.this.repaint();

		} catch (Exception e) {
			ExceptionDialog.show(SVGSelector.this, e);
		}
	}

	// final static Pattern svgFilePattern =

	final static Pattern dirPattern = Pattern.compile("\"[^\"/]*/\"");
	final static Pattern svgFilePattern = Pattern.compile("\"[^\"]*.svg\"");

	void parseSldFileanmeFromApacheOutput(List<Object> entriesForTheList,
			Set<URL> sortedSymbolURLStrings, String line)
			throws MalformedURLException {

		Matcher matcher = svgFilePattern.matcher(line);
		if (matcher.find()) {
			String svgName = matcher.group();
			String svgName2 = svgName.substring(1, svgName.length() - 1);

			sortedSymbolURLStrings.add(DataUtilities.extendURL(url, svgName2));
		} else {
			/**
			 * This hits lines that point to a directory, but not to the parent
			 * link.
			 */
			matcher = dirPattern.matcher(line);
			if (matcher.find()) {
				String dirName = matcher.group();
				String dirName2 = dirName.substring(1, dirName.length() - 1);

				entriesForTheList.add(DataUtilities.extendURL(url, dirName2));
			}

			else {
				// Ignore.. probably HTML
			}

		}
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		this.setSize(350, 450);
		this.setContentPane(getJContentPane());
	}

	/**
	 * This method initializes jContentPane
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJContentPane() {
		if (jContentPane == null) {
			GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = 0;
			gridBagConstraints2.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints2.gridy = 0;
			GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.gridx = 0;
			gridBagConstraints1.fill = GridBagConstraints.BOTH;
			gridBagConstraints1.weightx = 1.0;
			gridBagConstraints1.weighty = 1.0;
			gridBagConstraints1.ipady = 0;
			gridBagConstraints1.gridy = 1;
			GridBagConstraints gridBagConstraints = new GridBagConstraints();
			gridBagConstraints.gridx = 0;
			gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints.gridy = 2;
			jContentPane = new JPanel();
			jContentPane.setLayout(new GridBagLayout());
			jContentPane.add(getJPanelButtons(), gridBagConstraints);
			jContentPane.add(getJPanelBrowser(), gridBagConstraints1);
			jContentPane.add(getJPanel(), gridBagConstraints2);
		}
		return jContentPane;
	}
}
