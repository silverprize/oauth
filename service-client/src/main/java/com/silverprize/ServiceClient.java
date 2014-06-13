package com.silverprize;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxRequestConfig;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.IOUtils;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.ChildList;
import com.google.api.services.drive.model.ChildReference;
import com.silverprize.oauth2.Authorizer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

public class ServiceClient {

    private static final String FOLDER = "application/vnd.google-apps.folder";

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /** Directory to store user credentials. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".store/google_service");

    public static void main(String args[]) throws Exception {
        String userId = "silver";
        ServiceProvider serviceProvider = ServiceProvider.GOOGLE;
        Credential credential = Authorizer.authorize(userId, serviceProvider);

        switch (serviceProvider) {
            case GOOGLE:
                HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
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
                break;

            case DROPBOX:
                DbxRequestConfig dbxRequestConfig = new DbxRequestConfig(userId, Locale.getDefault().toString());
                DbxClient dbxClient = new DbxClient(dbxRequestConfig, credential.getAccessToken());
                DbxEntry.WithChildren children = dbxClient.getMetadataWithChildren("/");
                for (DbxEntry entry : children.children) {
                    System.out.println(entry);
                }
                break;
        }
    }



    /**
     * Download a file's content.<br/>
     * <a href="https://code.google.com/p/google-api-java-client/source/browse/drive-cmdline-sample/src/main/java/com/google/api/services/samples/drive/cmdline/DriveSample.java?repo=samples#172">google-api-java-client DriveSample</a><br/>
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
