# Dell EMC Stream Analytics - Flink Temperature Events Processor

This repository contains a comprehensive demonstration that simulates what it might look like to build a *Flink* application that generates temperature sensor events and data is being written to pravega stream. The data goes as input to flink application. The flink application process the data and do some transformations. After transformation the out put is written to pravega stream to consume further.This setup is deployed to nautilus cluster


## Components

* Helm Charts  
[*Helm*](https://helm.sh/) is "the package manager for *Kubernetes*", and we will use this tool to install the necessary *Kubernetes* components into our cluster.  

* Flink Temperature Events  
The *Java* code for the [*Flink*](https://flink.apache.org/) jobs that will process events from the *Pravega* stream, which is populated by the data using sensor data generator java classes.

  * Raw Data Processor  
  This *Flink* app forwards the event from the *Pravega* stream and do some transformations and writes to another pravega stream.


## Prerequisites

* A Unix-y local host system

  For the moment, this demo *only* works on Unix-like systems, and has not been built to work on **Windows**.

* A *Kubernetes* cluster with **Dell EMC Stream Analytics** installed.

  The cluster will need approximately 3 `worker` nodes with `2 CPU` + `8 GB RAM`.

* `kubectl`  
  Installation instructions for `kubectl` are available at [kubernetes.io](https://kubernetes.io/docs/tasks/tools/install-kubectl/).
  
  This will need to be properly configured to access your *Kubernetes* cluster.

* Helm v2.10  
  [*Helm*](https://helm.sh/) v2.10 can be obtained from their [github](https://github.com/helm/helm/releases/tag/v2.10.0) page.

* Docker  
  The latest version of [*Docker*](https://www.docker.com/) can be installed using the installer for your local host platform found at [docker.com](https://www.docker.com/)

* Docker Repository  
  A Docker repository you can use to push and pull images (for the IoT Gateway images).
  The repository should be accessible from your *Kubernetes* cluster.
  Save it in `DOCKER_REPOSITORY` environment variable.

* Java 8  
  The latest *Java* 8 is available on [oracle.com](https://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html)

## Getting Started

To start you'll need to clone this repository to your local host.


```shell
$ git clone <repo>
```

You will also need all of the software listed in the prerequisites section above. They will all need to be installed on your local host.

The rest of this guide will take you through all the steps of deploying the demo components slowly and carefully.
We will discuss what the function of each component is and use some tools to interrogate the state of our *Kubernetes* cluster.

## Quick-start
  
### Log In To The **Dell EMC Stream Analytics** UI

Using a web browser configured to use the *Socks5* proxy, log in to the **Dell EMC Stream Analytics** UI.  

[**http://nautilus-ui.nautilus-system.svc.cluster.local/**](http://nautilus-ui.nautilus-system.svc.cluster.local/)

The default credentials are as follows:

> **User Name**: `nautilus`

> **Password**: `password`

For more information about how to proxy into a cluster, please refer to the **Dell EMC Stream Analytics** documentation.  


## Creating An Analytics Project

It is now time to create an *Analytics Project* in **Dell EMC Stream Analytics** where we can run our *Flink* applications.
Navigate to the *Analytics* tab and create a *Project* called `flink-temperature-events`.
Clicking on the *Project* name will take you to the *Project* details page which displays some important information about the `taxidemo` project, including the `Status`.

Once the `Status` field changes from `Deploying` to `Ready` the *Project* is fully deployed.

The `flink-temperature-events` project is now ready to go with all the necessary elements required to interact with the rest of the components in the system, including a dedicated *Kubernetes* namespace, a [*Maven*](https://maven.apache.org/) repository, and a [*Service Account*](https://kubernetes.io/docs/tasks/configure-pod-container/configure-service-account/) for the project.

For more information about *Analytics Projects* and the **Dell EMC Stream Analytics** UI, please refer to the **Dell EMC Stream Analytics** documentation.

It is now possible to see the various components of the *Project* using `kubectl`

```shell
$ kubectl get pods --namespace flink-temperature-events
```

You should see a `repo` pod and a couple of `zookeeper` pods.

### Check The Deployment With `kubectl`

It is now possible to see the various components by listing them with `kubectl`

```shell
$ kubectl get pods --namespace default
```

When the `STATUS` field for all pods is `Running`, everything is up and ready to go.
This will take several minutes.

## Deploying The *Flink* flink-temperature-events

We can now turn our attention to deploying the *Flink* flink-temperature-events applications into the cluster.
Since we're starting from the source code in this repository there are a few steps required to get these *Flink* applications running in the cluster.  

### An Interesting Sample

The *Java* source code itself is in the `flink-temperature-events` directory, and it provides an interesting look at *Flink* application development for **Dell EMC Stream Analytics**. 

For example, the `build.gralde` file provides an example of publishing to the *Analytics Project* *Maven* repository, making use of basic authentication. 
This *Gradle* build also provides a look at building a [shadow jar](https://imperceptiblethoughts.com/shadow/) (AKA `uber jar` or `fat jar`) that can be deployed into *Flink* with all of its dependencies in the right place. 
The *Java* code itself shows how to interact with the *Pravega* client, how to use the [*Pravega Flink Connector*](https://github.com/pravega/flink-connectors), and how to build a simple *Flink* job.

### Building and Deploying The Source Code

The *Flink* application will need to be built into a jar file from the source code in this repository.
It can then be published to the [Maven](https://maven.apache.org/) repository running in the `taxidemo` project we created earlier, using the **Dell EMC Stream Analytics** UI.

To reach the *Project*'s *Maven* repository inside the cluster we'll need a `kubectl port-forward` as described in the **Dell EMC Stream Analytics** documentation on proxy-ing into the cluster.

```shell
$ kubectl port-forward service/repo 9090:80 --namespace flink-temperature-events
```

> **Note:** Leave this `port-forward` running in a terminal

We can now build and publish the *jar* using the [*gradle*](https://gradle.org/) build tool wrapper `gradlew`, also included in this repository.
The *gradle* tool will compile the *Java* code, package it up into a *jar*, and publish the *jar* to the *Maven* repository running in the *Project* on the cluster.

```shell
$ ./gradlew :flink-temperature-events:publish
```

Once the *jar* has been published, we can stop port-forwarding to the *Maven* repository inside `flink-temperature-events` *Project*.

### Viewing The Artifacts

If you're curious, you can now navigate to the **Dell EMC Stream Analytics** UI and view the `examples` *Project* on the *Analytics* tab.
This will show you the *Project* details page which displays some important information about the `flink-temperature-events` project.

One of the tabs on this page is the *Artifacts* tab, which shows a list of all of the artifacts that have been published to the *Maven* repository for this *Project*.
There should now be one artifact in the list, with a 'Maven Group Name' of `io.pravega.example.examples`, and a 'Maven Artifact Name' of `flink-temperature-events`.

This is the *jar* we just published using *gradle*, and it is the artifact we will use to deploy our *Flink* applications.

### Deploying The *Flink* Applications

Now that we have the *jar* artifact published into the *Maven* repository in the `flink-temperature-events` project we created, we can move on to actually deploying and launching the applications themselves.

This demo repository contains a *Helm* chart to make deployment of the *Flink* applications simple.

From the `flink-temperature-events` directory simply run:

```shell
$ helm install --name flink-temperature-events --namespace flink-temperature-events charts/
```

This chart will deploy a *Flink Cluster* into the `flink-temperature-events` project, and will launch our two *Flink* applications.

### Viewing The Flink Cluster

If you now navigate to the *Flink Clusters* tab on the `examples` *Project* details page you will see a new *Flink Cluster*.
It will initially be in the `Not Ready` state and will soon move to the `Ready` state.
Please take note that the *Flink Version* of this cluster is `1.6.2`.

### Viewing The Flink Applications

If you now navigate to the *Apps* tab on the `flink-temperature-events` *Project* details page you will see two new *Flink* applications - one called `flink-temperature-events`.

The two applications should initially be in the `Scheduling` state.
The cluster is looking for a *Flink* cluster to run them in, using various matching criteria, one of which is the *Flink Version*.
You can see the *Flink Version* for both of these applications is `1.7.2`, the same version as the *Flink* cluster that was deployed into this *Project*.

Once the cluster places the applications in the `flink-temperature-events` *Flink* cluster the 'State' should change to `Starting` while the application actually starts up. When it's running the state will change to `Started`.

### Checking With `kubectl`

We can check up on our newly deployed *Flink* application components with `kubectl`

```shell
$ kubectl get pods --namespace flink-temperature-events
```

We can now see a `jobmanager` pod and a `taskmanager` pod deployed into our *Project*. There should also be a pod for each of the applications we deployed, named `flink-temperature-events`.


## Checking Our Work

All of the components of this demo are now running and we should be able to poke around a bit and see everything working.

### **Dell EMC Stream Analytics** Components

Using `kubectl` we can get an idea of all of the components that were installed when we ran the **Dell EMC Stream Analytics** installer:

```shell
$ kubectl get all --namespace nautilus-system
```

and also the *Pravega* components:

```shell
$ kubectl get all --namespace nautilus-pravega
```

We can also dig out the *Socks5* proxy from the `kube-system` namespace:

```shell
$ kubectl get pods --namespace kube-system
```

One pod here will be called something like `socks5-proxy-xxxxxxxxxx-xxxxx` - this is our *Socks5* proxy.

### Elastic-Stack Components

Using `kubectl` we can take a look at the *Elastic-Stack* components that we deployed:

```shell
$ kubectl get all --namespace default
```

### Analytics Project Namespace

We can use `kubectl` to list all the namespaces in the cluster.
One of the namespaces listed here will be the `flink-temperature-events` namespace, which houses our `taxidemoflink-temperature-events` *Analytics Project*.

```shell
$ kubectl get namespaces
```

### Analytics Project Components

Using `kubectl` we can take a look all the components we deployed into our `flink-temperature-events` *Analytics Project*.
These will include:
* The *Maven* repository where we put our *Flink* application artifacts
* A pod for each of our *Flink* applications


```shell
$ kubectl get all --namespace flink-temperature-events
```

There are also [*Custom Resource Definitions*](https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources/) for some of the components we've deployed into the cluster.


### **Dell EMC Stream Analytics** UI