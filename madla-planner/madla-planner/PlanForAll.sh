#!/bin/bash

#./PlanForAll.sh <domain type> <domain file name> <.pddl problems path> <.plan plans path> <heuristic> <recursion> <timeout (min)>
#./PlanForAll.sh logistics00 domain mapddl-problems mapddl-plans saFF-glcl -1 10

JAVA="/usr/bin/java"
MADLA_JAR="madla-planner.jar"
#JAR="cz.agents.madla.creator.MAPDDLCreator"

MADLAcreator="cz.agents.madla.creator.MAPDDLCreator"

domainType=$1
domainFileName=$2
problemsPath=$3
plansPath=$4

heuristic=$5
recursion=$6
timeout=$7

problemsAbsPath="$PWD/$3/$1/*.pddl"
plansAbsPath="$PWD/$4/$1"

for f in $problemsAbsPath; do
	
	pddlFile=${f##*/} 
	pddFfileName="${pddlFile%.*}"	
	
	problem="mapddl-problems/$1/$pddlFile"
	
	if [[ "${pddFfileName}" != "${domainFileName}" ]] ; then
    	
    	#echo $pddFfileName
    	
    	#echo /usr/bin/timeout -s SIGSEGV $(($4+1))m $JAVA -Xmx8G -jar $MADLA_JAR $MADLAcreator "$problemsPath/$domainType/$domainFileName.pddl" "$problemsPath/$domainType/$pddlFile" "temp/$pddFfileName.addl" $5 $6 $7
	
		/usr/bin/timeout -s SIGSEGV $(($recursion+1))m $JAVA -Xmx8G -jar $MADLA_JAR $MADLAcreator "$problemsPath/$domainType/$domainFileName.pddl" "$problemsPath/$domainType/$pddlFile" "temp/$pddFfileName.addl" $heuristic $recursion $timeout
	
		mv "out.plan" "$pddFfileName.plan"
		
		cp "$pddFfileName.plan" "$plansPath/$domainType"	
		
		rm "$pddFfileName.plan"
	fi
	
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

  
