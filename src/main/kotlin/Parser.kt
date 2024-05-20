import java.lang.Double.max
import java.lang.Double.min
import kotlin.math.pow

val operators = listOf('+', '-', '*', '/')
val functions = listOf("sum", "max", "min", "pow")

val priorityOfOperators = mapOf('+' to 1, '-' to 1, '*' to 2, '/' to 2)


sealed class Token {
    data class NumberToken(val value: Double) : Token()
    data class CellToken(val cell: String) : Token()
    data class FunctionToken(val function: String, val leftTokens: List<Token>, val rightTokens: List<Token>) : Token()
    data class OperatorToken(val operator: Char) : Token()
    data object LeftParenthesisToken : Token()
    data object RightParenthesisToken : Token()
}

fun tokenize(formula: String): List<Token>? {
    val tokens = mutableListOf<Token>()
    var toTokenize = formula
    while (toTokenize.isNotEmpty()) {
        when {
            toTokenize.startsWith(' ') -> {
                toTokenize = toTokenize.substring(1)
            }

            toTokenize.startsWith('(') -> {
                tokens.add(Token.LeftParenthesisToken)
                toTokenize = toTokenize.substring(1)
            }

            toTokenize.startsWith(')') -> {
                tokens.add(Token.RightParenthesisToken)
                toTokenize = toTokenize.substring(1)
            }

            toTokenize.first() in operators -> {
                tokens.add(Token.OperatorToken(toTokenize.first()))
                toTokenize = toTokenize.substring(1)
            }

            toTokenize.first().isDigit() -> {
                val number = toTokenize.takeWhile { it.isDigit() || it == '.' }
                if (number.count { it == '.' } > 1) return null
                tokens.add(Token.NumberToken(number.toDouble()))
                toTokenize = toTokenize.substring(number.length)
            }

            toTokenize.first().isLetter() -> {
                val str = toTokenize.takeWhile { it.isLetter() || it.isDigit() }
                if (str in functions) {
                    val leftParenthesis = toTokenize.indexOf('(')
                    if (leftParenthesis == -1 || leftParenthesis != str.length) return null
                    var right = leftParenthesis + 1
                    var sep = -1
                    var bal = 1
                    while (bal > 0 && right < toTokenize.length) {
                        if (toTokenize[right] == '(') bal++
                        if (toTokenize[right] == ')') bal--
                        if (toTokenize[right] == ',' && bal == 1) sep = right
                        right++
                    }
                    if (sep == -1) return null
                    tokens.add(
                        Token.FunctionToken(
                            str,
                            tokenize(toTokenize.substring(leftParenthesis + 1, sep)) ?: return null,
                            tokenize(toTokenize.substring(sep + 1, right - 1)) ?: return null
                        )
                    )
                    toTokenize = toTokenize.substring(right)
                } else {
                    if (str[0].isLetter() && str[0].isUpperCase() && str.length > 1 && str.substring(1).toIntOrNull() != null){
                        tokens.add(Token.CellToken(str))
                        toTokenize = toTokenize.substring(str.length)
                    } else {
                        return null
                    }

                }
            }

            else -> return null
        }
    }
    return tokens

}

private fun applyFunction(token: Token.FunctionToken, cells: MutableMap<String, Double?>): Double? {
    val left = evaluate(token.leftTokens, cells)
    val right = evaluate(token.rightTokens, cells)
    if (left == null || right == null) return null
    return when (token.function) {
        "sum" -> left + right
        "max" -> max(left, right)
        "min" -> min(left, right)
        "pow" -> left.pow(right)
        else -> null
    }
}

fun evaluate(tokens: List<Token>, cells: MutableMap<String, Double?>): Double? {
    if (tokens.isEmpty()) return 0.0
    if (tokens.first() == Token.LeftParenthesisToken && tokens.last() == Token.RightParenthesisToken) {
        return evaluate(tokens.subList(1, tokens.size - 1), cells)
    }
    if (tokens.size == 1) {
        return when (val token = tokens.first()) {
            is Token.NumberToken -> token.value
            is Token.CellToken -> cells[token.cell]
            is Token.FunctionToken -> applyFunction(token, cells)
            else -> null
        }
    }
    if (tokens.size == 2) {
        return when (val token = tokens.first()) {
            is Token.OperatorToken -> {
                if (token.operator == '-') {
                    -evaluate(tokens.subList(1, tokens.size), cells)!!
                } else {
                    null
                }
            }

            else -> null
        }
    }
    var levelOfNesting = 0
    var indexOfOperator = -1
    var minPriority = Int.MAX_VALUE
    for (i in tokens.indices) {
        if (tokens[i] == Token.LeftParenthesisToken) {
            levelOfNesting++
        }
        if (tokens[i] == Token.RightParenthesisToken) {
            levelOfNesting--
        }
        if (levelOfNesting == 0 && tokens[i] is Token.OperatorToken) {
            val priority = priorityOfOperators[(tokens[i] as Token.OperatorToken).operator]
            if (priority != null) {
                if (priority <= minPriority && i != 0 && tokens[i - 1] != Token.LeftParenthesisToken) {
                    minPriority = priority
                    indexOfOperator = i
                }
            }
        }
    }
    if (indexOfOperator == -1 || indexOfOperator == tokens.size - 1) {
        if (tokens[0] == Token.OperatorToken('-')) {
            return -evaluate(tokens.subList(1, tokens.size), cells)!!
        }
        return null
    }
    val left = evaluate(tokens.subList(0, indexOfOperator), cells)
    val right = evaluate(tokens.subList(indexOfOperator + 1, tokens.size), cells)
    if (left == null || right == null) {
        return null
    }
    return when (val operator = tokens[indexOfOperator]) {
        is Token.OperatorToken -> {
            when (operator.operator) {
                '+' -> left + right
                '-' -> left - right
                '*' -> left * right
                '/' -> left / right
                else -> null
            }
        }

        else -> null
    }

}
