package org.poc.objs.sbom.demo

import java.util.UUID

enum class DemoStack {
    JAVA,
    PYTHON,
    WEB,
    MIXED,
}

data class DemoAppSpec(
    val id: UUID,
    val name: String,
    val description: String,
    val categoryId: UUID,
    val stack: DemoStack,
    val releasedVersions: List<String> = emptyList(),
    val openDraft: Boolean = false,
    val fingerprint: Boolean = false,
    val includeDataset: Boolean = false,
    val attachVuln: Boolean = false,
)

object SbomDemoApps {
    val all: List<DemoAppSpec> =
        listOf(
            DemoAppSpec(
                SbomDemoIds.APP_PAYMENTS,
                "Payments API",
                "ISO 20022 payment initiation and confirmation for retail checkout",
                SbomDemoIds.CATEGORY_PAYMENTS,
                DemoStack.JAVA,
                releasedLineage("2.3.1", 3),
                fingerprint = true,
                attachVuln = true,
            ),
            DemoAppSpec(
                SbomDemoIds.APP_BILLING,
                "Billing API",
                "Invoice generation, dunning, and receivables for commercial clients",
                SbomDemoIds.CATEGORY_PAYMENTS,
                DemoStack.JAVA,
                releasedLineage("1.8.0", 4),
            ),
            DemoAppSpec(
                SbomDemoIds.APP_PORTAL,
                "Customer Portal",
                "Authenticated web portal for balances, transfers, and document download",
                SbomDemoIds.CATEGORY_DIGITAL,
                DemoStack.WEB,
                releasedLineage("4.12.0", 5),
            ),
            app(4, "Card Issuing Platform", "Virtual and physical card issuing, PAN tokenization", SbomDemoIds.CATEGORY_PAYMENTS, DemoStack.JAVA, "3.1.0", 3),
            app(5, "ACH Gateway", "NACHA file exchange with originating depository banks", SbomDemoIds.CATEGORY_PAYMENTS, DemoStack.JAVA, "1.4.2", 2),
            app(6, "Merchant Acquiring", "Authorization, capture, and settlement for merchant POS", SbomDemoIds.CATEGORY_PAYMENTS, DemoStack.JAVA, "5.0.1", 4, attachVuln = true),
            app(7, "Fraud Detection Engine", "Real-time scoring of card and A2A payments", SbomDemoIds.CATEGORY_PAYMENTS, DemoStack.PYTHON, "0.9.4", 3, includeDataset = true),
            app(8, "Settlement Ledger", "Double-entry settlement and nostro reconciliation", SbomDemoIds.CATEGORY_PAYMENTS, DemoStack.JAVA, "2.0.0", 2),
            app(9, "Mobile Banking iOS BFF", "BFF aggregating accounts and payments for iOS", SbomDemoIds.CATEGORY_DIGITAL, DemoStack.JAVA, "6.2.0", 3),
            app(10, "Mobile Banking Android BFF", "BFF for Android retail banking clients", SbomDemoIds.CATEGORY_DIGITAL, DemoStack.JAVA, "6.2.0", 3),
            app(11, "Online Banking Web", "Responsive online banking SPA and edge BFF", SbomDemoIds.CATEGORY_DIGITAL, DemoStack.WEB, "8.1.3", 5),
            app(12, "Account Opening Portal", "Digital KYC onboarding and product origination", SbomDemoIds.CATEGORY_DIGITAL, DemoStack.WEB, "1.6.0", 2, includeDataset = true),
            app(13, "Statement Generator", "Monthly PDF/CSV statement batch pipeline", SbomDemoIds.CATEGORY_DIGITAL, DemoStack.JAVA, "2.4.0", 2),
            app(14, "Notification Hub", "Email, SMS, and push orchestration", SbomDemoIds.CATEGORY_DIGITAL, DemoStack.PYTHON, "3.3.1", 3),
            app(15, "Authentication Service", "OIDC/OAuth2 authorization server for retail channels", SbomDemoIds.CATEGORY_DIGITAL, DemoStack.JAVA, "9.0.0", 4, attachVuln = true),
            app(16, "Session Manager", "Distributed session store and step-up auth", SbomDemoIds.CATEGORY_DIGITAL, DemoStack.JAVA, null, 0, openDraft = true),
            app(17, "Portfolio Management", "Holdings, performance, and model portfolios", SbomDemoIds.CATEGORY_WEALTH, DemoStack.JAVA, "4.0.2", 3, includeDataset = true),
            app(18, "Trading Desk UI", "Advisor order-entry workstation", SbomDemoIds.CATEGORY_WEALTH, DemoStack.WEB, "2.11.0", 2),
            app(19, "Market Data Ingest", "Refinitiv/Bloomberg tick normalization", SbomDemoIds.CATEGORY_WEALTH, DemoStack.PYTHON, "1.2.8", 4, includeDataset = true),
            app(20, "Advisor Workstation", "CRM-adjacent advice and suitability workflows", SbomDemoIds.CATEGORY_WEALTH, DemoStack.WEB, "3.5.0", 3),
            app(21, "Fund Accounting", "NAV calculation and share-class accounting", SbomDemoIds.CATEGORY_WEALTH, DemoStack.JAVA, "7.1.0", 2),
            app(22, "Policy Admin System", "Life and P&C policy lifecycle", SbomDemoIds.CATEGORY_INSURANCE, DemoStack.JAVA, "12.0.0", 5),
            app(23, "Claims Processing", "FNOL, adjudication, and payouts", SbomDemoIds.CATEGORY_INSURANCE, DemoStack.JAVA, "5.4.1", 3, includeDataset = true),
            app(24, "Underwriting Workbench", "Risk rules and referral queues", SbomDemoIds.CATEGORY_INSURANCE, DemoStack.WEB, "2.0.3", 2),
            app(25, "Quote Engine", "Rating and bind APIs for brokers", SbomDemoIds.CATEGORY_INSURANCE, DemoStack.PYTHON, "1.9.0", 3),
            app(26, "Broker Portal", "External broker self-service", SbomDemoIds.CATEGORY_INSURANCE, DemoStack.WEB, "4.3.0", 2),
            app(27, "Cash Management", "Corporate liquidity and sweeps", SbomDemoIds.CATEGORY_CORPORATE_BANKING, DemoStack.JAVA, "3.8.0", 3),
            app(28, "Trade Finance", "Letters of credit and guarantees", SbomDemoIds.CATEGORY_CORPORATE_BANKING, DemoStack.JAVA, "2.2.1", 2),
            app(29, "FX Pricing Engine", "Spot/forward pricing and spreads", SbomDemoIds.CATEGORY_CORPORATE_BANKING, DemoStack.PYTHON, "0.7.5", 2, includeDataset = true),
            app(30, "SWIFT Gateway", "Alliance Lite2 / SWIFTNet file and MX traffic", SbomDemoIds.CATEGORY_CORPORATE_BANKING, DemoStack.JAVA, "1.5.0", 3, attachVuln = true),
            app(31, "Collateral Management", "Margin calls and eligible collateral", SbomDemoIds.CATEGORY_CORPORATE_BANKING, DemoStack.JAVA, "1.1.0", 1),
            app(32, "Customer 360 Lakehouse", "Conformed customer entities on Iceberg", SbomDemoIds.CATEGORY_DATA, DemoStack.PYTHON, "0.18.0", 4, includeDataset = true),
            app(33, "Credit Risk Models", "PD/LGD batch scoring on Spark", SbomDemoIds.CATEGORY_DATA, DemoStack.PYTHON, "2.6.0", 5, includeDataset = true, fingerprint = true),
            app(34, "AML Transaction Monitor", "Scenario detection and SAR caseing", SbomDemoIds.CATEGORY_DATA, DemoStack.PYTHON, "4.1.0", 3, includeDataset = true),
            app(35, "Feature Store", "Online/offline features for risk and personalization", SbomDemoIds.CATEGORY_DATA, DemoStack.PYTHON, "1.0.4", 2, includeDataset = true),
            app(36, "Experiment Tracking", "Model registry and experiment metadata", SbomDemoIds.CATEGORY_DATA, DemoStack.PYTHON, "0.4.2", 2),
            app(37, "Recommendation Engine", "Next-best-product ranking", SbomDemoIds.CATEGORY_DATA, DemoStack.PYTHON, "1.3.0", 3, includeDataset = true),
            app(38, "Document Intelligence", "OCR and classification for KYC packs", SbomDemoIds.CATEGORY_DATA, DemoStack.PYTHON, null, 0, openDraft = true, includeDataset = true),
            app(39, "API Gateway", "Edge routing, rate limits, and WAF integration", SbomDemoIds.CATEGORY_PLATFORM, DemoStack.JAVA, "8.4.0", 4),
            app(40, "Identity Provider", "Workforce SSO and privileged access", SbomDemoIds.CATEGORY_PLATFORM, DemoStack.JAVA, "11.2.0", 3, attachVuln = true),
            app(41, "CI/CD Control Plane", "Tekton/Jenkins orchestration and promotion", SbomDemoIds.CATEGORY_PLATFORM, DemoStack.MIXED, "5.0.0", 2),
            app(42, "Observability Stack", "Metrics, traces, and log routing", SbomDemoIds.CATEGORY_PLATFORM, DemoStack.MIXED, "2.9.1", 3),
            app(43, "Kubernetes Platform Portal", "Cluster self-service and golden paths", SbomDemoIds.CATEGORY_PLATFORM, DemoStack.WEB, "1.7.0", 2),
            app(44, "Secrets Manager", "App-role secrets and rotation", SbomDemoIds.CATEGORY_PLATFORM, DemoStack.JAVA, "3.2.0", 2),
            app(45, "Developer Portal", "Inner-source catalog and scorecards", SbomDemoIds.CATEGORY_PLATFORM, DemoStack.WEB, null, 0, openDraft = true),
            app(46, "HR Self-Service", "People Center leave, payroll views, org chart", SbomDemoIds.CATEGORY_CORP_FUNCTIONS, DemoStack.WEB, "6.0.0", 3),
            app(47, "Procurement Catalog", "Vendor items and purchase orders", SbomDemoIds.CATEGORY_CORP_FUNCTIONS, DemoStack.JAVA, "2.3.0", 2),
            app(48, "Finance Close", "Subledger to GL close orchestration", SbomDemoIds.CATEGORY_CORP_FUNCTIONS, DemoStack.JAVA, "1.0.5", 3),
            app(49, "Legal Matter Management", "Matters, holds, and outside counsel", SbomDemoIds.CATEGORY_CORP_FUNCTIONS, DemoStack.WEB, "0.8.0", 1),
            app(50, "Facilities IoT Hub", "Badge, HVAC, and occupancy telemetry", SbomDemoIds.CATEGORY_CORP_FUNCTIONS, DemoStack.PYTHON, "1.4.0", 2, includeDataset = true),
            app(51, "Branch Platform", "Teller and branch appointment orchestration", SbomDemoIds.CATEGORY_RETAIL, DemoStack.JAVA, "3.0.1", 3),
            app(52, "Open Banking PSD2", "Account information and payment initiation APIs", SbomDemoIds.CATEGORY_DIGITAL, DemoStack.JAVA, "2.5.0", 4),
            app(53, "Contact Center Voice", "IVR, transcription, and agent assist", SbomDemoIds.CATEGORY_DIGITAL, DemoStack.PYTHON, "1.2.0", 3, includeDataset = true),
            app(54, "ESG Reporting", "Financed-emissions and sustainability disclosures", SbomDemoIds.CATEGORY_CORP_FUNCTIONS, DemoStack.PYTHON, "0.6.2", 2, includeDataset = true),
            app(55, "Loan Origination", "Retail unsecured and mortgage origination", SbomDemoIds.CATEGORY_RETAIL, DemoStack.JAVA, "8.3.0", 5),
            app(56, "Collections Workspace", "Delinquency strategies and agent desktop", SbomDemoIds.CATEGORY_RETAIL, DemoStack.WEB, "2.1.4", 2),
            app(57, "ATM Switch", "ISO 8583 routing for ATM and POS", SbomDemoIds.CATEGORY_PAYMENTS, DemoStack.JAVA, "4.4.0", 3),
            app(58, "Loyalty Engine", "Points accrual and partner offers", SbomDemoIds.CATEGORY_DIGITAL, DemoStack.PYTHON, "1.8.0", 2),
            app(59, "Research Portal", "Investment research publishing for advisors", SbomDemoIds.CATEGORY_WEALTH, DemoStack.WEB, "3.2.1", 3),
            app(60, "Reinsurance Treaty", "Cession, bordereaux, and recoveries", SbomDemoIds.CATEGORY_INSURANCE, DemoStack.JAVA, "1.0.0", 1),
            app(61, "Custody Platform", "Safekeeping, corporate actions, and tax lots", SbomDemoIds.CATEGORY_CORPORATE_BANKING, DemoStack.JAVA, "6.1.0", 4),
            app(62, "Data Catalog", "Dataset discovery and classification", SbomDemoIds.CATEGORY_DATA, DemoStack.PYTHON, "0.11.0", 3, includeDataset = true),
            app(63, "Feature Pipeline UI", "Feature job authoring and lineage", SbomDemoIds.CATEGORY_DATA, DemoStack.WEB, "0.9.0", 2),
            app(64, "Service Mesh Control", "mTLS policy and traffic split", SbomDemoIds.CATEGORY_PLATFORM, DemoStack.MIXED, "2.0.3", 3),
            app(65, "Chaos Console", "Game-day experiments against platform SLOs", SbomDemoIds.CATEGORY_PLATFORM, DemoStack.WEB, "0.5.1", 1),
            app(66, "Payroll Engine", "Gross-to-net and statutory filings", SbomDemoIds.CATEGORY_CORP_FUNCTIONS, DemoStack.JAVA, "9.2.0", 4),
            app(67, "Expense Management", "T&E capture and policy checks", SbomDemoIds.CATEGORY_CORP_FUNCTIONS, DemoStack.WEB, "4.0.0", 3),
            app(68, "Vendor Risk Portal", "Third-party questionnaires and findings", SbomDemoIds.CATEGORY_CORP_FUNCTIONS, DemoStack.WEB, "1.3.2", 2),
            app(69, "Workplace Booking", "Desk and meeting-room reservations", SbomDemoIds.CATEGORY_CORP_FUNCTIONS, DemoStack.WEB, "2.4.0", 2),
            app(70, "Inner Source Wiki", "Engineering handbook and RFCs", SbomDemoIds.CATEGORY_PLATFORM, DemoStack.MIXED, "1.1.0", 2, openDraft = true),
        )

