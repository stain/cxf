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
package org.apache.cxf.sts.token.validator;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.TokenRequirements;
import org.apache.cxf.sts.token.realm.CertConstraintsParser;
import org.apache.cxf.sts.token.realm.SAMLRealmCodec;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.ws.security.SAMLTokenPrincipal;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDocInfo;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.saml.SAMLKeyInfo;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.validate.Credential;
import org.apache.ws.security.validate.SignatureTrustValidator;
import org.apache.ws.security.validate.Validator;

/**
 * Validate a SAML Assertion. It is valid if it was issued and signed by this STS.
 */
public class SAMLTokenValidator implements TokenValidator {
    
    private static final Logger LOG = LogUtils.getL7dLogger(SAMLTokenValidator.class);
    
    private Validator validator = new SignatureTrustValidator();
    
    private CertConstraintsParser certConstraints = new CertConstraintsParser();
    
    private SAMLRealmCodec samlRealmCodec;
    
    /**
     * Set a list of Strings corresponding to regular expression constraints on the subject DN
     * of a certificate that was used to sign a received Assertion
     */
    public void setSubjectConstraints(List<String> subjectConstraints) {
        certConstraints.setSubjectConstraints(subjectConstraints);
    }
    
    /**
     * Set the WSS4J Validator instance to use to validate the token.
     * @param validator the WSS4J Validator instance to use to validate the token
     */
    public void setValidator(Validator validator) {
        this.validator = validator;
    }
    
    /**
     * Set the SAMLRealmCodec instance to use to return a realm from a validated token
     * @param samlRealmCodec the SAMLRealmCodec instance to use to return a realm from a validated token
     */
    public void setSamlRealmCodec(SAMLRealmCodec samlRealmCodec) {
        this.samlRealmCodec = samlRealmCodec;
    }
    
    /**
     * Return true if this TokenValidator implementation is capable of validating the
     * ReceivedToken argument.
     */
    public boolean canHandleToken(ReceivedToken validateTarget) {
        return canHandleToken(validateTarget, null);
    }
    
    /**
     * Return true if this TokenValidator implementation is capable of validating the
     * ReceivedToken argument. The realm is ignored in this Validator.
     */
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        Object token = validateTarget.getToken();
        if (token instanceof Element) {
            Element tokenElement = (Element)token;
            String namespace = tokenElement.getNamespaceURI();
            String localname = tokenElement.getLocalName();
            if ((WSConstants.SAML_NS.equals(namespace) || WSConstants.SAML2_NS.equals(namespace))
                && "Assertion".equals(localname)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Validate a Token using the given TokenValidatorParameters.
     */
    public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
        LOG.fine("Validating SAML Token");
        TokenRequirements tokenRequirements = tokenParameters.getTokenRequirements();
        ReceivedToken validateTarget = tokenRequirements.getValidateTarget();
        STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
        Crypto sigCrypto = stsProperties.getSignatureCrypto();
        CallbackHandler callbackHandler = stsProperties.getCallbackHandler();
        
        TokenValidatorResponse response = new TokenValidatorResponse();
        response.setValid(false);
        
        if (validateTarget == null || !validateTarget.isDOMElement()) {
            return response;
        }
        
        try {
            Element validateTargetElement = (Element)validateTarget.getToken();
            AssertionWrapper assertion = new AssertionWrapper(validateTargetElement);
            
            SecurityToken secToken = null;
            if (tokenParameters.getTokenStore() != null) {
                int hash = 0;
                byte[] signatureValue = assertion.getSignatureValue();
                if (signatureValue != null && signatureValue.length > 0) {
                    hash = Arrays.hashCode(signatureValue);
                    secToken = tokenParameters.getTokenStore().getTokenByAssociatedHash(hash);
                }
            }
            if (secToken == null) {
                if (!assertion.isSigned()) {
                    LOG.log(Level.WARNING, "The received assertion is not signed, and therefore not trusted");
                    return response;
                }
                
                RequestData requestData = new RequestData();
                requestData.setSigCrypto(sigCrypto);
                WSSConfig wssConfig = WSSConfig.getNewInstance();
                requestData.setWssConfig(wssConfig);
                requestData.setCallbackHandler(callbackHandler);
                
                // Verify the signature
                assertion.verifySignature(
                    requestData, new WSDocInfo(validateTargetElement.getOwnerDocument())
                );

                // Now verify trust on the signature
                Credential trustCredential = new Credential();
                SAMLKeyInfo samlKeyInfo = assertion.getSignatureKeyInfo();
                trustCredential.setPublicKey(samlKeyInfo.getPublicKey());
                trustCredential.setCertificates(samlKeyInfo.getCerts());
    
                validator.validate(trustCredential, requestData);

                // Finally check that subject DN of the signing certificate matches a known constraint
                X509Certificate cert = null;
                if (trustCredential.getCertificates() != null) {
                    cert = trustCredential.getCertificates()[0];
                }
                
                if (!certConstraints.matches(cert)) {
                    return response;
                }
            }
            
            // Get the realm of the SAML token
            String tokenRealm = null;
            if (samlRealmCodec != null) {
                tokenRealm = samlRealmCodec.getRealmFromToken(assertion);
            }
            
            response.setValid(true);
            SAMLTokenPrincipal samlPrincipal = new SAMLTokenPrincipal(assertion);
            response.setPrincipal(samlPrincipal);
            response.setTokenRealm(tokenRealm);
        } catch (WSSecurityException ex) {
            LOG.log(Level.WARNING, "", ex);
        }

        return response;
    }
    
}
