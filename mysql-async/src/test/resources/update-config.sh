#!/usr/bin/env bash
cp -f /docker-entrypoint-initdb.d/ca.pem /var/lib/mysql/ca.pem
cp -f /docker-entrypoint-initdb.d/server-cert.pem /var/lib/mysql/server-cert.pem
cp -f /docker-entrypoint-initdb.d/server-key.pem /var/lib/mysql/server-key.pem
cp -f /docker-entrypoint-initdb.d/private_key.pem /var/lib/mysql/private_key.pem
cp -f /docker-entrypoint-initdb.d/public_key.pem /var/lib/mysql/public_key.pem
cat /etc/my.cnf || true
cat /etc/mysql/my.cnf || true
ls -lah /var/lib/mysql
ls -lah /etc/mysql/conf.d/ || true
ls -lah /etc/mysql/mysql.conf.d/ || true
