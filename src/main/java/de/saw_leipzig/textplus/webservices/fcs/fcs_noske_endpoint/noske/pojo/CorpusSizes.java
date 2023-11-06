package de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Corpus sizes. <code>null</code> if corpus is not compiled.
 */
// NOTE: will there be additional counts for user-specified structures?
@JsonIgnoreProperties(ignoreUnknown = true)
public class CorpusSizes {
    /**
     * Total number of <em>tokens</em> in corpus.
     */
    public long tokencount;
    /**
     * Total number of <em>words</em> (tokens minus punctuation etc.) in corpus.
     */
    public long wordcount;
    /**
     * Total number of <em>documents</em> in corpus.
     */
    public long doccount;
    /**
     * Total number of <em>paragraphs</em> in corpus.
     */
    public long parcount;
    /**
     * Total number of <em>sentences</em> in corpus.
     */
    public long sentcount;

    @Override
    public String toString() {
        return "CorpusSizes [tokencount=" + tokencount + ", wordcount=" + wordcount + ", doccount=" + doccount
                + ", parcount=" + parcount + ", sentcount=" + sentcount + "]";
    }
}
