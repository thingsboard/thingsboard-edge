/**
 * Thingsboard OÜ ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2018 Thingsboard OÜ. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of Thingsboard OÜ and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Thingsboard OÜ
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.config;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Predicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thingsboard.server.common.data.security.Authority;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.schema.AlternateTypeRule;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.List;

import static com.google.common.base.Predicates.and;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.Lists.newArrayList;
import static springfox.documentation.builders.PathSelectors.regex;

@Configuration
public class SwaggerConfiguration {

      @Bean
      public Docket thingsboardApi() {
          TypeResolver typeResolver = new TypeResolver();
          final ResolvedType jsonNodeType =
                  typeResolver.resolve(
                          JsonNode.class);
          final ResolvedType stringType =
                  typeResolver.resolve(
                          String.class);

            return new Docket(DocumentationType.SWAGGER_2)
                    .groupName("thingsboard")
                    .apiInfo(apiInfo())
                    .alternateTypeRules(
                        new AlternateTypeRule(
                                jsonNodeType,
                                stringType))
                    .select()
                    .paths(apiPaths())
                    .build()
                    .securitySchemes(newArrayList(jwtTokenKey()))
                    .securityContexts(newArrayList(securityContext()));
      }

      private ApiKey jwtTokenKey() {
            return new ApiKey("X-Authorization", "JWT token", "header");
      }

      private SecurityContext securityContext() {
            return SecurityContext.builder()
                    .securityReferences(defaultAuth())
                    .forPaths(securityPaths())
                    .build();
      }

      private Predicate<String> apiPaths() {
           return regex("/api.*");
      }

      private Predicate<String> securityPaths() {
           return and(
                    regex("/api.*"),
                    not(regex("/api/noauth.*"))
           );
      }

      List<SecurityReference> defaultAuth() {
            AuthorizationScope[] authorizationScopes = new AuthorizationScope[3];
            authorizationScopes[0] = new AuthorizationScope(Authority.SYS_ADMIN.name(), "System administrator");
            authorizationScopes[1] = new AuthorizationScope(Authority.TENANT_ADMIN.name(), "Tenant administrator");
            authorizationScopes[2] = new AuthorizationScope(Authority.CUSTOMER_USER.name(), "Customer");
            return newArrayList(
                    new SecurityReference("X-Authorization", authorizationScopes));
      }

      private ApiInfo apiInfo() {
            return new ApiInfoBuilder()
                .title("Thingsboard REST API")
                .description("For instructions how to authorize requests please visit <a href='http://thingsboard.io/docs/reference/rest-api/'>REST API documentation page</a>.")
                .contact(new Contact("Thingsboard team", "http://thingsboard.io", "info@thingsboard.io"))
                .license("Apache License Version 2.0")
                .licenseUrl("https://github.com/thingsboard/thingsboard/blob/master/LICENSE")
                .version("2.0")
                .build();
      }

}
