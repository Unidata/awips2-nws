/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 **/
package gov.noaa.nws.ocp.edex.climate.service.handler.display;

import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

import gov.noaa.nws.ocp.common.dataplugin.climate.request.display.DailyClimateMaxSnowUpdateServiceRequest;
import gov.noaa.nws.ocp.edex.common.climate.dao.DailyClimateDAO;

/**
 * Request service update handler for Daily max snow values.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 16 AUG 2016  20414      amoore     Initial creation
 * 
 * </pre>
 * 
 * @author amoore
 * @version 1.0
 */

public class DailyMaxSnowUpdateServiceHandler
        implements IRequestHandler<DailyClimateMaxSnowUpdateServiceRequest> {

    /**
     * The logger.
     */
    private static final IUFStatusHandler logger = UFStatus
            .getHandler(DailyMaxSnowUpdateServiceHandler.class);

    @Override
    public Object handleRequest(DailyClimateMaxSnowUpdateServiceRequest request)
            throws Exception {
        try {
            return new DailyClimateDAO().updateDailyDataForMaxSnow(
                    request.getStationID(), request.getMaxSnow(),
                    request.getDates());
        } catch (Exception e) {
            logger.error("Error updating display daily data for station ["
                    + request.getStationID() + "] and max snow ["
                    + request.getMaxSnow() + "] and date count ["
                    + request.getDates().size() + "]", e);
            throw e;
        }
    }
}
