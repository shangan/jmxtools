package com.meituan.data.jmxtools.jmx;

import com.meituan.data.jmxtools.conf.Endpoint;
import com.meituan.data.jmxtools.utils.DaemonThreadFactory;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;

/**
 * Manage JMX connections.
 */
public class JmxConnections {
    static final Logger LOG = LoggerFactory.getLogger(JmxConnections.class);
    static final String LOCAL_CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

    public static JMXConnector connectWithTimeout(Endpoint endpoint, long timeout, TimeUnit unit) throws IOException {
        JMXServiceURL url;

        if (endpoint.isRemote()) {
            url = getRemoteURL(endpoint.getRemoteHost(), endpoint.getRemotePort());
        } else {
            url = getLocalAttachURL(endpoint.getName());
        }

        LOG.info("Trying to connect {}", url);
        return connectWithTimeout(url, timeout, unit);
    }

    private static JMXServiceURL getRemoteURL(String host, int port) throws MalformedURLException {
        return new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi");
    }

    private static JMXServiceURL getLocalAttachURL(String processRegex) throws IOException {
        try {
            for (VirtualMachineDescriptor vmd : VirtualMachine.list()) {
                if (vmd.displayName().matches(processRegex)) {
                    VirtualMachine vm = VirtualMachine.attach(vmd);
                    String connectorAddress = vm.getAgentProperties().getProperty(LOCAL_CONNECTOR_ADDRESS);

                    //If jmx agent is not running in VM, load it and return the connector url
                    if (connectorAddress == null) {
                        LOG.debug("JMX agent not running in JVM({}), load it", vmd.id());
                        loadJMXAgent(vm);

                        // agent is started, get the connector address
                        connectorAddress = vm.getAgentProperties().getProperty(LOCAL_CONNECTOR_ADDRESS);
                    }

                    return new JMXServiceURL(connectorAddress);
                }
            }
            throw new IOException("Cannot find JVM matching regex: " + processRegex);

        } catch (AttachNotSupportedException e) {
            throw new IOException("Unable to attach to process regex: " + processRegex, e);
        }
    }

    private static void loadJMXAgent(VirtualMachine vm) throws IOException {
        String agent = vm.getSystemProperties().getProperty("java.home")
                + File.separator + "lib" + File.separator + "management-agent.jar";
        try {
            vm.loadAgent(agent);
        } catch (Exception e) {
            throw new IOException("Failed to load JMX agent", e);
        }
    }

    /**
     * Connect to a MBean Server with a timeout. Based on
     * https://weblogs.java.net/blog/emcmanus/archive/2007/05/making_a_jmx_co.html
     */
    private static JMXConnector connectWithTimeout(final JMXServiceURL url, long timeout, TimeUnit unit) throws IOException {
        final BlockingQueue<Object> mailbox = new ArrayBlockingQueue<Object>(1);

        // make connection in another thread so that we can timeout the request
        ExecutorService executor = Executors.newSingleThreadExecutor(DaemonThreadFactory.instance);
        executor.submit(new Runnable() {
            public void run() {
                try {
                    JMXConnector connector = JMXConnectorFactory.connect(url, null);
                    if (!mailbox.offer(connector)) {
                        connector.close();  //
                    }
                } catch (Throwable t) {
                    mailbox.offer(t);
                }
            }
        });

        // poll the result with timeout
        Object result;
        try {
            result = mailbox.poll(timeout, unit);
            if (result == null) {
                if (!mailbox.offer(""))
                    result = mailbox.take();    // connected right after timeout
            }
        } catch (InterruptedException e) {
            InterruptedIOException wrapper = new InterruptedIOException(e.getMessage());
            wrapper.initCause(e);
            throw wrapper;
        } finally {
            executor.shutdown();
        }
        if (result == null) {
            LOG.warn("Connection timed out: " + url);
            throw new SocketTimeoutException("Connection timed out: " + url);
        }
        if (result instanceof JMXConnector) {
            return (JMXConnector) result;
        }
        try {
            throw (Throwable) result;
        } catch (Throwable e) {
            throw new IOException(e.toString(), e);
        }
    }
}
