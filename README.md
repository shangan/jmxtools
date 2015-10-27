jmxtools is a library for collecting and reporting JMX metrics for java system.

```
Usage: jmxtools COMMAND
---------
where COMMAND is one of:
  report          collect JMX metrics and report to a monitoring system
  collect         collect JMX metrics and print them
  jmx2json        print a JSON representation of JMX information

Examples:
---------
jmxtools report <path-to-config-file>
jmxtools collect <endpoint>
                 --conf <path-to-config-file>
jmxtools jmx2json <endpoint>

where endpoint is one of:
local:process-regex     for local JVM
remote:host:port        for remote JVM
```

