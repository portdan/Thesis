(define (domain logistics)
	(:requirements :typing :multi-agent :unfactored-privacy)
(:types
	apn1 - airplane
	location vehicle package city - object
	tru1 - truck
	airport - location
	truck airplane - vehicle
)
(:predicates
	(at ?obj - object ?loc - location)
	(in ?obj1 - package ?veh - vehicle)

	(:private ?agent - truck
		(in-city ?loc - location ?city - city)
	)
)

(:action load-airplane-apn1
	:agent ?airplane - apn1
	:parameters (?obj - package ?loc - airport)
	:precondition (and
		(at ?obj ?loc)
		(at ?airplane ?loc)
	)
	:effect (and
		(not (at ?obj ?loc))
		(in ?obj ?airplane)
	)
)


(:action unload-airplane-apn1
	:agent ?airplane - apn1
	:parameters (?obj - package ?loc - airport)
	:precondition (and
		(in ?obj ?airplane)
		(at ?airplane ?loc)
	)
	:effect (and
		(not (in ?obj ?airplane))
		(at ?obj ?loc)
	)
)


(:action fly-airplane-apn1
	:agent ?airplane - apn1
	:parameters (?loc-from - airport ?loc-to - airport)
	:precondition 
		(at ?airplane ?loc-from)
	:effect (and
		(not (at ?airplane ?loc-from))
		(at ?airplane ?loc-to)
	)
)

)