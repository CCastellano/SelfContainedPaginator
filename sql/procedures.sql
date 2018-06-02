

--Create the procedure for inserting metadata and whatnot.
--create or replace function handlePage(p_pagename text, p_updatetime timestamp,
--p_title text, p_rating integer, p_created_on timestamp)
--RETURNS void as $body$
--begin
--insert into pages (pagename, updatetime, title, rating, created_on) values (?,?,?,?)
--on conflict(pagename) do
--Update set pages.title = excluded.title, pages.rating = excluded.rating;
--end
--$body$ language plpgsql;


create or replace function add2(p_arg1 integer, p_arg2 integer)
return integer as $body$
begin
return p_arg1 + p_arg2;

end
$body$ language plpgsql;