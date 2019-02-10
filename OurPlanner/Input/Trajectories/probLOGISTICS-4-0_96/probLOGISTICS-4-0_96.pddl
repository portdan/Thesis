(define (problem probLOGISTICS-4-0_96) (:domain logistics)
(:objects
	cit2 - city
	cit1 - city
	obj11 - package
	tru1 - tru1
	apn1 - apn1
	apt2 - airport
	apt1 - airport
	pos1 - location
)
(:init
	(at apn1 apt2)
	(at tru1 pos1)
	(at obj11 pos1)
	(in-city pos1 cit1)
	(in-city apt1 cit1)
	(in-city apt2 cit2)
)
(:goal
	(and
		(at obj11 pos1)
		(at apn1 apt1)
		(at tru1 apt1)
	)
)
)