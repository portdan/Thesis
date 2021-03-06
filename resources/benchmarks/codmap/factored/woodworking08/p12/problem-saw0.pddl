(define (problem wood-prob) (:domain woodworking)
(:objects
	black - acolour
	blue - acolour
	mauve - acolour
	green - acolour
	cherry - awood
	pine - awood
	p0 - part
	p1 - part
	p2 - part
	p3 - part
	p4 - part
	p5 - part
	b0 - board
	b1 - board
	s0 - aboardsize
	s1 - aboardsize
	s2 - aboardsize
	s3 - aboardsize
	s4 - aboardsize
	s5 - aboardsize
	s6 - aboardsize
	s7 - aboardsize
	s8 - aboardsize

	(:private
		saw0 - saw
	)
)
(:init
	(is-smooth smooth)
	(is-smooth verysmooth)
	(boardsize-successor s0 s1)
	(boardsize-successor s1 s2)
	(boardsize-successor s2 s3)
	(boardsize-successor s3 s4)
	(boardsize-successor s4 s5)
	(boardsize-successor s5 s6)
	(boardsize-successor s6 s7)
	(boardsize-successor s7 s8)
	(available p0)
	(colour p0 black)
	(wood p0 cherry)
	(surface-condition p0 rough)
	(treatment p0 glazed)
	(goalsize p0 small)
	(unused p1)
	(goalsize p1 large)
	(unused p2)
	(goalsize p2 large)
	(unused p3)
	(goalsize p3 small)
	(unused p4)
	(goalsize p4 large)
	(available p5)
	(colour p5 mauve)
	(wood p5 cherry)
	(surface-condition p5 rough)
	(treatment p5 colourfragments)
	(goalsize p5 medium)
	(boardsize b0 s5)
	(wood b0 cherry)
	(surface-condition b0 rough)
	(available b0)
	(boardsize b1 s8)
	(wood b1 pine)
	(surface-condition b1 smooth)
	(available b1)
	(= (total-cost) 0) 
	(= (spray-varnish-cost p0) 5) 
	(= (glaze-cost p0) 10) 
	(= (grind-cost p0) 15) 
	(= (plane-cost p0) 10) 
	(= (spray-varnish-cost p1) 15) 
	(= (glaze-cost p1) 20) 
	(= (grind-cost p1) 45) 
	(= (plane-cost p1) 30) 
	(= (spray-varnish-cost p2) 15) 
	(= (glaze-cost p2) 20) 
	(= (grind-cost p2) 45) 
	(= (plane-cost p2) 30) 
	(= (spray-varnish-cost p3) 5) 
	(= (glaze-cost p3) 10) 
	(= (grind-cost p3) 15) 
	(= (plane-cost p3) 10) 
	(= (spray-varnish-cost p4) 15) 
	(= (glaze-cost p4) 20) 
	(= (grind-cost p4) 45) 
	(= (plane-cost p4) 30) 
	(= (spray-varnish-cost p5) 10) 
	(= (glaze-cost p5) 15) 
	(= (grind-cost p5) 30) 
	(= (plane-cost p5) 20) 
)
(:goal
	(and
		(available p0)
		(surface-condition p0 verysmooth)
		(treatment p0 varnished)
		(available p1)
		(wood p1 pine)
		(surface-condition p1 smooth)
		(available p2)
		(colour p2 blue)
		(wood p2 pine)
		(surface-condition p2 verysmooth)
		(treatment p2 varnished)
		(available p3)
		(wood p3 cherry)
		(surface-condition p3 smooth)
		(available p4)
		(wood p4 cherry)
		(surface-condition p4 verysmooth)
		(available p5)
		(wood p5 cherry)
		(surface-condition p5 smooth)
	)
)
(:metric minimize (total-cost))
)