
#first: mvn clean package (not jar:jar because full "fat jar" with dependencies is needed) then run this then press ALL the buttons
&"C:\Users\komp\.jdks\graalvm-ce-25.0.2\bin\java" -agentlib:native-image-agent=config-output-dir=./src/main/resources/META-INF/native-image -jar .\target\majic-1.0-jar-with-dependencies.jar