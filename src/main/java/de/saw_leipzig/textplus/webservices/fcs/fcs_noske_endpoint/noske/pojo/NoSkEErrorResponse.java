package de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo;

public class NoSkEErrorResponse extends NoSkEResponse {
    public String error;

    @Override
    public String toString() {
        return "NoSkEErrorResponse [error=" + error
                + ", api_version=" + api_version + ", manatee_version=" + manatee_version + ", request=" + request
                + "]";
    }
}
