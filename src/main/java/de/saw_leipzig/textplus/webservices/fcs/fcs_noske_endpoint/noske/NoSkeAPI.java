package de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.logging.LoggingFeature;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo.NoSkEConcordanceResponse;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo.NoSkECorporaResponse;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo.NoSkECorpusInfoResponse;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo.NoSkEResponse;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo.NoSkEViewResponse;
import jakarta.json.Json;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriBuilder;

public class NoSkeAPI {
    private static final Logger LOGGER = LogManager.getLogger(NoSkeAPI.class);

    static {
        // org.slf4j.bridge.SLF4JBridgeHandler.removeHandlersForRootLogger();
        // org.slf4j.bridge.SLF4JBridgeHandler.install();

        org.apache.logging.log4j.jul.Log4jBridgeHandler.install(true, "", true);
    }

    // ---------------------------------------------------------------------

    protected URI baseUri;
    protected Client client;

    public NoSkeAPI(String baseUri, boolean insecure, boolean jsonIgnoreUnknown)
            throws KeyManagementException, NoSuchAlgorithmException {
        this.baseUri = UriBuilder.fromUri(baseUri).build();
        this.client = buildClient(insecure, jsonIgnoreUnknown);
    }

    public NoSkeAPI(String baseUri)
            throws KeyManagementException, NoSuchAlgorithmException {
        this(baseUri, false, false);
    }

    public NoSkeAPI(String baseUri, boolean insecure, boolean jsonIgnoreUnknown, String username, String password)
            throws KeyManagementException, NoSuchAlgorithmException {
        this(baseUri, insecure, jsonIgnoreUnknown);
        this.authenticate(username, password);
    }

    public NoSkeAPI authenticate(String username, String password) {
        // https://howtodoinjava.com/jersey/jersey-rest-client-authentication/
        this.client.register(HttpAuthenticationFeature.basicBuilder()
                // .nonPreemptive()
                .credentials(username, password)
                .build());
        return this;
    }

    // ---------------------------------------------------------------------

    public NoSkEResponse versions() {
        return client.target(baseUri)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get(NoSkEResponse.class);
    }

    public NoSkECorporaResponse corpora() {
        return client.target(baseUri)
                .path("corpora")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get(NoSkECorporaResponse.class);
    }

    public NoSkECorpusInfoResponse corpInfo(String corpname) {
        return client.target(baseUri)
                .path("corp_info")
                .queryParam("corpname", corpname)
                // we need structure info to see if there are sentences `<s/>` or not
                .queryParam("struct_attr_stats", 1)
                .queryParam("subcorpora", 1)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get(NoSkECorpusInfoResponse.class);
    }

    // https://www.sketchengine.eu/apidoc/#/Corpus%20Search/get_search_concordance
    // https://www.sketchengine.eu/documentation/methods-documentation/#view
    public NoSkEConcordanceResponse concordanceWebGDEXSample(String corpname, String iquery) {
        String json = Json.createObjectBuilder()
                .add("concordance_query",
                        Json.createArrayBuilder()
                                .add(Json.createObjectBuilder()
                                        .add("queryselector", "iqueryrow")
                                        .add("iquery", iquery)
                                        .build())
                                .add(Json.createObjectBuilder()
                                        .add("q", "r200")
                                        .build())
                                .add(Json.createObjectBuilder()
                                        .add("q", "E200") // e200 for no score display
                                        .build())
                                .build())
                .build()
                .toString();

        return client.target(baseUri)
                .path("concordance")
                .queryParam("corpname", corpname)
                .queryParam("attrs", "word")
                .queryParam("refs", "=s.id")
                .queryParam("struct_attr_stats", "all")
                .queryParam("viewmode", "kwic")
                .queryParam("cup_hl", "q")
                .queryParam("structs", "s,g")
                .queryParam("fromp", 1)
                .queryParam("pagesize", 20)
                .queryParam("kwicleftctx", "100#")
                .queryParam("kwicrightctx", "100#")
                .queryParam("json", "{jsonTemplate}")
                .resolveTemplate("jsonTemplate", json)
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .get(NoSkEConcordanceResponse.class);
    }

