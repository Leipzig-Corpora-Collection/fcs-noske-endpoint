package de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint;

import static de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.NoSkESRUFCSConstants.LAYER_PREFIX;
import static de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.NoSkESRUFCSConstants.RESOURCE_PREFIX;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.NoSkeAPI;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo.NoSkECorporaResponse;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo.NoSkECorporaResponse.Corpus;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo.NoSkECorpusInfoResponse;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo.NoSkEViewResponse;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.query.CQLtoNoSkECQLConverter;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.query.FCSQLtoNoSkECQLConverter;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.searcher.MyResults;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.util.LanguagesISO693;
import eu.clarin.sru.server.CQLQueryParser;
import eu.clarin.sru.server.SRUConfigException;
import eu.clarin.sru.server.SRUConstants;
import eu.clarin.sru.server.SRUDiagnosticList;
import eu.clarin.sru.server.SRUException;
import eu.clarin.sru.server.SRUQueryParserRegistry;
import eu.clarin.sru.server.SRURequest;
import eu.clarin.sru.server.SRUSearchEngine;
import eu.clarin.sru.server.SRUSearchResultSet;
import eu.clarin.sru.server.SRUServer;
import eu.clarin.sru.server.SRUServerConfig;
import eu.clarin.sru.server.fcs.Constants;
import eu.clarin.sru.server.fcs.DataView;
import eu.clarin.sru.server.fcs.DataView.DeliveryPolicy;
import eu.clarin.sru.server.fcs.EndpointDescription;
import eu.clarin.sru.server.fcs.FCSQueryParser;
import eu.clarin.sru.server.fcs.Layer;
import eu.clarin.sru.server.fcs.ResourceInfo;
import eu.clarin.sru.server.fcs.SimpleEndpointSearchEngineBase;
import eu.clarin.sru.server.fcs.utils.SimpleEndpointDescription;
import eu.clarin.sru.server.fcs.utils.SimpleEndpointDescriptionParser;
import jakarta.ws.rs.core.UriBuilder;

/**
 * Our implemention of a simple search engine to be used as a CLARIN-FCS
 * endpoint.
 * 
 * @see SimpleEndpointSearchEngineBase
 */
public class NoSkESRUFCSEndpointSearchEngine extends SimpleEndpointSearchEngineBase {
    private static final Logger LOGGER = LogManager.getLogger(NoSkESRUFCSEndpointSearchEngine.class);

    // set in `src/main/webapp/WEB-INF/web.xml` if you want to package a custom
    // endpoint-description.xml file at another location
    private static final String RESOURCE_INVENTORY_URL = "de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.resourceInventoryURL";

    private static NoSkeAPI api;

    /**
     * Endpoint Description with resources, capabilities etc.
     */
    private static EndpointDescription endpointDescription;
    /**
     * List of our endpoint's resources (identified by PID Strings)
     */
    private static List<String> pids;
    /**
     * Our default corpus if SRU requests do no explicitely request a resource
     * by PID with the <code>x-fcs-context</code> parameter.
     * Must not be <code>null</code>!
     */
    private static String defaultCorpusId = null;

    // ---------------------------------------------------------------------
    // params

    /**
     * Read an environment variable from <code>java:comp/env/paramName</code>
     * and return the value as Object.
     *
     * @param paramName
     *                  the environment variables name to extract the value from
     * @return the environment variable value as Object
     */
    protected Object readJndi(String paramName) {
        Object jndiValue = null;
        try {
            final InitialContext ic = new InitialContext();
            jndiValue = ic.lookup("java:comp/env/" + paramName);
        } catch (NamingException e) {
            // handle exception
        }
        return jndiValue;
    }

    /**
     * Read an environment variable and return the value as String.
     * 
     * @param paramName
     *                  the environment variables name to extract the value from
     * @return the environment variable value as String
     */
    protected String getEnvParam(String paramName) {
        return (String) readJndi("param/" + paramName);
    }

    protected Boolean getEnvParamBoolean(String paramName) {
        return (Boolean) readJndi("param/" + paramName);
    }

    // ---------------------------------------------------------------------
    // (No)SketchEngine API

