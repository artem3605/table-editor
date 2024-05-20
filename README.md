# Table Editor

This is a Kotlin-based table editor which supports the basic operations and functions. Also it supports a mechanism for
recalculating cell values in a table based on dependencies between cells.

## How the parser works

My parser has two parts: the tokenizer and the evaluator. The tokenizer is responsible for converting the input string
into a list of tokens. The evaluator is responsible for evaluating the tokens and returning the result. In my
implementation the following operators are supported: +, -, *, /. Also the following functions are supported: sum, min,
max, pow. But it is easy to add more functions and operators. To do it you need just to write logic for the new
operator/function.

## Cell dependencies

Throughout the program's operation, I maintain a dependency graph of cells. When a cell is updated, I update all cells
that depend on it. Also, when there is some cycle in the graph, cells that are part of the cycle consists some "random"
values (not errors).