/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 */
package gov.noaa.nws.ocp.viz.psh.ui.generator.tab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TabFolder;

import gov.noaa.nws.ocp.common.dataplugin.psh.PshData;
import gov.noaa.nws.ocp.common.dataplugin.psh.PshDataCategory;
import gov.noaa.nws.ocp.common.dataplugin.psh.StormData;
import gov.noaa.nws.ocp.common.dataplugin.psh.StormDataEntry;
import gov.noaa.nws.ocp.common.dataplugin.psh.WaterLevelDataEntry;
import gov.noaa.nws.ocp.common.localization.psh.PshCity;
import gov.noaa.nws.ocp.common.localization.psh.PshConfigurationManager;
import gov.noaa.nws.ocp.viz.psh.PshUtil;
import gov.noaa.nws.ocp.viz.psh.ui.generator.IPshData;
import gov.noaa.nws.ocp.viz.psh.ui.generator.tab.table.PshControlType;
import gov.noaa.nws.ocp.viz.psh.ui.generator.tab.table.PshTableColumn;
import gov.noaa.nws.ocp.viz.psh.ui.generator.tab.table.PshWaterLevelTable;

/**
 * Composite for the maximum water level tab.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 21, 2017 #34810      wpaintsil   Initial creation.
 * Nov,08  2017 #40423      jwu         Replace tide/surge with water level.
 * Nov,14  2017 #40426      jwu         Update GUI with water level.
 * 
 * </pre>
 * 
 * @author wpaintsil
 * @version 1.0
 *
 */
public class PshWaterLevelTabComp extends PshTabComp {
    public static final String WATER_LVL_LBL_STRING = "Water\nLevel";

    public PshWaterLevelTabComp(IPshData psh, TabFolder parent) {
        super(psh, parent, PshDataCategory.WATER_LEVEL);
    }

    @Override
    public void createControls() {
        SashForm verticalSashForm = new SashForm(mainComposite, SWT.VERTICAL);

        Group dataComp = new Group(verticalSashForm, SWT.SHADOW_IN);
        dataComp.setText(tabType.getDesc());
        GridLayout dataLayout = new GridLayout(1, false);
        dataComp.setLayout(dataLayout);
        dataComp.setLayoutData(
                new GridData(SWT.CENTER, SWT.CENTER, false, false));

        List<String> waterLevelDatumList = Arrays
                .asList(new String[] { "MHHW", "NAVD88", "MSL", "AGL", "N/A" });

        List<String> waterLevelSourceList = Arrays
                .asList(new String[] { "NOS", "USGS", "USCOE", "OTHER" });

        List<String> comboList = new ArrayList<>();
        List<PshCity> gaugeStations = PshConfigurationManager.getInstance()
                .getCities().getTideGaugeStations();

        if (gaugeStations == null || gaugeStations.isEmpty()) {
            comboList.add("[empty]");
        } else {
            for (PshCity station : gaugeStations) {
                comboList.add(station.getName());
            }
        }

        table = new PshWaterLevelTable(this, new PshTableColumn[] {
                new PshTableColumn("Location", CITY_WIDTH,
                        PshControlType.NONEMPTY_COMBO, comboList),
                new PshTableColumn("ID", 65, PshControlType.TEXT),
                new PshTableColumn(COUNTY_LBL_STRING, COUNTY_WIDTH,
                        PshControlType.TEXT),
                new PshTableColumn("State", 50, PshControlType.TEXT),
                new PshTableColumn("Lat", 75, PshControlType.NUMBER_TEXT),
                new PshTableColumn("Lon", 80, PshControlType.NUMBER_TEXT),
                new PshTableColumn(WATER_LVL_LBL_STRING, 58,
                        PshControlType.NUMBER_TEXT),
                new PshTableColumn("Datum", 100, PshControlType.NONEMPTY_COMBO,
                        waterLevelDatumList),
                new PshTableColumn(DATE_LBL_STRING, DATETIME_WIDTH,
                        PshControlType.DATETIME_TEXT),
                new PshTableColumn("Source", 92, PshControlType.NONEMPTY_COMBO,
                        waterLevelSourceList),
                new PshTableColumn(INCOMPLETE, INCOMPLETE_WIDTH,
                        PshControlType.CHECKBOX) });

        table.createTopControls(dataComp);
        table.createTable(dataComp);
        table.createBottomControls(dataComp);
        ((PshWaterLevelTable) table).addColumnSelectionListeners();

        GridData sashData = new GridData(SWT.CENTER, SWT.FILL, false, true);
        sashData.heightHint = 500;
        verticalSashForm.setLayoutData(sashData);

        createRemarksArea(verticalSashForm, false, true, "Remarks");

    }

    @Override
    public void setDataList() {
        List<WaterLevelDataEntry> surgeDataList = pshGeneratorData.getPshData()
                .getWaterLevel().getData();

        if (surgeDataList == null || surgeDataList.isEmpty()) {
            emptyData = true;
        } else {
            for (WaterLevelDataEntry dataItem : surgeDataList) {
                addItem(dataItem);
            }
        }
        String remarks = pshGeneratorData.getPshData().getWaterLevel()
                .getRemarks();
        if (!remarks.equals(StormData.NO_REMARKS)) {
            setRemarksText(remarks);
        }

        updatePreviewArea();
    }

    @Override
    public void savePshData(List<StormDataEntry> entries) {
        if ((entries == null || entries.isEmpty()) && emptyData) {
            // do nothing if the was no data and we're trying to save without
            // entering any new data
        } else {
            List<WaterLevelDataEntry> surgeDataList = new ArrayList<>();

            if (entries != null && !entries.isEmpty()) {
                emptyData = false;

                for (StormDataEntry entry : entries) {
                    surgeDataList.add((WaterLevelDataEntry) entry);
                }
            }
            PshData pshData = pshGeneratorData.getPshData();
            pshData.getWaterLevel().setRemarks(getRemarksText());
            pshData.getWaterLevel().setData(surgeDataList);

            saveAlert(PshUtil.savePshData(pshData));

            pshGeneratorData.setPshData(pshData);
        }
    }

    @Override
    public void updatePreviewArea() {
        PshData tempData = new PshData();
        tempData.getWaterLevel()
                .setData(table.getTableData(WaterLevelDataEntry.class));

        previewText.setText(
                PshUtil.buildPshPreview(tempData, PshDataCategory.WATER_LEVEL));

    }

    @Override
    public void saveFinalRemarks() {
        List<WaterLevelDataEntry> entries = pshGeneratorData.getPshData()
                .getWaterLevel().getData();
        List<WaterLevelDataEntry> dataList = new ArrayList<>(entries);

        PshData pshData = pshGeneratorData.getPshData();
        pshData.getWaterLevel().setRemarks(getRemarksText());
        pshData.getWaterLevel().setData(dataList);

        saveAlert(PshUtil.savePshData(pshData));

        pshGeneratorData.setPshData(pshData);

    }

}