    /**
     * Setup (No)SketchEngine API client.
     * 
     * <p>
     * The <code>NOSKE_API_URI</code> environment variable/servlet parameter will be
     * used to retrieve the API base URI.
     * </p>
     * 
     * @param context
     *                the {@link ServletContext} for the Servlet
     * @param params
     *                additional parameters gathered from the Servlet configuration
     *                and Servlet context.
     * @return the new {@link NoSkeAPI} object
     * @throws SRUConfigException
     */
    protected NoSkeAPI setupAPI(ServletContext context, Map<String, String> params) throws SRUConfigException {
        // or params.get("NOSKE_API_URI")
        String noskeUri = getEnvParam("NOSKE_API_URI");
        final URI baseUri = UriBuilder.fromUri(noskeUri).build();

        try {
            return new NoSkeAPI(baseUri.toString(), false, true);
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new SRUConfigException("Error building API Client for NoSketchEngine!", e);
        }
    }

    // ---------------------------------------------------------------------
    // EndpointDescription stuff

    /**
     * Load the {@link EndpointDescription} from the JAR resources or from the
     * <code>RESOURCE_INVENTORY_URL</code>.
     * 
     * @param context
     *                the {@link ServletContext} for the Servlet
     * @param params
     *                additional parameters gathered from the Servlet configuration
     *                and Servlet context.
     * @return the {@link EndpointDescription} object
     * @throws SRUConfigException
     *                            an error occurred during loading/reading the
     *                            <code>endpoint-description.xml</code> file
     */
    protected EndpointDescription loadEndpointDescriptionFromURI(ServletContext context, Map<String, String> params)
            throws SRUConfigException {
        try {
            URL url = null;
            String riu = params.get(RESOURCE_INVENTORY_URL);
            if ((riu == null) || riu.isEmpty()) {
                url = context.getResource("/WEB-INF/endpoint-description.xml");
                LOGGER.debug("using bundled 'endpoint-description.xml' file");
            } else {
                url = new File(riu).toURI().toURL();
                LOGGER.debug("using external file '{}'", riu);
            }

            return SimpleEndpointDescriptionParser.parse(url);
        } catch (MalformedURLException mue) {
            throw new SRUConfigException("Malformed URL for initializing resource info inventory", mue);
        }
    }

