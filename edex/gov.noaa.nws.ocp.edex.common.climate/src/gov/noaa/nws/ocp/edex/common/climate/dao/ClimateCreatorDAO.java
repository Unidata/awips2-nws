/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 **/
package gov.noaa.nws.ocp.edex.common.climate.dao;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.time.util.TimeUtil;

import gov.noaa.nws.ocp.common.dataplugin.climate.ClimateDate;
import gov.noaa.nws.ocp.common.dataplugin.climate.ClimateDates;
import gov.noaa.nws.ocp.common.dataplugin.climate.ClimateTime;
import gov.noaa.nws.ocp.common.dataplugin.climate.ClimateWind;
import gov.noaa.nws.ocp.common.dataplugin.climate.DailyClimateData;
import gov.noaa.nws.ocp.common.dataplugin.climate.DailyDataMethod;
import gov.noaa.nws.ocp.common.dataplugin.climate.exception.ClimateInvalidParameterException;
import gov.noaa.nws.ocp.common.dataplugin.climate.exception.ClimateQueryException;
import gov.noaa.nws.ocp.common.dataplugin.climate.parameter.ParameterBounds;
import gov.noaa.nws.ocp.common.dataplugin.climate.parameter.ParameterFormatClimate;
import gov.noaa.nws.ocp.common.dataplugin.climate.util.QCValues;
import gov.noaa.nws.ocp.edex.common.climate.util.MetarUtils;

/**
 * Implementations converted from Climate Creator-specific SUBROUTINES under
 * adapt/climate/src/create_climate that could not be generalized for other
 * modules.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 28 SEP 2016  21378      amoore      Initial creation
 * 02 NOV 2016  21378      amoore      Migrated more Climate Creator functionality.
 * 08 NOV 2016  21378      amoore      Implemented getSCDSnow.
 * 17 NOV 2016  21378      amoore      Use string builders for queries; do not use maps.
 * 03 JAN 2017  22134      amoore      Query result parsing fix.
 * 26 JAN 2017  27017      amoore      Fix time precision.
 * 01 MAR 2017  27862      amoore      Fix date and number return types.
 * 10 MAR 2017  27420      amoore      Change some exceptions to plain logging.
 * 16 MAR 2017  30162      amoore      Fix logging and exception throwing.
 * 20 MAR 2017  20632      amoore      Handle possible null DB values.
 * 23 MAR 2017  30515      amoore      Replace constants that are already defined in AWIPS.
 * 12 APR 2017  30166      amoore      Fix SQL casting bug.
 * 13 APR 2017  30166      amoore      Fix SQL casting bug.
 * 13 APR 2017  33104      amoore      Address comments from review. Casting fix.
 * 18 APR 2017  33104      amoore      Use query maps now that DB issue is fixed.
 * 03 MAY 2017  33104      amoore      Use abstract map.
 * 11 MAY 2017  33104      amoore      Fix query parameters.
 * 08 JUN 2017  33104      amoore      Touch up of logic. Remove unnecessary array elements and
 *                                     simplify weather looping. Comment on weather looping hours.
 * 15 JUN 2017  35170      amoore      Fix missing sky cover and weather elements in daily reports.
 * 15 JUN 2017  33104      amoore      Fix casting issue for weather elements.
 * 19 JUN 2017  33104      amoore      Fix casting issue for fog.
 * 20 JUN 2017  33104      amoore      Address review comments.
 * 27 JUN 2017  33104      amoore      Get rid of "'" appending/prepending on date strings for queries,
 *                                     which was causing poor retrieval.
 * 24 JUL 2017  33104      amoore      Use 24-hour time format when querying.
 * 26 JUL 2017  33104      amoore      Fix duplicated sql alias error from ATAN postgres.
 * 08 SEP 2017  37809      amoore      For queries, cast to Number rather than specific number type.
 * 04 OCT 2017  38800      amoore      Return empty string for FSS correction value if row was found but
 *                                     value is null.
 * 04 OCT 2017  38067      amoore      Fix PM/IM delay in data reports.
 * 13 DEC 2017  41565      wpaintsil   Corrected wrong date/time formats.
 * 24 OCT 2018  DR20945    wpaintsil   Include both METAR and SPECI observations 
 *                                     in the FSS_contin_real query.
 * 20 MAR 2019  DR21158    wpaintsil   Correct the SPECI queries for wx values.
 * 20 MAR 2019  DR 21189   dfriedman   Change queries to compare timestamps
 *                                     instead of formatted strings.
 * 24 APR 2019  DR 21258   dfriedman   Correct valid time ordering in getMetarCategSingle.
 * 08 AUG 2019  DR 21516   wpaintsil   'SPECI' query in buildDailyObsClimo includes an extra hour.
 * </pre>
 * 
 * @author amoore
 * @version 1.0
 */
public class ClimateCreatorDAO extends ClimateDAO {
    /*
     * Constants from build_daily_obs_weather.ec and build_daily_obs_weather.h.
     */
    /**
     * Dense fog visibility.
     */
    private static final int DENSE_FOG = 2010;

    /*
     * Legacy documentation:
     * 
     * These codes are taken from boolean_values table in hmdb database
     */
    /**
     * Thunderstorm weather element values.
     */
    private static final int[] THUNDERSTORM = { 101, 102, 103, 104, 105, 106,
            107, 108, 109, 110, 111, 112 };

    /**
     * Heavy rain weather element values.
     */
    private static final int[] HEAVY_RAIN = { 22, 23, 107 };

    /**
     * Rain weather element values.
     */
    private static final int[] RAIN = { 12, 13, 104 };

    /**
     * Light rain weather element values.
     */
    private static final int[] LIGHT_RAIN = { 1, 2, 3, 11, 21, 101 };

    /**
     * Freezing rain weather element values.
     */
    private static final int[] FREEZING_RAIN = { 19, 20, 30 };

    /**
     * Light freezing rain weather element values.
     */
    private static final int[] LIGHT_FREEZING_RAIN = { 9, 10, 29 };

    /**
     * Heavy snow weather element values.
     */
    private static final int[] HEAVY_SNOW = { 24, 25, 27, 108 };

    /**
     * Snow weather element values.
     */
    private static final int[] SNOW = { 14, 15, 17, 105 };

    /**
     * Light snow weather element values.
     */
    private static final int[] LIGHT_SNOW = { 4, 5, 7, 102 };

    /**
     * Sleet/ice pellets weather element values.
     */
    private static final int[] SLEET = { 6, 8, 16, 18, 26, 28, 103, 106, 109 };

    /**
     * Fog weather element values.
     */
    private static final int[] FOG = { 201, 202, 208, 209, 210, 211 };

    /**
     * Haze weather element value.
     */
    private static final int HAZE = 207;

    /**
     * Hail weather element values.
     */
    private static final int[] HAIL = { 33, 34, 35, 36, 111, 112 };

    /**
     * Blowing snow weather element value.
     */
    private static final int BLOWING_SNOW = 217;

    /**
     * Blowing dust, sandstorm, dust devil weather element values.
     */
    private static final int[] BLOWING_DUST = { 213, 215, 304, 305, 307, 308 };

    /**
     * Tornado, funnel cloud weather element value.
     */
    private static final int TORNADO = 306;
    /*
     * End constants from build_daily_obs_weather.ec and
     * build_daily_obs_weather.h.
     */

    /**
     * Float missing value used in compute_daily_precip.c and
     * build_daily_obs_temp.ec.
     */
    public static final float R_MISS = -9999;

    /**
     * Integer missing value used in build_daily_obs_temp.ec.
     */
    public static final int I_MISS = -9999;

    /**
     * Constructor.
     */
    public ClimateCreatorDAO() {
        super();
    }

    /**
     * Migrated from build_daily_obs_weather.ec
     * 
     * <pre>
     *  void build_daily_obs_weather (climate_date      day_begin_date, 
     *                            climate_time      day_begin_time,
     *                                climate_date      day_end_date,
     *                                climate_time      day_end_time,
     *                                int                   flag[],
     *                long*         station_id,
     *                                int*                  wx_count,
     *                                int*                  ec_quality
     *                )
     *
     *  Dan Zipper                 PRC/TDL         HP 9000/7xx
     *
     *   FUNCTION DESCRIPTION
     *   ====================
     *
     *  This function will get the correct weather types from the database.
     * </pre>
     * 
     * @param window
     * @param dailyClimateData
     *            Data to fill out weather events for. Must have station ID set.
     * @param dailyDataMethod
     *            QC data to fill out for weather events.
     * @throws ClimateQueryException
     * @throws ClimateInvalidParameterException
     */

    public void buildDailyObsWeather(ClimateDates window,
            DailyClimateData dailyClimateData, DailyDataMethod dailyDataMethod)
            throws ClimateQueryException, ClimateInvalidParameterException {
        int stationID = dailyClimateData.getInformId();

        /* Determine the length of the time period being computed. */
        int numHours = window.getIntervalInHours();

        Calendar baseTime = TimeUtil.newCalendar();

        baseTime.clear();
        // Java month indexing starts at 0
        // expected data has 0 minute
        baseTime.set(window.getStart().getYear(),
                window.getStart().getMon() - 1, window.getStart().getDay(),
                window.getStartTime().getHour(), 0);

        // start looping through hours, increment time at end of loop
        for (int i = 0; i <= numHours; i++) {
            Calendar nominalTime = (new ClimateDate(baseTime))
                    .getCalendarFromClimateDate();
            nominalTime.add(Calendar.HOUR_OF_DAY,
                    baseTime.get(Calendar.HOUR_OF_DAY));
            String nominalTimeString = (new ClimateDate(nominalTime))
                    .toFullDateString() + " "
                    + nominalTime.get(Calendar.HOUR_OF_DAY) + ":00:00";
            Calendar nominalHourIncrement = TimeUtil.newCalendar(nominalTime);
            nominalHourIncrement.add(Calendar.HOUR_OF_DAY, 1);
            StringBuilder wxQuery = new StringBuilder(
                    "SELECT distinct boo.element_value, rep.fss_rpt_instance FROM ");
            wxQuery.append(ClimateDAOValues.BOOLEAN_VALUES_TABLE_NAME);
            wxQuery.append(" as boo, ");
            wxQuery.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
            wxQuery.append(" as rep, ");
            wxQuery.append(ClimateDAOValues.FSS_CATEGORY_MULTI_TABLE_NAME);
            wxQuery.append(" as cat ");
            wxQuery.append(" WHERE nominal_dtime = :nominalTime");
            wxQuery.append(" AND station_id = :stationID");
            wxQuery.append(" AND report_subtype = 'MTR' ");
            wxQuery.append(" AND cat.element_id = ")
                    .append(MetarUtils.METAR_WX);
            wxQuery.append(" AND rep.fss_rpt_instance = cat.fss_rpt_instance ");
            wxQuery.append(" AND cat.element_value = boo.element_value ");
            wxQuery.append(" AND origin_dtime in ");
            wxQuery.append(" (SELECT max(origin_dtime) FROM ");
            wxQuery.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
            wxQuery.append(" WHERE nominal_dtime = :nominalTime");
            wxQuery.append(" AND station_id = :stationID");
            wxQuery.append(" AND report_subtype = 'MTR')");

            Map<String, Object> paramMap = new HashMap<>();
            paramMap.put("nominalTime", nominalTime);
            paramMap.put("stationID", stationID);

            /*
             * Legacy documentation:
             * 
             * Get the weather groups (if any) from the scheduled METARs and
             * their corrections.
             */
            int wxQueryResultsCount = getWeatherElements(dailyClimateData,
                    nominalTimeString, wxQuery.toString(), paramMap);

            if (wxQueryResultsCount == 0) {
                // no results from previous query
                /*
                 * Legacy documentation:
                 * 
                 * Check to see if this is because there was no weather to
                 * report or because there was no report...
                 */
                StringBuilder noWeatherQuery = new StringBuilder(
                        "SELECT count(*) FROM ");
                noWeatherQuery.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
                noWeatherQuery.append(" WHERE nominal_dtime = :nominalTime");
                noWeatherQuery.append(" AND station_id = :stationID");
                noWeatherQuery.append(" AND report_subtype= 'MTR'");

                Map<String, Object> noWeatherParamMap = new HashMap<>();
                noWeatherParamMap.put("stationID", stationID);
                noWeatherParamMap.put("nominalTime", nominalTime);

                /*
                 * not using queryForOneValue method because exceptions versus
                 * actual results means something
                 */
                try {
                    Object[] res = getDao().executeSQLQuery(
                            noWeatherQuery.toString(), noWeatherParamMap);
                    if ((res != null) && (res.length >= 1)
                            && (res[0] != null)) {
                        int count = ((Number) res[0]).intValue();

                        if (count == 0) {
                            logger.warn("The " + nominalTimeString
                                    + "Z report is missing.");
                        } else {
                            logger.info("No weather was reported in the "
                                    + nominalTimeString + "Z METAR report.");
                        }

                    } else {
                        throw new ClimateQueryException(
                                "Received null or empty results for query that must have a result: ["
                                        + noWeatherQuery.toString()
                                        + "] and map: [" + noWeatherParamMap
                                        + "].");
                    }
                } catch (Exception e) {
                    throw new ClimateQueryException(
                            "Error querying the climate database with query ["
                                    + noWeatherQuery + "] and map: ["
                                    + noWeatherParamMap + "]",
                            e);
                }
            }

            /*
             * Legacy documentation:
             * 
             * Now check for any special reports.
             */
            StringBuilder sp1Query = new StringBuilder(
                    "SELECT distinct valid_dtime FROM ");
            sp1Query.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
            sp1Query.append(
                    " WHERE nominal_dtime >= :nominalTime and nominal_dtime < :nominalHourIncrement");
            sp1Query.append(" AND station_id = :stationID");
            sp1Query.append(" AND report_subtype= 'SPECI'");
            StringBuilder sp2Query = new StringBuilder(
                    "SELECT distinct boo.element_value, rep.fss_rpt_instance FROM ");
            sp2Query.append(ClimateDAOValues.BOOLEAN_VALUES_TABLE_NAME);
            sp2Query.append(" as boo, ");
            sp2Query.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
            sp2Query.append(" as rep, ");
            sp2Query.append(ClimateDAOValues.FSS_CATEGORY_MULTI_TABLE_NAME);
            sp2Query.append(" as cat");
            sp2Query.append(
                    " WHERE nominal_dtime >= :nominalTime and nominal_dtime < :nominalHourIncrement");
            sp2Query.append(" AND station_id = :stationID");
            sp2Query.append(" AND report_subtype = 'SPECI' ");
            sp2Query.append(" AND cat.element_id = ")
                    .append(MetarUtils.METAR_WX);
            sp2Query.append(
                    " AND rep.fss_rpt_instance = cat.fss_rpt_instance ");
            sp2Query.append(" AND cat.element_value = boo.element_value ");
            sp2Query.append(" AND origin_dtime in ");
            sp2Query.append(" (SELECT max(origin_dtime) FROM ");
            sp2Query.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
            sp2Query.append(" WHERE valid_dtime = :validDTime");
            sp2Query.append(" AND station_id = :stationID");
            sp2Query.append(" AND report_subtype = 'SPECI')");

            Map<String, Object> specialParamMap = new HashMap<>();
            specialParamMap.put("nominalTime", nominalTime);
            specialParamMap.put("nominalHourIncrement", nominalHourIncrement);
            specialParamMap.put("stationID", stationID);

            try {

                Object[] results = getDao().executeSQLQuery(sp1Query.toString(),
                        specialParamMap);

                if ((results != null) && (results.length >= 1)) {
                    for (Object result : results) {
                        if (result instanceof Date) {
                            try {
                                Calendar validDTime = TimeUtil
                                        .newCalendar((Date) result);

                                /*
                                 * Legacy documentation:
                                 * 
                                 * A special exists ... grab it's weather
                                 * elements...
                                 */
                                specialParamMap.put("validDTime", validDTime);
                                getWeatherElements(dailyClimateData,
                                        nominalTimeString, sp2Query.toString(),
                                        specialParamMap);

                            } catch (ClimateQueryException e) {
                                // if an inner query failed
                                throw new ClimateQueryException(
                                        "Error with inner query: ", e);
                            }
                        } else {
                            throw new Exception(
                                    "Unexpected return type from query, expected java.util.Date, got "
                                            + result.getClass().getName());
                        }

                    }
                } else {
                    logger.warn(
                            "No results returned for weather observation query: ["
                                    + sp1Query.toString() + "] and map: ["
                                    + specialParamMap + "]");
                }
            } catch (ClimateQueryException e) {
                throw new ClimateQueryException(
                        "Error querying climate database: ", e);
            } catch (Exception e) {
                throw new ClimateQueryException(
                        "Error querying the climate database with query: ["
                                + sp1Query + "] and map: [" + specialParamMap
                                + "]",
                        e);
            }

            // go to next hour in next iteration
            baseTime.add(Calendar.HOUR, 1);
        }

        // set number of weather events
        for (int i = 0; i < dailyClimateData.getWxType().length; i++) {
            if (dailyClimateData.getWxType()[i] == 1) {
                if (dailyClimateData
                        .getNumWx() == ParameterFormatClimate.MISSING) {
                    dailyClimateData.setNumWx(1);
                } else {
                    dailyClimateData.setNumWx(dailyClimateData.getNumWx() + 1);
                }
            }
        }

        // set QC flag
        if (dailyClimateData.getNumWx() > 0) {
            if (dailyDataMethod.getWeatherQc() == QCValues.WX_FROM_DSM) {
                dailyDataMethod.setWeatherQc(QCValues.WX_COMBO);
            } else {
                dailyDataMethod.setWeatherQc(QCValues.WX_FROM_OBS);
            }
        } else {
            dailyDataMethod.setWeatherQc(QCValues.WX_BORING);
        }
    }

