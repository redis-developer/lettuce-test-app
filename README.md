# Lettuce Test App Runner

A workload runner for testing Lettuce client fault tolerance against Redis database upgrades.

## Usage

```sh
java -jar lettuce-test-app.jar --client-mode=cluster --clients=5 --connections-per-client=20 --threads=100 --use-tls=true
```
