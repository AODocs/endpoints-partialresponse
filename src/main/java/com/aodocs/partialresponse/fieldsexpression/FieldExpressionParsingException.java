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
package com.aodocs.partialresponse.fieldsexpression;

import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * Thrown on an invalid fields expression.
 * See https://developers.google.com/drive/api/v3/performance#partial-response for more detail.
 */
public class FieldExpressionParsingException extends IllegalArgumentException {
	
	private final String fieldsExpression;
	
	public FieldExpressionParsingException(String fieldsExpression, RecognitionException cause) {
		this(fieldsExpression, (Exception) cause);
	}
	
	public FieldExpressionParsingException(String fieldsExpression, ParseCancellationException cause) {
		this(fieldsExpression, (Exception) cause);
	}
	
	private FieldExpressionParsingException(String fieldsExpression, Exception cause) {
		super("fields expression '" + fieldsExpression + "' is invalid", cause);
		this.fieldsExpression = fieldsExpression;
		//TODO improve error reporting by analyzing Antlr cause
	}
	
	public String getFieldsExpression() {
		return fieldsExpression;
	}
	
}
