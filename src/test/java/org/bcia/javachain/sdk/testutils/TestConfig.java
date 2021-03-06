/*
 *  Copyright 2016, 2017 IBM, DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.bcia.javachain.sdk.testutils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bcia.javachain.sdk.helper.MspStore;
import org.bcia.javachain.sdk.helper.Utils;
import org.bcia.javachain.sdkintegration.SampleOrg;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Config allows for a global config of the toolkit. Central location for all
 * toolkit configuration defaults. Has a local config file that can override any
 * property defaults. Config file can be relocated via a system property
 * "org.bcia.javachain.sdk.configuration". Any property can be overridden
 * with environment variable and then overridden
 * with a java system property. Property hierarchy goes System property
 * overrides environment variable which overrides config file for default values specified here.
 */

/**
 * Test Configuration
 * modified by wangzhe 删掉fabric改动后的无用代码 2018/09/04
 */

public class TestConfig {
    private static final Log logger = LogFactory.getLog(TestConfig.class);

    private static final String ORG_HYPERLEDGER_FABRIC_SDK_CONFIGURATION = "org.bcia.javachain.sdktest.configuration";
    private static final String LOCALHOST = "localhost";

    private static final String PROPBASE = "org.bcia.javachain.sdktest.";

    private static final String INVOKEWAITTIME = PROPBASE + "InvokeWaitTime";
    private static final String INSTANTIATE_WAIT_TIME = PROPBASE + "InstantiateWaitTime";
    private static final String PROPOSALWAITTIME = PROPBASE + "ProposalWaitTime";

    private static final String INTEGRATIONTESTS_ORG = PROPBASE + "integrationTests.org.";
    private static final Pattern orgPat = Pattern.compile("^" + Pattern.quote(INTEGRATIONTESTS_ORG) + "([^\\.]+)\\.mspid$");

    private static TestConfig config;
    private static final Properties sdkProperties = new Properties();
    private final boolean runningTLS;
    public boolean isRunningFabricTLS() {
        return runningTLS;
    }
    private static final HashMap<String, SampleOrg> sampleOrgs = new HashMap<>();

