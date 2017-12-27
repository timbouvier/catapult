example-docker: example
	#build docker container with example jar
example: install
	cd example && mvn package
install: build
	mvn install:install-file -Dfile=./target/sdk-1.0-SNAPSHOT-jar-with-dependencies.jar -DpomFile=./pom.xml
build:
	mvn package
clean:
	mvn clean && cd example && mvn clean