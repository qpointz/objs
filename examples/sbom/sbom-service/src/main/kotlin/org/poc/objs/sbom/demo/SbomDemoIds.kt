package org.poc.objs.sbom.demo

import java.util.UUID

object SbomDemoIds {
    val PORTFOLIO: UUID = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val CATEGORY_PLATFORM: UUID = UUID.fromString("22222222-2222-2222-2222-222222222222")
    val CATEGORY_PAYMENTS: UUID = UUID.fromString("33333333-3333-3333-3333-333333333333")
    val CATEGORY_RETAIL: UUID = UUID.fromString("44444444-4444-4444-4444-444444444444")
    val CATEGORY_DIGITAL: UUID = UUID.fromString("55555555-5555-5555-5555-555555555555")
    val CATEGORY_WEALTH: UUID = UUID.fromString("66666666-6666-6666-6666-666666666666")
    val CATEGORY_INSURANCE: UUID = UUID.fromString("77777777-7777-7777-7777-777777777777")
    val CATEGORY_CORPORATE_BANKING: UUID = UUID.fromString("88888888-8888-8888-8888-888888888888")
    val CATEGORY_DATA: UUID = UUID.fromString("99999999-9999-9999-9999-999999999999")
    val CATEGORY_CORP_FUNCTIONS: UUID = UUID.fromString("aaaa0001-aaaa-4000-8000-000000000001")

    val APP_PAYMENTS: UUID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
    val APP_BILLING: UUID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
    val APP_PORTAL: UUID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc")

    fun numberedApp(n: Int): UUID = UUID.fromString("a0000000-0000-4000-8000-%012d".format(n))
}
