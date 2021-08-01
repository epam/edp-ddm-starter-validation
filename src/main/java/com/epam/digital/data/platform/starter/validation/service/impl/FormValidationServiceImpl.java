package com.epam.digital.data.platform.starter.validation.service.impl;

import com.epam.digital.data.platform.integration.ceph.dto.FormDataDto;
import com.epam.digital.data.platform.starter.errorhandling.dto.ValidationErrorDto;
import com.epam.digital.data.platform.starter.validation.client.FormManagementProviderClient;
import com.epam.digital.data.platform.starter.validation.client.exception.BadRequestException;
import com.epam.digital.data.platform.starter.validation.client.exception.dto.FormErrorDetailDto;
import com.epam.digital.data.platform.starter.validation.client.exception.dto.FormErrorListDto;
import com.epam.digital.data.platform.starter.validation.dto.ComponentsDto;
import com.epam.digital.data.platform.starter.validation.dto.FormDto;
import com.epam.digital.data.platform.starter.validation.dto.FormValidationResponseDto;
import com.epam.digital.data.platform.starter.validation.dto.NestedComponentDto;
import com.epam.digital.data.platform.starter.validation.mapper.FormValidationErrorMapper;
import com.epam.digital.data.platform.starter.validation.service.FormValidationService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FormValidationServiceImpl implements FormValidationService {

  private static final String TRACE_ID_KEY = "X-B3-TraceId";
  private static final List<String> EXCLUDED_TYPES = List.of("file", "date", "day");

  private final FormManagementProviderClient client;
  private final FormValidationErrorMapper errorMapper;

  @Override
  public FormValidationResponseDto validateForm(String formId, FormDataDto formDataDto) {
    try {
      client.validateFormData(formId, formDataDto);
      return FormValidationResponseDto.builder().isValid(true).error(null).build();
    } catch (BadRequestException ex) {
      return processException(formId, ex.getErrors());
    }
  }

  private FormValidationResponseDto processException(String formId,
      FormErrorListDto formErrorListDto) {
    var form = client.getForm(formId);
    var allowedErrors = formErrorListDto.getErrors().stream()
        .filter(formErrorDetailDto -> isAllowedType(formErrorDetailDto, form))
        .collect(Collectors.toList());
    if (allowedErrors.isEmpty()) {
      return FormValidationResponseDto.builder().isValid(true).error(null).build();
    }

    var errorsListDto = errorMapper.toErrorListDto(new FormErrorListDto(allowedErrors));
    var error = ValidationErrorDto.builder()
        .traceId(MDC.get(TRACE_ID_KEY))
        .code("FORM_VALIDATION_ERROR")
        .message("Form validation error")
        .details(errorsListDto)
        .build();
    return FormValidationResponseDto.builder().isValid(false).error(error).build();
  }

  private boolean isAllowedType(FormErrorDetailDto formErrorDetailDto, FormDto form) {
    String field = formErrorDetailDto.getField();
    if (field == null) {
      return false;
    }
    return isAllowedComponentsType(form.getComponents(), field);
  }

  private boolean isAllowedComponentsType(List<ComponentsDto> formComponents, String field) {
    for (ComponentsDto component : formComponents) {
      var nestedComponents = component.getComponents();
      if (nestedComponents != null) {
        for (NestedComponentDto nestedComponent : nestedComponents) {
          if (field.equals(nestedComponent.getKey())) {
            return !EXCLUDED_TYPES.contains(nestedComponent.getType());
          }
        }
      } else if (field.equals(component.getKey())) {
        return !EXCLUDED_TYPES.contains(component.getType());
      }
    }
    return true;
  }
}
