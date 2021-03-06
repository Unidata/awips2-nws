/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 */
package gov.noaa.nws.ocp.viz.psh.ui.generator.tab.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import gov.noaa.nws.ocp.common.dataplugin.psh.NonMetarDataEntry;
import gov.noaa.nws.ocp.common.dataplugin.psh.StormDataEntry;
import gov.noaa.nws.ocp.viz.psh.ui.generator.tab.PshTabComp;

/**
 * Sortable table for NonMetar tabs
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 22, 2017 #40299      wpaintsil   Initial creation.
 * 
 * </pre>
 * 
 * @author wpaintsil
 * @version 1.0
 *
 */
public class PshNonMetarTable extends PshSortableTable {

    /**
     * Constructor
     * 
     * @param tab
     * @param columns
     */
    public PshNonMetarTable(PshTabComp tab, PshTableColumn[] columns) {
        super(tab, columns);
    }

    /**
     * Add SelectionListeners for column sorting.
     */
    @Override
    public void addColumnSelectionListeners() {
        for (TableColumn column : table.getColumns()) {
            switch (column.getText()) {
            case PshTabComp.PRESSURE_LBL_STRING:
            case PshTabComp.SUST_WIND_LBL_STRING:
            case PshTabComp.PK_WIND_LBL_STRING:
                addColumnSelectionListener(column);
                break;
            }

        }
    }

    /**
     * Sort the table entries.
     * 
     * @param columnName
     * @param descending
     *            true if sorting in descending order; false if sorting in
     *            ascending order
     */
    @Override
    protected void sortData(String columnName, boolean descending) {
        if (tableData != null && !tableData.isEmpty()) {

            List<NonMetarDataEntry> dataList = new ArrayList<>();

            for (StormDataEntry data : tableData.values()) {
                dataList.add((NonMetarDataEntry) data);
            }
            int compareReturn = descending ? -1 : 1;

            switch (columnName) {
            case PshTabComp.PRESSURE_LBL_STRING:
                Collections.sort(dataList, new Comparator<NonMetarDataEntry>() {
                    @Override
                    public int compare(NonMetarDataEntry o1,
                            NonMetarDataEntry o2) {

                        float pressure1 = 0;
                        if (!o1.getMinSeaLevelPres().isEmpty()) {
                            try {
                                pressure1 = Float
                                        .parseFloat(o1.getMinSeaLevelPres());
                            } catch (NumberFormatException e) {
                                logger.warn("Could not parse '"
                                        + o1.getMinSeaLevelPres()
                                        + "' to float.");
                            }
                        }
                        float pressure2 = 0;
                        if (!o2.getMinSeaLevelPres().isEmpty()) {
                            try {
                                pressure2 = Float
                                        .parseFloat(o2.getMinSeaLevelPres());
                            } catch (NumberFormatException e) {
                                logger.warn("Could not parse '"
                                        + o2.getMinSeaLevelPres()
                                        + "' to float.");
                            }
                        }
                        if (pressure1 > pressure2) {
                            return 1 * compareReturn;
                        } else if (pressure1 < pressure2) {
                            return -1 * compareReturn;
                        } else {
                            return 0;
                        }
                    }
                });

                break;
            case PshTabComp.SUST_WIND_LBL_STRING:
                Collections.sort(dataList, new Comparator<NonMetarDataEntry>() {
                    @Override
                    public int compare(NonMetarDataEntry o1,
                            NonMetarDataEntry o2) {

                        int sustSpeed1 = 0;
                        if (o1.getSustWind().length() > 4) {
                            try {
                                sustSpeed1 = Integer.parseInt(
                                        o1.getSustWind().substring(4));
                            } catch (NumberFormatException e) {
                                logger.warn("Could not parse '"
                                        + o1.getSustWind().substring(4)
                                        + "' to int.");
                            }
                        }
                        int sustSpeed2 = 0;
                        if (o2.getSustWind().length() > 4) {
                            try {
                                sustSpeed2 = Integer.parseInt(
                                        o2.getSustWind().substring(4));
                            } catch (NumberFormatException e) {
                                logger.warn("Could not parse '"
                                        + o2.getSustWind().substring(4)
                                        + "' to int.");
                            }
                        }
                        if (sustSpeed1 > sustSpeed2) {
                            return 1 * compareReturn;
                        } else if (sustSpeed1 < sustSpeed2) {
                            return -1 * compareReturn;
                        } else {
                            return 0;
                        }
                    }
                });
                break;
            case PshTabComp.PK_WIND_LBL_STRING:
                Collections.sort(dataList, new Comparator<NonMetarDataEntry>() {
                    @Override
                    public int compare(NonMetarDataEntry o1,
                            NonMetarDataEntry o2) {

                        int peakSpeed1 = 0;
                        if (o1.getPeakWind().length() > 4) {
                            try {
                                peakSpeed1 = Integer.parseInt(
                                        o1.getPeakWind().substring(4));
                            } catch (NumberFormatException e) {
                                logger.warn("Could not parse '"
                                        + o1.getPeakWind().substring(4)
                                        + "' to int.");
                            }
                        }
                        int peakSpeed2 = 0;
                        if (o2.getPeakWind().length() > 4) {
                            try {
                                peakSpeed2 = Integer.parseInt(
                                        o2.getPeakWind().substring(4));
                            } catch (NumberFormatException e) {
                                logger.warn("Could not parse '"
                                        + o2.getPeakWind().substring(4)
                                        + "' to int.");
                            }
                        }
                        if (peakSpeed1 > peakSpeed2) {
                            return 1 * compareReturn;
                        } else if (peakSpeed1 < peakSpeed2) {
                            return -1 * compareReturn;
                        } else {
                            return 0;
                        }
                    }
                });
                break;
            }

            tableData.clear();
            table.removeAll();
            for (NonMetarDataEntry entry : dataList) {
                addItem(entry);
            }
            tab.updatePreviewArea();
        }
    }

    @Override
    public void addItem(StormDataEntry data, int tableIndex) {
        TableItem newItem;

        if (tableIndex > -1) {
            newItem = new TableItem(table, SWT.NONE, tableIndex);
        } else {
            newItem = new TableItem(table, SWT.NONE);
        }

        newItem.setText(0, ((NonMetarDataEntry) data).getSite());
        newItem.setText(1, String.valueOf(((NonMetarDataEntry) data).getLat()));
        newItem.setText(2, String.valueOf(((NonMetarDataEntry) data).getLon()));
        newItem.setText(3, ((NonMetarDataEntry) data).getMinSeaLevelPres());
        newItem.setText(4, ((NonMetarDataEntry) data).getMinSeaLevelPresTime());
        newItem.setText(5, String
                .valueOf(((NonMetarDataEntry) data).getMinSeaLevelComplete()));
        newItem.setText(6, ((NonMetarDataEntry) data).getSustWind());
        newItem.setText(7, ((NonMetarDataEntry) data).getSustWindTime());
        newItem.setText(8, String
                .valueOf(((NonMetarDataEntry) data).getSustWindComplete()));
        newItem.setText(9, ((NonMetarDataEntry) data).getPeakWind());
        newItem.setText(10, ((NonMetarDataEntry) data).getPeakWindTime());
        newItem.setText(11, String
                .valueOf(((NonMetarDataEntry) data).getPeakWindComplete()));
        newItem.setText(12,
                String.valueOf(((NonMetarDataEntry) data).getEstWind()));
        newItem.setText(13, ((NonMetarDataEntry) data).getAnemHgmt());

        insertTableData(newItem, data, tableIndex);
    }

}
