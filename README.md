# golang-jenkin


#### Requirements

To run this application on your machine, you need at least:

- jenkins-lts
- minikube
- docker

#### Plugin Jenkin
- Kubernetes CLI Plugin
- Kubernetes Plugin
- Pipeline: Stage Step

Run jenkin:
---------------------
You can start the jenkin:
```bash
brew services start jenkins-lts
```

Open Jenkin
```bash
http://localhost:8080
```

Get password jenkin
---------------------
```bash
sudo cat /Users/$(whoami)/.jenkins/secrets/initialAdminPassword
```

Run k8s:
---------------------
You can start the jenkin:
```bash
minikube start
```

Create namespace
```bash
kubectl create namespace minikube-local
```

Check PORT
```bash
minikube service golang-jenkin -n minikube-local
```