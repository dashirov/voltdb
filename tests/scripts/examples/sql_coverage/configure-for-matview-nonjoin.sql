<grammar.sql>

{_basetables |= "P2"}
{_basetables |= "R2"}

{@aftermath = " _math _value[int:1,3]"}
{@agg = "_numagg"}
{@cmp = "_cmp"} -- use all comparison operators (=, <>, !=, <, >, <=, >=)
{@somecmp = "_somecmp"} -- a smaller list of comparison operators (=, <, >=) [not used here?]
{@columnpredicate = "_variable[@comparabletype] @cmp _value[int16]"}
{@columntype = "int"}
{@comparableconstant = "42"}
{@comparabletype = "numeric"}
{@comparablevalue = "_numericvalue"}
{@dmlcolumnpredicate = "_variable[int] @cmp _value[int]"}
{@idcol = "V_G1"}
{@numcol = "V_SUM_AGE"}
{@star = "*"}
{@lhsstar = "*"}

{@dmltable = "_basetables"}
{@fromtables = "_table"}
