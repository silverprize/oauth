package com.silverprize;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.IOUtils;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.ChildReference;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class GDriveClient {

    private static final String FOLDER = "application/vnd.google-apps.folder";

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /** Directory to store user credentials. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".store/google_service");

    public static void main(String args[]) throws Exception {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = authorize();
        Drive drive = new Drive.Builder(httpTransport, JSON_FACTORY, credential).setApplicationName("API Project").build();
        ChildList childList = drive.children().list("root").execute();
        for (ChildReference ref : childList.getItems()) {
            com.google.api.services.drive.model.File gdFile = drive.files().get(ref.getId()).execute();
            if (!FOLDER.equals(gdFile.getMimeType())) {
                if (!Boolean.TRUE.equals(gdFile.getExplicitlyTrashed())) {
                    File downloadFile = downloadFile(drive, gdFile);
                    if (downloadFile != null && downloadFile.exists()) {
                        System.out.println("Success to download.");
                    }
                }
            }
        }
    }

    /** Authorizes the installed application to access user's protected data. */
    private static Credential authorize() throws Exception {
        // load client secrets
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(GDriveClient.class.getResourceAsStream("/client_secrets.json")));
        if (clientSecrets.getDetails().getClientId().startsWith("Enter")
                || clientSecrets.getDetails().getClientSecret().startsWith("Enter ")) {
            System.out.println(
                    "Enter Client ID and Secret from https://code.google.com/apis/console/?api=drive "
                            + "into client_secrets.json");
            System.exit(1);
        }
        // set up authorization code flow
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, DriveScopes.all())
                .setDataStoreFactory(new FileDataStoreFactory(DATA_STORE_DIR))
                .build();
        // authorize
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("silver");
    }

    /**
     * Download a file's content.
     *
     * @param service Drive API service instance.
     * @param gdFile Drive File instance.
     * @return InputStream containing the file's content if successful,
     *         {@code null} otherwise.
     */
    private static File downloadFile(Drive service, com.google.api.services.drive.model.File gdFile) {
        if (gdFile.getDownloadUrl() != null && gdFile.getDownloadUrl().length() > 0) {
            try {
                HttpResponse resp =
                        service.getRequestFactory().buildGetRequest(new GenericUrl(gdFile.getDownloadUrl()))
                                .execute();
                File dest = new File(gdFile.getTitle());
                OutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
                IOUtils.copy(resp.getContent(), out);
                out.close();
                return dest;
            } catch (IOException e) {
                // An error occurred.
                e.printStackTrace();
                return null;
            }
        } else {
            // The file doesn't have any content stored on Drive.
            return null;
        }
    }

}
