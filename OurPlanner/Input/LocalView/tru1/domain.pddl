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
	(in ?obj1 - package ?veh - vehicle)
	(at ?obj - object ?loc - location)

	(:private ?agent - truck
		(in-city ?loc - location ?city - city)
	)
)

(:action load-truck-tru1
	:agent ?truck - tru1
	:parameters (?obj - package ?loc - location)
	:precondition (and
		(at ?truck ?loc)
		(at ?obj ?loc)
	)
	:effect (and
		(not (at ?obj ?loc))
		(in ?obj ?truck)
	)
)


(:action unload-truck-tru1
	:agent ?truck - tru1
	:parameters (?obj - package ?loc - location)
	:precondition (and
		(at ?truck ?loc)
		(in ?obj ?truck)
	)
	:effect (and
		(not (in ?obj ?truck))
		(at ?obj ?loc)
	)
)


(:action drive-truck-tru1
	:agent ?truck - tru1
	:parameters (?loc-from - location ?loc-to - location ?city - city)
	:precondition (and
		(at ?truck ?loc-from)
		(in-city ?loc-from ?city)
		(in-city ?loc-to ?city)
	)
	:effect (and
		(not (at ?truck ?loc-from))
		(at ?truck ?loc-to)
	)
)

)