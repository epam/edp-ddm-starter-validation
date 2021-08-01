package com.epam.digital.data.platform.starter.validation.service;

import com.epam.digital.data.platform.integration.ceph.dto.FormDataDto;
import com.epam.digital.data.platform.starter.validation.dto.FormValidationResponseDto;

/**
 * The FormValidationService class represents a service that is used for form data validation.
 */
public interface FormValidationService {

  /**
   * Form data validation method.
   *
   * @param formId      form identifier.
   * @param formDataDto form data for validation.
   * @return {@link FormValidationResponseDto} entity.
   */
  FormValidationResponseDto validateForm(String formId, FormDataDto formDataDto);
}
