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
package com.aodocs.partialresponse.servlet;

import com.aodocs.partialresponse.fieldsexpression.FieldsExpression;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

class RetainedFieldCheckerImpl implements RetainedFieldChecker {

  private final static CharMatcher SLASH_MATCHER = CharMatcher.is('/');
  private final static Splitter SPLITTER = Splitter.on(SLASH_MATCHER).omitEmptyStrings();
  
  private final FieldsExpression fieldsExpression;

  public RetainedFieldCheckerImpl(FieldsExpression fieldsExpression) {
    Preconditions.checkArgument(!fieldsExpression.getFilterTree().getRoot().isWildcard(),
        "PartialResponseFieldsChecker must not be used on wildcard expressions");
    this.fieldsExpression = fieldsExpression;
  }

  @Override
  public boolean isRetained(String fieldPath) {
    checkFieldPath(fieldPath);
    FieldsExpression testedExpression = FieldsExpression.parse(fieldPath);
    return fieldsExpression.contains(testedExpression.getFilterTree());
  }

  @Override
  public RetainedFieldChecker startingFrom(String newRootPath) {
    checkFieldPath(newRootPath);
    return new RetainedFieldChecker() {
      @Override
      public boolean isRetained(String fieldPath) {
        return RetainedFieldCheckerImpl.this.isRetained(
            newRootPath + "/" + fieldPath);
      }

      @Override
      public RetainedFieldChecker startingFrom(String newRootPath2) {
        return RetainedFieldCheckerImpl.this.startingFrom(
            newRootPath + "/" + newRootPath2);
      }
    };
  }

  private void checkFieldPath(String fieldPath) {
    Preconditions.checkArgument(!StringUtils.isBlank(fieldPath), 
        "fieldPath must be a non-blank string");
    Preconditions.checkArgument(!fieldPath.contains("*"),
        "fieldPath must not contain wildcards");
    String normalized = SLASH_MATCHER.trimAndCollapseFrom(fieldPath, '/');
    Preconditions.checkArgument(normalized.equals(fieldPath),
        "fieldPath not start with, end with or have repeated '/' chars");
  }
  
}
