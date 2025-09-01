This script sets up port forward pods to support connecting from local dev devices to activities postgres databases in any environment, and their read replicas.

# How it works
This script creates a port forward pod in the target namespace that is unique to each developer, in the format
port-forward-$USER  
If the read-replica is selected, then the pod will be named 
port-forward-$USER-readrep  

Once the script terminates, the pod will be destroyed so there should be no long-running port forward pods. 

# Benefits
Previous scripts wouldn't update existing port forward pods in the namespace which meant the pod might not do what you expect e.g. may be connected to read replica instead of main rds instance  

The pod is cleaned up after usage so resource costs are reduced

Each developer is connecting via their own personal pod, so there's better isolation between developer work environments

The script handles some differences in rds instance name that exist between different environments for you so you never need to hack the script before connecting


# Usage

If used without any parameters this will create a port forward pod to the activities-rds instance in the hmpps-activities-management-dev namespace

To specify the environment pass the following parameters  
-d dev  
-pp preprod  
-p prod  

To select the read replica, pass the -r param  

# Usage examples  
## Create a port forward pod to the dev rds instance
./ activities-port-forward.sh  

## Create a port forward pod to the preprod rds instance 
./activities-port-forward.sh -pp -r   

## Create a port forward pod to the prod rds instance read replica
./activities-port-forward.sh -p  