    public NoSkEViewResponse concordanceWithPages(String corpname, String query, String[] attrs, String refs,
            boolean complete, int pageNr, int pageSize, boolean hasSentences) {
        // validation
        if (pageNr <= 0) {
            pageNr = 1;
        }
        if (pageSize < 1) {
            pageSize = 20;
        }

        WebTarget target = client.target(baseUri)
                .path("view")
                .queryParam("corpname", corpname)
                // NOTE: or as json?
                // we need to use templates to properly escape `{a,b}` repeat-instructions from
                // FCS-QL / NSE CQL
                .queryParam("q", "{query}")
                .resolveTemplate("query", query)
                // structures?
                .queryParam("structs", "s,g") // doc,p
                // window/batch
                .queryParam("fromp", pageNr)
                .queryParam("pagesize", pageSize);

        // borders left and right context
        if (!hasSentences) {
            target = target
                    // 10 tokens left and right in sen(tence) view mode
                    .queryParam("viewmode", "sen")
                    .queryParam("senleftctx", "-10")
                    .queryParam("senrightctx", "10");
        } else {
            target = target
                    .queryParam("viewmode", "sen");
            // .queryParam("kwicleftctx", "-1:s") // default 40# (characters)
            // .queryParam("kwicrightctx", "1:s")
            // .queryParam("senleftctx", "-1:s") // default, in "sen" mode
            // .queryParam("senrightctx", "1:s") // default, in "sen" mode
        }

        if (refs != null) {
            target = target
                    // line attributes (e.g., sentence id, ...)
                    .queryParam("refs", refs);
        }

        if (attrs != null && attrs.length >= 1) {
            // will tokenize (if more than one)
            String attrsStr = String.join(",", attrs);
            target = target
                    .queryParam("attr_allpos", "all") // only in bonito?, ctxattrs=attrs
                    .queryParam("attrs", attrsStr)
                    .queryParam("ctxattrs", attrsStr); // if set, then tokenized instead of sentence
        }

        if (complete) {
            // complete full result set (not just for current window)
            // --> exact total result count?
            target = target.queryParam("asyn", 0);
        }

        LOGGER.debug("NoSkE-Concordance-URI: {}", target.getUri());

        // NOTE: test to parse error response
        // @formatter:off
        // final Response response = target.request()
        //         .accept(MediaType.APPLICATION_JSON)
        //         .get();
        // LOGGER.debug("response: {}", response);
        // response.bufferEntity();
        // try {
        //     return response.readEntity(NoSkEViewResponse.class);
        // } catch (ProcessingException pe) {
        //     if (pe.getCause().getClass().equals(UnrecognizedPropertyException.class)) {
        //         UnrecognizedPropertyException urpe = ((UnrecognizedPropertyException) pe.getCause());
        //         if (urpe.getPropertyName().equals("error")) {
        //             final NoSkEErrorResponse errorResponse = response.readEntity(NoSkEErrorResponse.class);
        //             LOGGER.debug("Error response: {}", errorResponse);
        //         }
        //     }
        //     return null;
        // }
        // @formatter:on

        // TODO: process results (e.g., attrs to complex objects?)

        return target.request()
                .accept(MediaType.APPLICATION_JSON)
                .get(NoSkEViewResponse.class);
    }

    public NoSkEViewResponse concordance(String corpname, String query, String[] attrs, String refs, boolean complete,
            int maximumRecords, int startRecord, boolean hasSentences) {
        // validation
        if (startRecord <= 0) {
            startRecord = 1;
        }
        if (maximumRecords < 1) {
            maximumRecords = 20;
        }

        // NOTE: NoSkE API works with pages
        // we request a bit more to fit the requested window and then trim results to
        // fit the startRecord and maximumRecords parameters
        int pageSize = computePageSize(maximumRecords, startRecord);
        int pageNr = computePageNr(maximumRecords, startRecord, pageSize);
        int discardBefore = startRecord - ((pageNr - 1) * pageSize + 1);
        int discardAfter = (pageNr * pageSize) - (startRecord + maximumRecords - 1);
        LOGGER.debug(
                "Search window: startRecord:{}, maximumRecords:{} [{}-{}] --> NSE pageSize:{} pageNr:{} [{}-{}] (discard {}, {})",
                startRecord, maximumRecords, startRecord, startRecord + maximumRecords - 1, pageSize, pageNr,
                (pageNr - 1) * pageSize + 1, pageNr * pageSize, discardBefore, discardAfter);

        NoSkEViewResponse concResult = concordanceWithPages(corpname, query, attrs, refs, complete, pageNr, pageSize,
                hasSentences);

        if (concResult != null && concResult.Lines != null && !concResult.Lines.isEmpty()) {
            concResult.Lines = concResult.Lines.subList(Math.min(concResult.Lines.size() - 1, discardBefore),
                    Math.min(concResult.Lines.size(), pageSize - discardAfter));
        }

        return concResult;
    }

    public NoSkEViewResponse concordanceSimple(String corpname, String query, String refs, int maximumRecords,
            int startRecord, boolean hasSentences) {
        return concordance(corpname, query, null, refs, false, maximumRecords, startRecord, hasSentences);
    }

    public NoSkEViewResponse concordanceSimple(String corpname, String query, String refs, int maximumRecords,
            int startRecord) {
        return concordance(corpname, query, null, refs, false, maximumRecords, startRecord, true);
    }

