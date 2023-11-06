package de.saw_leipzig.textplus.webservices.fcs.fcs_noske_endpoint.query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.clarin.sru.server.SRUConstants;
import eu.clarin.sru.server.SRUException;
import eu.clarin.sru.server.fcs.parser.Expression;
import eu.clarin.sru.server.fcs.parser.Operator;
import eu.clarin.sru.server.fcs.parser.QueryNode;
import eu.clarin.sru.server.fcs.parser.QueryNodeType;
import eu.clarin.sru.server.fcs.parser.QuerySegment;
import eu.clarin.sru.server.fcs.parser.QuerySequence;

/**
 * Query converter from FCS-QL (ADVANCED search) to SketchEngine CQL.
 * The FCS-QL is really similar to SketchEngine CQL (CQP) but has slight changes
 * that we need to handle. This is for FCS ADVANCED search capability.
 */
public class FCSQLtoNoSkECQLConverter {
    private static final Logger LOGGER = LogManager.getLogger(FCSQLtoNoSkECQLConverter.class);

    /**
     * Converts a FCS-QL {@link QueryNode} to a (No)SketchEngine CQL query.
     * 
     * @param node             the root {@link QueryNode} to convert
     * @param searchAttributes the (No)SketchEngine corpus attributes (search
     *                         fields)
     * @return String with converted (No)SketchEngine CQL query.
     * @throws SRUException on any conversion error
     */
    public static String convertFCSQLtoNoSkECQL(final QueryNode node, final String[] searchAttributes)
            throws SRUException {
        // LOGGER.debug("Query dump:\n{}", node.toString());
        StringBuilder sb = new StringBuilder();

        convertFCSQLtoNoSkECQLSingle(node, searchAttributes, sb);

        return sb.toString();
    }

    /**
     * Convert a FCS-QL {@link QueryNode}. May be called recursively for different
     * {@link QueryNode#getNodeType}.
     * 
     * @param node             the {@link QueryNode} to convert
     * @param searchAttributes the (No)SketchEngine corpus attributes (search
     *                         fields)
     * @param sb               Converted (No)SketchEngine CQL query.
     * @throws SRUException on unknown query syntax/feature usage
     */
    private static void convertFCSQLtoNoSkECQLSingle(final QueryNode node, final String[] searchAttributes,
            StringBuilder sb)
            throws SRUException {

        if (node.getNodeType() == QueryNodeType.EXPRESSION) {
            convertFCSQLExpression((Expression) node, searchAttributes, sb);
        } else if (node.getNodeType() == QueryNodeType.QUERY_SEGMENT) {
            // [ ( word = "Apfel" | word = "Banane" ) ]
            // [ word = "Apfel" & word = "Banane" ]
            // [ ( word = "Apfel" | word = "Banane" ) & word = "Topf" ]
            convertFCSQLQuerySegment((QuerySegment) node, searchAttributes, sb);
        } else if (node.getNodeType() == QueryNodeType.QUERY_SEQUENCE) {
            // [ word = "Apfel" ] [ word = "Banane" ]
            for (QueryNode child : ((QuerySequence) node).getChildren()) {
                convertFCSQLtoNoSkECQLSingle(child, searchAttributes, sb);
            }
        } else {
            throw new SRUException(SRUConstants.SRU_QUERY_FEATURE_UNSUPPORTED,
                    "Unsupported query syntax or features usage: nodeType=" + node.getNodeType());
        }

        // unsupported by parser
        // [ word = "Apfel" & lbound(sentence) ] [ pos = "ADJ" & rbound(sentence) ] ?
    }

