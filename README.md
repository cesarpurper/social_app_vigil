## How to run it

After cloning the repository we need to build the docker image from sbt build:
```
cd social-app-cesar-vigil
sbt docker:publishLocal
```

After this the docker image will be ready to be run in the local docker daemon
Just go back to root folder and execute docker compose:

```
cd ..
docker-compose up -d
```