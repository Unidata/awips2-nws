/**
 * This software was developed and / or modified by NOAA/NWS/OCP/ASDT
 **/
package gov.noaa.nws.ocp.common.dataplugin.psh;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Contains a list of METAR data entries.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 12, 2017 #39468     jwu         Initial creation
 * Jan 11, 2018 DCS19326   jwu         Baseline version.
 *
 * </pre>
 *
 * @author jwu
 * @version 1.0
 */
@XmlRootElement(name = "MetarStormData")
@XmlAccessorType(XmlAccessType.FIELD)
@DynamicSerialize
public class MetarStormData extends StormData {

    @DynamicSerializeElement
    @XmlElements(@XmlElement(name = "MetarData", type = MetarDataEntry.class))
    private List<MetarDataEntry> data;

    public MetarStormData() {
        super();
    }

    /**
     * @return the data
     */
    public List<MetarDataEntry> getData() {
        if (data == null) {
            data = new ArrayList<>();
        }
        return data;
    }

    /**
     * @param data
     *            the data to set
     */
    public void setData(List<MetarDataEntry> data) {
        this.data = data;
    }

    /**
     * @param data
     *            the data to add
     */
    public void addData(MetarDataEntry dataEntry) {
        this.data.add(dataEntry);
    }

}
