package org.wso2.am.scenario.tests.subscribe.to.an.api;

import org.apache.axis2.AxisFault;
import org.json.JSONObject;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.am.integration.test.utils.APIManagerIntegrationTestException;
import org.wso2.am.integration.test.utils.base.APIMIntegrationConstants;
import org.wso2.am.integration.test.utils.bean.SubscriptionRequest;
import org.wso2.am.scenario.test.common.APIPublisherRestClient;
import org.wso2.am.scenario.test.common.APIRequest;
import org.wso2.am.scenario.test.common.APIStoreRestClient;
import org.wso2.am.scenario.test.common.ScenarioTestBase;
import org.wso2.carbon.apimgt.api.APIManagementException;
import org.wso2.carbon.apimgt.api.model.APIIdentifier;
import org.wso2.carbon.authenticator.stub.LoginAuthenticationExceptionException;
import org.wso2.carbon.automation.test.utils.http.client.HttpResponse;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;
import org.wso2.carbon.integration.common.admin.client.UserManagementClient;
import org.wso2.carbon.tenant.mgt.stub.TenantMgtAdminServiceExceptionException;
import org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean;
import org.wso2.carbon.integration.common.admin.client.TenantManagementServiceClient;
import org.wso2.carbon.user.mgt.stub.UserAdminUserAdminException;
import org.wso2.carbon.user.mgt.stub.types.carbon.FlaggedName;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;

import static org.apache.axis2.context.MessageContext.UTF_8;

public class ApiSubscriptionTestCases extends ScenarioTestBase {

    private static final String ADMIN_LOGIN_USERNAME = "admin";
    private static final String ADMIN_LOGIN_PW = "admin";
    private static final String TENANT_LOGIN_ADMIN_USERNAME = "admin@testwso2.com";
    private static final String TENANT_ADMIN_PW = "admin";
    private static final String TENANT_ADMIN_USERNAME = "admin";
    private static final String TENANT_DOMAIN = "testwso2.com";
    private static final String DEFAULT_STORE_URL = "https://localhost:9443/store";
    private static final String DEFAULT_PUBLISHER_URL = "https://localhost:9443/publisher";
    private static final String KEY_MANAGER_URL = "https://localhost:9443/services/";
    private final String FIRST_ROLE = "publisher_role1";
    private String apiName;
    private String apiVersion;
    private String apiProvider;
    private String tierCollection;
    private String apiContext;
    private String applicationName;

    private APIStoreRestClient apiStoreRestClient;
    private APIStoreRestClient tenantApiStoreRestClient;
    private APIPublisherRestClient apiPublisherRestClient;
    private APIPublisherRestClient tenantApiPublisherRestClient;
    private TenantManagementServiceClient tenantManagementClient;
    private UserManagementClient userManagementClient;
    private UserManagementClient tenantUserManagementClient;

    private String SUBSCRIBER_USERNAME = "user";
    private String SUBSCRIBER_PASSWORD = "Wso2123!";

    private String TENANT_SUBSCRIBER_USERNAME = "tenantUser";
    private String TENANT_SUBSCRIBER_PASSWORD = "Wso2123!";

    private static final String CREATOR_PUBLISHER_USERNAME = "creatorPublisherUser";
    private static final String CREATOR_PUBLISHER_PASSWORD = "Wso2123!";

    private static final String TENANT_CREATOR_PUBLISHER_USERNAME = "tenantCreatorPublisherUser";
    private static final String TENANT_CREATOR_PUBLISHER_PASSWORD = "Wso2123!";
    private String tempUsername = "user";
    private String tempPassword = "Wso2123!";

