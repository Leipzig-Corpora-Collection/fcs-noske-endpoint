package de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo;

public class QueryDesc {
    public String op;
    public String arg;
    public String nicearg;
    public float rel;
    public long size;
    public String tourl;

    @Override
    public String toString() {
        return "QueryDesc [op=" + op + ", arg=" + arg + ", nicearg=" + nicearg + ", rel=" + rel + ", size=" + size
                + ", tourl=" + tourl + "]";
    }
}
