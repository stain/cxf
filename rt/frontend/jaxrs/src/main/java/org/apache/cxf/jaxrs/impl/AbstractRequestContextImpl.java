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

import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;

public abstract class AbstractRequestContextImpl {

    private static final String PROPERTY_KEY = "jaxrs.filter.properties";
    
    protected HttpHeaders h;
    protected Message m;
    private Map<String, Object> props;
    private boolean responseContext;
    public AbstractRequestContextImpl(Message message, boolean responseContext) {
        this.m = message;
        this.props = CastUtils.cast((Map<?, ?>)message.get(PROPERTY_KEY));
        this.h = new HttpHeadersImpl(message);
        this.responseContext = responseContext;
    }
    
    public void abortWith(Response response) {
        checkContext();
        m.getExchange().put(Response.class, response);
    }

    public List<Locale> getAcceptableLanguages() {
        return getHttpHeaders().getAcceptableLanguages();
    }

    public List<MediaType> getAcceptableMediaTypes() {
        return getHttpHeaders().getAcceptableMediaTypes();
    }

    public Map<String, Cookie> getCookies() {
        return getHttpHeaders().getCookies();
    }

    public Date getDate() {
        return getHttpHeaders().getDate();
    }

    public String getHeaderString(String name) {
        return getHttpHeaders().getHeaderString(name);
    }

    public Locale getLanguage() {
        return getHttpHeaders().getLanguage();
    }

    public int getLength() {
        return getHttpHeaders().getLength();
    }

    public MediaType getMediaType() {
        return getHttpHeaders().getMediaType();
    }

    public String getMethod() {
        return (String)getProperty(Message.HTTP_REQUEST_METHOD);
    }

    public Object getProperty(String name) {
        return props == null ? null : props.get(name);
    }

    public Enumeration<String> getPropertyNames() {
        final Iterator<String> it = props.keySet().iterator();
        return new Enumeration<String>() {

            @Override
            public boolean hasMoreElements() {
                return it.hasNext();
            }

            @Override
            public String nextElement() {
                return it.next();
            }
            
        };
    }


    public void removeProperty(String name) {
        if (props != null) {
            props.remove(name);    
        }
    }


    public void setMethod(String method) throws IllegalStateException {
        checkContext();
        m.put(Message.HTTP_REQUEST_METHOD, method);

    }

    public void setProperty(String name, Object value) {
        if (props == null) {
            props = new HashMap<String, Object>();
            m.put(PROPERTY_KEY, props);
        }    
        props.put(name, value);    
        
    }

    protected HttpHeaders getHttpHeaders() {
        return h != null ? h : new HttpHeadersImpl(m);
    }
    
    protected void checkContext() {
        if (responseContext) {
            throw new IllegalStateException();
        }
    }
}