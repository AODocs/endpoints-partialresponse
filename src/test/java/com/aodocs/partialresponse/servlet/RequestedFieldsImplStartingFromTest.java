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
public class RequestedFieldsImplStartingFromTest {

  @Parameterized.Parameters(name = "{0} - {1} + {2} => {3}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {"foo", "foo", "bar", true},
        {"foo", "bar", "foo", false},
        {"foo", "bar", "bar", false},
        {"foo/bar", "foo", "bar", true},
        {"foo/bar", "foo", "baz", false},
        {"foo/bar/baz", "foo/bar", "baz", true},
        {"foo/bar/baz", "foo/bar", "bar", false},
        //canonical usage
        {"items/fieldName", "items", "fieldName", true},
    });
  }

  private final String fieldsExpression;
  private final String newRoot;
  private final String fieldPathToCheck;
  private final boolean expectedIsRequested;

  public RequestedFieldsImplStartingFromTest(String fieldsExpression, String newRootPath,
      String fieldPathToCheck, boolean expectedIsRequested) {
    this.fieldsExpression = fieldsExpression;
    this.newRoot = newRootPath;
    this.fieldPathToCheck = fieldPathToCheck;
    this.expectedIsRequested = expectedIsRequested;
  }

  @Test
  public void startsWithAndIsRequested() {
    FieldsExpression fieldsExpression = FieldsExpression.parse(this.fieldsExpression);
    boolean isRequested = new RequestedFieldsImpl(fieldsExpression).startingFrom(newRoot)
        .isRequested(fieldPathToCheck);
    assertEquals(expectedIsRequested, isRequested);
  }

}
