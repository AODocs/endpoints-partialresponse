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

import java.io.IOException;

import com.aodocs.partialresponse.fieldsexpression.FieldsExpressionTree;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.filter.FilteringGeneratorDelegate;

/**
 * Copies JSON from a JsonParser to a JsonGenerator, filtering the output based on a FieldsExpression.
 * Used for testing only.
 */
public class PartialJsonCopier {
	
	private JsonParser jp;
	private FilteringGeneratorDelegate jg;
	
	public PartialJsonCopier(JsonParser jp, JsonGenerator jg, FieldsExpressionTree filter) {
		this.jp = jp;
		this.jg = new PartialResponseJsonGenerator(jg, filter);
	}
	
	public void copyAndClose() throws IOException {
		jp.nextToken();
		jg.copyCurrentStructure(jp);
		jp.close();
		jg.close();
	}

}
