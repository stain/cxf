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

package org.apache.cxf.endpoint;

import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;

public interface Client extends InterceptorProvider, MessageObserver {
    String REQUEST_CONTEXT = "RequestContext";
    String RESPONSE_CONTEXT = "ResponseContext";
    String REQUEST_METHOD = "RequestMethod";
    /**
     * Invokes an operation syncronously
     * @param operationName The name of the operation to be invoked. The service namespace will be used
     * when looking up the BindingOperationInfo.
     * @param params  The params that matches the parts of the input message of the operation
     * @return The return values that matche the parts of the output message of the operation
     */
    Object[] invoke(String operationName,
                    Object... params) throws Exception;

    /**
     * Invokes an operation syncronously
     * @param operationName The name of the operation to be invoked
     * @param params  The params that matches the parts of the input message of the operation
     * @return The return values that matche the parts of the output message of the operation
     */
    Object[] invoke(QName operationName,
                    Object... params) throws Exception;

    /**
     * Invokes an operation syncronously
     * @param oi  The operation to be invoked
     * @param params  The params that matches the parts of the input message of the operation
     * @return The return values that matche the parts of the output message of the operation
     */
    Object[] invoke(BindingOperationInfo oi,
                    Object... params) throws Exception;

    /**
     * Invokes an operation syncronously
     * @param oi  The operation to be invoked
     * @param params  The params that matches the parts of the input message of the operation
     * @param context  Optional (can be null) contextual information for the invocation     
     * @return The return values that matche the parts of the output message of the operation
     */
    Object[] invoke(BindingOperationInfo oi,
                    Object[] params,
                    Map<String, Object> context) throws Exception;

    Endpoint getEndpoint();

    /**
     * Get the Conduit that messages for this client will be sent on.
     * @return Conduit
     */
    Conduit getConduit();
}
