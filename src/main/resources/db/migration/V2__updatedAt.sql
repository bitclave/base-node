alter table offer_search drop column last_updated;

alter table offer_search add column created_at timestamp with time zone default now();

alter table offer_search add column updated_at timestamp with time zone default now();

alter table offer add column created_at timestamp with time zone default now();

alter table offer add column updated_at timestamp with time zone default now();

alter table search_request add column created_at timestamp with time zone default now();

alter table search_request add column updated_at timestamp with time zone default now();
