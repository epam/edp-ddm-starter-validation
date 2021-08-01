package com.epam.digital.data.platform.starter.validation.client.config;

import com.epam.digital.data.platform.starter.validation.client.decoder.FormValidationErrorDecoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * The class represents a configuration for feign client.
 */
public class FeignConfig {

  /**
   * Returns error decoder {@link FormValidationErrorDecoder}
   *
   * @return error decoder for form management provider client
   */
  @Bean
  public FormValidationErrorDecoder formValidationDecoder(ObjectMapper objectMapper) {
    return new FormValidationErrorDecoder(objectMapper);
  }

  @Bean
  @ConditionalOnMissingBean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
