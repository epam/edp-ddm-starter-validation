package com.epam.digital.data.platform.starter.validation.service.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.integration.ceph.dto.FormDataDto;
import com.epam.digital.data.platform.starter.errorhandling.dto.ErrorDetailDto;
import com.epam.digital.data.platform.starter.errorhandling.dto.ErrorsListDto;
import com.epam.digital.data.platform.starter.validation.client.FormManagementProviderClient;
import com.epam.digital.data.platform.starter.validation.client.exception.BadRequestException;
import com.epam.digital.data.platform.starter.validation.client.exception.dto.FormErrorDetailDto;
import com.epam.digital.data.platform.starter.validation.client.exception.dto.FormErrorListDto;
import com.epam.digital.data.platform.starter.validation.dto.ComponentsDto;
import com.epam.digital.data.platform.starter.validation.dto.FormDto;
import com.epam.digital.data.platform.starter.validation.dto.NestedComponentDto;
import com.epam.digital.data.platform.starter.validation.mapper.FormValidationErrorMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FormValidationServiceImplTest {

  @Mock
  private FormManagementProviderClient client;
  @Mock
  private FormValidationErrorMapper errorMapper;
  @Mock
  private ObjectMapper objectMapper;
  @InjectMocks
  private FormValidationServiceImpl formValidationService;

  @Test
  @SuppressWarnings("unchecked")
  public void testFormDataValidationWithValidData() throws Exception {
    var expectedData = new LinkedHashMap<String, Object>();
    var expectedNestedData = new LinkedHashMap<String, Object>();
    expectedNestedData.put("specializationDate", "12/31/2021");
    expectedData.put("specializationEndDate", "01/01/2021");
    expectedData.put("list", List.of(expectedNestedData));
    var expectedFormDataDto = FormDataDto.builder().data(expectedData).build();
    var formId = "testFormId";
    var data = new LinkedHashMap<String, Object>();
    var nestedData = new LinkedHashMap<String, Object>();
    nestedData.put("specializationDate", "2021-12-31");
    data.put("specializationEndDate", "2021-01-01");
    data.put("list", List.of(nestedData));
    var formDataDto = FormDataDto.builder().data(data).build();
    var nestedComponentsDto = new ArrayList<NestedComponentDto>();
    var nestedComponent = new NestedComponentDto("specializationDate", "day", false);
    nestedComponentsDto.add(nestedComponent);
    var componentsDto = new ArrayList<ComponentsDto>();
    var component = new ComponentsDto("specializationEndDate", "day", true, nestedComponentsDto,
        null, null);
    componentsDto.add(component);
    when(client.getForm(formId)).thenReturn(new FormDto(componentsDto));
    when(objectMapper.writeValueAsString(formDataDto.getData())).thenReturn("{}");
    when(objectMapper.readValue(eq("{}"), (TypeReference<Object>) any()))
        .thenReturn(formDataDto.getData());
    var formValidationResponseDto = formValidationService.validateForm(formId, formDataDto);

    assertThat(formValidationResponseDto).isNotNull();
    assertThat(formValidationResponseDto.isValid()).isTrue();
    assertThat(formValidationResponseDto.getError()).isNull();
    verify(client, times(1)).validateFormData(formId, expectedFormDataDto);
  }

  @Test
  public void testFormDataValidationWithInvalidDataWithoutExcludedErrorTypes() {
    var message = "ValidationError";
    var field = "name";
    var value = "123";
    var formErrorDetailDto = new FormErrorDetailDto(message, field, value);
    var errorDetailDto = new ErrorDetailDto(message, field, value);
    var formErrorListDto = new FormErrorListDto(List.of(formErrorDetailDto));
    var errorsListDto = new ErrorsListDto(List.of(errorDetailDto));
    var formId = "testFormId";
    var formDataDto = FormDataDto.builder().data(new LinkedHashMap<>()).build();
    doThrow(new BadRequestException(formErrorListDto)).when(client)
        .validateFormData(eq(formId), any());
    when(client.getForm(formId)).thenReturn(new FormDto(new ArrayList<>()));
    when(errorMapper.toErrorListDto(formErrorListDto)).thenReturn(errorsListDto);

    var formValidationResponseDto = formValidationService.validateForm(formId, formDataDto);

    assertThat(formValidationResponseDto).isNotNull();
    assertThat(formValidationResponseDto.isValid()).isFalse();
    assertThat(formValidationResponseDto.getError()).isNotNull();
    assertThat(formValidationResponseDto.getError().getDetails().getErrors().get(0).getMessage())
        .isEqualTo(message);
    assertThat(formValidationResponseDto.getError().getDetails().getErrors().get(0).getField())
        .isEqualTo(field);
    assertThat(formValidationResponseDto.getError().getDetails().getErrors().get(0).getValue())
        .isEqualTo(value);
  }

  @Test
  public void testFormDataValidationWithInvalidDataWithJustExcludedErrorTypes() {
    var formErrorDetailDto = new FormErrorDetailDto("ValidationError", "fileName", "123");
    var formErrorDetailDto2 = new FormErrorDetailDto("ValidationError2", null, null);
    var formErrorListDto = new FormErrorListDto(List.of(formErrorDetailDto, formErrorDetailDto2));
    var formId = "testFormId";
    var formDataDto = FormDataDto.builder().data(new LinkedHashMap<>()).build();
    var componentsDtos = List.of(new ComponentsDto("fileName", "file", false, null, null, null));
    doThrow(new BadRequestException(formErrorListDto)).when(client)
        .validateFormData(eq(formId), any());
    when(client.getForm(formId)).thenReturn(new FormDto(componentsDtos));

    var formValidationResponseDto = formValidationService.validateForm(formId, formDataDto);

    assertThat(formValidationResponseDto).isNotNull();
    assertThat(formValidationResponseDto.isValid()).isTrue();
    assertThat(formValidationResponseDto.getError()).isNull();
  }

  @Test
  public void testFormDataValidationWithInvalidDataWithAllowedAndExcludedErrorTypes() {
    var formErrorDetailDto = new FormErrorDetailDto("ValidationError2", "name", "321");
    var formErrorDetailDto2 = new FormErrorDetailDto("ValidationError3", "fileName", "543");
    var formErrorListDto = new FormErrorListDto(
        List.of(formErrorDetailDto, formErrorDetailDto2));
    var formId = "testFormId";
    var formDataDto = FormDataDto.builder().data(new LinkedHashMap<>()).build();
    var nestedComponent = new NestedComponentDto("fullName", "textfield", false);
    var nestedComponent2 = new NestedComponentDto("fileName", "file", false);
    var componentsDtos = List
        .of(new ComponentsDto("name", "textfield", false, null, null, null),
            new ComponentsDto("fileName", "file", false, List.of(nestedComponent, nestedComponent2),
                null, null));
    var errorDetailDto = new ErrorDetailDto("ValidationError", "name", "321");
    var errorsListDto = new ErrorsListDto(List.of(errorDetailDto));
    doThrow(new BadRequestException(formErrorListDto)).when(client)
        .validateFormData(eq(formId), any());
    when(client.getForm(formId)).thenReturn(new FormDto(componentsDtos));
    when(errorMapper.toErrorListDto(any())).thenReturn(errorsListDto);

    var formValidationResponseDto = formValidationService.validateForm(formId, formDataDto);

    assertThat(formValidationResponseDto).isNotNull();
    assertThat(formValidationResponseDto.isValid()).isFalse();
    assertThat(formValidationResponseDto.getError().getDetails().getErrors().get(0).getField())
        .isEqualTo("name");
  }
}
