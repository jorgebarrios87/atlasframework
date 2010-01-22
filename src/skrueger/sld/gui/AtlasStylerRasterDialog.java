package skrueger.sld.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import net.miginfocom.swing.MigLayout;

import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Style;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;

import schmitzm.geotools.gui.ColorMapPanel;
import schmitzm.geotools.styling.StylingUtil;
import schmitzm.swing.JPanel;
import skrueger.RasterLegendData;
import skrueger.atlas.AtlasConfig;
import skrueger.atlas.gui.internal.LayoutUtil;
import skrueger.geotools.StyledRasterInterface;
import skrueger.i8n.Translation;
import skrueger.sld.AtlasStyler;
import skrueger.sld.StyleChangeListener;
import skrueger.sld.StyleChangedEvent;
import skrueger.swing.CancelButton;
import skrueger.swing.CancellableDialogAdapter;
import skrueger.swing.OkButton;

/**
 * A very basic SLD Editor for rasters.
 */
public class AtlasStylerRasterDialog extends CancellableDialogAdapter {

	final private RasterSymbolizer rasterSymbolizer;
	private ColorMapPanel colorMapPanel;
	private Set<StyleChangeListener> listeners = new TreeSet<StyleChangeListener>();
	private JPanel rasterLegendDataPanel;
	private JTable rasterLegendDataTable;
	private DefaultTableModel rasterLegendDataTableModel;
	
	final private RasterLegendData legendData;
	private final AtlasConfig ace;
	private final Style workingStyle;
	private final Style backupStyle;
	private final RasterLegendData backupLegendData;
	private final StyledRasterInterface<?> styledRaster;

	/**
	 * Add an {@link ActionListener} that will be called whenever the
	 * {@link Style} is applied.
	 */
	public void addListener(StyleChangeListener listener) {
		listeners.add(listener);
	}

	public AtlasStylerRasterDialog(Component parentGUI,
			StyledRasterInterface<?> styledRaster, AtlasConfig ac) {
		super(parentGUI);
		this.styledRaster = styledRaster;
		// this.styledRaster = styledRaster;
		this.ace = ac;

		// backup!
		{

			DuplicatingStyleVisitor dsv = new DuplicatingStyleVisitor();
			dsv.visit(styledRaster.getStyle());
			workingStyle = (Style) dsv.getCopy();
			dsv.visit(styledRaster.getStyle());
			backupStyle = (Style) dsv.getCopy();

			this.legendData = styledRaster.getLegendMetaData();
			backupLegendData = legendData.copy();
		}

		rasterSymbolizer = StylingUtil.getRasterSymbolizers(workingStyle)
				.get(0);

		// hardcode, but so many styles come with a wrong geometry property
		// name...
		rasterSymbolizer.setGeometryPropertyName("geom"); // Do we still need
															// this? No! It's
															// done in getStyle
															// i think

		// try{
		// StylingUtil.saveStyleToSLD(workingStyle, new
		// java.io.File("/home/stefan/Desktop/preOpenRasterStyler.sld"));
		// } catch (Exception e) {
		// }

		initGUI();
	}

	private void initGUI() {
		JPanel contentPane = new JPanel(new MigLayout("wrap 1, fillx"));

		contentPane.add(getColorMapPanel() , "grow, height 200");
		contentPane.add(getRasterLegendDataPanel(), "grow, height 200");

		contentPane.add(getApplyButton(), "left, split 3, tag apply"); //$NON-NLS-1$
		contentPane.add(getOkButton(), "left, tag ok"); //$NON-NLS-1$
		contentPane.add(getCancelButton(), "left, tag cancel"); //$NON-NLS-1$

		setContentPane(contentPane);

		pack();

	}

	private ColorMapPanel getColorMapPanel() {
		if (colorMapPanel == null) {
			colorMapPanel = new ColorMapPanel(rasterSymbolizer.getColorMap());
//			SwingUtil.setPreferredHeight(colorMapPanel, 300);
		}
		return colorMapPanel;
	}

