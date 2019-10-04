/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.am.integration.tests.other;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.clients.publisher.api.v1.dto.APIDTO;
import org.wso2.am.integration.clients.publisher.api.v1.dto.WorkflowResponseDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyDTO;
import org.wso2.am.integration.clients.store.api.v1.dto.ApplicationKeyGenerateRequestDTO;
import org.wso2.am.integration.test.Constants;
import org.wso2.am.integration.test.utils.base.APIMIntegrationBaseTest;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.APILifeCycleState;
import org.wso2.am.integration.test.utils.bean.APILifeCycleStateRequest;
import org.wso2.am.integration.test.utils.bean.APIRequest;
import org.wso2.am.integration.test.utils.bean.APPKeyRequestGenerator;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.integration.test.utils.clients.APIPublisherRestClient;
import org.wso2.am.integration.test.utils.clients.APIStoreRestClient;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import static org.testng.Assert.assertNotNull;

public class LocationHeaderTestCase extends APIMIntegrationBaseTest {

    private static final Log log = LogFactory.getLog(LocationHeaderTestCase.class);
    private String applicationId;
    private String apiId;

    @BeforeClass(alwaysRun = true)
    public void setEnvironment() throws Exception {
        super.init();
        String gatewaySessionCookie = createSession(gatewayContextMgt);

        //Load the back-end dummy API
        loadSynapseConfigurationFromClasspath("artifacts" + File.separator + "AM"
                                              + File.separator + "synapseconfigs" + File.separator + "rest"
                                              + File.separator + "dummy_api_loc_header.xml", gatewayContextMgt, gatewaySessionCookie);
    }

    @Test(groups = "wso2.am", description = "Check whether the Location header is correct")
    public void testAPIWithLocationHeader() throws Exception {

        String apiName = "LocationHeaderAPI";
        String apiVersion = "1.0.0";
        String apiContext = "locheader";
        String endpointUrl = getAPIInvocationURLHttp("response");

        //Create the api creation request object
        APIRequest apiRequest;
        apiRequest = new APIRequest(apiName, apiContext, new URL(endpointUrl));

        apiRequest.setVersion(apiVersion);
        apiRequest.setTiersCollection(APIMIntegrationConstants.API_TIER.UNLIMITED);
        apiRequest.setTier(APIMIntegrationConstants.API_TIER.UNLIMITED);

        APIDTO createdAPI = restAPIPublisher.addAPI(apiRequest, "v2");
        apiId = createdAPI.getId();

        WorkflowResponseDTO lifecycleChangeResponse = restAPIPublisher.changeAPILifeCycleStatus(
                createdAPI.getId(), Constants.PUBLISHED);
        Assert.assertNotNull(lifecycleChangeResponse.getLifecycleState());
        Assert.assertEquals(lifecycleChangeResponse.getLifecycleState().getState(),
                APILifeCycleState.PUBLISHED.getState(),
                "Lifecycle State was not changed to " + APILifeCycleState.PUBLISHED.getState());


        HttpResponse appCreateResponse = restAPIStore.createApplication("LocHeaderAPP", "",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, ApplicationDTO.TokenTypeEnum.OAUTH);
        applicationId = appCreateResponse.getData();

        //Subscribe the API to the LocHeaderAPP
        HttpResponse subscriptionResponse = restAPIStore.createSubscription(createdAPI.getId(), applicationId,
                APIMIntegrationConstants.API_TIER.UNLIMITED);

        ArrayList<String> grantTypes = new ArrayList<>();
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.PASSWORD);
        grantTypes.add(APIMIntegrationConstants.GRANT_TYPE.CLIENT_CREDENTIAL);
        ApplicationKeyDTO applicationKeyDTO = restAPIStore
                .generateKeys(applicationId, "36000", "",
                        ApplicationKeyGenerateRequestDTO.KeyTypeEnum.PRODUCTION, null, grantTypes);

        assertNotNull(applicationKeyDTO.getToken(), "Generated Keys doesn't include a token");
        //Get the accessToken which was generated.
        String accessToken = applicationKeyDTO.getToken().getAccessToken();
        assertNotNull(accessToken, "Production access token is Empty");

        //Going to access the API with the version in the request url.
        String apiInvocationUrl = getAPIInvocationURLHttp(apiContext, apiVersion);

        HttpClient httpclient = new DefaultHttpClient();
        HttpUriRequest get = new HttpGet(apiInvocationUrl);
        get.addHeader(new BasicHeader("Authorization", "Bearer " + accessToken));

        waitForAPIDeploymentSync(apiRequest.getProvider(), apiRequest.getName(), apiRequest.getVersion(),
                                 APIMIntegrationConstants.IS_API_EXISTS);

        org.apache.http.HttpResponse httpResponse = httpclient.execute(get);
        Header locationHeader = httpResponse.getFirstHeader("Location");

        Assert.assertFalse(locationHeader.getValue().endsWith("//abc/domain"),
                           "Location header contains additional / character");
        Assert.assertTrue(locationHeader.getValue().endsWith("/abc/domain"),
                          "Unexpected Location header. Expected to end with "
                          + "/abc/domain but received " + locationHeader);

    }

    @AfterClass(alwaysRun = true)
    public void destroy() throws Exception {
        restAPIStore.deleteApplication(applicationId);
        restAPIPublisher.deleteAPI(apiId);
        super.cleanUpUsingRest();
    }
}
