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
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.aodocs.partialresponse.fieldsexpression.FieldsExpressionNode;
import com.aodocs.partialresponse.fieldsexpression.FieldsExpressionTree;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

public class PartialJsonCopyTest {
	
	private JsonFactory factory = new JsonFactory();
	private URL testInput = Resources.getResource("testinput.json");
	private JsonParser jp;
	private StringWriter output;
	private JsonGenerator jg;
	
	@Before
	public void init() throws IOException {
		jp = factory.createParser(testInput);
		output = new StringWriter();
		jg = factory.createGenerator(output);
	}
	
	private PartialJsonCopier newJsonFilteringCopier(JsonParser jp, JsonGenerator jg, FieldsExpressionNode.Builder builder) {
		return new PartialJsonCopier(jp, jg, new FieldsExpressionTree(builder.getNode()));
	}
	
	@Test
	public void testArrayFilter1() throws IOException {
		String input = "[{\"a\":\"1\"}]";
		newJsonFilteringCopier(factory.createParser(input), jg, FieldsExpressionNode.Builder.ofBranch("a")).copyAndClose();
		checkResult(input);
	}
	
	@Test
	public void testArrayFilter2() throws IOException {
		String input = "[{\"a\":\"1\"}]";
		newJsonFilteringCopier(factory.createParser(input), jg, FieldsExpressionNode.Builder.ofBranch("b")).copyAndClose();
		checkResult("[]");
	}
	
	@Test
	public void testRootWildcardFilter() throws IOException {
		newJsonFilteringCopier(jp, jg, FieldsExpressionNode.Builder.ofBranch("*")).copyAndClose();
		checkResult(testInput);
	}
	
	@Test(expected = JsonGenerationException.class)
	public void testInvalidParser() throws IOException {
		jp.nextToken();
		newJsonFilteringCopier(jp, jg, FieldsExpressionNode.Builder.ofBranch("*")).copyAndClose();
	}
	
	@Test
	public void testNoOutput() throws IOException {
		FieldsExpressionNode.Builder root = FieldsExpressionNode.Builder.ofBranch("doesnotexist");
		newJsonFilteringCopier(jp, jg, root).copyAndClose();
		checkResult("{}");
	}
	
	@Test
	public void testSimpleFilter() throws IOException {
		FieldsExpressionNode.Builder root = FieldsExpressionNode.Builder.ofBranch("text");
		newJsonFilteringCopier(jp, jg, root).copyAndClose();
		checkResult("{\"text\":\"string\"}");
	}
	
	@Test
	public void testPrefixFilter() throws IOException {
		FieldsExpressionNode.Builder root = FieldsExpressionNode.Builder.ofBranch("object");
		newJsonFilteringCopier(jp, jg, root).copyAndClose();
		checkResult("{\"object\":{\"A\":\"a\",\"B\":\"b\"}}");
	}
	
	@Test
	public void testWildcardFilter() throws IOException {
		FieldsExpressionNode.Builder root = FieldsExpressionNode.Builder.ofBranch("object", "*");
		newJsonFilteringCopier(jp, jg, root).copyAndClose();
		checkResult("{\"object\":{\"A\":\"a\",\"B\":\"b\"}}");
	}
	
	@Test
	public void testWildcardFilterWithNested() throws IOException {
		FieldsExpressionNode.Builder root = FieldsExpressionNode.Builder.ofBranch("nestedObjects", "*");
		newJsonFilteringCopier(jp, jg, root).copyAndClose();
		checkResult("{\"nestedObjects\":{\"object1\":{\"A\":\"a\",\"B\":\"b\"},\"object2\":{\"A\":\"a\",\"B\":\"b\"}}}");
	}
	
	@Test
	public void testMiddleWildcardFilter() throws IOException {
		FieldsExpressionNode.Builder root = FieldsExpressionNode.Builder.ofBranch("nestedObjects", "*", "B");
		newJsonFilteringCopier(jp, jg, root).copyAndClose();
		checkResult("{\"nestedObjects\":{\"object1\":{\"B\":\"b\"},\"object2\":{\"B\":\"b\"}}}");
	}
	
	@Test
	public void testArrayOfObjects() throws IOException {
		FieldsExpressionNode.Builder root = FieldsExpressionNode.Builder.ofBranch("arrayOfObjects", "B");
		newJsonFilteringCopier(jp, jg, root).copyAndClose();
		checkResult("{\"arrayOfObjects\":[{\"B\":\"b\"}]}");
	}
	
	@Test
	public void testFilterMerging() throws IOException {
		FieldsExpressionNode.Builder root = FieldsExpressionNode.Builder.ofBranch("object", "A");
		root.getOrAddBranch(ImmutableList.of("object"));
		newJsonFilteringCopier(jp, jg, root).copyAndClose();
		checkResult("{\"object\":{\"A\":\"a\"}}");
	}
	
	@Test
	public void testWildcardPrecedence() throws IOException {
		FieldsExpressionNode.Builder root = FieldsExpressionNode.Builder.ofBranch("object", "A");
		root.getOrAddBranch(ImmutableList.of("object", "*"));
		newJsonFilteringCopier(jp, jg, root).copyAndClose();
		checkResult("{\"object\":{\"A\":\"a\",\"B\":\"b\"}}");
	}
	
	@Test
	public void testLeadingWildcard() throws IOException {
		FieldsExpressionNode.Builder root = FieldsExpressionNode.Builder.ofBranch("*", "object2", "B");
		newJsonFilteringCopier(jp, jg, root).copyAndClose();
		checkResult("{\"id\":1,\"text\":\"string\",\"boolean\":true,\"float\":1.3,\"nullable\":null,\"array\":[1,2,3],\"nestedObjects\":{\"object2\":{\"B\":\"b\"}},\"arrayOfArrays\":[[1,2],[3,4],[5,6]]}");
	}
	
	private void checkResult(URL expected) throws IOException {
		String expectedJson = Resources.toString(expected, StandardCharsets.UTF_8);
		checkResult(expectedJson);
	}
	
	private void checkResult(String expectedJson) {
		String outputJson = output.toString();
		Assert.assertEquals(
				expectedJson.replaceAll("\\s", ""),
				outputJson.replaceAll("\\s", "")
		);
	}
	
	////// Theoritical tests that can't happen if property names are validated //////
	
	@Test
	public void testSecondLevelNoMatchFilter() throws IOException {
		FieldsExpressionNode.Builder root = FieldsExpressionNode.Builder.ofBranch("object", "C");
		newJsonFilteringCopier(jp, jg, root).copyAndClose();
		checkResult("{}");
	}
	
	@Test
	public void testLongerFilter() throws IOException {
		FieldsExpressionNode.Builder root = FieldsExpressionNode.Builder.ofBranch("object", "A", "something");
		newJsonFilteringCopier(jp, jg, root).copyAndClose();
		checkResult("{}");
	}
	
	@Test
	public void testLongerFilterWithNested() throws IOException {
		FieldsExpressionNode.Builder root = FieldsExpressionNode.Builder.ofBranch("nestedObjects", "object1", "something");
		newJsonFilteringCopier(jp, jg, root).copyAndClose();
		checkResult("{}");
	}
	
}
