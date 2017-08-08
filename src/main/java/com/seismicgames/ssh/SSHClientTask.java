package com.seismicgames.ssh;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import org.apache.commons.lang.CharEncoding;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.Callable;

abstract public class SSHClientTask implements Callable<Boolean> {
    private static final String AGENT_COMMAND = "sudo /opt/puppetlabs/bin/puppet agent --test";

    abstract Session getSession();
    abstract void handleOutput(String output);

    @Override
    public Boolean call() throws Exception {
        Session session = getSession();
        if(session == null) {
            return false;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setOutputStream(baos);
        channel.setErrStream(baos);
        channel.setCommand(AGENT_COMMAND);
        channel.connect();

        while (!channel.isClosed()) {
            session.sendKeepAliveMsg();
        }

        channel.disconnect();
        session.disconnect();

        handleOutput(baos.toString(CharEncoding.UTF_8));
        return true;
    }
}
