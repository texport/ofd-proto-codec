package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.zxreport

import kotlinx.serialization.json.JsonObject
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.DateTimeValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.MoneyValidator

/**
 * Валидация ZXReport для протокола Казахтелеком v203.
 */
class ZXReportValidator {
    private val dateTimeValidator = DateTimeValidator()
    private val moneyValidator = MoneyValidator()
    private val revenueValidator = ZXReportRevenueValidator()
    private val sectionValidator = ZXReportSectionValidator()
    private val operationValidator = ZXReportOperationValidator()
    private val taxValidator = ZXReportTaxValidator()
    private val nonNullableSumValidator = ZXReportNonNullableSumValidator()
    private val ticketOperationValidator = ZXReportTicketOperationValidator()
    private val moneyPlacementValidator = ZXReportMoneyPlacementValidator()

    /**
     * Валидирует ZXReport по ключу в контейнере.
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    fun validate(container: JsonObject, key: String, path: String, reportType: String? = null): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        val zxReport = container[key] as? JsonObject
        if (zxReport == null) {
            errors.add(ValidationUtils.missingField(path))
            return errors
        }

        // Обязательные поля отчета.
        errors.addAll(dateTimeValidator.validate(zxReport, "dateTime", "$path.dateTime"))
        ValidationUtils.requireIntInRange(zxReport, "shiftNumber", 0, Int.MAX_VALUE, "$path.shiftNumber", errors)
        errors.addAll(moneyValidator.validate(zxReport, "cashSum", "$path.cashSum"))
        errors.addAll(revenueValidator.validate(zxReport, "revenue", "$path.revenue"))

        // Итоги по разделам/операциям (опционально).
        errors.addAll(sectionValidator.validateList(zxReport, "sections", "$path.sections"))
        errors.addAll(operationValidator.validateList(zxReport, "operations", "$path.operations"))
        errors.addAll(operationValidator.validateList(zxReport, "discounts", "$path.discounts"))
        errors.addAll(operationValidator.validateList(zxReport, "markups", "$path.markups"))
        errors.addAll(operationValidator.validateList(zxReport, "totalResult", "$path.totalResult"))

        // Налоги (опционально).
        errors.addAll(taxValidator.validateList(zxReport, "taxes", "$path.taxes"))

        // Необнуляемые суммы (опционально).
        errors.addAll(
            nonNullableSumValidator.validateList(
                zxReport,
                "startShiftNonNullableSums",
                "$path.startShiftNonNullableSums"
            )
        )
        errors.addAll(nonNullableSumValidator.validateList(zxReport, "nonNullableSums", "$path.nonNullableSums"))

        // Операции по чекам (опционально).
        errors.addAll(ticketOperationValidator.validateList(zxReport, "ticketOperations", "$path.ticketOperations"))

        // Операции внесения/снятия (опционально).
        errors.addAll(moneyPlacementValidator.validateList(zxReport, "moneyPlacements", "$path.moneyPlacements"))

        // Время открытия смены обязательно для v203.
        errors.addAll(dateTimeValidator.validate(zxReport, "openShiftTime", "$path.openShiftTime"))

        // Время закрытия смены обязательно только для Z-отчета.
        if (reportType == "REPORT_Z") {
            errors.addAll(dateTimeValidator.validate(zxReport, "closeShiftTime", "$path.closeShiftTime"))
        } else if (zxReport["closeShiftTime"] != null) {
            errors.addAll(dateTimeValidator.validate(zxReport, "closeShiftTime", "$path.closeShiftTime"))
        }

        // Контрольная сумма считается автоматически, но если пришла в JSON — проверяем тип.
        if (zxReport["checksum"] != null) {
            ValidationUtils.requireNonBlankString(zxReport, "checksum", "$path.checksum", errors)
        }

        return errors
    }
}
