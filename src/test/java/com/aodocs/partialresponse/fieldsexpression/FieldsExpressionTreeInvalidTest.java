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

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.common.collect.ImmutableList;

@RunWith(Parameterized.class)
public class FieldsExpressionTreeInvalidTest {
	
	@Parameterized.Parameters(name = "{index}: {0}")
	public static Collection<String> data() {
		return ImmutableList.of(
				"", " ", "\t", "\n", " \t\n", //empty
				"/", "/a", "a/", "a//b", //slashes
				",", ",a", "a,", "a,,b", //commas
				"(", ")", "()", "a(", "(a", ")a", "a(b", "((", "))" //parenth
				//these are passing, why? "a)", "a)b"
		);
	}
	
	private final String expression;
	
	public FieldsExpressionTreeInvalidTest(String expression) {
		this.expression = expression;
	}
	
	@Test(expected = FieldExpressionParsingException.class)
	public void testParsing() {
		FieldsExpressionTree.parse(expression);
	}
	
}
