package de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo;

import java.util.Map;

// for versions:
// - "api_version": "open-5.63.9" (bonito)
// - "manatee_version": "2.36.7-open-2.223.6"

// NOTE: add decorator if for all subclasses
// @JsonIgnoreProperties(ignoreUnknown=true)
public class NoSkEResponse {
    public String api_version;
    public String manatee_version;
    public Map<String, Object> request;

    @Override
    public String toString() {
        return "NoSkEResponse [api_version=" + api_version + ", manatee_version=" + manatee_version + ", request="
                + request + "]";
    }
}
