#!/usr/bin/env bash

mvn deploy:deploy-file -DgroupId=com.symphony -DartifactId=crypto -Dversion=1.55.0 -Durl=file:./local-maven-repo -DrepositoryId=local-maven-repo -DupdateReleaseInfo=true -Dfile=../../Security-Lib/crypto/target/crypto-1.55.0.jar