    private TestConfig() {

        // Default values

        defaultProperty(INVOKEWAITTIME, "240");
        defaultProperty(INSTANTIATE_WAIT_TIME, "1200000");
        defaultProperty(PROPOSALWAITTIME, "12000");

        //////
        defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg1.mspid", "DEFAULT");
        defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg1.domname", "org1.example.com");
        defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg1.ca_location", "http://" + LOCALHOST + ":7054");
        defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg1.caName", "ca0");
        defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg1.peer_locations", "peer0.org1.example.com@grpc://" + LOCALHOST + ":7051");//, peer1.org1.example.com@grpc://" + LOCALHOST + ":7051
        defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg1.orderer_locations", "orderer.example.com@grpc://" + LOCALHOST + ":7050");
        defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg1.eventhub_locations", "peer0.org1.example.com@grpc://" + LOCALHOST + ":7053");//,peer1.org1.example.com@grpc://" + LOCALHOST + ":7058
//            defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg2.mspid", "Org2MSP");
//            defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg2.domname", "org2.example.com");
//            defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg2.ca_location", "http://" + LOCALHOST + ":8054");
//            defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg2.caName", "ca0");
//            defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg2.peer_locations", "peer0.org2.example.com@grpc://" + LOCALHOST + ":8051,peer1.org2.example.com@grpc://" + LOCALHOST + ":8051");
//            defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg2.orderer_locations", "orderer.example.com@grpc://" + LOCALHOST + ":7050");
//            defaultProperty(INTEGRATIONTESTS_ORG + "peerOrg2.eventhub_locations", "peer0.org2.example.com@grpc://" + LOCALHOST + ":8053, peer1.org2.example.com@grpc://" + LOCALHOST + ":8058");

        runningTLS = false;

        for (Map.Entry<Object, Object> x : sdkProperties.entrySet()) {
            final String key = x.getKey() + "";
            final String val = x.getValue() + "";

            if (key.startsWith(INTEGRATIONTESTS_ORG)) {

                Matcher match = orgPat.matcher(key);

                if (match.matches() && match.groupCount() == 1) {
                    String orgName = match.group(1).trim();
                    sampleOrgs.put(orgName, new SampleOrg(orgName, val.trim()));

                }
            }
        }

        for (Map.Entry<String, SampleOrg> org : sampleOrgs.entrySet()) {
            final SampleOrg sampleOrg = org.getValue();
            final String orgName = org.getKey();

            String peerNames = sdkProperties.getProperty(INTEGRATIONTESTS_ORG + orgName + ".peer_locations");
            String[] ps = peerNames.split("[ \t]*,[ \t]*");
            for (String peer : ps) {
                String[] nl = peer.split("[ \t]*@[ \t]*");
                sampleOrg.addNodeLocation(nl[0], grpcTLSify(nl[1]));
            }

            final String domainName = sdkProperties.getProperty(INTEGRATIONTESTS_ORG + orgName + ".domname");

            sampleOrg.setDomainName(domainName);

            String ordererNames = sdkProperties.getProperty(INTEGRATIONTESTS_ORG + orgName + ".orderer_locations");
            ps = ordererNames.split("[ \t]*,[ \t]*");
            for (String peer : ps) {
                String[] nl = peer.split("[ \t]*@[ \t]*");
                sampleOrg.addConsenterLocation(nl[0], grpcTLSify(nl[1]));
            }

            String eventHubNames = sdkProperties.getProperty(INTEGRATIONTESTS_ORG + orgName + ".eventhub_locations");
            ps = eventHubNames.split("[ \t]*,[ \t]*");
            for (String peer : ps) {
                String[] nl = peer.split("[ \t]*@[ \t]*");
                sampleOrg.addEventHubLocation(nl[0], grpcTLSify(nl[1]));
            }

            sampleOrg.setCALocation(httpTLSify(sdkProperties.getProperty((INTEGRATIONTESTS_ORG + org.getKey() + ".ca_location"))));

            sampleOrg.setCAName(sdkProperties.getProperty((INTEGRATIONTESTS_ORG + org.getKey() + ".caName")));

            if (runningTLS) {
                //没来得及改
//                    String cert = "src/test/fixture/sdkintegration/e2e-2Orgs/FAB_CONFIG_GEN_VERS/crypto-config/peerOrganizations/DNAME/ca/ca.DNAME-cert.pem"
//                            .replaceAll("DNAME", domainName).replaceAll("FAB_CONFIG_GEN_VERS", FAB_CONFIG_GEN_VERS);
                String cert = "";//仅防止报错
                File cf = new File(cert);
                if (!cf.exists() || !cf.isFile()) {
                    throw new RuntimeException("TEST is missing cert file " + cf.getAbsolutePath());
                }
                Properties properties = new Properties();
                properties.setProperty("pemFile", cf.getAbsolutePath());

                properties.setProperty("allowAllHostNames", "true"); //testing environment only NOT FOR PRODUCTION!

                sampleOrg.setCAProperties(properties);
            }
        }


    }

    private String grpcTLSify(String location) {
        location = location.trim();
        Exception e = Utils.checkGrpcUrl(location);
        if (e != null) {
            throw new RuntimeException(String.format("Bad TEST parameters for grpc url %s", location), e);
        }
        return runningTLS ?
                location.replaceFirst("^grpc://", "grpcs://") : location;
    }

    private String httpTLSify(String location) {
        location = location.trim();

        return runningTLS ?
                location.replaceFirst("^http://", "https://") : location;
    }

    /**
     * getConfig return back singleton for SDK configuration.
     *
     * @return Global configuration
     */
    public static TestConfig getConfig() {
        if (null == config) {
            config = new TestConfig();
        }
        return config;

    }

    /**
     * getProperty return back property for the given value.
     *
     * @param property
     * @return String value for the property
     */
    private String getProperty(String property) {

        String ret = sdkProperties.getProperty(property);

        if (null == ret) {
            logger.warn(String.format("No configuration value found for '%s'", property));
        }
        return ret;
    }

    private static void defaultProperty(String key, String value) {

        String ret = System.getProperty(key);
        if (ret != null) {
            sdkProperties.put(key, ret);
        } else {
            String envKey = key.toUpperCase().replaceAll("\\.", "_");
            ret = System.getenv(envKey);
            if (null != ret) {
                sdkProperties.put(key, ret);
            } else {
                if (null == sdkProperties.getProperty(key) && value != null) {
                    sdkProperties.put(key, value);
                }

            }

        }
    }

