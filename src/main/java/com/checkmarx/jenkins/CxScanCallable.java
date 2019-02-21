package com.checkmarx.jenkins;

import com.cx.restclient.CxShragaClient;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.exception.CxClientException;
import com.cx.restclient.osa.dto.OSAResults;
import com.cx.restclient.sast.dto.SASTResults;
import hudson.FilePath;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;


public class CxScanCallable implements FilePath.FileCallable<ScanResults>, Serializable {

    private static final long serialVersionUID = 1L;

    private final CxScanConfig config;
    private final TaskListener listener;

    public CxScanCallable(CxScanConfig config, TaskListener listener) {
        this.config = config;
        this.listener = listener;
    }

    @Override
    public ScanResults invoke(File file, VirtualChannel channel) throws IOException, InterruptedException {

        CxLoggerAdapter log = new CxLoggerAdapter(listener.getLogger());
        config.setSourceDir(file.getAbsolutePath());
        config.setReportsDir(file);
        ScanResults ret = new ScanResults();
        ret.setSastResults(new SASTResults());
        ret.setOsaResults(new OSAResults());

        boolean sastCreated = false;
        boolean osaCreated = false;

        CxShragaWrapper shraga = new CxShragaWrapper(config, log);
        try {
            shraga.init();
        } catch (Exception e) {
            throw new IOException(e);
        }

        if (config.getSastEnabled()) {
            try {
                shraga.createSASTScan();
                sastCreated = true;
            } catch (IOException | CxClientException e) {
                log.warn("Failed to create SAST scan: " +e.getMessage());
                ret.setSastCreateException(e);
            }
        }

        if (config.getOsaEnabled()) {
            //---------------------------
            //we do this in order to redirect the logs from the filesystem agent component to the build console
            Logger rootLog = Logger.getLogger("");
            StreamHandler handler = new StreamHandler(listener.getLogger(), new ComponentScanFormatter());
            handler.setLevel(Level.ALL);
            rootLog.addHandler(handler);
            //---------------------------

            try {
                shraga.createOSAScan();
                osaCreated = true;
            } catch (CxClientException | IOException e) {
                log.error("Failed to create OSA scan: " + e.getMessage());
                ret.setOsaCreateException(e);
            } finally {
                handler.flush();
                rootLog.removeHandler(handler);
            }
        }

        if (sastCreated) {
            try {
                SASTResults sastResults = config.getSynchronous() ? shraga.waitForSASTResults() : shraga.getLatestSASTResults();
                ret.setSastResults(sastResults);
            } catch (InterruptedException e) {
                if(config.getSynchronous()) {
                    cancelScan(shraga);
                }
                throw e;

            } catch (CxClientException | IOException e) {
                log.error("Failed to get SAST scan results: " + e.getMessage());
                ret.setSastWaitException(e);
            }
        }

        if (osaCreated) {
            try {
                OSAResults osaResults = config.getSynchronous() ? shraga.waitForOSAResults() : shraga.getLatestOSAResults();
                ret.setOsaResults(osaResults);
            } catch (CxClientException | IOException e) {
                log.error("Failed to get OSA scan results: " + e.getMessage());
                ret.setOsaWaitException(e);
            }
        }

        return ret;
    }

    private void cancelScan(CxShragaClient shraga) {
        try {
            shraga.cancelSASTScan();
        } catch (Exception ignored) {
        }
    }
}
