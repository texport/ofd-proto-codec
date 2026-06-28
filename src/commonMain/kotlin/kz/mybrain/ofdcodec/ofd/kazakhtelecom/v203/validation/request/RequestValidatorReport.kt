package kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kz.mybrain.ofdcodec.domain.model.CommandType
import kz.mybrain.ofdcodec.domain.model.ValidationError
import kz.mybrain.ofdcodec.domain.port.Validator
import kz.mybrain.ofdcodec.domain.validation.ValidationUtils
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.DateTimeValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.enums.ReportTypeEnumValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.common.zxreport.ZXReportValidator
import kz.mybrain.ofdcodec.ofd.kazakhtelecom.v203.validation.request.service.ServiceRequestValidator

/**
 * Валидатор запроса для COMMAND_REPORT.
 */
internal class RequestValidatorReport : Validator {
    private val serviceValidator = ServiceRequestValidator()
    private val reportTypeValidator = ReportTypeEnumValidator()
    private val zxReportValidator = ZXReportValidator()
    private val dateTimeValidator = DateTimeValidator()

    /**
     * Проверяет JSON и собирает все ошибки по полям без остановки на первой.
     */
    override fun validate(commandType: CommandType, json: JsonObject): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        // В COMMAND_REPORT сервисная часть обязательна для бизнес-валидаций.
        val service = json["service"] as? JsonObject
        if (service == null) {
            errors.add(ValidationUtils.missingField("$.payload.service"))
            return errors
        }
        errors.addAll(serviceValidator.validate(service, "$.payload.service"))

        // Блок report обязателен.
        val report = json["report"] as? JsonObject
        if (report == null) {
            errors.add(ValidationUtils.missingField("$.payload.report"))
            return errors
        }

        // reportType обязателен и должен быть валидным enum.
        errors.addAll(reportTypeValidator.validate(report, "reportType", "$.payload.report.reportType"))
        val reportTypeValue = (report["reportType"] as? JsonPrimitive)?.content
        // zxReport обязателен для протокола и валидируется с учетом типа отчета.
        errors.addAll(zxReportValidator.validate(report, "zxReport", "$.payload.report.zxReport", reportTypeValue))
        // dateTime обязателен для report.
        errors.addAll(dateTimeValidator.validate(report, "dateTime", "$.payload.report.dateTime"))

        // isOffline опционален, но если есть — boolean.
        val isOffline = report["isOffline"]
        if (isOffline != null && (isOffline !is JsonPrimitive || isOffline.booleanOrNull == null)) {
            errors.add(ValidationUtils.invalidType("$.payload.report.isOffline"))
        }

        return errors
    }
}
