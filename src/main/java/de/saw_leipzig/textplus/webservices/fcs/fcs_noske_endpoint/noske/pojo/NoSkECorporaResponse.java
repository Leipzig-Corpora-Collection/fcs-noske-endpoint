package de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.noske.pojo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class NoSkECorporaResponse extends NoSkEResponse {
    public List<Corpus> data;

    @Override
    public String toString() {
        return "NoSkECorporaResponse [data=" + data
                + ", api_version=" + api_version + ", manatee_version=" + manatee_version + ", request=" + request
                + "]";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Corpus {
        public String corpname;
        public String language_id;
        public String language_name;
        public String name;
        public String info;

        public CorpusSizes sizes; // Map<String, Integer>

        public String id; // null
        public String owner_id; // null
        public String owner_name; // null
        public String tagset_id; // null
        public String created; // null
        public String compilation_status;

        public String handle; // null (if not available)
        public String fcsrefs; // null (if not available, might only be at corp_info level)
        public String fcsinfos; // null (if not available)

        @Override
        public String toString() {
            return "Corpus [corpname=" + corpname + ", language_id=" + language_id + ", language_name="
                    + language_name + ", name=" + name + ", info=" + info + ", sizes=" + sizes + ", id=" + id
                    + ", owner_id=" + owner_id + ", owner_name=" + owner_name + ", tagset_id=" + tagset_id
                    + ", created=" + created + ", compilation_status=" + compilation_status + ", handle=" + handle
                    + ", fcsrefs=" + fcsrefs + ", fcsinfos=" + fcsinfos

                    // + ", sketch_grammar_id=" + sketch_grammar_id + ", term_grammar_id=" +
                    // term_grammar_id + ", _is_sgdev=" + _is_sgdev + ", is_featured=" + is_featured
                    // + ", access_on_demand=" + access_on_demand + ", terms_of_use=" + terms_of_use
                    // + ", sort_to_end=" + sort_to_end + ", tags=" + tags + ", needs_recompiling="
                    // + needs_recompiling + ", user_can_read=" + user_can_read + ",
                    // user_can_upload=" + user_can_upload + ", user_can_manage=" + user_can_manage
                    // + ", is_shared=" + is_shared + ", is_error_corpus=" + is_error_corpus + ",
                    // new_version=" + new_version + ", wsdef=" + wsdef + ", termdef=" + termdef +
                    // ", diachronic=" + diachronic + ", aligned=" + aligned + ", docstructure=" +
                    // docstructure

                    + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((corpname == null) ? 0 : corpname.hashCode());
            result = prime * result + ((language_id == null) ? 0 : language_id.hashCode());
            result = prime * result + ((language_name == null) ? 0 : language_name.hashCode());
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            result = prime * result + ((info == null) ? 0 : info.hashCode());
            result = prime * result + ((sizes == null) ? 0 : sizes.hashCode());
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            result = prime * result + ((owner_id == null) ? 0 : owner_id.hashCode());
            result = prime * result + ((owner_name == null) ? 0 : owner_name.hashCode());
            result = prime * result + ((tagset_id == null) ? 0 : tagset_id.hashCode());
            result = prime * result + ((created == null) ? 0 : created.hashCode());
            result = prime * result + ((compilation_status == null) ? 0 : compilation_status.hashCode());
            result = prime * result + ((handle == null) ? 0 : handle.hashCode());
            result = prime * result + ((fcsrefs == null) ? 0 : fcsrefs.hashCode());
            result = prime * result + ((fcsinfos == null) ? 0 : fcsinfos.hashCode());

            // @formatter:off
            // result = prime * result + ((sketch_grammar_id == null) ? 0 : sketch_grammar_id.hashCode());
            // result = prime * result + ((term_grammar_id == null) ? 0 : term_grammar_id.hashCode());
            // result = prime * result + ((_is_sgdev == null) ? 0 : _is_sgdev.hashCode());
            // result = prime * result + ((is_featured == null) ? 0 : is_featured.hashCode());
            // result = prime * result + ((access_on_demand == null) ? 0 : access_on_demand.hashCode());
            // result = prime * result + ((terms_of_use == null) ? 0 : terms_of_use.hashCode());
            // result = prime * result + ((sort_to_end == null) ? 0 : sort_to_end.hashCode());
            // result = prime * result + ((tags == null) ? 0 : tags.hashCode());
            // result = prime * result + (needs_recompiling ? 1231 : 1237);
            // result = prime * result + (user_can_read ? 1231 : 1237);
            // result = prime * result + (user_can_upload ? 1231 : 1237);
            // result = prime * result + (user_can_manage ? 1231 : 1237);
            // result = prime * result + (is_shared ? 1231 : 1237);
            // result = prime * result + (is_error_corpus ? 1231 : 1237);
            // result = prime * result + ((new_version == null) ? 0 : new_version.hashCode());
            // result = prime * result + ((wsdef == null) ? 0 : wsdef.hashCode());
            // result = prime * result + ((termdef == null) ? 0 : termdef.hashCode());
            // result = prime * result + (diachronic ? 1231 : 1237);
            // result = prime * result + ((aligned == null) ? 0 : aligned.hashCode());
            // result = prime * result + ((docstructure == null) ? 0 : docstructure.hashCode());
            // @formatter:on

            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Corpus other = (Corpus) obj;
            if (corpname == null) {
                if (other.corpname != null)
                    return false;
            } else if (!corpname.equals(other.corpname))
                return false;
            if (language_id == null) {
                if (other.language_id != null)
                    return false;
            } else if (!language_id.equals(other.language_id))
                return false;
            if (language_name == null) {
                if (other.language_name != null)
                    return false;
            } else if (!language_name.equals(other.language_name))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (info == null) {
                if (other.info != null)
                    return false;
            } else if (!info.equals(other.info))
                return false;
            if (sizes == null) {
                if (other.sizes != null)
                    return false;
            } else if (!sizes.equals(other.sizes))
                return false;
            if (id == null) {
                if (other.id != null)
                    return false;
            } else if (!id.equals(other.id))
                return false;
            if (owner_id == null) {
                if (other.owner_id != null)
                    return false;
            } else if (!owner_id.equals(other.owner_id))
                return false;
            if (owner_name == null) {
                if (other.owner_name != null)
                    return false;
            } else if (!owner_name.equals(other.owner_name))
                return false;
            if (tagset_id == null) {
                if (other.tagset_id != null)
                    return false;
            } else if (!tagset_id.equals(other.tagset_id))
                return false;
            if (created == null) {
                if (other.created != null)
                    return false;
            } else if (!created.equals(other.created))
                return false;
            if (compilation_status == null) {
                if (other.compilation_status != null)
                    return false;
            } else if (!compilation_status.equals(other.compilation_status))
                return false;
            if (handle == null) {
                if (other.handle != null)
                    return false;
            } else if (!handle.equals(other.handle))
                return false;
            if (fcsrefs == null) {
                if (other.fcsrefs != null)
                    return false;
            } else if (!fcsrefs.equals(other.fcsrefs))
                return false;
            if (fcsinfos == null) {
                if (other.fcsinfos != null)
                    return false;
            } else if (!fcsinfos.equals(other.fcsinfos))
                return false;

            // @formatter:off
            // if (sketch_grammar_id == null) {
            //     if (other.sketch_grammar_id != null)
            //         return false;
            // } else if (!sketch_grammar_id.equals(other.sketch_grammar_id))
            //     return false;
            // if (term_grammar_id == null) {
            //     if (other.term_grammar_id != null)
            //         return false;
            // } else if (!term_grammar_id.equals(other.term_grammar_id))
            //     return false;
            // if (_is_sgdev == null) {
            //     if (other._is_sgdev != null)
            //         return false;
            // } else if (!_is_sgdev.equals(other._is_sgdev))
            //     return false;
            // if (is_featured == null) {
            //     if (other.is_featured != null)
            //         return false;
            // } else if (!is_featured.equals(other.is_featured))
            //     return false;
            // if (access_on_demand == null) {
            //     if (other.access_on_demand != null)
            //         return false;
            // } else if (!access_on_demand.equals(other.access_on_demand))
            //     return false;
            // if (terms_of_use == null) {
            //     if (other.terms_of_use != null)
            //         return false;
            // } else if (!terms_of_use.equals(other.terms_of_use))
            //     return false;
            // if (sort_to_end == null) {
            //     if (other.sort_to_end != null)
            //         return false;
            // } else if (!sort_to_end.equals(other.sort_to_end))
            //     return false;
            // if (tags == null) {
            //     if (other.tags != null)
            //         return false;
            // } else if (!tags.equals(other.tags))
            //     return false;
            // if (needs_recompiling != other.needs_recompiling)
            //     return false;
            // if (user_can_read != other.user_can_read)
            //     return false;
            // if (user_can_upload != other.user_can_upload)
            //     return false;
            // if (user_can_manage != other.user_can_manage)
            //     return false;
            // if (is_shared != other.is_shared)
            //     return false;
            // if (is_error_corpus != other.is_error_corpus)
            //     return false;
            // if (new_version == null) {
            //     if (other.new_version != null)
            //         return false;
            // } else if (!new_version.equals(other.new_version))
            //     return false;
            // if (wsdef == null) {
            //     if (other.wsdef != null)
            //         return false;
            // } else if (!wsdef.equals(other.wsdef))
            //     return false;
            // if (termdef == null) {
            //     if (other.termdef != null)
            //         return false;
            // } else if (!termdef.equals(other.termdef))
            //     return false;
            // if (diachronic != other.diachronic)
            //     return false;
            // if (aligned == null) {
            //     if (other.aligned != null)
            //         return false;
            // } else if (!aligned.equals(other.aligned))
            //     return false;
            // if (docstructure == null) {
            //     if (other.docstructure != null)
            //         return false;
            // } else if (!docstructure.equals(other.docstructure))
            //     return false;
            // @formatter:on

            return true;
        }

        // @formatter:off
        /*
        // just skip all those attributes, we probably will never need them
        public String sketch_grammar_id; // null
        public String term_grammar_id; // null
        public String _is_sgdev;
        public String is_featured;
        public String access_on_demand;
        public String terms_of_use; // null
        public String sort_to_end; // null
        public List<Object> tags;
        
        public boolean needs_recompiling;
        public boolean user_can_read;
        public boolean user_can_upload;
        public boolean user_can_manage;
        public boolean is_shared;
        public boolean is_error_corpus;
        
        public String new_version;
        public String wsdef;
        public String termdef;
        public boolean diachronic;
        public List<Object> aligned;
        public String docstructure;
        // */
        // @formatter:on
    }

}
