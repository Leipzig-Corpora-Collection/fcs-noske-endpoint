package de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.NoSkeAPI.buildClient;

import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.NoSkeAPI;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo.NoSkEConcordanceResponse;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo.NoSkECorporaResponse;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo.NoSkECorporaResponse.Corpus;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo.NoSkECorpusInfoResponse;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo.NoSkEResponse;
import de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo.NoSkEViewResponse;

import com.fasterxml.jackson.databind.DeserializationFeature;

public class NoSkEAPITest {
    private static final Logger LOGGER = LogManager.getLogger(NoSkEAPITest.class);

    private static Client client;
    private static NoSkeAPI api;
    private static URI baseUri = UriBuilder.fromUri("https://cql.wortschatz-leipzig.de/bonito/run.cgi").build();

    static {
        // setup proxy if in use
        NoSkeAPI.checkAndSetupProxy();
    }

    // ---------------------------------------------------------------------
    // local methods, to experiment

    private Response apiGet(Client client, String path) {
        // URI baseUri = UriBuilder.fromUri("https://httpbin.org/anything").build();
        LOGGER.debug("uri: {}", baseUri);

        WebTarget target = client.target(baseUri);
        if (path != null) {
            target = target.path(path);
        }

        final Response response = target.request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        // .get(String.class)
        LOGGER.debug("response: {}", response);

        return response;
    }

    private Response apiConcordance(Client client, String corpname, String iquery) {
        WebTarget target = client.target(baseUri)
                .path("concordance");

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
        LOGGER.debug("request json: {}", json);

        target = target
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
                .queryParam("kwicrightctx", "100#");
        target = target
                .queryParam("json", "{jsonTemplate}")
                .resolveTemplate("jsonTemplate", json);

        final Response response = target.request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        LOGGER.debug("response: {}", response);

        return response;
    }

    // ---------------------------------------------------------------------
    // local experiments

    @BeforeAll
    static void setupClient() throws KeyManagementException, NoSuchAlgorithmException {
        client = buildClient(false, false);
    }

    @Disabled("Experiments")
    @Test
    public void testAPIBasic() {
        final Response response = apiGet(client, null);

        response.bufferEntity();
        String jsonStringResponse = response.readEntity(String.class);
        LOGGER.info("response: {}", jsonStringResponse);

        // final ObjectMapper mapper = new ObjectMapper()
        // .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // NoSkEResponse jsonResponse = mapper.readValue(jsonStringResponse,
        // NoSkEResponse.class);
        NoSkEResponse jsonResponse = response.readEntity(NoSkEResponse.class);
        LOGGER.info("response: {}", jsonResponse);
    }

    @Disabled("Experiments")
    @Test
    public void testAPICorporaBasic() throws JsonMappingException, JsonProcessingException {
        final Response response = apiGet(client, "corpora");
        response.bufferEntity();

        assertThrows(ProcessingException.class, () -> {
            // NOTE: only throws if not globally disabled:
            // DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
            NoSkEResponse baseResponse = response.readEntity(NoSkEResponse.class);
            LOGGER.debug("response: {}", baseResponse);
        });

        String jsonStringResponse = response.readEntity(String.class);
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        NoSkEResponse value = mapper.readValue(jsonStringResponse, NoSkEResponse.class);
        LOGGER.info("value: {}", value);

        NoSkECorporaResponse jsonResponse = response.readEntity(NoSkECorporaResponse.class);
        LOGGER.info("response: {} corpora", jsonResponse.data.size());
    }

    @Disabled("Experiments, too much output")
    @Test
    public void testAPIConcordanceBasic() throws JsonMappingException, JsonProcessingException {
        final Response response = apiConcordance(client, "deu_news_2011", "apfel");

        NoSkEConcordanceResponse jsonResponse = response.readEntity(NoSkEConcordanceResponse.class);
        LOGGER.info("response: {}", jsonResponse);
    }

    @Disabled("Experiments")
    @Test
    public void testAPICorpInfoBasic() {
        WebTarget target = client.target(baseUri)
                .path("corp_info")
                .queryParam("corpname", "deu_news_2011");

        final Response response = target.request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        LOGGER.debug("response: {}", response);

        response.bufferEntity();
        String jsonResponse = response.readEntity(String.class);
        LOGGER.info("response: {}", jsonResponse);

        NoSkECorpusInfoResponse corpInfoResponse = response.readEntity(NoSkECorpusInfoResponse.class);
        LOGGER.info("response: {}", corpInfoResponse);

        // Map value = mapper.readValue(jsonResponse, Map.class);
        // NoSkECorpusInfoResponse value = mapper.readValue(jsonResponse,
        // NoSkECorpusInfoResponse.class);
        // LOGGER.info("value: {}", value);
    }

