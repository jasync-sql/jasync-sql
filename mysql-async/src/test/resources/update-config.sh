#!/usr/bin/env bash
cp -f /docker-entrypoint-initdb.d/server-key.pem /var/lib/mysql/server-key.pem
cp -f /docker-entrypoint-initdb.d/server-cert.pem /var/lib/mysql/server-cert.pem
cat /etc/mysql/my.cnf
ls -lah /var/lib/mysql
ls -lah /etc/mysql/conf.d/
ls -lah /etc/mysql/mysql.conf.d/