    /**
     * Get weather elements for the given time using the given query (either for
     * METAR or special report).
     * 
     * @param dailyClimateData
     * @param nominalTime
     * @param wxQuery
     * @param paramMap
     * @return number of query results.
     * @throws ClimateQueryException
     */
    private int getWeatherElements(DailyClimateData dailyClimateData,
            String nominalTime, String wxQuery, Map<String, Object> paramMap)
            throws ClimateQueryException {
        int wxQueryResultsCount = 0;
        try {
            int[] snapshot = new int[9];

            Object[] results = getDao().executeSQLQuery(wxQuery, paramMap);

            if ((results != null) && (results.length >= 1)) {
                for (Object result : results) {
                    if (result instanceof Object[]) {
                        Object[] oa = (Object[]) result;
                        try {
                            int index = 0;
                            // element (wx) value
                            int wxValue = ((Number) oa[index++]).intValue();
                            // fss report instance
                            int fssReportInstance = ((Number) oa[index++])
                                    .intValue();

                            // test for visibility
                            wxValue = testForFogVisibility(fssReportInstance,
                                    nominalTime, wxValue);

                            // process weather type
                            processWxType(dailyClimateData, wxValue, snapshot);
                            wxQueryResultsCount++;
                        } catch (ClimateQueryException e) {
                            // if an inner query failed
                            throw new ClimateQueryException(
                                    "Error with inner query: ", e);
                        }
                    } else {
                        throw new Exception(
                                "Unexpected return type from query, expected Object[], got "
                                        + result.getClass().getName());
                    }

                }
            } else {
                logger.warn("No results for weather query: [" + wxQuery
                        + "] and map: [" + paramMap + "]");
            }
        } catch (ClimateQueryException e) {
            throw new ClimateQueryException("Error querying climate database: ",
                    e);
        } catch (Exception e) {
            throw new ClimateQueryException(
                    "Error querying the climate database with query: ["
                            + wxQuery + "] and map: [" + paramMap + "]",
                    e);
        }
        return wxQueryResultsCount;
    }

    /**
     * Migrated from build_daily_obs_weather.ec.
     * 
     * @param dailyClimateData
     * @param wxValue
     * @param snapshot
     */
    private void processWxType(DailyClimateData dailyClimateData, int wxValue,
            int[] snapshot) {
        for (int i = 0; i < DailyClimateData.TOTAL_WX_TYPES; i++) {
            if (dailyClimateData.getWxType()[i] == 1) {
                // flag is already set; no need to repeat work with no effect
                continue;
            } else {
                /*
                 * Unfortunately with reliance on snapshot data reading/editing,
                 * and the different weather arrays, this switch is unable to be
                 * simplified to a single loop.
                 */
                switch (i) {
                case DailyClimateData.WX_THUNDER_STORM_INDEX:
                    /*
                     * This case is checking for any kind or report with
                     * thunder.
                     */
                    for (int k : THUNDERSTORM) {
                        if (k == wxValue) {
                            dailyClimateData.getWxType()[i] = 1;
                            return;
                        }
                    }
                    break;
                case DailyClimateData.WX_MIXED_PRECIP_INDEX:
                    /*
                     * This case is checking for any kind of report with any
                     * combination of frozen, freezing or liquid precipitation.
                     */
                    /*
                     * DAI 28: Legacy code only accessed indices 0-7, ignoring
                     * index 8 (sleet/ice pellets). IWT informed that sleet
                     * should likely not be skipped over and this was a Legacy
                     * bug.
                     */
                    int wxCount = 0;
                    for (int k = 0; k < 9; k++) {
                        if (snapshot[k] != 0) {
                            wxCount++;
                        }
                    }

                    if (wxCount > 1) {
                        dailyClimateData.getWxType()[i] = 1;
                    }
                    break;
                case DailyClimateData.WX_HEAVY_RAIN_INDEX:
                    /*
                     * This case is checking for heavy rain reports
                     */
                    for (int k : HEAVY_RAIN) {
                        if (k == wxValue) {
                            dailyClimateData.getWxType()[i] = 1;
                            snapshot[0] = 1;
                            return;
                        }
                    }
                    break;
                case DailyClimateData.WX_RAIN_INDEX:
                    /*
                     * This case is checking for rain reports
                     */
                    for (int k : RAIN) {
                        if (k == wxValue) {
                            dailyClimateData.getWxType()[i] = 1;
                            snapshot[1] = 1;
                            return;
                        }
                    }
                    break;
                case DailyClimateData.WX_LIGHT_RAIN_INDEX:
                    /*
                     * This case is checking for light_rain reports
                     */
                    for (int k : LIGHT_RAIN) {
                        if (k == wxValue) {
                            dailyClimateData.getWxType()[i] = 1;
                            snapshot[2] = 1;
                            return;
                        }
                    }
                    break;
                case DailyClimateData.WX_FREEZING_RAIN_INDEX:
                    /*
                     * This case is checking for freezing rain
                     */
                    for (int k : FREEZING_RAIN) {
                        if (k == wxValue) {
                            dailyClimateData.getWxType()[i] = 1;
                            snapshot[3] = 1;
                            return;
                        }
                    }
                    break;
                case DailyClimateData.WX_LIGHT_FREEZING_RAIN_INDEX:
                    /*
                     * This case is checking for light freezing rain
                     */
                    for (int k : LIGHT_FREEZING_RAIN) {
                        if (k == wxValue) {
                            dailyClimateData.getWxType()[i] = 1;
                            snapshot[4] = 1;
                            return;
                        }
                    }
                    break;
                case DailyClimateData.WX_HEAVY_SNOW_INDEX:
                    /*
                     * This case is checking for heavy snow
                     */
                    for (int k : HEAVY_SNOW) {
                        if (k == wxValue) {
                            dailyClimateData.getWxType()[i] = 1;
                            snapshot[5] = 1;
                            return;
                        }
                    }
                    break;
                case DailyClimateData.WX_SNOW_INDEX:
                    /*
                     * This case is checking for snow
                     */
                    for (int k : SNOW) {
                        if (k == wxValue) {
                            dailyClimateData.getWxType()[i] = 1;
                            snapshot[6] = 1;
                            return;
                        }
                    }
                    break;
                case DailyClimateData.WX_LIGHT_SNOW_INDEX:
                    /*
                     * This case is checking for light snow
                     */
                    for (int k : LIGHT_SNOW) {
                        if (k == wxValue) {
                            dailyClimateData.getWxType()[i] = 1;
                            snapshot[7] = 1;
                            return;
                        }
                    }
                    break;
                case DailyClimateData.WX_ICE_PELLETS_INDEX:
                    /*
                     * This case is checking for sleet
                     */
                    for (int k : SLEET) {
                        if (k == wxValue) {
                            dailyClimateData.getWxType()[i] = 1;
                            snapshot[8] = 1;
                            return;
                        }
                    }
                    break;
                case DailyClimateData.WX_FOG_INDEX:
                    /*
                     * This case is checking for fog
                     */
                    for (int k : FOG) {
                        if (k == wxValue) {
                            dailyClimateData.getWxType()[i] = 1;
                            return;
                        }
                    }
                    break;
                case DailyClimateData.WX_FOG_QUARTER_SM_INDEX:
                    /*
                     * This case is checking for fog when the visibility is
                     * equal to or under 1/4 of a mile.
                     */
                    if (wxValue == DENSE_FOG) {
                        dailyClimateData.getWxType()[i] = 1;
                        return;
                    }
                    break;
                case DailyClimateData.WX_HAZE_INDEX:
                    /*
                     * This case is checking for any reports of haze
                     */
                    if (HAZE == wxValue) {
                        dailyClimateData.getWxType()[i] = 1;
                        return;
                    }
                    break;
                case DailyClimateData.WX_HAIL_INDEX:
                    /*
                     * This case is checking for any reports of hail
                     */
                    for (int k : HAIL) {
                        if (k == wxValue) {
                            dailyClimateData.getWxType()[i] = 1;
                            return;
                        }
                    }
                    break;
                case DailyClimateData.WX_BLOWING_SNOW_INDEX:
                    /*
                     * This case is checking for any reports of blowing snow
                     */
                    if (BLOWING_SNOW == wxValue) {
                        dailyClimateData.getWxType()[i] = 1;
                        return;
                    }
                    break;
                case DailyClimateData.WX_SAND_STORM_INDEX:
                    /*
                     * This case is checking for any reports of blowing
                     * dust-sand
                     */
                    for (int k : BLOWING_DUST) {
                        if (k == wxValue) {
                            dailyClimateData.getWxType()[i] = 1;
                            return;
                        }
                    }
                    break;
                case DailyClimateData.WX_FUNNEL_CLOUD_INDEX:
                    /*
                     * This case is checking for any reports of a tornado
                     */
                    if (TORNADO == wxValue) {
                        dailyClimateData.getWxType()[i] = 1;
                        return;
                    }
                    break;
                default:
                    /*
                     * If some new weather element type has been added but is
                     * not included in this switch, log error.
                     */
                    logger.error("Unhandled weather type index: [" + i
                            + "]. Ensure Daily Data weather array size is correct.");
                    break;
                }
            }
        }
    }

    /**
     * Migrated from build_daily_obs_weather.ec.
     * 
     * <pre>
     * MODULE NUMBER: 3
     * MODULE NAME:   test_for_visib
     * PURPOSE:
     *
     * </pre>
     * 
     * Legacy module whose purpose appears to be to determine density of fog, if
     * the given weather element is indeed fog.
     * 
     * @param fssReportInstance
     * @param nominalTime
     * @param wxValue
     * @return the new wxValue to use instead.
     * @throws ClimateQueryException
     */

    private int testForFogVisibility(int fssReportInstance, String nominalTime,
            int wxValue) throws Exception {
        for (int j : FOG) {
            if (j == wxValue) {

                StringBuilder query = new StringBuilder(
                        "SELECT distinct element_value FROM ");
                query.append(ClimateDAOValues.FSS_CATEGORY_SINGLE_TABLE_NAME);
                query.append(" WHERE fss_rpt_instance = :fssReportInstance");
                query.append(" AND element_id = ")
                        .append(MetarUtils.METAR_VISIB);

                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("fssReportInstance", fssReportInstance);

                try {
                    Object[] results = getDao()
                            .executeSQLQuery(query.toString(), paramMap);
                    if ((results != null) && (results.length >= 1)) {
                        Object result = results[0];
                        if (result instanceof Number) {
                            try {
                                int visibility = ((Number) result).intValue();

                                if (visibility <= 4) {
                                    return DENSE_FOG;
                                }
                            } catch (NullPointerException e) {
                                throw new ClimateQueryException(
                                        "Unexpected null result with query: ["
                                                + query + "] and map: ["
                                                + paramMap + "].",
                                        e);
                            } catch (Exception e) {
                                // if casting failed
                                throw new ClimateQueryException(
                                        "Unexpected return column type.", e);
                            }

                        } else {
                            throw new ClimateQueryException(
                                    "Unexpected return type from query, expected a number, got "
                                            + result.getClass().getName());
                        }

                    } else {
                        logger.info(
                                "Couldn't find a visibility report for report time "
                                        + nominalTime
                                        + "Z. The fog will be processed as not dense. Query: ["
                                        + query.toString() + "] and map: ["
                                        + paramMap + "]");
                    }
                } catch (Exception e) {
                    throw new ClimateQueryException(
                            "An error was encountered retrieving the visibility value from"
                                    + " the METAR DB for the " + nominalTime
                                    + "Z METAR report.\n"
                                    + "Error querying the climate database with query: ["
                                    + query + "] and map: [" + paramMap + "]",
                            e);
                }
            }
        }

        // no changes, return original wxValue
        return wxValue;
    }

    /**
     * Migrated from build_daily_obs_sky.f
     * 
     * <pre>
     *   June 1998     Jason P. Tuell        PRC/TDL
     *   September     Dan Zipper            PRC/TDL
     *
     *   Purpose: This routine will call the function get_sky_cover which will
     *            compute the daily average sky cover for the morning and evening
     *            reports.
     * 
     * </pre>
     * 
     * Migrated from get_sky_cover.ec.
     * 
     * <pre>
     *       
     *       
     *       void get_sky_cover (climate_date        begin_date,
     *                           climate_time        begin_time,
     *                           climate_date        end_date,
     *                           climate_time        end_time,
     *                           long                *station_id,
     *                           daily_climate_date  *yesterday(i),
     *                           int                 qc(i)%sky_cover_qc  )  
     *
     *   Jason Tuell     PRC/TDL             HP 9000/7xx
     *   Dan Zipper          PRC/TDL
     *
     *
     *   FUNCTION DESCRIPTION
     *   ====================
     *
     *          This routine retrieves cloud coverage information from 
     *          a regularly scheduled METAR report for a user-specified
     *          station id and nominal time. This cloud cover information
     *          is reported in a range of 0 to 11, 0 is no clouds present,
     *          10 is overcast, and 11 is overcast due to an obscuration
     *          (such as heavy snow).
     *
     *          This routine returns the most recent METAR report in the 
     *          case of a METAR that has been transmitted multiple times
     *          or the case of a METAR that has been followed by one or 
     *          more corrections. 
     *
     *          The cloud information is read from the FSS_cloud_layer table
     *          in the hm database.
     *
     *          This routine is a hybrid of routines written by Bryon Lawrence and
     *          Bill Mattison.
     *
     * </pre>
     * 
     * @param window
     *            date range to search for.
     * @param dailyClimateData
     *            data to fill out sky cover information for. Assumed to already
     *            have related elements set to missing and inform ID (station
     *            ID) set.
     * @param qc
     *            QC data to fill out sky cover QC information for. Assumed to
     *            already have related elements set to missing.
     * @throws ClimateQueryException
     */

