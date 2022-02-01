package com.egarcia.auto_update;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class FileDownloader extends Thread{
    static final int chunkSize = 1024;

    private final String url;
    private final File file;

    public int downloaded = 0;
    public Exception exception;

    public FileDownloader(String url, File file){
        this.url = url;
        this.file = file;
    }

    public void run(){
        try {
            URL requestUrl = new URL(url);
            HttpsURLConnection connection = (HttpsURLConnection) requestUrl.openConnection();
            connection.setRequestProperty("Accept-Encoding",
                    "gzip, deflate, br");
            connection.setRequestProperty("User-Agent", "auto-update");

            if (connection.getResponseCode() == 200) {
                try {
                    BufferedInputStream inputStream = new BufferedInputStream(
                            connection.getInputStream());
                    FileOutputStream out = new FileOutputStream(file);

                    byte[] chunk = new byte[FileDownloader.chunkSize];
                    int read = 0;
                    while ((read = inputStream.read(chunk)) > 0) {
                        out.write(chunk, 0, read);
                    }

                    out.flush();
                    out.close();

                    inputStream.close();
                    downloaded = 1;
                } finally {
                    connection.disconnect();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            exception = e;
            downloaded = -1;
        }
    }
}