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

import static org.junit.Assert.assertTrue;

import static com.aodocs.partialresponse.servlet.PartialResponseEndpointsServlet.CHECK_FIELDS_EXPRESSION_INIT_PARAM;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;

import org.junit.Test;

public class PartialResponseEndpointsServletTest extends BaseEndpointsServletTest {
	
	@Test
	public void testFullResponse() throws IOException, ServletException {
		PartialResponseEndpointsServlet servlet = initServlet(Collections.<String, String>emptyMap(), TestApi.class);
		String response = serve(servlet, null, 200);
		assertSpaceInsensitiveEquals("{\"integer\":1,\"string\":\"a\",\"array\":[\"0\",\"1\"],\"object\":{\"integer\":2,\"string\":\"b\"}}", response);
	}
	
	@Test
	public void testEmptyResponse() throws IOException, ServletException {
		PartialResponseEndpointsServlet servlet = initServlet(Collections.<String, String>emptyMap(), TestApi.class);
		String response = serve(servlet, "doesnotexist", 200);
		assertSpaceInsensitiveEquals("{}", response);
	}
	
	@Test
	public void testPartialResponse() throws IOException, ServletException {
		PartialResponseEndpointsServlet servlet = initServlet(Collections.<String, String>emptyMap(), TestApi.class);
		String reponse = serve(servlet, "integer", 200);
		assertSpaceInsensitiveEquals("{\"integer\":1}", reponse);
	}
	
	@Test
	public void testInvalidFieldsNoCheck() throws IOException, ServletException {
		PartialResponseEndpointsServlet servlet = initServlet(Collections.<String, String>emptyMap(), TestApi.class);
		String response = serve(servlet, "integer,int", 200);
		assertSpaceInsensitiveEquals("{\"integer\":1}", response);
		
	}
	
	@Test
	public void testInvalidFieldsCheck() throws IOException, ServletException {
		PartialResponseEndpointsServlet servlet = initServlet(Collections.singletonMap(CHECK_FIELDS_EXPRESSION_INIT_PARAM, "true"), TestApi.class);
		String response = serve(servlet, "a,c", 400);
		assertTrue(response.contains("Invalid field selection"));
	}
	
}
