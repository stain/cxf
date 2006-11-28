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

package org.apache.cxf.systest.js;

import java.io.File;

import org.apache.cxf.js.rhino.ProviderFactory;
import org.apache.cxf.systest.common.TestServerBase;

public class Server extends TestServerBase {

    protected void run()  {
        
        try {            
            ProviderFactory pf = new ProviderFactory();            
            String f = getClass().getResource("resources/hello_world.js").getFile();
            pf.createAndPublish(new File(f), "http://localhost:9000/SoapContext/SoapPort", false);
            f = getClass().getResource("resources/hello_world.jsx").getFile();
            pf.createAndPublish(new File(f), "http://localhost:9100", false);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            System.err.println("Server main");
            Server s = new Server();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
