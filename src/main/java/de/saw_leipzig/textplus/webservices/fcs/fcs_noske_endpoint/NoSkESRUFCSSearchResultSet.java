package de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint;

import static de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.NoSkESRUFCSConstants.LAYER_PREFIX;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.searcher.MyResults;
import eu.clarin.sru.server.SRUConstants;
import eu.clarin.sru.server.SRUDiagnostic;
import eu.clarin.sru.server.SRUDiagnosticList;
import eu.clarin.sru.server.SRUException;
import eu.clarin.sru.server.SRURequest;
import eu.clarin.sru.server.SRUSearchResultSet;
import eu.clarin.sru.server.SRUServerConfig;
import eu.clarin.sru.server.fcs.AdvancedDataViewWriter;
import eu.clarin.sru.server.fcs.Constants;
import eu.clarin.sru.server.fcs.XMLStreamWriterHelper;

/**
 * A result set of a <em>searchRetrieve</em> operation. It it used to iterate
 * over the result set and provides a method to serialize the record in the
 * requested format.
 * <p>
 * A <code>SRUSearchResultSet</code> object maintains a cursor pointing to its
 * current record. Initially the cursor is positioned before the first record.
 * The <code>next</code> method moves the cursor to the next record, and because
 * it returns <code>false</code> when there are no more records in the
 * <code>SRUSearchResultSet</code> object, it can be used in a
 * <code>while</code> loop to iterate through the result set.
 * </p>
 * <p>
 * A required implemention for the target search engine.
 * </p>
 * <p>
 * This class only implements the minimal set of methods required to be
 * a valid implementation and to run.
 * </p>
 *
 * @see SRUSearchResultSet
 * @see <a href="http://www.loc.gov/standards/sru/specs/search-retrieve.html">
 *      SRU Search Retrieve Operation</a>
 */
public class NoSkESRUFCSSearchResultSet extends SRUSearchResultSet {
    private static final Logger LOGGER = LogManager.getLogger(NoSkESRUFCSSearchResultSet.class);

    /**
     * SRU Server Config, might be useful for response generation?
     */
    SRUServerConfig serverConfig = null;
    /**
     * The SRU Request we generate our response for. Can be used to access
     * requested parameters.
     */
    SRURequest request = null;
    /**
     * The list of Data View identifiers we need to generate our response for.
     */
    private Set<String> extraDataviews;

    /**
     * Array of layer attributes declared in EndpointDescription/Resource.
     */
    private String[] layerAttrs;

    /**
     * Results wrapper container for easy access to metadata (total count etc.)
     */
    private MyResults results;
    /**
     * The record cursor position for iterating through the result set.
     */
    private int currentRecordCursor = 0;

    /**
     * Constructor.
     * 
     * @param serverConfig
     *                     the {@link SRUServerConfig} object for this search engine
     * @param request
     *                     the {@link SRURequest} with request parameters
     * @param diagnostics
     *                     the {@link SRUDiagnosticList} object for storing
     *                     non-fatal diagnostics
     * @param dataviews
     *                     a list of String Data View identifiers to generate
     *                     responses for.
     *                     May be empty but must not be <code>null</code>.
     * @param results
     *                     the actual results from the search engine
     */
    protected NoSkESRUFCSSearchResultSet(SRUServerConfig serverConfig, SRURequest request,
            SRUDiagnosticList diagnostics, List<String> dataviews, MyResults results) {
        super(diagnostics);
        this.serverConfig = serverConfig;
        this.request = request;

        this.results = results;
        this.layerAttrs = results.getLayerAttrs();
        currentRecordCursor = -1;

        extraDataviews = new HashSet<>(dataviews);
    }

    /**
     * An identifier for the current record by which it can unambiguously be
     * retrieved in a subsequent operation.
     *
     * @return identifier for the record or <code>null</code> of none is
     *         available
     * @throws NoSuchElementException
     *                                result set is past all records
     */
    @Override
    public String getRecordIdentifier() {
        return null;
    }

    /**
     * Serialize the current record in the requested format.
     *
     * @param writer
     *               the {@link XMLStreamException} instance to be used
     * @throws XMLStreamException
     *                                an error occurred while serializing the result
     * @throws NoSuchElementException
     *                                result set past all records
     * @see #getRecordSchemaIdentifier()
     */
    @Override
    public String getRecordSchemaIdentifier() {
        return request.getRecordSchemaIdentifier() != null ? request.getRecordSchemaIdentifier()
                : NoSkESRUFCSConstants.CLARIN_FCS_RECORD_SCHEMA;
    }

    /**
     * Get surrogate diagnostic for current record. If this method returns a
     * diagnostic, the writeRecord method will not be called. The default
     * implementation returns <code>null</code>.
     *
     * @return a surrogate diagnostic or <code>null</code>
     * @see <a href=
     *      "https://github.com/clarin-eric/fcs-korp-endpoint/blob/ffccf7f65cc55744e1b1a8cebacce5485c530bda/src/main/java/se/gu/spraakbanken/fcs/endpoint/korp/KorpSRUSearchResultSet.java#L242-L253">
     *      Reference Implementation, Korp Endpoint</a>
     */
    @Override
    public SRUDiagnostic getSurrogateDiagnostic() {
        if ((getRecordSchemaIdentifier() != null) &&
                !NoSkESRUFCSConstants.CLARIN_FCS_RECORD_SCHEMA.equals(getRecordSchemaIdentifier())) {
            return new SRUDiagnostic(
                    SRUConstants.SRU_RECORD_NOT_AVAILABLE_IN_THIS_SCHEMA,
                    getRecordSchemaIdentifier(),
                    "Record is not available in record schema \"" +
                            getRecordSchemaIdentifier() + "\".");
        }

        return null;
    }

