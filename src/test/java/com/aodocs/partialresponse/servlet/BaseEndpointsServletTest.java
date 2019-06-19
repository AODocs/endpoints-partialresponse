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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class BaseEndpointsServletTest {
	
	void assertSpaceInsensitiveEquals(String expected, String actual) {
		assertEquals(expected.replaceAll("\\s+", ""), actual.replaceAll("\\s+", ""));
	}
	
	PartialResponseEndpointsServlet initServlet(Map<String, String> initParams, Class<?> apiClass) throws ServletException {
		ServletConfig config = mock(ServletConfig.class, RETURNS_MOCKS);
		when(config.getInitParameter(anyString())).thenReturn(null);
		when(config.getInitParameter("services")).thenReturn(apiClass.getName());
		for (Map.Entry<String, String> entry : initParams.entrySet()) {
			when(config.getInitParameter(entry.getKey())).thenReturn(entry.getValue());
		}
		PartialResponseEndpointsServlet servlet = new PartialResponseEndpointsServlet();
		servlet.init(config);
		return servlet;
	}
	
	protected String serve(PartialResponseEndpointsServlet servlet,String fieldsParamValue, int expectedStatus) throws IOException {
		HttpServletRequest request = mock(HttpServletRequest.class, RETURNS_MOCKS);
		when(request.getParameter(anyString())).thenReturn(null);
		if (fieldsParamValue != null)  {
			when(request.getParameter("fields")).thenReturn(fieldsParamValue);
		}
		when(request.getMethod()).thenReturn("GET");
		when(request.getRequestURI()).thenReturn("test/v1/test");
		
		HttpServletResponse response = mock(HttpServletResponse.class);
		StringWriter stringWriter = new StringWriter();
		when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));
		when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
			@Override
			public void write(int b) {
				stringWriter.write(b);
			}
		});
		
		servlet.service(request, response);
		response.flushBuffer();
		verify(response).setStatus(expectedStatus);
		return stringWriter.toString();
	}
}
