package com.epam.digital.data.platform.starter.validation.client.exception.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.util.Strings;

/**
 * Data transfer object that contain detailed description of the error.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FormErrorDetailDto {

  private String message;
  private String field;
  private String value;

  @JsonProperty("context")
  private void mapContextFields(Map<String, Object> context) {
    this.field = (String) context.get("key");
    this.value = getStringValue(context.get("value"));
  }

  private String getStringValue(Object obj) {
    if (obj != null) {
      return obj.toString();
    }
    return Strings.EMPTY;
  }
}
