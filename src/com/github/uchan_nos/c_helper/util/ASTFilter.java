package com.github.uchan_nos.c_helper.util;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.cdt.core.dom.ast.IASTNode;

/**
 * 根据指定条件过滤AST元素的类。
 * @author uchan
 */
public class ASTFilter {
    public interface Predicate {
        /**
         * 返回值表示过滤器是否通过.
         * @param node AST结点
         * @return 如果通过过滤器，则为true.
         */
        boolean pass(IASTNode node);
    }

    private IASTNode ast;

    /**
     * 为指定的AST生成过滤器.
     * @param ast
     */
    public ASTFilter(IASTNode ast) {
        this.ast = ast;
    }

    /**
     * 指定された述語によりフィルタリングを行う.
     * @param pred フィルタを通過させたい要素のみ pass() == true となる述語
     * @return 通过过滤器的元素集合.
     */
    public Collection<IASTNode> filter(Predicate pred) {
        class Visitor extends AllASTVisitor {
            private ArrayList<IASTNode> filteredNodes;
            private Predicate filterPred;
            public Visitor(Predicate pred) {
                super(true);
                this.filteredNodes = new ArrayList<IASTNode>();
                this.filterPred = pred;
            }
            @Override
            protected void visitAny(IASTNode node) {
                super.visitAny(node);
                if (this.filterPred.pass(node)) {
                    this.filteredNodes.add(node);
                }
            }
            public Collection<IASTNode> getFilteredNodes() {
                return this.filteredNodes;
            }
        }
        Visitor visitor = new Visitor(pred);
        this.ast.accept(visitor);
        return visitor.getFilteredNodes();
    }
}
