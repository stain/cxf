package org.objectweb.celtix.tools.extensions.jms;

import org.w3c.dom.Element;

import org.objectweb.celtix.tools.common.ToolConstants;
import org.objectweb.celtix.tools.utils.XMLUtil;

public class JMSAddressParser {

    public void parseElement(JMSAddress jmsAddress, Element element) {
        String jndiURL = XMLUtil.getAttribute(element, ToolConstants.JMS_ADDR_JNDI_URL).getValue();
        if (jndiURL != null) {
            jmsAddress.setJndiProviderURL(jndiURL);
        }
    }
}