    /**
     * Convert a single {@link QuerySegment} (minimal, mandatory unit).
     * 
     * @param node             the {@link QuerySegment} to convert
     * @param searchAttributes the (No)SketchEngine corpus attributes (search
     *                         fields)
     * @param sb               Converted (No)SketchEngine CQL query.
     * @throws SRUException if contained expression throws an error on conversion
     */
    private static void convertFCSQLQuerySegment(final QuerySegment node, final String[] searchAttributes,
            StringBuilder sb) throws SRUException {
        // single token
        sb.append('[');
        convertFCSQLExpressionAny(node.getFirstChild(), searchAttributes, sb);
        sb.append(']');

        if (node.getMinOccurs() != 1 || node.getMaxOccurs() != 1) {
            sb.append('{');
            sb.append(node.getMinOccurs());
            sb.append(',');
            sb.append(node.getMaxOccurs());
            sb.append('}');
        }
    }

    /**
     * Convert any FCS-QL Expression {@link QueryNode}, e.g., grouping, AND/OR or
     * simple expression.
     * 
     * @param node             the {@link QueryNode} to convert
     * @param searchAttributes the (No)SketchEngine corpus attributes (search
     *                         fields)
     * @param sb               Converted (No)SketchEngine CQL query.
     * @throws SRUException on unknown Expression node type
     */
    private static void convertFCSQLExpressionAny(final QueryNode node, final String[] searchAttributes,
            StringBuilder sb) throws SRUException {
        // single token (inner)
        if (node.getNodeType() == QueryNodeType.EXPRESSION) {
            convertFCSQLExpression((Expression) node, searchAttributes, sb);
        } else if (node.getNodeType() == QueryNodeType.EXPRESSION_GROUP) {
            // backets followed by OR usually
            sb.append('(');
            convertFCSQLExpressionAny(node.getFirstChild(), searchAttributes, sb);
            sb.append(')');
        } else if (node.getNodeType() == QueryNodeType.EXPRESSION_OR) {
            // OR (|)
            convertFCSQLExpressionAny(node.getChild(0), searchAttributes, sb);
            sb.append('|');
            convertFCSQLExpressionAny(node.getChild(1), searchAttributes, sb);
        } else if (node.getNodeType() == QueryNodeType.EXPRESSION_AND) {
            // AND (&)
            convertFCSQLExpressionAny(node.getChild(0), searchAttributes, sb);
            sb.append('&');
            convertFCSQLExpressionAny(node.getChild(1), searchAttributes, sb);
        } else {
            throw new SRUException(SRUConstants.SRU_QUERY_FEATURE_UNSUPPORTED,
                    "Unsupported query syntax or features usage: nodeType=" + node.getNodeType()
                            + ", expected EXPRESSION[_(GROUP|OR|AND)]");
            // Constants.FCS_DIAGNOSTIC_GENERAL_QUERY_TOO_COMPLEX_CANNOT_PERFORM_QUERY
        }
    }

    /**
     * Convert a simple {@link Expression} node.
     * 
     * <p>
     * NOTE: assumption that there is no explicit conversion or mapping between
     * requested layer identifier and (No)SketchEngine corpus attribute. If there
     * is, the translation should happen here. This can also include a 1:n mapping
     * if multiple corpus attributes should be searched through.
     * </p>
     * 
     * @param expression       the {@link Expression} to convert
     * @param searchAttributes the (No)SketchEngine corpus attributes (search
     *                         fields)
     * @param sb               Converted (No)SketchEngine CQL query.
     */
    private static void convertFCSQLExpression(final Expression expression, final String[] searchAttributes,
            StringBuilder sb) {
        String key = expression.getLayerIdentifier();
        String value = expression.getRegexValue();
        // boolean opIs = expression.getOperator() == Operator.EQUALS;
        // LOGGER.debug("FCS-CQL query parsed: key: '{}' op: {} val: '{}'", key, opIs,
        // value);

        // if POS add POS_UD17?
        // TODO: if unsupported layer, abort here but return 0 results with
        // http://clarin.eu/fcs/diagnostic/14

        sb.append(key);
        if (expression.getOperator() == Operator.NOT_EQUALS) {
            sb.append('!');
        }
        sb.append('=');
        sb.append('"');
        sb.append(value.replaceAll("\"", "\\\""));
        sb.append('"');
    }

}