    public void getSkyCover(ClimateDates window,
            DailyClimateData dailyClimateData, DailyDataMethod qc)
            throws ClimateQueryException {
        int informId = dailyClimateData.getInformId();

        /*
         * Set a counting variable. The variable counter will be used later in
         * determining the avg. sky cover of the day. The reason being that an
         * hour may be missing and we do not want that hour factored into the
         * avg. sky cover for the day.
         */
        int hourlyCounter = 0;
        float totalCover = 0;

        /* Creating a loop that will sum up the sky cover for each hour */
        int loopPeriod;
        if (window.getStart().getDay() == window.getEnd().getDay()) {
            loopPeriod = window.getEndTime().getHour()
                    - window.getStartTime().getHour() + 1;
        } else {
            loopPeriod = (window.getEndTime().getHour()
                    + TimeUtil.HOURS_PER_DAY) - window.getStartTime().getHour()
                    + 1;
        }

        ClimateDate currDate = new ClimateDate(window.getStart());
        ClimateTime currTime = new ClimateTime(window.getStartTime());

        // origin time, correction are used only for ordering
        StringBuilder reportQuery = new StringBuilder(
                "SELECT fss_rpt_instance, origin_dtime, correction FROM ");
        reportQuery.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
        reportQuery.append(" WHERE nominal_dtime = :datetime ");
        reportQuery.append(" AND station_id = :informId");
        reportQuery.append(" AND report_subtype = 'MTR'");
        reportQuery.append(" ORDER BY correction DESC, origin_dtime DESC");

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("informId", informId);

        for (int i = 1; i <= loopPeriod; i++) {
            // time/date incremented at the end of the loop
            Calendar dateTime = combine(currDate, currTime);
            String dateTimeString = currDate.toFullDateString() + " "
                    + currTime.toHourMinString();
            paramMap.put("datetime", dateTime);
            try {
                Object[] reportResults = getDao()
                        .executeSQLQuery(reportQuery.toString(), paramMap);
                if ((reportResults != null) && (reportResults.length >= 1)) {
                    Object reportResult = reportResults[0];
                    if (reportResult instanceof Object[]) {
                        Object[] reportOa = (Object[]) reportResult;
                        try {
                            /*
                             * At least one metar is available, but that could
                             * include multiple transmissions and corrections.
                             * Separate corrections from originals, and sort
                             * according to the time the reports were
                             * constructed. Get the report id number for the
                             * most recent correction, or the most recent
                             * original if there are no corrections.
                             */
                            int fssReportInstance = ((Number) reportOa[0])
                                    .intValue();

                            /*
                             * do not access nullable origin_dtime or
                             * correction, which are used only for sorting.
                             */
                            try {
                                totalCover += getSkyCoverCloud(informId,
                                        fssReportInstance);

                                // increment valid number of hours
                                hourlyCounter++;
                            } catch (Exception e) {
                                throw new ClimateQueryException(
                                        "An error was encountered retrieving the report from"
                                                + " the METAR DB for "
                                                + dateTimeString + "Z.\n",
                                        e);
                            }

                        } catch (ClimateQueryException e) {
                            throw new ClimateQueryException(
                                    "Error with inner query", e);
                        }
                    } else {
                        throw new ClimateQueryException(
                                "Unexpected return type from query, expected Object[], got "
                                        + reportResult.getClass().getName());
                    }

                } else {
                    /* no rows matched; no data for this time period */
                    logger.info("For station ID [" + informId
                            + "], there was no sky cover detected for the time ["
                            + dateTimeString + "] and query ["
                            + reportQuery.toString() + "] and map: [" + paramMap
                            + "]");
                }
            } catch (Exception e) {
                throw new ClimateQueryException(
                        "An error was encountered retrieving the report from"
                                + " the METAR DB for " + dateTimeString + "Z.\n"
                                + "Error querying the climate database with query: ["
                                + reportQuery.toString() + "] and map: ["
                                + paramMap + "]",
                        e);
            }

            // set up date/time for next iteration
            /*
             * nominal time is for the reports which come in at a fixed time. It
             * will be used to determine the date and hour of the metar ob.
             */
            currTime.setHour(currTime.getHour() + 1);
            /*
             * This is a special case for when the nominal hour goes to the next
             * day (24th hour). It converts the 24th hour to 00 and increments
             * the appropriate day/month/year if needed.
             */
            if (currTime.getHour() >= TimeUtil.HOURS_PER_DAY) {
                currTime.setHour(currTime.getHour() - TimeUtil.HOURS_PER_DAY);

                Calendar cal = currDate.getCalendarFromClimateDate();
                cal.add(Calendar.DAY_OF_MONTH, 1);
                currDate = new ClimateDate(cal);
            }
        }

        if (hourlyCounter > 0) {
            // data was processed, save and override the missing values
            dailyClimateData.setSkyCover(totalCover / hourlyCounter);
            qc.setSkyCoverQc(QCValues.SKY_COVER_CALCULATED);
        }
    }

    /**
     * Get cloud details for sky coverage, first performing a preliminary row
     * count.
     * 
     * Migrated from get_sky_cover.ec.
     * 
     * @param informId
     * @param fssReportInstance
     * @return coverage amount for given report instance.
     * @throws ClimateQueryException
     */
    private float getSkyCoverCloud(int informId, int fssReportInstance)
            throws ClimateQueryException {
        /*
         * does the metar include a value for the requested observation?
         */
        StringBuilder cloudGlanceQuery = new StringBuilder(
                "SELECT count(*) FROM ");
        cloudGlanceQuery.append(ClimateDAOValues.FSS_CLOUD_LAYER_TABLE_NAME);
        cloudGlanceQuery.append(" WHERE fss_rpt_instance = :fssReportInstance");
        cloudGlanceQuery.append(" AND element_id = ")
                .append(MetarUtils.METAR_CLOUD_COVER);

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("fssReportInstance", fssReportInstance);

        Object[] cloudGlanceResults = getDao()
                .executeSQLQuery(cloudGlanceQuery.toString(), paramMap);
        if ((cloudGlanceResults != null) && (cloudGlanceResults.length >= 1)) {
            Object cloudGlanceResult = cloudGlanceResults[0];
            if (cloudGlanceResult instanceof Number) {
                try {
                    int cloudCount = ((Number) cloudGlanceResult).intValue();

                    if (cloudCount == 0) {
                        logger.warn("For station ID [" + informId
                                + "], anticipated non-zero row count from mandatory data for query ["
                                + cloudGlanceQuery + "] and map: [" + paramMap
                                + "]");
                        return 0;
                    } else {
                        return getSkyCoverCloudSupport(informId,
                                fssReportInstance);
                    }
                } catch (NullPointerException e) {
                    throw new ClimateQueryException(
                            "Unexpected null result with query: ["
                                    + cloudGlanceQuery + "] and map: ["
                                    + paramMap + "].",
                            e);
                } catch (ClimateQueryException e) {
                    throw new ClimateQueryException("Error with inner query",
                            e);
                } catch (Exception e) {
                    throw new ClimateQueryException("Unexpected error.", e);
                }
            } else {
                throw new ClimateQueryException(
                        "Unexpected return type from query, expected Number, got "
                                + cloudGlanceResult.getClass().getName());
            }
        } else {
            throw new ClimateQueryException("For station ID [" + informId
                    + "], anticipated row count from mandatory data for query ["
                    + cloudGlanceQuery.toString() + "] and map: [" + paramMap
                    + "]. Null or no results were returned.");
        }
    }

    /**
     * Get cloud details for sky coverage.
     * 
     * Migrated from get_sky_cover.ec.
     * 
     * @param informId
     * @param fssReportInstance
     * @return cloud coverage amount for given report instance.
     * @throws ClimateQueryException
     */
    private float getSkyCoverCloudSupport(int informId, int fssReportInstance)
            throws ClimateQueryException {
        StringBuilder cloudQuery = new StringBuilder(
                "SELECT element_value FROM ");
        cloudQuery.append(ClimateDAOValues.FSS_CLOUD_LAYER_TABLE_NAME);
        cloudQuery.append(" main WHERE fss_rpt_instance = :fssReportInstance");
        cloudQuery.append(" AND element_id = ")
                .append(MetarUtils.METAR_CLOUD_COVER);
        cloudQuery
                .append(" AND layer_number = (SELECT MAX (layer_number) FROM ");
        cloudQuery.append(ClimateDAOValues.FSS_CLOUD_LAYER_TABLE_NAME);
        cloudQuery.append(
                " sub1 WHERE main.fss_rpt_instance = sub1.fss_rpt_instance ");
        cloudQuery.append(" AND main.element_id = sub1.element_id ");
        cloudQuery.append(
                " AND element_value = (SELECT MAX(element_value) FROM ");
        cloudQuery.append(ClimateDAOValues.FSS_CLOUD_LAYER_TABLE_NAME);
        cloudQuery.append(
                " sub2 WHERE main.fss_rpt_instance = sub2.fss_rpt_instance ");
        cloudQuery.append(" AND main.element_id = sub2.element_id))");

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("fssReportInstance", fssReportInstance);

        try {
            Object[] cloudResults = getDao()
                    .executeSQLQuery(cloudQuery.toString(), paramMap);
            if ((cloudResults != null) && (cloudResults.length >= 1)) {
                Object cloudResult = cloudResults[0];
                if (cloudResult instanceof Short) {
                    try {
                        int elementValue = ((Number) cloudResult).intValue();

                        /*
                         * This is where we will sum up all hourly sky cover in
                         * order to get the avg. sky cover for the day. First,
                         * transform to the category value to the midpoint of
                         * the range in eighths.
                         */
                        switch (elementValue) {
                        case 3:
                            return 0.1875f;
                        case 4:
                            return 0.4375f;
                        case 8:
                            return 0.75f;
                        case 10:
                        case 11:
                            return 1;
                        default:
                            logger.warn("Unhandled element value ["
                                    + elementValue + "] returned from query ["
                                    + cloudQuery + "] and map: [" + paramMap
                                    + "]");
                            return 0;
                        }
                    } catch (NullPointerException e) {
                        throw new ClimateQueryException(
                                "Unexpected null result with query: ["
                                        + cloudQuery + "] and map: [" + paramMap
                                        + "].",
                                e);
                    } catch (Exception e) {
                        // error casting
                        throw new ClimateQueryException(
                                "Unexpected column type.", e);
                    }
                } else {
                    throw new ClimateQueryException(
                            "Unexpected return type from query, expected short, got "
                                    + cloudResult.getClass().getName());
                }
            } else {
                logger.warn("For station ID [" + informId
                        + "], anticipated results for query: ["
                        + cloudQuery.toString() + "] and map: [" + paramMap
                        + "].");
                return 0;
            }
        } catch (Exception e) {
            throw new ClimateQueryException(
                    "Error querying the climate database with query: ["
                            + cloudQuery.toString() + "] and map: [" + paramMap
                            + "]",
                    e);
        }
    }

    /**
     * Converted from get_metar_categsingle.ecpp.
     * 
     * Original description:
     * 
     * <pre>
     * FILENAME:    get_metar_categsingle.ecpp
    *
    *  NUMBER OF MODULES:   1
    *  GENERAL INFORMATION:
    *       MODULE 1:       get_metar_categsingle_val
    *       DESCRIPTION:    Retrieves a specified metar observed datum from the 
    *                       FSS_categ_single table for a user-specified station id
    *                       element id, and nominal report time. This value is
    *                       taken from a regularly scheduled METAR report or a
    *                       correction to a regularly scheduled METAR report.
    *
     ******************************************************************************
    *
    *  Bob Morris          SAIC/GSC / MDL      HP-UX 10.20, Linux 7.0
    *  (follows get_metar_conreal.ecpp, originally coded by Bill Mattison)
    *
    *  FUNCTION DESCRIPTION
    *  ====================
    *
    *    This function gets a metar observation value and its data quality
    *    descriptor for a caller-specified station, nominal hour, and
    *    element_id number from the FSS_categ_single table in the hm database.
    *    The value is from a regularly scheduled metar, not a "special".
    *
    *    The station is specified by a long integer id number (from the
    *    station_location table in the hm database).  The observation element_id
    *    is specified by a long integer (from the hydromet_element table in
    *    the hm database).  The nominal date/time is specified by a string
    *    in "yyyy-mm-dd hh" format where yyyy is the year, mm is the month, dd
    *    is the day, and hh is the hour.
    *
    *    This function will retrieve data for ONLY the earliest METAR valid time
    *    in the nominal hour (from 15 minutes before the hour to 44 minutes past
    *    the nominal hour).  This is normally an issue only for stations that
    *    are scheduled to take routine observations and transmit routine metars
    *    more than once per hour.  "AWOS" stations, which report METARs every 20
    *    minutes (e.g., 0, 20, and 40 minutes after each hour), are an example.
    *    quality descriptor.  This function selects the earliest valid time's
    *    latest correction.  If there are no corrections, then this function
    *    selects the value from the earliest metar valid time's latest receipt.
    *    METARs with later valid times in the nominal hour are ignored.  See the
    *    function get_metar_tempc_for_nominal for an example where all METAR
    *    valid times in a nominal hour are considered.
    *
    *    The normal result is one scalar integer value and a one-character data
    *    quality descriptor.
    *    
    *    If this function succeeds in finding the requested value, the return
    *    status is STATUS_OK (= 0).  If metars for the requested station, time,
    *    and type are found, but the requested value is not found, the return
    *    status is STATUS_FAILURE (= 1).  If no metars for the requested station,
    *    time, and type are found, the return status is NO_HITS.  See
    *    POSSIBLE STATUS VALUES below for other possible status values and their
    *    meanings.
     *
     * </pre>
     * 
     * @param informId
     * @param elementID
     *            ID key of element to look for
     * @param dateTime
     *            date/time to query for
     * @param dateTimeString
     *            date time string in format "yyyy-MM-dd hh24:mm".
     * @return the value and data quality descriptor for the given element, or
     *         the missing value (9999) and an empty descriptor if no records
     *         were found.
     * @throws ClimateQueryException
     */
    public FSSReportResult getMetarCategSingle(int informId, int elementID,
            Calendar dateTime, String dateTimeString)
            throws ClimateQueryException {
        /*
         * Legacy documentation:
         * 
         * Look at METARs for the requested station and hour. Take the earliest
         * valid time for the nominal hour, take any corrections for this report
         * over its original version, and sort lastly according to the time the
         * reports were ingested to get the most recent correction. From this
         * one report only (defined by its fss_rpt_instance in the fss_report
         * table), try to get the element value and its dqd for the specified
         * element_id number from the subservient data table, fss_categ_single.
         */
        /*
         * Change from porting: Instead of returning failed status, on a
         * legitimate DB exception throw the exception. On no returned data,
         * return missing value.
         */
        StringBuilder query = new StringBuilder(
                "SELECT f.valid_dtime, f.origin_dtime, f.correction, ");
        query.append(" x.element_value, x.dqd FROM ");
        query.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
        query.append(" AS f LEFT OUTER JOIN (SELECT * FROM ");
        query.append(ClimateDAOValues.FSS_CATEGORY_SINGLE_TABLE_NAME);
        query.append(" WHERE element_id = :elementID");
        query.append(") AS x USING (fss_rpt_instance) ");
        query.append(" WHERE f.nominal_dtime = :dateTime");
        query.append(" AND f.station_id = :informId");
        query.append(" AND f.report_subtype = 'MTR' ");
        query.append(" ORDER BY f.valid_dtime ASC, f.correction DESC, ");
        query.append(" f.origin_dtime DESC");

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("elementID", elementID);
        paramMap.put("dateTime", dateTime);
        paramMap.put("informId", informId);

        try {
            Object[] results = getDao().executeSQLQuery(query.toString(),
                    paramMap);
            if ((results != null) && (results.length >= 1)) {
                Object result = results[0];
                if (result instanceof Object[]) {
                    Object[] oa = (Object[]) result;
                    // valid time used only for sorting, cannot be null
                    // correction is used only for sorting, could be null
                    // origin time is used only for sorting, could be null

                    double elementValue;
                    Object elementValueObj = oa[3];
                    /*
                     * element value shouldn't be null, but Legacy checks for
                     * this
                     */
                    if (elementValueObj != null) {
                        elementValue = ((Number) elementValueObj).doubleValue();

                        // dqd could be null
                        String dqd;
                        Object dqdObj = oa[4];
                        if (dqdObj != null) {
                            dqd = (String) dqdObj;
                        } else {
                            logger.warn("DQD for query [" + query
                                    + "] and map: [" + paramMap
                                    + "] is null. Empty DQD will be used.");
                            dqd = "";
                        }

                        return new FSSReportResult(elementValue, dqd);
                    } else {
                        logger.warn("Element value for query [" + query
                                + "] and map: [" + paramMap
                                + "] is null. Returning missing value.");
                    }

                } else {
                    throw new ClimateQueryException(
                            "Unexpected return type from query, expected Object[], got "
                                    + result.getClass().getName());
                }

            } else {
                logger.warn("Couldn't find a report for report time "
                        + dateTimeString
                        + "Z. Missing value will be returned for query: ["
                        + query.toString() + "] and map: [" + paramMap + "]");
            }
        } catch (Exception e) {
            throw new ClimateQueryException(
                    "An error was encountered retrieving the report from"
                            + " the SPECI DB for " + dateTimeString + "Z.\n"
                            + "Error querying the climate database with query: ["
                            + query + "] and map: [" + paramMap + "]",
                    e);
        }

        return new FSSReportResult();
    }

