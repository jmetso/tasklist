# Task List
Task list application.

## How to build

This application is built with maven.

How to build a jar:
```
mvn clean package
```

How to build a jar with OpenShift profile:
```
mvn -P openshift clean package
```

## How to install with Ansible

See [ansible/install.yaml](ansible/install.yaml). The playbook is described in the [README.md](ansible/README.md) file.

## How to uninstall with Ansible

See [ansible/uninstall.yml](ansible/uninstall.yml). The playbook is described in the [README.md](ansible/README.md) file.

## Other stuff

See [release-notes.txt](release-notes.txt) for release notes and [LICENSE](LICENSE) for license.

Uses [Fontawesome](https://fontawesome.com/) and [Patternfly 4](https://www.patternfly.org/v4/).

Built using [Vue.js](https://vuejs.org) and [Spring Boot](https://spring.io/projects/spring-boot).

