package com.checkmarx.jenkins;

import com.checkmarx.ws.CxJenkinsWebService.*;
import hudson.EnvVars;
import org.apache.log4j.Logger;

/**
 * Created by tsahib on 7/5/2016.
 */
public class CliScanArgsFactory {

    private Logger logger;
    private String preset;
    private final String projectName;
    private final String groupId;
    private final String sourceEncoding;
    private final String comment;
    private final boolean isThisBuildIncremental;
    private final byte[] compressedSources;
    private final EnvVars env;

    public CliScanArgsFactory(Logger logger, String preset, String projectName, String groupId, String sourceEncoding, String comment, boolean isThisBuildIncremental, byte[] compressedSources, EnvVars env) {
        this.logger = logger;
        this.preset = preset;
        this.projectName = projectName;
        this.groupId = groupId;
        this.sourceEncoding = sourceEncoding;
        this.comment = comment;
        this.isThisBuildIncremental = isThisBuildIncremental;
        this.compressedSources = compressedSources;
        this.env = env;
    }

    public CliScanArgs create() {

        ProjectSettings projectSettings = new ProjectSettings();

        long presetLong = 0; // Default value to use in case of exception
        try {
            presetLong = Long.parseLong(preset);
        } catch (Exception e) {
            logger.error("Encountered illegal preset value: " + preset + ". Using default preset.");
        }

        projectSettings.setPresetID(presetLong);
        projectSettings.setProjectName(env.expand(projectName));
        projectSettings.setAssociatedGroupID(groupId);

        long configuration = 0; // Default value to use in case of exception
        try {
            configuration = Long.parseLong(sourceEncoding);
        } catch (Exception e) {
            logger.error("Encountered illegal source encoding (configuration) value: " + sourceEncoding + ". Using default configuration.");
        }
        projectSettings.setScanConfigurationID(configuration);

        LocalCodeContainer localCodeContainer = new LocalCodeContainer();
        localCodeContainer.setFileName("src.zip");
        localCodeContainer.setZippedFile(compressedSources);

        SourceCodeSettings sourceCodeSettings = new SourceCodeSettings();
        sourceCodeSettings.setSourceOrigin(SourceLocationType.LOCAL);
        sourceCodeSettings.setPackagedCode(localCodeContainer);

        String commentText = comment != null ? env.expand(comment) : "";
        commentText = commentText.trim();

        CliScanArgs args = new CliScanArgs();
        args.setIsIncremental(isThisBuildIncremental);
        args.setIsPrivateScan(false);
        args.setPrjSettings(projectSettings);
        args.setSrcCodeSettings(sourceCodeSettings);
        args.setComment(commentText);

        return args;
    }
}
