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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.filter.FilteringGeneratorDelegate;
import com.fasterxml.jackson.core.filter.TokenFilter;

/**
 * A FilteringGeneratorDelegate that will not output a blank result if everything is filtered,
 * but will return an empty struct (either {} or [] depending on the first token encountered).
 */
class AbstractFilteringGenerator extends FilteringGeneratorDelegate {
	
	private Boolean rootIsArray;
	
	AbstractFilteringGenerator(JsonGenerator d, TokenFilter f, boolean includePath, boolean allowMultipleMatches) {
		super(d, f, includePath, allowMultipleMatches);
	}
	
	@Override
	public void writeStartArray() throws IOException {
		setRootIsArrayIfUndefined(true);
		super.writeStartArray();
	}
	
	@Override
	public void writeStartArray(int size) throws IOException {
		setRootIsArrayIfUndefined(true);
		super.writeStartArray(size);
	}
	
	@Override
	public void writeStartObject() throws IOException {
		setRootIsArrayIfUndefined(false);
		super.writeStartObject();
	}
	
	@Override
	public void writeStartObject(Object forValue) throws IOException {
		setRootIsArrayIfUndefined(false);
		super.writeStartObject(forValue);
	}
	
	private void setRootIsArrayIfUndefined(boolean rootIsArray) {
		if (this.rootIsArray == null) {
			this.rootIsArray = rootIsArray;
		}
	}
	
	@Override
	public void close() throws IOException {
		if (getMatchCount() == 0) {
			if (rootIsArray) {
				delegate.writeStartArray();
				delegate.writeEndArray();
			} else {
				delegate.writeStartObject();
				delegate.writeEndObject();
			}
		}
		super.close();
	}
	
}
