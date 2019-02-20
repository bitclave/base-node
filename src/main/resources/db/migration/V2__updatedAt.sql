alter table offer_search
  alter column last_updated type timestamp with time zone
  using last_updated::timestamp with time zone;

alter table offer_search
  rename column last_updated to updated_at;

alter table offer add column created_at timestamp with time zone default now();

alter table offer add column updated_at timestamp with time zone default now();

alter table search_request add column created_at timestamp with time zone default now();

alter table search_request add column updated_at timestamp with time zone default now();
