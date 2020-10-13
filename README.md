# sample-operator

source from https://github.com/operator-framework/operator-sdk

import to intellij sample-operator/java-operator-sdk/samples/pom.xml

howto run?
- minikube start
- minikube dashboard

$HOME = /Users/meron/Work/minikube/sample-operator

# apply the custom resource definition
- kubectl apply -f $HOME/java-operator-sdk/samples/pure-java/src/main/resources/crd.yaml

# run the operator externally
- mvn exec:java -Dexec.mainClass=com.github.containersolutions.operator.sample.PureJavaApplicationRunner

# invoke a trigger on custom resource
- kubectl apply -f $HOME/java-operator-sdk/samples/pure-java/src/main/resources/example.yaml

# delete custom resource
- kubectl delete -f $HOME/java-operator-sdk/samples/pure-java/src/main/resources/example.yaml

- minikube stop
- minikube delete