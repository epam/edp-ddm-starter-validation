package com.epam.digital.data.platform.starter.validation.client;

import com.epam.digital.data.platform.integration.ceph.dto.FormDataDto;
import com.epam.digital.data.platform.starter.validation.client.config.FeignConfig;
import com.epam.digital.data.platform.starter.validation.dto.FormDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


/**
 * The interface represents a feign client and used to validate form data.
 */
@FeignClient(name = "validation-form-client", url = "${form-management-provider.url}", configuration = FeignConfig.class)
public interface FormManagementProviderClient {

  /**
   * Form data validation method.
   *
   * @param id       form identifier.
   * @param formData form data for validation.
   * @return form data.
   */
  @PostMapping("/{id}/submission?dryrun=1")
  FormDataDto validateFormData(@PathVariable("id") String id, @RequestBody FormDataDto formData);

  /**
   * Get form method
   * @param id form identifier
   * @return {@link FormDto} form object
   */
  @GetMapping("/{id}")
  FormDto getForm(@PathVariable("id") String id);
}
