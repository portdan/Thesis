# Thesis
General Repository

#Installation guide
1. install eclipse
	1.1. install EGit plugin
	1.2. install maven plugin

2. clone repository (https://github.com/portdan/Thesis.git) to local folder
	2.1 import all projects from repository as existing maven projects

3. copy snapshot files to correct location:
	3.1. 'alite-1.0-SNAPSHOT.jar' to /home/<user name>/.m2/repository/cz/agents/alite/alite/1.0-SNAPSHOT
	3.2. 'zeromq-1.0-SNAPSHOT.jar' to /home/<user name>/.m2/repository/cz/agents/alite/zeromq/1.0-SNAPSHOT
	3.3. 'madla-planner-1.0-SNAPSHOT.jar' to /home/<user name>/.m2/repository/cz/agents/madla-planner/1.0-SNAPSHOT

4. Right click on project > Maven > Update maven project > select all > OK
5. Right click on project > Run > Run configurations > maven-build > run
