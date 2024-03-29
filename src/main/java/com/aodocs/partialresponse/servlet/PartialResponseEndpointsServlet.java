/*-
 * #%L
 * Partial response support for Cloud Endpoints v2
 * ---
 * Copyright (C) 2018 AODocs (Altirnao Inc)
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.aodocs.partialresponse.servlet;

import com.aodocs.partialresponse.discovery.ResourceTreeRepository;
import com.aodocs.partialresponse.fieldsexpression.FieldsExpression;
import com.aodocs.partialresponse.fieldsexpression.FieldsExpressionTree;
import com.aodocs.partialresponse.json.JsonPointerJsonFactory;
import com.aodocs.partialresponse.json.PartialResponseJsonFactory;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.api.server.spi.EndpointMethod;
import com.google.api.server.spi.EndpointsContext;
import com.google.api.server.spi.EndpointsServlet;
import com.google.api.server.spi.ServiceException;
import com.google.api.server.spi.SystemService;
import com.google.api.server.spi.TypeLoader;
import com.google.api.server.spi.config.model.ApiConfig;
import com.google.api.server.spi.config.model.ApiKey;
import com.google.api.server.spi.config.model.ApiMethodConfig;
import com.google.api.server.spi.config.model.ApiSerializationConfig;
import com.google.api.server.spi.config.model.SchemaRepository;
import com.google.api.server.spi.config.model.StandardParameters;
import com.google.api.server.spi.config.model.Types;
import com.google.api.server.spi.discovery.CachingDiscoveryProvider;
import com.google.api.server.spi.discovery.DiscoveryGenerator;
import com.google.api.server.spi.discovery.DiscoveryProvider;
import com.google.api.server.spi.discovery.LocalDiscoveryProvider;
import com.google.api.server.spi.handlers.EndpointsMethodHandler;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.RestResponseResultWriter;
import com.google.api.server.spi.response.ResultWriter;
import com.google.api.services.discovery.model.RestDescription;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import java.util.Optional;
import java.util.function.Function;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * Enables JSON partial response based on "fields" query parameter.
 * 
 * It supports two modes:<ul>
 * <li>If fields expression does not start with a /, the regular mode is enabled,
 * with very close behavior to
 * <a href="https://developers.google.com/discovery/v1/performance#partial-response">
 *     what is implemented in Google APIs</a></li>
 * <li>If fields expression starts with a /, the <a href="https://tools.ietf.org/html/rfc6901">JSON Pointer</a>
 * mode is enabled. It only allows a single node selection, and does not support wildcards</li>
 * </ul>
 * 
 * Initialization parameters:<ul>
 * <li>In regular mode, the fields expression is not checked against resource schema by default.
 *  To enable this features (available in Endpoints v1), set the "checkFieldsExpression"
 *  servlet init parameter to true.</li>
 * <li>The JSON pointer support is not enabled, set the "acceptJsonPointer" servlet init parameter to true
 * to activate it.</li>
 * </ul>
 * 
 * If the response is filtered by a standard fields expression (not using JSON Pointer), then
 * a {@link RequestedFields} instance is accessible with 
 * {@link PartialResponseEndpointsServlet#getRequestedFields}. This can be used by an API
 * implementation to perform optimization based on the requested fields.
 *
 */
public class PartialResponseEndpointsServlet extends EndpointsServlet {

	public static final String REQUESTED_FIELDS_ATTR_NAME = "endpoints.partialReponse.requestedFields";
	
	static final String ACCEPT_JSON_POINTER_INIT_PARAM = "acceptJsonPointer";
	static final String CHECK_FIELDS_EXPRESSION_INIT_PARAM = "checkFieldsExpression";

