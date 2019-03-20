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

import java.util.Objects;

import com.google.common.base.Strings;

/**
 * Describes a fields expression tree for partial response
 */
public final class FieldsExpressionTree {
	
	private final FieldsExpressionNode root;
	
	public FieldsExpressionTree(FieldsExpressionNode root) {
		this.root = root;
	}
	
	public FieldsExpressionNode getRoot() {
		return root;
	}
	
	/**
	 * Pretty prints of a tree with newlines and indent
	 *
	 * @return the pretty printed tree
	 */
	public String prettyPrint() {
		final StringBuilder output = new StringBuilder(getClass().getSimpleName() + "{\n");
		root.walk(new FieldsExpressionNode.TreeWalker() {
			@Override
			public void walkNode(String value, int depth) {
				if (depth > 0) {
					output.append(Strings.repeat("-", depth - 1) + value + "\n");
				}
			}
		});
		output.append('}');
		return output.toString();
	}
	
	/**
	 * Checks if the current tree "contains" other tree (contains all its nodes).
	 *
	 * @param otherTree another tree
	 * @return true if the current tree contains otherTree
	 */
	public boolean contains(FieldsExpressionTree otherTree) {
		return root.checkContainment(otherTree.root);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		FieldsExpressionTree that = (FieldsExpressionTree) o;
		return Objects.equals(root, that.root);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(root);
	}
	
}