    /**
     * Build an {@link EndpointDescription} object by querying the (No)SketchEngine
     * backend.
     * 
     * <p>
     * NOTE that this operation is expensive for a large number of corpora and the
     * result should be cached.
     * </p>
     * 
     * @return {@link EndpointDescription} for this endpoint
     */
    protected EndpointDescription buildEndpointDescriptionFromNoSkE() {
        LOGGER.info("Building EndpointDescription using NoSkE ...");

        // request corpora from api
        NoSkECorporaResponse corporaResponse = api.corpora();
        LOGGER.debug("Got {} corpora from NoSkE", corporaResponse.data.size());

        // create endpoint description
        HashMap<String, DataView> dataViews = new HashMap<>(2) {
            {
                put("hits", new DataView("hits", "application/x-clarin-fcs-hits+xml", DeliveryPolicy.SEND_BY_DEFAULT));
                put("adv", new DataView("adv", "application/x-clarin-fcs-adv+xml", DeliveryPolicy.SEND_BY_DEFAULT));
            }
        };
        HashMap<String, Layer> layers = new HashMap<>() {
            {
                put("word", new Layer("word", URI.create(LAYER_PREFIX + "word"), "text"));
                put("lc", new Layer("lc", URI.create(LAYER_PREFIX + "lc"), "text"));
                put("lemma", new Layer("lemma", URI.create(LAYER_PREFIX + "lemma"), "lemma"));
                put("lemma_lc", new Layer("lemma_lc", URI.create(LAYER_PREFIX + "lemma_lc"), "lemma"));
                put("pos", new Layer("pos", URI.create(LAYER_PREFIX + "pos"), "pos"));
                put("pos_ud17", new Layer("pos_ud17", URI.create(LAYER_PREFIX + "pos_ud17"), "pos"));
            }
        };
        return new SimpleEndpointDescription(
                2,
                new ArrayList<>() {
                    {
                        add(UriBuilder.fromUri("http://clarin.eu/fcs/capability/basic-search").build());
                        add(UriBuilder.fromUri("http://clarin.eu/fcs/capability/advanced-search").build());
                    }
                },
                new ArrayList<>(dataViews.values()),
                new ArrayList<>(layers.values()),
                new ArrayList<>() {
                    {
                        corporaResponse.data.stream()
                                // NOTE: limit for testing only
                                // .filter(c -> c.name.startsWith("deu"))
                                // .limit(10)
                                .sorted(Comparator.comparing(corpus -> corpus.corpname))
                                .forEach(new Consumer<NoSkECorporaResponse.Corpus>() {
                                    @Override
                                    public void accept(Corpus corpus) {
                                        final NoSkECorpusInfoResponse corpusInfoResponse = api
                                                .corpInfo(corpus.corpname);
                                        final List<String> attributes = corpusInfoResponse.attributes.stream()
                                                .map(att -> att.name)
                                                .collect(Collectors.toList());

                                        String langCode = corpus.language_id;
                                        if (langCode == null || langCode.length() != 3) {
                                            langCode = LanguagesISO693.getInstance()
                                                    .code_3ForName(corpus.language_name);
                                            if (langCode == null) {
                                                LOGGER.warn(
                                                        "No ISO639 language code found for language '{}' in corpus '{}'!",
                                                        corpus.language_name, corpus.corpname);
                                                langCode = corpus.name
                                                        .substring(corpus.name.lastIndexOf('/') + 1)
                                                        .substring(0, 3);
                                            }
                                        }

                                        add(new ResourceInfo(
                                                RESOURCE_PREFIX + corpus.corpname,
                                                new HashMap<>() {
                                                    {
                                                        put("en", corpus.name);
                                                    }
                                                },
                                                new HashMap<>() {
                                                    {
                                                        put("en", corpus.info);
                                                    }
                                                },
                                                corpusInfoResponse.infohref,
                                                List.of(langCode),
                                                new ArrayList<>(dataViews.values()),
                                                new ArrayList<>() {
                                                    {
                                                        add(layers.get("word"));
                                                        add(layers.get("lc"));
                                                        if (attributes.contains("pos")) {
                                                            add(layers.get("pos"));
                                                        }
                                                        if (attributes.contains("lemma")) {
                                                            add(layers.get("lemma"));
                                                            add(layers.get("lemma_lc"));
                                                        }
                                                    }
                                                },
                                                // TODO: subcorpora
                                                null));
                                    }
                                });
                    }
                },
                true);
    }

    /**
     * Get the {@link ResourceInfo} identified by <code>pid</code> from the
     * {@link EndpointDescription} object.
     * 
     * <p>
     * NOTE that we only allow root level resources and currently can not search for
     * sub-resources.
     * </p>
     * <p>
     * NOTE that the PID should exist, so validate the PID using
     * {@link #getResourcesFromEndpointDescription}.
     * </p>
     * 
     * @param ed  Endpoint Description
     * @param pid Resource PID String
     * @return {@link ResourceInfo}
     * @throws SRUException
     */
    protected ResourceInfo getResourceFromEndpointDescriptionByPID(EndpointDescription ed, String pid)
            throws SRUException {
        // NOTE: for now only support on root level
        return ed.getResourceList(EndpointDescription.PID_ROOT).stream().filter(res -> res.getPid().equals((pid)))
                .findAny().get();
    }

    /**
     * Parses the list of root resource PIDs from the {@link EndpointDescription}.
     * 
     * Note: This only considers root resources and not subresources!
     * 
     * @param ed
     *           the {@link EndpointDescription} for the Servlet
     * @return a list of String with root resource PIDs
     */
    protected List<String> getResourcesFromEndpointDescription(EndpointDescription ed) throws SRUException {
        // NOTE: only root resources!
        return ed.getResourceList(EndpointDescription.PID_ROOT).stream().map(ResourceInfo::getPid)
                .collect(Collectors.toList());
    }

