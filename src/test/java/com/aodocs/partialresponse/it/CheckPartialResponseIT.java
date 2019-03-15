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
package com.aodocs.partialresponse.it;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.aodocs.partialresponse.fieldsexpression.FieldsExpression;
import com.aodocs.partialresponse.json.PartialJsonCopier;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

/**
 * Compares the partial JSON returned by this project with the partial response returned by a Google API.
 * The API used is the Discovery API itself, as it's public.
 */
@RunWith(Parameterized.class)
public class CheckPartialResponseIT extends DiscoveryApiIntegrationTest {
	
	private static final String FULL_DISCOVERY;
	
	static {
		try {
			FULL_DISCOVERY = loadDriveV3Discovery(null).toString();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private JsonParser jp;
	private StringWriter output;
	private JsonGenerator jg;
	private final String fields;
	
	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "*" },
				{ "*/*" },
				{ "auth" },
				{ "*,auth" },
				{ "auth,*" },
				{ "*/oauth2" },
				{ "*,*/oauth2" },
				{ "*/oauth2,*" },
				{ "icons/x16" },
				{ "icons/x16, icons/x32" },
				{ "icons(x16,x32)" },
				{ "icons/*/doesnotexist,*/x16" },
				{ "parameters/*" },
				{ "parameters/*/type" },
				{ "schemas/Change/properties,schemas/About" },
				{ "schemas/*/properties,schemas/About/description" },
				{ "schemas/*/properties,schemas/*" },
				{ "schemas/*,schemas/*/properties" },
				{ "schemas/*,schemas/*(properties)" },
				{ "schemas(*,properties)" },
				{ "schemas(*,*/properties)" },
				{ "*/About/*,schemas/About/id" },
				{ "parameters/alt/*,parameters/alt/*/enum" },
				{ "resources/changes/methods/list/*,resources/changes/methods/list/*/response" },
				{ "baseUrl/*" },
				{ "schemas/*/id/*" },
		});
	}
	
	public CheckPartialResponseIT(String fields) throws IOException {
		JsonFactory factory = new JsonFactory();
		jp = factory.createParser(FULL_DISCOVERY);
		output = new StringWriter();
		jg = factory.createGenerator(output);
		this.fields = fields;
	}
	
	@Test
	public void check() throws IOException {
		String filtered = loadDriveV3Discovery(fields).toString();
		new PartialJsonCopier(jp, jg, FieldsExpression.parse(fields).getFilterTree()).copyAndClose();
		Assert.assertEquals(filtered, output.toString());
	}
	
}
