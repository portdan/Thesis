(define (problem BLOCKS-9-0)
(:domain BLOCKS)
(:objects H D I A E G B F C a1 a2 a3 a4)
(:INIT (agent a1) (agent a2) (agent a3) (agent a4) (HANDEMPTY a1) (HANDEMPTY a2) (HANDEMPTY a3) (HANDEMPTY a4) (CLEAR C) (CLEAR F) (ONTABLE C) (ONTABLE B) (ON F G) (ON G E) (ON E A)
 (ON A I) (ON I D) (ON D H) (ON H B) )
(:goal (AND (ON G D) (ON D B) (ON B C) (ON C A) (ON A I) (ON I F) (ON F E)
            (ON E H)))
)