    @BeforeClass(alwaysRun = true)
    public void init() throws APIManagerIntegrationTestException {

        String storeURL;
        Properties infraProperties = getDeploymentProperties();
        String publisherURL = infraProperties.getProperty(PUBLISHER_URL);

        if (publisherURL == null) {
            publisherURL = DEFAULT_PUBLISHER_URL;
        }

        storeURL = infraProperties.getProperty(STORE_URL);
        if (storeURL == null) {
            storeURL = DEFAULT_STORE_URL;
        }
        setKeyStoreProperties();

        try {
            setKeyStoreProperties();
            addTenantAndActivate(TENANT_DOMAIN, TENANT_ADMIN_USERNAME, TENANT_ADMIN_PW);
            tenantUserManagementClient = new UserManagementClient(KEY_MANAGER_URL, TENANT_LOGIN_ADMIN_USERNAME,
                    TENANT_ADMIN_PW);
            createUserWithPublisherAndCreatorRole(CREATOR_PUBLISHER_USERNAME, CREATOR_PUBLISHER_PASSWORD,
                    ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
            createUserWithPublisherAndCreatorRole(TENANT_CREATOR_PUBLISHER_USERNAME, TENANT_CREATOR_PUBLISHER_PASSWORD,
                    TENANT_LOGIN_ADMIN_USERNAME, TENANT_ADMIN_PW);

            createUserWithSubscriberRole(SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
            createUserWithSubscriberRole(TENANT_SUBSCRIBER_USERNAME, TENANT_SUBSCRIBER_PASSWORD, TENANT_LOGIN_ADMIN_USERNAME, TENANT_ADMIN_PW);

            apiPublisherRestClient = new APIPublisherRestClient(publisherURL);
            apiPublisherRestClient.login(CREATOR_PUBLISHER_USERNAME, CREATOR_PUBLISHER_PASSWORD);

            apiStoreRestClient = new APIStoreRestClient(storeURL);
            apiStoreRestClient.login(SUBSCRIBER_USERNAME, SUBSCRIBER_PASSWORD);

            userManagementClient = new UserManagementClient(KEY_MANAGER_URL, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
            AuthenticatorClient authenticatorClient = new AuthenticatorClient(KEY_MANAGER_URL);
            String sessionCookie = authenticatorClient.login("admin", "admin", "localhost");

            tenantManagementClient = new TenantManagementServiceClient(KEY_MANAGER_URL, sessionCookie);

            tenantApiPublisherRestClient = new APIPublisherRestClient(publisherURL);
            String tenantApiCreatorLoginUsername = TENANT_CREATOR_PUBLISHER_USERNAME + "@" + TENANT_DOMAIN;
            tenantApiPublisherRestClient.login(tenantApiCreatorLoginUsername, TENANT_CREATOR_PUBLISHER_PASSWORD);

            String tenantApiSubscriberLoginUsername = TENANT_SUBSCRIBER_USERNAME + "@" + TENANT_DOMAIN;

            tenantApiStoreRestClient = new APIStoreRestClient(storeURL);
            tenantApiStoreRestClient.login(tenantApiSubscriberLoginUsername, TENANT_SUBSCRIBER_PASSWORD);

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Failed when initialization of test", e);
        }
    }

    @Test
    public void testSubscribeToApiFromAppName() throws APIManagerIntegrationTestException,
            UnsupportedEncodingException {

        apiProvider = CREATOR_PUBLISHER_USERNAME;
        apiName = "testSubscribeToApiFromAppNameApi";
        apiContext = "/testSubscribeToApiFromAppName";
        apiVersion = "1.0.0";
        tierCollection = "Silver";
        applicationName = "testSubscribeToApiFromAppNameApp";

        if (isApplicationCreated(applicationName, apiStoreRestClient)) {
            verifyResponse(deleteApplication(applicationName, apiStoreRestClient));
        }

        if (isApplicationCreated(applicationName, tenantApiStoreRestClient)) {
            verifyResponse(deleteApplication(applicationName, tenantApiStoreRestClient));
        }

        if (isApiCreated(apiName, apiPublisherRestClient)) {
            verifyResponse(deleteApi(apiName, apiVersion, apiProvider, apiPublisherRestClient));
        }

        verifyResponse(createApi());
        verifyResponse(publishApi(apiProvider, apiName, apiVersion, apiPublisherRestClient));
        assertTrue(isApiCreated(apiName, apiPublisherRestClient), "Api was not created");

        verifyResponse(createApplication(applicationName, "",
                APIMIntegrationConstants.APPLICATION_TIER.UNLIMITED, apiStoreRestClient));
        assertTrue(isApplicationCreated(applicationName, apiStoreRestClient));

        verifyResponse(subscribeToApplication(apiName, apiVersion, apiProvider, applicationName, apiStoreRestClient));
        assertTrue(isSubscriptionSuccessful(applicationName, apiStoreRestClient), "Subscription was not successful");
    }

    @Test
    public void testSubscriptionForRestrictedApiCreatedByTenant() throws APIManagerIntegrationTestException {

        apiProvider = TENANT_CREATOR_PUBLISHER_USERNAME + "@" + TENANT_DOMAIN;
        apiName = "testSubscriptionForRestrictedApiCreatedByTenantApi";
        apiContext = "/testSubscriptionForRestrictedApiCreatedByTenant";
        apiVersion = "1.0.0";
        tierCollection = "Silver";
        applicationName = "testSubscriptionForRestrictedApiCreatedByTenantApp";

        try {
            if (!isRoleCreated(FIRST_ROLE)) {
                createRoles(FIRST_ROLE, tenantUserManagementClient);
            }

            if (isApplicationCreated(applicationName, tenantApiStoreRestClient)) {
                verifyResponse(deleteApplication(applicationName, tenantApiStoreRestClient));
            }

            if (isApiCreated(apiName, apiPublisherRestClient)) {
                verifyResponse(deleteApi(apiName, apiVersion, apiProvider, apiPublisherRestClient));
            }

            if (isApiCreated(apiName, tenantApiPublisherRestClient)) {
                verifyResponse(deleteApi(apiName, apiVersion, apiProvider, tenantApiPublisherRestClient));
            }

            verifyResponse(createRestrictedApi(tenantApiPublisherRestClient));
            verifyResponse(publishApi(apiProvider, apiName, apiVersion, tenantApiPublisherRestClient));
            assertTrue(isApiCreated(apiName, tenantApiPublisherRestClient), "Api was not created");

            createUser(tempUsername, tempPassword, new String[]{FIRST_ROLE}, TENANT_LOGIN_ADMIN_USERNAME, TENANT_ADMIN_PW);
             tenantApiStoreRestClient = new APIStoreRestClient(DEFAULT_STORE_URL);
            tenantApiStoreRestClient.login(tempUsername+"@"+TENANT_DOMAIN,tempPassword);

            verifyResponse(createApplication(applicationName, "", APIMIntegrationConstants.APPLICATION_TIER
                    .UNLIMITED, tenantApiStoreRestClient));

            assertTrue(isApplicationCreated(applicationName, tenantApiStoreRestClient));

            verifyResponse(subscribeToApplication(apiName, apiVersion, apiProvider, applicationName,
                    tenantApiStoreRestClient));

            assertTrue(isSubscriptionSuccessful(applicationName, tenantApiStoreRestClient), "Subscription was not successful");

        } catch (Exception e) {
            throw new APIManagerIntegrationTestException("Failed to run subscription for restricted api " +
                    "created by tenant test case", e);
        }
    }
//
//    @Test
//    private void testSusbscriptionToPublicApiCreatedByTenant() {
//
//        apiProvider = TENANT_LOGIN_ADMIN_USERNAME;
//        apiName = "PhoneVerificationOptionalAdd";
//        apiContext = "/phoneverifyOptionaladd";
//        apiVersion = "1.0.0";
//        tierCollection = "Silver";
//        applicationName = "app1";
//
//    }

    private void createRoles(String roleName, UserManagementClient client) throws RemoteException,
            UserAdminUserAdminException {

        String apiPublishPermission = "/permission/admin/manage/api/publish";
        String apiSubscribePermission = "/permission/admin/manage/api/subscribe";
        String apiCreatePermission = "/permission/admin/manage/api/create";
        String loginPermission = "/permission/admin/login";
        client.addRole(roleName, new String[]{}, new String[]{loginPermission, apiSubscribePermission});
    }

    private void deleteTenant(String domainName) {

        tenantManagementClient.deleteTenant(domainName);
    }

    private HttpResponse subscribeToApplication(String apiName, String apiVersion, String apiProvider,
                                                String applicationName, APIStoreRestClient client) throws APIManagerIntegrationTestException {

        SubscriptionRequest subscriptionRequest = new SubscriptionRequest(apiName, apiVersion, apiProvider,
                applicationName, tierCollection);
        return client.subscribe(subscriptionRequest);

    }

    private boolean isSubscriptionSuccessful(String applicationName, APIStoreRestClient client) throws APIManagerIntegrationTestException {

        HttpResponse allSubscriptions = client.getAllSubscriptionsOfApplication(applicationName);
        verifyResponse(allSubscriptions);
        JSONObject jsonObject = new JSONObject(allSubscriptions.getData());
        return Integer.parseInt(jsonObject.getJSONObject("subscriptions").get("totalLength").toString()) == 1;
    }

    private boolean isApplicationCreated(String apiName, APIStoreRestClient client) throws
            APIManagerIntegrationTestException {

        HttpResponse applications = client.getApplications();
        verifyResponse(applications);

        return applications.getData().contains(apiName);
    }

    private boolean isRoleCreated(String roleName) throws RemoteException, UserAdminUserAdminException {

        FlaggedName[] roles = tenantUserManagementClient.getAllRolesNames(FIRST_ROLE, 5);

        for (FlaggedName role : roles) {
            if (role.getItemName().equalsIgnoreCase(FIRST_ROLE)) {
                return true;
            }
        }
        return false;
    }

    private HttpResponse deleteApplication(String applicationName, APIStoreRestClient client) throws
            APIManagerIntegrationTestException {

        return client.removeApplication(applicationName);
    }

    private HttpResponse deleteApi(String apiName, String apiVersion, String apiProvider,
                                   APIPublisherRestClient client) throws APIManagerIntegrationTestException {

        return client.deleteAPI(apiName, apiVersion, apiProvider);
    }

    private void deleteRole(String roleName, UserManagementClient client) throws RemoteException,
            UserAdminUserAdminException {

        client.deleteRole(roleName);
    }

    private HttpResponse createRestrictedApi(APIPublisherRestClient client) throws MalformedURLException,
            APIManagerIntegrationTestException {

        String backendEndPoint = "http://ws.cdyne.com/phoneverify/phoneverify.asmx";
        String apiResource = "/find";
        String apiVisibility = "restricted";

        APIRequest apiRequest = new APIRequest(apiName, apiContext, apiVisibility, FIRST_ROLE, apiVersion, apiResource,
                tierCollection, new URL(backendEndPoint));

        return client.addAPI(apiRequest);
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

    private boolean isApiCreated(String apiName, APIPublisherRestClient client) throws APIManagerIntegrationTestException {

        HttpResponse api = client.getAllAPIs();
        verifyResponse(api);

        return api.getData().contains(apiName);
    }

    private HttpResponse publishApi(String providerName, String apiName, String apiVersion, APIPublisherRestClient
            client)
            throws APIManagerIntegrationTestException {

        APIIdentifier apiIdentifier = new APIIdentifier(providerName, apiName, apiVersion);
        return client.changeAPILifeCycleStatusToPublish(apiIdentifier, false);
    }

    private HttpResponse createApplication(String apiName, String apiDescription, String apiTier, APIStoreRestClient
            client)
            throws UnsupportedEncodingException, APIManagerIntegrationTestException {

        return client.addApplication(apiName, apiTier, "",
                URLEncoder.encode(apiDescription, UTF_8));
    }

    @AfterTest(alwaysRun = true)
    public void destroy() throws Exception {

        deleteApplication(applicationName, apiStoreRestClient);
        deleteApplication(applicationName, tenantApiStoreRestClient);
        deleteApi(apiName, apiVersion, apiProvider, apiPublisherRestClient);
        deleteApi(apiName, apiVersion, apiProvider, tenantApiPublisherRestClient);
        if (isRoleCreated(FIRST_ROLE)) {
            deleteRole(FIRST_ROLE, tenantUserManagementClient);
        }
        deleteUser(TENANT_SUBSCRIBER_USERNAME, TENANT_LOGIN_ADMIN_USERNAME, TENANT_ADMIN_PW);
        deleteUser(TENANT_CREATOR_PUBLISHER_USERNAME, TENANT_LOGIN_ADMIN_USERNAME, TENANT_ADMIN_PW);

        deleteUser(SUBSCRIBER_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        deleteUser(CREATOR_PUBLISHER_USERNAME, ADMIN_LOGIN_USERNAME, ADMIN_LOGIN_PW);
        deleteUser(tempUsername, TENANT_LOGIN_ADMIN_USERNAME, TENANT_ADMIN_PW);
//        deleteTenant(TENANT_DOMAIN);
    }
}
