package com.parkwhere.scanner.utils

import java.util.regex.Pattern

class RegexDetector {

    // Enhanced pattern for parking facility codes
    // Supports: B3-14, B314, 1F-10, 1F10, 2F 27, 2F27, A1-05, C1-88, B-12, B12, 101 등
    private val fullRegex = Pattern.compile("^(?:[A-Z]\\d*F?|\\d+F|[가-힣]|지하\\d+층|P\\d+)[- ]?\\d{1,4}$|^[A-Z]\\d{1,6}$|^\\d{1,4}$")

    // Enhanced prefix pattern for multi-line codes
    // Examples: "B3", "1F", "2F", "A1", "C1", "B", "가", "지하2층", "P1"
    private val prefixRegex = Pattern.compile("^(?:[A-Z]\\d*F?|\\d+F|[가-힣]|지하\\d+층|P\\d+)$")

    // This pattern identifies a potential second part of a multi-line code (numbers only).
    private val suffixRegex = Pattern.compile("^\\d{1,4}$")

    // Korean License Plate Regex: e.g., 12가3456, 123가4567
    private val licensePlateRegex = Pattern.compile("\\b(?:\\d{2,3}[가-힣][ -]?\\d{4}|[가-힣]{2}\\d{2}[가-힣][ -]?\\d{4})\\b")

    /**
     * 주차 코드 유효성 검사 함수
     */
    private fun isValidParkingCode(code: String): Boolean {
        // 8자 이상은 주차 코드가 아님
        if (code.length >= 8) {
            println("❌ 주차 코드 거부: 너무 긴 코드 (${code.length}자) - '$code'")
            return false
        }

        // 숫자와 '-'만 있는 경우는 주차 코드가 아님
        val numbersAndDashOnly = code.all { it.isDigit() || it == '-' }
        if (numbersAndDashOnly) {
            println("❌ 주차 코드 거부: 숫자와 '-'만 있는 코드 - '$code'")
            return false
        }

        // 비어있거나 너무 짧은 경우
        if (code.isEmpty() || code.length < 2) {
            println("❌ 주차 코드 거부: 너무 짧은 코드 - '$code'")
            return false
        }

        println("✅ 유효한 주차 코드: '$code'")
        return true
    }

    /**
     * 주차 코드 검출 메인 함수
     */
    fun firstMatch(lines: List<String>): String? {
        // Clean up lines by trimming whitespace, converting to uppercase, and removing empty lines.
        val cleanLines = lines.map { it.trim().uppercase() }.filter { it.isNotEmpty() }

        for ((index, line) in cleanLines.withIndex()) {
            // Skip lines that are too long (more than 10 characters)
            if (line.length > 10) continue

            // 1. Check for a full, valid parking code on the current line.
            if (fullRegex.matcher(line).matches()) {
                if (isValidParkingCode(line)) {
                    return line
                }
            }

            // 2. If no full match, check for a two-line pattern (prefix + suffix).
            if (prefixRegex.matcher(line).matches()) {
                // If the current line is a valid prefix, check the next line.
                if (index + 1 < cleanLines.size) {
                    val nextLine = cleanLines[index + 1]

                    // Skip if next line is too long
                    if (nextLine.length > 10) continue

                    // Check if the next line is a valid suffix.
                    if (suffixRegex.matcher(nextLine).matches()) {
                        // If both lines match, combine them with a hyphen.
                        val combinedCode = "$line-$nextLine"

                        // Skip if combined code is too long
                        if (combinedCode.length > 7) continue

                        // Re-validate the combined string against the full pattern to ensure correctness.
                        if (fullRegex.matcher(combinedCode).matches()) {
                            if (isValidParkingCode(combinedCode)) {
                                return combinedCode
                            }
                        }
                    }
                }
            }
        }

        // If no match is found after checking all lines and combinations.
        return null
    }

    /**
     * 번호판 검출 함수
     */
    fun firstLicensePlateMatch(lines: List<String>): String? {
        val cleanLines = lines.map { it.trim() }.filter { it.isNotEmpty() }

        for (line in cleanLines) {
            // Skip lines that are too long (more than 10 characters)
            if (line.length > 10) continue

            val matcher = licensePlateRegex.matcher(line)
            if (matcher.find()) {
                val plateText = matcher.group()
                // 길이 및 유효성 검사
                if (plateText.length in 6..10) {
                    println("✅ 유효한 번호판 발견: '$plateText'")
                    return plateText
                } else {
                    println("❌ 번호판 길이 부적절: '$plateText' (${plateText.length}자, 유효범위: 6-10자)")
                }
            }
        }
        println("❌ 모든 라인 검사 완료 - 번호판을 찾을 수 없음")
        return null
    }

    /**
     * 테스트용 함수: 다양한 주차장 코드 형식 테스트
     */
    fun testParkingCodes() {
        val testCases = listOf(
            // 일반적인 기둥 번호 형식 (유효)
            "B3-14", "1F-10", "2F-33", "A1-05", "C1-88",
            // 공백으로 구분 (유효)
            "2F 27", "B3 14", "A1 05",
            // 붙어있는 형식 (유효)
            "B314", "A105", "C188", "1F10", "2F33",
            // 단순 문자 + 숫자 (유효)
            "B12", "A5", "C99",
            // 한글 포함 (유효)
            "가123", "나456",
            // 지하 주차장 (유효 - 길이 초과로 인해 거부될 수 있음)
            "지하층15",
            // 비유효한 경우
            "101", "205", "88", "1234",  // 숫자와 '-'만 있는 경우
            "12-34", "5-6", "123-456",     // 숫자와 '-'만 있는 경우
            "ABCDEFGH", "12345678",       // 8자 이상
            "B3-", "-14", "", "   "         // 기타 비유효
        )

        println("📝 주차장 코드 정규식 테스트 시작:")

        for (testCase in testCases) {
            val lines = listOf(testCase)
            val result = firstMatch(lines)
            val status = if (result != null) "✅" else "❌"
            println("$status '$testCase' -> ${result ?: "null"}")
        }

        println("\n📝 멀티라인 테스트:")

        val multiLineTests = listOf(
            listOf("B3", "14") to "B3-14",
            listOf("2F", "27") to "2F-27",
            listOf("A1", "05") to "A1-05",
            listOf("C", "88") to "C-88",
            listOf("지하2층", "15") to "지하2층-15"
        )

        for ((lines, expected) in multiLineTests) {
            val result = firstMatch(lines)
            val status = if (result == expected) "✅" else "❌"
            println("$status $lines -> ${result ?: "null"} (expected: $expected)")
        }

        println("📝 주차장 코드 테스트 완료\n")
    }
}