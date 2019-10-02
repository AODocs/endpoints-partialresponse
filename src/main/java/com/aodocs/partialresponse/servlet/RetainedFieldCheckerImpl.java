package com.aodocs.partialresponse.servlet;

import com.aodocs.partialresponse.fieldsexpression.FieldsExpression;
import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
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
    List<String> toCheck = SPLITTER.splitToList(fieldPath);
    return fieldsExpression.getFieldPaths().stream()
        .anyMatch(path -> checkCommonPrefix(path, toCheck));
  }

  private boolean checkCommonPrefix(ImmutableList<String> expressionPath, List<String> toCheck) {
    int aIndex = 0;
    int bIndex = 0;
    while (aIndex < expressionPath.size() && bIndex < toCheck.size()) {
      String a = expressionPath.get(aIndex++);
      String b = toCheck.get(bIndex++);
      //as intermediate wildcards might check multiple levels at once, we return true
      if ("*".equals(a))
        return true;
      if (!a.equals(b))
        return false;
    }
    return true;
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
