(define (problem BLOCKS-4-0) (:domain blocks)
(:objects
	a - block
	c - block
	b - block
	d - block

	(:private a2
		a2 - agent
	)
)
(:init
	(handempty a2)
	(clear c)
	(clear a)
	(clear b)
	(clear d)
	(ontable c)
	(ontable a)
	(ontable b)
	(ontable d)
)
(:goal
	(and
		(on d c)
		(on c b)
		(on b a)
	)
)
)