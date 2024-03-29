/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 **/
package gov.noaa.nws.ocp.common.dataplugin.climate;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.util.TimeUtil;

import gov.noaa.nws.ocp.common.dataplugin.climate.parameter.ParameterFormatClimate;

/**
 * 
 * Converted from TYPE_climate_date.h
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 13, 2015            xzhang     Initial creation
 * 13 MAY 2016  16003      amoore     Make month-day and month-day-year date selection options.
 * 17 MAY 2016  18384      amoore     Logic clean-up.
 * 07 JUL 2016  16962      amoore     Fix serialization
 * 22 JUL 2016  20635      wkwock     toString functions handle missing-dates. Add isMissing()
 * 04 OCT 2016  20636      wpaintsil  Constructor to create from object.
 * 13 OCT 2016  20636      wpaintsil  Add missing day string.
 * 17 NOV 2016  21378      amoore     Added parsing from SQL Date object.
 * 19 DEC 2016  27015      amoore     Checking against missing date information should
 *                                    require all date data present if logic would need it.
 * 12 JAN 2017  20640      jwu        Add xml annotations
 * 26 JAN 2017  27017      amoore     Correct convertJulDay logic, which was adding an extra day.
 * 08 FEB 2017  28609      amoore     Date format with seconds.
 * 22 FEB 2017  28609      amoore     Address TODOs.
 * 15 MAR 2017  30162      amoore     Fix logging on constructors.
 * 13 APR 2017  33104      amoore     Address comments from review.
 * 03 MAY 2017  33104      amoore     Update logging.
 * 05 MAY 2017  33104      amoore     Add month-day constructor.
 * 10 MAY 2017  33104      amoore     Address Find Bugs issue about static date formats.
 * 11 MAY 2017  33104      amoore     More Find Bugs minor issues.
 * 16 JUN 2017  35182      amoore     Add util Date and SQL date as possible construction objects.
 * 24 JUL 2017  33104      amoore     Time format in 24-hours.
 * 18 SEP 2017  38078      amoore     Fixed bad formatting for First of month format.
 * 24 AUG 2018  DR20863    wpaintsil  With getLocalDate(), create a distinction between machine/GMT
 *                                    time and local time.
 * 04 APR 2019  DR21220    wpaintsil  Add a conversion method for ZonedDateTime.
 * 10 APR 2019  DR21159    wpaintsil  Revise date-time formatting and comparisons.
 * 04 JUN 2019  DR21430    wpaintsil  Revise month-day formatting.
 * </pre>
 * 
 * @author xzhang
 * @version 1.0
 */
@DynamicSerialize
@XmlRootElement(name = "ClimateDate")
@XmlAccessorType(XmlAccessType.NONE)
public class ClimateDate {
    /**
     * Logger.
     */
    private static final IUFStatusHandler logger = UFStatus
            .getHandler(ClimateDate.class);

    /**
     * Date separator.
     */
    public static final String DATE_SEPARATOR = "-";

    /**
     * Missing/empty two-digit day or month string
     */
    public static final String MISSING_DATE_NUM_STRING = String
            .valueOf(ParameterFormatClimate.MISSING_DATE);

    /**
     * Missing/empty month-day date string.
     */
    public static final String MISSING_MONTH_DAY_DATE_STRING = MISSING_DATE_NUM_STRING
            + DATE_SEPARATOR + MISSING_DATE_NUM_STRING;

    /**
     * Missing/empty month-day date string.
     */
    public static final String MISSING_FULL_DATE_STRING = ParameterFormatClimate.MISSING
            + DATE_SEPARATOR + MISSING_DATE_NUM_STRING + DATE_SEPARATOR
            + MISSING_DATE_NUM_STRING;

    /**
     * YYYY-MM-DD date string format
     */
    public static final String FULL_DATE_STRING_FORMAT = "%d" + DATE_SEPARATOR
            + "%02d" + DATE_SEPARATOR + "%02d";

    /**
     * MM-DD date string formt
     */
    public static final String MONTH_DAY_STRING_FORMAT = "%02d" + DATE_SEPARATOR
            + "%02d";

    /**
     * First day of month format
     */
    public static final String FIRST_DAY_MONTH_STRING_FORMAT = "%02d"
            + DATE_SEPARATOR + "01";

    @DynamicSerializeElement
    @XmlAttribute(name = "Day")
    private int day; /*
                      * day of the month (1-31)
                      */

