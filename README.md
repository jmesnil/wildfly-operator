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
$ mvn clean build
```

# Deploy the operator on Kubernetes

```
$ kubectl apply -f deploy/

namespace/wildfly-operator unchanged
deployment.apps/wildfly-operator configured
clusterrole.rbac.authorization.k8s.io/wildfly-operator unchanged
serviceaccount/wildfly-operator unchanged
clusterrolebinding.rbac.authorization.k8s.io/wildfly-operator unchanged
customresourcedefinition.apiextensions.k8s.io/wildflyservers.wildfly.org created
```
All resources are created in the `wildfly-operator` namespace.

* Check that the Operator is running:

```
$ kubectl --namespace wildfly-operator get pods
NAME                                READY   STATUS    RESTARTS   AGE
wildfly-operator-648dff6c6f-hf87k   1/1     Running   0          18s

$ kubectl --namespace wildfly-operator logs -f wildfly-operator-648dff6c6f-hf87k

__  ____  __  _____   ___  __ ____  ______
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/
2021-01-22 13:44:25,565 INFO  [io.quarkus] (main) wildfly-operator 1.0.0-SNAPSHOT on JVM (powered by Quarkus 1.11.0.Final) started in 1.715s. Listening on: http://0.0.0.0:8080
2021-01-22 13:44:25,569 INFO  [io.quarkus] (main) Profile prod activated.
2021-01-22 13:44:25,570 INFO  [io.quarkus] (main) Installed features: [cdi, kubernetes, kubernetes-client, operator-sdk, smallrye-health]
2021-01-22 13:44:26,644 INFO  [io.jav.ope.Operator] (main) Registered Controller: 'WildFlyServerController_ClientProxy' for CRD: 'class org.wildfly.operator.WildFlyServer' for namespaces: [all/client namespace]
...
```

# Deploy the example

```
$ kubectl apply -f examples/wildflyserver.cr.yaml
wildflyserver.wildfly.org/wildfly-app created
```

You can see in the logs of the WildFly Operator that the resource has been taken into account:

```
...
2021-01-22 13:45:49,190 INFO  [io.jav.ope.pro.EventDispatcher] (EventHandler-org.wildfly.operator.WildFlyServerController_ClientProxy) Adding finalizer to ObjectMeta(annotations={kubectl.kubernetes.
io/last-applied-configuration={"apiVersion":"wildfly.org/v1beta1","kind":"WildFlyServer","metadata":{"annotations":{},"name":"wildfly-app","namespace":"wildfly-operator"},"spec":{"applicationImage":
"myapp","replicas":1}}
}, clusterName=null, creationTimestamp=2021-01-22T13:45:48Z, deletionGracePeriodSeconds=null, deletionTimestamp=null, finalizers=[], generateName=null, generation=1, labels=null, managedFields=[Mana
gedFieldsEntry(apiVersion=wildfly.org/v1beta1, fieldsType=FieldsV1, fieldsV1=FieldsV1(additionalProperties={f:metadata={f:annotations={.={}, f:kubectl.kubernetes.io/last-applied-configuration={}}},
f:spec={.={}, f:applicationImage={}, f:replicas={}}}), manager=kubectl-client-side-apply, operation=Update, time=2021-01-22T13:45:48Z, additionalProperties={})], name=wildfly-app, namespace=wildfly-
operator, ownerReferences=[], resourceVersion=1113, selfLink=null, uid=305af3f1-2b55-4c8f-8b1d-21e68fd7284e, additionalProperties={})
...
```

You can also see the deployed `WildFlyServer` resource:

```
$ kubectl describe wfly/wildfly-example

$ kubectl get wfly/wildfly-example

```
# Clean up the Cluster

```
kubectl delete -f deploy/
kubectl delete -f examples/
```
