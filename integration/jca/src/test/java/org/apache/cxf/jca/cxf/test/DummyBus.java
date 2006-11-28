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
package org.apache.cxf.jca.cxf.test;


//import java.util.Map;
import java.util.ResourceBundle;
import org.apache.cxf.Bus;
// import org.apache.cxf.BusEvent;
// import org.apache.cxf.BusEventCache;
// import org.apache.cxf.BusEventFilter;
// import org.apache.cxf.BusEventListener;
import org.apache.cxf.BusException;
//import org.apache.cxf.bindings.BindingManager;
//import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.interceptor.AbstractBasicInterceptorProvider;
//import org.apache.cxf.jaxws.EndpointRegistry;
//import org.apache.cxf.management.InstrumentationManager;
//import org.apache.cxf.plugins.PluginManager;
//import org.apache.cxf.resource.ResourceManager;
//import org.apache.cxf.transports.TransportFactoryManager;
//import org.apache.cxf.workqueue.WorkQueueManager;
//import org.apache.cxf.wsdl.WSDLManager;



public class DummyBus extends AbstractBasicInterceptorProvider implements Bus {    
    // for initialise behaviours
    static int initializeCount;
    static int shutdownCount;
    static boolean correctThreadContextClassLoader;
    static boolean throwException;
    static Bus bus = new DummyBus();
  
   
    static String[] invokeArgs;
    static String cxfHome = "File:/local/temp";
    
    
    public static void reset() {
        initializeCount = 0;
        shutdownCount = 0; 
        correctThreadContextClassLoader = false;
        throwException = false;
    }
    
    
    public static Bus init(String[] args) throws BusException {
        
        initializeCount++;
        correctThreadContextClassLoader = 
            Thread.currentThread().getContextClassLoader() 
            == org.apache.cxf.jca.cxf.ManagedConnectionFactoryImpl.class.getClassLoader();
        if (throwException) {
            throw new BusException(new Message("tested bus exception!", 
                                               (ResourceBundle)null, new Object[]{}));
        }
        return bus;
        
    }

    
    public void shutdown(boolean wait) {
        shutdownCount++; 
        
    }


//     @Override
//     public void sendEvent(BusEvent event) {
//         // TODO Auto-generated method stub
        
//     }


//     @Override
//     public void addListener(BusEventListener l, BusEventFilter filter) throws BusException {
//         // TODO Auto-generated method stub
        
//     }


//     @Override
//     public void removeListener(BusEventListener l) throws BusException {
//         // TODO Auto-generated method stub
        
//     }


//     @Override
//     public BusEventCache getEventCache() {
//         // TODO Auto-generated method stub
//         return null;
//     }


//     @Override
//     public TransportFactoryManager getTransportFactoryManager() {
//         // TODO Auto-generated method stub
//         return null;
//     }


//     @Override
//     public BindingManager getBindingManager() {
//         // TODO Auto-generated method stub
//         return null;
//     }


//     @Override
//     public WSDLManager getWSDLManager() {
//         // TODO Auto-generated method stub
//         return null;
//     }


//     @Override
//     public PluginManager getPluginManager() {
//         // TODO Auto-generated method stub
//         return null;
//     }


//     @Override
//     public BusLifeCycleManager getLifeCycleManager() {
//         // TODO Auto-generated method stub
//         return null;
//     }


//     @Override
//     public WorkQueueManager getWorkQueueManager() {
//         // TODO Auto-generated method stub
//         return null;
//     }


//     @Override
//     public ResourceManager getResourceManager() {
//         // TODO Auto-generated method stub
//         return null;
//     }


//     @Override
//     public InstrumentationManager getInstrumentationManager() {
//         // TODO Auto-generated method stub
//         return null;
//     }

//    @Override
    public <T> T getExtension(Class<T> extensionType) {
        return null;
    }

    //    @Override
    public <T> void setExtension(T extension, Class<T> extensionType) {

    }
    
    //    @Override
    public String getId() {
        // TODO Auto-generated method stub
        return null;
    }


    //    @Override
    public void run() {
        // TODO Auto-generated method stub
        
    }


//     @Override
//     public EndpointRegistry getEndpointRegistry() {
//         // TODO Auto-generated method stub
//         return null;
//     }


//     @Override
//     public void initialize(String[] args, Map<String, Object> properties) throws BusException {
//         // TODO Auto-generated method stub
        
//     }


//     public static String getCXFHome() {
//         return cxfHome;
//     }


//     public static void setCXFHome(String home) {
//         DummyBus.cxfHome = home;
//     }


    public static boolean isCorrectThreadContextClassLoader() {
        return correctThreadContextClassLoader;
    }


    public static void setCorrectThreadContextClassLoader(boolean correct) {
        DummyBus.correctThreadContextClassLoader = correct;
    }


    public static int getInitializeCount() {
        return initializeCount;
    }


    public static void setInitializeCount(int count) {
        DummyBus.initializeCount = count;
    }


//     public static String[] getInvokeArgs() {
//         return invokeArgs;
//     }


//     public static void setInvokeArgs(String[] args) {
//         DummyBus.invokeArgs = args;
//     }


//     public static int getShutdownCount() {
//         return shutdownCount;
//     }


//     public static void setShutdownCount(int count) {
//         DummyBus.shutdownCount = count;
//     }


//     public static void setThrowException(boolean fthrow) {
//         DummyBus.throwException = fthrow;
//     } 

}