    @DynamicSerializeElement
    @XmlAttribute(name = "Month")
    private int mon; /*
                      * month of the year (1-12)
                      */

    @DynamicSerializeElement
    @XmlAttribute(name = "Year")
    private int year; /* year */

    /**
     * Empty constructor.
     */
    public ClimateDate() {
    }

    /**
     * Constructor.
     * 
     * @param iDay
     *            day to set.
     * @param iMon
     *            month to set.
     * @param iYear
     *            year to set.
     */
    public ClimateDate(int iDay, int iMon, int iYear) {
        super();
        this.day = iDay;
        this.mon = iMon;
        this.year = iYear;
    }

    /**
     * Constructor.
     * 
     * @param iDay
     *            day to set.
     * @param iMon
     *            month to set.
     */
    public ClimateDate(int iDay, int iMon) {
        super();
        this.day = iDay;
        this.mon = iMon;
        this.year = ParameterFormatClimate.MISSING;
    }

    /**
     * Copy constructor.
     * 
     * @param iDate
     */
    public ClimateDate(ClimateDate iDate) {
        this.day = iDate.getDay();
        this.mon = iDate.getMon();
        this.year = iDate.getYear();
    }

    /**
     * Construct from the given calendar.
     * 
     * @param iCal
     */
    public ClimateDate(Calendar iCal) {
        setDateFromCalendar(iCal);
    }

    /**
     * Construct from util Date.
     * 
     * @param iDate
     */
    public ClimateDate(Date iDate) {
        setDateFromDate(iDate);
    }

    /**
     * Construct from sql Date.
     * 
     * @param iDate
     */
    public ClimateDate(java.sql.Date iDate) {
        setDateFromDate(iDate);
    }

    /**
     * Set date from sql Date object.
     * 
     * @param iDate
     */
    public void setDateFromDate(java.sql.Date iDate) {
        setDateFromCalendar(TimeUtil.newCalendar(iDate.getTime()));
    }

    /**
     * @param iDate
     * @return instance based on date.
     */
    public static ClimateDate parseFullDateFromDate(java.sql.Date iDate) {
        ClimateDate date = new ClimateDate();
        date.setDateFromDate(iDate);
        return date;
    }

    /**
     * Set date from util Date object
     * 
     * @param iDate
     */
    public void setDateFromDate(Date iDate) {
        setDateFromCalendar(TimeUtil.newCalendar(iDate));
    }

    /**
     * @param iDate
     * @return instance based on date.
     */
    public static ClimateDate parseFullDateFromDate(Date iDate) {
        ClimateDate date = new ClimateDate();
        date.setDateFromDate(iDate);
        return date;
    }

    /**
     * Construct from an object. If the object is null, or not a String, or not
     * a util Date, or not an sql Date, then set data to missing.
     * 
     * @param iDateObject
     */
    public ClimateDate(Object iDateObject) {
        if (iDateObject == null) {
            logger.warn("Null object will not be parsed into a Date.");
            setDataToMissing();
        } else if (iDateObject instanceof String) {
            try {
                ClimateDate newDate = parseFullDateFromString(
                        (String) iDateObject);
                setDateFromDate(newDate);
            } catch (ParseException e) {
                logger.warn("Could not parse Climate Date from string ["
                        + iDateObject + "]");
                setDataToMissing();
            }
        } else if (iDateObject instanceof Date) {
            setDateFromDate((Date) iDateObject);
        } else if (iDateObject instanceof java.sql.Date) {
            setDateFromDate((java.sql.Date) iDateObject);
        } else {
            logger.warn("Invalid object: [" + iDateObject + "] of type: ["
                    + iDateObject.getClass()
                    + "] will not be parsed into a Date.");
            setDataToMissing();
        }
    }

    /**
     * Set the date from the given calendar.
     * 
     * @param iCal
     */
    public void setDateFromCalendar(Calendar iCal) {
        // offset month due to DateTime using 0-11
        this.mon = iCal.get(Calendar.MONTH) + 1;
        this.day = iCal.get(Calendar.DAY_OF_MONTH);
        this.year = iCal.get(Calendar.YEAR);
    }

    /**
     * Copy fields from other date instance.
     * 
     * @param iDate
     */
    public void setDateFromDate(ClimateDate iDate) {
        this.mon = iDate.getMon();
        this.day = iDate.getDay();
        this.year = iDate.getYear();
    }

    /**
     * @return the day
     */
    public int getDay() {
        return day;
    }

