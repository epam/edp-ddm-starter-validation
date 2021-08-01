package com.epam.digital.data.platform.starter.validation.service.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.doThrow;
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
  @InjectMocks
  private FormValidationServiceImpl formValidationService;

  @Test
  public void testFormDataValidationWithValidData() {
    var formId = "testFormId";
    var formDataDto = FormDataDto.builder().data(new LinkedHashMap<>()).build();
    when(client.validateFormData(formId, formDataDto)).thenReturn(formDataDto);

    var formValidationResponseDto = formValidationService.validateForm(formId, formDataDto);

    assertThat(formValidationResponseDto).isNotNull();
    assertThat(formValidationResponseDto.isValid()).isTrue();
    assertThat(formValidationResponseDto.getError()).isNull();
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
        .validateFormData(formId, formDataDto);
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
    var formErrorDetailDto = new FormErrorDetailDto("ValidationError", "createdDate", "123");
    var formErrorDetailDto2 = new FormErrorDetailDto("ValidationError2", null, null);
    var formErrorListDto = new FormErrorListDto(List.of(formErrorDetailDto, formErrorDetailDto2));
    var formId = "testFormId";
    var formDataDto = FormDataDto.builder().data(new LinkedHashMap<>()).build();
    var componentsDtos = List.of(new ComponentsDto("createdDate", "day", null, null, null));
    doThrow(new BadRequestException(formErrorListDto)).when(client)
        .validateFormData(formId, formDataDto);
    when(client.getForm(formId)).thenReturn(new FormDto(componentsDtos));

    var formValidationResponseDto = formValidationService.validateForm(formId, formDataDto);

    assertThat(formValidationResponseDto).isNotNull();
    assertThat(formValidationResponseDto.isValid()).isTrue();
    assertThat(formValidationResponseDto.getError()).isNull();
  }

  @Test
  public void testFormDataValidationWithInvalidDataWithAllowedAndExcludedErrorTypes() {
    var formErrorDetailDto = new FormErrorDetailDto("ValidationError", "createdDate", "123");
    var formErrorDetailDto2 = new FormErrorDetailDto("ValidationError2", "name", "321");
    var formErrorDetailDto3 = new FormErrorDetailDto("ValidationError3", "spDate","543");
    var formErrorListDto = new FormErrorListDto(
        List.of(formErrorDetailDto, formErrorDetailDto2, formErrorDetailDto3));
    var formErrorListToMap = new FormErrorListDto(List.of(formErrorDetailDto2));
    var formId = "testFormId";
    var formDataDto = FormDataDto.builder().data(new LinkedHashMap<>()).build();
    var nestedComponent = new NestedComponentDto("fullName", "textfield");
    var nestedComponent2 = new NestedComponentDto("spDate", "date");
    var componentsDtos = List
        .of(new ComponentsDto("createdDate", "day", null, null, null),
            new ComponentsDto("name", "textfield", null, null, null),
            new ComponentsDto("spDate", "date", List.of(nestedComponent, nestedComponent2), null, null));
    var errorDetailDto = new ErrorDetailDto("ValidationError", "name", "321");
    var errorsListDto = new ErrorsListDto(List.of(errorDetailDto));
    doThrow(new BadRequestException(formErrorListDto)).when(client)
        .validateFormData(formId, formDataDto);
    when(client.getForm(formId)).thenReturn(new FormDto(componentsDtos));
    when(errorMapper.toErrorListDto(formErrorListToMap)).thenReturn(errorsListDto);

    var formValidationResponseDto = formValidationService.validateForm(formId, formDataDto);

    assertThat(formValidationResponseDto).isNotNull();
    assertThat(formValidationResponseDto.isValid()).isFalse();
    assertThat(formValidationResponseDto.getError().getDetails().getErrors().get(0).getField())
        .isEqualTo("name");
  }
}
