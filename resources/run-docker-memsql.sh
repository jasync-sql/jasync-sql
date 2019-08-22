#!/bin/bash
docker run -d -p 4306:3306 -p 9000:9000 --name=memsql memsql/quickstart
