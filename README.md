# BASE-NODE
BASE-NODE is the implementation of the Node component in BASE platform.

[![Build Status](https://travis-ci.org/bitclave/base-node.svg?branch=develop)](https://travis-ci.org/bitclave/base-node)
[![Coverage Status](https://coveralls.io/repos/github/bitclave/base-node/badge.svg?branch=develop)](https://coveralls.io/github/bitclave/base-node?branch=develop)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

# Lint:
- If you use IntelliJ IDEA, you can call <code>gradlew installKtlintIdeaProject</code> for configure code-style.
- Also you can load config file for codestyle from root dir 'kotlin-codestyle.xml'
- Use for Intellij IDEA (Cherished keys for text formatting):
- ctrl + alt + O is Optimize imports
- ctrl + alt + L is Reformat code

- For Mac Os see:
- [Default key map](https://resources.jetbrains.com/storage/products/intellij-idea/docs/IntelliJIDEA_ReferenceCard.pdf)

- For auto-formatting call <code>gradlew ktlintFormat</code>

# Notes
- BASE-NODE API is available [here](https://base2-bitclva-com.herokuapp.com/swagger-ui.html#)

# Installation

- install PostgreSQL (https://www.postgresql.org/download)
- update password for user "postgres" to "bitclave"
```
sudo -u postgres psql postgres
\password postgres
bitclave
\q
```
- install NodeJS, NPM
- install pgadmin (https://www.pgadmin.org/)

- install Java:
```
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java8-installer
```

- install local blockchain:
```
sudo npm install -g ganache-cli
```

- start your local blockchain
```
./start-ganache.sh 
```


- start your local Postgres instance (Mac)
```
postgres -D .
```

- verify your Postgres instance is running (using pgAdmin for example)

- set base-node configuration to local
```
in file src/main/resources/application-local.yml, set ( spring.profiles.active=local)
or
set enviroment variable PROFILE=local
```

# How to Run

- build project:
```
gradlew build
```

- start local blockchain:
```
./start-ganache
```

- run tests
```
gradlew test
```

- compile and start spring-boot app:
```
gradlew bootRun
```

- check the base-node API
```
http://localhost:8080/swagger-ui.html
```
