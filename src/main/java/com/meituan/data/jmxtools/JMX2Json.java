package com.meituan.data.jmxtools;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.meituan.data.jmxtools.conf.Endpoint;
import com.meituan.data.jmxtools.jmx.JmxConnections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * This class simulates functions of Hadoop's JMXJsonServlet,
 * provides a JSON representation of all the JMX MBeans in a Java program.
 *
 * The return format is in the form
 * <p>
 *  <code><pre>
 *  {
 *    "beans" : [
 *      {
 *        "name":"bean-name"
 *        ...
 *      }
 *    ]
 *  }
 *  </pre></code>
 *  <p>
 *  The program attempts to convert the the JMXBeans into JSON. Each
 *  bean's attributes will be converted to a JSON object member.
 *
 *  If the attribute is a boolean, a number, a string, or an array
 *  it will be converted to the JSON equivalent.
 *
 *  If the value is a {@link CompositeData} then it will be converted
 *  to a JSON object with the keys as the name of the JSON member and
 *  the value is converted following these same rules.
 *
 *  If the value is a {@link TabularData} then it will be converted
 *  to an array of the {@link CompositeData} elements that it contains.
 *
 *  All other objects will be converted to a string and output as such.
 *
 *  The bean's name and modelerType will be returned for all beans.
 */
public class Jmx2Json implements AutoCloseable {
    final static Logger LOG = LoggerFactory.getLogger(Jmx2Json.class);
    final static long DEFAULT_JMX_TIMEOUT_SECOND = 10;

    JMXConnector connector;
    MBeanServerConnection mBeanServer;

    public Jmx2Json(Endpoint endpoint) throws IOException {
        connector = JmxConnections.connectWithTimeout(endpoint, DEFAULT_JMX_TIMEOUT_SECOND, TimeUnit.SECONDS);
        mBeanServer = connector.getMBeanServerConnection();
    }

    @Override
    public void close() throws IOException {
        if (connector != null) {
            connector.close();
        }
    }

    static void printUsageAndExit() {
        final String usage = "Usage:\n" +
                "jmx2json --remote host:port\n" +
                "         --local <process-regex>\n";
        System.err.println(usage);
        System.exit(1);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2 || !("--remote".equals(args[0]) || "--local".equals(args[0]))) {
            printUsageAndExit();
        }

        Endpoint endpoint = new Endpoint(/*name=*/args[1], /*isRemote=*/"--remote".equals(args[0]));

