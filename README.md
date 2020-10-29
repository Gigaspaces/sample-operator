# sample-operator

source from https://github.com/operator-framework/operator-sdk

import to intellij sample-operator/java-operator-sdk/samples/pom.xml

howto run?
# minikube
- minikube start
- minikube dashboard
- minikube stop
- minikube delete

# manager 
- helm install hello xap-manager --set service.type="NodePort",service.api.nodePort=30890

# Connect to ops-ui
- minikube service --url hello-xap-manager-service

# pu - just to check everything is working
- helm install world xap-pu --set manager.name=hello
- helm del world --keep-history


# home dir
$HOME = /Users/meron/Work/minikube/sample-operator

# apply the custom resource definition
- kubectl apply -f $HOME/gs-operator/src/main/resources/pu-crd.yaml

# create cluster Role Binding
- kubectl create clusterrolebinding default --clusterrole cluster-admin --serviceaccount=default:default


# run the operator externally
- mvn exec:java -Dexec.mainClass=com.gigaspaces.k8s.operators.DevOperator

# invoke a trigger on custom resource
- kubectl apply -f $HOME/gs-operator/examples/pu.yaml

# delete custom resource
- kubectl delete -f $HOME/gs-operator/examples/pu.yaml
