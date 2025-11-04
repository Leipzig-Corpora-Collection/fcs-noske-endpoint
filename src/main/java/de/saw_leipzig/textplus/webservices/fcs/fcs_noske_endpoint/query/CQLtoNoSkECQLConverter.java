package de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.query;

import org.z3950.zing.cql.CQLAndNode;
import org.z3950.zing.cql.CQLBooleanNode;
import org.z3950.zing.cql.CQLNode;
import org.z3950.zing.cql.CQLOrNode;
import org.z3950.zing.cql.CQLTermNode;

import eu.clarin.sru.server.SRUConstants;
import eu.clarin.sru.server.SRUException;

/**
 * Query converter from CQL (BASIC search) to SketchEngine CQL.
 * The CQL search query will only use Terms, and optionally AND/OR but
 * no other CQL features with the FCS BASIC search capability.
 */
public class CQLtoNoSkECQLConverter {

    /**
     * Convert a FCS basic search CQL query into a (No)SketchEngine CQL (CQP) query.
     * 
     * @param node             the root {@link CQLNode} query
     * @param searchAttributes the (No)SketchEngine corpus attributes (search
     *                         fields)
     * @return String with converted query for (No)SketchEngine
     * @throws SRUException on any conversion error (e.g., unsupported feature
     *                      usage)
     */
    public static String convertCQLtoNoSkECQL(final CQLNode node, final String[] searchAttributes)
            throws SRUException {
        // LOGGER.debug("Query dump:\n{}", node.toXCQL());
        StringBuilder sb = new StringBuilder();

        convertCQLtoNoSkECQLSingle(node, searchAttributes, sb);

        return sb.toString();
    }

    /**
     * Recursive query converter since parsed queries are nested trees.
     * Uses a {@link StringBuilder} to generate the converted query output.
     * 
     * @param node             the current {@link CQLNode} to convert
     * @param searchAttributes the (No)SketchEngine corpus attributes (search
     *                         fields)
     * @param sb               the {@link StringBuilder} with current converted
     *                         output query
     * @throws SRUException on unsupported query feature usage
     */
    private static void convertCQLtoNoSkECQLSingle(final CQLNode node, final String[] searchAttributes,
            StringBuilder sb)
            throws SRUException {
        if (node instanceof CQLTermNode) {
            final CQLTermNode tn = ((CQLTermNode) node);
            if (tn.getIndex() != null && !"cql.serverChoice".equalsIgnoreCase(tn.getIndex())) {
                throw new SRUException(SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                        "Queries with queryType 'cql' do not support index/relation on '"
                                + node.getClass().getSimpleName() + "' by this FCS Endpoint.");
            }
            String termEscaped = tn.getTerm().replaceAll("\"", "\\\"");
            sb.append('[');
            for (String attr : searchAttributes) {
                sb.append(attr);
                sb.append("=\"");
                sb.append(termEscaped);
                sb.append('"');
                sb.append(" | ");
            }
            sb.setLength(sb.length() - 3);
            sb.append(']');
        } else if (node instanceof CQLOrNode || node instanceof CQLAndNode) {
            final CQLBooleanNode bn = (CQLBooleanNode) node;
            if (!bn.getModifiers().isEmpty()) {
                throw new SRUException(SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                        "Queries with queryType 'cql' do not support modifiers on '" + node.getClass().getSimpleName()
                                + "' by this FCS Endpoint.");
                // TODO: http://clarin.eu/fcs/diagnostic/11
                // Constants.FCS_DIAGNOSTIC_GENERAL_QUERY_TOO_COMPLEX_CANNOT_PERFORM_QUERY
            }
            sb.append("(");
            convertCQLtoNoSkECQLSingle(bn.getLeftOperand(), searchAttributes, sb);
            if (node instanceof CQLOrNode) {
                sb.append(" | ");
            } else if (node instanceof CQLAndNode) {
                // implicit
            }
            convertCQLtoNoSkECQLSingle(bn.getRightOperand(), searchAttributes, sb);
            sb.append(")");
        } else {
            throw new SRUException(SRUConstants.SRU_CANNOT_PROCESS_QUERY_REASON_UNKNOWN,
                    "Queries with queryType 'cql' do not support '" + node.getClass().getSimpleName()
                            + "' by this FCS Endpoint.");
        }
    }
}
