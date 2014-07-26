#!/usr/bin/env bash
yum -y install mysql-server
service mysqld start
mysql -u root -e "GRANT ALL PRIVILEGES ON *.* TO root;"
mysql -u root -e "GRANT ALL PRIVILEGES ON *.* TO 'mysql_vagrant' IDENTIFIED BY 'generic_password' WITH GRANT OPTION";