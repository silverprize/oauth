package com.silverprize;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public class GDriveClient {
    public static void main(String args[]) throws IOException, GeneralSecurityException {
        HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
        details.setClientId("627339111893-vbnqjfvohhfuk2r431jkii7jpvu31mab.apps.googleusercontent.com");
        details.setClientSecret("M-Jm9FN00OT8Eqb0luLo7RD2");
        details.setRedirectUris(Arrays.asList("urn:ietf:wg:oauth:2.0:oob", "http://localhost"));
        details.setAuthUri("https://accounts.google.com/o/oauth2/auth");
        details.setTokenUri("https://accounts.google.com/o/oauth2/token");
        details.setFactory(GsonFactory.getDefaultInstance());
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
        clientSecrets.setInstalled(details);

        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(transport)
                .setClientSecrets(new GoogleClientSecrets().setInstalled(details))
                .build();
        Drive drive = new Drive.Builder(transport, GsonFactory.getDefaultInstance(), credential)
                .setApplicationName("API Project")
                .build();
        Drive.Files files = drive.files();
        Drive.Files.List list = files.list();
        FileList fileList = list.execute();
        System.out.println(fileList.getKind());
    }
}
