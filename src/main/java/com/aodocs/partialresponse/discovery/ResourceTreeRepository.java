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
package com.aodocs.partialresponse.discovery;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import com.aodocs.partialresponse.fieldsexpression.FieldsExpressionNode;
import com.aodocs.partialresponse.fieldsexpression.FieldsExpressionTree;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.json.JsonParser;
import com.google.api.services.discovery.model.JsonSchema;
import com.google.api.services.discovery.model.RestDescription;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

/**
 * This class loads an API description in Discovery format, and returns FieldsExpressionTrees
 * for each resource (schemas) in it, describing all available fields for this resource.
 * The result is a FieldsExpressionTree, that can be used to check if another expression is contained.
 */
public class ResourceTreeRepository {
	
	public static ResourceTreeRepository load(URL restDescriptionUrl) throws IOException {
		try (InputStream inputStream = restDescriptionUrl.openStream()) {
			JsonParser jsonParser = Utils.getDefaultJsonFactory().createJsonParser(inputStream);
			RestDescription restDescription = jsonParser.parse(RestDescription.class);
			return new ResourceTreeRepository(restDescription);
		}
	}
	
	private final Map<String, JsonSchema> schemas;
	private final Map<String, FieldsExpressionNode> rootNodeCache = Maps.newHashMap();
	
	public ResourceTreeRepository(RestDescription restDescription) {
		this.schemas = restDescription.getSchemas();
	}
	
	@VisibleForTesting
	Collection<FieldsExpressionNode> loadAllRootNodes() {
		if (schemas != null) { // an API might have no resource at all
			for (String name : schemas.keySet()) {
				getResourceTree(name);
			}
		}
		return rootNodeCache.values();
	}
	
	public FieldsExpressionTree getResourceTree(String schemaName) {
		return new FieldsExpressionTree(getRootNode(schemaName));
	}
	
	private FieldsExpressionNode getRootNode(String schemaName) {
		FieldsExpressionNode rootNode = rootNodeCache.get(schemaName);
		if (rootNode == null) {
			Preconditions.checkArgument(schemas.get(schemaName) != null,
					"Schema does not exist for resource " + schemaName);
			rootNode = buildRootNode(schemaName, schemas.get(schemaName));
		}
		return rootNode;
	}
	
	private FieldsExpressionNode buildRootNode(String name, JsonSchema schema) {
		FieldsExpressionNode.Builder builder = FieldsExpressionNode.Builder.createRoot();
		FieldsExpressionNode rootNode = builder.getNode();
		rootNodeCache.put(name, rootNode); //cache before recursing to handle cycles
		buildNode(builder, schema);
		return rootNode;
	}
	
	private void buildNode(FieldsExpressionNode.Builder builder, JsonSchema schema) {
		Map<String, JsonSchema> properties = schema.getProperties();
		JsonSchema additionalProperties = schema.getAdditionalProperties();
		if (properties != null) {
			for (Map.Entry<String, JsonSchema> propertyEntry : properties.entrySet()) {
				buildChild(builder, propertyEntry.getKey(), propertyEntry.getValue());
			}
		}
		if (additionalProperties != null) {
			buildChild(builder, "*", additionalProperties);
		}
	}
	
	private void buildChild(FieldsExpressionNode.Builder parentBuilder, String childName, JsonSchema childSchema) {
		FieldsExpressionNode.Builder builder = parentBuilder.getOrAddChild(childName);
		
		String $ref = childSchema.get$ref(); //schema reference
		String type = childSchema.getType(); //simple types
		
		if ($ref != null) {
			Preconditions.checkState(type == null, "type must be null for $ref");
			builder.merge(getResourceTree($ref).getRoot());
			return;
		}
		
		Preconditions.checkNotNull(type, "type must be present for non-$ref");
		switch (Preconditions.checkNotNull(type)) {
			case "array":
				//bypass array level
				buildChild(parentBuilder, childName,
						Preconditions.checkNotNull(childSchema.getItems(),
								"items must be present for array types"));
				break;
			case "object":
				buildNode(builder, childSchema);
				break;
			case "any":
				builder.setCatchAllChild();
			default:
				//leaf, stop here
				break;
		}
	}
	
}
