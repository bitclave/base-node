--delete  ALL offers
delete from offer_search_events;
delete from offer_search;

delete from offer_price_rules;
delete from offer_price;
delete from offer_tags;
delete from offer_rules;
delete from offer_compare;
delete from offer;
--~delete  ALL offers

--delete offerSearch with no matching offer
delete from offer_search_events where offer_search_events.offer_search_id in
	(select offer_search.id from offer_search where offer_search.id not in
		(
			select offer.id from offer
		));

delete from offer_search where offer_search.id not in
	(
		select offer.id from offer
	);
--~delete offerSearch with no matching offer

-- find dangling offerSearch with no matching Offer
select * from offer_search where offer_search.id not in
	(
		select offer.id from offer
	);
-- find dangling offerSearch with no matching Offer

-- delete all search requests
delete from search_request_tags;
delete from search_request;
-- ~delete all search requests

--  delete accounts and data
delete from account where public_key in (
'02155b8cd0c18c13fe48b4d268c021e3a86e50aa0e41904c4c6bb0e18c2ab8786f',
'0288d44c708d386fa02672513e8f4804d3632a16a8225991e644b7311698a11b50'
);
delete from client_data_data where client_data_public_key not in (select  public_key from account);
delete from client_data where public_key not in (select  public_key from account);
delete from request_data where to_pk not in (select public_key from account);
delete from offer_search_events where offer_search_id in
	(select id from offer_search where owner not in (select public_key from account));
delete from offer_search where owner not in (select public_key from account);

delete from search_request_tags where search_request_id in
	(select id from search_request where owner not in (select public_key from account));
delete from search_request where owner not in (select public_key from account);
-- ~delete accounts and data