    /**
     * Create {@link EndpointDescription} for this servlet.
     * 
     * @see #loadEndpointDescriptionFromURI(ServletContext, Map)
     * @see SimpleEndpointSearchEngineBase#createEndpointDescription(ServletContext,
     *      SRUServerConfig, Map)
     */
    @Override
    protected EndpointDescription createEndpointDescription(ServletContext context, SRUServerConfig config,
            Map<String, String> params) throws SRUConfigException {
        LOGGER.info("SRUServlet::createEndpointDescription");
        if (endpointDescription == null) {
            if (getEnvParamBoolean("FCS_RESOURCES_FROM_NOSKE")) {
                endpointDescription = buildEndpointDescriptionFromNoSkE();
            } else {
                endpointDescription = loadEndpointDescriptionFromURI(context, params);
            }
        }
        return endpointDescription;
    }

    // ---------------------------------------------------------------------
    // init

    /**
     * Initialize the search engine. This initialization should be tailed
     * towards your environment and needs.
     *
     * @param context
     *                            the {@link ServletContext} for the Servlet
     * @param config
     *                            the {@link SRUServerConfig} object for this search
     *                            engine
     * @param queryParsersBuilder
     *                            the {@link SRUQueryParserRegistry.Builder} object
     *                            to be used
     *                            for this search engine. Use to register additional
     *                            query
     *                            parsers with the {@link SRUServer}.
     * @param params
     *                            additional parameters gathered from the Servlet
     *                            configuration
     *                            and Servlet context.
     * @throws SRUConfigException
     *                            if an error occurred
     *
     * @see SimpleEndpointSearchEngineBase#doInit(ServletContext, SRUServerConfig,
     *      SRUQueryParserRegistry.Builder, Map)
     */
    @Override
    protected void doInit(ServletContext context, SRUServerConfig config,
            SRUQueryParserRegistry.Builder queryParsersBuilder, Map<String, String> params)
            throws SRUConfigException {
        LOGGER.info("SRUServlet::doInit {}", config.getPort());

        /* setup resource PID prefix, e.g. "my:" */
        LOGGER.debug("RESOURCE_PREFIX = {}", RESOURCE_PREFIX);

        /* initialize NoSkE API Client */
        api = setupAPI(context, params);

        /* load and store endpoint description */
        createEndpointDescription(context, config, params);

        /* process endpoint description, load available PIDs */
        try {
            pids = getResourcesFromEndpointDescription(endpointDescription);
        } catch (SRUException e) {
            throw new SRUConfigException("Error extracting resource pids", e);
        }
        LOGGER.info("Got root resource PIDs: {}", pids);

        /* set default corpus ID */
        // or params.get("DEFAULT_RESOURCE_PID")
        defaultCorpusId = getEnvParam("DEFAULT_RESOURCE_PID");
        LOGGER.info("Got defaultCorpusId resource PID: {}", defaultCorpusId);
        if (defaultCorpusId == null || !pids.contains(defaultCorpusId)) {
            throw new SRUConfigException("Parameter 'DEFAULT_RESOURCE_PID' contains unknown resource pid!");
        }
    }

    // ---------------------------------------------------------------------
    // search

