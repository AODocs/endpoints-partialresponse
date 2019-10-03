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
