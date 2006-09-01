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

package org.apache.cxf.wsdl;

import java.lang.reflect.Method;
import java.util.List;

import javax.wsdl.Binding;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

public class JAXBExtensionHelperTest
                extends TestCase {

    private WSDLFactory wsdlFactory;

    private WSDLReader wsdlReader;

    private Definition wsdlDefinition;

    private ExtensionRegistry registry;

    public void setUp() throws Exception {

        wsdlFactory = WSDLFactory.newInstance();
        wsdlReader = wsdlFactory.newWSDLReader();
        wsdlReader.setFeature("javax.wsdl.verbose", false);
        registry = wsdlReader.getExtensionRegistry();
        if (registry == null) {
            registry = wsdlFactory.newPopulatedExtensionRegistry();
        }
        JAXBExtensionHelper.addExtensions(registry, "javax.wsdl.BindingInput",
                        "org.apache.cxf.bindings.xformat.XMLBindingMessageFormat", Thread.currentThread()
                                        .getContextClassLoader());
    }

    public void tearDown() {

    }

    public void testAddExtension() throws Exception {

        Class extClass = Class.forName("org.apache.cxf.bindings.xformat.XMLBindingMessageFormat");

        String file = this.getClass().getResource("/wsdl/hello_world_xml_bare.wsdl").getFile();

        wsdlReader.setExtensionRegistry(registry);

        wsdlDefinition = wsdlReader.readWSDL(file);

        Binding b = wsdlDefinition.getBinding(new QName("http://objectweb.org/hello_world_xml_http/bare",
                        "Greeter_XMLBinding"));
        BindingOperation bo = b.getBindingOperation("sayHi", null, null);
        BindingInput bi = bo.getBindingInput();
        List extList = bi.getExtensibilityElements();
        Object extIns = null;
        for (Object ext : extList) {
            extIns = extClass.cast(ext);
        }
        assertEquals("can't found ext element XMLBindingMessageFormat", true, extIns != null);
        QName rootNode = getRootNode(extIns);
        assertEquals("get rootNode value back from extension element", "sayHi", rootNode.getLocalPart());
    }

    private QName getRootNode(Object ext) throws Exception {
        for (int i = 0; i < ext.getClass().getMethods().length; i++) {
            Method method = ext.getClass().getMethods()[i];
            if (method.getName().equals("getRootNode")) {
                return (QName) method.invoke(ext, new Object[] {});
            }
        }
        return null;
    }
}
