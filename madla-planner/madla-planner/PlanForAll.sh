#!/bin/bash

JAVA="/usr/bin/java"
JAR="madla-planner.jar"
#JAR="cz.agents.madla.creator.MAPDDLCreator"


problems="$PWD/mapddl-problems/$1/*.pddl"
plans="$PWD/mapddl-plans/$1"


for pddl in $problems; do
	
	pddlFile=${pddl##*/} 
	problem="mapddl-problems/$1/$pddlFile"
	
			
	#echo /usr/bin/timeout -s SIGSEGV $(($4+1))m $JAVA -Xmx8G -jar $JAR cz.agents.madla.creator.MAPDDLCreator "mapddl-benchmarks/$1/domain.pddl" $problem "temp/$2.addl" $3 $4 $5
	
	 /usr/bin/timeout -s SIGSEGV $(($4+1))m $JAVA -Xmx8G -jar $JAR cz.agents.madla.creator.MAPDDLCreator "mapddl-problems/$1/domain.pddl" $problem "temp/$2.addl" $3 $4 $5

	cp $pddl $plans
	#cp out.plan mapddl-problems-plans
done


#cz.agents.madla.creator.MAPDDLCreator 
#mapddl-benchmarks/logistics00/domain.pddl 
#mapddl-benchmarks/logistics00/probLOGISTICS-4-0.pddl 
#temp/probLOGISTICS-4-0.addl 
#saFF-glcl 
#-1 
#10



#echo timeout -s SIGSEGV $(($4+1))m $JAVA -Xmx8G -jar madla-planner.jar cz.agents.madla.creator.ProtobufCreator "./benchmarks/$1/domain.pddl" "./benchmarks/$1/$2.pddl" "./benchmarks/$1/$2.addl" $3 $4 $5
#timeout -s SIGSEGV $(($4+1))m $JAVA -Xmx8G -jar madla-planner.jar cz.agents.madla.creator.ProtobufCreator "./benchmarks/$1/domain.pddl" "./benchmarks/$1/$2.pddl" "./benchmarks/$1/$2.addl" $3 $4 $5

  
