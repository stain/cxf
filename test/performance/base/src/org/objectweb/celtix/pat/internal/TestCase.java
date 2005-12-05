package org.objectweb.celtix.pat.internal;

import java.util.*;

import javax.xml.namespace.QName;

import org.objectweb.celtix.Bus;
import org.objectweb.celtix.BusException;

public abstract class TestCase {
	private String name;

	private String[] args;

	protected String wsdlPath;

	protected String serviceName;

	protected String portName;

	protected String operationName;

	protected String hostname = null;

	protected String hostport = null;

	protected int packetSize = 1;

	private int numberOfThreads = 0;

	protected boolean usingTime = false;

	protected int amount = 1;

	protected String WSDL_NAME_SPACE;

	protected Bus bus = null;

	private static boolean initialized = false;

	protected ArrayList results = new ArrayList();

	private String faultReason;

	private QName serviceQName = null;

	public TestCase() {
		this("DEFAULT TESTCASE", null);
	}

	public TestCase(String name) {
		this(name, null);
	}

	public TestCase(String name, String[] args) {
		this.name = name;
		this.args = args;
	}

	public abstract void initTestData();

	public void init() throws Exception {		
		initBus();
		initTestData();
	}

	private void processArgs() {
		int count = 0;
		int argc = args.length; 
		
		while (true) {
			if (count >= argc) {
				break;
			}
			if ("-WSDL".equals(args[count])) {
				wsdlPath = args[count + 1];				
				count += 2;
			} else if ("-Service".equals(args[count])) {
				serviceName = args[count + 1];
				count += 2;
			} else if ("-Port".equals(args[count])) {
				portName = args[count + 1];
				count += 2;
			} else if ("-Operation".equals(args[count])) {
				operationName = args[count + 1];
				count += 2;
			} else if ("-BasedOn".equals(args[count])) {
				if ("Time".equals(args[count + 1])) {
					usingTime = true;
				}
				count += 2;
			} else if ("-Amount".equals(args[count])) {
				amount = Integer.parseInt(args[count + 1]);
				count += 2;
			} else if ("-Threads".equals(args[count])) {
				numberOfThreads = Integer.parseInt(args[count + 1]);
				count += 2;
			} else if ("-HostName".equals(args[count])) {
				hostname = args[count + 1];
				count += 2;
			} else if ("-HostPort".equals(args[count])) {
				hostport = args[count + 1];
				count += 2;
			} else if ("-PacketSize".equals(args[count])) {
				packetSize = Integer.parseInt(args[count + 1]);
				count += 2;
			} else {
				count++;
			}
		}
	}

	private boolean validate() {
		
		if (WSDL_NAME_SPACE == null || WSDL_NAME_SPACE.trim().equals("")) {
			System.out.println("WSDL name space is not specified");
			faultReason = "Missing WSDL name space";
			return false;
		}
		if (serviceName == null || serviceName.trim().equals("")) {
			System.out.println("Service name is not specified");
			faultReason = "Missing Service name";
			return false;
		}

		if (portName == null || portName.trim().equals("")) {
			System.out.println("Port name is not specified");
			faultReason = "Missing Port name";
			return false;
		}

		if (wsdlPath == null || wsdlPath.trim().equals("")) {
			System.out.println("WSDL path is not specifed");
			faultReason = "Missing WSDL path";
			return false;
		}		

		return true;
	}

	// for the celtix init , here do nothing
	private void initBus() throws BusException {
		bus = Bus.init();
	}
	
	private String replaceEndpoint(String old, String hostname, String hostport) {
		if (old == null)
			return "http://" + hostname + ":" + hostport;
		System.out.println("Old Endpoint:" + old);

		String tail = old.split("//", 2)[1].split("/", 2)[1];
		StringBuffer sb = new StringBuffer();
		sb.append("http://");
		sb.append(hostname);
		sb.append(":");
		sb.append(hostport);
		sb.append("/");
		sb.append(tail);
		System.out.println("New Endpoint: " + sb.toString());
		return sb.toString();
	}

