(define (problem logistics-4-0) (:domain logistics)
(:objects
	obj21 - package
	obj22 - package
	obj23 - package
	tru1 - truck
	cit1 - city
	cit2 - city
	tru2 - truck
	apn1 - airplane
	apt2 - airport
	apt1 - airport
	obj11 - package
	obj13 - package
	obj12 - package
	pos2 - location
	pos1 - location
)
(:init
	(at apn1 apt2)
	(at tru1 pos1)
	(at obj11 pos1)
	(at obj12 pos1)
	(at obj13 pos1)
	(at tru2 pos2)
	(at obj21 pos2)
	(at obj22 pos2)
	(at obj23 pos2)
	(in-city pos1 cit1)
	(in-city apt1 cit1)
	(in-city pos2 cit2)
	(in-city apt2 cit2)
)
(:goal
	(and
		(in obj23 tru2)
		(at obj21 pos2)
		(at obj13 pos1)
		(at obj11 pos1)
		(at apn1 apt2)
		(at tru1 pos1)
		(at tru2 pos2)
	)
)
)