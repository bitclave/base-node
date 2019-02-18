alter table offer_search
  alter column last_updated type timestamp using last_updated::timestamp;

alter table offer_search
  RENAME COLUMN last_updated to updated_at;
