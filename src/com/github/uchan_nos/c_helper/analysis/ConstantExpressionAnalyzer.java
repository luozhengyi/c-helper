package com.github.uchan_nos.c_helper.analysis;

import java.math.BigInteger;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTBinaryExpression;
import org.eclipse.cdt.core.dom.ast.IASTCastExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTLiteralExpression;

import com.github.uchan_nos.c_helper.analysis.values.IntegralValue;
import com.github.uchan_nos.c_helper.analysis.values.Value;

/**
 * 提供判断给定表达式是否具有常量值的功能.
 * @author uchan
 */
public class ConstantExpressionAnalyzer {
    public static class Info {
        private boolean isConstant;
        private boolean isUndefined;
        private String message;
        private Value value;

        /**
         * 生成代表具有指定值的常量的信息.
         * @param value 定数値
         */
        public Info(Value value) {
            this.isConstant = true;
            this.isUndefined = false;
            this.message = null;
            this.value = value;
        }

        /**
         * 生成信息，表明由于指定原因导致未定义.
         * @param message 未定义的原因
         */
        public Info(String message) {
            this.isConstant = false;
            this.isUndefined = true;
            this.message = message;
            this.value = null;
        }

        /**
         * 生成信息，表明它不是常数.
         */
        public Info() {
            this.isConstant = false;
            this.isUndefined = false;
            this.message = null;
            this.value = null;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public boolean isConstant() {
            return isConstant;
        }

        public boolean isUndefined() {
            return isUndefined;
        }

        public String message() {
            return message;
        }

        public Value value() {
            return value;
        }
    }

    private CFG cfg;
    private RD<CFG.Vertex> rd;
    private AnalysisEnvironment analysisEnvironment;

    public ConstantExpressionAnalyzer(CFG cfg, RD<CFG.Vertex> rd, AnalysisEnvironment env) {
        this.cfg = cfg;
        this.rd = rd;
        this.analysisEnvironment = env;
    }

    public Value eval(IASTExpression expression) {
        class SearchLiteralVisitor extends ASTVisitor {
            private IASTLiteralExpression literalExpression = null;
            public IASTLiteralExpression literalExpression() {
                return literalExpression;
            }
            public SearchLiteralVisitor() {
                super(true);
            }
            @Override
            public int visit(IASTExpression expression) {
                if (expression instanceof IASTCastExpression) {
                    // recursively
                    ((IASTCastExpression) expression).getOperand().accept(this); // equivalent to stripCast()
                } else if (expression instanceof IASTLiteralExpression) {
                    literalExpression = (IASTLiteralExpression) expression;
                }
                return super.visit(expression);
            }
        };

        if (expression instanceof IASTBinaryExpression) {
            IASTBinaryExpression be = (IASTBinaryExpression) expression;
            Value lhs = eval(be.getOperand1()); // recursively
            Value rhs = eval(be.getOperand2()); // recursively
            if (lhs != null && rhs != null) {
                if (lhs instanceof IntegralValue && rhs instanceof IntegralValue) {
                    IntegralValue lhs_ = (IntegralValue) lhs;
                    IntegralValue rhs_ = (IntegralValue) rhs;
                    return new IntegralValue(lhs_.getValue().multiply(rhs_.getValue()), lhs_.getType(), 0, analysisEnvironment);
                }
            }
        }
        // unaryExpression/constantExpression
        SearchLiteralVisitor searchLiteral = new SearchLiteralVisitor();
        expression.accept(searchLiteral);
        IASTLiteralExpression literalExpression = searchLiteral.literalExpression();
        if (literalExpression != null) {
            if (literalExpression.getKind() == IASTLiteralExpression.lk_integer_constant) {
                long value = Long.parseLong(String.valueOf(literalExpression.getValue()));
                return new IntegralValue(
                        BigInteger.valueOf(value),
                        literalExpression.getExpressionType(), 0, analysisEnvironment);
            }
        }
        return null;
    }
}
