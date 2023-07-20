/*
 * Copyright (c) 2019 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at:
 *
 *     https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.jkube.kit.enricher.handler;

import io.fabric8.kubernetes.api.model.ExecAction;
import io.fabric8.kubernetes.api.model.HTTPGetAction;
import io.fabric8.kubernetes.api.model.HTTPHeader;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Probe;
import io.fabric8.kubernetes.api.model.TCPSocketAction;
import org.eclipse.jkube.kit.common.util.CommandLine;
import org.eclipse.jkube.kit.config.resource.ProbeConfig;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.eclipse.jkube.kit.common.util.KubernetesHelper.convertMapToHTTPHeaderList;


/**
 * @author roland
 * @since 07/04/16
 */
public class ProbeHandler {

    public Probe getProbe(ProbeConfig probeConfig)  {
        if (probeConfig == null) {
            return null;
        }

        Probe probe = new Probe();
        setTimeoutInProbeIfNotNull(probe, probeConfig::getInitialDelaySeconds, (i, p) -> p.setInitialDelaySeconds(i));
        setTimeoutInProbeIfNotNull(probe, probeConfig::getTimeoutSeconds, (i, p) -> p.setTimeoutSeconds(i));
        setTimeoutInProbeIfNotNull(probe, probeConfig::getFailureThreshold, (i, p) -> p.setFailureThreshold(i));
        setTimeoutInProbeIfNotNull(probe, probeConfig::getSuccessThreshold, (i, p) -> p.setSuccessThreshold(i));
        setTimeoutInProbeIfNotNull(probe, probeConfig::getPeriodSeconds, (i, p) -> p.setPeriodSeconds(i));
        HTTPGetAction getAction = getHTTPGetAction(probeConfig.getGetUrl(), probeConfig.getHttpHeaders());
        if (getAction != null) {
            probe.setHttpGet(getAction);
            return probe;
        }
        ExecAction execAction = getExecAction(probeConfig.getExec());
        if (execAction != null) {
            probe.setExec(execAction);
            return probe;
        }
        TCPSocketAction tcpSocketAction = getTCPSocketAction(probeConfig.getGetUrl(), probeConfig.getTcpPort());
        if (tcpSocketAction != null) {
            probe.setTcpSocket(tcpSocketAction);
            return probe;
        }

        return null;
    }

    // ========================================================================================

    private HTTPGetAction getHTTPGetAction(String getUrl, Map<String, String> headers) {
        if (getUrl == null || !getUrl.subSequence(0,4).toString().equalsIgnoreCase("http")) {
            return null;
        }
        try {
            URL url = new URL(getUrl);
            List<HTTPHeader> httpHeaders = convertMapToHTTPHeaderList(headers);

            return new HTTPGetAction(url.getHost(),
                    httpHeaders.isEmpty() ? null : httpHeaders,
                    url.getPath(),
                    new IntOrString(url.getPort()),
                    url.getProtocol().toUpperCase());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL " + getUrl + " given for HTTP GET readiness check");
        }
    }

    @SuppressWarnings("java:S4276") // IntSupplier throws NullPointerException when unboxing null Integers
    private void setTimeoutInProbeIfNotNull(Probe probe, Supplier<Integer> integerSupplier, BiConsumer<Integer, Probe> probeConsumer) {
        Integer i = integerSupplier.get();
        if (i != null) {
            probeConsumer.accept(i, probe);
        }
    }

    private TCPSocketAction getTCPSocketAction(String getUrl, String port) {
        if (port != null) {
            IntOrString portObj;
            try {
                portObj = new IntOrString(Integer.parseInt(port));
            } catch (NumberFormatException e) {
                portObj = new IntOrString(port);
            }
            if(getUrl==null)
                return new TCPSocketAction(getUrl, portObj);
            String validurl = getUrl.replaceFirst("(([a-zA-Z])+)://","http://");
            try{
                URL url = new URL(validurl);
                return new TCPSocketAction(url.getHost(), portObj);
            }
            catch (MalformedURLException e){
                throw new IllegalArgumentException("Invalid URL " + getUrl + " given for TCP readiness check");
            }
        }
        return null;
    }

    private ExecAction getExecAction(String execCmd) {
        if (isNotBlank(execCmd)) {
            List<String> splitCommandLine = CommandLine.translateCommandline(execCmd);
            if (!splitCommandLine.isEmpty()) {
                return new ExecAction(splitCommandLine);
            }
        }
        return null;
    }
}
