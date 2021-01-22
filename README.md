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
$ mvn clean install
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

* Check that the Operator is created and wait until it is running and ready:

```
$ kubectl --namespace wildfly-operator get -w pods
NAME                                READY   STATUS              RESTARTS   AGE
wildfly-operator-648dff6c6f-s6bvp   0/1     ContainerCreating   0          19s
wildfly-operator-648dff6c6f-s6bvp   0/1     Running             0          104s
wildfly-operator-648dff6c6f-s6bvp   1/1     Running             0          113s

$ kubectl --namespace wildfly-operator logs -f wildfly-operator-648dff6c6f-s6bvp

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
$ kubectl apply -f examples/wildfly-app.yaml
wildflyserver.wildfly.org/wildfly-app created
```

You can see in the logs of the WildFly Operator that the resource has been taken into account:

```
...
2021-01-22 15:10:19,169 INFO  [io.jav.ope.pro.EventDispatcher] (EventHandler-org.wildfly.operator.WildFlyServerController_ClientProxy) Adding finalizer to ObjectMeta(annotations={kubectl.kubernetes.io/last-applied-configuration={"apiVersion":"wildfly.org/v1beta1","kind":"WildFlyServer","metadata":{"annotations":{},"name":"wildfly-app","namespace":"wildfly-operator"},"spec":{"applicationImage":"myapp","replicas":1}}
}, clusterName=null, creationTimestamp=2021-01-22T15:10:18Z, deletionGracePeriodSeconds=null, deletionTimestamp=null, finalizers=[], generateName=null, generation=1, labels=null, managedFields=[ManagedFieldsEntry(apiVersion=wildfly.org/v1beta1, fieldsType=FieldsV1, fieldsV1=FieldsV1(additionalProperties={f:metadata={f:annotations={.={}, f:kubectl.kubernetes.io/last-applied-configuration={}}}, f:spec={.={}, f:applicationImage={}, f:replicas={}}}), manager=kubectl-client-side-apply, operation=Update, time=2021-01-22T15:10:18Z, additionalProperties={})], name=wildfly-app, namespace=wildfly-operator, ownerReferences=[], resourceVersion=659, selfLink=null, uid=e7f37ec8-b461-492a-98a7-c7632611bd9c, additionalProperties={})
2021-01-22 15:10:19,217 WARN  [io.fab.kub.cli.int.VersionUsageUtils] (EventHandler-org.wildfly.operator.WildFlyServerController_ClientProxy) The client is using resource type 'wildflyservers' with unstable version 'v1beta1'
...
```

You can also see the deployed `WildFlyServer` resource:

```
$ kubectl get --namespace wildfly-operator wfly wildfly-app
NAME          REPLICAS   AGE
wildfly-app   1          85s
$ kubectl get --namespace wildfly-operator wfly wildfly-app
Name:         wildfly-app
Namespace:    wildfly-operator
API Version:  wildfly.org/v1beta1
Kind:         WildFlyServer
Metadata:
  ...
Spec:
  Application Image:  myapp
  Replicas:           1
Status:
  Replicas:  1
Events:      <none>
```

To see the application being deployed, you can watch the pods:

```
$ kubectl --namespace wildfly-operator get -w pods
NAME                                READY   STATUS              RESTARTS   AGE
wildfly-app-0                       0/1     ContainerCreating   0          2m46s
wildfly-operator-648dff6c6f-wgwxw   1/1     Running             0          3m19s
wildfly-app-0                       1/1     Running             0          3m30s
```

and look at the logs of the WildFly application with `$kubectl --namespace wildfly-operator logs -f wildfly-app-0`

Finally, you can query the application:

```
$ curl $(minikube service wildfly-app-loadbalancer --url)
```

# Clean up the Cluster

You can delete the application example with:

```
kubectl delete -f examples/
```

Then the Operator can be deleted with:

```
kubectl delete -f deploy/
```
