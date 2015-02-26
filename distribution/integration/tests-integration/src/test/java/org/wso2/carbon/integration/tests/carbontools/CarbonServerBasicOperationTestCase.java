/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.integration.tests.carbontools;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.ContextXpathConstants;
import org.wso2.carbon.integration.common.exception.CarbonToolsIntegrationTestException;
import org.wso2.carbon.integration.common.utils.CarbonCommandToolsUtil;
import org.wso2.carbon.integration.common.utils.CarbonIntegrationBaseTest;
import org.wso2.carbon.utils.ServerConstants;
import sun.management.VMManagement;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import static org.testng.Assert.assertTrue;

/**
 * This class has the test methods for start, stop, restart, dump and run build.xml test cases
 */
public class CarbonServerBasicOperationTestCase extends CarbonIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(CarbonServerBasicOperationTestCase.class);
    private HashMap<String, String> serverPropertyMap = new HashMap<String, String>();
    private String carbonHome;
    private AutomationContext context;
    private final int portOffset = 1;
    private String processId;
    Process processStop;

    @BeforeClass(alwaysRun = true)
    public void init() throws CarbonToolsIntegrationTestException, XPathExpressionException {
        serverPropertyMap.put("-DportOffset", Integer.toString(portOffset));
        context = new AutomationContext("CARBON", "carbon002",
                                        ContextXpathConstants.SUPER_TENANT,
                                        ContextXpathConstants.ADMIN);
        carbonHome = CarbonCommandToolsUtil.getCarbonHome(context);
    }

    @Test(groups = {"wso2.as"}, description = "Server start test")
    public void testStartCommand() throws CarbonToolsIntegrationTestException,
                                          NoSuchFieldException, IllegalAccessException {
        String[] cmdArrayToStart;
        Process process;

        if (CarbonCommandToolsUtil.isCurrentOSWindows()) {
            throw new SkipException("Feature --start is not available for windows");
        } else {
            cmdArrayToStart = new String[]
                    {"sh", "wso2server.sh", "--start", "-DportOffset=" + portOffset};
            process = CarbonCommandToolsUtil.runScript(carbonHome + "/bin", cmdArrayToStart);
        }
        boolean startupStatus = CarbonCommandToolsUtil.isServerStartedUp(context, portOffset);
        // Waiting until start the server

        Field field = process.getClass().getDeclaredField("pid");
        field.setAccessible(true);
        processId = field.get(process).toString();
        log.info("process id for carbon server with offset 1 : " + processId);

        assertTrue(startupStatus, "Unsuccessful login");
    }

    @Test(groups = {"wso2.as"}, description = "Server start test", dependsOnMethods = {"testStartCommand"})
    public void testDumpCommandOnLinux() throws CarbonToolsIntegrationTestException {
        String[] cmdArray;
        Process processDump = null;
        try {
            if (CarbonCommandToolsUtil.isCurrentOSWindows()) {
                throw new SkipException("Feature --start is not available for windows");
                // Since we are skipping --start feature it won
            } else {
                cmdArray = new String[]
                        {"sh", "carbondump.sh", "-carbonHome", carbonHome, "-pid", processId};
                processDump = CarbonCommandToolsUtil.runScript(carbonHome + "/bin", cmdArray);
            }
            assertTrue(isFoundDumpFile(carbonHome), "Couldn't find the dump file");
        } finally {
            processDump.destroy();
        }

    }

    @Test(groups = {"wso2.as"}, description = "Server restart test",
            dependsOnMethods = {"testDumpCommandOnLinux"})
    public void testRestartCommand()
            throws CarbonToolsIntegrationTestException, InterruptedException {
        String[] cmdArrayToReStart;
        if (CarbonCommandToolsUtil.isCurrentOSWindows()) {
            throw new SkipException("Feature --restart is not available for windows");
        } else {
            cmdArrayToReStart = new String[]
                    {"sh", "wso2server.sh", "--restart", "-DportOffset=" + portOffset};
            CarbonCommandToolsUtil.runScript(carbonHome + "/bin", cmdArrayToReStart);
        }

        boolean isServerDown = CarbonCommandToolsUtil.isServerDown(context, portOffset);
        assertTrue(isServerDown, "Shutting down the server failed");

        boolean isServerUp = CarbonCommandToolsUtil.isServerStartedUp(context, portOffset);

        assertTrue(isServerUp, "Unsuccessful login");
    }

    @Test(groups = {"wso2.as"}, description = "Server stop test",
            dependsOnMethods = {"testRestartCommand"})
    public void testStopCommand() throws CarbonToolsIntegrationTestException, InterruptedException {
        String[] cmdArray;
        boolean startupStatus = false;
        try {
            if (CarbonCommandToolsUtil.isCurrentOSWindows()) {
                throw new SkipException("Feature --stop is not available for windows");
            } else {
                cmdArray = new String[]{"sh", "wso2server.sh", "--stop", "-DportOffset=" + portOffset};
                processStop = CarbonCommandToolsUtil.runScript(carbonHome + "/bin", cmdArray);
            }
            startupStatus = CarbonCommandToolsUtil.isServerDown(context, portOffset);
        } finally {
            if (processStop != null) {
                processStop.destroy();
            }
        }
        assertTrue(startupStatus, "Unsuccessful login");
    }

    @Test(groups = {"wso2.as"}, description = "Server stop test")
    public void testDumpCommandOnWindows()
            throws CarbonToolsIntegrationTestException, NoSuchFieldException,
                   IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Process processDump = null;
        String carbonHome = System.getProperty(ServerConstants.CARBON_HOME);
        try {
            if (CarbonCommandToolsUtil.isCurrentOSWindows()) {
                RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
                Field jvmField = runtimeMXBean.getClass().getDeclaredField("jvm");
                jvmField.setAccessible(true);
                VMManagement vmManagement = (VMManagement) jvmField.get(runtimeMXBean);
                Method getProcessIdMethod = vmManagement.getClass().getDeclaredMethod("getProcessId");
                getProcessIdMethod.setAccessible(true);
                Integer processId = (Integer) getProcessIdMethod.invoke(vmManagement);
                String[] cmdArray = new String[]
                        {"cmd.exe", "/c", "carbondump.bat", "-carbonHome"
                                , carbonHome, "-pid", Integer.toString(processId)};
                processDump = CarbonCommandToolsUtil.runScript(carbonHome + "/bin", cmdArray);
                assertTrue(isFoundDumpFile(carbonHome), "Couldn't find the dump file");
            } else {
                throw new SkipException(" This test method is only for windows");
            }
        } finally {
            if (processDump != null) {
                processDump.destroy();
            }
        }
    }

    @AfterClass(alwaysRun = true)
    public void serverShutDown() throws CarbonToolsIntegrationTestException {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    CarbonCommandToolsUtil.serverShutdown(1, context);
                } catch (Exception e) {
                    log.error("Error while server shutdown ..", e);
                }
            }
        });
    }

    private boolean isFoundDumpFile(String carbonHome) {
        boolean isFoundDumpFolder = false;
        File folder = new File(carbonHome);
        long startTime = System.currentTimeMillis();
        long timeout = 10000;
        while ((System.currentTimeMillis() - startTime) < timeout) {
            if (folder.exists() && folder.isDirectory()) {
                File[] listOfFiles = folder.listFiles();
                for (File file : listOfFiles) {
                    if (file.getName().contains("carbondump") && file.getName().contains("zip")) {
                        double bytes = file.length();
                        double kilobytes = (bytes / 1024);
                        if (kilobytes > 0) {
                            log.info("carbon bump file name " + file.getName());
                            isFoundDumpFolder = true;
                        } else {

                        }
                    }
                }
                if (isFoundDumpFolder) {
                    break;
                }
            }
        }
        return isFoundDumpFolder;
    }


}
