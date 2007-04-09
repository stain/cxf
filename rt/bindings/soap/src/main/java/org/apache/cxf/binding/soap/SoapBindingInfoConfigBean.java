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
package org.apache.cxf.binding.soap;

import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;

public class SoapBindingInfoConfigBean {
    private SoapVersion soapVersion = Soap11.getInstance();
    private String style = "document";
    private String use;
    private String transportURI = "http://schemas.xmlsoap.org/soap/http";
    private String defaultSoapAction = "";
    private boolean mtomEnabled;
    

    public SoapBindingInfoConfigBean() {
        
    }

    protected boolean isHeader(BindingOperationInfo op, MessagePartInfo part) {
        return false;
    }

    public String getSoapAction(OperationInfo op) {
        return defaultSoapAction;
    }

    public String getTransportURI() {
        return transportURI;
    }

    public void setTransportURI(String transportURI) {
        this.transportURI = transportURI;
    }

    protected String getStyle() {
        return style;
    }

    protected String getStyle(OperationInfo op) {
        return getStyle();
    }

    public SoapVersion getSoapVersion() {
        return soapVersion;
    }

    public void setSoapVersion(SoapVersion soapVersion) {
        this.soapVersion = soapVersion;
    }

    public String getUse() {
        return use;
    }

    public void setUse(String use) {
        this.use = use;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public boolean isMtomEnabled() {
        return mtomEnabled;
    }

    public void setMtomEnabled(boolean mtomEnabled) {
        this.mtomEnabled = mtomEnabled;
    }
    
}
