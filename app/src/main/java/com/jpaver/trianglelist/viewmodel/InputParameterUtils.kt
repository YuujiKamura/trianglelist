package com.jpaver.trianglelist.viewmodel

// Utility functions and validation rules extracted from Triangle.kt
// formattedString / spaced_by は common の viewmodel/FormattedString.kt に移動 (editmodel 降ろし)

data class ValidationRule(val falseCondition: (InputParameter) -> Boolean, val message: String) {
    fun isValid(params: InputParameter, showToast: (String, Boolean) -> Unit): Boolean {
        if (falseCondition(params)) {
            showToast(message, false)
            return false
        }
        return true
    }
}

val rules_triangle = listOf(
    ValidationRule({ it.a <= 0.0f || it.b <= 0.0f || it.c <= 0.0f }, "Invalid!! : Negative or zero side length"),
    ValidationRule({ it.a + it.b <= it.c }, "Invalid!! : C > A + B"),
    ValidationRule({ it.b + it.c <= it.a }, "Invalid!! : A > B + C"),
    ValidationRule({ it.c + it.a <= it.b }, "Invalid!! : B > C + A"),
    ValidationRule({ it.pn < 1 && it.number != 1 }, "Invalid!! : number of parent"),
    ValidationRule({ it.pl < 1 && it.number != 1 }, "Invalid!! : connection in parent")
)

