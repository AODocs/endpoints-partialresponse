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
import java.io.OutputStream;
import java.io.Writer;

import com.aodocs.partialresponse.fieldsexpression.FieldsExpressionTree;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.IOContext;
import com.google.common.base.Preconditions;

/**
 * Wraps a JsonFactory to generate JsonGenerators that filter the JSON output
 * according to a provided FieldsExpressionTree filter.
 */
public class PartialResponseJsonFactory extends JsonFactory {
	
	private final FieldsExpressionTree filterTree;
	
	public PartialResponseJsonFactory(JsonFactory base, FieldsExpressionTree filterTree) {
		super(base, null);
		this.filterTree = Preconditions.checkNotNull(filterTree, "filterTree cannot be null");
	}
	
	@Override
	protected JsonGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
		return new PartialResponseJsonGenerator(super._createGenerator(out, ctxt), filterTree);
	}
	
	@Override
	protected JsonGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) throws IOException {
		return new PartialResponseJsonGenerator(super._createUTF8Generator(out, ctxt), filterTree);
	}
	
}