    /**
     * Handle a <em>searchRetrieve</em> operation.
     *
     * @see SRUSearchEngine#search(SRUServerConfig, SRURequest,
     *      SRUDiagnosticList)
     */
    @Override
    public SRUSearchResultSet search(SRUServerConfig config, SRURequest request, SRUDiagnosticList diagnostics)
            throws SRUException {
        /* validate params */
        List<String> pids = parsePids(request);
        pids = checkPids(pids, diagnostics);
        LOGGER.debug("Search restricted to PIDs: {}", pids);
        /* we restrict our search to the first PID */
        final String pid = checkPid(pids);
        LOGGER.debug("Search restricted to first PID: {}", pid);

        /* get corpus/resource info from pid */
        final String corpname = pid.substring(RESOURCE_PREFIX.length());
        final ResourceInfo ri = getResourceFromEndpointDescriptionByPID(endpointDescription, pid);

        /* check against search query types that are not supported */
        final boolean hasADVCap = ri.getAvailableDataViews().stream()
                .filter(dv -> dv.getIdentifier().equalsIgnoreCase("adv")).findAny().isPresent();
        if (request.isQueryType(Constants.FCS_QUERY_TYPE_FCS) && !hasADVCap) {
            // TODO: what is here the correct diagnostic?
            // http://docs.oasis-open.org/search-ws/searchRetrieve/v1.0/os/part3-sru2.0/searchRetrieve-v1.0-os-part3-sru2.0.html#_Toc324162491
            // http://clarin.eu/fcs/diagnostic/14 - General processing hint. - non fatal?
            // http://clarin.eu/fcs/diagnostic/4 - Requested Data View not valid for this
            // resource. - fatal
            throw new SRUException(SRUConstants.SRU_GENERAL_SYSTEM_ERROR,
                    "Performing Advanced Search query for this resource is not supported!");
        }

        List<String> dataviews = parseDataViews(request, diagnostics, pid);
        LOGGER.debug("Search requested dataviews: {}", dataviews);

        /* attributes for ADVANCED/layers */
        final String[] attrs = ri.getAvailableLayers().stream().map(l -> l.getId()).collect(Collectors.toList())
                .toArray(new String[] {});
        LOGGER.debug("Layer attributes: {}", Arrays.asList(attrs));

        /* parse and translate query */
        String query = parseQuery(request, pid);
        // update query for API
        // TODO: add gdex?
        query = "q" + query;

        int startRecord = ((request.getStartRecord() < 1) ? 1 : request.getStartRecord()) - 1;
        int maximumRecords = request.getMaximumRecords();
        // LOGGER.debug("Search window: start:{}, size:{}", startRecord,
        // maximumRecords);

        /*
         * start search (query = query, offset = startRecord, limit = maximumRecords)
         */
        MyResults results = null;
        if (request.isQueryType(Constants.FCS_QUERY_TYPE_CQL)) {
            // BASIC/CQL/fulltext search

            NoSkEViewResponse concResults = api.concordanceSimple(corpname, query, maximumRecords, startRecord);
            // LOGGER.debug("results: {}", concResults);

            if (concResults == null || concResults.Lines == null) {
                throw new SRUException(SRUConstants.SRU_GENERAL_SYSTEM_ERROR, "Error in Searcher");
            }

            results = new MyResults(pid, ri, query, new ArrayList<MyResults.ResultEntry>() {
                {
                    concResults.Lines.forEach(line -> {
                        MyResults.ResultEntry entry = new MyResults.ResultEntry();
                        // kwic result
                        entry.left = line.Left.stream().filter(e -> !e.sClass.equals("strc")).map(e -> e.str).findAny()
                                .orElse(null);
                        entry.kwic = line.Kwic.get(0).str;
                        entry.right = line.Right.stream().filter(e -> !e.sClass.equals("strc")).map(e -> e.str)
                                .findAny()
                                .orElse(null);
                        // metadata
                        entry.sid = line.Refs.get(0);
                        entry.date = line.Refs.get(1);
                        entry.url = line.Refs.get(2);
                        // sid?
                        entry.landingpage = pid + "#sid=" + entry.sid;
                        add(entry);
                    });
                }
            }, concResults.fullsize, startRecord);
        } else if (request.isQueryType(Constants.FCS_QUERY_TYPE_FCS)) {
            // ADVANCED/Layers search/results

            NoSkEViewResponse concResults = api.concordanceLayers(corpname, query, attrs, maximumRecords, startRecord);
            // LOGGER.debug("results: {}", concResults);

            if (concResults == null || concResults.Lines == null) {
                throw new SRUException(SRUConstants.SRU_GENERAL_SYSTEM_ERROR, "Error in Searcher");
            }

            results = new MyResults(pid, ri, query, new ArrayList<MyResults.ResultEntry>() {
                {
                    concResults.Lines.forEach(line -> {
                        MyResults.LayerResultEntry entry = new MyResults.LayerResultEntry();
                        // kwic result
                        entry.leftTokens = convertNoSkELineTokensToKWICTokens(line.Left, attrs);
                        entry.kwicTokens = convertNoSkELineTokensToKWICTokens(line.Kwic, attrs);
                        entry.rightTokens = convertNoSkELineTokensToKWICTokens(line.Right, attrs);
                        // metadata
                        entry.sid = line.Refs.get(0);
                        entry.date = line.Refs.get(1);
                        entry.url = line.Refs.get(2);
                        // sid?
                        entry.landingpage = pid + "#s.id=" + entry.sid;
                        add(entry);
                    });
                }
            }, concResults.fullsize, startRecord);
        }

        if (results == null) {
            throw new SRUException(SRUConstants.SRU_GENERAL_SYSTEM_ERROR, "Error trying to wrap results");
        }

        /* wrap results into custom SRUSearchResultSet */
        return new NoSkESRUFCSSearchResultSet(config, request, diagnostics, dataviews, results);
    }

