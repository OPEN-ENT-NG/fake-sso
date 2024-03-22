#!/bin/bash

if [ ! -e node_modules ]
then
  mkdir node_modules
fi

MVN_OPTS="-Duser.home=/var/maven"


case `uname -s` in
  MINGW*)
    USER_UID=1000
    GROUP_GID=1000
    ;;
  *)
    if [ -z ${USER_UID:+x} ]
    then
      USER_UID=`id -u`
      GROUP_GID=`id -g`
    fi
esac

init() {
  me=`id -u`:`id -g`
  echo "DEFAULT_DOCKER_USER=$me" > .env
}

test () {
  docker compose run --rm maven mvn $MVN_OPTS test
}

clean () {
  docker compose run --rm maven mvn $MVN_OPTS clean
}

buildNode () {
  docker compose run --rm -u "$USER_UID:$GROUP_GID" node sh -c "npm install && node_modules/gulp/bin/gulp.js build"
}

buildGradle () {
  docker compose run --rm maven mvn $MVN_OPTS install -DskipTests
}

publish () {
  version=`docker compose run --rm maven mvn $MVN_OPTS help:evaluate -Dexpression=project.version -q -DforceStdout`
  level=`echo $version | cut -d'-' -f3`
  case "$level" in
    *SNAPSHOT) export nexusRepository='snapshots' ;;
    *)         export nexusRepository='releases' ;;
  esac

  docker compose run --rm  maven mvn -DrepositoryId=ode-$nexusRepository -DskipTests --settings /var/maven/.m2/settings.xml deploy
}

for param in "$@"
do
  case $param in
    init)
      init
      ;;
    clean)
      clean
      ;;
    buildNode)
      buildNode
      ;;
    buildGradle)
      buildGradle
      ;;
    install)
      buildGradle #buildNode && buildGradle
      ;;
    publish)
      publish
      ;;
    test)
      test
      ;;
    *)
      echo "Invalid argument : $param"
  esac
  if [ ! $? -eq 0 ]; then
    exit 1
  fi
done

