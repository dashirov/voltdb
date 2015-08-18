/* This file is part of VoltDB.
 * Copyright (C) 2008-2015 VoltDB Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */
 package org.voltdb.sqlparser.syntax;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.voltdb.sqlparser.syntax.grammar.ICatalogAdapter;
import org.voltdb.sqlparser.syntax.grammar.IInsertStatement;
import org.voltdb.sqlparser.syntax.grammar.ISemantino;
import org.voltdb.sqlparser.syntax.grammar.IOperator;
import org.voltdb.sqlparser.syntax.grammar.ISelectQuery;
import org.voltdb.sqlparser.syntax.grammar.SQLParserBaseListener;
import org.voltdb.sqlparser.syntax.grammar.SQLParserParser;
import org.voltdb.sqlparser.syntax.grammar.SQLParserParser.Column_nameContext;
import org.voltdb.sqlparser.syntax.grammar.SQLParserParser.ValueContext;
import org.voltdb.sqlparser.syntax.symtab.IColumn;
import org.voltdb.sqlparser.syntax.symtab.IExpressionParser;
import org.voltdb.sqlparser.syntax.symtab.IParserFactory;
import org.voltdb.sqlparser.syntax.symtab.ISymbolTable;
import org.voltdb.sqlparser.syntax.symtab.ITable;
import org.voltdb.sqlparser.syntax.symtab.IType;
import org.voltdb.sqlparser.syntax.util.ErrorMessage;
import org.voltdb.sqlparser.syntax.util.ErrorMessageSet;
/**
 * Objects of this class can be used to walk an Antlr4 parse
 * tree.  See <code>SQLParserDriver</code> for it's only intended
 * use.
 *
 * @author bwhite
 *
 */
public class VoltSQLlistener extends SQLParserBaseListener implements ANTLRErrorListener {
    private static final int DEFAULT_STRING_SIZE = 64;
    private List<IExpressionParser> m_expressionStack = new ArrayList<IExpressionParser>();
    private ITable m_currentlyCreatedTable = null;
    private ISymbolTable m_symbolTable;
    private IParserFactory m_factory;
    private ICatalogAdapter m_catalog;
    private ErrorMessageSet m_errorMessages;
    private ISelectQuery m_selectQuery = null;
    private IInsertStatement m_insertStatement = null;
    private String m_defaultValue;
    private boolean m_hasDefaultValue;
    private boolean m_isPrimaryKey;
    private boolean m_isUniqueConstraint;
    private boolean m_isNullable;
    private boolean m_isNull;

    public VoltSQLlistener(IParserFactory aFactory) {
        m_factory = aFactory;
        m_symbolTable = aFactory.getStandardPrelude();
        m_catalog = aFactory.getCatalog();
        m_selectQuery = null;
        m_insertStatement = null;
        m_errorMessages = aFactory.getErrorMessages();
    }

    public boolean hasErrors() {
        return m_errorMessages.numberErrors() > 0;
    }

    private final void addError(int line, int col, String errorMessageFormat, Object ... args) {
        m_errorMessages.addError(line, col, errorMessageFormat, args);
    }

    private final void addWarning(int line, int col, String errorMessageFormat, Object ... args) {
        m_errorMessages.addWarning(line, col, errorMessageFormat, args);
    }

    public final ErrorMessageSet getErrorMessages() {
        return m_errorMessages;
    }

    public String getErrorMessagesAsString() {
        StringBuffer sb = new StringBuffer();
        int nerrs = getErrorMessages().size();
        sb.append(String.format("\nOh, dear, there seem%s to be %serror%s here.\n",
                                nerrs > 1 ? "" : "s",
                                nerrs > 1 ? "" : "an ",
                                nerrs > 1 ? "s" : ""));
        for (ErrorMessage em : getErrorMessages()) {
            sb.append(String.format("line %d, column %d: %s: %s\n",
                                    em.getLine(),
                                    em.getCol(),
                                    em.getSeverity(),
                                    em.getMsg()));
        }
        return sb.toString();
    }

