import javax.swing.table.DefaultTableModel


open class FormulaTableModel(rowCount: Int, columnCount: Int) : DefaultTableModel(rowCount, columnCount) {
    var formulas = MutableList(rowCount) { MutableList(columnCount) { "" } }
    var results = MutableList(rowCount) { MutableList(columnCount) { "" } }
    private var affectsTo = mutableMapOf<String, MutableSet<String>>()
    private var dependsFrom = mutableMapOf<String, MutableSet<String>>()
    override fun getValueAt(row: Int, column: Int): Any {
        return results[row][column]
    }

    private fun findAffectedCells(cell: String): MutableSet<String> {
        val affectedCells = mutableSetOf<String>()
        val stack = mutableListOf(cell)
        while (stack.isNotEmpty()) {
            val currentCell = stack.removeAt(stack.size - 1)
            if (affectsTo.containsKey(currentCell)) {
                for (affectedCell in affectsTo[currentCell]!!) {
                    if (!affectedCells.contains(affectedCell)) {
                        stack.add(affectedCell)
                        affectedCells.add(affectedCell)
                    }
                }
            }
        }
        return affectedCells
    }

    private fun recalculate(cell: String, recalculatedCells: MutableSet<String>, visitedCells: MutableSet<String> = mutableSetOf()) {
        if (cell in recalculatedCells) {
            return
        }
        if (cell in visitedCells) {
            return
        }
        visitedCells.add(cell)
        if (dependsFrom.containsKey(cell)) {
            for (dependentCell in dependsFrom[cell]!!) {
                recalculate(dependentCell, recalculatedCells, visitedCells)
            }
        }
        val row = cell.substring(1).toInt() - 1
        val column = cell[0] - 'A' + 1
        recalculatedCells.add(cell)
        if (row > results.size || column > results[0].size) {
            return
        }
        results[row][column] = calculateResult(formulas[row][column])
        fireTableCellUpdated(row, column)
    }

    override fun setValueAt(value: Any, row: Int, column: Int) {
        formulas[row][column] = value.toString()
        if (column == 0) {
            results[row][column] = value.toString()
            return
        }
        results[row][column] = calculateResult(value.toString())

        val columnName = getColumnName(column)
        val cell = columnName + (row + 1).toString()
        if (dependsFrom.containsKey(cell)) {
            for (affectingCell in dependsFrom[cell]!!) {
                affectsTo[affectingCell]!!.remove(cell)
            }
        }
        val tokens: List<Token>? = tokenize(value.toString())
        if (tokens == null) {
            dependsFrom[cell] = mutableSetOf()
        } else {
            val cells = getCells(tokenize(value.toString())!!).keys.toMutableSet()
            dependsFrom[cell] = cells
            for (dependentCell in cells) {
                if (!affectsTo.containsKey(dependentCell)) {
                    affectsTo[dependentCell] = mutableSetOf()
                }
                affectsTo[dependentCell]!!.add(cell)
            }
        }

        val affectedCells = findAffectedCells(cell)
        val recalculatedCells = mutableSetOf<String>()
        for (affectedCell in affectedCells) {
            recalculate(affectedCell, recalculatedCells)
        }
    }

    fun getFormulaAt(row: Int, column: Int): String {
        return formulas[row][column]
    }

    private fun getCells(tokens: List<Token>): MutableMap<String, Double?> {
        val cells = mutableMapOf<String, Double?>()
        for (token in tokens) {
            if (token is Token.CellToken) {
                val cell = token.cell
                val column = cell[0] - 'A' + 1
                val row = cell.substring(1).toInt() - 1
                if (row >= results.size || column >= results[0].size) {
                    cells[cell] = null
                    continue
                }
                cells[cell] = results[row][column].toDoubleOrNull()
            }
            if (token is Token.FunctionToken) {
                val resLeft = getCells(token.leftTokens)
                val resRight = getCells(token.rightTokens)
                cells.putAll(resLeft)
                cells.putAll(resRight)

            }
        }
        return cells
    }

    private fun calculateResult(formula: String): String {
        val tokens = tokenize(formula) ?: return "ERROR"
        val cells = getCells(tokens)
        val result = evaluate(tokens, cells) ?: return "ERROR"
        return result.toString()
    }
}
