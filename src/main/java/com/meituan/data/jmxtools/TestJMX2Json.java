package com.meituan.data.jmxtools;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Set;

// TODO refactor into a runnable tools
public class TestJMX2Json {
    final Logger LOG = LoggerFactory.getLogger(TestJMX2Json.class);

    MBeanServerConnection mBeanServer;

    public TestJMX2Json(String host, int port) throws IOException {
        mBeanServer = createConnection(host, port);
    }

    private MBeanServerConnection createConnection(String host, int port) throws IOException {
        // connect to remote mbean server
        JMXServiceURL address = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi");
        JMXConnector jmxc = JMXConnectorFactory.connect(address);
        return jmxc.getMBeanServerConnection();
    }

    public static void main(String[] args) throws IOException {
        new TestJMX2Json(args[0], Integer.valueOf(args[1])).printMBeansAsJson(new OutputStreamWriter(System.out));
    }

    private void printMBeansAsJson(Writer writer) throws IOException {
        JsonFactory jsonFactory = new JsonFactory();
        JsonGenerator jg = jsonFactory.createGenerator(writer);
        jg.useDefaultPrettyPrinter();
        jg.writeStartObject();

        try {
            listBeans(jg, new ObjectName("*:*"), null);

        } catch (MalformedObjectNameException e) {
            LOG.error("shouldn't happen", e);
        }

        jg.close();
    }

    private void listBeans(JsonGenerator jg, ObjectName qry, String attribute)
            throws IOException {
        LOG.debug("Listing beans for "+qry);
        Set<ObjectName> names = null;
        names = mBeanServer.queryNames(qry, null);

        jg.writeArrayFieldStart("beans");
        Iterator<ObjectName> it = names.iterator();
        while (it.hasNext()) {
            ObjectName oname = it.next();
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
                    if (attribute!=null) {
                        prs = attribute;
                        attributeinfo = mBeanServer.getAttribute(oname, prs);
                    }
                } catch (AttributeNotFoundException e) {
                    // If the modelerType attribute was not found, the class name is used
                    // instead.
                    LOG.error("getting attribute " + prs + " of " + oname
                            + " threw an exception", e);
                } catch (MBeanException e) {
                    // The code inside the attribute getter threw an exception so log it,
                    // and fall back on the class name
                    LOG.error("getting attribute " + prs + " of " + oname
                            + " threw an exception", e);
                } catch (RuntimeException e) {
                    // For some reason even with an MBeanException available to them
                    // Runtime exceptionscan still find their way through, so treat them
                    // the same as MBeanException
                    LOG.error("getting attribute " + prs + " of " + oname
                            + " threw an exception", e);
                } catch ( ReflectionException e ) {
                    // This happens when the code inside the JMX bean (setter?? from the
                    // java docs) threw an exception, so log it and fall back on the
                    // class name
                    LOG.error("getting attribute " + prs + " of " + oname
                            + " threw an exception", e);
                }
            } catch (InstanceNotFoundException e) {
                //Ignored for some reason the bean was not found so don't output it
                continue;
            } catch ( IntrospectionException e ) {
                // This is an internal error, something odd happened with reflection so
                // log it and don't output the bean.
                LOG.error("Problem while trying to process JMX query: " + qry
                        + " with MBean " + oname, e);
                continue;
            } catch ( ReflectionException e ) {
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
                for (int i = 0; i < attrs.length; i++) {
                    writeAttribute(jg, oname, attrs[i]);
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
        if (attName.indexOf("=") >= 0 || attName.indexOf(":") >= 0
                || attName.indexOf(" ") >= 0) {
            return;
        }
        Object value = null;
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
        } catch (MBeanException e) {
            //The code inside the attribute getter threw an exception so log it, and
            // skip outputting the attribute
            LOG.error("getting attribute "+attName+" of "+oname+" threw an exception", e);
            return;
        } catch (RuntimeException e) {
            //For some reason even with an MBeanException available to them Runtime exceptions
            //can still find their way through, so treat them the same as MBeanException
            LOG.error("getting attribute "+attName+" of "+oname+" threw an exception", e);
            return;
        } catch (ReflectionException e) {
            //This happens when the code inside the JMX bean (setter?? from the java docs)
            //threw an exception, so log it and skip outputting the attribute
            LOG.error("getting attribute "+attName+" of "+oname+" threw an exception", e);
            return;
        } catch (InstanceNotFoundException e) {
            //Ignored the mbean itself was not found, which should never happen because we
            //just accessed it (perhaps something unregistered in-between) but if this
            //happens just don't output the attribute.
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
