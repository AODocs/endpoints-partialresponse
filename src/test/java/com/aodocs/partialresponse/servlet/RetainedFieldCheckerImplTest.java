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
