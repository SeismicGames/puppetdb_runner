package com.seismicgames;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;

import javax.annotation.Nonnull;
import java.io.IOException;

public class PuppetDBBuilder extends Builder implements SimpleBuildStep {

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener)
            throws InterruptedException, IOException
    {


    }
}
