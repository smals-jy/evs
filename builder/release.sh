#!/usr/bin/env bash  
  
if ! sh ../release/1_check_version_presence.sh; then exit $?; fi  
if ! sh ../release/2_apply_version_to_poms.sh; then exit $?; fi  
# all individual modules, INCLUDING the builder project must first be built  
if ! mvn clean package; then exit $?; fi  
# only then the EVS installation can be packaged  
if ! mvn install -B -f ././../distribution/pom.xml; then exit $?; fi