        try (Jmx2Json jmx2Json = new Jmx2Json(endpoint)) {
            jmx2Json.printMBeansAsJson(new OutputStreamWriter(System.out));
        }
    }

    // ----------------------------------------------------------------
    // Functions pertaining to serializing JMX info to JSON.
    //
    // Code brought from org.apache.hadoop.jmx.JMXJsonServlet.
    // ----------------------------------------------------------------

    private void printMBeansAsJson(Writer writer) throws IOException {
        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator jg = jsonFactory.createGenerator(writer);
        jg.useDefaultPrettyPrinter();
        jg.writeStartObject();

        try {
            listBeans(jg, new ObjectName("*:*"), null);

        } catch (MalformedObjectNameException e) {
            throw new AssertionError(e);
        }

        jg.close();
    }

    private void listBeans(JsonGenerator jg, ObjectName qry, String attribute)
            throws IOException {
        LOG.debug("Listing beans for "+qry);
        Set<ObjectName> names = null;
        names = mBeanServer.queryNames(qry, null);

        jg.writeArrayFieldStart("beans");
        for (ObjectName oname : names) {
            MBeanInfo minfo;
            String code = "";
            Object attributeinfo = null;
            try {
                minfo = mBeanServer.getMBeanInfo(oname);
                code = minfo.getClassName();
                String prs = "";
                try {
                    if ("org.apache.commons.modeler.BaseModelMBean".equals(code)) {
                        prs = "modelerType";
                        code = (String) mBeanServer.getAttribute(oname, prs);
                    }
                    if (attribute != null) {
                        prs = attribute;
                        attributeinfo = mBeanServer.getAttribute(oname, prs);
                    }
                } catch (AttributeNotFoundException | MBeanException | ReflectionException | RuntimeException e) {
                    LOG.error("getting attribute " + prs + " of " + oname
                            + " threw an exception", e);
                }
            } catch (InstanceNotFoundException e) {
                //Ignored for some reason the bean was not found so don't output it
                continue;
            } catch (IntrospectionException e) {
                // This is an internal error, something odd happened with reflection so
                // log it and don't output the bean.
                LOG.error("Problem while trying to process JMX query: " + qry
                        + " with MBean " + oname, e);
                continue;
            } catch (ReflectionException e) {
                // This happens when the code inside the JMX bean threw an exception, so
                // log it and don't output the bean.
                LOG.error("Problem while trying to process JMX query: " + qry
                        + " with MBean " + oname, e);
                continue;
            }

            jg.writeStartObject();
            jg.writeStringField("name", oname.toString());

            jg.writeStringField("modelerType", code);
            if ((attribute != null) && (attributeinfo == null)) {
                jg.writeStringField("result", "ERROR");
                jg.writeStringField("message", "No attribute with name " + attribute
                        + " was found.");
                jg.writeEndObject();
                jg.writeEndArray();
                jg.close();
                return;
            }

            if (attribute != null) {
                writeAttribute(jg, attribute, attributeinfo);
            } else {
                MBeanAttributeInfo attrs[] = minfo.getAttributes();
                for (MBeanAttributeInfo attr : attrs) {
                    writeAttribute(jg, oname, attr);
                }
            }
            jg.writeEndObject();
        }
        jg.writeEndArray();
    }

    private void writeAttribute(JsonGenerator jg, ObjectName oname, MBeanAttributeInfo attr) throws IOException {
        if (!attr.isReadable()) {
            return;
        }
        String attName = attr.getName();
        if ("modelerType".equals(attName)) {
            return;
        }
        if (attName.contains("=") || attName.contains(":") || attName.contains(" ")) {
            return;
        }

        Object value;
        try {
            value = mBeanServer.getAttribute(oname, attName);
        } catch (RuntimeMBeanException e) {
            // UnsupportedOperationExceptions happen in the normal course of business,
            // so no need to log them as errors all the time.
            if (e.getCause() instanceof UnsupportedOperationException) {
                LOG.debug("getting attribute "+attName+" of "+oname+" threw an exception", e);
            } else {
                LOG.error("getting attribute "+attName+" of "+oname+" threw an exception", e);
            }
            return;
        } catch (RuntimeErrorException e) {
            // RuntimeErrorException happens when an unexpected failure occurs in getAttribute
            // for example https://issues.apache.org/jira/browse/DAEMON-120
            LOG.debug("getting attribute "+attName+" of "+oname+" threw an exception", e);
            return;
        } catch (AttributeNotFoundException e) {
            //Ignored the attribute was not found, which should never happen because the bean
            //just told us that it has this attribute, but if this happens just don't output
            //the attribute.
            return;
        } catch (MBeanException | RuntimeException | InstanceNotFoundException | ReflectionException e) {
            LOG.error("getting attribute "+attName+" of "+oname+" threw an exception", e);
            return;
        }

        writeAttribute(jg, attName, value);
    }

    private void writeAttribute(JsonGenerator jg, String attName, Object value) throws IOException {
        jg.writeFieldName(attName);
        writeObject(jg, value);
    }

    private void writeObject(JsonGenerator jg, Object value) throws IOException {
        if(value == null) {
            jg.writeNull();
        } else {
            Class<?> c = value.getClass();
            if (c.isArray()) {
                jg.writeStartArray();
                int len = Array.getLength(value);
                for (int j = 0; j < len; j++) {
                    Object item = Array.get(value, j);
                    writeObject(jg, item);
                }
                jg.writeEndArray();
            } else if(value instanceof Number) {
                Number n = (Number)value;
                jg.writeNumber(n.toString());
            } else if(value instanceof Boolean) {
                Boolean b = (Boolean)value;
                jg.writeBoolean(b);
            } else if(value instanceof CompositeData) {
                CompositeData cds = (CompositeData)value;
                CompositeType comp = cds.getCompositeType();
                Set<String> keys = comp.keySet();
                jg.writeStartObject();
                for(String key: keys) {
                    writeAttribute(jg, key, cds.get(key));
                }
                jg.writeEndObject();
            } else if(value instanceof TabularData) {
                TabularData tds = (TabularData)value;
                jg.writeStartArray();
                for(Object entry : tds.values()) {
                    writeObject(jg, entry);
                }
                jg.writeEndArray();
            } else {
                jg.writeString(value.toString());
            }
        }
    }
}
