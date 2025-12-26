package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response

import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.port.Validator
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.enums.ReportTypeEnumValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.zxreport.ZXReportValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.response.service.ServiceResponseValidator
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Валидатор ответа для COMMAND_CLOSE_SHIFT.
 */
class ResponseValidatorCloseShift : Validator {
    private val reportTypeValidator = ReportTypeEnumValidator()
    private val zxReportValidator = ZXReportValidator()
    private val serviceValidator = ServiceResponseValidator()

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
        // Для ответа на COMMAND_CLOSE_SHIFT допустим только REPORT_Z.
        val reportTypeValue = (report["reportType"] as? JsonPrimitive)?.content
        if (reportTypeValue != null && reportTypeValue != "REPORT_Z") {
            errors.add(ValidationUtils.invalidValue("$.payload.report.reportType"))
        }

        val zxReport = report["zxReport"] as? JsonObject
        if (zxReport == null) {
            errors.add(ValidationUtils.missingField("$.payload.report.zxReport"))
        } else {
            errors.addAll(zxReportValidator.validate(report, "zxReport", "$.payload.report.zxReport", null))
        }

        val service = json["service"]
        if (service is JsonObject) {
            errors.addAll(serviceValidator.validate(service, "$.payload.service"))
        } else if (service != null) {
            errors.add(ValidationUtils.invalidType("$.payload.service"))
        }

        return errors
    }
}