    /**
     * Internal. Convert a single (No)SketchEngine result line into FCS KWIC token
     * format.
     *
     * @param lineTokens  (No)SketchEngine single result line tokens
     * @param corpusAttrs Array with corpus attributes
     * @return List of tokens, each token is a mapping of corpus attribute to value.
     */
    protected List<Map<String, String>> convertNoSkELineTokensToKWICTokens(
            List<NoSkEViewResponse.Line.TokenOrString> lineTokens, String[] corpusAttrs) {
        List<Map<String, String>> kwicTokens = new ArrayList<>();
        lineTokens.stream().filter(e -> !e.sClass.equals("strc")).collect(Collectors.toList())
                .forEach(t -> {
                    if (/* normal token */ t.sClass == null || t.sClass.isBlank()
                            || /* kwic token */ t.sClass.equalsIgnoreCase("col0 coll")) {
                        kwicTokens.add(new HashMap<>());
                        kwicTokens.get(kwicTokens.size() - 1).put(corpusAttrs[0], t.str);
                    } else if (t.sClass.equalsIgnoreCase("attr")) {
                        if (kwicTokens.size() <= 0) {
                            return;
                        }
                        Map<String, String> res = kwicTokens.get(kwicTokens.size() - 1);
                        String[] parts = t.str.split("/", -1); // do not remove trailing empty results!
                        parts = Arrays.copyOfRange(parts, 1, parts.length);
                        for (int i = 1; i < corpusAttrs.length; i++) {
                            res.put(corpusAttrs[i], parts[i - 1]);
                        }
                    }
                });
        return kwicTokens;
    }

    // ---------------------------------------------------------------------
    // search param utils (parsing/validation)

    /**
     * Extract and parse the query from the {@link SRURequest}.
     *
     * @param request
     *                the {@link SRURequest} with request parameters
     * @return the raw query as String
     * @throws SRUException
     *                      if an error occurred trying to extract or to parse the
     *                      query
     *
     * @see #search(SRUServerConfig, SRURequest, SRUDiagnosticList)
     */
    protected String parseQuery(SRURequest request, String pid) throws SRUException {
        /* filter search attributes based on supported layers from corpus */
        ResourceInfo ri = getResourceFromEndpointDescriptionByPID(endpointDescription, pid);
        Set<String> attrs = ri.getAvailableLayers().stream().map(layer -> layer.getId()).collect(Collectors.toSet());
        attrs.retainAll(Set.of("lc", "lemma_lc"));
        String[] searchAttrs = attrs.toArray(new String[attrs.size()]);

        final String skeCQLQuery;
        if (request.isQueryType(Constants.FCS_QUERY_TYPE_CQL)) {
            /*
             * Got a CQL query (either SRU 1.1 or higher).
             * Translate to a proper NoSketchEngine CQL query ...
             */
            final CQLQueryParser.CQLQuery q = request.getQuery(CQLQueryParser.CQLQuery.class);
            LOGGER.info("FCS-CQL query: {}", q.getRawQuery());

            try {
                skeCQLQuery = CQLtoNoSkECQLConverter.convertCQLtoNoSkECQL(q.getParsedQuery(), searchAttrs);
                LOGGER.debug("SketchEngine query: {}", skeCQLQuery);
            } catch (Exception e) {
                if (e instanceof SRUException) {
                    throw e;
                }
                throw new SRUException(
                        SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                        "Converting query with queryType 'cql' to SketchEngine CQL failed.",
                        e);
            }
        } else if (request.isQueryType(Constants.FCS_QUERY_TYPE_FCS)) {
            /*
             * Got a FCS query (SRU 2.0).
             * Translate to a proper CQP query
             */
            final FCSQueryParser.FCSQuery q = request.getQuery(FCSQueryParser.FCSQuery.class);
            LOGGER.info("FCS-QL query: {}", q.getRawQuery());

            try {
                // NOTE: searchAttrs may be required for more complicated conversion?
                skeCQLQuery = FCSQLtoNoSkECQLConverter.convertFCSQLtoNoSkECQL(q.getParsedQuery(), searchAttrs);
                LOGGER.debug("SketchEngine query: {}", skeCQLQuery);
            } catch (Exception e) { // TODO: check type?
                // TODO: check if diagnostic for unsupported field?
                if (e instanceof SRUException) {
                    throw e;
                }
                throw new SRUException(
                        SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                        "Converting query with queryType 'fcs' to SketchEngine CQL failed.",
                        e);
            }
        } else {
            /*
             * Got something else we don't support. Send error ...
             */
            throw new SRUException(
                    SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                    "Queries with queryType '" +
                            request.getQueryType() +
                            "' are not supported by this FCS Endpoint.");
        }
        return skeCQLQuery;
    }

