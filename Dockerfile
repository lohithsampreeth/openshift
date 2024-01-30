FROM bellsoft/liberica-runtime-container:jre-17-stream-musl

WORKDIR /app

COPY target/demo-0.0.1-SNAPSHOT.jar /app/dockerimage.jar

COPY src/main/resources/data.txt /app/data.txt

CMD ["java", "-jar", "dockerimage.jar"]
