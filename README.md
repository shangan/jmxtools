WARNING: this description is outdated.

Report JMX metrics to Falcon according to `setting.json`:
```
./jmx2falcon-0.1-uber.jar report [--dry-run] --conf conf/setting.json
```

Dump JMX metrics of remote java process as a JSON document:
```
./jmx2falcon-0.1-uber.jar dump host:port
```


A hypothetical file structure for a metrics collecting service using jmx2falcon. 
```
bin/
  jmx2falcon
conf/
  hivemetastore.json
  hiveserver2.json
  presto.json
lib/
  jmx2falcon-0.1-uber.jar
```

