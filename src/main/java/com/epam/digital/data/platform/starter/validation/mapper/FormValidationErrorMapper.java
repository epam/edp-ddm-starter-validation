package com.epam.digital.data.platform.starter.validation.mapper;

import com.epam.digital.data.platform.starter.errorhandling.dto.ErrorsListDto;
import com.epam.digital.data.platform.starter.validation.client.exception.dto.FormErrorListDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * The interface represents a mapper for errors. The methods are implemented using the MapStruct.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FormValidationErrorMapper {

  /**
   * Method for converting validation form errors.
   *
   * @param formErrorListDto list of {@link FormErrorListDto} entities.
   * @return list of {@link ErrorsListDto} entities.
   */
  ErrorsListDto toErrorListDto(FormErrorListDto formErrorListDto);
}
