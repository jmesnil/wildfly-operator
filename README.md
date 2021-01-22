# WildFly Operator

# Prerequisites

## Docker registry authentication

To be able to push the Docker image to a Docker registry, you need to add the server authentication credentials to 
your `~.m2/settings.xml`:

```
    <server>
      <id>quay.io</id>
      <username>my_user_name_on_quay.io</username>
      <password>my_password_on_quay.io</password>
    </server>
```

# Build the operator

```
mvn -P no-integration-tests clean package deploy
```

# Deploy the operator on Kubernetes

```
kubectl apply -f deploy/*.yaml
```

# Deploy the example

```
kubectl apply -f examples/wildflyserver.cr.yaml
```