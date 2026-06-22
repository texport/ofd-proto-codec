package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.port.Validator
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.enums.ReportTypeEnumValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.zxreport.ZXReportValidator

/**
 * Валидатор ответа для COMMAND_REPORT.
 */
internal class ResponseValidatorReport : Validator {
    private val reportTypeValidator = ReportTypeEnumValidator()
    private val zxReportValidator = ZXReportValidator()

    /**
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    override fun validate(commandType: CommandType, json: JsonObject): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        val result = json["result"] as? JsonObject
        if (result == null) {
            errors.add(ValidationUtils.missingField("$.payload.result"))
            return errors
        }
        ValidationUtils.requireIntInRange(
            result,
            "resultCode",
            0,
            Int.MAX_VALUE,
            "$.payload.result.resultCode",
            errors
        )

        val resultCodeValue = (result["resultCode"] as? JsonPrimitive)?.intOrNull
        if (resultCodeValue != null && resultCodeValue != 0) {
            return errors
        }

        val report = json["report"] as? JsonObject
        if (report == null) {
            errors.add(ValidationUtils.missingField("$.payload.report"))
            return errors
        }
        errors.addAll(reportTypeValidator.validate(report, "reportType", "$.payload.report.reportType"))
        val zxReport = report["zxReport"] as? JsonObject
        if (zxReport == null) {
            errors.add(ValidationUtils.missingField("$.payload.report.zxReport"))
        } else {
            errors.addAll(zxReportValidator.validate(report, "zxReport", "$.payload.report.zxReport", null))
        }

        return errors
    }
}
