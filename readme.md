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

## Example usage

```
java -jar target/landsat8-cadu-reader-1.1.6-release.jar \
  -q -i "Data0_Start2015_295_17_05_07_Stop2015_295_17_18_02.rec" \
  -r "output/realtime" \
  -p "output/playback" \
  -s "output/ssoh"
```

## Licence

* Refer to [LICENSE](LICENSE)