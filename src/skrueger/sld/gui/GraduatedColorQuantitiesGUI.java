/*******************************************************************************
 * Copyright (c) 2009 Stefan A. Krüger.
 * 
 * This file is part of the AtlasStyler SLD/SE Editor application - A Java Swing-based GUI to create OGC Styled Layer Descriptor (SLD 1.0) / OGC Symbology Encoding 1.1 (SE) XML files.
 * http://www.geopublishing.org
 * 
 * AtlasStyler SLD/SE Editor is part of the Geopublishing Framework hosted at:
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
package skrueger.sld.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import net.miginfocom.swing.MigLayout;

import org.apache.log4j.Logger;
import org.geotools.brewer.color.BrewerPalette;
import org.geotools.brewer.color.PaletteType;
import org.geotools.map.event.MapLayerEvent;
import org.geotools.map.event.MapLayerListener;
import org.geotools.styling.Symbolizer;

import schmitzm.geotools.map.event.MapLayerAdapter;
import schmitzm.swing.ExceptionDialog;
import schmitzm.swing.SwingUtil;
import skrueger.i8n.Translation;
import skrueger.sld.ASUtil;
import skrueger.sld.AtlasStyler;
import skrueger.sld.GraduatedColorRuleList;
import skrueger.sld.RuleChangeListener;
import skrueger.sld.RuleChangedEvent;
import skrueger.sld.SingleRuleList;
import skrueger.sld.classification.ClassificationChangeEvent;
import skrueger.sld.classification.ClassificationChangedAdapter;
import skrueger.sld.classification.QuantitiesClassification;
import skrueger.sld.classification.QuantitiesClassification.METHOD;
import skrueger.swing.TranslationAskJDialog;
import skrueger.swing.TranslationEditJPanel;

public class GraduatedColorQuantitiesGUI extends JPanel implements
		ClosableSubwindows {

	private static final Dimension ICON_SIZE = new Dimension(25, 25);

	final protected static Logger LOGGER = Logger
			.getLogger(GraduatedColorQuantitiesGUI.class);

	private static final long serialVersionUID = 1L;

	private JPanel jPanel = null;

	private JPanel jPanel2 = null;

	private JLabel jLabelClassificationTypeDescription = null;

	private JLabel jLabelParam = null;

	private JToggleButton jToggleButton_Classify = null;

	private JLabel jLabel2 = null;

	private JLabel jLabel3 = null;

	private JComboBox jComboBoxValueField = null;

	private JComboBox jComboBoxNormlization = null;

	private JTable jTable = null;

	private JLabel jLabelHeading = null;

	protected final GraduatedColorRuleList rulesList;

	private final QuantitiesClassification classifier;

	protected SwingWorker<TreeSet<Double>, String> calculateStatisticsWorker;

	private DefaultTableModel tableModel;

	protected QuantitiesClassificationGUI quantGUI;

	private JComboBox jComboBoxPalettes = null;

	private JLabel jLabelTemplate = null;

	private JButton jButtonTemplate = null;

	private final AtlasStyler atlasStyler;

	private NumClassesJComboBox jComboBoxNumClasses;

	private MapLayerListener listenToFilterChangesAndRecalcStatistics = new MapLayerAdapter() {

		@Override
		public void layerChanged(MapLayerEvent event) {
			if (event.getReason() == MapLayerEvent.FILTER_CHANGED)
				classifier.onFilterChanged();
		}
	};

	/**
	 * This is the default constructor
	 */
	public GraduatedColorQuantitiesGUI(final GraduatedColorRuleList ruleList,
			final AtlasStyler atlasStyler) {

		ruleList.pushQuite();

		this.rulesList = ruleList;
		this.atlasStyler = atlasStyler;

		classifier = new QuantitiesClassification(ruleList.getStyledFeatures(),
				ruleList.getValue_field_name(), ruleList
						.getNormalizer_field_name());
		classifier.setMethod(ruleList.getMethod());
		classifier.setNumClasses(ruleList.getNumClasses());
		classifier.setClassLimits(ruleList.getClassLimits());

		/**
		 * If the ruleList doesn't contain calculated class limits, we have to
		 * start calculation directly.
		 */
		if (ruleList.getClassLimits().size() == 0) {
			classifier.setMethod(METHOD.QUANTILES);
			classifier.setNumClasses(5);
		}

		/** Any changes to the classifier must be reported to the RuleList */
		classifier.addListener(new ClassificationChangedAdapter() {

			@Override
			public void classifierAvailableNewClasses(
					final ClassificationChangeEvent e) {
				ruleList.pushQuite();
				ruleList.setValue_field_name(classifier.getValue_field_name());
				ruleList.setNormalizer_field_name(classifier
						.getNormalizer_field_name());
				ruleList.setMethod(classifier.getMethod());
				// rulesList.setNumClasses(classifier.getNumClasses());
				ruleList.setClassLimits(classifier.getClassLimits(), true); // here

				if (classifier.getMethod() == METHOD.MANUAL) {
					getNumClassesJComboBox().setEnabled(false);
					getNumClassesJComboBox().setSelectedItem(
							new Integer(ruleList.getNumClasses()));
				} else
					getNumClassesJComboBox().setEnabled(true);

				ruleList.popQuite();

				// TODO removed the message because it also appears when t calc
				// thread has been cancelled because another thread staretd

				// // Show a warning if the classification didn't create the
				// expected number of classes
				// if (classifier.getMethod() != METHOD.MANUAL &&
				// getNumClassesJComboBox().getSelectedItem() != null &&
				// !((Number)getNumClassesJComboBox().getSelectedItem()).equals(ruleList.getNumClasses()))
				// {
				// AVUtil.showMessageDialog(GraduatedColorQuantitiesGUI.this,
				// AtlasStyler.R("ClassificationFaildMsg.BadData",
				// getNumClassesJComboBox().getSelectedItem()));
				// }
			}
		});

		atlasStyler.getMapLayer().addMapLayerListener(
				listenToFilterChangesAndRecalcStatistics);

		initialize();

		ruleList.popQuite();
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		setLayout(new MigLayout("wrap 2, w 100%", "grow"));
		jLabelHeading = new JLabel(AtlasStyler
				.R("GraduatedColorQuantities.Heading"));
		jLabelHeading.setFont(jLabelHeading.getFont().deriveFont(
				AtlasStylerTabbedPane.HEADING_FONT_SIZE));

		this.add(jLabelHeading, "span 2");
		this.add(getJPanelFields());
		this.add(getJPanelClassification());
		this.add(getJPanelColorsAndTemplate(), "span 2, growx");
		this.add(new JScrollPane(getJTableClasses()), "span 2, grow, bottom");
	}

	/**
	 * This method initializes jPanel
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelFields() {
		if (jPanel == null) {
			final GridBagConstraints gridBagConstraints8 = new GridBagConstraints();
			gridBagConstraints8.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints8.gridy = 1;
			gridBagConstraints8.weightx = 1.0;
			gridBagConstraints8.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints8.gridx = 1;
			final GridBagConstraints gridBagConstraints7 = new GridBagConstraints();
			gridBagConstraints7.fill = GridBagConstraints.HORIZONTAL;
			gridBagConstraints7.gridy = 0;
			gridBagConstraints7.weightx = 1.0;
			gridBagConstraints7.insets = new Insets(0, 5, 0, 0);
			gridBagConstraints7.gridx = 1;
			final GridBagConstraints gridBagConstraints6 = new GridBagConstraints();
			gridBagConstraints6.gridx = 0;
			gridBagConstraints6.gridy = 1;
			jLabel3 = new JLabel();
			jLabel3.setText(AtlasStyler
					.R("GraduatedColorQuantities.NormalizationAttribute"));
			jLabel3.setToolTipText(AtlasStyler
					.R("GraduatedColorQuantities.NormalizationAttribute.TT"));
			final GridBagConstraints gridBagConstraints5 = new GridBagConstraints();
			gridBagConstraints5.gridx = 0;
			gridBagConstraints5.gridy = 0;
			jLabel2 = new JLabel();
			jLabel2.setText(AtlasStyler
					.R("GraduatedColorQuantities.ValueAttribute"));
			jLabel2.setToolTipText(AtlasStyler
					.R("GraduatedColorQuantities.ValueAttribute.TT"));
			jPanel = new JPanel();
			jPanel.setLayout(new GridBagLayout());
			jPanel.setBorder(BorderFactory.createTitledBorder(AtlasStyler
					.R("GraduatedColorQuantities.Attributes.BorderTitle")));
			jPanel.add(jLabel2, gridBagConstraints5);
			jPanel.add(jLabel3, gridBagConstraints6);
			jPanel.add(getJComboBoxValueField(), gridBagConstraints7);
			jPanel.add(getJComboBoxNormalizationField(), gridBagConstraints8);
		}
		return jPanel;
	}

	/**
	 * This method initializes jPanel2
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelClassification() {
		if (jPanel2 == null) {
			final GridBagConstraints gridBagConstraints4 = new GridBagConstraints();
			gridBagConstraints4.gridx = 2;
			gridBagConstraints4.gridy = 1;
			final GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
			gridBagConstraints3.fill = GridBagConstraints.VERTICAL;
			gridBagConstraints3.gridy = 1;
			gridBagConstraints3.weightx = 1.0;
			gridBagConstraints3.insets = new Insets(0, 0, 0, 10);
			gridBagConstraints3.gridx = 1;
			final GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
			gridBagConstraints2.gridx = 0;
			gridBagConstraints2.gridy = 1;
			jLabelParam = new JLabel();
			jLabelParam.setText(AtlasStyler.R("ComboBox.NumberOfClasses"));
			jLabelParam.setToolTipText(AtlasStyler
					.R("ComboBox.NumberOfClasses.TT"));

			final GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
			gridBagConstraints1.gridx = 0;
			gridBagConstraints1.gridwidth = 3;
			gridBagConstraints1.insets = new Insets(0, 0, 5, 0);
			gridBagConstraints1.gridy = 0;
			jLabelClassificationTypeDescription = new JLabel(AtlasStyler.R(
					"GraduatedColorQuantities.classification.Method",
					classifier.getMethod().getDesc()));
			jLabelClassificationTypeDescription.setToolTipText(classifier
					.getMethod().getToolTip());

			classifier.addListener(new ClassificationChangedAdapter() {

				@Override
				public void classifierMethodChanged(
						final ClassificationChangeEvent e) {
					jLabelClassificationTypeDescription.setText(AtlasStyler.R(
							"GraduatedColorQuantities.classification.Method",
							classifier.getMethod().getDesc()));
					jLabelClassificationTypeDescription
							.setToolTipText(classifier.getMethod().getToolTip());
				}

			});

			jPanel2 = new JPanel();
			jPanel2.setLayout(new GridBagLayout());
			jPanel2.setBorder(BorderFactory.createTitledBorder(AtlasStyler
					.R("GraduatedColorQuantities.classification.BorderTitle")));
			jPanel2.add(jLabelClassificationTypeDescription,
					gridBagConstraints1);
			jPanel2.add(jLabelParam, gridBagConstraints2);
			jPanel2.add(getNumClassesJComboBox(), gridBagConstraints3);
			jPanel2.add(getClassifyJToggleButton(), gridBagConstraints4);
		}
		return jPanel2;
	}

	private NumClassesJComboBox getNumClassesJComboBox() {
		if (jComboBoxNumClasses == null) {
			jComboBoxNumClasses = new NumClassesJComboBox(classifier);
			;
		}
		return jComboBoxNumClasses;
	}

	/**
	 * This method initializes jButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JToggleButton getClassifyJToggleButton() {
		if (jToggleButton_Classify == null) {
			jToggleButton_Classify = new JToggleButton();
			jToggleButton_Classify.setAction(new AbstractAction(AtlasStyler
					.R("GraduatedColorQuantities.Classify.Button")) {

				public void actionPerformed(final ActionEvent e) {
					if (jToggleButton_Classify.isSelected()) {

						// Test here, if the data is problematic and show the
						// exception to the user without opening the dialog.
						try {
							classifier.getStatistics();
						} catch (final Exception eee) {
							jToggleButton_Classify.setSelected(false);
							ExceptionDialog.show(
									GraduatedColorQuantitiesGUI.this, eee);
							return;
						}

						getQuantitiesClassificationGUI().setVisible(true);
					} else {
						getQuantitiesClassificationGUI().setVisible(false);
					}

				}

				private QuantitiesClassificationGUI getQuantitiesClassificationGUI() {
					if (quantGUI == null) {
						quantGUI = new QuantitiesClassificationGUI(
								jToggleButton_Classify, classifier, atlasStyler);
						quantGUI.addWindowListener(new WindowAdapter() {

							@Override
							public void windowClosed(final WindowEvent e) {
								jToggleButton_Classify.setSelected(false);
							}

							@Override
							public void windowClosing(final WindowEvent e) {
								jToggleButton_Classify.setSelected(false);
							}

						});
					}
					return quantGUI;
				}

			});
			jToggleButton_Classify.setToolTipText(AtlasStyler
					.R("GraduatedColorQuantities.Classify.Button.TT"));
		}
		return jToggleButton_Classify;
	}

	/**
	 * This method initializes jComboBox1
	 * 
	 * @return javax.swing.JComboBox
	 */
	private JComboBox getJComboBoxValueField() {
		if (jComboBoxValueField == null) {
			jComboBoxValueField = new AttributesJComboBox(atlasStyler,
					classifier.getValueFieldsComboBoxModel());

			jComboBoxValueField
					.setSelectedItem(rulesList.getValue_field_name());

			jComboBoxValueField
					.addItemListener(new java.awt.event.ItemListener() {
						public void itemStateChanged(
								final java.awt.event.ItemEvent e) {
							if (e.getStateChange() == ItemEvent.SELECTED) {

								final String valueField = (String) e.getItem();

								final Object oldNormSelection = getJComboBoxNormalizationField()
										.getSelectedIndex();

								getJComboBoxNormalizationField()
										.setModel(
												classifier
														.createNormalizationFieldsComboBoxModel());
								// jComboBoxNormlization.repaint();

								/**
								 * We do not divide A/A ! So when Attribute A is
								 * selected as the value field, the normalized
								 * field may not be A.
								 */
								if (valueField == oldNormSelection) {
									getJComboBoxNormalizationField()
											.setSelectedItem(
													QuantitiesClassification.NORMALIZE_NULL_VALUE_IN_COMBOBOX);
								} else {
									getJComboBoxNormalizationField()
											.setSelectedItem(oldNormSelection);
								}

								LOGGER.debug("Set valuefield to " + valueField);
								classifier.setValue_field_name(valueField);

								// When the valueField has been changed by the
								// user, throw away the ruleTitles
								rulesList.getRuleTitles().clear();
							}
						}
					});

			ASUtil.addMouseWheelForCombobox(jComboBoxValueField);
		}
		return jComboBoxValueField;
	}

	/**
	 * This method initializes jComboBox2
	 * 
	 * @return javax.swing.JComboBox
	 */
	private JComboBox getJComboBoxNormalizationField() {
		if (jComboBoxNormlization == null) {

			jComboBoxNormlization = new AttributesJComboBox(atlasStyler,
					classifier.createNormalizationFieldsComboBoxModel());

			jComboBoxNormlization.setSelectedItem(rulesList
					.getNormalizer_field_name());

			jComboBoxNormlization.addItem(null); // means "no normalization"
			jComboBoxNormlization
					.addItemListener(new java.awt.event.ItemListener() {
						public void itemStateChanged(
								final java.awt.event.ItemEvent e) {
							if (e.getStateChange() == ItemEvent.SELECTED) {

								if (QuantitiesClassification.NORMALIZE_NULL_VALUE_IN_COMBOBOX == e
										.getItem())
									classifier.setNormalizer_field_name(null);
								else
									classifier
											.setNormalizer_field_name((String) e
													.getItem());

								// When the normalizationField has been changed
								// by the user, throw away the ruleTitles
								rulesList.getRuleTitles().clear();

							}
						}
					});
			ASUtil.addMouseWheelForCombobox(jComboBoxNormlization);
		}
		return jComboBoxNormlization;
	}

	/**
	 * This method initializes jTable
	 */
	private JTable getJTableClasses() {
		if (jTable == null) {
			jTable = new JTable(getTableModel());

			jTable
					.setDefaultRenderer(Color.class,
							new ColorTableCellRenderer());

			((JLabel) jTable.getDefaultRenderer(String.class))
					.setHorizontalAlignment(SwingConstants.RIGHT);

			/*******************************************************************
			 * Listening to clicks on the JTable.
			 */
			jTable.addMouseListener(new MouseAdapter() {

				private TranslationAskJDialog ask;

				@Override
				public void mouseClicked(final MouseEvent e) {

					if (e.getClickCount() == 1) {
						final int col = jTable.columnAtPoint(e.getPoint());
						final int row = jTable.rowAtPoint(e.getPoint());

						if (col == 0) {
							// Click on the color field => Manually change the
							// color.
							final Color oldColor = rulesList.getColors()[row];
							final Color newColor = ASUtil.showColorChooser(
									GraduatedColorQuantitiesGUI.this, "",
									oldColor);

							if (newColor != oldColor) {
								rulesList.getColors()[row] = newColor;
								rulesList.fireEvents(new RuleChangedEvent(
										"Manually changed a color", rulesList));
							}

						} else if (col == 1) {
							JOptionPane
									.showMessageDialog(
											SwingUtil
													.getParentWindowComponent(GraduatedColorQuantitiesGUI.this),
											AtlasStyler
													.R(
															"GraduatedColorQuantities.ClassesTable.ClickLimits.Message",
															AtlasStyler
																	.R("GraduatedColorQuantities.Classify.Button")));
							return;
						}

						if (col != 2)
							return;

						/**
						 * If its a right mouse click, we open a context menu
						 * which allows to reset all labels to default.
						 */
						if (e.getButton() == MouseEvent.BUTTON3) {
							final JPopupMenu toolPopup = new JPopupMenu();
							toolPopup
									.add(new JMenuItem(
											new AbstractAction(
													AtlasStyler
															.R("GraduatedColorQuantities.ClassesTable.PopupMenuCommand.ResetLabels")) {

												public void actionPerformed(
														final ActionEvent e) {
													rulesList
															.setClassLimits(
																	rulesList
																			.getClassLimits(),
																	true);
													jTable.repaint();
												}

											}));
							toolPopup.show(jTable, e.getX(), e.getY());
							return;
						}

						final String ruleTitle = (String) rulesList
								.getRuleTitles().get(row);

						if (AtlasStyler.getLanguageMode() == AtlasStyler.LANGUAGE_MODE.ATLAS_MULTILANGUAGE) {
							final Translation translation = new Translation(
									ruleTitle);

							if (ask == null) {

								final TranslationEditJPanel transLabel = new TranslationEditJPanel(
										AtlasStyler
												.R(
														"GraduatedColorsQuant.translate_label_for_classN",
														(row + 1)),
										translation, AtlasStyler.getLanguages());

								ask = new TranslationAskJDialog(
										GraduatedColorQuantitiesGUI.this,
										transLabel);
								ask
										.addPropertyChangeListener(new PropertyChangeListener() {

											public void propertyChange(
													final PropertyChangeEvent evt) {
												if (evt
														.getPropertyName()
														.equals(
																TranslationAskJDialog.PROPERTY_CANCEL_AND_CLOSE)) {
													ask = null;
												}
												if (evt
														.getPropertyName()
														.equals(
																TranslationAskJDialog.PROPERTY_APPLY_AND_CLOSE)) {
													rulesList
															.getRuleTitles()
															.put(
																	row,
																	translation
																			.toOneLine());
												}
												ask = null;
												rulesList
														.fireEvents(new RuleChangedEvent(
																"Legend Label changed",
																rulesList));
											}

										});

							}
							ask.setVisible(true);
						} else {
							/***************************************************
							 * Simple OGC conform labels.. not in multi-language
							 * mode
							 */
							final String newTitle = ASUtil
									.askForString(
											GraduatedColorQuantitiesGUI.this,
											ruleTitle,
											AtlasStyler
													.R(
															"GraduatedColorsQuant.translate_label_for_classN",
															(row + 1)));

							if (newTitle != null) {
								rulesList.getRuleTitles().put(row, newTitle);

								rulesList.fireEvents(new RuleChangedEvent(
										"Legend Label changed", rulesList));
							}
						}
					}
				}
			});

			classifier.addListener(new ClassificationChangedAdapter() {

				@Override
				public void classifierAvailableNewClasses(
						final ClassificationChangeEvent e) {
					jTable.setEnabled(true);
					getTableModel().fireTableStructureChanged();
				}

				@Override
				public void classifierCalculatingStatistics(
						final ClassificationChangeEvent e) {

					jTable.setEnabled(false);

				}

			});

			jTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
			jTable.getTableHeader().setResizingAllowed(true);
			final TableColumn col = jTable.getColumnModel().getColumn(0);
			col.setMinWidth(40);
			col.setMaxWidth(40);
			col.setPreferredWidth(40);

		}
		return jTable;
	}

	public DefaultTableModel getTableModel() {
		if (tableModel == null) {

			tableModel = new DefaultTableModel() {

				@Override
				public Class<?> getColumnClass(final int columnIndex) {
					if (columnIndex == 0) // Colors
						return Color.class;

					if (columnIndex == 1) // Limits
						return String.class;

					if (columnIndex == 2) // Label
						return String.class;

					return null;
				}

				@Override
				public int getColumnCount() {
					return 3;
				}

				@Override
				public String getColumnName(final int columnIndex) {
					if (columnIndex == 0)
						return AtlasStyler
								.R("GraduatedColorQuantities.Column.Color");
					if (columnIndex == 1)
						return AtlasStyler
								.R("GraduatedColorQuantities.Column.Limits");
					if (columnIndex == 2)
						return AtlasStyler
								.R("GraduatedColorQuantities.Column.Label");
					return super.getColumnName(columnIndex);
				}

				@Override
				public int getRowCount() {
					final int numClasses = rulesList.getNumClasses();
					return numClasses >= 0 ? numClasses : 0;
				}

				@Override
				public Object getValueAt(final int rowIndex,
						final int columnIndex) {

					/***********************************************************
					 * getValue 0 Color
					 */
					if (columnIndex == 0) { // Color
						return rulesList.getColors()[rowIndex];
					}

					/***********************************************************
					 * getValue 1 Limit
					 */
					if (columnIndex == 1) { // Limits

						final ArrayList<Double> classLimitsAsArrayList = rulesList
								.getClassLimitsAsArrayList();

						if (classLimitsAsArrayList.size() < getRowCount()) {
							return "-";
						}

						final Number lower = (Number) classLimitsAsArrayList
								.get(rowIndex);
						final Number upper = (Number) classLimitsAsArrayList
								.get(rowIndex + 1);

						final String limitsLabel = ASUtil.df.format(lower)
								+ " -> " + ASUtil.df.format(upper);
						return limitsLabel;
					}

					/***********************************************************
					 * getValue 2 Label
					 */
					if (columnIndex == 2) { // Label

						String string = (String) rulesList.getRuleTitles().get(
								rowIndex);

						if (AtlasStyler.getLanguageMode() == AtlasStyler.LANGUAGE_MODE.ATLAS_MULTILANGUAGE) {
							string = new Translation(string).toString();
						}

						return string;
					}

					return super.getValueAt(rowIndex, columnIndex);
				}

			};

		}
		return tableModel;
	}

	/**
	 * This method initializes jPanel3
	 * 
	 * @return javax.swing.JPanel
	 */
	private JPanel getJPanelColorsAndTemplate() {
		final JPanel panel = new JPanel(new MigLayout("width 100%", "grow"));

		jLabelTemplate = new JLabel(AtlasStyler
				.R("GraduatedColorQuantities.Template"));
		jLabelTemplate.setToolTipText(AtlasStyler
				.R("GraduatedColorQuantities.Template.TT"));

		final JLabel jLabelColorPalette = new JLabel(AtlasStyler
				.R("GraduatedColorQuantities.ColorRamp"));

		panel.add(jLabelColorPalette, "left");
		panel.add(getJComboBoxColors(), "left");
		panel.add(getInvertColorsButton(), "left");

		panel.add(new JPanel(), "growx");
		panel.add(jLabelTemplate, "gapx unrelated, right");
		panel.add(getJButtonTemplate(), "right");

		return panel;
	}

	/**
	 * A button to invert the order of the applied colors
	 * 
	 * @return
	 */
	private JButton getInvertColorsButton() {
		final JButton button = new JButton(new AbstractAction("",
				new ImageIcon(getClass().getResource(
						"images/reverseColorOrder.gif"))) {

			@Override
			public void actionPerformed(final ActionEvent e) {
				final Color[] colors = rulesList.getColors();
				final List<Color> asList = Arrays.asList(colors);
				Collections.reverse(asList);
				getTableModel().fireTableDataChanged();
				rulesList.fireEvents(new RuleChangedEvent("Colors changed",
						rulesList));
			}

		});

		button.setMargin(new Insets(1, 1, 1, 1));

		// button.setBorder( BorderFactory.createEmptyBorder(1,1,1,1));

		button.setToolTipText(AtlasStyler
				.R("GraduatedColorQuantities.ColorRamp.RevertButton.TT"));

		return button;
	}

	/**
	 * This method initializes jComboBox
	 * 
	 * @return javax.swing.JComboBox
	 */
	private JComboBox getJComboBoxColors() {

		if (jComboBoxPalettes == null) {

			final DefaultComboBoxModel aModel = new DefaultComboBoxModel(ASUtil
					.getPalettes(new PaletteType(true, false), classifier
							.getNumClasses()));
			aModel.setSelectedItem(rulesList.getBrewerPalette());

			jComboBoxPalettes = new JComboBox(aModel);

			jComboBoxPalettes.setRenderer(new PaletteCellRenderer());

			jComboBoxPalettes.addItemListener(new ItemListener() {

				public void itemStateChanged(final ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						rulesList
								.setBrewerPalette((BrewerPalette) (e.getItem()));
						getTableModel().fireTableDataChanged();
					}
				}

			});

			classifier.addListener(new ClassificationChangedAdapter() {

				/**
				 * Create a new list of BrewerPalettes depending on the number
				 * of classes. Reselect the old selection if it is still
				 * contained in the new list *
				 */
				@Override
				public void classifierAvailableNewClasses(
						final ClassificationChangeEvent e) {
					final BrewerPalette oldSelection = (BrewerPalette) jComboBoxPalettes
							.getSelectedItem();

					final BrewerPalette[] palettes = ASUtil.getPalettes(
							new PaletteType(true, false), classifier
									.getNumClasses());
					jComboBoxPalettes.setModel(new DefaultComboBoxModel(
							palettes));

					if (oldSelection != null)
						for (final BrewerPalette bp : palettes) {
							if (bp.getDescription().equals(
									oldSelection.getDescription())) {
								jComboBoxPalettes.setSelectedItem(bp);
								break;
							}
						}

					rulesList
							.setBrewerPalette((BrewerPalette) jComboBoxPalettes
									.getSelectedItem());
				}

			});

			ASUtil.addMouseWheelForCombobox(jComboBoxPalettes, false);
		}
		return jComboBoxPalettes;
	}

	/**
	 * Listens to close/cancel of any {@link SymbolSelectorGUI}.
	 */
	PropertyChangeListener listenCancelOkForSelectionInSymbolSelectionGUI = new PropertyChangeListener() {

		public void propertyChange(final PropertyChangeEvent evt) {

			if (evt.getPropertyName().equals(
					SymbolSelectorGUI.PROPERTY_CANCEL_CHANGES)) {

				backup.copyTo(rulesList.getTemplate());
			}

			if (evt.getPropertyName().equals(SymbolSelectorGUI.PROPERTY_CLOSED)) {

			}
		}
	};

	/**
	 * A backup of the template symbol. Used when the GUI opens.
	 */
	protected SingleRuleList<? extends Symbolizer> backup;

	/**
	 * Listens to realtime modifications of the template icon and updates the
	 * preview
	 */
	RuleChangeListener listenerUpdatePreviewIconOnTemplateChange = new RuleChangeListener() {

		public void changed(final RuleChangedEvent e) {

			// LOGGER.debug("reason = " + e.toString());
			rulesList.setTemplate(rulesList.getTemplate());

			jButtonTemplate.setIcon(new ImageIcon(rulesList.getTemplate()
					.getImage(ICON_SIZE)));
		}

	};

	/**
	 * This method initializes jButton
	 * 
	 * @return javax.swing.JButton
	 */
	private JButton getJButtonTemplate() {
		if (jButtonTemplate == null) {
			jButtonTemplate = new JButton();

			final ImageIcon imageIcon = new ImageIcon(rulesList.getTemplate()
					.getImage(ICON_SIZE));
			jButtonTemplate.setAction(new AbstractAction("", imageIcon) {

				public void actionPerformed(final ActionEvent e) {

					final SingleRuleList<? extends Symbolizer> template = rulesList
							.getTemplate();
					template.getListeners().clear();

					backup = template.copy();

					final SymbolSelectorGUI gui = new SymbolSelectorGUI(
							GraduatedColorQuantitiesGUI.this, AtlasStyler
									.R("SymbolSelector.ForTemplate.Title"),
							template);

					gui
							.addPropertyChangeListener(listenCancelOkForSelectionInSymbolSelectionGUI);

					template
							.addListener(listenerUpdatePreviewIconOnTemplateChange);

					gui.setModal(true);
					gui.setVisible(true);
				}

			});

		}
		return jButtonTemplate;
	}

	public void dispose() {
		if (quantGUI != null) {
			quantGUI.dispose();
			quantGUI = null;
		}
		if (atlasStyler != null) {
			if (atlasStyler.getMapLayer() != null)
				atlasStyler.getMapLayer().removeMapLayerListener(
						listenToFilterChangesAndRecalcStatistics);
		}
	}

}
