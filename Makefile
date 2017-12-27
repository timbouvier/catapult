example-docker: install
	cd example && make container
install: build
	mvn install:install-file -Dfile=./target/sdk-1.0-SNAPSHOT-jar-with-dependencies.jar -DpomFile=./pom.xml
build:
	mvn package
clean:
	mvn clean && cd example && make clean