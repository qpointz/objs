package org.poc.objs.service.web

import org.poc.objs.core.validation.ValidationIssue
import org.poc.objs.core.validation.ValidationResult
import org.springframework.http.ResponseEntity

/** Shared format query values for registry / graph multi-format I/O. */
object ObjsIoFormats {
    const val SEEDS = "seeds"
    const val JSON_SCHEMA = "json-schema"
    /** Full-catalog JSON Schema with synthetic root for POJO / codegen tools. */
    const val JSON_SCHEMA_CODEGEN = "json-schema-codegen"
    const val YAML_MEDIA_TYPE = "application/yaml"
    const val JSON_SCHEMA_MEDIA_TYPE = "application/schema+json"

    fun unknownFormat(format: String): ResponseEntity<Any> =
        ResponseEntity.badRequest().body(
            ValidationResult.of(
                ValidationIssue(
                    code = "IO_FORMAT_UNSUPPORTED",
                    message = "Unsupported format='$format'",
                    path = "format",
                ),
            ),
        )
}
