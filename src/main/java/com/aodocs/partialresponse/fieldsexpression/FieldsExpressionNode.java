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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ForwardingCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Describes a fields expression "node".
 */
public class FieldsExpressionNode {
	
	private static final String WILDCARD_VALUE = "*";
	private static final String CATCH_ALL_VALUE = "**";
	
	private final String value;
	//uses an iterable to allow lazy evaluation to handle cycles
	private Iterable<FieldsExpressionNode> children = Collections.emptySet();
	
	private FieldsExpressionNode(String value) {
		this.value = value;
	}
	
	/***
	 * @return current node's children indexed by their name
	 */
	public ImmutableMap<String, FieldsExpressionNode> getChildMap() {
		return Maps.uniqueIndex(children, new Function<FieldsExpressionNode, String>() {
			@Override
			public String apply(FieldsExpressionNode input) {
				return input.value;
			}
		});
	}
	
	public boolean isRoot() {
		return value == null;
	}
	
	public boolean isLeaf() {
		return Iterables.isEmpty(children);
	}
	
	/**
	 * @return true if this node is either :
	 * - a direct leaf
	 * - or a transitive leaf (ie: has a wildcard+transitiveLeaf child)
	 */
	public boolean isTransitiveLeaf() {
		return isLeaf() || FluentIterable.from(children).anyMatch(new Predicate<FieldsExpressionNode>() {
			@Override
			public boolean apply(@NullableDecl FieldsExpressionNode child) {
				return child.isWildcard() && child.isTransitiveLeaf();
			}
		});
	}
	
	public boolean isWildcard() {
		return WILDCARD_VALUE.equals(value);
	}
	
	public boolean isCatchAll() {
		return CATCH_ALL_VALUE.equals(value);
	}
	
	public boolean matches(String otherValue) {
		return isWildcard() || value.equals(otherValue);
	}
	
	/**
	 * @param other another node
	 * @return true if this node contains the other node fully
	 */
	boolean checkContainment(FieldsExpressionNode other) {
		if (isCatchAll()) {
			return true; //short circuit
		}
		if (Objects.equals(this.value, other.value) || isWildcard() || other.isWildcard()) {
			return Iterables.all(other.children, new Predicate<FieldsExpressionNode>() {
				@Override
				public boolean apply(final FieldsExpressionNode otherChild) {
					return Iterables.any(children, new Predicate<FieldsExpressionNode>() {
						@Override
						public boolean apply(FieldsExpressionNode ownChild) {
							return ownChild.checkContainment(otherChild);
						}
					});
				}
			});
		}
		return false;
	}
	
	/**
	 * Helper to traverse a tree
	 *
	 * @param walker
	 */
	void walk(TreeWalker walker) {
		walk(walker, 0);
	}
	
	private void walk(TreeWalker walker, int depth) {
		walker.walkNode(value, depth);
		for (FieldsExpressionNode child : children) {
			child.walk(walker, depth + 1);
		}
	}
	
	public interface TreeWalker {
		void walkNode(String value, int depth);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof FieldsExpressionNode))
			return false;
		FieldsExpressionNode that = (FieldsExpressionNode) o;
		return Objects.equals(value, that.value) &&
				Objects.equals(getChildMap(), that.getChildMap());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(value, getChildMap());
	}
	
	/**
	 * Mutable implementation of a FilteringNode, used for building fields expression trees.
	 */
	public static class Builder {
		
		/**
		 * Creates a new empty root node.
		 *
		 * @return an empty root node
		 */
		public static Builder createRoot() {
			return new Builder();
		}
		
		/**
		 * Creates a new root node with the provided children.
		 * Example: ofChildren("a", "b", "c") ⇒ root → a, b, c
		 *
		 * @param childValues
		 * @return the root node containing the children
		 */
		public static Builder withChildren(String... childValues) {
			Builder builder = new Builder();
			for (String value : childValues) {
				builder.getOrAddChild(value);
			}
			return builder;
		}
		
		/**
		 * Creates a new root node with the provided "branch".
		 * Example: ofBranch("a", "b", "c") ⇒ root → a → b → c
		 *
		 * @param branchValues
		 * @return the root node containing the branch
		 */
		public static Builder ofBranch(String... branchValues) {
			Builder builder = new Builder();
			builder.getOrAddBranch(Arrays.asList(branchValues));
			return builder;
		}
		
		private final FieldsExpressionNode node;
		
		private Builder() {
			node = new FieldsExpressionNode(null);
		}
		
		private Builder(FieldsExpressionNode node) {
			this.node = node;
		}
		
		public FieldsExpressionNode getNode() {
			return node;
		}
		
		/**
		 * If the child exists, returns the existing child node, or create a new one.
		 *
		 * @param childValue the value of the child node to get or add
		 * @return the child builder
		 */
		public Builder getOrAddChild(String childValue) {
			FieldsExpressionNode child = node.getChildMap().get(childValue);
			if (child == null) {
				child = new FieldsExpressionNode(childValue);
				node.children = Iterables.concat(node.children, Collections.singleton(child));
			}
			return new Builder(child);
		}
		
		/**
		 * Creates the missing branch values if needed, or reuses the existing ones.
		 *
		 * @param branchValues the branch values to add
		 * @return the leaf node
		 */
		public Builder getOrAddBranch(Iterable<String> branchValues) {
			Builder current = this;
			for (String value : branchValues) {
				current = current.getOrAddChild(value);
			}
			return current;
		}
		
		/**
		 * Clears existing children and sets the only child as the catch-all node.
		 *
		 * @return the child catch-all node
		 */
		public Builder setCatchAllChild() {
			FieldsExpressionNode catchAllNode = new FieldsExpressionNode(CATCH_ALL_VALUE);
			node.children = Collections.singleton(catchAllNode);
			return new Builder(catchAllNode);
		}
		
		/**
		 * Merges another root node.
		 *
		 * @param other the other node to merge
		 */
		public void merge(final FieldsExpressionNode other) {
			String value = other.value;
			Preconditions.checkArgument(value == null, "Only root nodes can be merged");
			Sets.SetView<String> sameNodes = Sets.intersection(node.getChildMap().keySet(), other.getChildMap().keySet());
			Preconditions.checkState(sameNodes.isEmpty(), "Duplicate children %s", sameNodes);
			node.children = Iterables.concat(node.children, lazyChildIterable(other));
		}
		
		/**
		 * Wraps the children iterable in a forwarding collection, so it is evaluated lazily.
		 * This is necessary to handle cycles properly in the tree.
		 */
		private static Iterable<FieldsExpressionNode> lazyChildIterable(final FieldsExpressionNode node) {
			return new ForwardingCollection<FieldsExpressionNode>() {
				@Override
				protected Collection<FieldsExpressionNode> delegate() {
					return Lists.newArrayList(node.children);
				}
			};
		}
		
	}
}
