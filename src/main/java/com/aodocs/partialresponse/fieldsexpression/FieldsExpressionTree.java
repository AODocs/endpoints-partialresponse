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

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.base.MoreObjects.firstNonNull;

import java.util.List;
import java.util.Objects;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import com.aodocs.partialresponse.fieldsexpression.parser.FieldsExpressionBaseVisitor;
import com.aodocs.partialresponse.fieldsexpression.parser.FieldsExpressionLexer;
import com.aodocs.partialresponse.fieldsexpression.parser.FieldsExpressionParser;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Describes a fields expression for partial response, as described there: https://developers.google.com/drive/api/v3/performance#partial-response
 */
public final class FieldsExpressionTree {
	
	public static FieldsExpressionTree parse(String fieldsExpression) {
		return new FieldsExpressionTree(new Parser(fieldsExpression).root);
	}
	
	private final FieldsExpressionNode root;
	
	public FieldsExpressionTree(FieldsExpressionNode root) {
		Preconditions.checkArgument(root.isRoot(),
				"Only oot nodes can be accepted as root of FieldsExpressionTree");
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
	 * Checks if the current tree "contains" other tree (contains all his nodes).
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
	
	/**
	 * Generates a FilteringNode (root aof a filtering tree) based on the parsing of a fields expression string.
	 */
	private static class Parser {
		
		private final String fieldsExpression; //for debug
		private final FieldsExpressionLexer lexer;
		private final FieldsExpressionParser parser;
		private final FieldsExpressionNode root;
		
		private Parser(String fieldsExpression) {
			this.fieldsExpression = fieldsExpression;
			
			//init Antlr
			this.lexer = new FieldsExpressionLexer(CharStreams.fromString(fieldsExpression));
			this.parser = new FieldsExpressionParser(new BufferedTokenStream(lexer));
			this.parser.setErrorHandler(new BailErrorStrategy()); //throws ParseCancellationException
			
			//parse expression and handle errors
			FieldsExpressionParser.ExpressionContext expression;
			try {
				expression = this.parser.expression();
			} catch (RecognitionException e) {
				throw new FieldExpressionParsingException(fieldsExpression, e);
			} catch (ParseCancellationException e) {
				throw new FieldExpressionParsingException(fieldsExpression, e);
			}
			
			//explode expression to individual paths and retain only live paths
			FluentIterable<ImmutableList<String>> allPaths = new PathExplodingVisitor().visit(expression);
			FluentIterable<ImmutableList<String>> livePaths = retainLivePaths(allPaths);
			
			FieldsExpressionNode.Builder builder = FieldsExpressionNode.Builder.createRoot();
			for (ImmutableList<String> livePath : livePaths) {
				builder.getOrAddBranch(livePath);
			}
			this.root = builder.getNode();
		}
		
		/**
		 * A path p1 is dead if there another path p2 with a terminal wildcard that is a prefix of p1.
		 * Only live paths are retained.
		 *
		 * @param paths
		 * @return only the live paths
		 */
		private FluentIterable<ImmutableList<String>> retainLivePaths(FluentIterable<ImmutableList<String>> paths) {
			final FluentIterable<ImmutableList<String>> terminalWildcardPaths = paths
					.filter(new Predicate<ImmutableList<String>>() {
						@Override
						public boolean apply(ImmutableList<String> strings) {
							return Iterables.getLast(strings).equals("*");
						}
					});
			return paths.filter(new Predicate<ImmutableList<String>>() {
				@Override
				public boolean apply(final ImmutableList<String> path) {
					return !terminalWildcardPaths.anyMatch(new Predicate<List<String>>() {
						@Override
						public boolean apply(List<String> terminalWildcardPath) {
							return path.size() > terminalWildcardPath.size()
									&& path.subList(0, terminalWildcardPath.size()).equals(terminalWildcardPath);
						}
					});
				}
			});
		}
		
		/**
		 * This visitor will create a list of possible paths from a fields expression.
		 */
		private class PathExplodingVisitor extends FieldsExpressionBaseVisitor<FluentIterable<ImmutableList<String>>> {
			@Override
			public FluentIterable<ImmutableList<String>> visitSelection(FieldsExpressionParser.SelectionContext ctx) {
				final FluentIterable<String> selectionPath = FluentIterable.from(ctx.FIELDNAME()).transform(toStringFunction());
				FluentIterable<ImmutableList<String>> childrenPaths = super.visitSelection(ctx);
				if (childrenPaths.isEmpty()) {
					return FluentIterable.of(selectionPath.toList());
				}
				return childrenPaths.transform(new Function<ImmutableList<String>, ImmutableList<String>>() {
					@Override
					public ImmutableList<String> apply(ImmutableList<String> childPath) {
						return selectionPath.append(childPath).toList();
					}
				});
			}
			
			@Override
			protected FluentIterable<ImmutableList<String>> aggregateResult(
					FluentIterable<ImmutableList<String>> aggregate, FluentIterable<ImmutableList<String>> nextResult) {
				return nullToEmpty(aggregate).append(nullToEmpty(nextResult));
			}
			
			private FluentIterable<ImmutableList<String>> nullToEmpty(FluentIterable<ImmutableList<String>> aggregate) {
				return firstNonNull(aggregate, FluentIterable.<ImmutableList<String>>of());
			}
		}
	}
}
