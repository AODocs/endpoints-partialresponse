package com.aodocs.partialresponse.servlet;

import javax.servlet.http.HttpServletRequest;

/**
 * Given a field "path", this class will check if the corresponding field should be retained in
 * the partial response.
 * A "retained field" does not mean it will be present in the partial response, it just means it
 * should not be filtered out if present in the unfiltered response.
 * The validity of the fieldPaths is not checked by these methods.
 */
public interface RetainedFieldChecker {

  String REQUEST_ATTRIBUTE_NAME = RetainedFieldChecker.class.getName();

  /**
   * 
   * @param request a HttpServletRequest
   * @return the checker stored in the request (if any)
   */
  static RetainedFieldChecker getFromRequest(HttpServletRequest request) {
    return (RetainedFieldChecker) request.getAttribute(REQUEST_ATTRIBUTE_NAME);
  }
  
  /**
   * Indicates the field denoted by the given path is retained in the partial response.
   * If individual fields is a subresource is retained, then the  field is considered retained.
   * The path separator is "/".
   * 
   * @param fieldPath the path of the field to check
   * @return if the field should be retained in the partial response
   */
  boolean isRetained(String fieldPath);

  /**
   * Returns a new checker with a new root in the fields expression tree.
   * This is useful to normalize checks on list / get methods for the same resource.
   * 
   * @param newRootPath the path of the field to use as the new root
   * @return a new checker at the new root path
   */
  RetainedFieldChecker startingFrom(String newRootPath);
  
}
