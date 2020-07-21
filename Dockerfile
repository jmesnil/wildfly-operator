FROM scratch

COPY manifests /manifests/
COPY metadata /metadata/

# These are three labels needed to control how the pipeline should handle this container image
# This first label tells the pipeline that this is a bundle image and should be
# delivered via an index image
LABEL com.redhat.delivery.operator.bundle=true

# This second label tells the pipeline which versions of OpenShift the operator supports.
# This is used to control which index images should include this operator.
LABEL com.redhat.openshift.versions="v4.5"

# This third label tells the pipeline that this operator should *also* be supported on OCP 4.4 and
# earlier.  It is used to control whether or not the pipeline should attempt to automatically
# backport this content into the old appregistry format and upload it to the quay.io application
# registry endpoints.
LABEL com.redhat.delivery.backport=true

LABEL operators.operatorframework.io.bundle.mediatype.v1=plain
LABEL operators.operatorframework.io.bundle.manifests.v1=manifests/
LABEL operators.operatorframework.io.bundle.metadata.v1=metadata/
LABEL operators.operatorframework.io.bundle.package.v1=eap
LABEL operators.operatorframework.io.bundle.channels.v1=alpha
LABEL operators.operatorframework.io.bundle.channel.default.v1=alpha

LABEL \
    com.redhat.component="jboss-eap-7-eap73-operator-container" \
    version="1.0" \
    name="operator-1.0-rhel-8-bundle" \
    License="ASL 2.0" \
    io.k8s.display-name="EAP Operator bundle" \
    io.k8s.description="Bundle for the EAP Operator 1.0" \
    summary="Bundle for the EAP Operator 1.0" \
    maintainer="Jeff Mesnil <jmesnil@redhat.com>"