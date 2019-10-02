package com.aodocs.partialresponse.servlet;

import static org.junit.Assert.assertEquals;

import com.aodocs.partialresponse.fieldsexpression.FieldsExpression;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class RetainedFieldCheckerImplTest {

  private final String fieldsExpression;
  private final String toCheck;
  private final boolean expected;

  @Parameterized.Parameters(name = "{0},{1}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        //no need to check *, as everything matches
        //simple
        {"foo", "foo", true},
        {"foo", "fooo", false},
        {"foo", "bar", false},
        {"foo", "foo/bar", true},
        {"foo", "foo/baz", true},
        {"foo", "fooo/bar", false},
        {"foo/bar", "foo", true},
        {"foo/bar", "bar", false},
        {"foo/bar", "foo/bar", true},
        {"foo/bar", "foo/baz", false},
        {"foo/bar", "foo/bar/baz", true},
        //multiple paths
        {"foo,bar", "foo", true},
        {"foo,bar", "bar", true},
        {"foo,bar", "baz", false},
        {"foo(bar,baz)", "foo", true},
        {"foo(bar,baz)", "foo/bar", true},
        {"foo(bar,baz)", "foo/baz", true},
        {"foo(bar,baz)", "foo/foo", false},
        //terminal wildcards
        {"foo/*", "foo", true},
        {"foo/*", "bar", false},
        {"foo/*", "fooo", false},
        {"foo/*", "foo/bar", true},
        //intermediate wildcards
        {"foo/*/bar", "foo", true},
        {"foo/*/bar", "fooo", false},
        {"foo/*/bar", "bar", false},
        {"foo/*/bar", "foo/bar", true},
        {"foo/*/bar", "foo/baz/bar", true},
        {"foo/*/bar", "foo/baz/baz/bar", true},
        {"foo/*/bar", "foo/baz/bar/baz", true},
        //all combined
        {"foo,bar/*,baz/*/baz", "foo", true},
        {"foo,bar/*,baz/*/baz", "fooo", false},
        {"foo,bar/*,baz/*/baz", "bar", true},
        {"foo,bar/*,baz/*/baz", "bar/foo", true},
        {"foo,bar/*,baz/*/baz", "baz", true},
        {"foo,bar/*,baz/*/baz", "baz/foo", true},
        {"foo,bar/*,baz/*/baz", "baz/foo/bar", true},
    });
  }

  public RetainedFieldCheckerImplTest(String fieldsExpression, String toCheck,
      boolean expected) {
    this.fieldsExpression = fieldsExpression;
    this.toCheck = toCheck;
    this.expected = expected;
  }

  @Test
  public void isIncludedInPartialResponse() {
    FieldsExpression fieldsExpression = FieldsExpression.parse(this.fieldsExpression);
    boolean retained = new RetainedFieldCheckerImpl(fieldsExpression).isRetained(toCheck);
    assertEquals(expected, retained);
  }

}
