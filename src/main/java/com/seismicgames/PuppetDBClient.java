package com.seismicgames;

import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.security.*;
import java.security.cert.CertificateException;

public class PuppetDBClient {
    public enum Endpoint {
        node;

        public static Endpoint fromString(String endpoint) {
            try {
                return Endpoint.valueOf(endpoint);
            } catch (Exception e) {
                return Endpoint.node;
            }
        }
    }

    private final String host;
    private final int port;
    private final SSLHelper sslHelper;
    private final PrintStream logger;

    public PuppetDBClient(String host, int port, SSLHelper sslHelper, PrintStream logger) {
        this.host = host;
        this.port = port;
        this.sslHelper = sslHelper;
        this.logger = logger;
    }

    public void runQuery(String endpoint) {
        try {
            String path = getEndpoint(Endpoint.fromString(endpoint));
            HttpClient client = getHttpClient();

            logger.println(String.format("Connecting to https://%s:%s%s", host, port, path));
            URI uri = new URI("https", null, host, port, path, null, null);

            HttpGet get = new HttpGet(uri);
            client.execute(get, (ResponseHandler<Void>) response -> {
                StatusLine line = response.getStatusLine();
                int code = line.getStatusCode();
                logger.println("Response code: " + code);

                return null;
            });
        } catch (Exception e) {
            logger.println(String.format("runQuery exception! %s", e.getMessage()));
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

    private String getEndpoint(Endpoint endpoint) {
        switch (endpoint) {
            case node:
                return "/pdb/query/v4/nodes";
        }

        throw new IllegalArgumentException("Invalid Endpoint sent to getEndpoint");
    }

    // get the http client with ssl handler
    private HttpClient getHttpClient() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, KeyManagementException, IOException {
        DefaultHttpClient client = new DefaultHttpClient(new BasicHttpParams());
        client.getConnectionManager().getSchemeRegistry().register(new Scheme("https", 443, sslHelper.getSSLFactory()));
        return client;
    }
}
