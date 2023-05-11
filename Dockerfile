FROM aigupf/starter-kit:latest

ENV TZ=Europe/Berlin
ENV STAGE=COMPOSE

#################################
# Install JAVA
#################################
RUN apt update && \
    apt upgrade --yes && \
    apt-get install software-properties-common --yes && \
    add-apt-repository ppa:openjdk-r/ppa
RUN apt update && \
    apt-get install openjdk-15-jdk-headless --yes
RUN export JAVA_HOME=/usr/lib/jvm/openjdk-15-jdk && \
    export PATH=\$PATH:\$JAVA_HOME/bin

#################################
# Setup Springboot
#################################
RUN mkdir /data
# paste the jar to the container
COPY ./build/libs/validating-cap-val-0.0.1.jar /data/microservice.jar

# create folder for temporary-pddl-files
RUN mkdir /data/temp

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=${STAGE}", "/data/microservice.jar"]
