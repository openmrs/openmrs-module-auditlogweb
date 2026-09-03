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

Running with Docker
-------------------
This repo ships a docker compose setup that runs the full O3 Reference Application
(gateway, frontend with the audit-log app, backend with this module, MariaDB) with
Hibernate Envers enabled. The backend image sets `hibernate.hbm2ddl.auto=update`
(the configuration the module's admin hint prescribes) so the Envers schema is
created before the first audited write and kept in step with the audited
mappings on every boot.

The images are built from this repo (`Dockerfile.backend`, `Dockerfile.frontend`)
and published to Docker Hub by the `build-docker.yml` workflow — the same setup
as openmrs-module-chartsearchai. Pushes to `main` and a nightly schedule publish
`openmrs/openmrs-reference-application-3-{backend,frontend}:nightly-auditlog`;
release tags publish `<version>-auditlog` (e.g. `1.1.0-auditlog`), which pins
the module version — the O3 base images, ESMs, and gateway still track the
RefApp's moving `nightly`/`next` tags, so treat this stack as an
evaluation/demo deployment rather than a fully pinned production install.
The dev compose override builds the images locally under separate tags, so it
works before any image is published.

Prerequisites: Docker with the compose plugin.

**Run the published images (demo/evaluation):**
```
cp .env.example .env       # then edit .env and set OMRS_DB_PASSWORD and MYSQL_ROOT_PASSWORD
docker compose up -d
docker compose logs -f backend   # first boot can take 30+ minutes
```
Open http://localhost (admin/Admin123; change the port with `GATEWAY_PORT` in `.env`).

**Development** (mounts your locally built omod over the bundled one, default passwords from `.env.dev`):
```
mvn clean package
docker compose --env-file .env.dev -f docker-compose.yml -f docker-compose.dev.yml up -d
```
After each module rebuild, restart the backend so the file mount picks up the
new build (`up -d` alone won't restart an unchanged container):
```
mvn clean package
docker compose --env-file .env.dev -f docker-compose.yml -f docker-compose.dev.yml up -d --force-recreate backend
```
Pass `--build` when the Dockerfiles change — compose only builds automatically
when the image doesn't exist yet. (The frontend's module list is fetched from
the RefApp distro at build time, with the audit-log app added.) A compose
error about the omod bind-mount path means `MODULE_VERSION` in `.env.dev` has
drifted from the pom version.

Every compose command in this flow needs `--env-file .env.dev` as well, the
Verify and Stop / reset blocks below included; without it compose cannot
interpolate the passwords the base file requires and exits before reaching
Docker.

**Verify the audit schema** (uses the credentials from the container's own env):
```
docker compose exec db sh -c 'mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e "SHOW TABLES LIKE \"%_AUD\"; SHOW TABLES LIKE \"revision_entity\";"'
```
If tables are missing, check the backend log for `SchemaUpdate` errors — schema
failures are logged, not fatal to startup. (On platforms built from core
master/2.9+ the default audit suffix changes from `_AUD` to `_audit`.)

Note the backend log always carries ERROR lines from the module's audit-column
sweep: with `hbm2ddl.auto=update`, most `_AUD` tables differ from their base
tables in column type/width (SchemaUpdate builds them from the entity mappings
while the base tables come from Liquibase), and the sweep logs each divergence
on every boot. These errors are expected with this configuration and do not
block auditing.

**Stop / reset:**
```
docker compose down       # stop; data survives in the volumes
docker compose down -v    # wipe everything for a fresh start
```
