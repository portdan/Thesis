(define (problem BLOCKS-6-1)
(:domain BLOCKS)
(:objects F D C E B A a1 a2 a3 a4)
(:INIT (agent a1) (agent a2) (agent a3) (agent a4) (HANDEMPTY a1) (HANDEMPTY a2) (HANDEMPTY a3) (HANDEMPTY a4) (CLEAR A) (CLEAR B) (CLEAR E) (CLEAR C) (CLEAR D) (ONTABLE F)
 (ONTABLE B) (ONTABLE E) (ONTABLE C) (ONTABLE D) (ON A F) )
(:goal (AND (ON E F) (ON F C) (ON C B) (ON B A) (ON A D)))
)