	private LoadingCache<ApiKey, ResourceTreeRepository> resourceTreeRepositoryCache;
	private boolean acceptJsonPointer;
	private boolean checkFieldsExpression;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		this.acceptJsonPointer = getBooleanInitParam(config, 
				ACCEPT_JSON_POINTER_INIT_PARAM, false);
		this.checkFieldsExpression = getBooleanInitParam(config, 
				CHECK_FIELDS_EXPRESSION_INIT_PARAM, false);
		if (checkFieldsExpression) {
			resourceTreeRepositoryCache = CacheBuilder.newBuilder().build(new CacheLoader<ApiKey, ResourceTreeRepository>() {
				private DiscoveryProvider discoveryProvider = createDiscoveryProvider();
				
				@Override
				public ResourceTreeRepository load(ApiKey apiKey) throws Exception {
					RestDescription restDocument = discoveryProvider.getRestDocument(
							"dummy", apiKey.getName(), apiKey.getVersion());
					return new ResourceTreeRepository(restDocument);
				}
			});
		}
	}
	
	private Boolean getBooleanInitParam(ServletConfig config, String name, boolean defaultValue) {
		return Optional.ofNullable(config.getInitParameter(name)).map(Boolean::parseBoolean).orElse(defaultValue);
	}
	
	private DiscoveryProvider createDiscoveryProvider() {
		ImmutableList<ApiConfig> apiConfigs = getSystemService().getEndpoints().stream().map(SystemService.EndpointNode::getConfig).collect(ImmutableList.toImmutableList());
		try {
			TypeLoader typeLoader = new TypeLoader(getClass().getClassLoader());
			return new CachingDiscoveryProvider(new LocalDiscoveryProvider(
					apiConfigs,
					new DiscoveryGenerator(typeLoader),
					new SchemaRepository(typeLoader)));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	protected EndpointsMethodHandler createEndpointsMethodHandler(EndpointMethod method, ApiMethodConfig methodConfig) {
		return new EndpointsMethodHandler(getInitParameters(), getServletContext(), method, methodConfig, getSystemService()) {
			@Override
			protected ResultWriter createResultWriter(EndpointsContext context, ApiSerializationConfig serializationConfig) throws ServiceException {
				HttpServletRequest request = context.getRequest();
				String fieldsParameterValue = request.getParameter(StandardParameters.FIELDS);
				if (Strings.isNullOrEmpty(fieldsParameterValue) || "*".equals(fieldsParameterValue)) {
					return super.createResultWriter(context, serializationConfig);
				}
				Function<JsonFactory, JsonFactory> jsonFactoryConfigurator 
						= getConfigurator(fieldsParameterValue, serializationConfig, request);
				
				return new RestResponseResultWriter(context.getResponse(), serializationConfig, 
						StandardParameters.shouldPrettyPrint(context), getInitParameters().isAddContentLength(),
						getInitParameters().isExceptionCompatibilityEnabled()) {
					@Override
					protected ObjectWriter configureWriter(ObjectWriter objectWriter) {
						return objectWriter.with(jsonFactoryConfigurator.apply(objectWriter.getFactory()));
					}
				};
			}
			
			private Function<JsonFactory, JsonFactory> getConfigurator(
					String fieldsParameterValue, ApiSerializationConfig serializationConfig,
					HttpServletRequest request) throws BadRequestException {
				if (fieldsParameterValue.startsWith("/")) {
					if (acceptJsonPointer) {
						return input -> new JsonPointerJsonFactory(input, fieldsParameterValue);
					} else {
						throw new BadRequestException("Invalid fields parameter '" + fieldsParameterValue + "'", "invalidParameter", "global");
					}
				} else {
					FieldsExpression fieldsExpression = FieldsExpression.parse(fieldsParameterValue);
					if (checkFieldsExpression) {
						FieldsExpressionTree resourceTree = resourceTreeRepositoryCache
								.getUnchecked(methodConfig.getApiConfig().getApiKey())
								.getResourceTree(Types.getSimpleName(method.getReturnType(), serializationConfig));
						if (!fieldsExpression.isValidAgainst(resourceTree)) {
							//can't match exactly response from Google APIs, as we can't set location and locationType
							throw new BadRequestException("Invalid field selection '" + fieldsParameterValue + "'", "invalidParameter", "global");
						}
					}
					RequestedFieldsImpl requestedFields = new RequestedFieldsImpl(fieldsExpression);
					request.setAttribute(REQUESTED_FIELDS_ATTR_NAME, requestedFields);
					return input -> new PartialResponseJsonFactory(input, fieldsExpression.getFilterTree());
				}
			}
		};
	}
	
	/**
	 * If the response will be filtered by a fields expression, returns an instance of
	 * {@link RequestedFields} that can be used to perform checks on fields to be returned.
	 * 
	 * @param request a HttpServletRequest
	 * @return a requested fields instance (null if no filtering is performed)
	 */
	public static RequestedFields getRequestedFields(HttpServletRequest request) {
		return (RequestedFields) request.getAttribute(REQUESTED_FIELDS_ATTR_NAME);
	}
	
}
