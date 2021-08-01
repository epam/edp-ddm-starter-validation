package com.epam.digital.data.platform.starter.validation.client.decoder;

import com.epam.digital.data.platform.starter.validation.client.exception.BadRequestException;
import com.epam.digital.data.platform.starter.validation.client.exception.dto.FormErrorListDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import lombok.SneakyThrows;

/**
 * The class represents an implementation of {@link ErrorDecoder} error decoder for feign client,
 * that is used to raise exception based on status.
 */
public class FormValidationErrorDecoder implements ErrorDecoder {

  private final ObjectMapper objectMapper;
  private final ErrorDecoder errorDecoder;

  public FormValidationErrorDecoder(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.errorDecoder = new Default();
  }

  @SneakyThrows
  @Override
  public Exception decode(String methodKey, Response response) {
    if (response.status() == 400) {
      return new BadRequestException(objectMapper
          .readValue(Util.toByteArray(response.body().asInputStream()), objectMapper.constructType(
              FormErrorListDto.class)));
    }
    return errorDecoder.decode(methodKey, response);
  }
}
