package com.seismicgames;

import org.apache.commons.lang.StringUtils;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Base64;

/**
 * SSL code comes from https://github.com/puppetlabs/puppetdb-javaclient
 */

public class SSLHelper {
    private final String PASSWORD = "puppet";

    private String cacert;
    private String cert;
    private String privateKey;
    private final PrintStream LOGGER;

    public SSLHelper(String cacert, String cert, String privateKey, PrintStream logger) {
        if(StringUtils.isEmpty(cacert) || StringUtils.isEmpty(privateKey) || StringUtils.isEmpty(privateKey)) {
            throw new IllegalArgumentException("Cannot create SSLHelper with empty arguments");
        }

        this.cacert = cacert;
        this.cert = cert;
        this.privateKey = privateKey;
        this.LOGGER = logger;

        cleanCertsAndKey();
    }

    public SSLSocketFactory getSSLFactory() throws CertificateException, NoSuchAlgorithmException, KeyStoreException,
            IOException, UnrecoverableKeyException, KeyManagementException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        KeyStore trustKeyStore = getTrustKeyStore(factory);
        TrustStrategy strategy = trustKeyStore == null ? new TrustSelfSignedStrategy() : null;

        return new SSLSocketFactory(SSLSocketFactory.TLS, getKeyStore(factory, PASSWORD), PASSWORD, trustKeyStore, null,
                strategy, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
    }

    // private helper methods
    private Certificate generateCertificate(String cert, CertificateFactory factory) throws CertificateException {
        byte[] bytes = Base64.getDecoder().decode(cert);
        InputStream stream = new ByteArrayInputStream(bytes);
        return factory.generateCertificate(stream);
    }

    private Certificate getCACert(CertificateFactory factory) throws CertificateException {
        return generateCertificate(cacert, factory);
    }

    private Certificate getHostCert(CertificateFactory factory) throws CertificateException {
        return generateCertificate(cert, factory);
    }

    private KeyStore getKeyStore(CertificateFactory factory, String password) throws KeyStoreException,
            CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null);
        store.setKeyEntry("key-alias", getPrivateKey(), password.toCharArray(),
                new Certificate[] { getHostCert(factory)});
        return store;
    }

    private PrivateKey getPrivateKey() throws IOException {
        PEMParser pemParser = new PEMParser(new StringReader(privateKey));
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
        KeyPair pair = converter.getKeyPair((PEMKeyPair) pemParser.readObject());
        return pair.getPrivate();
    }

    private KeyStore getTrustKeyStore(CertificateFactory factory) throws CertificateException, KeyStoreException,
            IOException, NoSuchAlgorithmException {
        Certificate ca = getCACert(factory);

        if(ca == null) {
            throw new CertificateException("Invalid CA Cert used");
        }

        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null);

        TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustFactory.init(store);
        store.setCertificateEntry("ca-cert-alias", ca);
        return store;
    }

    /**
     * Removes comments from the certs and key strings if they are there
     */
    private void cleanCertsAndKey() {
        StringBuilder sb = new StringBuilder();

        for (String line : cacert.split("\\r\\n|\\n|\\r")) {
            if(!line.startsWith("---")) {
                sb.append(line.trim());
            }
        }
        cacert = sb.toString();

        for (String line : cert.split("\\r\\n|\\n|\\r")) {
            if(!line.startsWith("---")) {
                sb.append(line.trim());
            }
        }
        cert = sb.toString();
    }
}
