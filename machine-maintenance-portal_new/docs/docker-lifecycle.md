# Docker container lifecycle

- **Build:** Maven packages the Spring Boot application JAR, and Docker builds an image containing that JAR and a Java 21 runtime.
- **Tag:** The built image is tagged as `machine-portal:1.0` and `machine-portal:latest` so either name can refer to the same image.
- **Run:** Docker starts MySQL on `portal-net` with its data stored in the persistent `portal-db-data` volume, then starts the portal container on the same network with host port 8085 mapped to container port 8080.
- **Inspect:** `docker ps`, `docker logs`, `docker exec`, `docker stats`, and `docker inspect` show container state, output, process user, resource use, and port mappings.
- **Stop:** `docker stop portal-app` stops the application container without deleting the container or database volume.
- **Restart:** `docker start` or `docker restart portal-app` starts the existing application container again, using the database data retained in `portal-db-data`.
- **Remove:** `docker rm portal-app` removes the stopped application container; the image and database volume remain, and `portal-db` is left running for later work.
