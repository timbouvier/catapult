CONTAINER_NAME=catapult-example
CONTAINER_TAG=0.01

container: build
	docker build -t $(CONTAINER_NAME):$(CONTAINER_TAG) container
build:
	mvn package && mv target/*.jar container && cp config/server.yml.template container && cp config/environment container
clean:
	mvn clean && rm -f *~ container/*.jar container/*~