    /**
     * Converted from get_speci_categsingle.ecpp.
     * 
     * Original description:
     * 
     * <pre>
     * FILENAME:   get_speci_categsingle.ecpp 
     *  NUMBER OF MODULES:   1
     *  GENERAL INFORMATION:
     *       MODULE 1:       get_speci_categsingle_val
     *       DESCRIPTION:    Retrieves a specified special observed datum from
     *                   the FSS_categ_single table for a user-specified
     *                       station id, element id, element number and
     *                       valid report time. This value is taken from a 
     *                       special metar report (SPECI) or a correction to
     *                       a SPECI report.
     *
      ******************************************************************************
     *
     *  void get_speci_categsingle_val ( const long station_id ,
     *                                   const long element_id , 
     *                                   const char *valid_dtime ,
     *                                   short *value , char *dqd ,
     *                                   STATUS *status )
     *
     *  Bob Morris          SAIC/GSC / MDL      HP-UX 10.20, Linux 7.0
     *  (follows get_metar_conreal.ecpp, originally coded by Bill Mattison)
     *
     *  FUNCTION DESCRIPTION
     *  ====================
     *
     *    This function retrieves a metar special (SPECI) observation value and
     *    its data quality descriptor for a caller-specified station and valid
     *    time from the FSS_categ_single table in the hm database. Values from
     *    regularly-scheduled metars are not processed.
     *
     *    The station is specified by a long integer id number (from the
     *    station_location table in the hm database).  The observation element_id
     *    is specified by a long integer (from the hydromet_element table in
     *    the hm database).  The report valid date/time is specified by a string
     *    in "yyyy-mm-dd hh:mm" format where yyyy is the year, mm is the month,
     *    dd is the day, hh is the hour, and mm is the minute.
     *
     *    This function selects the latest-received correction for the report's
     *    valid time.  If there are no corrections, then this function selects
     *    the value from the SPECI valid time's latest receipt.
     *
     *    The normal result is one scalar integer value and a one-character data
     *    quality descriptor.
     *    
     *    If this function succeeds in finding the requested value, the return
     *    status is STATUS_OK (= 0).  If metars for the requested station, time,
     *    and type are found, but the requested value is not found, the return
     *    status is STATUS_FAILURE (= 1).  If no SPECIs for the requested station,
     *    time, and type are found, the return status is NO_HITS.  See
     *    POSSIBLE STATUS VALUES below for other possible status values and their
     *    meanings.
     *
     *
     * </pre>
     * 
     * @param informId
     * @param elementID
     *            ID key of element to look for
     * @param dateTime
     *            date/time to query for
     * @param dateTimeString
     *            date time string in format "yyyy-MM-dd hh24:mm".
     * @return the value and data quality descriptor for the given element, or
     *         the missing value (9999) and an empty descriptor if no records
     *         were found.
     * @throws ClimateQueryException
     */
    public FSSReportResult getSpeciCategSingle(int informId, int elementID,
            Date dateTime, String dateTimeString) throws ClimateQueryException {
        /*
         * Legacy documentation:
         * 
         * Look at SPECIs for the requested station and valid time. Take any
         * corrections for this report over its original version, and sort
         * lastly according to the time the reports were ingested to get the
         * most recent correction. From this one report only (defined by its
         * fss_rpt_instance in the fss_report table), try to get the element
         * value and its dqd for the specified element_id number from the
         * subservient data table, fss_categ_single.
         */
        /*
         * Change from porting: Instead of returning failed status, on a
         * legitimate DB exception throw the exception. On no returned data,
         * return missing value.
         */
        StringBuilder query = new StringBuilder(
                "SELECT f.origin_dtime, f.correction, ");
        query.append(" x.element_value, x.dqd FROM ");
        query.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
        query.append(" AS f LEFT OUTER JOIN  (SELECT * FROM ");
        query.append(ClimateDAOValues.FSS_CATEGORY_SINGLE_TABLE_NAME);
        query.append(" WHERE element_id = :elementID");
        query.append(") AS x USING (fss_rpt_instance) ");
        query.append(" WHERE f.valid_dtime = :dateTime");
        query.append(" AND f.station_id = :informId");
        query.append(" AND f.report_subtype = 'SPECI' ");
        query.append(" ORDER BY f.correction DESC, f.origin_dtime DESC");

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("elementID", elementID);
        paramMap.put("dateTime", toSqlDate(dateTime));
        paramMap.put("informId", informId);

        try {
            Object[] results = getDao().executeSQLQuery(query.toString(),
                    paramMap);
            if ((results != null) && (results.length >= 1)) {
                Object result = results[0];
                if (result instanceof Object[]) {
                    Object[] oa = (Object[]) result;
                    // correction is used only for sorting, could be null
                    // origin time is used only for sorting, could be null

                    double elementValue;
                    Object elementValueObj = oa[2];
                    /*
                     * element value shouldn't be null, but Legacy checks for
                     * this this
                     */
                    if (elementValueObj != null) {
                        elementValue = ((Number) elementValueObj).doubleValue();

                        // dqd could be null
                        String dqd;
                        Object dqdObj = oa[3];
                        if (dqdObj != null) {
                            dqd = (String) dqdObj;
                        } else {
                            logger.warn("DQD for query [" + query
                                    + "] and map: [" + paramMap
                                    + "] is null. Empty DQD will be used.");
                            dqd = "";
                        }

                        return new FSSReportResult(elementValue, dqd);
                    } else {
                        logger.warn("Element value for query [" + query
                                + "] and map: [" + paramMap
                                + "] is null. Returning missing value.");
                    }
                } else {
                    throw new ClimateQueryException(
                            "Unexpected return type from query, expected Object[], got "
                                    + result.getClass().getName());
                }

            } else {
                logger.warn("Couldn't find a report for report time "
                        + dateTimeString
                        + "Z. Missing value will be returned for query: ["
                        + query.toString() + "] and map: [" + paramMap + "]");
            }
        } catch (Exception e) {
            throw new ClimateQueryException(
                    "An error was encountered retrieving the report from"
                            + " the SPECI DB for " + dateTimeString + "Z.\n"
                            + "Error querying the climate database with query: ["
                            + query + "] and map: [" + paramMap + "]",
                    e);
        }

        return new FSSReportResult();
    }

    /**
     * Converted from get_speci_conreal.ecpp.
     * 
     * Original description:
     * 
     * <pre>
     * FILENAME:    get_speci_conreal.ecpp
    *  NUMBER OF MODULES:   1
    *  GENERAL INFORMATION:
    *       MODULE 1:       get_speci_conreal_val
    *       DESCRIPTION:    Retrieves a specified special observed datum from
    *                       the FSS_contin_real table for a user-specified 
    *                       station id, element id, element number, and
    *                       nominal report time. This value is taken from a 
    *                       sepcial metar report (SPECI) or a correction
    *                       to a SPECI report.
    *
     ******************************************************************************
    *
    * void get_speci_conreal_val ( const long station_id ,
    *                               const long element_id , char *valid_dtime ,
    *                               float *value , char *dqd , STATUS *status )
    *
    *  Bill Mattison          GSC/TDL          HP 9000/7xx
    *
    *  FUNCTION DESCRIPTION
    *  ====================
    *
    *    This function gets a continuous real metar observation value and its
    *    data quality descriptor for a caller-specified station and time.  The
    *    value is from a "special", not a regualarly scheduled metar.
    *
    *    The station is specified by a long integer id number (from the
    *    station_location table in the hm database).  The observation element is
    *    specified by a long integer id number (from the hydromet_element table in
    *    the hm database.  The date/time of the special is specified by a string
    *    in "yyyy-mm-dd hh:mm" format.
    *
    *    The result is one scalar float value and a one-character data quality
    *    descriptor.  This function selects the value from the special's latest
    *    correction.  If there are no corrections, then this function selects
    *    the value from the special's latest transmission.
    *    
    *    If this function succeeds in finding the requested value, the return
    *    status is STATUS_OK (= 0).  If specials for the requested station and
    *    time are found, but the requested value is not found, the return
    *    status is STATUS_FAILURE (= 1).  If no specials for the requested
    *    station and time are found, the return status is NO_HITS (= 2006).  See
    *    POSSIBLE STATUS VALUES below for other possible status values and their
    *    meanings.
     *
     *
     * </pre>
     * 
     * @param informId
     * @param elementID
     *            ID key of element to look for
     * @param dateTime
     *            date/time to query for
     * @param dateTimeString
     *            date time string in format "yyyy-MM-dd hh24:mm".
     * @return the value and data quality descriptor for the given element, or
     *         the missing value (9999) and an empty descriptor if no records
     *         were found.
     * @throws ClimateQueryException
     */
    public FSSReportResult getSpeciConreal(int informId, int elementID,
            Date dateTime, String dateTimeString) throws ClimateQueryException {
        /*
         * Legacy documentation:
         * 
         * The specials available for the time of interest could include
         * multiple transmissions and corrections. So the corrections get
         * separated from the "originals"; and the SPECIs are sorted according
         * to the times they were constructed. The most recently constructed
         * correction, or the most recently constructed original if there are no
         * corrections, is the SPECI to use. No rows indicates no specials for
         * the time of interest. Rows with NULL element_value and dqd column
         * values indicate the field of interest was not reported in the SPECI.
         */
        /*
         * Change from porting: Instead of returning failed status, on a
         * legitimate DB exception throw the exception. On no returned data,
         * return missing value.
         */
        StringBuilder query = new StringBuilder(
                "SELECT f.correction, f.origin_dtime, ");
        query.append(" x.element_value, x.dqd FROM ");
        query.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
        query.append(" AS f LEFT OUTER JOIN (SELECT * FROM ");
        query.append(ClimateDAOValues.FSS_CONTIN_REAL_TABLE_NAME);
        query.append(" WHERE element_id = :elementID");
        query.append(") AS x USING (fss_rpt_instance) ");
        query.append(" WHERE f.valid_dtime = :dateTime");
        query.append(" AND f.station_id = :informId");
        query.append(" AND f.report_subtype = 'SPECI' ");
        query.append(" ORDER BY f.correction DESC, f.origin_dtime DESC");

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("elementID", elementID);
        paramMap.put("dateTime", toSqlDate(dateTime));
        paramMap.put("informId", informId);

        try {
            Object[] results = getDao().executeSQLQuery(query.toString(),
                    paramMap);
            if ((results != null) && (results.length >= 1)) {
                Object result = results[0];
                if (result instanceof Object[]) {
                    Object[] oa = (Object[]) result;
                    // correction is used only for sorting, could be null
                    // origin time is used only for sorting, could be null

                    double elementValue;
                    Object elementValueObj = oa[2];
                    // element value could be null
                    if (elementValueObj != null) {
                        elementValue = ((Number) elementValueObj).doubleValue();

                        // dqd could be null
                        String dqd;
                        Object dqdObj = oa[3];
                        if (dqdObj != null) {
                            dqd = (String) dqdObj;
                        } else {
                            logger.warn("DQD for query [" + query
                                    + "] and map: [" + paramMap
                                    + "] is null. Empty DQD will be used.");
                            dqd = "";
                        }

                        return new FSSReportResult(elementValue, dqd);
                    } else {
                        logger.warn("Element value for query [" + query
                                + "] and map: [" + paramMap
                                + "] is null. Returning missing value.");
                    }
                } else {
                    throw new ClimateQueryException(
                            "Unexpected return type from query, expected Object[], got "
                                    + result.getClass().getName());
                }

            } else {
                logger.warn("Couldn't find a report for report time "
                        + dateTimeString
                        + "Z. Missing value will be returned for query: ["
                        + query + "] and map: [" + paramMap + "]");
            }
        } catch (Exception e) {
            throw new ClimateQueryException(
                    "An error was encountered retrieving the report from"
                            + " the SPECI DB for " + dateTimeString + "Z.\n"
                            + "Error querying the climate database with query: ["
                            + query + "] and map: [" + paramMap + "]",
                    e);
        }

        return new FSSReportResult();
    }

