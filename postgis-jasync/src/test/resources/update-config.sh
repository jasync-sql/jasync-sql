#!/usr/bin/env bash
cp -f /docker-entrypoint-initdb.d/pg_hba.conf /var/lib/postgresql/data/
cp -f /docker-entrypoint-initdb.d/server.crt /var/lib/postgresql/data/
cp -f /docker-entrypoint-initdb.d/server.key /var/lib/postgresql/data/
sed -i'' 's/#ssl = off/ssl = on/' /var/lib/postgresql/data/postgresql.conf
sed -i'' 's/#password_encryption = md5/password_encryption = scram-sha-256/' /var/lib/postgresql/data/postgresql.conf
cat /var/lib/postgresql/data/postgresql.conf
chown postgres:postgres /var/lib/postgresql/data/pg_hba.conf
chown postgres:postgres /var/lib/postgresql/data/server.crt
chown postgres:postgres /var/lib/postgresql/data/server.key
chmod 0600 /var/lib/postgresql/data/server.key
