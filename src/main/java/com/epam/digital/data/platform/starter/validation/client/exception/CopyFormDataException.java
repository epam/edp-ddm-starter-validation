package com.epam.digital.data.platform.starter.validation.client.exception;

/**
 * Exception that is thrown when copying form data fails
 */
public class CopyFormDataException extends RuntimeException {

  public CopyFormDataException(String message) {
    super(message);
  }
}