    /**
     * Extract and parse the requested resource PIDs from the {@link SRURequest}.
     * 
     * Returns the list of resource PIDs if <code>x-fcs-context</code> parameter
     * was used and it was non-empty. If no FCS context was set, then return with
     * the <code>defaultCorpusId</code>.
     *
     * @param request
     *                the {@link SRURequest} with request parameters
     * @return a list of String resource PIDs
     *
     * @see #search(SRUServerConfig, SRURequest, SRUDiagnosticList)
     */
    protected List<String> parsePids(SRURequest request) throws SRUException {
        boolean hasFcsContextCorpus = false;
        String fcsContextCorpus = "";

        for (String erd : request.getExtraRequestDataNames()) {
            if (NoSkESRUFCSConstants.X_FCS_CONTEXT_KEY.equals(erd)) {
                hasFcsContextCorpus = true;
                fcsContextCorpus = request.getExtraRequestData(NoSkESRUFCSConstants.X_FCS_CONTEXT_KEY);
                break;
            }
        }
        if (!hasFcsContextCorpus || "".equals(fcsContextCorpus)) {
            LOGGER.debug("Received 'searchRetrieve' request without x-fcs-context - Using default '{}'",
                    defaultCorpusId);
            fcsContextCorpus = defaultCorpusId;
        }
        if (fcsContextCorpus == null) {
            return new ArrayList<>();
        }

        List<String> selectedPids = new ArrayList<>(Arrays.asList(fcsContextCorpus.split(
                NoSkESRUFCSConstants.X_FCS_CONTEXT_SEPARATOR)));

        return selectedPids;
    }

    /**
     * Validate the requested resource PIDs from the {@link SRURequest} against
     * the list of resource PIDs declared in the servlet's
     * {@link EndpointDescription}.
     * 
     * Returns the list of valid resource PIDs. Generates SRU diagnostics for
     * each invalid/unknown resource PID. If the list of valid PIDs is empty
     * then raise an {@link SRUException}.
     *
     * @param pids
     *                    the list of resource PIDs
     * @param diagnostics
     *                    the {@link SRUDiagnosticList} object for storing
     *                    non-fatal diagnostics
     * @return a list of String resource PIDs
     * @throws SRUException
     *                      if no valid resource PIDs left
     *
     * @see #search(SRUServerConfig, SRURequest, SRUDiagnosticList)
     * @see #getResourcesFromEndpointDescription(EndpointDescription)
     * @see #parsePids(SRURequest)
     */
    protected List<String> checkPids(List<String> pids, SRUDiagnosticList diagnostics) throws SRUException {
        // set valid and existing resource PIDs
        List<String> knownPids = new ArrayList<>();
        for (String pid : pids) {
            if (!NoSkESRUFCSEndpointSearchEngine.pids.contains(pid)) {
                // allow only valid resources that can be queried by CQL
                diagnostics.addDiagnostic(
                        Constants.FCS_DIAGNOSTIC_PERSISTENT_IDENTIFIER_INVALID,
                        pid,
                        "Resource PID for search is not valid or can not be queried by FCS/CQL!");
            } else {
                knownPids.add(pid);
            }
        }
        if (knownPids.isEmpty()) {
            // if search was restricted to resources but all were invalid, then do we fail?
            // or do we adjust to our default corpus?
            throw new SRUException(
                    SRUConstants.SRU_UNSUPPORTED_PARAMETER_VALUE,
                    "All values passed to '" + NoSkESRUFCSConstants.X_FCS_CONTEXT_KEY
                            + "' were not valid PIDs or can not be queried by FCS/CQL.");
        }

        return knownPids;
    }