    /**
     * Converted from get_metar_conreal.ecpp.
     * 
     * Original description:
     * 
     * <pre>
     * FILENAME: get_metar_conreal.ecpp
     *
     ******************************************************************************
     *
     * Bill Mattison GSC/TDL HP 9000/7xx
     *
     * FUNCTION DESCRIPTION ====================
     *
     * This function gets a continuous real METAR observation value and its data
     * quality descriptor for a caller-specified station and nominal hour. The
     * value is from a regularly scheduled METAR, not a "special" (SPECI).
     *
     * The station is specified by a long integer id number (from the
     * station_location table in the hm database). The observation element_id is
     * specified by a long integer (from the hydromet_element table in the hm
     * database). The nominal date/time is specified by a string in
     * "yyyy-mm-dd hh" format.
     *
     * This function will retrieve data for ONLY the earliest METAR valid time
     * in the nominal hour (from 15 minutes before the hour to 44 minutes past
     * the nominal hour). This is normally an issue only for stations that are
     * scheduled to take routine observations and transmit routine metars more
     * than once per hour. "AWOS" stations, which report METARs every 20 minutes
     * (e.g., 0, 20, and 40 minutes after each hour), are an example. quality
     * descriptor. This function selects the earliest valid time's latest
     * correction. If there are no corrections, then this function selects the
     * value from the earliest metar valid time's latest receipt. METARs with
     * later valid times in the nominal hour are ignored. See the function
     * get_metar_tempc_for_nominal for an example where all METAR valid times in
     * a nominal hour are considered.
     *
     * The normal result is one scalar integer value and a one-character data
     * quality descriptor.
     * 
     * If this function succeeds in finding the requested value, the return
     * status is STATUS_OK (= 0). If metars for the requested station, time, and
     * type are found, but the requested value is not found, the return status
     * is STATUS_FAILURE (= 1). If no metars for the requested station, time,
     * and type are found, the return status is NO_HITS. See POSSIBLE STATUS
     * VALUES below for other possible status values and their meanings.
     *
     * </pre>
     * 
     * @param informId
     * @param elementID
     *            ID key of element to look for
     * @param dateTime
     *            date/time to query for
     * @param dateTimeString
     *            date time string in format "yyyy-MM-dd hh24:mm".
     * @param includeSPECI
     *            true to include SPECI observations
     * @return the value and data quality descriptor for the given element, or
     *         the missing value (9999) and an empty descriptor if no records
     *         were found.
     * @throws ClimateQueryException
     */
    public List<FSSReportResult> getMetarConreal(int informId, int elementID,
            Calendar dateTime, boolean includeSPECI)
            throws ClimateQueryException {
        /*
         * Legacy documentation:
         * 
         * Look at METARs for the requested station and hour. Take the earliest
         * valid time for the nominal hour, take any corrections for this report
         * over its original version, and sort lastly according to the time the
         * reports were ingested to get the most recent correction. From this
         * one report only (defined by its fss_rpt_instance in the fss_report
         * table), try to get the element value and its dqd from the subservient
         * data table, fss_contin_real.
         * 
         * fss_report LEFT OUTER JOIN fss_contin_real USING (fss_rpt_instance)
         * forms a table with the rows of the LEFT table fss_report and columns
         * corresponding to the union of the set of columns of table fss_report
         * and the set of columns of table fss_contin_real. The rows of the two
         * tables are matched by the value of fss_rpt_instance. The
         * fss_contin_real columns of the new table are left blank if there is
         * no match. No METAR corresponds to a query status of 'NO DATA' or
         * 'NO_HITS'. METAR existing, but NO or BAD element value corresponds to
         * a query status of 'STATUS_FAILURE'.
         */
        /*
         * Change from porting: Instead of returning failed status, on a
         * legitimate DB exception throw the exception. On no returned data,
         * return missing value.
         */
        String dateTimeString = ClimateDate.getFullDateTimeFormat()
                .format(dateTime.getTime());
        StringBuilder query = new StringBuilder(
                "SELECT f.valid_dtime, f.origin_dtime, f.correction, ");
        query.append(" x.element_value, x.dqd FROM ");
        query.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
        query.append(" AS f LEFT OUTER JOIN (SELECT * FROM ");
        query.append(ClimateDAOValues.FSS_CONTIN_REAL_TABLE_NAME);
        query.append(" WHERE element_id = :elementID");
        query.append(") AS x USING (fss_rpt_instance) ");
        query.append("WHERE f.nominal_dtime = :dateTime");
        query.append(" AND f.station_id = :informId");
        query.append(" AND (f.report_subtype = 'MTR' ");
        if (includeSPECI) {
            query.append(" OR f.report_subtype = 'SPECI') ");
        } else {
            query.append(") ");
        }
        query.append(" ORDER BY f.valid_dtime ASC, f.correction DESC, ");
        query.append(" f.origin_dtime DESC");

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("elementID", elementID);
        paramMap.put("dateTime", dateTime);
        paramMap.put("informId", informId);

        try {
            Object[] results = getDao().executeSQLQuery(query.toString(),
                    paramMap);
            if ((results != null) && (results.length >= 1)) {

                List<FSSReportResult> resultList = new ArrayList<FSSReportResult>();

                for (Object result : results) {
                    if (result instanceof Object[]) {
                        Object[] oa = (Object[]) result;
                        // valid time used only for sorting, cannot be null
                        // correction is used only for sorting, could be null
                        // origin time is used only for sorting, could be null

                        double elementValue;
                        Object elementValueObj = oa[3];
                        // element value could be null
                        if (elementValueObj != null) {
                            elementValue = ((Number) elementValueObj)
                                    .doubleValue();

                            // dqd could be null
                            String dqd;
                            Object dqdObj = oa[4];
                            if (dqdObj != null) {
                                dqd = (String) dqdObj;
                            } else {
                                logger.warn("DQD for query [" + query
                                        + "] and map: [" + paramMap
                                        + "] is null. Empty DQD will be used.");
                                dqd = "";
                            }

                            resultList.add(
                                    new FSSReportResult(elementValue, dqd));

                        } else {
                            logger.warn("Element value for query [" + query
                                    + "] and map: [" + paramMap
                                    + "] is null. Returning missing value.");
                        }
                    } else {
                        throw new ClimateQueryException(
                                "Unexpected return type from query, expected Object[], got "
                                        + result.getClass().getName());
                    }
                }
                return resultList;
            } else {
                logger.warn("Couldn't find a report for report time "
                        + dateTimeString
                        + "Z. Missing value will be returned for query: ["
                        + query + "] and map: [" + paramMap + "]");
            }
        } catch (Exception e) {
            throw new ClimateQueryException(
                    "An error was encountered retrieving the report from"
                            + " the METAR DB for " + dateTimeString + "Z.\n"
                            + "Error querying the climate database with query: ["
                            + query + "] and map: [" + paramMap + "]",
                    e);
        }

        return new ArrayList<FSSReportResult>();
    }

    /**
     * Overload for the above. Does not include SPECI observations.
     * 
     * @param informId
     * @param elementID
     * @param dateTime
     * @return
     * @throws ClimateQueryException
     */
    public FSSReportResult getMetarConreal(int informId, int elementID,
            Calendar dateTime) throws ClimateQueryException {
        List<FSSReportResult> result = getMetarConreal(informId, elementID,
                dateTime, false);

        if (!result.isEmpty()) {
            return result.get(0);
        } else {
            return new FSSReportResult();
        }
    }

    /**
     * Inner class to wrap multiple return types from FSS-related queries.
     * 
     * <pre>
     * 
     * SOFTWARE HISTORY
     * 
     * Date         Ticket#    Engineer    Description
     * ------------ ---------- ----------- --------------------------
     * 11 OCT 2016  21378      amoore      Initial creation
     * 03 NOV 2016  21378      amoore      More generic naming.
     * 
     * </pre>
     * 
     * @author amoore
     * @version 1.0
     */
    public static class FSSReportResult {

        /**
         * Element value result.
         */
        private final double value;

        /**
         * Data quality descriptor result.
         */
        private final String dqd;

        /**
         * Empty constructor. Assign missing values.
         */
        private FSSReportResult() {
            value = ParameterFormatClimate.MISSING;
            dqd = "";
        }

        /**
         * Constructor.
         */
        private FSSReportResult(double iValue, String iDqd) {
            value = iValue;
            dqd = iDqd;
        }

        /**
         * @return the value
         */
        public double getValue() {
            return value;
        }

        /**
         * @return the dqd
         */
        public String getDqd() {
            return dqd;
        }

        /**
         * @return true if the element value is set to missing.
         */
        public boolean isMissing() {
            return value == ParameterFormatClimate.MISSING;
        }
    }

    /**
     * 
     * Wrapper class for various results from building daily temperature
     * observation for a period.
     * 
     * <pre>
     * SOFTWARE HISTORY
     * 
     * Date         Ticket#    Engineer    Description
     * ------------ ---------- ----------- --------------------------
     * 10 NOV 2016  21378      amoore      Initial creation.
     * </pre>
     * 
     * @author amoore
     * @version 1.0
     */
    public static class ExtremeTempPeriodResult {

        /**
         * Time of extreme temperature.
         */
        private final ClimateTime time;

        /**
         * Extreme temperature.
         */
        private final float temp;

        /**
         * METAR or QC flag.
         */
        private final int flag;

        /**
         * Constructor for missing values.
         */
        public ExtremeTempPeriodResult() {
            time = ClimateTime.getMissingClimateTime();
            temp = R_MISS;
            flag = I_MISS;
        }

        /**
         * Set time to missing.
         * 
         * @param iTemp
         * @param iFlag
         */
        public ExtremeTempPeriodResult(float iTemp, int iFlag) {
            time = ClimateTime.getMissingClimateTime();
            temp = iTemp;
            flag = iFlag;
        }

        /**
         * 
         * @param iTime
         * @param iTemp
         * @param iFlag
         */
        public ExtremeTempPeriodResult(ClimateTime iTime, float iTemp,
                int iFlag) {
            time = iTime;
            temp = iTemp;
            flag = iFlag;
        }

        /**
         * @return true if temp or qc flag is the missing value.
         */
        public boolean isMissing() {
            return temp == R_MISS || flag == I_MISS;
        }

        /**
         * @return the time
         */
        public ClimateTime getTime() {
            return time;
        }

        /**
         * @return the temp
         */
        public float getTemp() {
            return temp;
        }

        /**
         * @return the METAR or QC flag
         */
        public int getFlag() {
            return flag;
        }
    }

    /**
     * Migrated from get_all_speci_winds.ec.
     * 
     * <pre>
     *Name:
     *       get_all_speci_winds.ec
     *       GFS1-NHD:A6742.0000-SRC;22
     *
     *       void get_all_speci_winds  (  climate_date  begin_date,
     *                                    climate_time  begin_time,
     *                    climate_date  end_date,
     *                                    climate_time  end_time,
     *                    long      *station_id,
     *                    climate_wind  *speci_wind,
     *                    climate_time      *speci_time,
     *                                    int               *windgust)
     *
     *   Jason Tuell        PRC/TDL             HP 9000/7xx
     *   Dan Zipper                 PRC/TDL
     *
     *   FUNCTION DESCRIPTION
     *   ====================
     *
     *  This function retrieves all special metars withing a specified time period of
     *      up to 24 hours. The routine will return the highest reported wind speed and direction
     *      of the special metars along with its time of occurance.
     *
     * 
     * </pre>
     * 
     * @param window
     * @param informId
     * @param wind
     * @param windTime
     * @param windOrGust
     *            false for gust, true for wind.
     * @throws ClimateQueryException
     */

    public void getAllSpeciWinds(ClimateDates window, int informId,
            ClimateWind wind, ClimateTime windTime, boolean windOrGust)
            throws ClimateQueryException {
        Calendar beginCal = combine(window.getStart(), window.getStartTime());
        String beginDateTimeString = window.getStart().toFullDateString() + " "
                + window.getStartTime().toHourMinString();
        Calendar endCal = combine(window.getEnd(), window.getEndTime());
        String endDateTimeString = window.getEnd().toFullDateString() + " "
                + window.getEndTime().toHourMinString();

        StringBuilder query = new StringBuilder(
                "SELECT DISTINCT valid_dtime FROM ");
        query.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
        query.append(" WHERE station_id = :informId");
        query.append(" AND report_subtype = 'SPECI' ");
        query.append(" AND nominal_dtime BETWEEN :beginDateTime");
        query.append(" AND :endDateTime");

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("informId", informId);
        paramMap.put("beginDateTime", beginCal);
        paramMap.put("endDateTime", endCal);

        try {
            Object[] results = getDao().executeSQLQuery(query.toString(),
                    paramMap);
            if ((results != null) && (results.length >= 1)) {
                for (Object result : results) {

                    if (result instanceof Date) {
                        Date oa = (Date) result;
                        try {
                            String validDTimeString = ClimateDate
                                    .getFullDateTimeFormat().format(oa);

                            float currSpeed;

                            if (windOrGust) {
                                /* get SPECI max wind */
                                currSpeed = (float) getSpeciConreal(informId,
                                        MetarUtils.METAR_WIND_SPEED, oa,
                                        validDTimeString).getValue();
                            } else {
                                /* get SPECI max gust */
                                currSpeed = (float) getSpeciConreal(informId,
                                        MetarUtils.METAR_MAX_WIND_GUST, oa,
                                        validDTimeString).getValue();
                            }

                            /*
                             * If a metar wind speed was found, a check is done
                             * to see whether it is greater than currently
                             * stored speed (or currently stored speed is
                             * missing).
                             */
                            if ((wind
                                    .getSpeed() == ParameterFormatClimate.MISSING_SPEED)
                                    || (currSpeed > wind.getSpeed())) {
                                // check against different bounds based on flag
                                if (!((!windOrGust
                                        && ((currSpeed > ParameterBounds.WIND_SPD_UPPER_BOUND)
                                                || (currSpeed < ParameterBounds.WIND_SPD_LOWER_BOUND)))
                                        || (windOrGust
                                                && ((currSpeed > ParameterBounds.SPECI_UPPER_BOUND_QC)
                                                        || (currSpeed < ParameterBounds.SPECI_LOWER_BOUND_QC))))) {
                                    /*
                                     * The speci's wind speed was determined to
                                     * be the highest so far, so get and check
                                     * the speci's wind direction
                                     */
                                    int direction = (int) getSpeciCategSingle(
                                            informId,
                                            MetarUtils.METAR_WIND_DIRECTION, oa,
                                            validDTimeString).getValue();

                                    if ((direction < ParameterBounds.WIND_DIR_LOWER_BOUND)
                                            || (direction > ParameterBounds.WIND_DIR_UPPER_BOUND)) {
                                        logger.warn("Wind direction ["
                                                + direction
                                                + "] is outside of valid bounds for datetime ["
                                                + validDTimeString
                                                + "] with station ID: ["
                                                + informId
                                                + "]. Skipping record.");
                                        continue;
                                    } else {
                                        /*
                                         * The speci's wind speed was determined
                                         * to be the highest so far so the time
                                         * of the speci is calculated. The data
                                         * is then pointed to and stored in the
                                         * structure
                                         */
                                        wind.setSpeed(currSpeed);
                                        wind.setDir(direction);

                                        Calendar cal = TimeUtil.newCalendar();
                                        cal.setTime(ClimateDate
                                                .getFullDateTimeFormat()
                                                .parse(validDTimeString));

                                        windTime.setHour(
                                                cal.get(Calendar.HOUR_OF_DAY));
                                        windTime.setMin(
                                                cal.get(Calendar.MINUTE));
                                    }
                                } else {
                                    logger.warn("Speed [" + currSpeed
                                            + "] is outside of valid bounds for datetime ["
                                            + validDTimeString
                                            + "] with station ID: [" + informId
                                            + "]. Skipping record.");
                                    continue;
                                }
                            }
                        } catch (ClimateQueryException e) {
                            logger.error("Error querying SPECI.", e);
                            continue;
                        } catch (NullPointerException e) {
                            throw new ClimateQueryException(
                                    "Unexpected null result with query: ["
                                            + query + "] and map: [" + paramMap
                                            + "].",
                                    e);
                        } catch (Exception e) {
                            // if casting failed
                            throw new ClimateQueryException(
                                    "Unexpected return column type.", e);
                        }

                    } else {
                        throw new ClimateQueryException(
                                "Unexpected return type from query, expected java.util.Date, got "
                                        + result.getClass().getName());
                    }
                }
            } else {
                wind.setDataToMissing();
                windTime.setDataToMissing();

                logger.info("For station ID [" + informId
                        + "], no SPECI reports found between ["
                        + beginDateTimeString + "] and [" + endDateTimeString
                        + "]. Zero row count returned for query: [" + query
                        + "] and map: [" + paramMap + "]");
            }
        } catch (Exception e) {
            throw new ClimateQueryException(
                    "An error was encountered retrieving times from"
                            + " the FSS Reports.\n"
                            + "Error querying the climate database with query: ["
                            + query + "] and map: [" + paramMap + "]",
                    e);
        }
    }

    /**
     * Migrated from get_hourly_winds.c.
     * 
     * <pre>
     * void get_hourly_winds (   climate_date   *this_date,
     *                            climate_time  *now_time,
     *                long      *station_id,
     *                climate_wind  *result          )
     *
     *   Jason Tuell        PRC/TDL             HP 9000/7xx
     *   Dan Zipper                 PRC/TDL
     *
     *   FUNCTION DESCRIPTION
     *   ====================
     *
     *  This function retrieves up to 24 hours of hourly wind 
     *      directions and speeds from which the resultant wind is calculated.
     *
     * </pre>
     * 
     * @param date
     * @param time
     * @param informId
     * @param aWind
     */