    /**
     * @return the mon
     */
    public int getMon() {
        return mon;
    }

    /**
     * @return the year
     */
    public int getYear() {
        return year;
    }

    /**
     * @param iday
     *            the day to set
     */
    public void setDay(int iday) {
        this.day = iday;
    }

    /**
     * @param imon
     *            the month to set
     */
    public void setMon(int imon) {
        this.mon = imon;
    }

    /**
     * @param iyear
     *            the year to set
     */
    public void setYear(int iyear) {
        this.year = iyear;
    }

    /**
     * @return format of "MM-dd"
     */
    public String toMonthDayDateString() {
        if (day == ParameterFormatClimate.MISSING_DATE
                || day == ParameterFormatClimate.MISSING
                || mon == ParameterFormatClimate.MISSING_DATE
                || mon == ParameterFormatClimate.MISSING) {
            return MISSING_MONTH_DAY_DATE_STRING;
        }

        LocalDate date = validDate(mon, day, year);

        return String.format(MONTH_DAY_STRING_FORMAT, date.getMonthValue(),
                date.getDayOfMonth());
    }

    /**
     * Parse month-day date from string.
     * 
     * @param dateString
     *            string to parse.
     * @return a {@link ClimateDate} instance with info from the date string.
     * @throws ParseException
     *             on exception parsing date.
     */
    public static ClimateDate parseMonthDayDateFromString(String dateString)
            throws ParseException {
        Date date = getMonthDayDateFormat().parse(dateString);

        return new ClimateDate(date);
    }

    /**
     * @return "YYYY-MM-DD"
     */
    public String toFullDateString() {
        if (isPartialMissing()) {
            return MISSING_FULL_DATE_STRING;
        }
        return String.format(FULL_DATE_STRING_FORMAT, year, mon, day);
    }

    /**
     * Parse full date from string.
     * 
     * @param dateString
     *            string to parse.
     * @return a {@link ClimateDate} instance with info from the date string.
     * @throws ParseException
     *             on exception parsing date.
     */
    public static ClimateDate parseFullDateFromString(String dateString)
            throws ParseException {
        if (dateString == null || dateString.equals(MISSING_FULL_DATE_STRING)
                || dateString.equals(MISSING_MONTH_DAY_DATE_STRING)) {
            logger.debug("Could not parse a date from missing value ["
                    + dateString + "]. Returning missing date.");
            return ClimateDate.getMissingClimateDate();
        }

        Date date = getFullDateFormat().parse(dateString);

        return new ClimateDate(date);
    }

    /**
     * Parse full date from SQL date object.
     * 
     * @param date
     *            date object.
     * @return a {@link ClimateDate} instance with info from the date object.
     */
    public static ClimateDate parseFullDateFromSQLDate(java.sql.Date date) {
        Calendar cal = TimeUtil.newCalendar();
        cal.setTimeInMillis(date.getTime());
        return new ClimateDate(cal);
    }

    /**
     * @return format of "MM-01"
     */
    public String toFirstDayOfMonthDateString() {
        return String.format(FIRST_DAY_MONTH_STRING_FORMAT, mon);
    }

    /**
     * Set data to missing values.
     */
    public void setDataToMissing() {
        this.day = ParameterFormatClimate.MISSING_DATE;
        this.mon = ParameterFormatClimate.MISSING_DATE;
        this.year = ParameterFormatClimate.MISSING;
    }

    /**
     * @return object based on the local date as well as the timezone.
     * @param timeZone
     *            the time zone string
     */
    public static ClimateDate getLocalDate(String timeZone) {
        Calendar cal;
        if (timeZone == null) {
            cal = TimeUtil.newCalendar();
        } else {
            TimeZone localTimeZone = TimeZone.getTimeZone(timeZone);

            cal = TimeUtil.newCalendar(localTimeZone);

            if (!localTimeZone.getID().equalsIgnoreCase(timeZone)) {
                logger.warn("Formatter tried to use globalDay timezone of: ["
                        + timeZone + "], but Java parsed this as: ["
                        + localTimeZone.getID() + "].");
            }
        }
        return new ClimateDate(cal);
    }

    /**
     * @return object based on the local date
     */
    public static ClimateDate getLocalDate() {
        return getLocalDate(null);
    }

    /**
     * @return object based on previous day
     */
    public static ClimateDate getPreviousDay() {
        Calendar cal = TimeUtil.newCalendar();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        return new ClimateDate(cal);
    }

