/*
 * Copyright 2016 Hammock and its contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ws.ament.hammock.web.cors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.servlet.annotation.WebInitParam;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import ws.ament.hammock.web.api.FilterDescriptor;

import java.util.Optional;

import static ws.ament.hammock.web.base.Constants.DISPATCHER_TYPES;

@ApplicationScoped
public class CORSConfiguration {


   @Inject
   @ConfigProperty(name = "cors.enabled",defaultValue = "false")
   private boolean enabled;
   @Inject
   @ConfigProperty(name = "cors.origin")
   private Optional<String> origin;
   @Inject
   @ConfigProperty(name = "cors.headers")
   private Optional<String> headers;
   @Inject
   @ConfigProperty(name = "cors.credentials")
   private Optional<String> credentials;
   @Inject
   @ConfigProperty(name = "cors.methods")
   private Optional<String> methods;
   @Inject
   @ConfigProperty(name = "cors.maxAge")
   private Optional<String> maxAge;

   @Produces
   @Dependent
   private FilterDescriptor resourceFilter = new FilterDescriptor("CORSFilter", new String[]{"/*"}, new String[]{"/*"},
                                                                  DISPATCHER_TYPES,
                                                                  new WebInitParam[]{}, true, null, CORSFilter.class);

   public boolean isEnabled() {
      return enabled;
   }

   public String getOrigin() {
      return origin.orElse(null);
   }

   public String getHeaders() {
      return headers.orElse(null);
   }

   public String getCredentials() {
      return credentials.orElse(null);
   }

   public String getMethods() {
      return methods.orElse(null);
   }

   public String getMaxAge() {
      return maxAge.orElse(null);
   }
}
