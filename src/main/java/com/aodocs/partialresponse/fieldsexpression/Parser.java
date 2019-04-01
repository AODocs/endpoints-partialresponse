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

import java.util.Collections;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import com.aodocs.partialresponse.fieldsexpression.parser.FieldsExpressionBaseVisitor;
import com.aodocs.partialresponse.fieldsexpression.parser.FieldsExpressionLexer;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * Generates a FilteringNode (root of a filtering tree) based on the parsing of a fields expression string.
 */
class Parser {
	
	private final String fieldsExpression;
	
	private Parser(String fieldsExpression) {
		this.fieldsExpression = fieldsExpression;
	}
	
	static FluentIterable<ImmutableList<String>> parse(String fieldsExpression) {
		return new Parser(fieldsExpression).parse();
	}
	
	private FluentIterable<ImmutableList<String>> parse() {
		
		//init Antlr
		FieldsExpressionLexer lexer = new FieldsExpressionLexer(CharStreams.fromString(fieldsExpression));
		com.aodocs.partialresponse.fieldsexpression.parser.FieldsExpressionParser parser = new com.aodocs.partialresponse.fieldsexpression.parser.FieldsExpressionParser(new BufferedTokenStream(lexer));
		parser.setErrorHandler(new BailErrorStrategy()); //throws ParseCancellationException
		
		//parse expression and handle errors
		com.aodocs.partialresponse.fieldsexpression.parser.FieldsExpressionParser.ExpressionContext expression;
		try {
			expression = parser.expression();
		} catch (RecognitionException e) {
			throw new FieldExpressionParsingException(fieldsExpression, e);
		} catch (ParseCancellationException e) {
			throw new FieldExpressionParsingException(fieldsExpression, e);
		}
		
		//explode expression to individual paths
		return sanitizePaths(new PathExplodingVisitor().visit(expression));
	}
	
	/**
	 * Ending wildcards are not needed : fieldA, fieldA / *, fieldA / * / * are equal
	 *
	 * @param paths
	 * @return sanitized paths
	 */
	private FluentIterable<ImmutableList<String>> sanitizePaths(final FluentIterable<ImmutableList<String>> paths) {
		return paths.transform(new Function<ImmutableList<String>, ImmutableList<String>>() {
			@Override
			public ImmutableList<String> apply(ImmutableList<String> paths) {
				return removeEndingWildcards(paths);
			}
		});
	}
	
	private ImmutableList<String> removeEndingWildcards(ImmutableList<String> pathItems) {
		if (pathItems.size() > 1 && Iterables.getLast(pathItems).equals("*")) {
			return removeEndingWildcards(pathItems.subList(0, pathItems.size() - 1));
		}
		return pathItems;
	}
	
	/**
	 * This visitor will create a list of possible paths from a fields expression.
	 */
	private class PathExplodingVisitor extends FieldsExpressionBaseVisitor<FluentIterable<ImmutableList<String>>> {
		@Override
		public FluentIterable<ImmutableList<String>> visitSelection(com.aodocs.partialresponse.fieldsexpression.parser.FieldsExpressionParser.SelectionContext ctx) {
			final FluentIterable<String> selectionPath = FluentIterable.from(ctx.FIELDNAME()).transform(toStringFunction());
			FluentIterable<ImmutableList<String>> childrenPaths = super.visitSelection(ctx);
			if (childrenPaths.isEmpty()) {
				return FluentIterable.from(Collections.singleton(selectionPath.toList()));
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
	
	@Override
	public String toString() {
		return fieldsExpression;
	}
}
