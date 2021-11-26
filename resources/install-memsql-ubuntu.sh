#!/bin/bash
wget http://download.memsql.com/memsql-ops-4.0.35/memsql-ops-4.0.35.tar.gz
tar -xzf memsql-ops-4.0.35.tar.gz
cd memsql-ops-4.0.35
sudo ./install.sh

echo memsql is ready!
