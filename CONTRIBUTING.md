# CONTRIBUTING

Pull requests are welcome!  
To see what is currently on the table head to [TODO](TODO.md).  
Before submitting a PR, it is usually better to discuss your intentions either by opening a new issue or in our [slack channel](https://kotlinlang.slack.com/messages/CCGG2R64Q/).

## How to run MySQL tests locally

To run tests agaisnt a live mysql instance we are using [test-containers](https://github.com/testcontainers/testcontainers-java).  
No need to run any special script for a local mysql instance, however, to speed up the tests, you can run the `script/run-docker-mysql.sh` script.
