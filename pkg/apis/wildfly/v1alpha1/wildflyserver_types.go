package v1alpha1

import (
	corev1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
)

// EDIT THIS FILE!  THIS IS SCAFFOLDING FOR YOU TO OWN!
// NOTE: json tags are required.  Any new fields you add must have json tags for the fields to be serialized.

// WildFlyServerSpec defines the desired state of WildFlyServer
// +k8s:openapi-gen=true
type WildFlyServerSpec struct {
	// ApplicationImage is the name of the application image to be deployed
	ApplicationImage string `json:"applicationImage,omitempty"`
	// ApplicationSource contains the specification to build the image from source code
	ApplicationSource *ApplicationSourceSpec `json:"applicationSource,omitempty"`
	// Replicas is the desired number of replicas for the application
	// +kubebuilder:validation:Minimum=0
	Replicas int32 `json:"replicas"`
	// SessionAffinity defines if connections from the same client ip are passed to the same WildFlyServer instance/pod each time (false if omitted)
	SessionAffinity bool `json:"sessionAffinity,omitempty"`
	// DisableHTTPRoute disables the creation a route to the HTTP port of the application service (false if omitted)
	DisableHTTPRoute    bool                     `json:"disableHTTPRoute,omitempty"`
	StandaloneConfigMap *StandaloneConfigMapSpec `json:"standaloneConfigMap,omitempty"`
	// StorageSpec defines specific storage required for the server own data directory. If omitted, an EmptyDir is used (that will not
	// persist data across pod restart).
	Storage            *StorageSpec `json:"storage,omitempty"`
	ServiceAccountName string       `json:"serviceAccountName,omitempty"`
	// EnvFrom contains environment variables from a source such as a ConfigMap or a Secret
	// +kubebuilder:validation:MinItems=1
	// +listType=set
	EnvFrom []corev1.EnvFromSource `json:"envFrom,omitempty,list_type=corev1.EnvFromSource"`
	// Env contains environment variables for the containers running the WildFlyServer application
	// +kubebuilder:validation:MinItems=1
	// +listType=set
	Env []corev1.EnvVar `json:"env,omitempty"`
	// Secrets is a list of Secrets in the same namespace as the WildFlyServer
	// object, which shall be mounted into the WildFlyServer Pods.
	// The Secrets are mounted into /etc/secrets/<secret-name>.
	// +kubebuilder:validation:MinItems=1
	// +listType=set
	Secrets []string `json:"secrets,omitempty"`
	// ConfigMaps is a list of ConfigMaps in the same namespace as the WildFlyServer
	// object, which shall be mounted into the WildFlyServer Pods.
	// The ConfigMaps are mounted into /etc/configmaps/<configmap-name>.
	// +kubebuilder:validation:MinItems=1
	// +listType=set
	ConfigMaps []string `json:"configMaps,omitempty"`
}

// ApplicationSourceSpec defines the specification to build the image from source code
// +k8s:openapi-gen=true
type ApplicationSourceSpec struct {
	SourceRepository *SourceRepositorySpec `json:"sourceRepository"`
	Source2Image     *Source2ImageSpec     `json:"source2Image"`
}

// SourceRepositorySpec defines the Git repository of the application source code
// +k8s:openapi-gen=true
type SourceRepositorySpec struct {
	// URL of the Git repository
	URL string `json:"url"`
	// Reference in the Git repository (can be a branch, a tag or a SHA-1 checksum)
	Ref string `json:"ref,omitempty"`
	// Sub-directory where the source code for the application exists
	ContextDir string `json:"contextDir,omitempty"`
	// Secret for GitHub WebHook. This references a Secret in the same namespace which has a key named WebHookSecretKey whose value is supplied when invoking the webhook. If omitted, a secret will be automatically generated.
	GitHubWebHookSecret string `json:"gitHubWebHookSecret,omitempty"`
	// Secret for Generic WebHook. This references a Secret in the same namespace which has a key named WebHookSecretKey whose value is supplied when invoking the webhook. If omitted, a secret will be automatically generated.
	GenericWebHookSecret string `json:"genericWebHookSecret,omitempty"`
}

// Source2ImageSpec defines which S2I builder and runtime images to use to build the application image
// +k8s:openapi-gen=true
type Source2ImageSpec struct {
	// Image Stream Tag of the builder image
	BuilderImage string `json:"builderImage"`
	// Image Stream Tag of the runtime image. If omitted, the application image will be built directly from the builder image.
	RuntimeImage string `json:"runtimeImage,omitempty"`
	// Namespace where the builder (and potentially runtime) images streams are defined. If omitted, the "openshift" namespace is used
	Namespace string `json:"namespace,omitempty"`
	// Env contains environment variables for the containers building the application image from the SourceRepository
	// +kubebuilder:validation:MinItems=1
	// +listType=set
	Env []corev1.EnvVar `json:"env,omitempty"`
}

