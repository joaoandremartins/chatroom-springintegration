FROM gcr.io/google_appengine/openjdk
# Replace occurrences of YOUR_ARTIFACT_NAME_HERE with the name of the deployed jar
ADD chatroom-springintegration-0.0.1-SNAPSHOT.jar /app/
ENTRYPOINT ["/docker-entrypoint.bash"]
CMD ["java","-jar","/app/chatroom-springintegration-0.0.1-SNAPSHOT.jar"]
