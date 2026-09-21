package kz.mybrain.ofdcodec

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kz.bfd.proto.v204.CommodityTypeEnum
import kz.mybrain.ofdcodec.ofd.bfd.v204.codec.ticket.TicketRequestBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Тип предмета потребления в чеке 2.0.4.
 *
 * Реквизит читался нигде: касса его посылала, сборщик до protobuf его
 * не доносил, и ОФД на позицию с НТИН отвечал кодом 15 «Касса
 * заблокирована» — первая же продажа товара из каталога останавливала
 * работу.
 */
class BfdV204CommodityTypeTest {

    @Test
    fun `тип предмета потребления доходит до protobuf`() {
        val commodity = ticket(""""ntin": "0200198799025", "commodityType": "COMMODITY_TYPE_PRODUCT",""")
            .items[0].commodity

        assertEquals(CommodityTypeEnum.COMMODITY_TYPE_PRODUCT, commodity?.commodity_type)
    }

    @Test
    fun `без реквизита позиция остаётся без типа`() {
        // Реквизит необязательный: у позиции без НТИН его не спрашивают.
        assertNull(ticket("").items[0].commodity?.commodity_type)
    }

    @Test
    fun `неизвестный тип назван в отказе целиком`() {
        val failure = runCatching { ticket(""""commodityType": "COMMODITY_TYPE_HOUSE",""") }.exceptionOrNull()

        assertTrue(failure?.message?.contains("COMMODITY_TYPE_HOUSE") == true, "отказ: ${failure?.message}")
    }

    private fun ticket(extra: String) = TicketRequestBuilder().build(
        Json.parseToJsonElement(
            """
            {
              "ticket": {
                "operation": "OPERATION_SELL",
                "dateTime": {
                  "date": { "year": 2026, "month": 9, "day": 21 },
                  "time": { "hour": 21, "minute": 45, "second": 0 }
                },
                "operator": { "code": 1 },
                "domain": "DOMAIN_TRADING",
                "items": [
                  {
                    "type": "ITEM_TYPE_COMMODITY",
                    "commodity": {
                      "name": "Котёл отопительный",
                      "sectionCode": "001",
                      "quantity": 1000,
                      "price": { "bills": 2500, "coins": 0 },
                      "sum": { "bills": 2500, "coins": 0 },
                      "measureUnitCode": "796",
                      $extra
                      "taxes": []
                    }
                  }
                ],
                "payments": [
                  { "type": "PAYMENT_CASH", "sum": { "bills": 2500, "coins": 0 } }
                ],
                "amounts": {
                  "total": { "bills": 2500, "coins": 0 },
                  "taken": { "bills": 2500, "coins": 0 },
                  "change": { "bills": 0, "coins": 0 }
                }
              }
            }
            """.trimIndent()
        ) as JsonObject
    )
}
