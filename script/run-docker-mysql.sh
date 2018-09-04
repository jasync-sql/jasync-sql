#!/bin/bash
docker run -p 33306:3306 -e MYSQL_ROOT_PASSWORD=test -e MYSQL_USER=mysql_async -e MYSQL_PASSWORD=root -e MYSQL_DATABASE=mysql_async_tests -it --rm mysql:5.7