    public void getHourlyWinds(ClimateDate date, ClimateTime time, int informId,
            ClimateWind aWind) {
        Calendar dateTime = combine(date, time);
        String dateTimeString = date.toFullDateString() + " "
                + time.toHourMinString();

        /*
         * Legacy documentation:
         * 
         * Changing the logic on Feb 2003 for OB2. If no valid wind speed found,
         * set all output variable values to missing, don't even try to retrieve
         * direction. Ditto if anything but STATUS_OK on first retrieval
         * attempt.
         */
        try {
            float windSpeed = (float) getMetarConreal(informId,
                    MetarUtils.METAR_WIND_SPEED, dateTime).getValue();

            /*
             * If the retrieved wind speed is bad, set it and the direction to
             * missing and we are done.
             */
            if ((windSpeed < ParameterBounds.WIND_SPD_LOWER_BOUND)
                    || (windSpeed > ParameterBounds.WIND_SPD_UPPER_BOUND)) {
                logger.info("For station id [" + informId
                        + "], the wind speed failed QC for the datetime ["
                        + dateTimeString + "]. Gust will be set to missing.");
                aWind.setDataToMissing();
            } else {
                aWind.setSpeed(windSpeed);

                /* We look for direction only if we have a good speed value. */
                try {
                    int direction = (int) getMetarCategSingle(informId,
                            MetarUtils.METAR_WIND_DIRECTION, dateTime,
                            dateTimeString).getValue();

                    if ((direction < ParameterBounds.WIND_DIR_LOWER_BOUND)
                            || (direction > ParameterBounds.WIND_DIR_UPPER_BOUND)) {
                        logger.info("For station id [" + informId
                                + "], the wind direction failed QC for the datetime ["
                                + dateTimeString
                                + "]. It will be set to missing for the report.");
                        aWind.setDir(ParameterFormatClimate.MISSING);
                    } else {
                        aWind.setDir(direction);
                    }
                } catch (ClimateQueryException e) {
                    logger.error(
                            "Error getting wind information through METAR conreal. Wind direction will be set to missing.");
                    aWind.setDir(ParameterFormatClimate.MISSING);
                }
            }
        } catch (ClimateQueryException e) {
            logger.error(
                    "Error getting wind information through METAR conreal. Wind will be set to missing.",
                    e);
            aWind.setDataToMissing();
        }
    }

    /**
     * Migrated from get_all_hourly_gusts.c.
     * 
     * <pre>
     * void get_all_hourly_gusts (  climate_date    this_date,
     *                               climate_time   now_time,
     *               long       *station_id,
     *               climate_wind   *a_gust,
     *                               climate_time   *a_time  )
     *
     *   Jason Tuell        PRC/TDL             HP 9000/7xx
     *   Dan Zipper                 PRC/TDL
     *
     *
     *   FUNCTION DESCRIPTION
     *   ====================
     *
     *  This function retrieves up to 24 hours of hourly wind 
     *      directions and speeds from which the resultant wind is calculated. The call is inside of
     *      a FORTRAN loop and it will return one gust speed, direction, and time for the current hour 
     *      which is passed into the program from the structure "now_time".
     *
     *
     * </pre>
     * 
     * @param date
     * @param time
     * @param informId
     * @param aGust
     */

    public void getAllHourlyGusts(ClimateDate date, ClimateTime time,
            int informId, ClimateWind aGust) {
        Calendar dateTime = combine(date, time);
        String dateTimeString = date.toFullDateString() + " "
                + time.toHourMinString();

        /*
         * Legacy documentation:
         * 
         * This function will retrieve the max_wind gust reported during a
         * single hour Using the max_db_status as an error check of the INFORMIX
         * database, it will check to see the data is missing, not reported, or
         * if there was another type of error. Once the error status has been
         * determined, a quality check done on the data to see whether or not
         * the data is legitimate.
         */
        try {
            float gustSpeed = (float) getMetarConreal(informId,
                    MetarUtils.METAR_MAX_WIND_GUST, dateTime).getValue();

            /*
             * If the retrieved gust speed is bad, set it and the direction to
             * missing and we are done.
             */
            if ((gustSpeed < ParameterBounds.WIND_SPD_LOWER_BOUND)
                    || (gustSpeed > ParameterBounds.WIND_SPD_UPPER_BOUND)) {
                logger.info("For station id [" + informId
                        + "], the wind gust speed failed QC for the datetime ["
                        + dateTimeString + "]. Gust will be set to missing.");
                aGust.setDataToMissing();
            } else {
                aGust.setSpeed(gustSpeed);

                /*
                 * If the METAR exists and valid gusts were reported, retrieve
                 * the 2-minute wind's direction for use as the wind gust
                 * direction
                 */
                try {
                    int direction = (int) getMetarCategSingle(informId,
                            MetarUtils.METAR_WIND_DIRECTION, dateTime,
                            dateTimeString).getValue();

                    if ((direction < ParameterBounds.WIND_DIR_LOWER_BOUND)
                            || (direction > ParameterBounds.WIND_DIR_UPPER_BOUND)) {
                        logger.info("For station id [" + informId
                                + "], the wind gust direction failed QC for the datetime ["
                                + dateTimeString
                                + "]. It will be set to missing for the report.");
                        aGust.setDir(ParameterFormatClimate.MISSING);
                    } else {
                        aGust.setDir(direction);
                    }
                } catch (ClimateQueryException e) {
                    logger.error(
                            "Error getting gust information through METAR conreal. Gust direction will be set to missing.");
                    aGust.setDir(ParameterFormatClimate.MISSING);
                }
            }
        } catch (ClimateQueryException e) {
            logger.error(
                    "Error getting gust information through METAR conreal. Gust will be set to missing.",
                    e);
            aGust.setDataToMissing();
        }
    }

    /**
     * Migrated from get_hourly_peak_winds.c.
     * 
     * <pre>
     * void get_hourly_peak_winds (  climate_date   this_date,
     *                                climate_time  now_time,
     *                long      *station_id,
     *                climate_wind  *a_pk_wind,
     *                    climate_time  *a_pk_wind_time)
     *
     *   Jason Tuell        PRC/TDL             HP 9000/7xx
     *   Dan Zipper                 PRC/TDL
     *
     *   FUNCTION DESCRIPTION
     *   ====================
     *
     *  This function retrieves up to 24 hours of hourly wind 
     *      directions and speeds from which the resultant wind is calculated.
     *
     * </pre>
     * 
     * @param date
     * @param time
     * @param informId
     * @param peakWind
     * @param peakWindTime
     */

    public void getHourlyPeakWinds(ClimateDate date, ClimateTime time,
            int informId, ClimateWind peakWind, ClimateTime peakWindTime) {
        Calendar dateTime = combine(date, time);
        String dateTimeString = date.toFullDateString() + " "
                + time.toHourMinString();

        /*
         * Legacy documentation:
         * 
         * This function will retrieve the peak wind speed reported during a
         * single hour Using the max_db_status as an error check of the INFORMIX
         * database, it will check to see the data is missing, not reported, or
         * if there was another type of error. Once the error status has been
         * determined, a quality check done on the data to see whether or not
         * the data is legitimate.
         * 
         * Changing the logic on Feb 2003 for OB2. If no valid peak wind speed
         * found, set all output variable values to missing, don't even try to
         * retrieve dir or time. Ditto if anything but STATUS_OK on first
         * retrieval attempt.
         */
        try {
            float windSpeed = (float) getMetarConreal(informId,
                    MetarUtils.METAR_PEAK_WIND_SPEED, dateTime).getValue();

            /*
             * If the retrieved peak wind speed is bad, set it and the direction
             * to missing and we are done.
             */
            if ((windSpeed < ParameterBounds.WIND_SPD_LOWER_BOUND)
                    || (windSpeed > ParameterBounds.WIND_SPD_UPPER_BOUND)) {
                logger.info("For station id [" + informId
                        + "], the peak wind speed failed QC for the datetime ["
                        + dateTimeString
                        + "]. Peak wind will be set to missing.");
                peakWind.setDataToMissing();
            } else {
                peakWind.setSpeed(windSpeed);

                /*
                 * Good speed, so retrieve the peak wind direction and do a
                 * quality check on the data to see whether or not the data is
                 * legitimate.
                 */
                try {
                    int direction = (int) getMetarCategSingle(informId,
                            MetarUtils.METAR_PEAK_WIND_DIR, dateTime,
                            dateTimeString).getValue();

                    if ((direction < ParameterBounds.WIND_DIR_LOWER_BOUND)
                            || (direction > ParameterBounds.WIND_DIR_UPPER_BOUND)) {
                        logger.info("For station id [" + informId
                                + "], the peak wind direction failed QC for the datetime ["
                                + dateTimeString
                                + "]. It will be set to missing for the report.");
                        peakWind.setDir(ParameterFormatClimate.MISSING);
                    } else {
                        peakWind.setDir(direction);
                    }

                    /*
                     * Now try to retrieve the peak wind time and do a quality
                     * check on the data to see whether or not the data is
                     * legitimate.
                     */
                    try {
                        // time stored in HHmm format as integer (not actual
                        // time datatype)
                        int metarTime = (int) getMetarConreal(informId,
                                MetarUtils.METAR_PEAK_WIND_TIME, dateTime)
                                        .getValue();

                        if ((metarTime < ParameterBounds.PEAK_TIME_LOWER_BOUND_QC)
                                || (metarTime > ParameterBounds.PEAK_TIME_UPPER_BOUND_QC)) {
                            logger.info("For station id [" + informId
                                    + "], the peak wind time failed QC for the datetime ["
                                    + dateTimeString
                                    + "]. It will be set to missing for the report.");
                            peakWindTime.setDataToMissing();
                        } else {
                            peakWindTime.setHour(metarTime / 100);
                            peakWindTime.setMin(metarTime % 100);
                        }
                    } catch (ClimateQueryException e) {
                        logger.error(
                                "Error getting peak wind information through METAR conreal. Peak wind time will be set to missing.");
                        peakWindTime.setDataToMissing();
                    }
                } catch (ClimateQueryException e) {
                    logger.error(
                            "Error getting peak wind information through METAR conreal. Peak wind direction will be set to missing.");
                    peakWind.setDir(ParameterFormatClimate.MISSING);
                }
            }
        } catch (ClimateQueryException e) {
            logger.error(
                    "Error getting peak wind information through METAR conreal. Peak wind will be set to missing.",
                    e);
            peakWind.setDataToMissing();
            peakWindTime.setDataToMissing();
        }
    }

    /**
     * Migrated from compute_daily_snow.ec.
     * 
     * <pre>
     * FILENAME:            compute_daily_snow.c
     * FILE DESCRIPTION:
     * NUMBER OF MODULES:   2
     * GENERAL INFORMATION:
     *       MODULE 1:      compute_daily_snow
     *       DESCRIPTION:   Computes the snowfall amount for a station
     *                      using snow reports gleaned from Supplemental Climate
     *                      Data (SCD).
     *       MODULE 2:      get_SCD_snow
     *       DESCRIPTION:   Retrieves a SCD snow amount from the HM database 
     *                      based upon a user-inputted time range.
     *
     * MODULE NUMBER: 2
     * MODULE NAME:   get_SCD_snow
     * PURPOSE: This routine retrieves snowfall information from a SCD report for a
     * specified station id. The station id is a numeric identifier as defined
     * in the station_location table. An upper and a lower time bound are passed
     * to this routine to indicate the time period over which to search for 
     * a SCD. According to the rules put forth in the National Weather Service
     * Observing Handbook No.7, Part IV, Supplementary Observations, SCDs should 
     * be transmitted at the 6 hour synoptic hours (00, 06, 12, and 18) when 
     * it comes to reporting snowfall amounts. For any one of these synoptic hours,
     * the SCD should be generated in a 20 minute window centered on the top of the
     * hour. 
     *
     * SCD reports can have corrections. This routine takes the most recent SCD
     * report (in the event of multiple transmissions of a single report) or the
     * most recent correction (in the case of a single SCD report followed by 
     * several corrections).
     *
     * If no SCD reports are found then this routine returns a status of NO_HITS.
     * 
     * </pre>
     * 
     * @param informId
     *            station ID.
     * @param lowerTimeString
     *            lower bound datetime string in form yyyy-MM-dd hh24:mm.
     * @param upperTimeString
     *            upper bound datetime string in form yyyy-MM-dd hh24:mm.
     * @return
     * @throws ClimateQueryException
     */

    public FSSReportResult getSCDSnow(int informId, Calendar lowerTimeCal,
            Calendar upperTimeCal) throws ClimateQueryException {
        String lowerTimeString = ClimateDate.getFullDateTimeFormat()
                .format(lowerTimeCal.getTime());
        String upperTimeString = ClimateDate.getFullDateTimeFormat()
                .format(upperTimeCal.getTime());
        StringBuilder query = new StringBuilder(
                "SELECT fss_rpt_instance, origin_dtime, correction FROM ");
        query.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
        query.append(" WHERE valid_dtime >= :lowerTime");
        query.append(" AND valid_dtime <= :upperTime");
        query.append(" AND station_id = :informId");
        query.append(" AND report_type = 'SCD' ");
        query.append(" ORDER BY correction DESC, origin_dtime DESC");

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("lowerTime", lowerTimeCal);
        paramMap.put("upperTime", upperTimeCal);
        paramMap.put("informId", informId);

        try {
            Object[] results = getDao().executeSQLQuery(query.toString(),
                    paramMap);
            if ((results != null) && (results.length >= 1)) {
                Object result = results[0];
                if (result instanceof Object[]) {
                    Object[] oa = (Object[]) result;
                    try {
                        int fssRptInstance = ((Number) oa[0]).intValue();

                        // origin_dtime used only for sorting, and could be null
                        // correction used only for sorting, and could be null

                        /*
                         * does the metar include a value for the requested
                         * observation?
                         */
                        StringBuilder innerQuery = new StringBuilder(
                                "SELECT element_value, dqd FROM ");
                        innerQuery.append(
                                ClimateDAOValues.FSS_CONTIN_REAL_TABLE_NAME);
                        innerQuery.append(
                                " WHERE fss_rpt_instance = :fssRptInstance");
                        innerQuery.append(" AND element_id = ")
                                .append(MetarUtils.SCD_SNOW);

                        Map<String, Object> innerParamMap = new HashMap<>();
                        innerParamMap.put("fssRptInstance", fssRptInstance);

                        try {
                            Object[] innerResults = getDao().executeSQLQuery(
                                    innerQuery.toString(), innerParamMap);
                            if ((innerResults != null)
                                    && (innerResults.length == 1)) {
                                Object innerResult = innerResults[0];
                                if (innerResult instanceof Object[]) {
                                    Object[] innerOa = (Object[]) innerResult;
                                    double value = ((Number) innerOa[0])
                                            .doubleValue();

                                    // dqd could be null
                                    String dqd;
                                    Object dqdObj = innerOa[1];
                                    if (dqdObj != null) {
                                        dqd = (String) dqdObj;
                                    } else {
                                        logger.warn("DQD for query ["
                                                + innerQuery + "] and map: ["
                                                + innerParamMap
                                                + "] is null. Empty DQD will be used.");
                                        dqd = "";
                                    }

                                    return new FSSReportResult(value, dqd);
                                } else {
                                    throw new ClimateQueryException(
                                            "Unexpected return type from query, expected Object[], got "
                                                    + innerResult.getClass()
                                                            .getName());
                                }

                            } else if ((innerResults != null)
                                    && (innerResults.length > 1)) {
                                logger.error(
                                        "Found multiple returns for query: ["
                                                + innerQuery + "] and map: ["
                                                + innerParamMap
                                                + "]. Expected at most one return.");
                            } else {
                                logger.warn(
                                        "Couldn't find a report for report instance "
                                                + fssRptInstance
                                                + "Missing value will be returned.");
                            }
                        } catch (Exception e) {
                            throw new ClimateQueryException(
                                    "An error was encountered retrieving the report from"
                                            + " the METAR DB for report instance "
                                            + fssRptInstance + ".\n"
                                            + "Error querying the climate database with query: ["
                                            + innerQuery + "] and map: ["
                                            + innerParamMap + "]",
                                    e);
                        }
                    } catch (ClimateQueryException e) {
                        throw new ClimateQueryException(
                                "Unexpected error with inner query", e);
                    }
                } else {
                    throw new ClimateQueryException(
                            "Unexpected return type from query, expected Object[], got "
                                    + result.getClass().getName());
                }

            } else {
                logger.warn("Couldn't find a report for report time "
                        + lowerTimeString + "Z to " + upperTimeString
                        + "Z. Missing value will be returned for query: ["
                        + query + "] and map: [" + paramMap + "]");
            }
        } catch (Exception e) {
            throw new ClimateQueryException(
                    "An error was encountered retrieving the report from"
                            + " the METAR DB for " + lowerTimeString + "Z to "
                            + upperTimeString + "Z.\n"
                            + "Error querying the climate database with query: ["
                            + query + "] and map: [" + paramMap + "]",
                    e);
        }

        return new FSSReportResult(ParameterFormatClimate.MISSING_SNOW, "");
    }