	public void tearDown() throws BusException {
		System.out.println("Bus is going to shutdown");
		if(bus!=null){
			bus.shutdown(true);
		}
	}

	private void setUp() throws Exception {
		
		processArgs();
		if(!validate()){
			System.out.println("Configure Exception!");
			System.exit(1);
		}
			
		clearTestResults();
		printTitle();

		printSetting("Default Setting: ");
		init();
		printSetting("Runtime Setting: ");
	}

	public void initialize() {
		try {
			if (!initialized)
				setUp();
			initialized = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public abstract void doJob();

	public abstract void getPort();

	protected void internalTestRun(String name) throws Exception {
		int numberOfInvocations = 0;

		long startTime = System.currentTimeMillis();
		long endTime = startTime + amount * 1000;
		getPort();

		if (usingTime) {
			while (System.currentTimeMillis() < endTime) {
				doJob();
				numberOfInvocations++;
			}
		} else {
			for (int i = 0; i < amount; i++) {
				doJob();
				numberOfInvocations++;
			}
		}
		endTime = System.currentTimeMillis();

		TestResult testResult = new TestResult(name, this);
		testResult.compute(startTime, endTime, numberOfInvocations);
		addTestResult(testResult);
	}

	public void testRun() throws Exception {
		if (numberOfThreads == 0) {
			internalTestRun(name);
		}

		ArrayList threadList = new ArrayList();
		for (int i = 0; i < numberOfThreads; i++) {
			TestRunner runner = new TestRunner("No." + i + " TestRunner", this);
			Thread thread = new Thread(runner, "RunnerThread No." + i);
			thread.start();

			threadList.add(thread);
		}

		for (Iterator iter = threadList.iterator(); iter.hasNext();) {
			Thread thread = (Thread) iter.next();
			try {
				thread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void run() {
		try {
			System.out.println("TestCase " + name + " is running");
			testRun();
			tearDown();
			System.out.println("TestCase " + name + " is finished");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void clearTestResults() {
		results.clear();
	}

	protected void addTestResult(TestResult result) {
		results.add(result);
	}

	public List getTestResults() {
		return results;
	}

	public abstract void printUsage();

	public void printSetting(String settingType) {

		System.out.println(settingType + "  [Service] -- > " + serviceName);
		System.out.println(settingType + "  [Port] -- > " + portName);
		System.out.println(settingType + "  [Operation] -- > " + operationName);
		System.out.println(settingType + "  [Threads] -- > " + numberOfThreads);
		System.out.println(settingType + "  [Packet Size] -- > " + packetSize
                           + " packet(s) ");
		if (usingTime) {
			System.out.println(settingType + "  [Running] -->  " + amount
                               + " (secs)");
		} else {
			System.out.println(settingType + "  [Running] -->  " + amount
                               + " (invocations)");
		}
	}

	public void printTitle() {
		System.out.println(" ---------------------------------");
		System.out.println(name + "  Client (JAVA Version)");
		System.out.println(" ---------------------------------");
	}

	public void setWSDLNameSpace(String nameSpace) {
		this.WSDL_NAME_SPACE = nameSpace;
	}

	public void setWSDLPath(String wsdlPath) {
		this.wsdlPath = wsdlPath;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public void setPortName(String portName) {
		this.portName = portName;
	}

	public void setOperationName(String operationName) {
		this.operationName = operationName;
	}

	public String getServiceName() {
		return this.serviceName;
	}

	public String getPortName() {
		return this.portName;
	}

	public String getOperationName() {
		return this.operationName;
	}

	public String getName() {
		return this.name;
	}
	
	public Bus getBus() {
		return this.bus;
	}

	public void setServiceQName(QName serviceQName) {
		this.serviceQName = serviceQName;
	}
}
