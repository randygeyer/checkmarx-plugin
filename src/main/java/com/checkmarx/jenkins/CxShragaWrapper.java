package com.checkmarx.jenkins;

import java.net.MalformedURLException;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import com.cx.restclient.CxShragaClient;
import com.cx.restclient.httpClient.CxProxyUtility;
import com.cx.restclient.configuration.CxScanConfig;
import com.cx.restclient.exception.CxClientException;

import hudson.AbortException;
import jenkins.model.Jenkins;

/**
 * Wraps the CxShragaClient adding support for Proxy configuration.
 * 
 * @author randy@checkmarx.com
 *
 */
public class CxShragaWrapper extends CxShragaClient {

    private void configureProxy(@NotNull String cxUrl,
            @NotNull Logger logger) throws AbortException {
    
	    try {
			CxScanBuilder.DescriptorImpl descriptor = (CxScanBuilder.DescriptorImpl) 
			        Jenkins.getInstance().getDescriptor(CxScanBuilder.class);

			if (descriptor != null) {
			    CxProxyUtility.configureProxy(cxUrl, descriptor.isUseProxy(),
			            descriptor.getProxyHost(), 
			            descriptor.getProxyPort(),
			            logger);
			}
		} catch (CxClientException e) {
			final String errMsg = "Configure proxy failed: " + e.getMessage();
			logger.error(errMsg, e);
			throw new AbortException(errMsg);
		} 
    }

	public CxShragaWrapper(@NotNull CxScanConfig config, @NotNull Logger log) throws MalformedURLException, AbortException {
		super(config, log);
        configureProxy(config.getUrl(), log);
	}

    public CxShragaWrapper(String serverUrl, String username, String password,
    		boolean useProxy, String proxyHost, Integer proxyPort, 
    		String origin, boolean disableCertificateValidation, Logger log) throws MalformedURLException, AbortException {
        super(new CxScanConfig(serverUrl, username, password, origin, disableCertificateValidation), log);
        
        try {
			CxProxyUtility.configureProxy(serverUrl, useProxy, proxyHost, proxyPort, log);
		} catch (CxClientException e) {
			final String errMsg = "Configure proxy failed: " + e.getMessage();
			log.error(errMsg, e);
			throw new AbortException(errMsg);
		}
    }

}
