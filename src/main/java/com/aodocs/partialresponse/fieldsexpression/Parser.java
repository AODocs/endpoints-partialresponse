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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import com.aodocs.partialresponse.fieldsexpression.parser.FieldsExpressionBaseVisitor;
import com.aodocs.partialresponse.fieldsexpression.parser.FieldsExpressionLexer;
import com.aodocs.partialresponse.fieldsexpression.parser.FieldsExpressionParser;
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
	
	static List<ImmutableList<String>> parse(String fieldsExpression) {
		return new Parser(fieldsExpression).parse();
	}
	
	private List<ImmutableList<String>> parse() {
		
		//init Antlr
		FieldsExpressionLexer lexer = new FieldsExpressionLexer(CharStreams.fromString(fieldsExpression));
		FieldsExpressionParser parser = new FieldsExpressionParser(new BufferedTokenStream(lexer));
		parser.setErrorHandler(new BailErrorStrategy()); //throws ParseCancellationException
		
		//parse expression and handle errors
		FieldsExpressionParser.ExpressionContext expression;
		try {
			expression = parser.expression();
		} catch (RecognitionException e) {
			throw new FieldExpressionParsingException(fieldsExpression, e);
		} catch (ParseCancellationException e) {
			throw new FieldExpressionParsingException(fieldsExpression, e);
		}
		
		//explode expression to individual paths
		return sanitizePaths(new PathExplodingVisitor().visit(expression)).collect(Collectors.toList());
	}
	
	/**
	 * Ending wildcards are not needed : fieldA, fieldA / *, fieldA / * / * are equal
	 *
	 * @param paths
	 * @return sanitized paths
	 */
	private Stream<ImmutableList<String>> sanitizePaths(Stream<ImmutableList<String>> paths) {
		return paths.map(this::removeEndingWildcards);
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
	private class PathExplodingVisitor extends FieldsExpressionBaseVisitor<Stream<ImmutableList<String>>> {
		@Override
		public Stream<ImmutableList<String>> visitSelection(FieldsExpressionParser.SelectionContext ctx) {
			ImmutableList<String> selectionPath = ctx.FIELDNAME().stream().map(toStringFunction()).collect(ImmutableList.toImmutableList());
			List<ImmutableList<String>> childrenPaths = super.visitSelection(ctx).collect(Collectors.toList());
			if (childrenPaths.isEmpty()) {
				return Stream.of(selectionPath);
			}
			return childrenPaths.stream()
					.map(childPath -> Stream.concat(selectionPath.stream(), childPath.stream())
					.collect(ImmutableList.toImmutableList()));
		}
		
		@Override
		protected Stream<ImmutableList<String>> aggregateResult(
				Stream<ImmutableList<String>> aggregate, Stream<ImmutableList<String>> nextResult) {
			return Stream.concat(nullToEmpty(aggregate), nullToEmpty(nextResult));
		}
		
		private Stream<ImmutableList<String>> nullToEmpty(Stream<ImmutableList<String>> aggregate) {
			return firstNonNull(aggregate, Stream.<ImmutableList<String>>of());
		}
	}
	
	@Override
	public String toString() {
		return fieldsExpression;
	}
}