    /**
     * Migrated from compute_daily_snow.ec.
     * 
     * <pre>
     * FILENAME:            compute_daily_snow.c
     * FILE DESCRIPTION:
     * NUMBER OF MODULES:   2
     * GENERAL INFORMATION:
     *       MODULE 1:      compute_daily_snow
     *       DESCRIPTION:   Computes the snowfall amount for a station
     *                      using snow reports gleaned from Supplemental Climate
     *                      Data (SCD).
     *       MODULE 2:      get_SCD_snow
     *       DESCRIPTION:   Retrieves a SCD snow amount from the HM database 
     *                      based upon a user-inputted time range.
     *
     * MODULE NUMBER: 3
     * MODULE NAME:   get_correction
     * PURPOSE: This routine retrieves the correction information for the 
     * METAR report. Mainly, looking to see if the METAR report is an unedited
     * automated report for use in snow depth assumption/value.
     *
     * </pre>
     * 
     * @param informId
     *            station ID.
     * @param nominalTime
     *            nominal date/time to query for
     * @return Correction character (as String), empty string if row exists
     *         matching criteria but correction is null, or null if no row in DB
     *         matches criteria.
     * @throws ClimateQueryException
     */

    public String getCorrection(int informId, Calendar nominalTime)
            throws ClimateQueryException {
        String nominalTimeString = ClimateDate.getFullDateTimeFormat()
                .format(nominalTime.getTime());
        StringBuilder query = new StringBuilder(
                "SELECT correction, origin_dtime FROM ");
        query.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
        query.append(" WHERE nominal_dtime = :nominalTime");
        query.append(" AND station_id = :informId");
        query.append(" AND report_subtype = 'MTR' ");
        query.append(" ORDER BY correction DESC, origin_dtime DESC");

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("nominalTime", nominalTime);
        paramMap.put("informId", informId);

        try {
            Object[] results = getDao().executeSQLQuery(query.toString(),
                    paramMap);
            if ((results != null) && (results.length >= 1)) {
                Object result = results[0];
                if (result instanceof Object[]) {
                    Object[] oa = (Object[]) result;
                    // correction could be null
                    Object correctionObj = oa[0];
                    if (correctionObj != null) {
                        return (String) correctionObj;
                    } else {
                        logger.warn("Correction for query [" + query
                                + "] and map: [" + paramMap + "] is null. "
                                + "Returning empty string for correction.");
                        return "";
                    }

                    // origin_dtime used only for sorting, could be null
                } else {
                    throw new ClimateQueryException(
                            "Unexpected return type from query, expected Object[], got "
                                    + result.getClass().getName());
                }

            } else {
                logger.warn("Couldn't find a report for report time "
                        + nominalTimeString
                        + "Z. Null will be returned for correction value for query: ["
                        + query + "] and map: [" + paramMap + "]");
                return null;
            }
        } catch (Exception e) {
            throw new ClimateQueryException(
                    "An error was encountered retrieving the report from"
                            + " the METAR DB for " + nominalTimeString + "Z.\n"
                            + "Error querying the climate database with query: ["
                            + query + "] and map: [" + paramMap + "]",
                    e);
        }
    }

    /**
     * Migrated from build_daily_obs_temp.ec.
     * 
     * <pre>
     * MODULE NUMBER:   8
     * MODULE NAME:     get_pd_metar_max
     * PURPOSE:         This routine determines the maximum temperature in a 
     *                  user-defined period of METAR observations.
     *
     * </pre>
     * 
     * @param informId
     *            station ID.
     * @param beginTimeMilli
     * @param endTimeMilli
     * @return
     */

    public ExtremeTempPeriodResult getPeriodMetarMax(int informId,
            long beginTimeMilli, long endTimeMilli) {
        /*
         * Contains the number of real reports retrieved... This is crucial in
         * determining whether to return an exit status of NO_HITS (absolutely
         * no METAR reports found) or an exit status of STATUS_OK (at least a
         * few METAR reports were found this period).
         */
        int numReports = 0;
        float maxTemp = -9999;
        int flag = I_MISS;
        ClimateTime maxTime = ClimateTime.getMissingClimateTime();

        /*
         * get each metar's temperature; check against max temp found thus far
         */
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("informId", informId);

        for (long nominalMilliTicks = beginTimeMilli; nominalMilliTicks <= endTimeMilli; nominalMilliTicks += TimeUtil.MILLIS_PER_HOUR) {
            Calendar nominalCal = TimeUtil.newCalendar();
            nominalCal.setTimeInMillis(nominalMilliTicks);
            String nominalTimeString = ClimateDate.getFullDateTimeFormat()
                    .format(nominalCal.getTime());

            StringBuilder query = new StringBuilder(
                    "SELECT DISTINCT valid_dtime FROM ");
            query.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
            query.append(" WHERE station_id = :informId");
            query.append(" AND report_subtype = 'MTR' ");
            query.append(" AND nominal_dtime = :nominalTime");

            paramMap.put("nominalTime", nominalCal);

            try {
                Object[] results = getDao().executeSQLQuery(query.toString(),
                        paramMap);
                if ((results != null) && (results.length >= 1)) {
                    for (Object result : results) {
                        if (result instanceof Date) {
                            try {
                                Date resultDate = (Date) result;
                                String validTime = ClimateDate
                                        .getFullDateTimeFormat()
                                        .format(resultDate);
                                /*
                                 * get each metar's temperature; check against
                                 * max temp found thus far
                                 */
                                ExtremeTempPeriodResult metarResult = getMetarTempCForValidTime(
                                        informId, resultDate, validTime);
                                if (!metarResult.isMissing()) {
                                    numReports++;

                                    /*
                                     * is the metar's temp the highest so far?
                                     */
                                    if (metarResult.getTemp() >= maxTemp) {

                                        maxTemp = metarResult.getTemp();
                                        maxTime = new ClimateTime(nominalCal);
                                        if (metarResult
                                                .getFlag() == MetarUtils.METAR_TEMP_2_TENTHS) {
                                            flag = QCValues.TEMP_FROM_HOURLY;
                                        } else if (metarResult
                                                .getFlag() == MetarUtils.METAR_TEMP) {
                                            flag = QCValues.TEMP_FROM_WHOLE_HOURLY;
                                        }
                                    }
                                }
                            } catch (NullPointerException e) {
                                throw new ClimateQueryException(
                                        "Unexpected null result with query: ["
                                                + query + "] and map: ["
                                                + paramMap + "].",
                                        e);
                            } catch (Exception e) {
                                // if casting failed
                                throw new ClimateQueryException(
                                        "Unexpected return column type.", e);
                            }

                        } else {
                            throw new ClimateQueryException(
                                    "Unexpected return type from query, expected java.util.Date, got "
                                            + result.getClass().getName());
                        }
                    }
                } else {
                    logger.warn("Couldn't find a report for report time "
                            + nominalTimeString + "Z using query: [" + query
                            + "] and map: [" + paramMap + "]");
                }
            } catch (Exception e) {
                logger.error(
                        "An error was encountered retrieving the report from"
                                + " the METAR DB for " + nominalTimeString
                                + "Z.\n"
                                + "Error querying the climate database with query: ["
                                + query + "] and map: [" + paramMap + "]",
                        e);
                return new ExtremeTempPeriodResult();
            }
        }

        if (numReports > 0) {
            logger.info("Found [" + numReports + "] METAR reports for begin ["
                    + beginTimeMilli + "] and end [" + endTimeMilli + "].");
            return new ExtremeTempPeriodResult(maxTime, maxTemp, flag);
        } else {
            logger.warn("Could not find any METAR reports for begin ["
                    + beginTimeMilli + "] and end [" + endTimeMilli + "].");
            return new ExtremeTempPeriodResult();
        }
    }

    /**
     * Migrated from build_daily_obs_temp.ec.
     * 
     * <pre>
     * MODULE NUMBER:   9
     * MODULE NAME:     get_pd_metar_min
     * PURPOSE:         This module determines the minimum temperature for a user
     *                  defined time period.
     *
     * </pre>
     * 
     * @param informId
     *            station ID.
     * @param beginTimeMilli
     *            in milliseconds
     * @param endTimeMilli
     *            in milliseconds
     * @return
     */

    public ExtremeTempPeriodResult getPeriodMetarMin(int informId,
            long beginTimeMilli, long endTimeMilli) {
        /*
         * Contains the number of real reports retrieved... This is crucial in
         * determining whether to return an exit status of NO_HITS (absolutely
         * no METAR reports found) or an exit status of STATUS_OK (at least a
         * few METAR reports were found this period).
         */
        int numReports = 0;
        float minTemp = 9999;
        int flag = I_MISS;
        ClimateTime minTime = ClimateTime.getMissingClimateTime();

        /*
         * get each metar's temperature; check against max temp found thus far
         */
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("informId", informId);

        for (long nominalMilliTicks = beginTimeMilli; nominalMilliTicks <= endTimeMilli; nominalMilliTicks += TimeUtil.MILLIS_PER_HOUR) {
            Calendar nominalCal = TimeUtil.newCalendar();
            nominalCal.setTimeInMillis(nominalMilliTicks);
            String nominalTimeString = ClimateDate.getFullDateTimeFormat()
                    .format(nominalCal.getTime());

            StringBuilder query = new StringBuilder(
                    "SELECT DISTINCT valid_dtime FROM ");
            query.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
            query.append(" WHERE station_id = :informId");
            query.append(" AND report_subtype = 'MTR' ");
            query.append(" AND nominal_dtime = :nominalTime");

            paramMap.put("nominalTime", nominalCal);

            try {
                Object[] results = getDao().executeSQLQuery(query.toString(),
                        paramMap);
                if ((results != null) && (results.length >= 1)) {
                    for (Object result : results) {
                        if (result instanceof Date) {
                            try {
                                Date resultDate = (Date) result;
                                String validTime = ClimateDate
                                        .getFullDateTimeFormat()
                                        .format(resultDate);

                                /*
                                 * get each metar's temperature; check against
                                 * max temp found thus far
                                 */
                                ExtremeTempPeriodResult metarResult = getMetarTempCForValidTime(
                                        informId, resultDate, validTime);
                                if (!metarResult.isMissing()) {
                                    numReports++;

                                    /*
                                     * is the metar's temp the lowest so far?
                                     */
                                    if (metarResult.getTemp() <= minTemp) {

                                        minTemp = metarResult.getTemp();
                                        minTime = new ClimateTime(nominalCal);
                                        if (metarResult
                                                .getFlag() == MetarUtils.METAR_TEMP_2_TENTHS) {
                                            flag = QCValues.TEMP_FROM_HOURLY;
                                        } else if (metarResult
                                                .getFlag() == MetarUtils.METAR_TEMP) {
                                            flag = QCValues.TEMP_FROM_WHOLE_HOURLY;
                                        }
                                    }
                                }
                            } catch (NullPointerException e) {
                                throw new ClimateQueryException(
                                        "Unexpected null result with query: ["
                                                + query + "] and map: ["
                                                + paramMap + "].",
                                        e);
                            } catch (Exception e) {
                                // if casting failed
                                throw new ClimateQueryException(
                                        "Unexpected return column type.", e);
                            }

                        } else {
                            throw new ClimateQueryException(
                                    "Unexpected return type from query, expected java.util.Date, got "
                                            + result.getClass().getName());
                        }
                    }
                } else {
                    logger.warn("Couldn't find a report for report time "
                            + nominalTimeString + "Z using query: [" + query
                            + "] and map: [" + paramMap + "]");
                }
            } catch (Exception e) {
                logger.error(
                        "An error was encountered retrieving the report from"
                                + " the METAR DB for " + nominalTimeString
                                + "Z.\n"
                                + "Error querying the climate database with query: ["
                                + query + "] and map: [" + paramMap + "]",
                        e);
                return new ExtremeTempPeriodResult();
            }
        }

        if (numReports > 0) {
            logger.info("Found [" + numReports + "] METAR reports for begin ["
                    + beginTimeMilli + "] and end [" + endTimeMilli + "].");
            return new ExtremeTempPeriodResult(minTime, minTemp, flag);
        } else {
            logger.warn("Could not find any METAR reports for begin ["
                    + beginTimeMilli + "] and end [" + endTimeMilli + "].");
            return new ExtremeTempPeriodResult();
        }
    }

    /**
     * Migrated from build_daily_obs_temp.ec.
     * 
     * <pre>
     * MODULE NUMBER:   10
     * MODULE NAME:     get_pd_speci_max
     * PURPOSE:         This routine determines the maximum temperature from
     *                  all the special reports occurring withing a user-specified
     *                  time period.
     *
     * </pre>
     * 
     * @param informId
     *            station ID
     * @param beginTime
     *            earliest time to query for
     * @param endTime
     *            latest time to query for
     * @return
     */

