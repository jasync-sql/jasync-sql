# CONTRIBUTING

Pull requests are welcome!  
To see what is currently on the table head to [TODO](TODO.md).  
Before submitting a PR, it is usually better to discuss your intentions either by opening a new issue or in our [gitter](https://gitter.im/jasync-sql/support).

## How to run MySQL tests locally

To run tests agaisnt a live mysql instance we are using [test-containers](https://github.com/testcontainers/testcontainers-java).
Docker is required to run the tests.  
No need to run any special script for a local mysql instance, however, to speed up the tests, you can run the `script/run-docker-mysql.sh` script.  
Note that all tables are created with `CREATE TEMPORARY TABLE`. In case you want to debug it might be helpful to remove the `TEMPORARY`.
