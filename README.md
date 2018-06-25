# BASE-NODE
BASE-NODE is the implementation of the Node component in BASE platform

[![Build Status](https://travis-ci.org/bitclave/base-node.svg?branch=develop)](https://travis-ci.org/bitclave/base-node)
[![Coverage Status](https://coveralls.io/repos/github/bitclave/base-node/badge.svg?branch=develop)](https://coveralls.io/github/bitclave/base-node?branch=develop)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

# Notes
- BASE-NODE API is available [here](https://base2-bitclva-com.herokuapp.com/swagger-ui.html#)

# Installation
- install ganache-cli - your local blockchain
```
npm install -g ganache-cli
```

- start your local blockchain
```
./start-ganache.sh 
```

- start your local Postgres instance
```
postgres -D .
```

- check the base-node API
```
http://localhost:8080/swagger-ui.html
```
