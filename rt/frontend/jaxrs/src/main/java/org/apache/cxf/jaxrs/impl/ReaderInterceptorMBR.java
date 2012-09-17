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
package org.apache.cxf.jaxrs.impl;

import java.io.IOException;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

public class ReaderInterceptorMBR implements ReaderInterceptor {

    private MessageBodyReader<?> reader;
    
    public ReaderInterceptorMBR(MessageBodyReader<?> reader) {
        this.reader = reader;
    }
    
    @SuppressWarnings({
        "unchecked", "rawtypes"
    })
    @Override
    public Object aroundReadFrom(ReaderInterceptorContext c) throws IOException, WebApplicationException {
        return reader.readFrom((Class)c.getType(), c.getGenericType(),
                               c.getAnnotations(), c.getMediaType(),
                               c.getHeaders(), c.getInputStream());
    }

}