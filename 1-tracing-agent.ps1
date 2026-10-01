
#first: mvn clean package (not jar:jar because full "fat jar" with dependencies is needed) then:
&"C:\Users\komp\.jdks\graalvm-ce-25.0.2\bin\java" -agentlib:native-image-agent -jar .\target\majic-1.0-jar-with-dependencies.jar