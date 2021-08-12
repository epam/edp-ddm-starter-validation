package com.epam.digital.data.platform.starter.validation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data transfer object that contain form components.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComponentsDto {

  private String key;
  private String type;
  private Boolean dayFirst;
  private List<NestedComponentDto> components;
  private String filePattern;
  private String fileMaxSize;
  private ValidateComponentDto validate;
}
