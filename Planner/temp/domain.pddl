(define (domain logistics)
	(:requirements :typing)
(:types
	apn1 - airplane
	airplane location vehicle package city truck - object
	tru1 tru2 - truck
	airport - location
	truck airplane - vehicle
)
(:predicates
	(at ?obj - object ?loc - location)
	(in ?obj1 - package ?veh - vehicle)
	(in-city ?loc - location ?city - city)
)

(:action load-airplane-apn1
	:parameters (?airplane - apn1 ?obj - package ?loc - airport)
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
	:parameters (?airplane - apn1 ?obj - package ?loc - airport)
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
	:parameters (?airplane - apn1 ?loc-from - airport ?loc-to - airport)
	:precondition 
		(at ?airplane ?loc-from)
	:effect (and
		(not (at ?airplane ?loc-from))
		(at ?airplane ?loc-to)
	)
)

)