This project provides an implementation of EndpointsServlet with [partial response](https://developers.google.com/discovery/v1/performance#partial-response) support using "fields" parameter in Discovery-based APIs.

This feature used to be available in Cloud Endpoints v1, but [was removed in v2](https://cloud.google.com/endpoints/docs/frameworks/java/migrating#currently_excluded_features_and_tools).

It provides some additional features on top of partial responses:
- Validates the fields expression BEFORE calling backend methods, to avoid unnecessary work. This is disabled by default, enabled with "checkFieldsExpression=true" servlet init param
- Support for [Json Pointer](https://tools.ietf.org/html/rfc6901) expressions if fields starts with a slash. This is disabled by default, enabled with "acceptJsonPointer=true" servlet init param

Some technical context:
- Antlr4 is used to generate the parser for the "fields" expression, and produces a tree describing the expression
- A similar structure is generated using Discovery files, so valid fields expression can be checked
- JSON "filtering" is implemented using the [FilteringGeneratorDelegate](https://github.com/FasterXML/jackson-core/blob/master/src/main/java/com/fasterxml/jackson/core/filter/FilteringGeneratorDelegate.java) feature from Jackson

To use this feature, just replace the declaration of com.google.api.server.spi.EndpointsServlet with com.aodocs.partialresponse.servlet.PartialResponseEndpointsServlet in your web.xml (or use annotations).
