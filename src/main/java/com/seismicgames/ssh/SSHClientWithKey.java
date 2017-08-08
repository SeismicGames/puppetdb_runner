package com.seismicgames.ssh;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import hudson.util.Secret;

import java.io.PrintStream;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class SSHClientWithKey extends SSHClientTask {
    private final String host;
    private final String username;
    private final Secret passphrase;
    private final String hostKey;
    private final List<String> privateKeys;
    private final PrintStream logger;

    public SSHClientWithKey(String host, String username, List<String> privateKeys, Secret passphrase, String hostKey,
                            PrintStream logger) {
        this.host = host;
        this.username = username;
        this.privateKeys = privateKeys;
        this.passphrase = passphrase;
        this.hostKey = hostKey;
        this.logger = logger;
    }

    // help from https://dentrassi.de/2015/07/13/programmatically-adding-a-host-key-with-jsch/
    @Override
    Session getSession() {
        JSch jsch;
        Session session = null;
        String privateKey;
        Queue<String> queue = new LinkedList<>(privateKeys);
        while (!queue.isEmpty()) {
            privateKey = queue.remove();
            jsch = new JSch();

            try {
                byte[] key = Base64.getDecoder().decode(hostKey);
                HostKey hostKey = new HostKey(host, key);
                jsch.getHostKeyRepository().add(hostKey, null);

                if (Secret.toString(passphrase).equals("")) {
                    jsch.addIdentity(privateKey);
                } else {
                    jsch.addIdentity(privateKey, Secret.toString(passphrase));
                }

                session = jsch.getSession(username, host, 22);
                session.connect();
            } catch (JSchException e) {
                // can't use this key, continue
                session = null;
            }
        }

        return session;
    }

    @Override
    void handleOutput(String output) {
        logger.println(output);
    }
}
