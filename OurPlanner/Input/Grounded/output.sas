begin_version
3
end_version
begin_metric
0
end_metric
3
begin_variable
var0
-1
2
Atom at(apn1, apt1)
Atom at(apn1, apt2)
end_variable
begin_variable
var1
-1
5
Atom at(obj11, apt1)
Atom at(obj11, apt2)
Atom at(obj11, pos1)
Atom in(obj11, apn1)
Atom in(obj11, tru1)
end_variable
begin_variable
var2
-1
2
Atom at(tru1, apt1)
Atom at(tru1, pos1)
end_variable
3
begin_mutex_group
2
0 0
0 1
end_mutex_group
begin_mutex_group
5
1 0
1 1
1 2
1 3
1 4
end_mutex_group
begin_mutex_group
2
2 0
2 1
end_mutex_group
begin_state
1
2
1
end_state
begin_goal
1
1 1
end_goal
12
begin_operator
drive-truck-tru1 tru1 apt1 pos1 cit1
0
1
0 2 0 1
1
end_operator
begin_operator
drive-truck-tru1 tru1 pos1 apt1 cit1
0
1
0 2 1 0
1
end_operator
begin_operator
fly-airplane-apn1 apn1 apt1 apt2
0
1
0 0 0 1
1
end_operator
begin_operator
fly-airplane-apn1 apn1 apt2 apt1
0
1
0 0 1 0
1
end_operator
begin_operator
load-airplane-apn1 apn1 obj11 apt1
1
0 0
1
0 1 0 3
1
end_operator
begin_operator
load-airplane-apn1 apn1 obj11 apt2
1
0 1
1
0 1 1 3
1
end_operator
begin_operator
load-truck-tru1 tru1 obj11 apt1
1
2 0
1
0 1 0 4
1
end_operator
begin_operator
load-truck-tru1 tru1 obj11 pos1
1
2 1
1
0 1 2 4
1
end_operator
begin_operator
unload-airplane-apn1 apn1 obj11 apt1
1
0 0
1
0 1 3 0
1
end_operator
begin_operator
unload-airplane-apn1 apn1 obj11 apt2
1
0 1
1
0 1 3 1
1
end_operator
begin_operator
unload-truck-tru1 tru1 obj11 apt1
1
2 0
1
0 1 4 0
1
end_operator
begin_operator
unload-truck-tru1 tru1 obj11 pos1
1
2 1
1
0 1 4 2
1
end_operator
0
