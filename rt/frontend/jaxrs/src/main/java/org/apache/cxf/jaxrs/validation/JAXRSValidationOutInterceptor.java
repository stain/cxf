/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jaxrs.validation;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Response;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.validation.ValidationOutInterceptor;


public class JAXRSValidationOutInterceptor extends ValidationOutInterceptor
    implements ContainerResponseFilter {
    public JAXRSValidationOutInterceptor() {
    }
    public JAXRSValidationOutInterceptor(String phase) {
        super(phase);
    }
    
    @Override
    protected Object getServiceObject(Message message) {
        return ValidationUtils.getResourceInstance(message);
    }
    
    @Override
    protected Object unwrapEntity(Object entity) {
        return entity instanceof Response ? ((Response)entity).getEntity() : entity;
    }
    
    @Override
    public void filter(ContainerRequestContext in, ContainerResponseContext out) throws IOException {
        super.handleMessage(PhaseInterceptorChain.getCurrentMessage());
    }
}
