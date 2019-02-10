(define (domain logistics)
	(:requirements :typing)
(:types
	apn1 - airplane
	airplane location vehicle package city truck - object
	tru1 - truck
	airport - location
	truck airplane - vehicle
)
(:predicates
	(in ?obj1 - package ?veh - vehicle)
	(at ?obj - object ?loc - location)
	(in-city ?loc - location ?city - city)
)

(:action load-truck-tru1
	:parameters (?truck - tru1 ?obj - package ?loc - location)
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
	:parameters (?truck - tru1 ?obj - package ?loc - location)
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
	:parameters (?truck - tru1 ?loc-from - location ?loc-to - location ?city - city)
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


(:action fly-airplane-apn1-param-apn1-param-apt2-param-apt1
	:parameters (?apn1 - apn1)
	:precondition 
		(at apn1 apt2)
	:effect 
		(at apn1 apt1)
)


(:action fly-airplane-apn1-param-apn1-param-apt1-param-apt2
	:parameters (?apn1 - apn1)
	:precondition 
		(at apn1 apt1)
	:effect 
		(at apn1 apt2)
)


(:action load-airplane-apn1-param-apn1-param-obj11-param-apt1
	:parameters (?apn1 - apn1)
	:precondition (and
		(at apn1 apt1)
		(at obj11 apt1)
	)
	:effect 
		(in obj11 apn1)
)


(:action unload-airplane-apn1-param-apn1-param-obj11-param-apt2
	:parameters (?apn1 - apn1)
	:precondition (and
		(at apn1 apt2)
		(in obj11 apn1)
		(at tru1 apt1)
	)
	:effect 
		(at obj11 apt2)
)

)