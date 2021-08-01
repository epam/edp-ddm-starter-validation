package com.epam.digital.data.platform.starter.validation.dto;

import com.epam.digital.data.platform.starter.errorhandling.dto.ValidationErrorDto;
import lombok.Builder;
import lombok.Data;

/**
 * Data transfer object that is used for the form validation response.
 */
@Data
@Builder
public class FormValidationResponseDto {

  private boolean isValid;
  private ValidationErrorDto error;
}