    private fun app(
        n: Int,
        name: String,
        description: String,
        categoryId: UUID,
        stack: DemoStack,
        latest: String?,
        history: Int,
        openDraft: Boolean = false,
        fingerprint: Boolean = false,
        includeDataset: Boolean = false,
        attachVuln: Boolean = false,
    ) = DemoAppSpec(
        id = SbomDemoIds.numberedApp(n),
        name = name,
        description = description,
        categoryId = categoryId,
        stack = stack,
        releasedVersions = if (latest == null) emptyList() else releasedLineage(latest, history),
        openDraft = openDraft,
        fingerprint = fingerprint,
        includeDataset = includeDataset,
        attachVuln = attachVuln,
    )
}

internal fun releasedLineage(latest: String, count: Int): List<String> {
    val n = count.coerceIn(1, 5)
    if (n == 1) return listOf(latest)
    data class TripleV(val a: Int, val b: Int, val c: Int) {
        fun prev(): TripleV =
            when {
                c > 0 -> TripleV(a, b, c - 1)
                b > 0 -> TripleV(a, b - 1, 0)
                a > 0 -> TripleV(a - 1, 0, 0)
                else -> TripleV(0, 0, 0)
            }

        override fun toString(): String = "$a.$b.$c"
    }
    val bits = latest.split('.').map { part -> part.filter { it.isDigit() }.toIntOrNull() ?: 0 }
    var cur = TripleV(bits.getOrElse(0) { 1 }, bits.getOrElse(1) { 0 }, bits.getOrElse(2) { 0 })
    val acc = ArrayDeque<String>()
    acc.addFirst(latest)
    repeat(n - 1) {
        cur = cur.prev()
        var next = cur.toString()
        var bump = 0
        while (acc.contains(next) || next == latest) {
            bump++
            next = "${cur.a}.${cur.b}.${cur.c + bump}"
        }
        acc.addFirst(next)
    }
    return acc.toList()
}
