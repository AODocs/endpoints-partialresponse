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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import nl.jqno.equalsverifier.EqualsVerifier;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.common.collect.ImmutableList;

@RunWith(Parameterized.class)
public class FieldsExpressionTest {
	
	@Parameterized.Parameters(name = "{index}: {0}")
	public static Collection<Object[]> data() {
		FieldsExpressionNode.Builder nested = FieldsExpressionNode.Builder.ofBranch("items", "author", "uri");
		nested.getOrAddBranch(ImmutableList.of("items", "title"));
		
		FieldsExpressionNode.Builder deepNested = FieldsExpressionNode.Builder.ofBranch("items", "author", "uri", "a");
		deepNested.getOrAddBranch(ImmutableList.of("items", "author", "uri", "b"));
		deepNested.getOrAddBranch(ImmutableList.of("items", "title"));
		
		return Arrays.asList(new Object[][] {
				{ "items", FieldsExpressionNode.Builder.ofBranch("items") },
				{ "etag,items", FieldsExpressionNode.Builder.withChildren("etag", "items") },
				{ "context/facets/label", FieldsExpressionNode.Builder.ofBranch("context", "facets", "label") },
				{ "items/pagemap/*", FieldsExpressionNode.Builder.ofBranch("items", "pagemap") },
				{ "items/pagemap/*/title", FieldsExpressionNode.Builder.ofBranch("items", "pagemap", "*", "title") },
				{ "items(id)", FieldsExpressionNode.Builder.ofBranch("items", "id") },
				{ "items/id", FieldsExpressionNode.Builder.ofBranch("items", "id") },
				{ "items(author/uri,title)", nested },
				{ "items/author/uri,items/title", nested },
				{ "items(title,author/uri(a,b))", deepNested },
				{ "items/title,items/author/uri/a,items/author/uri/b))", deepNested },
				{ "items/id,items", FieldsExpressionNode.Builder.ofBranch("items") },
		});
	}
	
	private String input;
	private FieldsExpressionTree expectedOutput;
	
	public FieldsExpressionTest(String input, FieldsExpressionNode.Builder output) {
		this.input = input;
		this.expectedOutput = new FieldsExpressionTree(output.getNode());
	}
	
	@Test
	public void equalsContract() {
		EqualsVerifier.forClass(FieldsExpressionTree.class)
				.withPrefabValues(FieldsExpressionNode.class,
						expectedOutput.getRoot(),
						FieldsExpressionNode.Builder.ofBranch("test").getNode())
				.verify();
	}
	
	@Test
	public void testParsing() {
		FieldsExpressionTree actualOutput = FieldsExpression.parse(input).getFilterTree();
		assertEquals("Unexpected parsing output for '" + input + "':\n" + prettyDiff(actualOutput),
				expectedOutput, actualOutput
		);
	}
	
	private String prettyDiff(FieldsExpressionTree output) {
		return "actual: " + output.prettyPrint() + "\n expected: " + expectedOutput.prettyPrint() + "\n";
	}
	
}
