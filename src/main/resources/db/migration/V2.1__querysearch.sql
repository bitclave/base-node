create table "query_search_request" (
 "id" int8 NOT NULL primary key,
 "owner" varchar(256) collate "pg_catalog"."default",
 "query" varchar(256) collate "pg_catalog"."default"
);
