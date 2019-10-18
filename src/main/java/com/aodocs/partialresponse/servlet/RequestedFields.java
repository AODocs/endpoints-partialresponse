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

/**
 * Given a field "path", this instance will check if the corresponding field is requested to be 
 * included in the partial response.
 * A "requested field" does not mean it will be present in the partial response, it just means it
 * should not be filtered out if present in the unfiltered response.
 * The validity of the fieldPaths is not checked by these methods against the schema of the resource.
 */
public interface RequestedFields {

  /**
   * Indicates whether the field denoted by the given path is requested in the partial response.
   * The path separator is "/" (same as a fields expression). Wildcards are not accepted.
   * 
   * @param fieldPath the path of the field to check
   * @return if the field is requested in the partial response
   */
  boolean isRequested(String fieldPath);

  /**
   * Returns a new instance with a new root to check requested fields against.
   * This is useful to perform similar checks on list / get API methods of the same resource.
   * 
   * @param newRootPath the path of the field to use as the new root
   * @return a new checker at the new root path
   */
  RequestedFields startingFrom(String newRootPath);
  
}
