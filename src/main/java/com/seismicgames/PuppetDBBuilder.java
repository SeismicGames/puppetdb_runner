package com.seismicgames;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHAuthenticator;
import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.seismicgames.pojo.HostInfo;
import com.seismicgames.ssh.SSHClient;
import com.seismicgames.ssh.SSHClientTask;
import com.seismicgames.ssh.SSHClientWithKey;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

public class PuppetDBBuilder extends Builder implements SimpleBuildStep {
    // static variables
    private static final String DISPLAY_NAME = "Refresh Puppet Agents";

    // plugin variables
    private final String host;
    private final String query;
    private final String endpoint;
    private final String credentialsId;

    @DataBoundConstructor
    public PuppetDBBuilder(String host, String query, String endpoint, String credentialsId) {
        this.host = host;
        this.query = query;
        this.endpoint = endpoint;
        this.credentialsId = credentialsId;
    }

    // getters for Jelly
    public String getHost() {
        return host;
    }

    public String getQuery() {
        return query;
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
        @Nonnull
        public String getDisplayName() {
            return DISPLAY_NAME;
        }

        // Credentials select
        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {
            StandardListBoxModel result = new StandardListBoxModel();

            if(item == null) {
                if(!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                    return result.includeCurrentValue(credentialsId);
                }
            } else {
                if(!item.hasPermission(Item.EXTENDED_READ) && item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return result.includeCurrentValue(credentialsId);
                }
            }

            return result
                    .includeMatchingAs(
                            item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task)item) : ACL.SYSTEM,
                            item,
                            SSHUserPrivateKey.class,
                            Collections.emptyList(),
                            SSHAuthenticator.matcher()
                    )
                    .includeMatchingAs(
                            item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task)item) : ACL.SYSTEM,
                            item,
                            StandardUsernamePasswordCredentials.class,
                            Collections.emptyList(),
                            CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class)
                    )
                    .includeCurrentValue(credentialsId);
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

        public FormValidation doCheckEndpoint(@QueryParameter String value) {
            if(StringUtils.isEmpty(value)) {
                return FormValidation.error("Please select a PuppetDB Endpoint");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckCredentialsId(@AncestorInPath Item item, @QueryParameter String value) {
            if(item == null) {
                if(!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } else {
                if(!item.hasPermission(Item.EXTENDED_READ) && item.hasPermission(CredentialsProvider.USE_ITEM)) {
                    return FormValidation.ok();
                }
            }

            if(StringUtils.isEmpty(value)) {
                return FormValidation.error("Please select valid credentials");
            }

            if(value.startsWith("${") && value.endsWith("}")) {
                return FormValidation.error("Cannot use expression based credentials");
            }

            // check SSH values
            for (ListBoxModel.Option o : CredentialsProvider.listCredentials(
                    SSHUserPrivateKey.class,
                    item,
                    item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task)item) : ACL.SYSTEM,
                    Collections.emptyList(),
                    SSHAuthenticator.matcher()
            )) {
                if(StringUtils.equals(value, o.value)) {
                    return FormValidation.ok();
                }
            }

            // check username/password values
            for (ListBoxModel.Option o : CredentialsProvider.listCredentials(
                    StandardUsernamePasswordCredentials.class,
                    item,
                    item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task)item) : ACL.SYSTEM,
                    Collections.emptyList(),
                    CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class)
            )) {
                if(StringUtils.equals(value, o.value)) {
                    return FormValidation.ok();
                }
            }

            return FormValidation.ok();
        }
    }

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener)
            throws InterruptedException, IOException
    {
        // check which credentials we are using
        StandardUsernamePasswordCredentials creds = null;
        SSHUserPrivateKey privateKey = CredentialsProvider.findCredentialById(
                credentialsId,
                SSHUserPrivateKey.class,
                run,
                Collections.emptyList()
        );

        if(privateKey == null) {
            creds = CredentialsProvider.findCredentialById(
                    credentialsId,
                    StandardUsernamePasswordCredentials.class,
                    run,
                    Collections.emptyList()
            );

            if(creds == null) {
                throw new RuntimeException("Can't find valid credentials to use to continue");
            }
        }

        // expression evaluate the query and host
        String evalHost = "";
        String evalQuery = "";

        try {
            evalHost = TokenMacro.expandAll(run, filePath, taskListener, host);
            evalQuery = TokenMacro.expandAll(run, filePath, taskListener, query);
        } catch (MacroEvaluationException e) {
            taskListener.getLogger().println("MacroEvaluationException: " + e.getMessage());
            e.printStackTrace();
        }

        // query puppetdb for machines
        URL url;
        PuppetDBClient.Endpoint ePoint = PuppetDBClient.Endpoint.fromString(endpoint);
        try {
            url = new URL(evalHost);
        } catch (MalformedURLException e) {
            taskListener.getLogger().println("Received an invalid URL as host, aborting...");
            throw new RuntimeException(e);
        }

        PuppetDBClient client = new PuppetDBClient(url, evalQuery, taskListener.getLogger());
        Collection<HostInfo> instances = client.run(ePoint);

        if(instances.isEmpty()) {
            taskListener.getLogger().println("No nodes found");
            return;
        }

        // ssh into machines
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("PuppetDBBuilder-%d")
                .build();
        ExecutorService executorService = Executors.newFixedThreadPool(5,threadFactory);
        List<SSHClientTask> tasks = new ArrayList<>();

        if(privateKey == null) {
            taskListener.getLogger().println("Using username/password for SSH auth");
            for (HostInfo instance : instances) {
                tasks.add(new SSHClient(instance.host, creds.getUsername(), creds.getPassword(), instance.rsaKey,
                        taskListener.getLogger()));
            }
        } else {
            taskListener.getLogger().println("Using private key for SSH auth");
            for (HostInfo instance : instances) {
                tasks.add(new SSHClientWithKey(instance.host, privateKey.getUsername(),
                        privateKey.getPrivateKeys(), privateKey.getPassphrase(), instance.rsaKey,
                        taskListener.getLogger()));
            }
        }

        executorService.invokeAll(tasks);
        executorService.shutdown();
    }
}
