/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 **/
package gov.noaa.nws.ocp.viz.common.climate.util;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.localization.LocalizationManager;

/**
 * 
 * GUI Utility class for Climate.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 06 DEC 2016  26345      astrakovsky Initial creation
 * 24 JAN 2017  28499      amoore      Make final, and have private constructor.
 * </pre>
 * 
 * @author astrakovsky
 * @version 1.0
 */
public final class ClimateGUIUtils {
    /**
     * Logger.
     */
    private static final IUFStatusHandler logger = UFStatus
            .getHandler(ClimateGUIUtils.class);

    /**
     * Set the cursor to a work in progress icon.
     * 
     * @param shell
     */
    public static void setCursorWait(Shell shell) {
        shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));
    }

    /**
     * Set the cursor to the default icon.
     * 
     * @param shell
     */
    public static void resetCursor(Shell shell) {
        shell.setCursor(null);
    }

    /**
     * Private constructor. This is a utility class.
     */
    private ClimateGUIUtils() {
    }

    /**
     * Get the SITE id for the current site.
     * 
     * @return ID of current site.
     */
    public static String getCurrentSite() {
        String wfo = LocalizationManager.getInstance().getCurrentSite();
        if (wfo == null || wfo.equalsIgnoreCase("none") || wfo.isEmpty()) {
            logger.warn("Localization site is either 'none', empty, or null.");
            wfo = "ALY";
        }
        return wfo;
    }
}
