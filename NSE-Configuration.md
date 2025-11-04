# NSE Corpus Configuration

The following will describe relevant (No)SketchEngine configuration values so that this FCS Endpoint will correctly configure itself - if enabled with the `FCS_RESOURCES_FROM_NOSKE` env var. Additionally, the `FCS_RESOURCES_FROM_NOSKE_WITH_HANDLE_ONLY` is important to, to filter corpora/resources.

## Resource Handle

If the `FCS_RESOURCES_FROM_NOSKE_WITH_HANDLE_ONLY` value is set to `true`, this endpoint will filter all available corpora to only include those that have a non-empty value for `handle` (via the `/bonito/run.cgi/corpora` API). Those `handle` values will then be used to transparently map the plain corpus name into the persistend identifier in `handle` and exposes the resource as such to the FCS infrastructure. On SRU/FCS search requests the handle will be mapped back to the corpus name for NoSkE concordance queries.

Set a handle like this:

```c
HANDLE "hdl:11022/0000-1000-0200-3"
```

## Reference links and other metadata

To generate or obtain a good backlink for FCS search results, the following strategy is used:
First, check if `fcsrefs` are set in the `/bonito/run.cgi/corp_info?corpname=<name>` API. This value is set in `FCSREFS`.

The Refs should be four (4) fields,

- Link to result (e.g., to NSE search page or somewhere else, the _"backlink"_),
- ID of result (might also be the PID of the resource, or e.g., a sentence or document ID),
- URL of result (e.g., source URL of web pages),
- Date of result (e.g., when was the URL fetched).

More values can be added but will be ignored. If any of the Refs has no meaningful value, you should just set `?` or any other invalid structure/attribute, so the resulting `Refs` value for concordance results will be empty.

```c
# default for LCC/Wortschatz corpora
# the link to the result will then be generated dynamically using the ID (`s.id`)
FCSREFS "?,=s.id,=s.url,=s.date"
```

## Multilingual metadata for Resources

To support multilingual resource metadata (title, description, institution) and custom landingpages that are not automatically derived from the NoSketchEngine corpus information (`corpus.name` for title, `corpus.info` for description and `corpusinfo.infohref` for landingpage) the custom `corpus.fcsinfos` (`FCSINFOS` JSON string in the NoSketchEngine corpus configuration) will be used.

The `FCSINFOS` is a _plain string_ that is being interpreted as JSON-formatted object. It allows to provide multilingual values for the FCS EndpointDescription.

```py
# no information
FCSINFOS '{}'

# the FCSINFOS may contain fields: "title", "description", "institution", and "landingpage"
# each field may still be empty, e.g., `null`
FCSINFOS '{"title": {}, "description": null, "institution": {"en": null}, "landingpage": null}'
# for the fields "title", "description", "institution", `null`, `{}` or `{"xx": null}` or absence will all be interpreted as empty / no information

FCSINFOS '{"title": {"de": "title", "en": null}}'
# will result in titles: de->"title" but not "en" title; and no other metadata

# or more full example
FCSINFOS '{"title": {"en": "En-Title", "de": "DE Title"}, "description": {"en": "some description"}, "institution": {"de": "Mein Institut"}, "landingpage": "https://resource.org/PID"}'
```

## Example

```c
# general information about the corpus (primarily for NSE)
# ...
INFO "<description of corpus>"
NAME "<name of corpus>"
# ...

# relevant for `FCS_RESOURCES_FROM_NOSKE_WITH_HANDLE_ONLY`
# will
HANDLE "<some handle value, like 'hdl:11022/0000-0000-223E-5'>"
# ...
```