    /**
     * The number of records matched by the query. If the query fails this must
     * be 0. If the search engine cannot determine the total number of matched
     * by a query, it must return -1.
     *
     * @return the total number of results or 0 if the query failed or -1 if the
     *         search engine cannot determine the total number of results
     */
    @Override
    public int getTotalRecordCount() {
        return (int) results.getTotal();
    }

    /**
     * The number of records matched by the query but at most as the number of
     * records requested to be returned (maximumRecords parameter). If the query
     * fails this must be 0.
     *
     * @return the number of results or 0 if the query failed
     */
    @Override
    public int getRecordCount() {
        return results.getResults().size();
    }

    /**
     * Moves the cursor forward one record from its current position. A
     * <code>SRUSearchResultSet</code> cursor is initially positioned before the
     * first record; the first call to the method <code>next</code> makes the
     * first record the current record; the second call makes the second record
     * the current record, and so on.
     * <p>
     * When a call to the <code>next</code> method returns <code>false</code>,
     * the cursor is positioned after the last record.
     * </p>
     *
     * @return <code>true</code> if the new current record is valid;
     *         <code>false</code> if there are no more records
     * @throws SRUException
     *                      if an error occurred while fetching the next record
     */
    @Override
    public boolean nextRecord() throws SRUException {
        if (currentRecordCursor < (getRecordCount() - 1)) {
            currentRecordCursor++;
            return true;
        }
        return false;
    }

    /**
     * Serialize the current record in the requested format.
     *
     * @param writer
     *               the {@link XMLStreamException} instance to be used
     * @throws XMLStreamException
     *                                an error occurred while serializing the result
     * @throws NoSuchElementException
     *                                result set past all records
     * @see #getRecordSchemaIdentifier()
     */
    @Override
    public void writeRecord(XMLStreamWriter writer) throws XMLStreamException {
        MyResults.ResultEntry result = results.getResults().get(currentRecordCursor);

        XMLStreamWriterHelper.writeStartResource(writer, results.getPid(), null);
        XMLStreamWriterHelper.writeStartResourceFragment(writer, null, result.landingpage);

        if (result instanceof MyResults.LayerResultEntry) {
            MyResults.LayerResultEntry layerResult = (MyResults.LayerResultEntry) result;

            Map<String, URI> layerIds = new HashMap<>() {
                {
                    for (String attr : layerAttrs) {
                        put(attr, URI.create(LAYER_PREFIX + attr));
                    }
                }
            };
            URI wordLayerId = layerIds.get("word");

            AdvancedDataViewWriter helper = new AdvancedDataViewWriter(AdvancedDataViewWriter.Unit.ITEM);

            long start = 1;
            // left context
            start = addSpans(layerResult.leftTokens, start, -1, layerIds, helper);
            // kwic hit
            start = addSpans(layerResult.kwicTokens, start, 1, layerIds, helper);
            // right context
            start = addSpans(layerResult.rightTokens, start, -1, layerIds, helper);

            helper.writeHitsDataView(writer, wordLayerId);
            if (request == null || request.isQueryType(Constants.FCS_QUERY_TYPE_FCS)) {
                helper.writeAdvancedDataView(writer);
            }
        } else {
            XMLStreamWriterHelper.writeHitsDataView(writer, result.left, result.kwic, result.right);
        }

        XMLStreamWriterHelper.writeEndResourceFragment(writer);
        XMLStreamWriterHelper.writeEndResource(writer);
    }

    /**
     * Add tokens to Data View as Spans.
     *
     * @param tokens    List of tokens (String-Map)
     * @param start     Start Offset
     * @param highlight Whether current tokens are highlights (<code>1</code> or not
     *                  <code>-1</code>)
     * @param layerIds  Map with layer-id and URIs
     * @param helper    Advanced Data View Writer to serialize Spans
     * @return New Start Offset for subsequent calls (left -> kwic -> right)
     */
    protected long addSpans(List<Map<String, String>> tokens, long start, int highlight, Map<String, URI> layerIds,
            AdvancedDataViewWriter helper) {
        for (Map<String, String> tok : tokens) {
            // LOGGER.debug("tok: {}", tok);
            // compute ranges
            final String curToken = tok.get("word");
            long end = start + curToken.length() - 1;
            if (curToken.startsWith(" ")) {
                start += curToken.length() - curToken.stripLeading().length();
            }
            // store all fields but with same start:end ranges to connect together
            for (Map.Entry<String, URI> layerId : layerIds.entrySet()) {
                helper.addSpan(layerId.getValue(), start, end, tok.get(layerId.getKey()).stripLeading(), highlight);
            }
            start = end + 1;
        }
        return start;
    }
}
