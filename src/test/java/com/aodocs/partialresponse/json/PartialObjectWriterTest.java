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
package com.aodocs.partialresponse.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import org.junit.Assert;
import org.junit.Test;

import com.aodocs.partialresponse.fieldsexpression.FieldsExpressionTree;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;

public class PartialObjectWriterTest {
	
	@Test
	public void testWriter() throws IOException {
		StringWriter output = new StringWriter();
		FieldsExpressionTree filter = FieldsExpressionTree.parse("b");
		new PartialObjectWriter(new ObjectMapper().writer(), filter).writeValue(output, new SimpleObject());
		Assert.assertEquals("{\"b\":\"b\"}", output.toString());
	}
	
	@Test
	public void testOutputStream() throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		FieldsExpressionTree filter = FieldsExpressionTree.parse("b");
		new PartialObjectWriter(new ObjectMapper().writer(), filter).writeValue(output, new SimpleObject());
		Assert.assertEquals("{\"b\":\"b\"}", new String(output.toByteArray()));
	}
	
	@Test
	public void testSequenceWriter() throws IOException {
		StringWriter output = new StringWriter();
		FieldsExpressionTree filter = FieldsExpressionTree.parse("b");
		ObjectWriter writer = new ObjectMapper().writer();
		SequenceWriter sequenceWriter = new PartialObjectWriter(writer, filter)
				.writeValues(output);
		sequenceWriter.write(new SimpleObject());
		sequenceWriter.flush();
		Assert.assertEquals("{\"b\":\"b\"}", output.toString());
	}
	
	public static class SimpleObject {
		private String a;
		private String b;
		
		public SimpleObject() {
			this.a = "a";
			this.b = "b";
		}
		
		public String getA() {
			return a;
		}
		
		public void setA(String a) {
			this.a = a;
		}
		
		public String getB() {
			return b;
		}
		
		public void setB(String b) {
			this.b = b;
		}
	}
	
	private class PartialObjectWriter extends ObjectWriter {
		
		private PartialObjectWriter(ObjectWriter base, FieldsExpressionTree filter) {
			super(base, new PartialResponseJsonFactory(base.getFactory(), filter));
		}
		
	}
}