    @Override public void enterColumn_definition(SQLParserParser.Column_definitionContext ctx) {
        m_defaultValue       = null;
        m_hasDefaultValue    = false;
        m_isPrimaryKey       = false;
        m_isUniqueConstraint = false;
        m_isNullable         = true;
        m_isNull             = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override public void exitColumn_definition(SQLParserParser.Column_definitionContext ctx) {
        String colName = ctx.column_name().IDENTIFIER().getText();
        String type = ctx.type_expression().type_name().IDENTIFIER().getText();
        int nparms, p1 = -1, p2 = -1;
        nparms = ctx.type_expression().NUMBER().size();
        if (nparms > 0) {
            p1 = Integer.parseInt(ctx.type_expression().NUMBER().get(0).getText());
            if (nparms > 1) {
                p2 = Integer.parseInt(ctx.type_expression().NUMBER().get(1).getText());
            }
        }
        IType colType = m_symbolTable.getType(type);
        if (colType == null) {
            addError(ctx.start.getLine(), ctx.start.getCharPositionInLine(), "Type expected");
            colType = m_factory.getErrorType();
        } else if (colType.getTypeKind().isFixedPoint()) {
            /*
             * Warn if we get scale and precision arguments.  We
             * just ignore them.
             */
            if (nparms > 0) {
                addWarning(ctx.start.getLine(),
                           ctx.start.getCharPositionInLine(),
                           "The type %s has a fixed scale and precision.  These arguments will be ignored.",
                           colType.getName().toUpperCase());
            }
        } else if (colType instanceof IStringType) {
            int size;
            /*
             * We should get zero or one argument.  It is an
             * error otherwise.
             */
            if (nparms == 0) {
                size = DEFAULT_STRING_SIZE;
            } else if (nparms == 1) {
                size = p1;
                colType = ((IStringType) colType).makeInstance(size);
            } else {
                addError(ctx.start.getLine(),
                         ctx.start.getCharPositionInLine(),
                         "The string type %s takes only one size parameter.",
                         colType.getName().toUpperCase());
                colType = m_factory.getErrorType();
            }
        } else if (nparms > 0) {
            addError(ctx.start.getLine(),
                     ctx.start.getCharPositionInLine(),
                     "The type %s takes no type parameters.",
                     colType.getName().toUpperCase());
            colType = m_factory.getErrorType();
        }
        IColumn column = m_factory.newColumn(colName, colType);
        column.setHasDefaultValue(m_hasDefaultValue);
        column.setDefaultValue(m_defaultValue);
        column.setIsPrimaryKey(m_isPrimaryKey);
        column.setIsUniqueConstraint(m_isUniqueConstraint);
        column.setIsNullable(m_isNullable);
        column.setIsNull(m_isNull);
        m_currentlyCreatedTable.addColumn(colName, column);
    }

    /**
     * {@inheritDoc}
     */
    @Override public void exitNullableColumnAttribute(org.voltdb.sqlparser.syntax.grammar.SQLParserParser.NullableColumnAttributeContext ctx) {
        m_isNullable = (ctx.NOT() == null);
    };

    /**
     * {@inheritDoc}
     */
    @Override public void exitDefaultValueColumnAttribute(org.voltdb.sqlparser.syntax.grammar.SQLParserParser.DefaultValueColumnAttributeContext ctx) {
        m_hasDefaultValue = true;
        m_defaultValue = ctx.literal_value().getText();
    };

    /**
     * {@inheritDoc}
     */
    @Override public void exitPrimaryKeyColumnAttribute(org.voltdb.sqlparser.syntax.grammar.SQLParserParser.PrimaryKeyColumnAttributeContext ctx) {
        m_isPrimaryKey = true;
    };

    /**
     * {@inheritDoc}
     */
    @Override public void exitUniqueColumnAttribute(org.voltdb.sqlparser.syntax.grammar.SQLParserParser.UniqueColumnAttributeContext ctx) {
        m_isUniqueConstraint = true;
    };

    /**
     * {@inheritDoc}
     */
    @Override public void enterCreate_table_statement(SQLParserParser.Create_table_statementContext ctx) {
        String tableName = ctx.table_name().IDENTIFIER().getText();
        m_currentlyCreatedTable = m_factory.newTable(tableName);
    }
    /**
     * {@inheritDoc}
     */
    @Override public void exitCreate_table_statement(SQLParserParser.Create_table_statementContext ctx) {
        m_catalog.addTable(m_currentlyCreatedTable);
        m_currentlyCreatedTable = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public void enterSelect_statement(SQLParserParser.Select_statementContext ctx) {
        m_selectQuery = m_factory.newSelectQuery(m_symbolTable,
                                                 ctx.stop.getLine(),
                                                 ctx.stop.getCharPositionInLine());
    }

    /**
     * {@inheritDoc}
     */
    @Override public void exitSelect_statement(SQLParserParser.Select_statementContext ctx) {
        if (m_selectQuery.validate()) {
            m_factory.processQuery(m_selectQuery);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public void exitProjection(SQLParserParser.ProjectionContext ctx) {
        if (ctx.STAR() != null) {
            m_selectQuery.addProjection(ctx.STAR().getSymbol().getLine(),
                                        ctx.STAR().getSymbol().getCharPositionInLine());
        } else {
            String tableName = null;
            String columnName = ctx.projection_ref().column_name().IDENTIFIER().getText();
            String alias = null;
            if (ctx.projection_ref().table_name() != null) {
                tableName = ctx.projection_ref().table_name().IDENTIFIER().getText();
            }
            if (ctx.column_name() != null) {
                alias = ctx.column_name().IDENTIFIER().getText();
            }
            m_selectQuery.addProjection(tableName,
                                        columnName,
                                        alias,
                                        ctx.start.getLine(),
                                        ctx.start.getCharPositionInLine());
        }
    }
    /**
     * {@inheritDoc}
     *
     */
    @Override public void exitTable_clause(SQLParserParser.Table_clauseContext ctx) {
        for (SQLParserParser.Table_refContext tr : ctx.table_ref()) {
            String tableName = tr.table_name().get(0).IDENTIFIER().getText();
            String alias = tableName;
            if (tr.table_name().size() > 1) {
                alias = tr.table_name().get(1).IDENTIFIER().getText();
            }
            ITable table = m_catalog.getTableByName(tableName);
            if (table == null) {
                addError(tr.start.getLine(),
                         tr.start.getCharPositionInLine(),
                         "Cannot find table %s",
                         tableName);
            } else {
                m_selectQuery.addTable(table, alias);
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>The default implementation does nothing.</p>
     */
    @Override public void enterWhere_clause(SQLParserParser.Where_clauseContext ctx) {
        IExpressionParser expr = m_factory.makeExpressionParser(m_selectQuery.getTables());
        m_expressionStack.add(expr);
        m_selectQuery.setExpressionParser(expr);
    }
    /**
     * {@inheritDoc}
     */
    @Override public void exitWhere_clause(SQLParserParser.Where_clauseContext ctx) {
        assert(m_expressionStack.size() > 0);
        IExpressionParser expr = m_expressionStack.remove(m_expressionStack.size()-1);
        assert(expr == m_selectQuery.getExpressionParser());
        ISemantino ret = expr.popSemantino();
        if (!(ret != null && ret.getType().isBooleanType())) {
                addError(ctx.start.getLine(),
                        ctx.start.getCharPositionInLine(),
                        "Boolean expression expected");
        } else {
                // Push where statement, select knows if where exists and can pop it off if it does.
                m_selectQuery.setWhereCondition(ret);
        }
        m_selectQuery.setExpressionParser(null);
    }

    private void binOp(String opString, int lineno, int colno) {
        IOperator op = m_factory.getExpressionOperator(opString);
        if (op == null) {
            addError(lineno, colno,
                     "Unknown operator \"%s\"",
                     opString);
            return;
        }
        //
        // Now, given the kind of operation, calculate the output.
        //
        ISemantino rightoperand = (ISemantino) m_selectQuery.popSemantino();
        ISemantino leftoperand = (ISemantino) m_selectQuery.popSemantino();
        ISemantino answer;
        if (op.isArithmetic()) {
            answer = getTopExpressionParser().getSemantinoMath(op,
                                                              leftoperand,
                                                              rightoperand);
        } else if (op.isRelational()) {
            answer = getTopExpressionParser().getSemantinoCompare(op,
                                                                 leftoperand,
                                                                 rightoperand);
        } else if (op.isBoolean()) {
            answer = getTopExpressionParser().getSemantinoBoolean(op,
                                                                 leftoperand,
                                                                 rightoperand);
        } else {
            addError(lineno, colno,
                    "Internal Error: Unknown operation kind for operator \"%s\"",
                    opString);
            return;
        }
        if (answer == null) {
            addError(lineno, colno,
                     "Incompatible argument types %s and %s",
                     leftoperand.getType().getName(),
                     rightoperand.getType().getName());
            return;
        }
        getTopExpressionParser().pushSemantino(answer);
    }
    /**
     * {@inheritDoc}
     *
     * <p>Combine two Semantinos with a product op.</p>
     */
    @Override public void exitTimes_expr(SQLParserParser.Times_exprContext ctx) {
        binOp(ctx.op.start.getText(),
              ctx.op.start.getLine(),
              ctx.op.start.getCharPositionInLine());
    }
    /**
     * {@inheritDoc}
     *
     * <p>Combine two Semantinos with an add op.</p>
     */
    @Override public void exitAdd_expr(SQLParserParser.Add_exprContext ctx) {
        binOp(ctx.op.start.getText(),
              ctx.op.start.getLine(),
              ctx.op.start.getCharPositionInLine());
    }
    /**
     * {@inheritDoc}
     *
     * <p>Combine two Semantinos with a relational op.</p>
     */
    @Override public void exitRel_expr(SQLParserParser.Rel_exprContext ctx) {
        binOp(ctx.op.start.getText(),
              ctx.op.start.getLine(),
              ctx.op.start.getCharPositionInLine());
    }
    /**
     * {@inheritDoc}
     */
    @Override public void exitBool_expr(SQLParserParser.Bool_exprContext ctx) {
        binOp(ctx.op.start.getText(),
              ctx.op.start.getLine(),
              ctx.op.start.getCharPositionInLine());
    }
    /**
     * {@inheritDoc}
     *
     * <p>Push a true semantino</p>
     */
    @Override public void exitTrue_expr(SQLParserParser.True_exprContext ctx) {
        IType boolType = m_factory.getBooleanType();
        getTopExpressionParser().pushSemantino(getTopExpressionParser().getConstantSemantino(Boolean.valueOf(true), boolType));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Push a False Semantino.</p>
     */
    @Override public void exitFalse_expr(SQLParserParser.False_exprContext ctx) {
        IType boolType = m_factory.getBooleanType();
        m_selectQuery.pushSemantino(getTopExpressionParser().getConstantSemantino(Boolean.valueOf(false), boolType));
    }

    /**
     * {@inheritDoc}
     */
    @Override public void exitColref_expr(SQLParserParser.Colref_exprContext ctx) {
        SQLParserParser.Column_refContext crctx = ctx.column_ref();
        String tableName = (crctx.table_name() != null) ? crctx.table_name().IDENTIFIER().getText() : null;
        String columnName = crctx.column_name().IDENTIFIER().getText();
        ISemantino crefSemantino = getTopExpressionParser().getColumnSemantino(columnName, tableName);
        getTopExpressionParser().pushSemantino(crefSemantino);
    }
    /**
     * {@inheritDoc}
     */
    @Override public void exitNumeric_expr(SQLParserParser.Numeric_exprContext ctx) {
        IType intType = m_symbolTable.getType("integer");
        getTopExpressionParser().pushSemantino(getTopExpressionParser().getConstantSemantino(Integer.valueOf(ctx.NUMBER().getText()),
                                                                     intType));
    }

    /**
     * {@inheritDoc}
     */
    @Override public void exitInsert_statement(SQLParserParser.Insert_statementContext ctx) {
        String tableName = ctx.table_name().IDENTIFIER().getText();
        ITable table = m_catalog.getTableByName(tableName);
        if (table == null) {
            addError(ctx.table_name().start.getLine(),
                     ctx.table_name().start.getCharPositionInLine(),
                     "Undefined table name %s",
                     tableName);
            return;
        }
        /*
         * Calculate names and values.  Don't do any semantic checking here.
         * We'll do it all later.
         */
        if (ctx.values() == null) {
            addError(ctx.start.getLine(),
                     ctx.start.getCharPositionInLine(),
                     "No values specified.");
        }
        List<IColumnIdent> columns = new ArrayList<IColumnIdent>();
        if (ctx.column_name_list() != null) {
            for (Column_nameContext cnctx : ctx.column_name_list().column_name()) {
                String colName = cnctx.IDENTIFIER().getText();
                int colLineNo = cnctx.start.getLine();
                int colColNo = cnctx.start.getCharPositionInLine();
                columns.add(m_factory.makeColumnRef(colName, colLineNo, colColNo));
            }
        } else {
            List<String> colNames = table.getColumnNames();
            for (String cname : colNames) {
                columns.add(m_factory.makeColumnRef(cname, -1, -1));
            }
        }

        m_insertStatement = m_factory.newInsertStatement();
        m_insertStatement.addTable(table);
        int idx = 0;
        List<String> colVals = new ArrayList<String>();
        for (ValueContext val : ctx.values().value()) {
            /*
             * TODO: This is not right.  These are expressions in general.  We
             * need to traffic in Semantinos here.
             */
            String valStr = val.NUMBER().getText();
            colVals.add(valStr);
            idx += 1;
        }
        m_insertStatement.addColumns(ctx.start.getLine(),
                                     ctx.start.getCharPositionInLine(),
                                     m_errorMessages,
                                     columns,
                                     colVals);
    }

    @Override
    public void reportAmbiguity(Parser aArg0, DFA aArg1, int aArg2, int aArg3,
            boolean aArg4, java.util.BitSet aArg5, ATNConfigSet aArg6) {
        // Nothing to be done here.
    }

    @Override
    public void reportAttemptingFullContext(Parser aArg0, DFA aArg1, int aArg2,
            int aArg3, java.util.BitSet aArg4, ATNConfigSet aArg5) {
        // Nothing to be done here.
    }

    @Override
    public void reportContextSensitivity(Parser aArg0, DFA aArg1, int aArg2,
            int aArg3, int aArg4, ATNConfigSet aArg5) {
        // Nothing to be done here.
    }

    @Override
    public void syntaxError(Recognizer<?, ?> aArg0, Object aTokObj, int aLine,
            int aCol, String msg, RecognitionException aArg5) {
        addError(aLine, aCol, msg);
    }

    public final ISelectQuery getSelectQuery() {
        return m_selectQuery;
    }

    public final IInsertStatement getInsertStatement() {
        return m_insertStatement;
    }

    public ICatalogAdapter getCatalogAdapter() {
        return m_catalog;
    }

    protected final IParserFactory getFactory() {
        return m_factory;
    }
    private IExpressionParser getTopExpressionParser() {
        assert(m_expressionStack.size() > 0);
        return m_expressionStack.get(m_expressionStack.size() - 1);
    }
}