    /**
     * Validate the requested resource PIDs from the {@link SRURequest} to be
     * only a single PID as this endpoint can only handle searching through one
     * resource at a time.
     * 
     * NOTE: The CLARIN SRU/FCS Aggregator also only seems to request results
     * for each resource separately, we only allow requests with one resource!
     * 
     * Returns the resource PID. Raises an {@link SRUException} if more than
     * one resource PID in <code>pids</code>.
     *
     * @param pids
     *             the list of resource PIDs
     * @return the resource PID as String
     * @throws SRUException
     *                      if no valid resource PIDs left
     *
     * @see #search(SRUServerConfig, SRURequest, SRUDiagnosticList)
     * @see #checkPids(List, SRUDiagnosticList)
     */
    protected String checkPid(List<String> pids) throws SRUException {
        // NOTE: we only search for first PID
        // (FCS Aggregator only provides one resource PID per search request, so
        // multiple PIDs should usually not happen)
        final String pid;
        if (pids.size() > 1) {
            throw new SRUException(
                    SRUConstants.SRU_UNSUPPORTED_PARAMETER_VALUE,
                    "Parameter '" + NoSkESRUFCSConstants.X_FCS_CONTEXT_KEY
                            + "' received multiple PIDs. Endpoint only supports a single PIDs for querying by CQL/FCS-QL/LexCQL.");

            // TODO: http://clarin.eu/fcs/diagnostic/2
        } else if (pids.size() == 0) {
            pid = defaultCorpusId;
            LOGGER.debug("Falling back to default resource: {}", pid);
            pids.add(pid);
        } else {
            pid = pids.get(0);
        }
        return pid;
    }

    /**
     * Extract and parse the requested result Data Views from the
     * {@link SRURequest}.
     * 
     * Returns the list of Data View identifiers if <code>x-fcs-dataviews</code>
     * parameter was used and is non-empty.
     * 
     * Validates the requested Data Views against the ones declared in the servlet's
     * {@link EndpointDescription} for the resource identified by the value in
     * <code>pid</code>. For each non-valid Data View generate a SRU diagnostic.
     *
     * @param request
     *                    the {@link SRURequest} with request parameters
     * @param diagnostics
     *                    the {@link SRUDiagnosticList} object for storing
     *                    non-fatal diagnostics
     * @param pid
     *                    resource PID String, to validate requested Data Views
     * @return a list of String Data View identifiers, may be empty
     *
     * @see #search(SRUServerConfig, SRURequest, SRUDiagnosticList)
     */
    protected List<String> parseDataViews(SRURequest request, SRUDiagnosticList diagnostics, String pid)
            throws SRUException {
        List<String> extraDataviews = new ArrayList<>();
        if (request != null) {
            for (String erd : request.getExtraRequestDataNames()) {
                if (NoSkESRUFCSConstants.X_FCS_DATAVIEWS_KEY.equals(erd)) {
                    String dvs = request.getExtraRequestData(NoSkESRUFCSConstants.X_FCS_DATAVIEWS_KEY);
                    extraDataviews = new ArrayList<>(
                            Arrays.asList(dvs.split(NoSkESRUFCSConstants.X_FCS_DATAVIEWS_SEPARATOR)));
                    break;
                }
            }
        }
        if (extraDataviews.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> resourceDataViews = getResourceFromEndpointDescriptionByPID(endpointDescription, pid)
                .getAvailableDataViews().stream()
                .map(DataView::getIdentifier).collect(Collectors.toSet());

        List<String> allowedDataViews = new ArrayList<>();
        for (String dv : extraDataviews) {
            if (!resourceDataViews.contains(dv)) {
                // allow only valid dataviews for this resource that can be requested
                diagnostics.addDiagnostic(
                        Constants.FCS_DIAGNOSTIC_PERSISTENT_IDENTIFIER_INVALID,
                        pid,
                        "DataViews with identifier '" + dv + "' for resource PID='" + pid + "' is not valid!");
            } else {
                allowedDataViews.add(dv);
            }
        }
        return allowedDataViews;
    }
}
