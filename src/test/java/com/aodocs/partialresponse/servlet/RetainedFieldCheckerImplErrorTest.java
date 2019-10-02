package com.aodocs.partialresponse.servlet;

import com.aodocs.partialresponse.fieldsexpression.FieldsExpression;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class RetainedFieldCheckerImplErrorTest {

  private static final RetainedFieldCheckerImpl CHECKER 
      = new RetainedFieldCheckerImpl(FieldsExpression.parse("foo"));

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {null},
        {""},
        {"  "},
        {"/foo"},
        {"foo/"},
        {"foo//bar"},
        {"*"},
        {"foo/*"},
        {"foo/*/bar"},
    });
  }

  private final String toCheck;

  public RetainedFieldCheckerImplErrorTest(String toCheck) {
    this.toCheck = toCheck;
  }
  
  @Test(expected = IllegalArgumentException.class)
  public void nullFieldPath() {
    CHECKER.isRetained(toCheck);
  }

}
