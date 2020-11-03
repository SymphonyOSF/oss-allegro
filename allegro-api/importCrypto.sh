#!/usr/bin/env bash

echo run from allegro-api directory.
mvn deploy:deploy-file -DgroupId=com.symphony -DartifactId=crypto -Dversion=1.63.2 -Durl=file:./local-maven-repo -DrepositoryId=local-maven-repo -DupdateReleaseInfo=true -Dfile=../../Security-Lib/crypto/target/crypto-1.63.2.jar
