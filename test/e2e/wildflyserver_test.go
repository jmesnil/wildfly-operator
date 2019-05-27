// +build !unit

package e2e

import (
	goctx "context"
	"fmt"
	"io/ioutil"
	"testing"
	"time"

	rbac "k8s.io/api/rbac/v1"

	framework "github.com/operator-framework/operator-sdk/pkg/test"
	"github.com/operator-framework/operator-sdk/pkg/test/e2eutil"
	"github.com/wildfly/wildfly-operator/pkg/apis"
	wildflyv1alpha1 "github.com/wildfly/wildfly-operator/pkg/apis/wildfly/v1alpha1"
	wildflyframework "github.com/wildfly/wildfly-operator/test/framework"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/types"
)

var (
	retryInterval        = time.Second * 5
	timeout              = time.Minute * 3
	cleanupRetryInterval = time.Second * 1
	cleanupTimeout       = time.Second * 5
)

func TestWildFlyServer(t *testing.T) {
	wildflyServerList := &wildflyv1alpha1.WildFlyServerList{
		TypeMeta: metav1.TypeMeta{
			Kind:       "WildFlyServer",
			APIVersion: "wildfly.org/v1alpha1",
		},
	}
	err := framework.AddToFrameworkScheme(apis.AddToScheme, wildflyServerList)
	if err != nil {
		t.Fatalf("failed to add custom resource scheme to framework: %v", err)
	}
	// run subtests
	t.Run("WildFlyServer", func(t *testing.T) {
		t.Run("WildFly17", func(t *testing.T) {
			t.Run("BasicTest", WildFly17BasicTest)
		})
		t.Run("WildFly16", func(t *testing.T) {
			//t.Run("BasicTest", WildFly16BasicTest)
			//t.Run("ClusterTest", WildFlyClusterTest)
		})
	})
}

func wildflyBasicServerScaleTest(t *testing.T, f *framework.Framework, ctx *framework.TestCtx, applicationTag string) error {
	namespace, err := ctx.GetNamespace()
	if err != nil {
		return fmt.Errorf("could not get namespace: %v", err)
	}

	name := "example-wildfly"
	// create wildflyserver custom resource
	wildflyServer := wildflyframework.MakeBasicWildFlyServer(namespace, name, "quay.io/jmesnil/wildfly-operator-quickstart:"+applicationTag, 1)
	err = wildflyframework.CreateAndWaitUntilReady(f, ctx, t, wildflyServer)
	if err != nil {
		return err
	}

	fmt.Printf("Application %s is deployed with %d instance\n", name, 1)

	context := goctx.TODO()

	// update the size to 2
	err = f.Client.Get(context, types.NamespacedName{Name: name, Namespace: namespace}, wildflyServer)
	if err != nil {
		return err
	}
	wildflyServer.Spec.Size = 2
	err = f.Client.Update(context, wildflyServer)
	if err != nil {
		return err
	}
	fmt.Printf("Updated aplication %s size to %d\n", name, wildflyServer.Spec.Size)

	// check that the resource have been updated
	return wildflyframework.WaitUntilReady(f, t, wildflyServer)
}

func wildflyClusterViewTest(t *testing.T, f *framework.Framework, ctx *framework.TestCtx) error {
	namespace, err := ctx.GetNamespace()
	if err != nil {
		return fmt.Errorf("could not get namespace: %v", err)
	}

	name := "clusterbench"
	standaloneConfigXML, err := ioutil.ReadFile("test/e2e/standalone-clustering-test.xml")
	if err != nil {
		return err
	}

	// create RBAC so that JGroups can view the k8s cluster
	roleBinding := &rbac.RoleBinding{
		TypeMeta: metav1.TypeMeta{
			Kind:       "RoleBinding",
			APIVersion: "rbac.authorization.k8s.io",
		},
		ObjectMeta: metav1.ObjectMeta{
			Name:      name,
			Namespace: namespace,
		},
		Subjects: []rbac.Subject{{
			Kind: "ServiceAccount",
			Name: "default",
		}},
		RoleRef: rbac.RoleRef{
			Kind:     "ClusterRole",
			Name:     "view",
			APIGroup: "rbac.authorization.k8s.io",
		},
	}

	err = f.Client.Create(goctx.TODO(), roleBinding, &framework.CleanupOptions{TestContext: ctx, Timeout: cleanupTimeout, RetryInterval: cleanupRetryInterval})
	if err != nil {
		return err
	}

	// create config map for the standalone config
	wildflyframework.CreateStandaloneConfigMap(f, ctx, namespace, "clusterbench-configmap", "standalone-openshift.xml", standaloneConfigXML)
	// create wildflyserver custom resource
	wildflyServer := wildflyframework.MakeBasicWildFlyServer(namespace, name, "quay.io/jmesnil/clusterbench-ee7:16.0", 2)
	wildflyServer.Spec.StandaloneConfigMap = &wildflyv1alpha1.StandaloneConfigMapSpec{
		Name: "clusterbench-configmap",
		Key:  "standalone-openshift.xml",
	}

	err = wildflyframework.CreateAndWaitUntilReady(f, ctx, t, wildflyServer)
	if err != nil {
		return err
	}

	err = wildflyframework.WaitUntilReady(f, t, wildflyServer)
	if err != nil {
		return err
	}

	return wildflyframework.WaitUntilClusterIsFormed(f, t, wildflyServer, "clusterbench-0", "clusterbench-1")
}

func WildFly16BasicTest(t *testing.T) {
	WildFlyBasicTest(t, "16.0")
}

func WildFly17BasicTest(t *testing.T) {
	WildFlyBasicTest(t, "17.0")
}

func WildFlyBasicTest(t *testing.T, applicationTag string) {
	ctx := framework.NewTestCtx(t)
	defer ctx.Cleanup()
	err := ctx.InitializeClusterResources(&framework.CleanupOptions{TestContext: ctx, Timeout: cleanupTimeout, RetryInterval: cleanupRetryInterval})
	if err != nil {
		t.Fatalf("failed to initialize cluster resources: %v", err)
	}
	t.Log("Initialized cluster resources")
	namespace, err := ctx.GetNamespace()
	fmt.Printf("Initialized cluster resources\n")
	if err != nil {
		t.Fatal(err)
	}
	// get global framework variables
	f := framework.Global
	// wait for wildfly-operator to be ready
	err = e2eutil.WaitForDeployment(t, f.KubeClient, namespace, "wildfly-operator", 1, retryInterval, timeout)
	if err != nil {
		t.Fatal(err)
	}
	fmt.Printf("Operator is deployed\n")

	if err = wildflyBasicServerScaleTest(t, f, ctx, applicationTag); err != nil {
		t.Fatal(err)
	}
}

func WildFlyClusterTest(t *testing.T) {
	ctx := framework.NewTestCtx(t)
	defer ctx.Cleanup()
	err := ctx.InitializeClusterResources(&framework.CleanupOptions{TestContext: ctx, Timeout: cleanupTimeout, RetryInterval: cleanupRetryInterval})
	if err != nil {
		t.Fatalf("failed to initialize cluster resources: %v", err)
	}
	t.Log("Initialized cluster resources")
	namespace, err := ctx.GetNamespace()
	if err != nil {
		t.Fatal(err)
	}
	// get global framework variables
	f := framework.Global
	// wait for wildfly-operator to be ready
	err = e2eutil.WaitForDeployment(t, f.KubeClient, namespace, "wildfly-operator", 1, retryInterval, timeout)
	if err != nil {
		t.Fatal(err)
	}

	if err = wildflyClusterViewTest(t, f, ctx); err != nil {
		t.Fatal(err)
	}
}
