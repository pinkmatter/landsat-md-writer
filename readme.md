# Pinkmatter Landsat mission data writer

* Generates LPGS compatible mission data from a raw telemetry data.
* Supports both realtime, playback, and SSOH decoding.
* Also supports LDPC decoding.

## Binary releases

* Java is required to run the software and both Oracle JDK and OpenJDK is supported.
* Binary releases are available for download [here](https://github.com/pinkmatter/landsat-md-writer/releases)
* Extract the zip file and run the Java program (refer to example usages for more info).

## Building from sources

* Requires Apache Maven and at least Java 8.

```
git clone https://github.com/pinkmatter/landsat-md-writer.git
cd src/landsat-md-writer
mvn install
```

## Command line examples

* Command line arguments are as follows.

```
-i, --input-file <arg>         Required. Input file
-r, --realtime-output <arg>    Optional. Realtime output data directory.
-p, --playback-output <arg>    Optional. Playback output data directory.
-s, --ssoh-output <arg>        Optional. Satellite-state-of-health (SSOH) output data directory.
-q, --queued                   Optional. Whether to queue output writing (defaults to false).
-l, --ldpc-enabled             Optional. Enable LDPC decoding (defaults to false).
-d, --derandomize-ldpc-frame   Optional. Enable LDPC frame derandomization (defaults to false).
-c, --ldpc-correct-errors      Optional. Correct LDPC errors (defaults to false).
-m, --max-ldpc-errors <arg>    Optional. Max LDPC errors to fix (defaults to 60).
```

### 1. Typical decoding example

```
java -jar landsat-md-writer-1.1.6-release.jar \
  -q -i "Data0_Start2015_295_17_05_07_Stop2015_295_17_18_02.rec" \
  -r "output" 
```

The following directory listing shows an example of the decoded output.

```
-rwx---r-x    739.5M   Dec  3  11:39   350.002.2018337093933929
-rwx---r-x       244   Dec  3  11:40   350.002.2018337093933929.properties
-rwx---r-x    752.9M   Dec  3  11:44   351.000.2018337094007505
-rwx---r-x       178   Dec  3  11:44   351.000.2018337094007505.properties
-rwx---r-x   1017.8M   Dec  3  11:40   352.000.2018337094008995
-rwx---r-x       244   Dec  3  11:40   352.000.2018337094008995.properties
-rwx---r-x   1018.0M   Dec  3  11:40   352.001.2018337094035965
-rwx---r-x       245   Dec  3  11:40   352.001.2018337094035965.properties
-rwx---r-x   1017.5M   Dec  3  11:41   352.002.2018337094059805
-rwx---r-x       246   Dec  3  11:41   352.002.2018337094059805.properties
-rwx---r-x   1017.3M   Dec  3  11:41   352.003.2018337094126305
-rwx---r-x       246   Dec  3  11:41   352.003.2018337094126305.properties
-rwx---r-x   1016.8M   Dec  3  11:42   352.004.2018337094154245
-rwx---r-x       245   Dec  3  11:42   352.004.2018337094154245.properties
-rwx---r-x   1016.8M   Dec  3  11:42   352.005.2018337094225415
-rwx---r-x       245   Dec  3  11:42   352.005.2018337094225415.properties
-rwx---r-x   1018.6M   Dec  3  11:43   352.006.2018337094256215
-rwx---r-x       246   Dec  3  11:43   352.006.2018337094256215.properties
-rwx---r-x   1018.4M   Dec  3  11:43   352.007.2018337094316519
-rwx---r-x       246   Dec  3  11:43   352.007.2018337094316519.properties
-rwx---r-x   1017.7M   Dec  3  11:44   352.008.2018337094338235
-rwx---r-x       244   Dec  3  11:44   352.008.2018337094338235.properties
-rwx---r-x    126.4M   Dec  3  11:44   352.009.2018337094400804
-rwx---r-x       178   Dec  3  11:44   352.009.2018337094400804.properties
-rwx---r-x    327.3M   Dec  3  11:39   VC00.2018337093930715
-rwx---r-x       218   Dec  3  11:39   VC00.2018337093930715.properties
-rwx---r-x     96.5M   Dec  3  11:40   VC05.2018337093930725
-rwx---r-x       217   Dec  3  11:40   VC05.2018337093930725.properties
```

* For interval 352 (root file ID) a total of 10 sequential mission data files (sub root file IDs 000 through 009) were decoded.
* Interval 352 would consist of OLI data, where 350 and 351 would likely be TIRS data.
* Data decoded before the start of an full interval are included as leading partials.
* In this case leading partials for VC0 (OLI) and VC5 (TIRS) were decoded.
* Root file IDs and sub-root file IDs can be compared to official USGS STS and CCS ancillary files to validate expected telemetry ranges.

### 2. Decoding both real-time, playback and SSOH data

```
java -jar landsat-md-writer-1.1.6-release.jar \
  -q -i "Data0_Start2015_295_17_05_07_Stop2015_295_17_18_02.rec" \
  -r "output/realtime" -p "output/playback" -s "output/ssoh"
```

### 3. Decoding LDPC source data

```
java -jar landsat-md-writer-1.1.6-release.jar \
  -q -i "Data0_Start2015_295_17_05_07_Stop2015_295_17_18_02.rec" \
  -r "output/realtime" -p "output/playback" -s "output/ssoh" \
  -l -d -c -m 60
```

## Licence

* Refer to [LICENSE](LICENSE)