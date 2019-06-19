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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

/**
 * Describes a fields expression for partial response, as described there: https://developers.google.com/drive/api/v3/performance#partial-response
 */
public final class FieldsExpression {
	
	public static FieldsExpression parse(String fieldsExpression) {
		return new FieldsExpression(fieldsExpression);
	}
	
	private final String fieldExpression;
	private final List<ImmutableList<String>> fieldsExpressionPaths;
	
	private FieldsExpression(String fieldsExpression) {
		this.fieldExpression = fieldsExpression;
		this.fieldsExpressionPaths = Parser.parse(fieldsExpression);
	}
	
	public boolean isValidAgainst(FieldsExpressionTree schema) {
		return asPathNodes().allMatch(root -> schema.contains(new FieldsExpressionTree(root)));
	}
	
	private Stream<FieldsExpressionNode> asPathNodes() {
		return fieldsExpressionPaths.stream().map(path -> {
			FieldsExpressionNode.Builder builder = FieldsExpressionNode.Builder.createRoot();
			builder.getOrAddBranch(path);
			return builder.getNode();
		});
	}
	
	public FieldsExpressionTree getFilterTree() {
		FieldsExpressionNode.Builder builder = FieldsExpressionNode.Builder.createRoot();
		retainLivePaths(fieldsExpressionPaths).forEach(builder::getOrAddBranch);
		return new FieldsExpressionTree(builder.getNode());
	}
	
	/**
	 * A path p1 is dead if there another path p2 with a terminal wildcard that is a prefix of p1.
	 * Only live paths are retained.
	 *
	 * @param paths
	 * @return only the live paths
	 */
	private Stream<ImmutableList<String>> retainLivePaths(List<ImmutableList<String>> paths) {
		List<ImmutableList<String>> redundantPaths = new ArrayList<>();
		for (int pathIndex = 0; pathIndex < paths.size() - 1; pathIndex++) {
			ImmutableList<String> path = paths.get(pathIndex);
			for (int anotherPathIndex = pathIndex + 1; anotherPathIndex < paths.size(); anotherPathIndex++) {
				ImmutableList<String> anotherPath = paths.get(anotherPathIndex);
				boolean pathIsRedundant = path.size() > anotherPath.size() && path.subList(0, anotherPath.size()).equals(anotherPath);
				boolean anotherPathIsRedundant = anotherPath.size() > path.size() && anotherPath.subList(0, path.size()).equals(path);
				if (pathIsRedundant) {
					redundantPaths.add(path);
				} else if (anotherPathIsRedundant) {
					redundantPaths.add(anotherPath);
				}
			}
		}
		return paths.stream().filter(path -> !redundantPaths.contains(path));
	}
	
	@Override
	public String toString() {
		return fieldExpression;
	}
}
