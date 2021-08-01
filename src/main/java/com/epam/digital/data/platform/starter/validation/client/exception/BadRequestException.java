package com.epam.digital.data.platform.starter.validation.client.exception;

import com.epam.digital.data.platform.starter.validation.client.exception.dto.FormErrorListDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The class represents an exception which will be thrown in case the form data didn't pass
 * validation.
 */
@Getter
@RequiredArgsConstructor
public class BadRequestException extends RuntimeException {

  private final FormErrorListDto errors;
}
