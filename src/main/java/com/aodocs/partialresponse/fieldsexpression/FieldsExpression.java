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

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Describes a fields expression for partial response, as described there: 
 * https://developers.google.com/drive/api/v3/performance#partial-response
 */
public final class FieldsExpression {
	
	public static FieldsExpression parse(String fieldsExpression) {
		return new FieldsExpression(fieldsExpression);
	}
	
	private final String fieldsExpression;
	private final List<ImmutableList<String>> allPaths;
	private final List<ImmutableList<String>> collapsedPaths;
	private final FieldsExpressionTree tree;
	
	private FieldsExpression(String fieldsExpression) {
		this.fieldsExpression = fieldsExpression;
		this.allPaths = Parser.parse(fieldsExpression);
		FieldsExpressionNode.Builder builder = FieldsExpressionNode.Builder.createRoot();
		this.collapsedPaths = collapsePaths(allPaths)
				.peek(builder::getOrAddBranch).collect(Collectors.toList());
		this.tree = new FieldsExpressionTree(builder.getNode());
	}

	public List<ImmutableList<String>> getFieldPaths() {
		return collapsedPaths;
	}

	public FieldsExpressionTree getFilterTree() {
		return tree;
	}

	public boolean isValidAgainst(FieldsExpressionTree schema) {
		//we check validity with all paths, as collapsePaths might have removed invalid paths
		return allPaths.stream()
				.map(path -> createTreeFromPath(path))
				.allMatch(root -> schema.contains(root));
	}

	public boolean contains(FieldsExpressionTree testedTree) {
		return collapsedPaths.stream()
				.map(path -> createTreeFromPath(path))
				.anyMatch(root -> root.contains(testedTree) || testedTree.contains(root));
	}

	private static FieldsExpressionTree createTreeFromPath(ImmutableList<String> path) {
		FieldsExpressionNode.Builder builder = FieldsExpressionNode.Builder.createRoot();
		builder.getOrAddBranch(path);
		return new FieldsExpressionTree(builder.getNode());
	}

	/**
	 * A path p1 is "dead" if there another path p2 with a terminal wildcard that is a prefix of p1.
	 * Only live paths are retained in the result.
	 *
	 * @param paths all paths
	 * @return the collpased paths
	 */
	private Stream<ImmutableList<String>> collapsePaths(List<ImmutableList<String>> paths) {
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
		return fieldsExpression;
	}
}
