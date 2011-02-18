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

package org.apache.cxf.jaxrs.provider;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.validation.Schema;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.ReflectionInvokationHandler;
import org.apache.cxf.jaxb.JAXBBeanInfo;
import org.apache.cxf.jaxb.JAXBContextProxy;
import org.apache.cxf.jaxb.JAXBUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.jaxrs.utils.schemas.SchemaHandler;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.staxutils.transform.TransformUtils;

public abstract class AbstractJAXBProvider extends AbstractConfigurableProvider
    implements MessageBodyReader<Object>, MessageBodyWriter<Object> {
    
    protected static final ResourceBundle BUNDLE = BundleUtils.getBundle(AbstractJAXBProvider.class);

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractJAXBProvider.class);
    private static final String JAXB_DEFAULT_NAMESPACE = "##default";
    private static final String JAXB_DEFAULT_NAME = "##default";
    
    private static Map<String, JAXBContext> packageContexts = new HashMap<String, JAXBContext>();
    private static Map<Class<?>, JAXBContext> classContexts = new HashMap<Class<?>, JAXBContext>();

   
    protected Set<Class<?>> collectionContextClasses = new HashSet<Class<?>>();
    protected JAXBContext collectionContext; 
    
    protected Map<String, String> jaxbElementClassMap;
    protected boolean unmarshalAsJaxbElement;
    protected boolean marshalAsJaxbElement;
    
    protected Map<String, String> outElementsMap;
    protected Map<String, String> outAppendMap;
    protected List<String> outDropElements;
    protected List<String> inDropElements;
    protected Map<String, String> inElementsMap;
    protected Map<String, String> inAppendMap;
    private boolean attributesToElements;
    
    private MessageContext mc;
    private Schema schema;
    private String collectionWrapperName;
    private Map<String, String> collectionWrapperMap;
    private List<String> jaxbElementClassNames;
    private Map<String, Object> cProperties;
    private Map<String, Object> uProperties;
    
    private boolean skipJaxbChecks;
    private boolean singleJaxbContext;
    private Class[] extraClass;
    
    public void setSingleJaxbContext(boolean useSingleContext) {
        singleJaxbContext = useSingleContext;
    }
    
    public void setExtraClass(Class[] userExtraClass) {
        extraClass = userExtraClass;
    }
    
    @Override
    public void init(List<ClassResourceInfo> cris) {
        if (singleJaxbContext) {
            Set<Class<?>> allTypes = 
                new HashSet<Class<?>>(ResourceUtils.getAllRequestResponseTypes(cris, true).keySet());
            JAXBContext context = 
                ResourceUtils.createJaxbContext(allTypes, extraClass, cProperties);
            if (context != null) {
                for (Class<?> cls : allTypes) {
                    classContexts.put(cls, context);
                }
            }
        }
    }
    
    public void setContextProperties(Map<String, Object> contextProperties) {
        cProperties = contextProperties;
    }
    
    public void setUnmarshallerProperties(Map<String, Object> unmarshalProperties) {
        uProperties = unmarshalProperties;
    }
    
    public void setUnmarshallAsJaxbElement(boolean value) {
        unmarshalAsJaxbElement = value;
    }
    
    public void setMarshallAsJaxbElement(boolean value) {
        marshalAsJaxbElement = value;
    }
    
    public void setJaxbElementClassNames(List<String> names) {
        jaxbElementClassNames = names;
    }
    
    public void setJaxbElementClassMap(Map<String, String> map) {
        jaxbElementClassMap = map;
    }
    
    protected void checkContentLength() {
        if (mc != null) {
            HttpHeaders headers = mc.getHttpHeaders();
            if (headers != null) {
                List<String> values = mc.getHttpHeaders().getRequestHeader(HttpHeaders.CONTENT_LENGTH);
                if (values.size() == 1 && "0".equals(values.get(0))) {
                    String message = new org.apache.cxf.common.i18n.Message("EMPTY_BODY", BUNDLE).toString();
                    LOG.warning(message);
                    throw new WebApplicationException(400);
                }
            }
        }
    }
    
    protected <T> T getStaxHandlerFromCurrentMessage(Class<T> staxCls) {
        Message m = PhaseInterceptorChain.getCurrentMessage();
        if (m != null) {
            return staxCls.cast(m.getContent(staxCls));
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    protected Object convertToJaxbElementIfNeeded(Object obj, Class<?> cls, Type genericType) 
        throws Exception {
        
        QName name = null;
        if (jaxbElementClassNames != null && jaxbElementClassNames.contains(cls.getName()) 
            || jaxbElementClassMap != null && jaxbElementClassMap.containsKey(cls.getName())) {
            if (jaxbElementClassMap != null) {
                name = JAXRSUtils.convertStringToQName(jaxbElementClassMap.get(cls.getName()));
            } else {
                name = getJaxbQName(cls, genericType, obj, false);
            }
        }
        if (name == null && marshalAsJaxbElement) {
            name = JAXRSUtils.convertStringToQName(cls.getSimpleName());
        }
        if (name != null) {
            return new JAXBElement(name, cls, null, obj);
        }
        return obj;
    }
    
    public void setCollectionWrapperName(String wName) {
        collectionWrapperName = wName;
    }
    
    public void setCollectionWrapperMap(Map<String, String> map) {
        collectionWrapperMap = map;
    }
    
    protected void setContext(MessageContext context) {
        mc = context;
    }
    
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        
        if (InjectionUtils.isSupportedCollectionOrArray(type)) {
            type = InjectionUtils.getActualType(genericType);
            if (type == null) {
                return false;
            }
        }
        
        return unmarshalAsJaxbElement || isSupported(type, genericType, anns);
    }
    
    protected JAXBContext getCollectionContext(Class<?> type) throws JAXBException {
        synchronized (collectionContextClasses) {
            if (!collectionContextClasses.contains(type)) {
                collectionContextClasses.add(CollectionWrapper.class);
                collectionContextClasses.add(type);
            }
            collectionContext = JAXBContext.newInstance(collectionContextClasses.toArray(new Class[]{}), 
                                                        cProperties);
            return collectionContext;
        }
    }
    
    protected QName getCollectionWrapperQName(Class<?> cls, Type type, Object object, boolean pluralName)
        throws Exception {
        String name = getCollectionWrapperName(cls);
        if (name == null) {
            return getJaxbQName(cls, type, object, pluralName);
        }
            
        return JAXRSUtils.convertStringToQName(name);
    }
    
    private String getCollectionWrapperName(Class<?> cls) {
        if (collectionWrapperName != null) { 
            return collectionWrapperName;
        }
        if (collectionWrapperMap != null) {
            return collectionWrapperMap.get(cls.getName());
        }
        
        return null;
    }
    
    protected QName getJaxbQName(Class<?> cls, Type type, Object object, boolean pluralName) 
        throws Exception {
        
        if (cls == JAXBElement.class) {
            return object != null ? ((JAXBElement)object).getName() : null;
        }
        
        XmlRootElement root = cls.getAnnotation(XmlRootElement.class);
        QName qname = null;
        if (root != null) {
            String namespace = getNamespace(root.namespace());
            if ("".equals(namespace)) {
                String packageNs = JAXBUtils.getPackageNamespace(cls);
                if (packageNs != null) {
                    namespace = getNamespace(packageNs);
                }
            }
            String name = getLocalName(root.name(), cls.getSimpleName(), pluralName);
            return new QName(namespace, name);
        } else {
            JAXBContext context = getJAXBContext(cls, type);
            JAXBContextProxy proxy = ReflectionInvokationHandler.createProxyWrapper(context,
                                                                                    JAXBContextProxy.class);
            JAXBBeanInfo info = JAXBUtils.getBeanInfo(proxy, cls);
            if (info != null) {
                try {
                    Object instance = object == null ? cls.newInstance() : object;
                    String name = getLocalName(info.getElementLocalName(instance), cls.getSimpleName(), 
                                               pluralName);
                    String namespace = getNamespace(info.getElementNamespaceURI(instance));
                    return new QName(namespace, name);
                } catch (Exception ex) {
                    // ignore
                }
            }
        }
        return qname;
    }
    
    private String getLocalName(String name, String clsName, boolean pluralName) {
        if (JAXB_DEFAULT_NAME.equals(name)) {
            name = clsName;
            if (name.length() > 1) {
                name = name.substring(0, 1).toLowerCase() + name.substring(1); 
            } else {
                name = name.toLowerCase();
            }
        }
        if (pluralName) {
            name += 's';
        }
        return name;
    }
    
    private String getNamespace(String namespace) {
        if (JAXB_DEFAULT_NAMESPACE.equals(namespace)) {
            return "";
        }
        return namespace;
    }
    
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] anns, MediaType mt) {
        return marshalAsJaxbElement || isSupported(type, genericType, anns);
    }

    public void setSchemaLocations(List<String> locations) {
        schema = SchemaHandler.createSchema(locations, getBus());    
    }
    
    public void setSchema(Schema s) {
        schema = s;    
    }
    
    public long getSize(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mt) {
        return -1;
    }

    protected MessageContext getContext() {
        return mc;
    }
    
    @SuppressWarnings("unchecked")
    protected JAXBContext getJAXBContext(Class<?> type, Type genericType) throws JAXBException {
        if (mc != null) {
            ContextResolver<JAXBContext> resolver = 
                mc.getResolver(ContextResolver.class, JAXBContext.class);
            if (resolver != null) {
                JAXBContext customContext = resolver.getContext(type);
                if (customContext != null) {
                    return customContext;
                }
            }
        }
        
        synchronized (classContexts) {
            JAXBContext context = classContexts.get(type);
            if (context != null) {
                return context;
            }
        }
        
        JAXBContext context = getPackageContext(type);
                
        return context != null ? context : getClassContext(type);
    }
    
    public JAXBContext getClassContext(Class<?> type) throws JAXBException {
        synchronized (classContexts) {
            JAXBContext context = classContexts.get(type);
            if (context == null) {
                context = JAXBContext.newInstance(new Class[]{type}, cProperties);
                classContexts.put(type, context);
            }
            return context;
        }
    }
    
    public JAXBContext getPackageContext(Class<?> type) {
        if (type == null || type == JAXBElement.class) {
            return null;
        }
        synchronized (packageContexts) {
            String packageName = PackageUtils.getPackageName(type);
            JAXBContext context = packageContexts.get(packageName);
            if (context == null) {
                try {
                    if (type.getClassLoader() != null && objectFactoryOrIndexAvailable(type)) { 
                        context = JAXBContext.newInstance(packageName, type.getClassLoader(), cProperties);
                        packageContexts.put(packageName, context);
                    }
                } catch (JAXBException ex) {
                    LOG.fine("Error creating a JAXBContext using ObjectFactory : " 
                                + ex.getMessage());
                    return null;
                }
            }
            return context;
        }
    }
    
    protected boolean isSupported(Class<?> type, Type genericType, Annotation[] anns) {
        if (jaxbElementClassMap != null && jaxbElementClassMap.containsKey(type.getName())
            || isSkipJaxbChecks()) {
            return true;
        }
        return type.getAnnotation(XmlRootElement.class) != null
            || JAXBElement.class.isAssignableFrom(type)
            || objectFactoryOrIndexAvailable(type)
            || (type != genericType && objectFactoryForType(genericType))
            || getAdapter(type, anns) != null;
    
    }
    
    protected boolean objectFactoryOrIndexAvailable(Class<?> type) {
        return type.getResource("ObjectFactory.class") != null
               || type.getResource("jaxb.index") != null; 
    }
    
    private boolean objectFactoryForType(Type genericType) {
        return objectFactoryOrIndexAvailable(InjectionUtils.getActualType(genericType));
    }
    
    protected Unmarshaller createUnmarshaller(Class<?> cls, Type genericType) 
        throws JAXBException {
        return createUnmarshaller(cls, genericType, false);        
    }
    
    protected Unmarshaller createUnmarshaller(Class<?> cls, Type genericType, boolean isCollection) 
        throws JAXBException {
        JAXBContext context = isCollection ? getCollectionContext(cls) 
                                           : getJAXBContext(cls, genericType);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        if (schema != null) {
            unmarshaller.setSchema(schema);
        }
        if (uProperties != null) {
            for (Map.Entry<String, Object> entry : uProperties.entrySet()) {
                unmarshaller.setProperty(entry.getKey(), entry.getValue());
            }
        }
        return unmarshaller;        
    }
    
    protected Marshaller createMarshaller(Object obj, Class<?> cls, Type genericType, String enc)
        throws JAXBException {
        
        Class<?> objClazz = JAXBElement.class.isAssignableFrom(cls) 
                            ? ((JAXBElement)obj).getDeclaredType() : cls;
                            
        JAXBContext context = getJAXBContext(objClazz, genericType);
        Marshaller marshaller = context.createMarshaller();
        if (enc != null) {
            marshaller.setProperty(Marshaller.JAXB_ENCODING, enc);
        }
        return marshaller;
    }
    
        
    protected Class<?> getActualType(Class<?> type, Type genericType, Annotation[] anns) {
        Class<?> theType = null;
        if (JAXBElement.class.isAssignableFrom(type)) {
            theType = InjectionUtils.getActualType(genericType);
        } else {
            theType = type;
        }
        XmlJavaTypeAdapter adapter = getAdapter(theType, anns);
        if (adapter != null) {
            if (adapter.type() != XmlJavaTypeAdapter.DEFAULT.class) {
                theType = adapter.type();
            } else {
                Type[] types = InjectionUtils.getActualTypes(adapter.value().getGenericSuperclass());
                if (types != null && types.length == 2) {
                    theType = InjectionUtils.getActualType(types[0]);
                }
            }
        }
        
        return theType;
    }
    
    @SuppressWarnings("unchecked")
    protected Object checkAdapter(Object obj, Class<?> cls, Annotation[] anns, boolean marshal) {
        XmlJavaTypeAdapter typeAdapter = getAdapter(obj.getClass(), anns); 
        if (typeAdapter != null) {
            try {
                XmlAdapter xmlAdapter = typeAdapter.value().newInstance();
                if (marshal) {
                    return xmlAdapter.marshal(obj);
                } else {
                    return xmlAdapter.unmarshal(obj);
                }
            } catch (Exception ex) {
                LOG.warning("Problem using the XmlJavaTypeAdapter");
                ex.printStackTrace();
            }
        }
        return obj;
    }
    
    protected XmlJavaTypeAdapter getAdapter(Class<?> objectClass, Annotation[] anns) {
        XmlJavaTypeAdapter typeAdapter = AnnotationUtils.getAnnotation(anns, XmlJavaTypeAdapter.class);
        if (typeAdapter == null) {
            typeAdapter = objectClass.getAnnotation(XmlJavaTypeAdapter.class);
            if (typeAdapter == null) {
                // lets just try the 1st interface for now
                Class<?>[] interfaces = objectClass.getInterfaces();
                typeAdapter = interfaces.length > 0 
                    ? interfaces[0].getAnnotation(XmlJavaTypeAdapter.class) : null;
            }
        }
        return typeAdapter;
    }
    
    
    protected Schema getSchema() {
        return schema;
    }

    
    public static void clearContexts() {
        classContexts.clear();
        packageContexts.clear();
    }
    
    protected static void handleJAXBException(JAXBException e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        LOG.warning(sw.toString());
        StringBuilder sb = new StringBuilder();
        if (e.getMessage() != null) {
            sb.append(e.getMessage()).append(". ");
        }
        if (e.getCause() != null && e.getCause().getMessage() != null) {
            sb.append(e.getCause().getMessage()).append(". ");
        }
        if (e.getLinkedException() != null && e.getLinkedException().getMessage() != null) {
            sb.append(e.getLinkedException().getMessage()).append(". ");
        }
        Throwable t = e.getLinkedException() != null 
            ? e.getLinkedException() : e.getCause() != null ? e.getCause() : e;
        String message = new org.apache.cxf.common.i18n.Message("JAXB_EXCEPTION", 
                             BUNDLE, sb.toString()).toString();
        Response r = Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .type(MediaType.TEXT_PLAIN).entity(message).build();
        throw new WebApplicationException(t, r);
    }
    
    public void setOutTransformElements(Map<String, String> outElements) {
        this.outElementsMap = outElements;
    }
    
    public void setInAppendElements(Map<String, String> inElements) {
        this.inAppendMap = inElements;
    }
    
    public void setInTransformElements(Map<String, String> inElements) {
        this.inElementsMap = inElements;
    }
    
    public void setOutAppendElements(Map<String, String> map) {
        this.outAppendMap = map;
    }

    public void setOutDropElements(List<String> dropElementsSet) {
        this.outDropElements = dropElementsSet;
    }

    public void setInDropElements(List<String> dropElementsSet) {
        this.inDropElements = dropElementsSet;
    }
    
    
    
    public void setAttributesToElements(boolean value) {
        this.attributesToElements = value;
    }

    public void setSkipJaxbChecks(boolean skipJaxbChecks) {
        this.skipJaxbChecks = skipJaxbChecks;
    }

    public boolean isSkipJaxbChecks() {
        return skipJaxbChecks;
    }

    @XmlRootElement
    protected static class CollectionWrapper {
        
        @XmlAnyElement(lax = true)
        private List<?> l;
        
        public void setList(List<?> list) {
            l = list;
        }
        
        public List<?> getList() {
            if (l == null) {
                l = new ArrayList<Object>();
            }
            return l;
        }
        
        @SuppressWarnings("unchecked")
        public <T> Object getCollectionOrArray(Class<T> type, Class<?> origType) {
            List<?> theList = getList();
            if (theList.size() > 0) {
                Object first = theList.get(0);
                if (first instanceof JAXBElement && !JAXBElement.class.isAssignableFrom(type)) {
                    List<Object> newList = new ArrayList<Object>(theList.size());
                    for (Object o : theList) {
                        newList.add(((JAXBElement)o).getValue());
                    }
                    theList = newList;
                }
            }
            if (origType.isArray()) {
                T[] values = (T[])Array.newInstance(type, theList.size());
                for (int i = 0; i < theList.size(); i++) {
                    values[i] = (T)theList.get(i);
                }
                return values;
            } else if (origType == Set.class) {
                return new HashSet(theList);
            } else {
                return theList;
            }
        }
        
    }
    
    protected XMLStreamWriter createTransformWriterIfNeeded(XMLStreamWriter writer,
                                                            OutputStream os) {
        return TransformUtils.createTransformWriterIfNeeded(writer, os, 
                                                      outElementsMap,
                                                      outDropElements,
                                                      outAppendMap,
                                                      attributesToElements);
    }
    
    protected XMLStreamReader createTransformReaderIfNeeded(XMLStreamReader reader, InputStream is) {
        return TransformUtils.createTransformReaderIfNeeded(reader, is,
                                                            inDropElements,
                                                            inElementsMap,
                                                            inAppendMap,
                                                            true);
    }
}