    public int getTransactionWaitTime() {
        return Integer.parseInt(getProperty(INVOKEWAITTIME));
    }

    public int getInstantiateWaitTime() {
        return Integer.parseInt(getProperty(INSTANTIATE_WAIT_TIME));
    }

    public long getProposalWaitTime() {
        return Integer.parseInt(getProperty(PROPOSALWAITTIME));
    }

    public Collection<SampleOrg> getIntegrationTestsSampleOrgs() {
        return Collections.unmodifiableCollection(sampleOrgs.values());
    }

    public SampleOrg getIntegrationTestsSampleOrg(String name) {
        return sampleOrgs.get(name);

    }

    public Properties getNodeProperties(String name) {

        return getEndPointProperties("peer", name);

    }

    public Properties getConsenterProperties(String name) {

        return getEndPointProperties("orderer", name);

    }

    private Properties getEndPointProperties(final String type, final String name) {

        final String domainName = getDomainName(name);
        //得到证书
        byte[] permBytes = MspStore.getInstance().getClientCerts().get(0);
//        File cert = Paths.get(getTestGroupPath(), "crypto-config/ordererOrganizations".replace("orderer", type), domainName, type + "s",
//                name, "tls/server.crt").toFile();
//        if (!cert.exists()) {
//            throw new RuntimeException(String.format("Missing cert file for: %s. Could not find at location: %s", name,
//                    cert.getAbsolutePath()));
//        }

        Properties ret = new Properties();
        ret.put("pemBytes", permBytes);
        //      ret.setProperty("trustServerCertificate", "true"); //testing environment only NOT FOR PRODUCTION!
        ret.setProperty("hostnameOverride", name);
        ret.setProperty("sslProvider", "openSSL");
        ret.setProperty("negotiationType", "plainText");//先不TLS

        return ret;
    }

    public Properties getEventHubProperties(String name) {

        return getEndPointProperties("peer", name); //uses same as named peer

    }

    public boolean isRunningAgainstFabric10() {

        return "IntegrationSuiteV1.java".equals(System.getProperty("org.bcia.javachain.sdktest.ITSuite"));

    }

    /**
     * url location of configtxlator
     *
     * @return
     */

    public String getFabricConfigTxLaterLocation() {
        return "http://" + LOCALHOST + ":7059";
    }

    /**
     * Returns the appropriate Network Config YAML file based on whether TLS is currently
     * enabled or not
     *
     * @return The appropriate Network Config YAML file
     */
    public File getTestNetworkConfigFileYAML() {
        String fname = runningTLS ? "network-config-tls.yaml" : "network-config.yaml";
        String pname = "src/test/fixture/sdkintegration/network_configs/";
        File ret = new File(pname, fname);

        if (!"localhost".equals(LOCALHOST)) {
            // change on the fly ...
            File temp = null;

            try {
                //create a temp file
                temp = File.createTempFile(fname, "-FixedUp.yaml");

                if (temp.exists()) { //For testing start fresh
                    temp.delete();
                }

                byte[] data = Files.readAllBytes(Paths.get(ret.getAbsolutePath()));

                String sourceText = new String(data, StandardCharsets.UTF_8);

                sourceText = sourceText.replaceAll("https://localhost", "https://" + LOCALHOST);
                sourceText = sourceText.replaceAll("http://localhost", "http://" + LOCALHOST);
                sourceText = sourceText.replaceAll("grpcs://localhost", "grpcs://" + LOCALHOST);
                sourceText = sourceText.replaceAll("grpc://localhost", "grpc://" + LOCALHOST);

                Files.write(Paths.get(temp.getAbsolutePath()), sourceText.getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);

//                if (!Objects.equals("true", System.getenv(System.getenv(ORG_HYPERLEDGER_FABRIC_SDK_TEST_FABRIC_HOST + "_KEEP")))) {
//                    temp.deleteOnExit();
//                } else {
                    //保留
                    System.err.println("produced new network-config.yaml file at:" + temp.getAbsolutePath());
//                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            ret = temp;
        }

        return ret;
    }

    private String getDomainName(final String name) {
        int dot = name.indexOf(".");
        if (-1 == dot) {
            return null;
        } else {
            return name.substring(dot + 1);
        }

    }

}
