(define (problem BLOCKS-11-2)
(:domain BLOCKS)
(:objects E J D C F K H G A I B a1 a2 a3 a4)
(:INIT (agent a1) (agent a2) (agent a3) (agent a4) (HANDEMPTY a1) (HANDEMPTY a2) (HANDEMPTY a3) (HANDEMPTY a4) (CLEAR B) (CLEAR I) (ONTABLE A) (ONTABLE G) (ON B H) (ON H K) (ON K F)
 (ON F C) (ON C D) (ON D J) (ON J A) (ON I E) (ON E G) )
(:goal (AND (ON I G) (ON G C) (ON C D) (ON D E) (ON E J) (ON J B) (ON B H)
            (ON H A) (ON A F) (ON F K)))
)