    /**
     * rewritten from julday.f
     * 
     * Purpose: This routine calculates the Julian day for an input date. It
     * takes into account leap years. It is Y2K compliant.
     * 
     * Migration comments: This actually returns, as in Legacy, the DAY OF YEAR
     * number, not the Julian day.
     */
    public int julday() {
        return getCalendarFromClimateDate().get(Calendar.DAY_OF_YEAR);
    }

    /**
     * Get a {@link Calendar} object using date from this instance.
     * 
     * @return {@link Calendar} object.
     */
    public Calendar getCalendarFromClimateDate() {
        Calendar cal = TimeUtil.newCalendar();
        cal.clear();
        cal.set(year, mon - 1, day);
        return cal;
    }

    /**
     * Get a {@link ZonedDateTime} object using date from this instance.
     * 
     * @param timeZone
     * @return {@link ZonedDateTime} object.
     */
    public ZonedDateTime getZonedDateTimeFromClimateDate(String timeZone) {
        return ZonedDateTime.of(validDate(mon, day, year), LocalTime.now(),
                ZoneId.of(timeZone));

    }

    /**
     * 
     * @param month
     * @param day
     * @param year
     * @return a LocalDate object with a day within the bounds appropriate for
     *         that specific month and year.
     */
    private static LocalDate validDate(int month, int day, int year) {
        int validDay = day;
        boolean outOfBounds = false;

        // Return the first day of the month if the day parameter is somehow
        // less than 1.
        if (day < 1) {
            validDay = 1;
            outOfBounds = true;
        }

        // Return the last day of the month if the day parameter is greater than
        // the last day of the given month and year.
        int lastDayOfMonth = YearMonth.of(year, month).lengthOfMonth();
        if (day > lastDayOfMonth) {
            validDay = lastDayOfMonth;
            outOfBounds = true;
        }

        if (outOfBounds) {
            String monthString = DateFormatSymbols.getInstance()
                    .getMonths()[month - 1];
            logger.warn("Invalid Date: " + monthString + " " + day + ", " + year
                    + ". Defaulted to " + monthString + " " + validDay + ", "
                    + year + ".");
        }

        return LocalDate.of(year, month, validDay);
    }

    /**
     * @return a new object filled with missing values.
     */
    public static ClimateDate getMissingClimateDate() {
        ClimateDate date = new ClimateDate();
        date.setDataToMissing();
        return date;
    }

    /**
     * Is this a missing date(day=99, mon=99, year=9999) ?
     * 
     * @return true for all missing data or false for at least one non-missing
     *         data field.
     */
    public boolean isMissing() {
        if ((day == ParameterFormatClimate.MISSING_DATE)
                && (mon == ParameterFormatClimate.MISSING_DATE)
                && (year == ParameterFormatClimate.MISSING)) {
            return true;
        }

        return false;
    }

    /**
     * Is this a date missing at least some data (day=99, mon=99, year=9999)?
     * 
     * @return true for some missing data or false for all non-missing data.
     */
    public boolean isPartialMissing() {
        if ((day == ParameterFormatClimate.MISSING_DATE)
                || (mon == ParameterFormatClimate.MISSING_DATE)
                || (year == ParameterFormatClimate.MISSING)) {
            return true;
        }

        return false;
    }

    /**
     * Tests if this date is after the given date.
     * 
     * @return true if this date is after the given one. False otherwise.
     */
    public boolean after(ClimateDate iDate) {
        // automatic false if other date is missing any data
        if (isPartialMissing() || iDate.isPartialMissing()) {
            return false;
        }

        LocalDate thisDate = validDate(mon, day, year);

        LocalDate otherDate = validDate(iDate.getMon(), iDate.getDay(),
                iDate.getYear());

        return thisDate.isAfter(otherDate);
    }

    /**
     * Tests if this date is before the given date.
     * 
     * @return true if this date is before the given one. False otherwise.
     */
    public boolean before(ClimateDate iDate) {
        // automatic false if other date is missing any data
        if (isPartialMissing() || iDate.isPartialMissing()) {
            return false;
        }

        LocalDate thisDate = validDate(mon, day, year);

        LocalDate otherDate = validDate(iDate.getMon(), iDate.getDay(),
                iDate.getYear());

        return thisDate.isBefore(otherDate);
    }

