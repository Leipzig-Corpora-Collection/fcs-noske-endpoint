package de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NoSkECorpusInfoResponse extends NoSkEResponse {
    public String name;
    public String lang;
    public String infohref;
    public String info;
    public String encoding;
    public String tagsetdoc;
    public String errsetdoc; // ?
    public String compiled;
    public boolean is_error_corpus;
    public boolean righttoleft;
    public CorpusSizes sizes;
    public List<Attribute> attributes;
    // /**
    //  * Available <em>structures</em> or <em>tags</em> in the corpus. Structures like <code>s</code> (sentence), <code>g</code> (glue), <code>doc</code> (document).
    //  */
    public List<Structure> structures;
    public List<Object> subcorpora;

    public static class Attribute {
        public String name;
        public long id_range;
        public String label;
        public String dynamic;
        public String fromattr;

        @Override
        public String toString() {
            return "Attribute [name=" + name + ", id_range=" + id_range + ", label=" + label + ", dynamic="
                    + dynamic + ", fromattr=" + fromattr + "]";
        }
    }

    public static class Structure {
        public String name;
        public String label;
        public List<Attribute> attributes;
        public long size;

        public static class Attribute {
            public String name;
            public String label;
            public String dynamic;
            public String fromattr;
            public long size;

            @Override
            public String toString() {
                return "Attribute [name=" + name + ", label=" + label + ", dynamic=" + dynamic + ", fromattr="
                        + fromattr + ", size=" + size + "]";
            }
        }

        @Override
        public String toString() {
            return "Structure [name=" + name + ", label=" + label + ", attributes=" + attributes + ", size=" + size
                    + "]";
        }
    }

    @Override
    public String toString() {
        return "NoSkECorpusInfoResponse [name=" + name + ", lang=" + lang + ", infohref=" + infohref + ", info="
                + info + ", encoding=" + encoding + ", tagsetdoc=" + tagsetdoc + ", errsetdoc=" + errsetdoc
                + ", compiled=" + compiled + ", is_error_corpus=" + is_error_corpus + ", righttoleft=" + righttoleft
                + ", sizes=" + sizes + ", attributes=" + attributes + ", structures=" + structures + ", subcorpora="
                + subcorpora

                // + ", wposlist=" + wposlist + ", lposlist=" + lposlist + ", wsposlist=" +
                // wsposlist + ", structs=" + structs + ", defaultattr=" + defaultattr + ",
                // starattr=" + starattr + ", unicameral=" + unicameral + ", wsattr=" + wsattr +
                // ", wsdef=" + wsdef + ", termdef=" + termdef + ", diachronic=" + diachronic +
                // ", aligned=" + aligned + ", aligned_details=" + aligned_details + ",
                // freqttattrs=" + freqttattrs + ", subcorpattrs=" + subcorpattrs + ",
                // shortref=" + shortref + ", docstructure=" + docstructure + ", newversion=" +
                // newversion + ", structctx=" + structctx + ", deffilterlink=" + deffilterlink
                // + ", defaultstructs=" + defaultstructs + ", wsttattrs=" + wsttattrs + ",
                // terms_compiled=" + terms_compiled + ", gramrels=" + gramrels + ", alsizes=" +
                // alsizes

                + ", api_version=" + api_version + ", manatee_version=" + manatee_version + ", request=" + request
                + "]";
    }

    // @formatter:off
    /*
    public List<Object> wposlist;
    public List<Object> lposlist;
    public List<Object> wsposlist;
    public List<Object> structs;
    public String defaultattr;
    public String starattr;
    public boolean unicameral;
    public String wsattr;
    public String wsdef;
    public String termdef;
    public String diachronic; // null
    public List<Object> aligned;
    public List<Object> aligned_details;
    public List<Object> freqttattrs;
    public List<Object> subcorpattrs;
    public String shortref;
    public String docstructure;
    public String newversion;
    public String structctx;
    public boolean deffilterlink;
    public List<Object> defaultstructs;
    public String wsttattrs;
    public boolean terms_compiled;
    public List<Object> gramrels;
    public List<Object> alsizes;
        */
    // @formatter:on
}
