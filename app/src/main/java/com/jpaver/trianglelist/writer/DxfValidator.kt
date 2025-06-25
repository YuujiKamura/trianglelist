package com.jpaver.trianglelist.writer

import java.io.File

/**
 * Very lightweight DXF sanity checker.
 * It performs three inexpensive validations:
 * 1. group-code 5 handles are unique.
 * 2. every group-code 330/340/350 reference points to an existing handle.
 * 3. SECTION order roughly follows HEADER → TABLES → BLOCKS → ENTITIES → OBJECTS.
 *
 * This is **not** a full DXF parser – it is just enough to catch common mistakes
 * that make AutoCAD refuse to open the file.
 */
object DxfValidator {

    data class Result(
        val ok: Boolean,
        val errors: List<String>
    )

    fun validate(file: File): Result {
        val handles = mutableSetOf<String>()
        val referenced = mutableSetOf<String>()
        val errors = mutableListOf<String>()
        val definedLayers = mutableSetOf<String>()
        val definedLtypes = mutableSetOf<String>()
        val definedStyles = mutableSetOf<String>()
        val usedLayers = mutableSetOf<String>()
        val usedLtypes = mutableSetOf<String>()
        val usedStyles = mutableSetOf<String>()
        val sectionOrder = mutableListOf<String>()
        var currentSection: String? = null
        var currentTable: String? = null
        // HEADER vars we care about
        var extMinX: Float? = null
        var extMaxX: Float? = null
        var currentHeaderVar: String? = null
        val entityOwners = mutableSetOf<String>()

        file.bufferedReader().useLines { seq ->
            val itr = seq.iterator()
            while (itr.hasNext()) {
                val codeLine = itr.next().trim()
                if (!itr.hasNext()) break
                val valueLine = itr.next().trim()
                val code = codeLine.toIntOrNull() ?: continue

                when (code) {
                    0 -> {
                        if (valueLine == "SECTION") {
                            // next 2/line holds section name later, we peek ahead cheaply
                            if (itr.hasNext()) {
                                val c2 = itr.next().trim()
                                val v2 = if (itr.hasNext()) itr.next().trim() else ""
                                if (c2 == "2") {
                                    currentSection = v2
                                    sectionOrder.add(v2)
                                    currentTable = null
                                }
                            }
                        } else if (currentSection == "TABLES") {
                            when (valueLine) {
                                "TABLE" -> {
                                    // next code/value is 2/<name>
                                    if (itr.hasNext()) {
                                        val c2 = itr.next().trim()
                                        val v2 = if (itr.hasNext()) itr.next().trim() else ""
                                        if (c2 == "2") currentTable = v2
                                    }
                                }
                                "ENDTAB" -> currentTable = null
                                else -> {
                                    // record start inside a table
                                    when (currentTable) {
                                        "LAYER", "LTYPE", "STYLE" -> {
                                            // read ahead until we find group 2 = name
                                            var recName: String? = null
                                            var innerIter = 0
                                            // limited to avoid infinite loop
                                            while (itr.hasNext() && innerIter < 10) {
                                                val cN = itr.next().trim()
                                                val vN = if (itr.hasNext()) itr.next().trim() else ""
                                                innerIter++
                                                if (cN == "2") { recName = vN; break }
                                            }
                                            recName?.let {
                                                when (currentTable) {
                                                    "LAYER" -> definedLayers.add(it)
                                                    "LTYPE" -> definedLtypes.add(it)
                                                    "STYLE" -> definedStyles.add(it)
                                                    else -> {}
                                                }
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                    9 -> if (currentSection == "HEADER") {
                        currentHeaderVar = valueLine
                    }
                    5 -> {
                        // Skip handles appearing in HEADER (e.g., $HANDSEED); these values are
                        // not part of the object handle namespace and can legally duplicate
                        // real entity/table/object handles.
                        if (currentSection != "HEADER") {
                            if (!handles.add(valueLine)) {
                                errors.add("Duplicate handle 5 $valueLine")
                            }
                        }
                    }
                    330, 340, 350 -> {
                        referenced.add(valueLine)
                        if (currentSection == "ENTITIES" && code == 330) {
                            entityOwners.add(valueLine)
                        }
                    }
                    8 -> if (currentSection == "ENTITIES") usedLayers.add(valueLine)
                    6 -> if (currentSection == "ENTITIES") usedLtypes.add(valueLine)
                    7 -> if (currentSection == "ENTITIES") usedStyles.add(valueLine)
                    10 -> if (currentSection == "HEADER") {
                        when (currentHeaderVar) {
                            "${'$'}EXTMIN" -> extMinX = valueLine.toFloatOrNull()
                            "${'$'}EXTMAX" -> extMaxX = valueLine.toFloatOrNull()
                            else -> {}
                        }
                    }
                }
            }
        }

        // refs check
        referenced.filter { it !in handles }.forEach {
            errors.add("Reference handle $it not present in file (via 330/340/350)")
        }

        // section order – very lenient check
        val expected = listOf("HEADER", "TABLES", "BLOCKS", "ENTITIES", "OBJECTS")
        val mismatch = expected.filter { sec -> sectionOrder.indexOf(sec) < expected.indexOf(sec) }
        if (mismatch.isNotEmpty()) {
            errors.add("Section order seems incorrect: $sectionOrder")
        }

        // symbol table references check
        (usedLayers - definedLayers).forEach { errors.add("Layer '$it' referenced but not defined") }
        (usedLtypes - definedLtypes).forEach { errors.add("Linetype '$it' referenced but not defined") }
        (usedStyles - definedStyles).forEach { errors.add("TextStyle '$it' referenced but not defined") }

        // OWNER 330 in ENTITIES should be Model or Paper block record (36/32)
        val allowedOwners = setOf("36", "32")
        (entityOwners - allowedOwners).forEach {
            errors.add("Entity owner 330 '$it' is not *Model_Space(36) or *Paper_Space(32)")
        }

        // Check extMinX/extMaxX consistency
        if (extMinX != null && extMaxX != null) {
            if (extMinX!! >= extMaxX!!) {
                errors.add("EXTMIN / EXTMAX values are not coherent: min >= max")
            }
        }

        // -------------------------------------------------
        // Low-level structural checks (line pairs, EOF, CRLF)
        // -------------------------------------------------
        errors.addAll(DxfLowLevelChecks.run(file))

        return Result(errors.isEmpty(), errors)
    }
} 