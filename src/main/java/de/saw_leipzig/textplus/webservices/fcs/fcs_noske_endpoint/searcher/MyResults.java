package de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.searcher;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import eu.clarin.sru.server.fcs.ResourceInfo;

/**
 * <em>Wrapper</em> around own, proprietary results.
 * 
 * Exposes metadata like the resource PID and search query as well as
 * result offset and total result count for pagination.
 * 
 * In particular the {@link ResultEntry} class is customized for the
 * (No)SketchEngine search engine results.
 */
public class MyResults {
    /** Resource PID */
    private String pid;
    /**
     * {@link ResourceInfo} object to allow access to declared resource meta data
     * (layers, dataviews, ...)
     */
    private ResourceInfo resourceInfo;
    /** Query to get this resultset. */
    private String query;
    /** Actual result entries (rows, ...). */
    private List<ResultEntry> results;
    /** Window parameter, how many results in total at the backend. */
    private long total;
    /**
     * Window parameter, at which offset are we currently in the total result list.
     */
    private long offset;

    public MyResults(String pid, ResourceInfo resourceInfo, String query, List<ResultEntry> results, long total,
            long offset) {
        this.pid = pid;
        this.resourceInfo = resourceInfo;
        this.query = query;
        this.results = results;
        this.total = total;
        this.offset = offset;
    }

    public String getPid() {
        return pid;
    }

    public ResourceInfo getResourceInfo() {
        return resourceInfo;
    }

    /**
     * Return resource Advanced Data View Layer IDs.
     * 
     * @return String Array with layer IDs
     */
    public String[] getLayerAttrs() {
        return resourceInfo.getAvailableLayers().stream().map(l -> l.getId()).collect(Collectors.toList())
                .toArray(new String[] {});
    }

    public String getQuery() {
        return query;
    }

    public List<ResultEntry> getResults() {
        return results;
    }

    public long getTotal() {
        return total;
    }

    public long getOffset() {
        return offset;
    }

    /**
     * Minimal single result entry.
     * Consisting of only a left/kwic/right texts, metadata and
     * backlink to the result (if available).
     */
    public static class ResultEntry {
        public String left;
        public String kwic;
        public String right;
        // metadata
        public String sid;
        public String url;
        public String date;
        // link to results
        public String landingpage;

        public ResultEntry() {
        }

        @Override
        public String toString() {
            return "ResultEntry [left=" + left + ", kwic=" + kwic + ", right=" + right + ", sid=" + sid + ", url=" + url
                    + ", date=" + date + ", landingpage=" + landingpage + "]";
        }
    }

    /**
     * Extended result entry that has tokens for kwic, left and right context.
     * Each token should be a Map with attribute layer ID and value. The layer IDs
     * should match the whole list of IDs retrivable by
     * {@link MyResults#getLayerAttrs}.
     */
    public static class LayerResultEntry extends ResultEntry {
        public List<Map<String, String>> leftTokens;
        public List<Map<String, String>> kwicTokens;
        public List<Map<String, String>> rightTokens;

        @Override
        public String toString() {
            return "LayerResultEntry [leftTokens=" + leftTokens + ", kwicTokens=" + kwicTokens + ", rightTokens="
                    + rightTokens + ", sid=" + sid + ", url=" + url
                    + ", date=" + date + ", landingpage=" + landingpage + "]";
        }

    }
}
