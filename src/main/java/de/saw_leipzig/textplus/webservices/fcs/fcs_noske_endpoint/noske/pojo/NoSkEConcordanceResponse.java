package de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NoSkEConcordanceResponse extends NoSkEResponse {
    public List<Line> Lines;

    public long fromp;
    public long concsize;
    public long concordance_size_limit;
    public List<Object> Sort_idx;
    public boolean righttoleft;
    public List<Object> Aligned_rtl;
    public int numofcolls;
    public int finished;
    public long fullsize;
    public float relsize;
    public List<String> q;
    public List<QueryDesc> Desc;
    public int port;
    public List<Object> sc_strcts;
    /* "sc_strcts": [  [ "s", "s" ]  ], */

    @JsonIgnore
    public Map<Long, Float> gdex_scores;

    @JsonSetter("gdex_scores")
    public void setGdex_scores(Object gdex_scores) {
        if (gdex_scores instanceof Map) {
            this.gdex_scores = (Map<Long, Float>) gdex_scores;
        } else {
            // gdex_scores instanceof List // empty
            this.gdex_scores = null;
        }
    }

    @Override
    public String toString() {
        return "NoSkEConcordanceResponse [Lines=" + Lines + ", fromp=" + fromp + ", concsize=" + concsize
                + ", concordance_size_limit=" + concordance_size_limit + ", Sort_idx=" + Sort_idx + ", righttoleft="
                + righttoleft + ", Aligned_rtl=" + Aligned_rtl + ", numofcolls=" + numofcolls + ", finished=" + finished
                + ", fullsize=" + fullsize + ", relsize=" + relsize + ", q=" + q + ", Desc=" + Desc + ", port=" + port
                + ", gdex_scores=" + gdex_scores + ", sc_strcts=" + sc_strcts
                + ", api_version=" + api_version + ", manatee_version=" + manatee_version + ", request=" + request
                + "]";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Line {
        public long toknum;
        public int hitlen;
        public List<String> Refs;
        public List<String> Tbl_refs;
        public List<Token> Left;
        public List<Token> Kwic;
        public List<Token> Right;
        public List<Object> Links;
        public String linegroup;
        public int linegroup_id;

        @Override
        public String toString() {
            return "Line [toknum=" + toknum + ", hitlen=" + hitlen + ", Refs=" + Refs + ", Tbl_refs=" + Tbl_refs
                    + ", Left=" + Left + ", Kwic=" + Kwic + ", Right=" + Right + ", Links=" + Links + ", linegroup="
                    + linegroup + ", linegroup_id=" + linegroup_id + "]";
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Token {
            public String str = null;
            public String strc = null;
            public Integer coll = null;

            @Override
            public String toString() {
                return "Token ["
                        + ((str != null) ? "str=" + str : "")
                        + ((strc != null) ? "strc=" + strc : "")
                        + ((coll != null) ? ", coll=" + coll : "")
                        + "]";
            }
        }
    }
}
