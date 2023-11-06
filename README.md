# NoSketchEngine SRU/FCS Endpoint

This is an [FCS](https://www.clarin.eu/content/content-search) Endpoint implementation for the [(No)SketchEngine](https://nlp.fi.muni.cz/trac/noske). It uses the _bonito-open_ API as search backend.

It is being developed by the _Leipzig Corpora Collection (LCC)_ and the _Saxon Academy of Sciences and Humanities in Leipzig (SAW)_ and the code is licensed under [MIT](LICENSE).

This repository should only be regarded as basis for own deployments. While templates and example configurations contain LCC specific URLs, those should only be used for testing and if you want to try out this code base! If you want to deploy your own FCS endpoint, please check that you have the permissions to use the specific NoSketchEngine API. You can setup your own NoSketchEngine easily with e.g. [ELTE-DH/NoSketch-Engine-Docker](https://github.com/ELTE-DH/NoSketch-Engine-Docker).

There is some partial (No)SketchEngine API adapter in [`d.s.t.w.f.f.noske`](src/main/java/de/saw_leipzig/textplus/webservices/fcs/fcs_noske_endpoint/noske) that can be extracted and used as is. There is a [test case](src/test/java/de/saw_leipzig/textplus/webservices/fcs/fcs_noske_endpoint/NoSkEAPITest.java) to see its usage besides the one in this endpoint.


## NoSketchEngine Corpus Configuration

Note that there are some basic assumptions about the backend NoSketchEngine searcher.

Those are implementation details and can be seen in the classes `d.s.t.w.f.f.NoSkESRUFCSEndpointSearchEngine` and `d.s.t.w.f.f.query.FCSQLtoNoSkECQLConverter`.

* We assume that all corpora are freely accessible and that there are not sub-corpora. The endpoint will dynamically configure itself by listing all corpora available and setting the appropriate metadata.
* The corpus `language_id` is an [ISO 639-3](https://iso639-3.sil.org/code_tables/639/data) identifier, e.g. `deu`.
* We only have a single (required) structure: `s`, meaning sentence (with optional attributes `id`/`source`/`date` that are not really used at this point).
* We use the following attributes: `word` (required), `lemma`, `pos` (with `pos_ud17`) and `lc` (required) / `lemma_lc` as automatic lower cased variants for `word` / `lemma`. `lemma` and `pos` are optional attributes.
  * The attributes `pos` and `pos_ud17` are not completely integrated. At the moment, only the `pos` attribute is checked which might not be [UD17](https://universaldependencies.org/u/pos/) (as required by FCS).

Adaptions to own corpus configurations should not be too complicated.


## Files

### Project and Deployment

- [`Dockerfile`](Dockerfile)  
  Multi-stage Maven build and slim Jetty runtime image.
- [`docker-compose.yml`](docker-compose.yml)
- [`pom.xml`](pom.xml)  
  Java dependencies for use with Maven.
- [`.env.template`](.env.template)  
  Template `.env` file for Docker deployments.

### Java SourceCode

The following classes live in the `de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint` namespace.

#### SRU/FCS Implementation

* [`d.s.t.w.f.f.NoSkESRUFCSConstants`](src/main/java/de/saw_leipzig/textplus/webservices/fcs/fcs_noske_endpoint/NoSkESRUFCSConstants.java)  
  Constants for accessing FCS request parameters and output generation. Can be used to store own constants.
* [`d.s.t.w.f.f.NoSkESRUFCSEndpointSearchEngine`](src/main/java/de/saw_leipzig/textplus/webservices/fcs/fcs_noske_endpoint/NoSkESRUFCSEndpointSearchEngine.java)  
  The glue between the FCS and our own search engine. It is the actual implementation that handles SRU/FCS explain and search requests. Here, we load and initialize our FCS endpoint.
  It will perform searches with our own search engine (here only with static results), and wrap results into the appropriate output (`d.s.t.w.f.f.NoSkESRUFCSSearchResultSet`). 
* [`d.s.t.w.f.f.NoSkESRUFCSSearchResultSet`](src/main/java/de/saw_leipzig/textplus/webservices/fcs/fcs_noske_endpoint/NoSkESRUFCSSearchResultSet.java)  
  FCS Data View output generation. Generates the basic HITS and ADVANCED Data Views. Here custom output can be generated from the result wrapper `d.s.t.w.f.f.searcher.MyResults`.
* [`d.s.t.w.f.f.searcher.MyResults`](src/main/java/de/saw_leipzig/textplus/webservices/fcs/fcs_noske_endpoint/searcher/MyResults.java)  
  Lightweight wrapper around own results that allows access to results counts and result items per index and wraps the native result entries with kwic, left and right context as well as some metadata.

#### Query Converters

* [`d.s.t.w.f.f.query.CQLtoNoSkECQLConverter`](src/main/java/de/saw_leipzig/textplus/webservices/fcs/fcs_noske_endpoint/query/CQLtoNoSkECQLConverter.java)  
  Query converion from simple CQL to (No)SketchEngine CQL (CQP) query.
* [`d.s.t.w.f.f.query.FCSQLtoNoSkECQLConverter`](src/main/java/de/saw_leipzig/textplus/webservices/fcs/fcs_noske_endpoint/query/FCSQLtoNoSkECQLConverter.java)  
  Query converion from FCS-QL to (No)SketchEngine CQL (CQP) query.

#### (No)SketchEngine (Bonito) API Client

* [`d.s.t.w.f.f.noske.NoSkeAPI`](src/main/java/de/saw_leipzig/textplus/webservices/fcs/fcs_noske_endpoint/noske/NoSkeAPI.java)  
  NoSkE Bonito API Client.
* Namespace [`d.s.t.w.f.f.noske.pojo`](src/main/java/de/saw_leipzig/textplus/webservices/fcs/fcs_noske_endpoint/noske/pojo)  
  NoSkE Bonito API response wrapper classes.

#### Utils

* [`d.s.t.w.f.f.util.LanguagesISO693`](src/main/java/de/saw_leipzig/textplus/webservices/fcs/fcs_noske_endpoint/util/LanguagesISO693.java)  
  Helper class (from [FCS SRU Aggregator](https://github.com/clarin-eric/fcs-sru-aggregator)) that handles conversion between ISO639 Codes and Language names.
* [src/main/resources/lang/iso-639-3_20230123.tab](src/main/resources/lang/iso-639-3_20230123.tab)  
  Resource file for ISO639 conversion

### Resources

Only the [`log4j2.xml`](src/main/resources/log4j2.xml) is important in case of changing logging settings.


## Endpoint configuration:

* [`endpoint-description.xml`](src/main/webapp/WEB-INF/endpoint-description.xml)  
  FCS Endpoint Description, like resources, capabilities etc.  
  This file can be used to pre-configure the endpoint, e.g., to restrict the exposed resources. Otherwise, using the `FCS_RESOURCES_FROM_NOSKE` parameter, resource information will be queried from the (No)SketchEngine API and all found resources are exposed. The Endpoint Description will be generated programmatically.
* [`jetty-env.xml`](src/main/webapp/WEB-INF/jetty-env.xml)  
  Jetty environment variable settings.
* [`sru-server-config.xml`](src/main/webapp/WEB-INF/sru-server-config.xml)  
  SRU Endpoint Settings.
* [`web.xml`](src/main/webapp/WEB-INF/web.xml)  
  Java Servlet configuration, SRU/FCS endpoint settings.

The configuration (via Java environment variable context) for the endpoint are:

* `NOSKE_API_URI`: URI; base URI to (No)SketchEngine Bonito endpoint, required!
* `FCS_RESOURCES_FROM_NOSKE`: Boolean, if (No)SketchEngine `/corpora` API endpoint should be used to automatically generate the Endpoint Description with the list of resources (corpora). If `false`, the embedded or with `RESOURCE_INVENTORY_URL` (`"de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.resourceInventoryURL"`) specified Endpoint Description file is being used.
* `DEFAULT_RESOURCE_PID`: String, default resource PID for searches where no `x-fcs-context` is specified. Take care that you include the possible resource PID prefix, specified in [`d.s.t.w.f.f.NoSkESRUFCSConstants`](src/main/java/de/saw_leipzig/textplus/webservices/fcs/fcs_noske_endpoint/NoSkESRUFCSConstants.java).


## Build and Deployment

Build [`fcs.war`](target/fcs.war) file for webapp deployment:

```bash
mvn [clean] package
```

Some endpoint/resource configurations are being set using environment variables. See [`jetty-env.xml`](src/main/webapp/WEB-INF/jetty-env.xml) for details. You can set default values there.
For production use, you can set values in the `.env` file that is then loaded with the `docker-compose.yml` configuration. Take a look at the [`.env.template`](.env.template) file, save a copy to `.env` with your own configuration.

This SRU/FCS Endpoint project includes both a [`Dockerfile`](Dockerfile) and a [`docker-compose.yml`](docker-compose.yml) configuration.
The `Dockerfile` can be used to build a simple Jetty image to run the FCS endpoint. It still needs to be configured with port-mappings, environment variables etc. The `docker-compose.yml` file bundles all those runtime configurations to allow easier deployment. You still need to create an `.env` file or set the environment variables if you use the generated code as is.

### Using docker

```bash
# build the image and label it "fcs-endpoint"
docker build -t fcs-endpoint .

# run the image in the foreground (to see logs and interact with it) with environment variables from .env file
docker run --rm -it --name fcs-endpoint -p 8200:8080 --env-file .env fcs-endpoint

# or run in background with automatic restart
docker run -d --restart=unless-stopped --name fcs-endpoint -p 8200:8080 --env-file .env fcs-endpoint
```

### Using docker-compose

```bash
# build
docker-compose build
# run
docker-compose up [-d]
```

### Run with Jetty (Maven)

Uses Jetty 10. See [`pom.xml`](pom.xml) --> plugin `jetty-maven-plugin`.

```bash
mvn [package] jetty:run-war
```

NOTE: `jetty:run-war` uses built war file in [`target/`](target/) folder.


The search request for _something_ in CQL/BASIC-Search:

```bash
curl '127.0.0.1:8080?operation=searchRetrieve&queryType=cql&query=something&x-indent-response=1'
# or port 8200 if run with docker
```

### Debug (Jetty, Maven) with VSCode

Add default debug setting `Attach by Process ID`, then start the jetty server with the following command, and start debugging in VSCode while it waits to attach.

```bash
# export configuration values, see section #Configuration
MAVEN_OPTS="-Xdebug -Xnoagent -Djava.compiler=NONE -agentlib:jdwp=transport=dt_socket,server=y,address=5005" mvn jetty:run-war
```

### Tests

There are a few basic tests in [`src/test/java/d.s.t.w.f.f/`](src/test/java/de/saw_leipzig/textplus/webservices/fcs/fcs_noske_endpoint) with hopefully more to come...
There exists a custom tests [`log4j2.xml`](src/test/resources/log4j2.xml) configuration file.
