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
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.aodocs.partialresponse.fieldsexpression.FieldsExpression;
import com.aodocs.partialresponse.fieldsexpression.FieldsExpressionTree;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

/**
 * Checks {@link FieldsExpressionTree#contains} on various schema configurations.
 */
@RunWith(Parameterized.class)
public class ResourceTreeRepositoryTest {
	
	private static final String DRIVE_FILE = "driveFile";
	private static final String DIRECTORY_USER = "directoryUser";
	//used because it has a recursive definition
	private static final String DIRECTORY_PRIVILEGE = "directoryPrivilege";
	//used because contains both additionalProperties with $ref and any
	private static final String APPENGINE_LOCATION = "appengineLocation";
	private static final String APPENGINE_DEPLOYMENT = "appengineDeployment";
	//from https://github.com/google/google-api-go-client/blob/master/google-api-go-generator/testdata/mapofobjects.json
	private static final String MAP_ENTITY = "mapEntity";
	private static final String CYCLE_ENTITY = "cycleEntity";
	
	//Various existing schema
	private static Map<String, FieldsExpressionTree> RESOURCE_TREES = ImmutableMap.<String, FieldsExpressionTree>builder()
			.put(DRIVE_FILE, getResourceFromApi("drive", "v3", "File"))
			.put(DIRECTORY_USER, getResourceFromApi("admin", "directory_v1", "User"))
			.put(DIRECTORY_PRIVILEGE, getResourceFromApi("admin", "directory_v1", "Privilege"))
			.put(APPENGINE_LOCATION, getResourceFromApi("appengine", "v1", "Location"))
			.put(APPENGINE_DEPLOYMENT, getResourceFromApi("appengine", "v1", "Deployment"))
			.put(MAP_ENTITY, getResourceFromApi("testcases", "v1", "MapEntity"))
			.put(CYCLE_ENTITY, getResourceFromApi("testcases", "v1", "CycleEntity"))
			.build();
	
	public static FieldsExpressionTree getResourceFromApi(String api, String version, String resourceName) {
		return getRepository(api, version).getResourceTree(resourceName);
	}
	
	public static ResourceTreeRepository getRepository(String api, String version) {
		try {
			return ResourceTreeRepository.load(Resources.getResource("googleapis/" + api + "/" + version.replace("_", "/") + "/"
					+ api + "-api.json"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Parameterized.Parameters(name = "{0}|{1} => {2}")
	public static Object[][] createParams() {
		return new Object[][] {
				//wildcard
				{ DRIVE_FILE, "*", true },
				{ DRIVE_FILE, "*,capabilities", true },
				{ DRIVE_FILE, "capabilities,*", true },
				{ DRIVE_FILE, "*,doesNotExist", false },
				{ DRIVE_FILE, "doesNotExist,*", false },
				{ DRIVE_FILE, "*/canAddChildren", true },
				{ DRIVE_FILE, "*/doesNotExist", true }, //matches appProperties/*
				{ DRIVE_FILE, "*/*/doesNotExist", false }, //matches nothing
				{ DRIVE_FILE, "*,capabilities/canAddChildren", true },
				{ DRIVE_FILE, "capabilities/canAddChildren,*", true },
				{ DRIVE_FILE, "*,capabilities/doesNotExist", false },
				{ DRIVE_FILE, "capabilities/doesNotExist,*", false },
				//root simple types
				{ DRIVE_FILE, "name", true },
				{ DRIVE_FILE, "title", false },
				//root with nested simple types
				{ DRIVE_FILE, "capabilities", true },
				{ DRIVE_FILE, "capability", false },
				//sublevels with simple types
				{ DRIVE_FILE, "capabilities/canAddChildren", true },
				{ DRIVE_FILE, "capabilities/canAddChild", false },
				{ DRIVE_FILE, "imageMediaMetadata/location/altitude", true },
				//root with nested $ref
				{ DRIVE_FILE, "lastModifyingUser", true },
				{ DRIVE_FILE, "lastModifyingUser/emailAddress", true },
				{ DRIVE_FILE, "lastModifyingUser/email", false },
				//wildcards
				{ DRIVE_FILE, "*/canAddChildren", true },
				{ DRIVE_FILE, "capabilities/canAddChildren/*", true }, //no additional sublevel
				{ DRIVE_FILE, "imageMediaMetadata/*/altitude", true },
				{ DRIVE_FILE, "imageMediaMetadat/*/doesNotExist", false },
				//any
				{ DIRECTORY_USER, "addresses", true },
				{ DIRECTORY_USER, "addresses/anything", true },
				{ DIRECTORY_USER, "addresses/anything/at/any/level", true },
				//recursive definition
				{ DIRECTORY_PRIVILEGE, "privilegeName", true },
				{ DIRECTORY_PRIVILEGE, "childPrivileges/childPrivileges/childPrivileges", true },
				{ DIRECTORY_PRIVILEGE, "childPrivileges/childPrivileges/childPrivileges/privilegeName", true },
				//additionalProperties with simple types
				{ DRIVE_FILE, "appProperties/anyValue", true },
				{ DRIVE_FILE, "appProperties/anotherValue", true },
				{ DRIVE_FILE, "appProperties/anyValue/anyValue", false },
				//additionalProperties with object
				{ MAP_ENTITY, "properties/anyKey", true },
				{ MAP_ENTITY, "properties/anyKey/name", true },
				{ MAP_ENTITY, "properties/anyKey/notInEntity", false },
				{ MAP_ENTITY, "nestedAdditionalProperties/anyKey/anyKey", true },
				{ MAP_ENTITY, "nestedAdditionalProperties/anyKey/anyKey/name", true },
				{ MAP_ENTITY, "nestedAdditionalProperties/anyKey/anyKey/notInEntity", false },
				//complex cycles
				{ CYCLE_ENTITY, "self/self/self/self", true },
				{ CYCLE_ENTITY, "indirect/cycle/indirect/cycle/indirect/cycle", true },
				{ CYCLE_ENTITY, "indirect/cycle/self/indirect/cycle/self", true },
				//additionalProperties with any
				{ APPENGINE_LOCATION, "metadata/anyKey", true },
				{ APPENGINE_LOCATION, "metadata/anotherKey", true },
				{ APPENGINE_LOCATION, "metadata/anyKey/anyValue", true },
				{ APPENGINE_LOCATION, "metadata/anyKey/anyValue/anyValue", true },
				//additionalProperties with $ref
				{ APPENGINE_DEPLOYMENT, "files", true },
				{ APPENGINE_DEPLOYMENT, "files/anyKey", true },
				{ APPENGINE_DEPLOYMENT, "files/anyKey/mimeType", true },
				{ APPENGINE_DEPLOYMENT, "files/anyKey/notInFile", false },
		};
	}
	
	private String resourceName;
	private String fieldsExpression;
	private boolean shouldMatch;
	
	public ResourceTreeRepositoryTest(String resourceName, String fieldsExpression, boolean shouldMatch) {
		this.resourceName = resourceName;
		this.fieldsExpression = fieldsExpression;
		this.shouldMatch = shouldMatch;
	}
	
	@Test
	public void testContainment() {
		FieldsExpressionTree resourceSchema = RESOURCE_TREES.get(resourceName);
		FieldsExpression parsedExpression = FieldsExpression.parse(fieldsExpression);
		Assert.assertEquals(shouldMatch, parsedExpression.isValidAgainst(resourceSchema));
	}
	
}
