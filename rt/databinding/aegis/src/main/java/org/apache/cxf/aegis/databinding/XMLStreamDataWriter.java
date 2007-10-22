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
package org.apache.cxf.aegis.databinding;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.validation.Schema;

import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeUtil;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.aegis.xml.stax.ElementWriter;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;

public class XMLStreamDataWriter implements DataWriter<XMLStreamWriter> {

    private static final Logger LOG = LogUtils.getL7dLogger(XMLStreamDataReader.class);

    private AegisDatabinding databinding;

    private Collection<Attachment> attachments;
    
    private Map<String, Object> properties;
    
    public XMLStreamDataWriter(AegisDatabinding databinding) {
        this.databinding = databinding;
    }

    public void setAttachments(Collection<Attachment> attachments) {
        this.attachments = attachments;
    }

    public void setSchema(Schema s) {
        // TODO Auto-generated method stub

    }

    public void write(Object obj, MessagePartInfo part, XMLStreamWriter output) {
        Type type = databinding.getType(part);

        if (type == null) {
            throw new Fault(new Message("NO_MESSAGE_FOR_PART", LOG));
        }


        Map<String, Object> props = (Endpoint)getProperty(ENDPOINT);
        if (props == null) {
            props = new HashMap<String, Object>();
        }
        Context context = new Context(props);
        
        // I'm not sure that this is the right type mapping
        context.setTypeMapping(type.getTypeMapping());
        context.setOverrideTypes(CastUtils.cast(databinding.getOverrideTypes(), String.class));
        context.setAttachments(attachments);
        Object val = databinding.getService().get(AegisDatabinding.WRITE_XSI_TYPE_KEY);
        if ("true".equals(val) || Boolean.TRUE.equals(val)) {
            context.setWriteXsiTypes(true);
        }
        
        type = TypeUtil.getWriteType(context, obj, type);
        
        try {
            ElementWriter writer = new ElementWriter(output);
            MessageWriter w2 = writer.getElementWriter(part.getConcreteName());
            if (type.isNillable() && type.isWriteOuter() && obj == null) {
                w2.writeXsiNil();
                w2.close();
                return;
            }

            type.writeObject(obj, w2, context);
            w2.close();
        } catch (DatabindingException e) {
            throw new RuntimeException(e);
        }
    }

    public void write(Object obj, XMLStreamWriter output) {
        write(obj, null, output);
    }


    public void setProperty(String prop, Object value) {
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        
        properties.put(prop, value);
    }

    public Object getProperty(String key) {
        if (properties == null) {
            return null;
        }
        return properties.get(key);
    }


}
