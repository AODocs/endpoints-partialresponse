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

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.aodocs.partialresponse.fieldsexpression.FieldsExpressionNode;
import com.aodocs.partialresponse.fieldsexpression.FieldsExpressionTree;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.filter.TokenFilter;
import com.google.common.base.Preconditions;

/**
 * Wraps a JsonGenerator to add filtering of the JSON output
 * according to a provided FieldsExpressionTree filter.
 */
class PartialResponseJsonGenerator extends AbstractFilteringGenerator {
	
	PartialResponseJsonGenerator(JsonGenerator delegate, FieldsExpressionTree filterTree) {
		super(delegate, new FilteringNodeTokenFilter(
				Preconditions.checkNotNull(filterTree, "filterTree cannot be null").getRoot()),
				true, true);
	}
	
	/**
	 * {@link TokenFilter} implementation that takes a {@link FieldsExpressionNode} and filters properties accordingly.
	 */
	static class FilteringNodeTokenFilter extends TokenFilter {
		
		private final FieldsExpressionNode filterContext;
		
		FilteringNodeTokenFilter(FieldsExpressionNode filterContext) {
			this.filterContext = filterContext;
		}
		
		/**
		 * Include a property only if it matches the filterContext:
		 * - if the filterContext is a leaf, we include the property
		 * - elsif the filterContext has some children matching the name (can be wildcard), we derive a new TokenFilter from them
		 * - otherwise we exclude the property
		 *
		 * @param name field name
		 * @return the new TokenFilter
		 */
		@Override
		public TokenFilter includeProperty(String name) {
			//leafs match anything below
			if (filterContext.isTransitiveLeaf()) {
				return TokenFilter.INCLUDE_ALL;
			}
			//build a new TokenFilter from matching children (including wildcards)
			return buildTokenFilter(filterContext.getChildMap().values().stream().filter(node -> node.matches(name)).map(FilteringNodeTokenFilter::new).collect(Collectors.toSet()));
		}
		
		private TokenFilter buildTokenFilter(Set<? extends TokenFilter> subFilters) {
			if (subFilters.isEmpty()) {
				return null;
			}
			if (subFilters.size() == 1) {
				return subFilters.iterator().next();
			}
			return new MultiTokenFilter(subFilters);
		}
		
		/**
		 * Include values only if :
		 * - there are no more filters to be applied downstream
		 * - it is middle wildcard
		 *
		 * @return true if it is a leaf
		 */
		@Override
		protected boolean _includeScalar() {
			return filterContext.isWildcard() || filterContext.isTransitiveLeaf();
		}
		
		/**
		 * {@link TokenFilter} implementation that combines multiple token filters.
		 */
		private class MultiTokenFilter extends TokenFilter {
			
			private Set<? extends TokenFilter> filters;
			
			private MultiTokenFilter(Set<? extends TokenFilter> filters) {
				this.filters = filters;
			}
			
			@Override
			public TokenFilter includeProperty(String name) {
				return buildTokenFilter(filters.stream().map(tokenFilter -> tokenFilter.includeProperty(name)).filter(Objects::nonNull).collect(Collectors.toSet()));
			}
			
		}
	}
	
}
