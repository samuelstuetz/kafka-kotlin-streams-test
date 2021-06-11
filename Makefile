.PHONY: init-helm remove-helm test test-it clean
run:
		./gradlew bootRun
test: build
		./gradlew test --rerun-tasks
test-it:
		bash interation-test.sh
build:
		./gradlew assemble
publish: build
		./gradlew jib --console plain \
                      -Djib.to.image=samuelst/kafka-boot-example:latest
deploy:
		kubectx docker-desktop
		kubectx apply -f need some files
clean:
		rm -rf build
help:
		@echo "please just read the make file"

# ended up using ccloud essentially free and better cli
init-helm:
		kubectx docker-desktop
		kubens default
		helm repo add bitnami https://charts.bitnami.com/bitnami
		helm repo update
		helm install sam-kafka bitnami/kafka --version 12.19.1
remove-helm:
		kubectx docker-desktop
		kubens default
		helm uninstall sam-kafka
