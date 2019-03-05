package com.bitclave.node

import com.bitclave.node.account.AccountServiceHybridTest
import com.bitclave.node.account.AccountServiceTest
import com.bitclave.node.account.AuthControllerTest
import com.bitclave.node.clientData.ClientProfileControllerTest
import com.bitclave.node.clientData.ClientProfileServiceHybridTest
import com.bitclave.node.clientData.ClientProfileServiceTest
import com.bitclave.node.offer.OfferControllerTest
import com.bitclave.node.offer.OfferServiceTest
import com.bitclave.node.requestData.RequestDataControllerTest
import com.bitclave.node.requestData.RequestDataServiceHybridTest
import com.bitclave.node.requestData.RequestDataServiceTest
import com.bitclave.node.search.SearchRequestControllerTest
import com.bitclave.node.search.SearchRequestServiceTest
import com.bitclave.node.search.offer.OfferSearchControllerTest
import com.bitclave.node.search.offer.OfferSearchServiceTest
import com.bitclave.node.share.OfferShareControllerTest
import com.bitclave.node.share.OfferShareServiceTest
import com.bitclave.node.verify.VerifyConsistencyControllerTest
import com.bitclave.node.verify.VerifyConsistencyIntegrationTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    value = [
        VerifyConsistencyControllerTest::class,
        VerifyConsistencyIntegrationTest::class,
        AuthControllerTest::class,
        AccountServiceTest::class,
        AccountServiceHybridTest::class,
        ClientProfileControllerTest::class,
        ClientProfileServiceTest::class,
        ClientProfileServiceHybridTest::class,
        RequestDataServiceHybridTest::class,
        RequestDataControllerTest::class,
        RequestDataServiceTest::class,
        OfferControllerTest::class,
        OfferServiceTest::class,
        SearchRequestControllerTest::class,
        SearchRequestServiceTest::class,
        OfferSearchControllerTest::class,
        OfferSearchServiceTest::class,
        OfferShareControllerTest::class,
        OfferShareServiceTest::class
    ]
)
class RunAllTests