    @Disabled("Experiments")
    @Test
    public void testAPIConcordanceViewBasic() {
        WebTarget target = client.target(baseUri)
                .path("view")
                .queryParam("corpname", "deu_news_2011")
                .queryParam("asyn", 1)
                .queryParam("pagesize", 2) // default 20
                .queryParam("fromp", 1)
                .queryParam("viewmode", "sen") // kwic/sen (only for {kwic,sen}{left,right}ctx)
                // .queryParam("kwicleftctx", "-1:s") // default 40# (characters)
                // .queryParam("kwicrightctx", "-1:s")
                // .queryParam("senleftctx", "-1:s") // default, in "sen" mode?
                // .queryParam("senrightctx", "-1:s") // default, in "sen" mode?
                // .queryParam("attrs", "word,lemma,pos,pos_ud17")
                // .queryParam("ctxattrs", "word,lemma,pos,pos_ud17") // if set, then tokenized
                // instead of sentence
                // .queryParam("structs", "p,g,s,doc") // p,g default
                .queryParam("refs", "=s.id,=s.date,=s.source")
                // .queryParam("where", "") // ?
                .queryParam("q", "q[lemma=\"Apfel\"]");

        final Response response = target.request()
                .accept(MediaType.APPLICATION_JSON)
                .get();
        LOGGER.debug("response: {}", response);
        response.bufferEntity();

        String jsonResponse = response.readEntity(String.class);
        LOGGER.info("response: {}", jsonResponse);

        NoSkEViewResponse parsedResponse = response.readEntity(NoSkEViewResponse.class);
        LOGGER.info("response: {}", parsedResponse);
    }

    // ---------------------------------------------------------------------
    // api tests

    @BeforeAll
    static void setupAPI() throws KeyManagementException, NoSuchAlgorithmException {
        api = new NoSkeAPI(baseUri.toString(), false, false);
    }

    @Test
    public void testAPIVersions() {
        NoSkEResponse response = api.versions();
        LOGGER.info("API version: bonito: {}, manatee: {}", response.api_version, response.manatee_version);
    }

    @Test
    public void testAPICorpInfo() {
        String corpname = "deu_news_2011";
        NoSkECorpusInfoResponse ci = api.corpInfo(corpname);
        LOGGER.info("corp_info ({}): {}", corpname, ci);
    }

    @Disabled("only fake creds anyway")
    @Test
    public void testAPIAuth() throws KeyManagementException, NoSuchAlgorithmException {
        NoSkeAPI api = new NoSkeAPI(baseUri.toString(), false, false);

        NoSkECorporaResponse jsonResponse = api.corpora();
        LOGGER.info("response: {} corpora", jsonResponse.data.size());

        LOGGER.info("Login");
        api.authenticate("test", "test");

        NoSkECorporaResponse jsonResponse2 = api.corpora();
        LOGGER.info("response: {} corpora", jsonResponse2.data.size());

        Set<String> corpnames1 = jsonResponse.data.stream().map(corp -> corp.corpname).collect(Collectors.toSet());

        List<NoSkECorporaResponse.Corpus> newCorpora = jsonResponse2.data.stream()
                .filter(new Predicate<NoSkECorporaResponse.Corpus>() {
                    @Override
                    public boolean test(Corpus t) {
                        return !corpnames1.contains(t.corpname);
                    }
                }).collect(Collectors.toList());
        // newCorpora.removeAll(jsonResponse.data);
        // NOTE: hashing/equals does not work correctly?

        LOGGER.info("after login got {} private corpora: {}", newCorpora.size(),
                newCorpora.stream().map(corp -> corp.corpname).collect(Collectors.toList()));
    }

    @Disabled("too much output")
    @Test
    public void testAPIConcordance() {
        String corpname = "deu_news_2011";
        String iquery = "apfel";
        NoSkEConcordanceResponse conc = api.concordanceWebGDEXSample(corpname, iquery);
        LOGGER.info("concordance: {}", conc);
    }

    @Test
    public void testAPIConcordanceSimple() {
        String corpname = "deu_news_2011";
        String query = "apfel";
        String cqlQuery = "aword,\"" + query + "\"";
        // cqlQuery = "aword,"; // cause error
        NoSkEViewResponse conc = api.concordanceSimple(corpname, cqlQuery, 2, 1);
        LOGGER.info("concordance: {}", conc);
    }
}