    public ExtremeTempPeriodResult getPeriodSpeciMax(int informId,
            Calendar beginTime, Calendar endTime) {
        String beginTimeString = ClimateDate.getFullDateTimeFormat()
                .format(beginTime.getTime());
        String endTimeString = ClimateDate.getFullDateTimeFormat()
                .format(endTime.getTime());
        int numReports = 0;

        float maxTemp = -9999;
        ClimateTime maxTime = ClimateTime.getMissingClimateTime();

        StringBuilder query = new StringBuilder(
                "SELECT DISTINCT valid_dtime FROM ");
        query.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
        query.append(" WHERE station_id = :informId");
        query.append(" AND report_subtype = 'SPECI' ");
        query.append(" AND nominal_dtime BETWEEN :beginTime")
                .append(" AND :endTime");

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("informId", informId);
        paramMap.put("beginTime", beginTime);
        paramMap.put("endTime", endTime);

        try {
            Object[] results = getDao().executeSQLQuery(query.toString(),
                    paramMap);
            if ((results != null) && (results.length >= 1)) {
                for (Object result : results) {
                    if (result instanceof Date) {
                        Date oa = (Date) result;
                        try {
                            String validTime = ClimateDate
                                    .getFullDateTimeFormat().format(oa);

                            /*
                             * get each speci's temperature; check against max
                             * temp found thus far
                             */
                            try {
                                FSSReportResult speciResult = getSpeciConreal(
                                        informId, MetarUtils.METAR_TEMP, oa,
                                        validTime);

                                if (speciResult.isMissing()) {
                                    logger.warn(
                                            "Missing values returned from query.");
                                } else {
                                    numReports++;
                                    /*
                                     * is the speci's temp the highest so far?
                                     */
                                    if (speciResult.getValue() >= maxTemp) {
                                        maxTemp = (float) speciResult
                                                .getValue();
                                        Calendar cal = TimeUtil.newCalendar();
                                        cal.setTime(ClimateDate
                                                .getFullDateTimeFormat()
                                                .parse(validTime));
                                        maxTime = new ClimateTime(cal);
                                    }
                                }
                            } catch (ClimateQueryException e) {
                                logger.error("Error querying for SPECI", e);
                            }
                        } catch (NullPointerException e) {
                            throw new ClimateQueryException(
                                    "Unexpected null result with query: ["
                                            + query + "] and map: [" + paramMap
                                            + "].",
                                    e);
                        } catch (Exception e) {
                            // if casting failed
                            throw new ClimateQueryException(
                                    "Unexpected return column type.", e);
                        }

                    } else {
                        throw new ClimateQueryException(
                                "Unexpected return type from query, expected java.util.Date, got "
                                        + result.getClass().getName());
                    }
                }
            } else {
                logger.warn("Couldn't find a report for report time "
                        + beginTimeString + "Z to " + endTimeString
                        + "Z using query: [" + query + "] and map: [" + paramMap
                        + "]");
            }
        } catch (Exception e) {
            logger.error("An error was encountered retrieving the report from"
                    + " the SPECI DB for " + beginTimeString + "Z to "
                    + endTimeString + "Z.\n"
                    + "Error querying the climate database with query: ["
                    + query + "] and map: [" + paramMap + "]", e);
            return new ExtremeTempPeriodResult();
        }

        if (numReports > 0) {
            logger.info("Found [" + numReports + "] SPECI reports for begin ["
                    + beginTimeString + "] and end [" + endTimeString + "].");
            return new ExtremeTempPeriodResult(maxTime, maxTemp,
                    QCValues.TEMP_FROM_WHOLE_HOURLY);
        } else {
            logger.warn("Could not find any SPECI reports for begin ["
                    + beginTimeString + "] and end [" + endTimeString + "].");
            return new ExtremeTempPeriodResult();
        }
    }

    /**
     * Migrated from build_daily_obs_temp.ec.
     * 
     * <pre>
     * MODULE NUMBER:  11
     * MODULE NAME:    get_pd_speci_min
     * PURPOSE:        This routine checks all of the special reports in a
     *                 user-specified time period and determines the minimum
     *                 temperature from them.
     *
     * </pre>
     * 
     * @param informId
     *            station ID
     * @param beginTime
     *            earliest time to query for
     * @param endTime
     *            latest time to query for
     * @return
     */

    public ExtremeTempPeriodResult getPeriodSpeciMin(int informId,
            Calendar beginTime, Calendar endTime) {
        String beginTimeString = ClimateDate.getFullDateTimeFormat()
                .format(beginTime.getTime());
        String endTimeString = ClimateDate.getFullDateTimeFormat()
                .format(endTime.getTime());
        int numReports = 0;

        float minTemp = 9999;
        ClimateTime minTime = ClimateTime.getMissingClimateTime();

        StringBuilder query = new StringBuilder(
                "SELECT DISTINCT valid_dtime FROM ");
        query.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
        query.append(" WHERE station_id = :informId");
        query.append(" AND report_subtype = 'SPECI' ");
        query.append(" AND nominal_dtime BETWEEN :beginTime")
                .append(" AND :endTime");

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("informId", informId);
        paramMap.put("beginTime", beginTime);
        paramMap.put("endTime", endTime);

        try {
            Object[] results = getDao().executeSQLQuery(query.toString(),
                    paramMap);
            if ((results != null) && (results.length >= 1)) {
                for (Object result : results) {
                    if (result instanceof Date) {
                        Date oa = (Date) result;
                        try {
                            String validTime = ClimateDate
                                    .getFullDateTimeFormat().format(oa);

                            /*
                             * get each speci's temperature; check against min
                             * temp found thus far
                             */
                            try {
                                FSSReportResult speciResult = getSpeciConreal(
                                        informId, MetarUtils.METAR_TEMP, oa,
                                        validTime);

                                if (speciResult.isMissing()) {
                                    logger.warn(
                                            "Missing values returned from query.");
                                } else {
                                    numReports++;
                                    /* is the speci's temp the lowest so far? */
                                    if (speciResult.getValue() <= minTemp) {
                                        minTemp = (float) speciResult
                                                .getValue();
                                        Calendar cal = TimeUtil.newCalendar();
                                        cal.setTime(ClimateDate
                                                .getFullDateTimeFormat()
                                                .parse(validTime));
                                        minTime = new ClimateTime(cal);
                                    }
                                }
                            } catch (ClimateQueryException e) {
                                logger.error("Error querying for SPECI", e);
                            }
                        } catch (NullPointerException e) {
                            throw new ClimateQueryException(
                                    "Unexpected null result with query: ["
                                            + query + "] and map: [" + paramMap
                                            + "].",
                                    e);
                        } catch (Exception e) {
                            // if casting failed
                            throw new ClimateQueryException(
                                    "Unexpected return column type.", e);
                        }

                    } else {
                        throw new ClimateQueryException(
                                "Unexpected return type from query, expected java.util.Date, got "
                                        + result.getClass().getName());
                    }
                }
            } else {
                logger.warn("Couldn't find a report for report time "
                        + beginTimeString + "Z to " + endTimeString
                        + "Z using query: [" + query + "] and map: [" + paramMap
                        + "]");
            }
        } catch (Exception e) {
            logger.error("An error was encountered retrieving the report from"
                    + " the SPECI DB for " + beginTimeString + "Z to "
                    + endTimeString + "Z.\n"
                    + "Error querying the climate database with query: ["
                    + query + "] and map: [" + paramMap + "]", e);
            return new ExtremeTempPeriodResult();
        }

        if (numReports > 0) {
            logger.info("Found [" + numReports + "] SPECI reports for begin ["
                    + beginTimeString + "] and end [" + endTimeString + "].");
            return new ExtremeTempPeriodResult(minTime, minTemp,
                    QCValues.TEMP_FROM_WHOLE_HOURLY);
        } else {
            logger.warn("Could not find any SPECI reports for begin ["
                    + beginTimeString + "] and end [" + endTimeString + "].");
            return new ExtremeTempPeriodResult();
        }
    }

    /**
     * Migrated from get_metar_tempc_for_valid.ec.
     * 
     * <pre>
     * NUMBER OF MODULES:   1
     *  GENERAL INFORMATION:
     *       MODULE 1:       get_metar_tempc_for_valid
     *       DESCRIPTION:    Retrieve the current observed temperature/dqd from a
     *                       decoded METAR report contained in the FSS-series of 
     *                       hmdb database tables, given the station_id and report
     *                       valid (observed) time.  If the temperature in tenths
     *                       Celsius (as reported in the RMK fields of the METAR)
     *                       is available, this value is returned.  If the RMK
     *                       temperature is not available, the temperature in
     *                       whole degrees C (as reported in the body of the
     *                       METAR) is returned.  The hmdb element_id is returned
     *                       to indicate which type of the two possible types of
     *                       temperature values is being returned.  This value is
     *                       taken either from a regularly-scheduled metar report,
     *                       or (if one is present) the most recent correction to
     *                       such a regularly-scheduled metar report.
     *
     ******************************************************************************
     *void get_metar_tempc_for_valid ( const long station_id ,
     *                                   const char *valid_dtime ,
     *                                   long *element_id , float *value ,
     *                                   char *dqd , STATUS *status )
     *
     *  Bob Morris       SAIC/GSC, MDL    HP-UX 10.20, Linux 7.0, Informix 7.3.1
     *
     *  FUNCTION DESCRIPTION
     *  ====================
     *
     *    This function gets a current temperature observation value and its data
     *    quality descriptor (dqd) for a decoded METAR stored in the FSS_REPORT
     *    and FSS_CONTIN_REAL hm database tables, given a station and report time.
     *    The input time is when the observation was taken (its hmdb valid_dtime).
     *    If the temperature value in tenths C from the RMKs is available, it is
     *    obtained.  Otherwise, the temperature in whole degrees C from the body
     *    of the report is obtained.  The element_id of whichever of the two
     *    temperature values was obtained is also provided to the caller.  Data
     *    are taken only from a regularly scheduled METAR report, not a "SPECI".
     *
     *    The station is specified by a long integer id number (from the
     *    station_location table in the hm database).  The element_id is
     *    specified by a long integer id number (from the hydromet_element table
     *    in the hm database.  The valid date/time of the metar is specified by a
     *    string in SQL-friendly "yyyy-mm-dd hh:mm" format.
     *
     *    The result is one scalar float value (value), a one-character data
     *    quality descriptor (dqd) and one scalar long value (element_id).
     *
     *    This function selects the data values from the latest correction to the
     *    metar, if any corrections.  If there are no corrections, then the
     *    data are selected from the metar's latest transmission.  Note this 
     *    function is expected to be called multiple times for each nominal hour
     *    for stations which produce routine METAR observations (no SPECIs)
     *    more than once per hour, or only once per nominal hour for stations which
     *    produce one METAR per hour with SPECIs as needed (e.g. ASOS and manual
     *    sites.  AWOS stations, which post METARs at around 15, 35, and 55 minutes
     *    after each hour, are an example of the former station type.  Note the
     *    same nominal hour applies to reports with valid times from 15 minutes
     *    before the nominal hour to 44 minutes past the nominal hour.  Note also
     *    that AWOS stations are those most likely to not have RMKs, so they are
     *    the primary raison d'etre for this utility -- we want to take care of
     *    temperature_in_tenthslessness for these stations.  It would be up to the
     *    calling routine to loop over the valid times for a given nominal hour,
     *    call this function for each valid time, and process the observed values.
     *
     *    If this function succeeds in finding the requested value, the return
     *    status is STATUS_OK (= 0).  If a metar for the requested station and
     *    time is found, but neither temperature value is found, the return
     *    status is STATUS_FAILURE (= 1).  If no metars for the requested station
     *    and time are found, the return status is NO_HITS.  See POSSIBLE STATUS
     *    VALUES below for other possible status values and their meanings.
     *
     * </pre>
     * 
     * @param informId
     *            station ID
     * @param validTime
     *            date/time to query for
     * @return
     */

    private ExtremeTempPeriodResult getMetarTempCForValidTime(int informId,
            Date validTime, String validTimeString) {
        /*
         * Query on metars for the requested station and time. Sort primarily on
         * corrections ahead of originals, and sort lastly according to the time
         * of report receipt. We'll fetch only the first returned row; i.e., the
         * metar with the report id number corresponding to the most recent
         * correction to the METAR issuance for the specified valid time; or the
         * most recent receipt of this METAR issuance if there are no
         * corrections.
         */
        /*
         * OUTER join assures that we try to get RMKs and report-body
         * temperature values and their dqds, from whichever of these is/are
         * available in the fss_contin_real table, ONLY from the report of
         * interest, whether or not the data are present. Otherwise, we indicate
         * missing data via status value of STATUS_FAILURE.
         */
        StringBuilder query = new StringBuilder(
                "SELECT a.origin_dtime, a.correction, ");
        query.append(" b.element_value as belement_value, b.dqd as bdqd, ");
        query.append(" c.element_value as celement_value, c.dqd as cdqd FROM ");
        query.append(ClimateDAOValues.FSS_REPORT_TABLE_NAME);
        query.append(" as a LEFT OUTER JOIN (");
        query.append(" SELECT * FROM ");
        query.append(ClimateDAOValues.FSS_CONTIN_REAL_TABLE_NAME);
        query.append(" WHERE element_id = ")
                .append(MetarUtils.METAR_TEMP_2_TENTHS);
        query.append(" ) AS b USING (fss_rpt_instance) LEFT OUTER JOIN (");
        query.append(" SELECT * FROM ");
        query.append(ClimateDAOValues.FSS_CONTIN_REAL_TABLE_NAME);
        query.append(" WHERE element_id = ").append(MetarUtils.METAR_TEMP);
        query.append(" ) AS c USING (fss_rpt_instance)");
        query.append(" WHERE a.valid_dtime = :validTime");
        query.append(" AND a.station_id = :informId");
        query.append(" AND a.report_subtype = 'MTR' ");
        query.append(" ORDER BY a.correction DESC, a.origin_dtime DESC");

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("validTime", toSqlDate(validTime));
        paramMap.put("informId", informId);

        try {
            Object[] results = getDao().executeSQLQuery(query.toString(),
                    paramMap);
            if ((results != null) && (results.length >= 1)) {
                for (Object result : results) {
                    if (result instanceof Object[]) {
                        Object[] oa = (Object[]) result;
                        /*
                         * Get the temperature data. Take only the first row
                         * returned by query.
                         */
                        /*
                         * origin time and correction are skipped, used only for
                         * sorting
                         */
                        // prefer tenths response (b)
                        Object tenthsElementValue = oa[2];
                        // dqd could be null
                        Object tenthsDQD = oa[3];
                        if (tenthsElementValue != null && tenthsDQD != null) {
                            /*
                             * We found a RMKs temperature-in-tenths, return
                             * these data to caller
                             */
                            return new ExtremeTempPeriodResult(
                                    ((Number) tenthsElementValue).floatValue(),
                                    MetarUtils.METAR_TEMP_2_TENTHS);
                        } else {
                            logger.info(
                                    "Missing tenths temp values for station ID ["
                                            + informId + "] at time "
                                            + validTimeString
                                            + "Z. Will attempt whole degrees. Query: ["
                                            + query + "] and map: [" + paramMap
                                            + "].");

                            Object wholeElementValue = oa[4];
                            // dqd could be null
                            Object wholeDQD = oa[5];
                            if (wholeElementValue != null && wholeDQD != null) {
                                /*
                                 * No RMKs temp/dqd or one of the RMKs fields is
                                 * NULL, but have non-null report body
                                 * temperature and dqd -- return these data.
                                 */
                                return new ExtremeTempPeriodResult(
                                        ((Number) wholeElementValue)
                                                .floatValue(),
                                        MetarUtils.METAR_TEMP);
                            } else {
                                logger.warn(
                                        "Missing tenths and whole temp values for station ID ["
                                                + informId + "] at time "
                                                + validTimeString
                                                + "Z. Query: [" + query
                                                + "] and map: [" + paramMap
                                                + "].");
                            }
                        }
                    } else {
                        throw new ClimateQueryException(
                                "Unexpected return type from query, expected Object[], got "
                                        + result.getClass().getName());
                    }
                }
            } else {
                logger.warn("Couldn't find a report for report time "
                        + validTimeString + "Z using query: [" + query
                        + "] and map: [" + paramMap + "]");
            }
        } catch (Exception e) {
            logger.error("An error was encountered retrieving the report from"
                    + " the METAR DB for " + validTimeString + "Z.\n"
                    + "Error querying the climate database with query: ["
                    + query + "] and map: [" + paramMap + "]", e);
        }

        return new ExtremeTempPeriodResult();
    }

    /**
     * Return a Date instance for the the given Date (possibly subclass)
     * instance. Subclasses of Date may not be usable as query parameters.
     *
     * @param d
     * @return
     */
    private static Date toSqlDate(Date d) {
        return new Date(d.getTime());
    }

    private static Calendar combine(ClimateDate date, ClimateTime time) {
        Calendar cc = date.getCalendarFromClimateDate();
        cc.add(Calendar.HOUR_OF_DAY, time.getHour());
        cc.add(Calendar.MINUTE, time.getMin());
        return cc;
    }

}
