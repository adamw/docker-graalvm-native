# Docker + GraalVM native-image

1. create locally the `graalvm-native-image` container using `graalvm-native-image/build.sh`. This container will be used to build the native image.
```bash
cd graalvm-native-image
./build.sh
cd ..
```
2. `sbt dockerGraalvmNative`: this will generate the `docker-graalvm-native-test` container
3. `sbt docker:publishLocal`: this will generate the `docker-test` continer
4. `time docker run --rm docker-graalvm-native-test`
5. `time docker run --rm docker-test` and compare the timing
6. `docker images | grep docker-` and compare the image size
