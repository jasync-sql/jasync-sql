#!/usr/bin/env sh

echo "Preparing MySQL configs"
mysql -u root -e 'create database mysql_async_tests;'
mysql -u root -e "GRANT ALL PRIVILEGES ON *.* TO ‘mysql_async’@'localhost’ IDENTIFIED BY ‘root’ WITH GRANT OPTION";

echo "preparing postgresql configs"

psql -c 'create database netty_driver_test;' -U postgres
psql -c "CREATE USER postgres_md5 WITH PASSWORD 'postgres_md5'; GRANT ALL PRIVILEGES ON DATABASE netty_driver_test to postgres_md5;" -U postgres
psql -c "CREATE USER postgres_cleartext WITH PASSWORD 'postgres_cleartext'; GRANT ALL PRIVILEGES ON DATABASE netty_driver_test to postgres_cleartext;" -U postgres
psql -c "CREATE USER postgres_kerberos WITH PASSWORD 'postgres_kerberos'; GRANT ALL PRIVILEGES ON DATABASE netty_driver_test to postgres_kerberos;" -U postgres

sudo chmod 777 /etc/postgresql/9.1/main/pg_hba.conf
sudo echo "host     all             postgres_md5    127.0.0.1/32            md5" > /etc/postgresql/9.1/main/pg_hba.conf
sudo echo "host     all             postgres_cleartext 127.0.0.1/32         password" > /etc/postgresql/9.1/main/pg_hba.conf
sudo echo "host     all             postgres_kerberos 127.0.0.1/32         krb5" > /etc/postgresql/9.1/main/pg_hba.conf

cat "/etc/postgresql/9.1/main/pg_hba.conf"