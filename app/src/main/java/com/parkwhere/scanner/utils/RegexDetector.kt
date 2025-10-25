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

    // Pattern for "number-code-number" format (e.g., "29 B2 29")
    // This captures parking codes where the same number appears on both sides
    private val numberCodeNumberRegex = Pattern.compile("^(\\d{1,4})\\s+([A-Z]\\d*F?|\\d+F|[가-힣]|지하\\d+층|P\\d+)\\s+\\1$")

    // Pattern for repeated numbers (e.g., "29 29" when text is very large)
    // This captures cases where only the parking number is visible twice
    private val repeatedNumberRegex = Pattern.compile("^(\\d{1,4})\\s+\\1$")

    // Korean License Plate Regex: e.g., 12가3456, 123가4567
    private val licensePlateRegex = Pattern.compile("\\b(?:\\d{2,3}[가-힣][ -]?\\d{4}|[가-힣]{2}\\d{2}[가-힣][ -]?\\d{4})\\b")

    /**
     * 주차 코드 유효성 검사 함수
     */
    private fun isValidParkingCode(code: String): Boolean {
        // 비어있거나 너무 짧은 경우
        if (code.isEmpty() || code.length < 2) {
            println("❌ 주차 코드 거부: 너무 짧은 코드 - '$code'")
            return false
        }

        // 숫자만 있는 경우는 유효 (반복 숫자 패턴으로 감지된 경우)
        val numbersOnly = code.all { it.isDigit() }
        if (numbersOnly) {
            // 2~4자리 숫자는 유효한 주차 코드
            if (code.length in 2..4) {
                println("✅ 유효한 주차 코드 (숫자만): '$code' (${code.length}자)")
                return true
            } else {
                println("❌ 주차 코드 거부: 숫자만 있지만 길이 부적절 - '$code' (${code.length}자)")
                return false
            }
        }

        // 숫자와 '-'만 있는 경우는 주차 코드가 아님
        val numbersAndDashOnly = code.all { it.isDigit() || it == '-' }
        if (numbersAndDashOnly) {
            println("❌ 주차 코드 거부: 숫자와 '-'만 있는 코드 - '$code'")
            return false
        }

        println("✅ 유효한 주차 코드: '$code' (${code.length}자)")
        return true
    }

    /**
     * 주차 코드 검출 메인 함수
     */
    fun firstMatch(lines: List<String>): String? {
        // Clean up lines by trimming whitespace, converting to uppercase, and removing empty lines.
        val cleanLines = lines.map { it.trim().uppercase() }.filter { it.isNotEmpty() }

        // Check if text is too long (likely not a parking code)
        // If we have many long lines (15+ chars), it's probably not a parking sign
        val longLines = cleanLines.filter { it.length >= 15 }
        if (longLines.size >= 3) {
            println("❌ 긴 텍스트 라인이 너무 많음 (${longLines.size}개, 15자 이상) - 주차 코드가 아닐 가능성 높음")
            println("   샘플: ${longLines.take(3).joinToString(" / ")}")
            return null
        }

        for ((index, line) in cleanLines.withIndex()) {
            // 1. Check for "number-code-number" pattern (e.g., "29 B2 29")
            val numberCodeNumberMatcher = numberCodeNumberRegex.matcher(line)
            if (numberCodeNumberMatcher.matches()) {
                // Extract the middle part (the actual parking code)
                val parkingCode = numberCodeNumberMatcher.group(2)
                if (parkingCode != null) {
                    println("✅ 숫자-코드-숫자 패턴 발견: '$line' -> 주차코드: '$parkingCode'")
                    if (isValidParkingCode(parkingCode)) {
                        return parkingCode
                    }
                }
            }

            // 2. Check for repeated number pattern (e.g., "29 29" - large text)
            val repeatedNumberMatcher = repeatedNumberRegex.matcher(line)
            if (repeatedNumberMatcher.matches()) {
                // Extract the number (first capture group)
                val parkingNumber = repeatedNumberMatcher.group(1)
                if (parkingNumber != null) {
                    println("✅ 반복 숫자 패턴 발견 (큰 글씨): '$line' -> 주차코드: '$parkingNumber'")
                    // Repeated numbers are valid parking codes (2-4 digits)
                    if (parkingNumber.length in 2..4) {
                        return parkingNumber
                    }
                }
            }

            // 3. Check for a full, valid parking code on the current line.
            if (fullRegex.matcher(line).matches()) {
                if (isValidParkingCode(line)) {
                    return line
                }
            }

            // 4. If no full match, check for a two-line pattern (prefix + suffix).
            if (prefixRegex.matcher(line).matches()) {
                // If the current line is a valid prefix, check the next line.
                if (index + 1 < cleanLines.size) {
                    val nextLine = cleanLines[index + 1]

                    // Check if the next line is a valid suffix.
                    if (suffixRegex.matcher(nextLine).matches()) {
                        // If both lines match, combine them with a hyphen.
                        val combinedCode = "$line-$nextLine"

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

        // 5. Try combining multiple consecutive short lines (for large text that gets split)
        // Examples: ["B", "2"] -> "B2", ["2", "F", "2", "7"] -> "2F-27"
        val multiLineCode = tryMultiLineCombination(cleanLines)
        if (multiLineCode != null) {
            println("✅ 멀티라인 조합 성공: '$multiLineCode'")
            return multiLineCode
        }

        // If no match is found after checking all lines and combinations.
        return null
    }

    /**
     * Helper function to try combining multiple consecutive short lines
     */
    private fun tryMultiLineCombination(lines: List<String>): String? {
        // Only try if we have multiple short lines (likely from large text)
        val shortLines = lines.filter { it.length <= 3 && it.isNotEmpty() }

        if (shortLines.size < 2 || shortLines.size > 6) {
            return null
        }

        println("🔍 멀티라인 조합 시도: $shortLines")

        // Try different combinations of consecutive lines
        for (startIndex in shortLines.indices) {
            val maxEndIndex = minOf(startIndex + 4, shortLines.size - 1)

            // Skip if range is invalid
            if (maxEndIndex < startIndex + 1) {
                continue
            }

            for (endIndex in (startIndex + 1)..maxEndIndex) {
                val combinedLines = shortLines.subList(startIndex, endIndex + 1)

                // Try with no separator
                val noSep = combinedLines.joinToString("")
                if (isValidParkingCode(noSep)) {
                    if (fullRegex.matcher(noSep).matches()) {
                        println("✅ 조합 성공 (구분자 없음): $combinedLines -> '$noSep'")
                        return noSep
                    }
                }

                // Try with hyphen between parts (e.g., ["B", "3", "1", "4"] -> "B3-14")
                if (combinedLines.size >= 3) {
                    for (splitPoint in 1 until combinedLines.size) {
                        val prefix = combinedLines.subList(0, splitPoint).joinToString("")
                        val suffix = combinedLines.subList(splitPoint, combinedLines.size).joinToString("")
                        val withHyphen = "$prefix-$suffix"

                        if (isValidParkingCode(withHyphen)) {
                            if (fullRegex.matcher(withHyphen).matches()) {
                                println("✅ 조합 성공 (하이픈): $combinedLines -> '$withHyphen'")
                                return withHyphen
                            }
                        }
                    }
                }
            }
        }

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