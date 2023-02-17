FROM openjdk:8-jre-alpine
WORKDIR /opt/docker
ADD --chown=daemon:daemon opt /opt
EXPOSE 8080
USER daemon
ENTRYPOINT ["/opt/docker/bin/social-app-cesar-vigil"]
CMD []
