import javax.swing.*
import java.awt.Component


val editor = object : DefaultCellEditor(JTextField()) {
    override fun getTableCellEditorComponent(
        table: JTable,
        value: Any,
        isSelected: Boolean,
        row: Int,
        column: Int
    ): Component {
        val model = table.model as FormulaTableModel
        super.getCellEditorValue() // get the current value of the cell editor
        return super.getTableCellEditorComponent(table, model.getFormulaAt(row, column), isSelected, row, column)
    }
}


fun createAndShowGUI() {
    val frame = JFrame("Table Editor")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE

    val initialColumnNames = arrayOf("№", "A", "B", "C")

    val model = object : FormulaTableModel(3, initialColumnNames.size) {
        override fun isCellEditable(row: Int, column: Int): Boolean {
            return column != 0
        }
    }
    model.setColumnIdentifiers(initialColumnNames)

    for (i in 0 until model.rowCount) {
        model.setValueAt((i + 1).toString(), i, 0)
    }

    val table = JTable(model)

    for (column in 1 until table.columnModel.columnCount) {
        table.columnModel.getColumn(column).cellEditor = editor
    }

    val scrollPane = JScrollPane(table)

    val panel = JPanel()
    val addColumnButton = JButton("Add column")
    val removeColumnButton = JButton("Remove column")
    val addRowButton = JButton("Add row")
    val removeRowButton = JButton("Remove row")
    panel.add(addColumnButton)
    panel.add(removeColumnButton)
    panel.add(addRowButton)
    panel.add(removeRowButton)

    addColumnButton.addActionListener {
        val columnCount = model.columnCount
        if (columnCount == 27){
            JOptionPane.showMessageDialog(frame, "Impossible to add column. Maximum number of columns is 26.")
            return@addActionListener
        }
        val columnName = ('A' + columnCount - 1).toString()
        model.addColumn(columnName)
        model.formulas.forEach { it.add("") }
        model.results.forEach { it.add("") }
        for (column in 1 until model.columnCount) {
            table.columnModel.getColumn(column).cellEditor = editor
        }

    }

    removeColumnButton.addActionListener {
        val columnCount = model.columnCount
        if (columnCount > 1) {
            model.setColumnCount(columnCount - 1)
        }
    }

    addRowButton.addActionListener {
        val rowCount = model.rowCount
        model.formulas.add(MutableList(model.columnCount) { "" })
        model.results.add(MutableList(model.columnCount) { "" })
        model.addRow(arrayOfNulls<Any>(model.columnCount))
        model.setValueAt((rowCount + 1).toString(), rowCount, 0)


    }

    removeRowButton.addActionListener {
        val rowCount = model.rowCount
        if (rowCount > 0) {
            model.removeRow(rowCount - 1)

            for (i in 0 until model.rowCount) {
                model.setValueAt((i + 1).toString(), i, 0)
            }

            model.formulas.removeAt(rowCount - 1)
            model.results.removeAt(rowCount - 1)
        } else {
            JOptionPane.showMessageDialog(frame, "Impossible to remove row.")
        }
    }
    frame.layout = BoxLayout(frame.contentPane, BoxLayout.Y_AXIS)
    frame.add(scrollPane)
    frame.add(panel)

    frame.setSize(800, 600)
    frame.isVisible = true
}

fun main() {
    SwingUtilities.invokeLater {
        createAndShowGUI()
    }
}