	private JButton getCancelButton() {
		CancelButton cancelButton = new CancelButton();
		cancelButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				cancel();
				dispose();
			}

		});
		return cancelButton;
	}

	private JButton getOkButton() {
		OkButton okButton = new OkButton();
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				apply();
				dispose();
			}

		});
		return okButton;
	}

	private JButton getApplyButton() {
		JButton okButton = new JButton("Apply");
		okButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				apply();
			}

		});
		return okButton;
	}

	@Override
	public void cancel() {
		// rasterSymbolizer.setColorMap(backupColorMap);

		for (StyleChangeListener al : listeners) {
			al.changed(new StyleChangedEvent(backupStyle));
		}

		backupLegendData.copyTo( styledRaster.getLegendMetaData() );
	}

	protected void apply() {
		rasterSymbolizer.setColorMap(getColorMapPanel().getColorMap());

		for (StyleChangeListener al : listeners) {
			al.changed(new StyleChangedEvent(workingStyle));
		}

		try {
			StylingUtil.saveStyleToSLD(workingStyle, new java.io.File(
					"/home/stefan/Desktop/postOpenRasterStyler.sld"));
		} catch (Exception e) {
		}

	}

	@Override
	public boolean okClose() {
		apply();
		return super.okClose();
	}

	@Override
	public void dispose() {
		listeners.clear();
		super.dispose();
	}

	private JTable getRasterLegendDataTable() {
		if (rasterLegendDataTable == null) {

			rasterLegendDataTable = new JTable();
			rasterLegendDataTable.setModel(getRasterLegendTableModel());
			rasterLegendDataTable.setColumnSelectionAllowed(false);
			rasterLegendDataTable.getSelectionModel().setSelectionMode(
					ListSelectionModel.SINGLE_SELECTION);

			getRasterLegendTableModel().addColumn(
					AtlasStyler.R("RasterLegendPanel.Table.Column.Value"));
			rasterLegendDataTable.getColumnModel().getColumn(0).setHeaderValue(
					AtlasStyler.R("RasterLegendPanel.Table.Column.Value"));

			int idx = 0;
			for (String code : ace.getLanguages()) {
				idx++;
				getRasterLegendTableModel().addColumn(code);
				rasterLegendDataTable.getColumnModel().getColumn(idx)
						.setHeaderValue(code); // lang name in locale is nicer
			}
			
//			SwingUtil.setPreferredHeight(rasterLegendDataPanel, 300);
		}

		return rasterLegendDataTable;

	}

	private DefaultTableModel getRasterLegendTableModel() {

		if (rasterLegendDataTableModel == null) {

			// ****************************************************************************
			// Preparing the Table and it's model
			// ****************************************************************************
			rasterLegendDataTableModel = new DefaultTableModel() {

				@Override
				public Object getValueAt(int row, int column) {
					final Double key = legendData.getSortedKeys().get(row);
					if (column == 0) {
						return key;
					}

					final String langCode = ace.getLanguages().get(column - 1);
					final Translation translation = legendData.get(key);
					String string = translation.get(langCode);
					if (string == null) {
						string = "";
						translation.put(langCode, string);
					}
					return string.toString();
				};

				@Override
				public int getRowCount() {
					return legendData.getSortedKeys().size();
				};

				@Override
				public void setValueAt(Object value, int row, int column) {
					final Double key = legendData.getSortedKeys().get(row);
					Translation translation = legendData.get(key);
					if (column == 0) {
						legendData.remove(key);
						legendData.put((Double) value, translation);
					} else {
						translation.put(ace.getLanguages().get(column - 1),
								(String) value);
						legendData.put(key, translation);
					}

					getRasterLegendTableModel().fireTableStructureChanged();
					apply();
				};

				// This method returns the Class object of the first
				// cell in specified column in the table model.
				// Unless this method is overridden, all values are
				// assumed to be the type Object.
				@Override
				public Class<?> getColumnClass(int columnIndex) {
					if (columnIndex > 0)
						return String.class;
					else
						return Double.class;
				}

				@Override
				public boolean isCellEditable(int row, int column) {
					return true;
				}
			};
		}

		return rasterLegendDataTableModel;
	}

	/**
	 * @return A {@link JPanel} allowing to deinf the legend for a raster
	 */
	JPanel getRasterLegendDataPanel() {

		if (rasterLegendDataPanel == null) {

			// ****************************************************************************
			//
			// Preparing the Buttons
			//
			// ****************************************************************************
			JPanel buttons = new JPanel();

			// ****************************************************************************
			// Delete a legend entry
			// ****************************************************************************
			JButton delete = new JButton(new AbstractAction(AtlasStyler
					.R("RasterLegendPanel.Button.Remove")) {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (rasterLegendDataTable.getSelectedRow() < 0)
						return;

					// TODO sicher?
					Double key = legendData.getSortedKeys().get(
							rasterLegendDataTable.getSelectedRow());
					legendData.remove(key);
					getRasterLegendTableModel().fireTableStructureChanged();
					apply();
				}

			});

			// ****************************************************************************
			// Switch the gaps boolean
			// ****************************************************************************
			JToggleButton gaps = new JToggleButton(new AbstractAction(
					AtlasStyler.R("RasterLegendPanel.Button.GapsOnOff")) {
				@Override
				public void actionPerformed(ActionEvent e) {
					legendData.setPaintGaps(!legendData.isPaintGaps());
					apply();
				}
			});
			gaps.setSelected(legendData.isPaintGaps());

			// ****************************************************************************
			// Add a legend entry
			// ****************************************************************************
			JButton add = new JButton(new AbstractAction(AtlasStyler
					.R("RasterLegendPanel.Button.InsertNew")) {

				@Override
				public void actionPerformed(ActionEvent e) {
					if (legendData.getSortedKeys().size() > 0)
						legendData.put(legendData.getSortedKeys().get(
								legendData.getSortedKeys().size() - 1) + 1,
								new Translation());
					else
						legendData.put(0., new Translation());
					getRasterLegendTableModel().fireTableStructureChanged();
					apply();
				}
			});

			buttons.add(add);
			buttons.add(delete);
			buttons.add(gaps);

			// ****************************************************************************
			// The JTable is in the CENTER, the buttons ar SOUTH all on the left
			// side
			// ****************************************************************************
			rasterLegendDataPanel = new JPanel(new BorderLayout());
			rasterLegendDataPanel.add(new JScrollPane(
					getRasterLegendDataTable()), BorderLayout.CENTER);
			rasterLegendDataPanel.add(buttons, BorderLayout.SOUTH);
			LayoutUtil.borderTitle(rasterLegendDataPanel, AtlasStyler
					.R("RasterLegendPanel.Border.Title"));
		}
		return rasterLegendDataPanel;
	}

}
