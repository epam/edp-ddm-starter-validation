package com.epam.digital.data.platform.starter.validation.service.impl;

import com.epam.digital.data.platform.integration.ceph.dto.FormDataDto;
import com.epam.digital.data.platform.starter.errorhandling.dto.ValidationErrorDto;
import com.epam.digital.data.platform.starter.validation.client.FormManagementProviderClient;
import com.epam.digital.data.platform.starter.validation.client.exception.BadRequestException;
import com.epam.digital.data.platform.starter.validation.client.exception.CopyFormDataException;
import com.epam.digital.data.platform.starter.validation.client.exception.dto.FormErrorDetailDto;
import com.epam.digital.data.platform.starter.validation.client.exception.dto.FormErrorListDto;
import com.epam.digital.data.platform.starter.validation.dto.ComponentsDto;
import com.epam.digital.data.platform.starter.validation.dto.FormDto;
import com.epam.digital.data.platform.starter.validation.dto.FormValidationResponseDto;
import com.epam.digital.data.platform.starter.validation.mapper.FormValidationErrorMapper;
import com.epam.digital.data.platform.starter.validation.service.FormValidationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FormValidationServiceImpl implements FormValidationService {

  private static final String TRACE_ID_KEY = "X-B3-TraceId";
  private static final String DAY_TYPE = "day";
  private static final String DATE_FORMAT = "%s/%s/%s";
  private static final String DATE_SEPARATOR = "-";
  private static final int YEAR_INDEX = 0;
  private static final int MONTH_INDEX = 1;
  private static final int DAY_INDEX = 2;
  private static final String EXCLUDED_TYPES = "file";

  private final FormManagementProviderClient client;
  private final FormValidationErrorMapper errorMapper;
  private final ObjectMapper objectMapper;

  @Override
  public FormValidationResponseDto validateForm(String formId, FormDataDto formDataDto) {
    var form = client.getForm(formId);
    try {
      var dayComponents = getDayTypeComponents(form.getComponents());
      var dataCopy = getCopyFormData(formDataDto);

      changeDateFormat(dataCopy, dayComponents);
      client.validateFormData(formId, FormDataDto.builder().data(dataCopy).build());

      return FormValidationResponseDto.builder().isValid(true).error(null).build();
    } catch (BadRequestException ex) {
      return processException(form, ex.getErrors());
    }
  }

  private LinkedHashMap<String, Object> getCopyFormData(FormDataDto formDataDto){
    try {
      var stringFormData = objectMapper.writeValueAsString(formDataDto.getData());
      return objectMapper.readValue(stringFormData, new TypeReference<>() {});
    } catch (JsonProcessingException ex){
      throw new CopyFormDataException("Error while copying form data");
    }
  }

  @SuppressWarnings("unchecked")
  private void changeDateFormat(Map<String, Object> formData, Map<String, Boolean> dayComponents) {
    if (Objects.isNull(formData)) {
      return;
    }
      for (var dataEntry : formData.entrySet()) {
        var key = dataEntry.getKey();
        var value = dataEntry.getValue();
        if (value instanceof String && dayComponents.containsKey(key)) {
          formData.put(key, convertDateToFormIOFormat((String) value, dayComponents.get(key)));
        } else if (value instanceof List) {
          var list = (List<Object>) value;
          for (Object nestedObj : list) {
            var nestedMap = (Map<String, Object>) nestedObj;
            changeDateFormat(nestedMap, dayComponents);
          }
        }
      }
  }

  private FormValidationResponseDto processException(FormDto form,
      FormErrorListDto formErrorListDto) {
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
    var field = formErrorDetailDto.getField();
    if (field == null) {
      return false;
    }
    return isAllowedComponentsType(form.getComponents(), field);
  }

  private boolean isAllowedComponentsType(List<ComponentsDto> formComponents, String field) {
    for (var component : formComponents) {
      var nestedComponents = component.getComponents();
      if (nestedComponents != null) {
        for (var nestedComponent : nestedComponents) {
          if (field.equals(nestedComponent.getKey())) {
            return !EXCLUDED_TYPES.equals(nestedComponent.getType());
          }
        }
      }
      if (field.equals(component.getKey())) {
        return !EXCLUDED_TYPES.equals(component.getType());
      }
    }
    return true;
  }

  private Map<String, Boolean> getDayTypeComponents(List<ComponentsDto> formComponents) {
    var dayComponents = new HashMap<String, Boolean>();
    for (var component : formComponents) {
      var nestedComponents = component.getComponents();
      if (nestedComponents != null) {
        for (var nestedComponent : nestedComponents) {
          if (DAY_TYPE.equals(nestedComponent.getType())) {
            dayComponents.put(nestedComponent.getKey(), nestedComponent.getDayFirst());
          }
        }
      }
      if (DAY_TYPE.equals(component.getType())) {
        dayComponents.put(component.getKey(), component.getDayFirst());
      }
    }
    return dayComponents;
  }

  private String convertDateToFormIOFormat(String date, boolean dayFirst) {
    var dateArray = date.split(DATE_SEPARATOR);
    if (dayFirst) {
      return String
          .format(DATE_FORMAT, dateArray[DAY_INDEX], dateArray[MONTH_INDEX], dateArray[YEAR_INDEX]);
    } else {
      return String
          .format(DATE_FORMAT, dateArray[MONTH_INDEX], dateArray[DAY_INDEX], dateArray[YEAR_INDEX]);
    }
  }
}
