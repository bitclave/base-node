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
