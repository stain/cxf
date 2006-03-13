package org.objectweb.celtix.routing;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.wsdl.WSDLException;

import junit.framework.TestCase;

import org.objectweb.celtix.Bus;
import org.objectweb.celtix.common.i18n.Exception;
import org.objectweb.celtix.wsdl.WSDLManager;

public class RouterManagerTest extends TestCase {
    private Map<String, Object> properties;
    public void setUp() {
        properties = new HashMap<String, Object>();
    }

    public void tearDown() throws Exception {
        Bus bus = Bus.getCurrent();
        bus.shutdown(true);
        Bus.setCurrent(null);
        
        System.clearProperty("celtix.config.file");
    }
    
    public void testGetRouterWSDLList() throws Exception {
        URL routerConfigFileUrl = getClass().getResource("resources/router_config1.xml");
        System.setProperty("celtix.config.file", routerConfigFileUrl.toString());
        
        properties.put("org.objectweb.celtix.BusId", "celtix1");
        Bus bus = Bus.init(null, properties);
        Bus.setCurrent(bus);
        
        org.objectweb.celtix.routing.RouterManager rm = new RouterManager(bus);
        List<String> urlList = rm.getRouteWSDLList();
        
        assertNotNull("a valid list should be present", urlList);
        assertEquals(1, urlList.size());
    }

    public void testInit() throws Exception {

        URL routerConfigFileUrl = getClass().getResource("resources/router_config2.xml");
        System.setProperty("celtix.config.file", routerConfigFileUrl.toString());
        
        properties.put("org.objectweb.celtix.BusId", "celtix2");
        Bus bus = Bus.init(null, properties);
        Bus.setCurrent(bus);
        
        org.objectweb.celtix.routing.RouterManager rm = new RouterManager(bus);
        rm.init();

        List<String> urlList = rm.getRouteWSDLList();
        WSDLManager wsdlManager = Bus.getCurrent().getWSDLManager();
        
        try {
            for (String wsdlUrl : urlList) {
                URL url = getClass().getResource(wsdlUrl);
                assertNotNull("Should have a valid wsdl definition", 
                              wsdlManager.getDefinition(url));
            }
        } catch (WSDLException we) {
            fail("Should not have thrown a wsdl exception");
        }
        
        assertNotNull("Router Factory should be intialized", rm.getRouterFactory());
    }
    
    public static void main(String[] args) {
        junit.textui.TestRunner.run(RouterManagerTest.class);
    }
}
