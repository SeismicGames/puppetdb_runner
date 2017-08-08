package com.seismicgames.ssh;

import com.jcraft.jsch.*;
import hudson.util.Secret;

import java.io.PrintStream;
import java.util.Base64;

public class SSHClient extends SSHClientTask {
    private final String host;
    private final String username;
    private final Secret password;
    private final String hostKey;
    private final PrintStream logger;

    public SSHClient(String host, String username, Secret password, String hostKey, PrintStream logger) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.hostKey = hostKey;
        this.logger = logger;
    }

    // help from https://dentrassi.de/2015/07/13/programmatically-adding-a-host-key-with-jsch/
    @Override
    Session getSession() {
        JSch jsch = new JSch();
        Session session;
        try {
            byte[] key = Base64.getDecoder().decode(hostKey);
            HostKey hostKey = new HostKey(host, key);
            jsch.getHostKeyRepository().add(hostKey, null);

            session = jsch.getSession(username, host, 22);
            session.setPassword(Secret.toString(password));
            session.connect();
            return session;
        } catch (JSchException e) {
            logger.println("SSH auth failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    void handleOutput(String output) {
        logger.println(output);
    }
}
