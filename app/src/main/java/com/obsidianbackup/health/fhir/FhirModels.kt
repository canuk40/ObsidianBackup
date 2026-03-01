// health/fhir/FhirModels.kt
package com.obsidianbackup.health.fhir

import kotlinx.serialization.Serializable

/**
 * FHIR R4 Data Models for Health Connect Data Export
 * 
 * Implementation based on HL7 FHIR R4 Specification:
 * https://www.hl7.org/fhir/R4/observation.html
 * 
 * These are lightweight data classes for manual FHIR serialization
 * without the ~5MB overhead of the Android FHIR SDK.
 * 
 * FHIR Resources Implemented:
 * - Bundle: Container for multiple resources
 * - Observation: Measurements and simple assertions (vital signs, activity data)
 * 
 * Reference Standards:
 * - FHIR R4 (v4.0.1)
 * - LOINC codes for observations (loinc.org)
 * - UCUM codes for units (unitsofmeasure.org)
 */

@Serializable
data class FhirBundle(
    val resourceType: String = "Bundle",
    val type: String,
    val timestamp: String,
    val entry: List<FhirBundleEntry>
)

@Serializable
data class FhirBundleEntry(
    val resource: FhirObservation
)

@Serializable
data class FhirObservation(
    val resourceType: String = "Observation",
    val status: String,
    val category: List<FhirCodeableConcept>? = null,
    val code: FhirCodeableConcept,
    val subject: FhirReference? = null,
    val effectiveDateTime: String? = null,
    val effectivePeriod: FhirPeriod? = null,
    val issued: String? = null,
    val valueQuantity: FhirQuantity? = null,
    val valueString: String? = null,
    val note: List<FhirAnnotation>? = null
)

@Serializable
data class FhirCodeableConcept(
    val coding: List<FhirCoding>? = null,
    val text: String? = null
)

@Serializable
data class FhirCoding(
    val system: String,
    val code: String,
    val display: String? = null
)

@Serializable
data class FhirQuantity(
    val value: Double,
    val unit: String? = null,
    val system: String? = null,
    val code: String? = null
)

@Serializable
data class FhirReference(
    val reference: String
)

@Serializable
data class FhirPeriod(
    val start: String? = null,
    val end: String? = null
)

@Serializable
data class FhirAnnotation(
    val text: String
)
