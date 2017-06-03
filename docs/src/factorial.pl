/* 01 = 1 */ 
factorial(0,1).
/* A! = A*(A-1)! */
factorial(A,B) :-
	A>0, C is A-1,
	factorial(C,D), B is A*D.