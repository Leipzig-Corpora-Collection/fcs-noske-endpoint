package de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint;

public final class NoSkESRUFCSConstants {
    // FCS request parameters to extract Resource PIDs
    public static final String X_FCS_CONTEXT_KEY = "x-fcs-context";
    public static final String X_FCS_CONTEXT_SEPARATOR = ",";
    // FCS request parameters to extract Data Views
    public static final String X_FCS_DATAVIEWS_KEY = "x-fcs-dataviews";
    public static final String X_FCS_DATAVIEWS_SEPARATOR = ",";

    public static final String CLARIN_FCS_RECORD_SCHEMA = "http://clarin.eu/fcs/resource";

    /** Resource URI prefix */
    public static final String RESOURCE_PREFIX = "lcc:";
    /** Resource Advanced DataView Layer base URI */
    public static final String LAYER_PREFIX = "http://wortschatz-leipzig.de/noske/fcs/layer/";
}
