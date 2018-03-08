# BASE-NODE
BASE-NODE is the implementation of the Node component in BASE platform

[![Build Status](https://travis-ci.org/bitclave/base-node.svg?branch=develop)](https://travis-ci.org/bitclave/base-node)
[![Coverage Status](https://coveralls.io/repos/github/bitclave/base-node/badge.svg)](https://coveralls.io/github/bitclave/base-node)

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