    public NoSkEViewResponse concordanceSimple(String corpname, String query, int maximumRecords, int startRecord) {
        return concordance(corpname, query, null, "?,=s.id,=s.date,=s.source", false, maximumRecords, startRecord,
                true);
    }

    public NoSkEViewResponse concordanceLayers(String corpname, String query, String[] attrs, String refs,
            int maximumRecords, int startRecord, boolean hasSentences) {
        return concordance(corpname, query, attrs, refs, false, maximumRecords, startRecord, hasSentences);
    }

    public NoSkEViewResponse concordanceLayers(String corpname, String query, String[] attrs, String refs,
            int maximumRecords, int startRecord) {
        return concordance(corpname, query, attrs, refs, false, maximumRecords, startRecord, true);
    }

    protected int computePageSize(int maximumRecords, int startRecord) {
        // easy conditions
        if ((startRecord - 1) % maximumRecords == 0) {
            return maximumRecords;
        }
        if ((startRecord - 1) < maximumRecords) {
            return startRecord - 1 + maximumRecords;
        }
        // trial by loop
        for (int pageSize = maximumRecords + 1; pageSize < Math.max(1000, 3 * maximumRecords + 1); pageSize++) {
            int pageNr = computePageNr(maximumRecords, startRecord, pageSize);
            if (((pageNr - 1) * pageSize) >= startRecord) {
                continue;
            }
            if ((pageNr * pageSize) < startRecord) {
                continue;
            }
            if ((pageNr * pageSize) < (startRecord + maximumRecords - 1)) {
                continue;
            }
            return pageSize;

        }
        LOGGER.warn("Unable to compute NSE pagesize for startRecord={} maximumRecords={}", startRecord, maximumRecords);
        return maximumRecords;
    }

    protected int computePageNr(int maximumRecords, int startRecord, int pageSize) {
        return (startRecord + pageSize - 1) / pageSize;
    }

    // ---------------------------------------------------------------------

    public static Client buildClient(boolean insecure, boolean jsonIgnoreUnknown)
            throws KeyManagementException, NoSuchAlgorithmException {
        ClientConfig config = new ClientConfig();
        config.property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT, LoggingFeature.Verbosity.PAYLOAD_ANY);

        // if (username != null && password != null) {
        // config.register(HttpAuthenticationFeature.basic(username, password));
        // }

        ClientBuilder clientBuilder = ClientBuilder.newBuilder().withConfig(config);

        if (insecure) {
            SSLContext ctx = SSLContext.getInstance("SSL");
            TrustManager[] trustAllCerts = { new InsecureTrustManager() };
            ctx.init(null, trustAllCerts, new java.security.SecureRandom());

            // HostnameVerifier hostnameVerifier =
            // HttpsURLConnection.getDefaultHostnameVerifier();
            HostnameVerifier allHostsValid = new InsecureHostnameVerifier();

            clientBuilder.sslContext(ctx).hostnameVerifier(allHostsValid);
        }

        if (jsonIgnoreUnknown) {
            // https://stackoverflow.com/a/16390260/9360161
            // https://cassiomolin.com/2017/11/21/customizing-objectmapper-in-a-jaxrs-application/
            // https://stackoverflow.com/a/31296138/9360161

            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            clientBuilder.register(new JacksonJsonProvider(mapper));
        }

        Client client = clientBuilder.build();

        return client;
    }

    // ---------------------------------------------------------------------

    public static class InsecureHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    public static class InsecureTrustManager implements X509TrustManager {
        /**
         * {@inheritDoc}
         */
        @Override
        public void checkClientTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException {
            // Everyone is trusted!
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void checkServerTrusted(final X509Certificate[] chain, final String authType)
                throws CertificateException {
            // Everyone is trusted!
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    // ---------------------------------------------------------------------

    public static void checkAndSetupProxy() {
        // https://stackoverflow.com/questions/10415607/jersey-client-set-proxy
        if (System.getenv("HTTP_PROXY") != null) {
            URI httpProxyUri = UriBuilder.fromUri(System.getenv("HTTP_PROXY")).build();
            LOGGER.debug("Setup HTTP Proxy: {}", httpProxyUri);
            System.setProperty("http.proxyHost", httpProxyUri.getHost());
            System.setProperty("http.proxyPort", String.valueOf(httpProxyUri.getPort()));
        }
        if (System.getenv("HTTPS_PROXY") != null) {
            URI httpsProxyUri = UriBuilder.fromUri(System.getenv("HTTPS_PROXY")).build();
            LOGGER.debug("Setup HTTPS Proxy: {}", httpsProxyUri);
            System.setProperty("https.proxyHost", httpsProxyUri.getHost());
            System.setProperty("https.proxyPort", String.valueOf(httpsProxyUri.getPort()));
        }
    }
}
