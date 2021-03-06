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
package org.apache.cxf.rs.security.saml.sso;

import org.apache.wss4j.dom.WSConstants;

public final class SSOConstants {
    public static final String SAML_REQUEST = "SAMLRequest";
    public static final String SAML_RESPONSE = "SAMLResponse"; 
    public static final String RELAY_STATE = "RelayState";
    public static final String SIG_ALG = "SigAlg";
    public static final String SIGNATURE = "Signature";
    public static final String SECURITY_CONTEXT_TOKEN = "org.apache.cxf.websso.context";
    public static final long DEFAULT_STATE_TIME = 2L * 60L * 1000L;
    
    public static final String RSA_SHA1 = WSConstants.RSA_SHA1;
    public static final String DSA_SHA1 = WSConstants.DSA;
    
    
    private SSOConstants() {
    }
}
