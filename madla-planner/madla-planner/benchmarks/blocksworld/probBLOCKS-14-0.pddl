(define (problem BLOCKS-14-0)
(:domain BLOCKS)
(:objects I D B L C K M H J N E F G A a1 a2 a3 a4)
(:INIT (agent a1) (agent a2) (agent a3) (agent a4) (HANDEMPTY a1) (HANDEMPTY a2) (HANDEMPTY a3) (HANDEMPTY a4) (CLEAR A) (CLEAR G) (CLEAR F) (ONTABLE E) (ONTABLE N) (ONTABLE F)
 (ON A J) (ON J H) (ON H M) (ON M K) (ON K C) (ON C L) (ON L B) (ON B E)
 (ON G D) (ON D I) (ON I N)  )
(:goal (AND (ON E L) (ON L F) (ON F B) (ON B J) (ON J I) (ON I N) (ON N C)
            (ON C K) (ON K G) (ON G D) (ON D M) (ON M A) (ON A H)))
)