    @Override
    public boolean equals(Object iObject) {
        if (iObject != null && iObject instanceof ClimateDate) {
            ClimateDate otherDate = (ClimateDate) iObject;
            if (otherDate.getYear() == year && otherDate.getMon() == mon
                    && otherDate.getDay() == day) {
                return true;
            }
        }

        return false;
    }

    /**
     * July 1998 Jason P. Tuell PRC/TDL
     * 
     * Purpose: This routine calculates the date for an input Julian day. It
     * takes into account leap years and is Y2K compliant. It adjusts the year
     * to take into account those days that cross year boundaries. It assumes
     * that the year for the Julian day is already stored in the variable
     * a_date. If the Julian day is greater than 365 in normal years or 366 in
     * leap years, then the year is incremented by one. If the Julian day is
     * less than 1, then the year is decremented by one.
     * 
     * Migration comments: This actually converts, as in Legacy, the DAY OF YEAR
     * number, not the Julian day.
     */
    public void convertJulday(int julday) {
        if (!isLeapYear(getYear())) {
            if (julday <= 0) {
                julday += 365;
                setYear(getYear() - 1);
            } else if (julday >= 366) {
                julday -= 365;
                setYear(getYear() + 1);
            }
            Calendar cal = TimeUtil.newCalendar();
            cal.set(getYear(), 0, 1);
            cal.add(Calendar.DATE, julday - 1);
            setMon(cal.get(Calendar.MONTH) + 1);
            setDay(cal.get(Calendar.DATE));
        } else {
            if (julday <= 0) {
                julday += 366;
                setYear(getYear() - 1);
            } else if (julday >= 367) {
                julday -= 366;
                setYear(getYear() + 1);
            }
            Calendar cal = TimeUtil.newCalendar();
            cal.set(getYear(), 0, 1);
            cal.add(Calendar.DATE, julday - 1);
            setMon(cal.get(Calendar.MONTH) + 1);
            setDay(cal.get(Calendar.DATE));
        }
    }

    /**
     * @return true if this instance represents a leap year.
     */
    public boolean isLeapYear() {
        return isLeapYear(year);
    }

    /**
     * Converted from leap.f
     * 
     * Original comments:
     * 
     * <pre>
     * Nov 1997         Jason P. Tuell         PRC
     *    Purpose:  This function checks to see if the input year is a
     *             leap year.  It returns a value of TRUE if the year
     *             is a leap year and a value of FALSE if the year is
     *             not a leap year.  Note that the input year must be
     *             a four digit year.  This subroutine is Y2K compliant.
     * 
     * </pre>
     */
    public static boolean isLeapYear(int year) {
        return new GregorianCalendar().isLeapYear(year);
    }

    /**
     * Get the full 4-digit year as a String for the given date.
     * 
     * @param iDate
     * @return
     */
    public static String getYearStringFromClimateDate(ClimateDate iDate) {
        Calendar cal = iDate.getCalendarFromClimateDate();
        return new SimpleDateFormat("yyyy").format(cal.getTime());
    }

    /**
     * Get the spelled out name of a month for the given date.
     * 
     * @param iDate
     * @return
     */
    public static String getMonthStringFromClimateDate(ClimateDate iDate) {
        Calendar cal = iDate.getCalendarFromClimateDate();
        return new SimpleDateFormat("MMMMM").format(cal.getTime());
    }

    /**
     * @return Format for full date: yyyy-MM-dd
     */
    public static SimpleDateFormat getFullDateFormat() {
        return new SimpleDateFormat(
                "yyyy" + DATE_SEPARATOR + "MM" + DATE_SEPARATOR + "dd");
    }

    /**
     * @return Format for month-day date: MM-dd
     */
    private static SimpleDateFormat getMonthDayDateFormat() {
        return new SimpleDateFormat("MM" + DATE_SEPARATOR + "dd");
    }

    /**
     * @return Format for full date and time. No seconds: yyyy-MM-dd hh24:mm
     */
    public static SimpleDateFormat getFullDateTimeFormat() {
        return new SimpleDateFormat("yyyy" + DATE_SEPARATOR + "MM"
                + DATE_SEPARATOR + "dd" + " HH:mm");
    }

    /**
     * @return Format for full date and time, with seconds: yyyy-MM-dd
     *         hh24:mm:ss
     */
    public static SimpleDateFormat getFullDateTimeSecondsFormat() {
        return new SimpleDateFormat("yyyy" + DATE_SEPARATOR + "MM"
                + DATE_SEPARATOR + "dd" + " HH:mm:ss");
    }
}