// StandaloneConfigMapSpec defines the desired configMap configuration to obtain the standalone configuration for WildFlyServer
// +k8s:openapi-gen=true
type StandaloneConfigMapSpec struct {
	Name string `json:"name"`
	// Key of the config map whose value is the standalone XML configuration file ("standalone.xml" if omitted)
	Key string `json:"key,omitempty"`
}

// StorageSpec defines the desired storage for WildFlyServer
// +k8s:openapi-gen=true
type StorageSpec struct {
	EmptyDir *corev1.EmptyDirVolumeSource `json:"emptyDir,omitempty"`
	// VolumeClaimTemplate defines the template to store WildFlyServer standalone data directory.
	// The name of the template is derived from the WildFlyServer name.
	//  The corresponding volume will be mounted in ReadWriteOnce access mode.
	// This template should be used to specify specific Resources requirements in the template spec.
	VolumeClaimTemplate corev1.PersistentVolumeClaim `json:"volumeClaimTemplate,omitempty"`
}

// WildFlyServerStatus defines the observed state of WildFlyServer
// +k8s:openapi-gen=true
type WildFlyServerStatus struct {
	// Replicas is the actual number of replicas for the application
	Replicas int32 `json:"replicas"`
	// +listType=set
	Pods []PodStatus `json:"pods,omitempty"`
	// +listType=set
	Hosts []string `json:"hosts,omitempty"`
	// Represents the number of pods which are in scaledown process
	// what particular pod is scaling down can be verified by PodStatus
	//
	// Read-only.
	ScalingdownPods int32 `json:"scalingdownPods"`
}

const (
	// PodStateActive represents PodStatus.State when pod is active to serve requests
	// it's connected in the Service load balancer
	PodStateActive = "ACTIVE"
	// PodStateScalingDownRecoveryInvestigation represents the PodStatus.State when pod is in state of scaling down
	// and is to be verified if it's dirty and if recovery is needed
	// as the pod is under recovery verification it can't be immediatelly removed
	// and it needs to be wait until it's marked as clean to be removed
	PodStateScalingDownRecoveryInvestigation = "SCALING_DOWN_RECOVERY_INVESTIGATION"
	// PodStateScalingDownRecoveryDirty represents the PodStatus.State when the pod was marked as recovery is needed
	// because there are some in-doubt transactions.
	// The app server was restarted with the recovery properties to speed-up recovery nad it's needed to wait
	// until all ind-doubt transactions are processed.
	PodStateScalingDownRecoveryDirty = "SCALING_DOWN_RECOVERY_DIRTY"
	// PodStateScalingDownClean represents the PodStatus.State when pod is not active to serve requests
	// it's in state of scaling down and it's clean
	// 'clean' means it's ready to be removed from the kubernetes cluster
	PodStateScalingDownClean = "SCALING_DOWN_CLEAN"
)

// PodStatus defines the observed state of pods running the WildFlyServer application
// +k8s:openapi-gen=true
type PodStatus struct {
	Name  string `json:"name"`
	PodIP string `json:"podIP"`
	// Represent the state of the Pod, it is used especially during scale down.
	// +kubebuilder:validation:Enum=ACTIVE;SCALING_DOWN_RECOVERY_INVESTIGATION;SCALING_DOWN_RECOVERY_DIRTY;SCALING_DOWN_CLEAN
	State string `json:"state"`
}

// WildFlyServer is the Schema for the wildflyservers API
// +k8s:openapi-gen=true
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
// +kubebuilder:subresource:status
// +kubebuilder:subresource:scale:specpath=.spec.replicas,statuspath=.status.replicas
// +kubebuilder:printcolumn:name="Replicas",type="integer",JSONPath=".spec.replicas"
// +kubebuilder:printcolumn:name="Age",type="date",JSONPath=".metadata.creationTimestamp"
// +kubebuilder:resource:shortName=wfly
type WildFlyServer struct {
	metav1.TypeMeta   `json:",inline"`
	metav1.ObjectMeta `json:"metadata,omitempty"`

	Spec   WildFlyServerSpec   `json:"spec,omitempty"`
	Status WildFlyServerStatus `json:"status,omitempty"`
}

// WildFlyServerList contains a list of WildFlyServer
// +k8s:deepcopy-gen:interfaces=k8s.io/apimachinery/pkg/runtime.Object
type WildFlyServerList struct {
	metav1.TypeMeta `json:",inline"`
	metav1.ListMeta `json:"metadata,omitempty"`
	Items           []WildFlyServer `json:"items"`
}

func init() {
	SchemeBuilder.Register(&WildFlyServer{}, &WildFlyServerList{})
}
