package com.epam.digital.data.platform.starter.validation.config;

import com.epam.digital.data.platform.starter.validation.client.FormManagementProviderClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "form-management-provider.url")
@ComponentScan(basePackages = "com.epam.digital.data.platform.starter.validation")
@EnableFeignClients(clients = FormManagementProviderClient.class)
public class ValidationAutoConfiguration {
}
