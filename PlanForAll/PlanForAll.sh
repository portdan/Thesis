#!/bin/bash

#./PlanForAll.sh <.pddl domain full path> <.pddl problems path> <.plan plans path> <heuristic> <recursion> <timeout (min)>
#./PlanForAll.sh Input/logistics00/domain.pddl Input/logistics00 Traces/logistics00 saFF-glcl -1 10

JAVA="/usr/bin/java"
MADLA_JAR="madla-planner.jar"
#JAR="cz.agents.madla.creator.MAPDDLCreator"

MADLAcreator="cz.agents.madla.creator.MAPDDLCreator"

domainPath=$1
problemsPath=$2
plansPath=$3

heuristic=$4
recursion=$5
timeout=$6

problemsAbsPath="$PWD/$2/*.pddl"
plansAbsPath="$PWD/$3"

for f in $problemsAbsPath; do
	
	pddlFile=${f##*/} 
	pddFfileName="${pddlFile%.*}"
	
	if [[ "${pddFfileName}" != "${domainFileName}" ]] ; then
    	
    	#echo $pddFfileName
    	
    	#echo /usr/bin/timeout -s SIGSEGV $(($4+1))m $JAVA -Xmx8G -jar $MADLA_JAR $MADLAcreator "$problemsPath/$domainType/$domainFileName.pddl" "$problemsPath/$domainType/$pddlFile" "temp/$pddFfileName.addl" $5 $6 $7
	
		/usr/bin/timeout -s SIGSEGV $(($recursion+1))m $JAVA -Xmx8G -jar $MADLA_JAR $MADLAcreator "$domainPath" "$problemsPath/$pddlFile" "temp/$pddFfileName.addl" $heuristic $recursion $timeout
	
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

  
