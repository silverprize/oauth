package com.silverprize.oauth2;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.DriveScopes;
import com.silverprize.ServiceProvider;

import javax.swing.JOptionPane;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.ResourceBundle;

/**
 * Created by silver on 14. 6. 13.
 */
public class Authorizer {
    /**
     * Authorizes the installed application to access user's protected data.<br/>
     * <a href="https://code.google.com/p/google-api-java-client/source/browse/drive-cmdline-sample/src/main/java/com/google/api/services/samples/drive/cmdline/DriveSample.java?repo=samples#87">google-api-java-client DriveSample</a>
     **/
    public static Credential authorize(String userId, ServiceProvider serviceProvider) throws Exception {
        ClientSecrets clientSecrets = ClientSecrets.create(serviceProvider);
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        AuthorizationCodeFlow.Builder flowBuilder = new AuthorizationCodeFlow.Builder(
                BearerToken.authorizationHeaderAccessMethod(),
                httpTransport,
                JSON_FACTORY,
                new GenericUrl(clientSecrets.tokenUri),
                new ClientParametersAuthentication(clientSecrets.clientId, clientSecrets.clientSecret),
                clientSecrets.clientId,
                clientSecrets.authUri);

        // save to local permanently.
        flowBuilder.setDataStoreFactory(new FileDataStoreFactory(
                new File(DATA_STORE_DIR, serviceProvider.toString().toLowerCase())));

        if (clientSecrets.scopes != null) {
            // optional
            flowBuilder.setScopes(clientSecrets.scopes);
        }

        return authorize(flowBuilder.build(), userId, serviceProvider);
    }

    private static Credential authorize(AuthorizationCodeFlow flow, String userId, ServiceProvider serviceProvider) throws IOException {
        VerificationCodeReceiver receiver = VerificationCodeReceiverFactory.createVerificationCodeReceiver(serviceProvider);
        try {
            Credential credential = flow.loadCredential(userId);
            if (credential != null
                    && (credential.getRefreshToken() != null || credential.getExpiresInSeconds() > 60)) {
                return credential;
            }

            String redirectUri = receiver.getRedirectUri();
            AuthorizationCodeRequestUrl authorizationUrl = flow.newAuthorizationUrl();
            if (redirectUri != null) {
                authorizationUrl.setRedirectUri(redirectUri);
            }

            // open in browser
            AuthorizationCodeInstalledApp.browse(authorizationUrl.build());

            // receive authorization code and exchange it for an access token
            String code = receiver.waitForCode();

            AuthorizationCodeTokenRequest request;
            if (!flow.getScopes().isEmpty()) {
                request = flow.newTokenRequest(code);

            } else {
                request = new AuthorizationCodeTokenRequest(flow.getTransport(),
                        flow.getJsonFactory(),
                        new GenericUrl(flow.getTokenServerEncodedUrl()),
                        code)
                        .setClientAuthentication(flow.getClientAuthentication())
                        .setRequestInitializer(flow.getRequestInitializer());
            }

            if (redirectUri != null) {
                request.setRedirectUri(redirectUri);
            }

            TokenResponse response = request.execute();
            if (response.getExpiresInSeconds() == null) {
                response.setExpiresInSeconds(EXPIRES_SECONDS);
            }

            // store credential and return it
            return flow.createAndStoreCredential(response, userId);
        } finally {
            receiver.stop();
        }
    }

    private static class ClientSecrets {
        public String authUri;
        public String tokenUri;
        public String clientId;
        public String clientSecret;
        public Collection<String> scopes = null;

        public static ClientSecrets create(ServiceProvider serviceProvider) throws IOException {
            String authUri;
            String tokenUri;
            String clientId;
            String clientSecret;
            Collection<String> scopes = null;

            switch (serviceProvider) {
                case GOOGLE:
                    // load client secrets
                    GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                            new InputStreamReader(Authorizer.class.getResourceAsStream("/com/silverprize/oauth2/google_client_secrets.json")));
                    tokenUri = clientSecrets.getDetails().getTokenUri();
                    authUri = clientSecrets.getDetails().getAuthUri();
                    clientId = clientSecrets.getDetails().getClientId();
                    clientSecret = clientSecrets.getDetails().getClientSecret();
                    scopes = DriveScopes.all();
                    break;
                case DROPBOX:
                    ResourceBundle bundle = ResourceBundle.getBundle("com.silverprize.oauth2.ClientSecrets");
                    tokenUri = bundle.getString("DROPBOX_TOKEN_URI");
                    authUri = bundle.getString("DROPBOX_AUTH_URI");
                    clientId = bundle.getString("DROPBOX_CLIENT_ID");
                    clientSecret = bundle.getString("DROPBOX_CLIENT_SECRET");
                    break;
                default:
                    throw new RuntimeException(serviceProvider + " : Unsupported service provider");
            }

            ClientSecrets clientSecrets = new ClientSecrets();
            clientSecrets.authUri = authUri;
            clientSecrets.clientId = clientId;
            clientSecrets.clientSecret = clientSecret;
            clientSecrets.tokenUri = tokenUri;
            clientSecrets.scopes = scopes;
            return clientSecrets;
        }
    }

    private static class VerificationCodeReceiverFactory {
        public static VerificationCodeReceiver createVerificationCodeReceiver(ServiceProvider serviceProvider) {
            switch (serviceProvider) {
                case GOOGLE:
                    return new LocalServerReceiver();
                case DROPBOX:
                    return new VerificationCodeReceiverImpl(null);
                default:
                    throw new RuntimeException(serviceProvider + " : Unsupported service provider");
            }
        }

        private static class VerificationCodeReceiverImpl implements VerificationCodeReceiver {

            private String redirectUri;

            public VerificationCodeReceiverImpl(String redirectUri) {
                this.redirectUri = redirectUri;
            }

            @Override
            public String getRedirectUri() throws IOException {
                return redirectUri;
            }

            @Override
            public String waitForCode() throws IOException {
                return JOptionPane.showInputDialog(null, "");
            }

            @Override
            public void stop() throws IOException {

            }
        }
    }

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /** Directory to store user credentials. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".store");

    private static final long EXPIRES_SECONDS = 600;
}
