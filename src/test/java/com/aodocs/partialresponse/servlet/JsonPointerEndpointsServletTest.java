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

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.servlet.ServletException;

import org.junit.Assert;
import org.junit.Test;

import com.aodocs.partialresponse.json.JsonPointerJsonFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class JsonPointerEndpointsServletTest extends BaseEndpointsServletTest {
	
	private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
	
	private final static Map<String, String> ENABLE_JSON_PATH = Collections.singletonMap(PartialResponseEndpointsServlet.ACCEPT_JSON_POINTER_INIT_PARAM, "true");
	
	@Test
	public void testJsonPointerJsonFactory() throws JsonProcessingException {
		checkJsonPointer("/doesNotExist", "{}");
		checkJsonPointer("/integer", "{\"integer\":1}");
		checkJsonPointer("/string", "{\"string\":\"a\"}");
		checkJsonPointer("/array", "{\"array\":[\"0\",\"1\"]}");
		checkJsonPointer("/array/0", "{\"array\":[\"0\"]}");
		checkJsonPointer("/array/1", "{\"array\":[\"1\"]}");
		checkJsonPointer("/object", "{\"object\":{\"integer\":2,\"string\":\"b\"}}");
		checkJsonPointer("/object/integer", "{\"object\":{\"integer\":2}}");
		checkJsonPointer("/object/string", "{\"object\":{\"string\":\"b\"}}");
	}
	
	private void checkJsonPointer(String jsonPointer, String expected) throws JsonProcessingException {
		ObjectWriter writer = OBJECT_MAPPER.writer()
				.with(new JsonPointerJsonFactory(OBJECT_MAPPER.getFactory(), jsonPointer));
		String result = writer.writeValueAsString(new TestApi().get());
		Assert.assertEquals(expected, result);
	}
	
	@Test
	public void testNotEnabled() throws IOException, ServletException {
		PartialResponseEndpointsServlet servlet = initServlet(Collections.<String, String>emptyMap(), TestApi.class);
		String response = serve(servlet, "/integer", 400);
		Assert.assertTrue(response.contains("Invalid fields parameter"));
	}
	
	@Test
	public void testEmptyResponse() throws IOException, ServletException {
		PartialResponseEndpointsServlet servlet = initServlet(ENABLE_JSON_PATH, TestApi.class);
		String response = serve(servlet, "/doesnotexist", 200);
		assertSpaceInsensitiveEquals("{}", response);
	}
	
	@Test
	public void testLeafResponse() throws IOException, ServletException {
		PartialResponseEndpointsServlet servlet = initServlet(ENABLE_JSON_PATH, TestApi.class);
		String response = serve(servlet, "/integer", 200);
		assertSpaceInsensitiveEquals("{\"integer\":1}", response);
	}
	
}
