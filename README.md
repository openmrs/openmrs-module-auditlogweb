[![CI Build Status](https://github.com/openmrs/openmrs-module-webservices.rest/actions/workflows/maven.yml/badge.svg)](https://github.com/nsalifu/openmrs-module-auditlogweb/blob/unit-tests/.github/workflows/maven.yml)

<img src="https://talk.openmrs.org/uploads/default/original/2X/f/f1ec579b0398cb04c80a54c56da219b2440fe249.jpg" alt="OpenMRS"/>

# OpenMRS Audit Log Web Module

> Audit Log Web Module for [OpenMRS](https://openmrs.atlassian.net/wiki/spaces/projects/pages/363757631/Improved+Audit+Logging) Wiki Page
>
> 
> Audit Log Web Module for [OpenMRS](https://openmrs.atlassian.net/jira/software/c/projects/AUDIT/summary)  JIRA Board

Description
-----------
This module provides enhanced audit logging capabilities for OpenMRS, allowing administrators and developers to track, view, and analyze changes made to data within the system through a user-friendly web interface.

# Building from Source
--------------------
Pre-requisites:
1. Java 1.8+ 
2. Maven 2.x+
3. OpenMRS SDK (optional, but recommended for easier module management)
4. OpenMRS instance running (for testing purposes)
5. Git (to clone the repository)
6. An IDE (like IntelliJ IDEA or Eclipse) for easier development and debugging

To build the module from source, clone this repo:
```
git clone https://github.com/openmrs/openmrs-module-auditlogweb
```
Navigate into the `openmrs-module-auditlogweb` directory and compile the module using Maven:
```
cd openmrs-module-auditlogweb && mvn clean package
```

Installation
------------
1. Build the module to produce the .omod file.
2. Use the OpenMRS Administration > Manage Modules screen to upload and install the .omod file.

Alternative Installation Method: 
If OpenMRS SDk is installed, you can use the following command to install the module:
```
mvn openmrs-sdk:deploy -DserverId={serverName} -DmoduleVersion=1.0.0-SNAPSHOT
```
As a developer, you can also use the OpenMRS SDK to watch the module by your running OpenMRS instance.
```
mvn openmrs-sdk:watch -DserverId={serverName} -DmoduleVersion=1.0.0-SNAPSHOT
```
This will automatically deploy changes to the module without needing to manually upload the .omod file each time.

If uploads are not allowed from the web (changable via a runtime property), you can drop the omod
into the ~/.OpenMRS/modules folder.  (Where ~/.OpenMRS is assumed to be the Application 
Data Directory that the running openmrs is currently using.)  After putting the file in there 
simply restart OpenMRS/tomcat and the module will be loaded and started.
