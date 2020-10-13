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
helm install hello xap-manager --set service.type="NodePort",service.api.nodePort=30890
# pu - just to check everything is working
helm install world xap-pu --set manager.name=hello
helm del world --keep-history


# home dir
$HOME = /Users/meron/Work/minikube/sample-operator

# apply the custom resource definition
- kubectl apply -f $HOME/java-operator-sdk/samples/pure-java/src/main/resources/crd.yaml

# run the operator externally
- mvn exec:java -Dexec.mainClass=com.github.containersolutions.operator.sample.PureJavaApplicationRunner

# invoke a trigger on custom resource
- kubectl apply -f $HOME/java-operator-sdk/samples/pure-java/src/main/resources/example.yaml

# delete custom resource
- kubectl delete -f $HOME/java-operator-sdk/samples/pure-java/src/main/resources/example.yaml
