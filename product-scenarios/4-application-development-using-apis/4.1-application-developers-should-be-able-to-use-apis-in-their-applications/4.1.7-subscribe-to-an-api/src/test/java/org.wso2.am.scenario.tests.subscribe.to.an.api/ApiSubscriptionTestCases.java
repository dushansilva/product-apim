package org.wso2.am.scenario.tests.subscribe.to.an.api;

import org.apache.axis2.AxisFault;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;

import static org.apache.axis2.context.MessageContext.UTF_8;

public class ApiSubscriptionTestCases extends ScenarioTestBase {

    private APIStoreRestClient apiStoreRestClient;
    private APIPublisherRestClient apiPublisherRestClient;
    private static final String ADMIN_LOGIN_USERNAME = "admin";
    private static final String ADMIN_LOGIN_PW = "admin";
    private static final String DEFAULT_STORE_URL = "https://localhost:9443/";
    private static final String DEFAULT_PUBLISHER_URL = "https://localhost:9443/publisher";
    private static final String USER_MANAGEMENT_BACKEND_URL = "https://localhost:9443/services/";

    private UserManagementClient userManagementClient;
    private String apiName;
    private String apiVersion;
    private String apiProvider;
    private String tierCollection;
    private String apiContext;
    private String applicationName;

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException, AxisFault {

        String storeURL;

        Properties infraProperties = getDeploymentProperties();
        String publisherURL = infraProperties.getProperty(PUBLISHER_URL);

        if (publisherURL == null) {
            publisherURL = DEFAULT_PUBLISHER_URL;
        }

        setKeyStoreProperties();
        apiPublisherRestClient = new APIPublisherRestClient(publisherURL);
        apiPublisherRestClient.login("admin", "admin");

        storeURL = infraProperties.getProperty(STORE_URL);
        if (storeURL == null) {
            storeURL = DEFAULT_STORE_URL;
        }
        setKeyStoreProperties();
        apiStoreRestClient = new APIStoreRestClient(storeURL);
        apiStoreRestClient.login(ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);

        userManagementClient = new UserManagementClient(USER_MANAGEMENT_BACKEND_URL,
                ADMIN_LOGIN_USERNAME,ADMIN_LOGIN_PW);

    }

    @Test
    public void testSubscribeToApiFromAppName() throws APIManagerIntegrationTestException,
            UnsupportedEncodingException {

        apiProvider = "admin";
        apiName = "PhoneVerificationOptionalAdd";
        apiContext = "/phoneverifyOptionaladd";
        apiVersion = "1.0.0";
        tierCollection = "Silver";
        applicationName = "app1";

        if (isApplicationCreated(applicationName)) {
            verifyResponse(deleteApplication(applicationName));
        }
        if (isApiCreated(apiName)) {
            verifyResponse(deleteApi(apiName, apiVersion, apiProvider));
        }

        verifyResponse(createApi());
        verifyResponse(publishApi(apiProvider, apiName, apiVersion));
        assertTrue(isApiCreated(apiName), "Api was not created");


        verifyResponse(createApplication(applicationName, "",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED));
        assertTrue(isApplicationCreated(applicationName));

        verifyResponse(subscribeToApplication(apiName, apiVersion, apiProvider, applicationName));
        assertTrue(isSubscriptionSuccessful(applicationName), "Subscription was not successful");

        verifyResponse(deleteApplication(applicationName));
        assertFalse(isApplicationCreated(applicationName), "Application was not deleted");

        verifyResponse(deleteApi(apiName, apiVersion, apiProvider));
        assertFalse(isApiCreated(apiName), "Api was not deleted");
    }

    public void createTenant(){

    }


    private HttpResponse subscribeToApplication(String apiName, String apiVersion, String apiProvider,
                                                String applicationName) throws APIManagerIntegrationTestException {

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, apiVersion, apiProvider,
                applicationName, tierCollection);
        return apiStoreRestClient.subscribe(subscriptionRequest);

    }

    private boolean isSubscriptionSuccessful(String applicationName) throws APIManagerIntegrationTestException {

        HttpResponse allSubscriptions = apiStoreRestClient.getAllSubscriptionsOfApplication(applicationName);
        verifyResponse(allSubscriptions);
        JSONObject jsonObject = new JSONObject(allSubscriptions.getData());
        return Integer.parseInt(jsonObject.getJSONObject("subscriptions").get("totalLength").toString()) == 1;
    }

    private boolean isApplicationCreated(String apiName) throws APIManagerIntegrationTestException {

        HttpResponse applications = apiStoreRestClient.getApplications();
        verifyResponse(applications);

        return applications.getData().contains(apiName);
    }

    private HttpResponse deleteApplication(String applicationName) throws APIManagerIntegrationTestException {

        return apiStoreRestClient.removeApplication(applicationName);
    }

    private HttpResponse deleteApi(String apiName, String apiVersion, String apiProvider) throws
            APIManagerIntegrationTestException {

        return apiPublisherRestClient.deleteAPI(apiName, apiVersion, apiProvider);
    }

    private HttpResponse createApi() throws APIManagerIntegrationTestException {

        String apiResource = "/find";
        String apiVisibility = "public";

        String description = "This is a API creation description";
        String tag = "APICreationTag";
        String bizOwner = "wso2Test";
        String bizOwnerMail = "wso2test@gmail.com";
        String techOwner = "wso2";
        String techOwnerMail = "wso2@gmail.com";
        String endpointType = "secured";
        String endpointAuthType = "basicAuth";
        String epUsername = "wso2";
        String epPassword = "wso2123";
        String default_version_checked = "default_version";
        String responseCache = "enabled";
        String cacheTimeout = "300";
        String subscriptions = "all_tenants";
        String http_checked = "http";
        String https_checked = "";
        String inSequence = "debug_in_flow";
        String outSequence = "debug_out_flow";

        String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";

        APIRequest apiRequest = new APIRequest(apiName, apiContext, apiVisibility, apiVersion, apiResource,
                description, tag, tierCollection, backendEndPoint, bizOwner, bizOwnerMail, techOwner, techOwnerMail,
                endpointType, endpointAuthType, epUsername, epPassword, default_version_checked, responseCache,
                cacheTimeout, subscriptions, http_checked, https_checked, inSequence, outSequence);
        return apiPublisherRestClient.addAPI(apiRequest);
    }

    private boolean isApiCreated(String apiName) throws APIManagerIntegrationTestException {

        HttpResponse api = apiStoreRestClient.getAPI();
        verifyResponse(api);

        return api.getData().contains(apiName);
    }

    private HttpResponse publishApi(String providerName, String apiName, String apiVersion)
            throws APIManagerIntegrationTestException {

        APIIdentifier apiIdentifier = new APIIdentifier(providerName, apiName, apiVersion);
        return apiPublisherRestClient.changeAPILifeCycleStatusToPublish(apiIdentifier, false);
    }

    private HttpResponse createApplication(String apiName, String apiDescription, String apiTier)
            throws UnsupportedEncodingException, APIManagerIntegrationTestException {

        return apiStoreRestClient.addApplication(apiName, apiTier, "",
                URLEncoder.encode(apiDescription, UTF_8));
    }
}
