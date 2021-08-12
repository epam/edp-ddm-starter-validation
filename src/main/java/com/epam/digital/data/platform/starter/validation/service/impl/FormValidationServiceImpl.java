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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
  private static final String DATE_FORMAT_REGEX = "(.+-.+-.+)";
  private static final String FILE_TYPE = "file";
  private static final String FILE_VALUE_ID = "id";
  private static final String FILE_VALUE_CHECKSUM = "checksum";

  private final FormManagementProviderClient client;
  private final FormValidationErrorMapper errorMapper;
  private final ObjectMapper objectMapper;

  @Override
  public FormValidationResponseDto validateForm(String formId, FormDataDto formDataDto) {
    var form = client.getForm(formId);
    var dayComponents = getDayTypeComponents(form.getComponents());
    var dataCopy = getCopyFormData(formDataDto);
    changeDateFormat(dataCopy, dayComponents);
    var errorsRequiredFileType = getErrorsRequiredFileType(form, formDataDto);
    try {
      client.validateFormData(formId, FormDataDto.builder().data(dataCopy).build());
      if (errorsRequiredFileType.isEmpty()) {
        return FormValidationResponseDto.builder().isValid(true).error(null).build();
      } else {
        return createFailedValidationResponse(errorsRequiredFileType);
      }
    } catch (BadRequestException ex) {
      return processException(form, ex.getErrors(), errorsRequiredFileType);
    }
  }

  private LinkedHashMap<String, Object> getCopyFormData(FormDataDto formDataDto) {
    try {
      var stringFormData = objectMapper.writeValueAsString(formDataDto.getData());
      return objectMapper.readValue(stringFormData, new TypeReference<>() {
      });
    } catch (JsonProcessingException ex) {
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
      FormErrorListDto formErrorListDto, List<FormErrorDetailDto> fileErrors) {
    var allowedErrors = formErrorListDto.getErrors().stream()
        .filter(formErrorDetailDto -> isAllowedType(formErrorDetailDto, form))
        .collect(Collectors.toList());
    allowedErrors.addAll(fileErrors);

    if (allowedErrors.isEmpty()) {
      return FormValidationResponseDto.builder().isValid(true).error(null).build();
    }
    return createFailedValidationResponse(allowedErrors);
  }

  private FormValidationResponseDto createFailedValidationResponse(List<FormErrorDetailDto> err) {
    var errorsListDto = errorMapper.toErrorListDto(new FormErrorListDto(err));
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
            return !FILE_TYPE.equals(nestedComponent.getType());
          }
        }
      }
      if (field.equals(component.getKey())) {
        return !FILE_TYPE.equals(component.getType());
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
    if (Objects.nonNull(date) && date.matches(DATE_FORMAT_REGEX)) {
      var dateArray = date.split(DATE_SEPARATOR);
      return dayFirst ? createDate(DAY_INDEX, MONTH_INDEX, dateArray)
          : createDate(MONTH_INDEX, DAY_INDEX, dateArray);
    }
    return date;
  }

  private String createDate(int firstIndex, int secondIndex, String[] dateArr) {
    return String
        .format(DATE_FORMAT, dateArr[firstIndex], dateArr[secondIndex], dateArr[YEAR_INDEX]);
  }

  private List<FormErrorDetailDto> getErrorsRequiredFileType(FormDto form, FormDataDto formData) {
    var errors = new ArrayList<FormErrorDetailDto>();
    var requiredFileKeys = getRequiredFileKeys(form.getComponents());
    var emptyRequiredComponentFileKeys = getEmptyRequiredComponentFileKeys(formData.getData(),
        requiredFileKeys.keySet());
    emptyRequiredComponentFileKeys.forEach((key, value) -> {
      var stringValue = Objects.isNull(value) ? null : value.toString();
      var formErrorDetailDto = new FormErrorDetailDto(requiredFileKeys.get(key), key, stringValue);
      errors.add(formErrorDetailDto);
    });
    return errors;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getEmptyRequiredComponentFileKeys(Map<String, Object> formData,
      Set<String> requiredKeys) {
    var emptyRequiredComponentFileKeys = new HashMap<String, Object>();
    if (Objects.isNull(formData)) {
      return emptyRequiredComponentFileKeys;
    }
    for (var dataEntry : formData.entrySet()) {
      var key = dataEntry.getKey();
      var value = dataEntry.getValue();
      if (requiredKeys.contains(key) && isEmptyFileValue(value)) {
        emptyRequiredComponentFileKeys.put(key, value);
      }
      if (value instanceof List) {
        var list = (List<Object>) value;
        for (Object nestedObj : list) {
          var nestedMap = (Map<String, Object>) nestedObj;
          emptyRequiredComponentFileKeys
              .putAll(getEmptyRequiredComponentFileKeys(nestedMap, requiredKeys));
        }
      }
    }
    return emptyRequiredComponentFileKeys;
  }

  @SuppressWarnings("unchecked")
  private boolean isEmptyFileValue(Object fileValue) {
    var fileObj = (Map<String, String>) fileValue;
    if (Objects.isNull(fileObj) || fileObj.isEmpty()) {
      return true;
    }
    var id = fileObj.get(FILE_VALUE_ID);
    var checksum = fileObj.get(FILE_VALUE_CHECKSUM);
    return StringUtils.isEmpty(id) || StringUtils.isEmpty(checksum);
  }

  private Map<String, String> getRequiredFileKeys(List<ComponentsDto> formComponents) {
    var fileKeys = new HashMap<String, String>();
    for (var component : formComponents) {
      var nestedComponents = component.getComponents();
      if (nestedComponents != null) {
        for (var nestedComponent : nestedComponents) {
          var validateComponent = nestedComponent.getValidate();
          if (FILE_TYPE.equals(nestedComponent.getType()) && Objects.nonNull(validateComponent)
              && validateComponent.isRequired()) {
            fileKeys.put(nestedComponent.getKey(), validateComponent.getCustomMessage());
          }
        }
      }
      var validateComponent = component.getValidate();
      if (FILE_TYPE.equals(component.getType()) && Objects.nonNull(validateComponent)
          && validateComponent.isRequired()) {
        fileKeys.put(component.getKey(), validateComponent.getCustomMessage());
      }
    }
    return fileKeys;
  }
}
