package com.seismicgames;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class PuppetDBBuilder extends Builder implements SimpleBuildStep {
    // static variables
    private static final String DISPLAY_NAME = "Refresh Puppet Agents";

    // plugin variables
    private final String host;
    private final String query;
    private final String username;
    private final String password;
    private final String cacert;
    private final String cert;
    private final String privateKey;
    private final String endpoint;

    @DataBoundConstructor
    public PuppetDBBuilder(String host, String query, String username, String password, String cacert, String cert,
                           String privateKey, String endpoint) {
        this.host = host;
        this.query = query;
        this.username = username;
        this.password = password;
        this.cacert = cacert;
        this.cert = cert;
        this.privateKey = privateKey;
        this.endpoint = endpoint;
    }

    // getters for Jelly
    public String getHost() {
        return host;
    }

    public String getQuery() {
        return query;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getCacert() {
        return cacert;
    }

    public String getCert() {
        return cert;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    @Override
    public Descriptor<Builder> getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            // can be used with all job types
            return true;
        }

        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }

        // validate form parameters
        public FormValidation doCheckHost(@QueryParameter String value) {
            if(StringUtils.isEmpty(value)) {
                return FormValidation.error("Please enter a host url");
            }

            try {
                URL url = new URL(value);
            } catch (MalformedURLException e) {
                return FormValidation.error("Please enter a valid host url");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckQuery(@QueryParameter String value) throws IOException, ServletException {
            if(StringUtils.isEmpty(value)) {
                return FormValidation.error("Please enter a query");
            }

            // TODO: potentially add PuppetDB query validator

            return FormValidation.ok();
        }

        public FormValidation doCheckUsername(@QueryParameter String value) {
            if(StringUtils.isEmpty(value)) {
                return FormValidation.error("Please enter a SSH username");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckPassword(@QueryParameter String value) {
            if(StringUtils.isEmpty(value)) {
                return FormValidation.error("Please enter a SSH password");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckCacert(@QueryParameter String value) {
            if(StringUtils.isEmpty(value)) {
                return FormValidation.error("Please enter a CA Certificate");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckCert(@QueryParameter String value) {
            if(StringUtils.isEmpty(value)) {
                return FormValidation.error("Please enter a Host Certificate");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckPrivateKey(@QueryParameter String value) {
            if(StringUtils.isEmpty(value)) {
                return FormValidation.error("Please enter a Private Key");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckEndpoint(@QueryParameter String value) {
            if(StringUtils.isEmpty(value)) {
                return FormValidation.error("Please select a PuppetDB Endpoint");
            }

            return FormValidation.ok();
        }
    }

    /**
     * Main method that handles the work
     * @param run
     * @param filePath
     * @param launcher
     * @param taskListener
     * @throws InterruptedException
     * @throws IOException
     */
    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener)
            throws InterruptedException, IOException
    {
        URL url;
        try {
            url = new URL(host);
        } catch (MalformedURLException e) {
            taskListener.getLogger().println("Received an invalid URL as host, aborting...");
            throw new RuntimeException(e);
        }

        SSLHelper sslHelper = new SSLHelper(cacert, cert, privateKey, taskListener.getLogger());
        PuppetDBClient client = new PuppetDBClient(url.getHost(), url.getPort(), sslHelper, taskListener.getLogger());
        client.runQuery(endpoint);
    }
}
