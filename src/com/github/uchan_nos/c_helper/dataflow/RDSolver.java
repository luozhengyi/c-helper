package com.github.uchan_nos.c_helper.dataflow;

import java.io.File;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;

import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.*;

import org.eclipse.core.runtime.CoreException;

import com.github.uchan_nos.c_helper.analysis.AssignExpression;
import com.github.uchan_nos.c_helper.analysis.CFG;
import com.github.uchan_nos.c_helper.analysis.CFGCreator;
import com.github.uchan_nos.c_helper.analysis.DummyAssignExpression;
import com.github.uchan_nos.c_helper.analysis.FileInfo;
import com.github.uchan_nos.c_helper.analysis.IGraph;
import com.github.uchan_nos.c_helper.analysis.Parser;

import com.github.uchan_nos.c_helper.util.Util;

public class RDSolver extends GenKillForwardSolver<CFG.Vertex, AssignExpression> {
    private ArrayList<AssignExpression> assignList; // cfg中的赋值语句列表（包括DummyAssignExpression）
    private Set<IASTIdExpression> idExpressionList; // cfg中的ID表达式列表
    private ArrayList<DummyAssignExpression> dummyAssignList; // cfg中虚拟变量定义的列表

    public RDSolver(IGraph<CFG.Vertex> cfg, CFG.Vertex entryVertex, IASTTranslationUnit ast) {
        super(cfg, entryVertex);
        this.assignList = createAssignExpressionList(ast);
        this.idExpressionList = createIdExpressionList(ast);
        this.dummyAssignList = createInitialAssigns(this.assignList.size(), this.idExpressionList);
        this.assignList.addAll(this.dummyAssignList);
    }

    @Override
    protected Set<AssignExpression> getInitValue() {
        return new HashSet<AssignExpression>(this.dummyAssignList);
    }

    @Override
    protected Set<AssignExpression> createDefaultSet() {
        return new HashSet<AssignExpression>();
    }

    @Override
    protected boolean join(Set<AssignExpression> result,
            Set<AssignExpression> set) {
        return result.addAll(set);
    }

    @Override
    protected Set<AssignExpression> clone(Set<AssignExpression> set) {
        return new HashSet<AssignExpression>(set);
    }

    /**
     * 为一个给定的顶点生成一个gen, kill集合.
     * @param v 需要生成gen, kill集合的顶点.
     * @return gen, kill集合
     */
    @Override
    protected GenKill<AssignExpression> getGenKill(CFG.Vertex v) {
        GenKill<AssignExpression> result = null;
        if (v.getASTNode() != null) {
            result = createGenKill(v.getASTNode());
        }
        if (result == null) {
            result = new GenKill<AssignExpression>(new HashSet<AssignExpression>(), new HashSet<AssignExpression>());
        }
        return result;
    }

    // 为指定的AST节点生成gen、kill集.
    private GenKill<AssignExpression> createGenKill(IASTNode ast) {
        if (ast instanceof IASTExpressionStatement) {
            return createGenKill((IASTExpressionStatement)ast);
        } else if (ast instanceof IASTDeclarationStatement) {
            return createGenKill((IASTDeclarationStatement)ast);
        }
        return null;
    }

    private GenKill<AssignExpression> createGenKill(IASTExpressionStatement ast) {
        /**
         * 与assign相对应的gen, kill,
         *  gen即是assign,
         *  kill即是killedAssigns
         */
        AssignExpression assign = getAssignExpressionOfNode(ast.getExpression());
        ArrayList<AssignExpression> killedAssigns = null;

        if (assign != null) {
            if (assign.getLHS() instanceof IASTIdExpression) {
                killedAssigns = getAssignExpressionsOfName(
                        ((IASTIdExpression)assign.getLHS()).getName());
            }
        }

        if (assign != null && killedAssigns != null) {
            Set<AssignExpression> gen = new HashSet<AssignExpression>();
            Set<AssignExpression> kill = new HashSet<AssignExpression>(killedAssigns);
            gen.add(assign);
            return new GenKill<AssignExpression>(gen, kill);
        }
        return null;

    }

    private GenKill<AssignExpression> createGenKill(IASTDeclarationStatement ast) {
        // 与带初始化的变量定义对应的gen, kill
        if (ast.getDeclaration() instanceof IASTSimpleDeclaration) {
            IASTDeclarator[] declarators =
                    ((IASTSimpleDeclaration)ast.getDeclaration()).getDeclarators();
            Set<AssignExpression> gen = new HashSet<AssignExpression>();
            Set<AssignExpression> kill = new HashSet<AssignExpression>();
            for (int i = 0; i < declarators.length; ++i) {
                if (declarators[i].getInitializer() != null) {
                    AssignExpression assign = getAssignExpressionOfNode(declarators[i]);
                    // 如果变量定义未被初始化, assign为null
                    gen.add(assign);
                    IASTName nameToBeKilled = null;
                    if (assign.getLHS() instanceof IASTName) {
                        nameToBeKilled = (IASTName)assign.getLHS();
                    } else if (assign.getLHS() instanceof IASTIdExpression) {
                        nameToBeKilled = ((IASTIdExpression)assign.getLHS()).getName();
                    }
                    if (nameToBeKilled != null) {
                        ArrayList<AssignExpression> killedAssigns =
                                getAssignExpressionsOfName(nameToBeKilled);
                        kill.addAll(killedAssigns);
                    }
                }
            }

            return new GenKill<AssignExpression>(gen, kill);
        }
        return null;
    }

    /**
     * 获取对应于指定AST节点的变量定义.
     * 指定的AST节点对应于"初始化的变量定义中的IASTDeclarator"或
     * "赋值语句中的IASTBinaryExpression".
     * @param ast
     * @return
     */
    private AssignExpression getAssignExpressionOfNode(IASTNode ast) {
        for (int i = 0; i < assignList.size(); ++i) {
            if (assignList.get(i).getAST() == ast) {
                return assignList.get(i);
            }
        }
        return null;
    }

    /**
     * 获得一个以指定名称为左边值的变量定义列表,以用于计算killed list.
     * @param name 要提取的名称
     * @return 提取的变量定义的清单
     */
    private ArrayList<AssignExpression> getAssignExpressionsOfName(IASTName name) {
        ArrayList<AssignExpression> result = new ArrayList<AssignExpression>();
        IBinding nameBinding = name.resolveBinding(); // 通过IBinding进行比较

        for (int i = 0; i < assignList.size(); ++i) {
            IASTNode lhs = assignList.get(i).getLHS(); // 对于 int* p = &b, return p, but not *p;
            if (lhs instanceof IASTName) { // 变量定义返回IASTName
                IASTName n = (IASTName)lhs;
                if (n.resolveBinding().equals(nameBinding)) {
                    result.add(assignList.get(i));
                }
            } else if (lhs instanceof IASTIdExpression) { // 变量赋值返回IASTIdExpression
                IASTIdExpression e = (IASTIdExpression)lhs;
                if (e.getName().resolveBinding().equals(nameBinding)) {
                    result.add(assignList.get(i));
                } else if(e.getName().resolveBinding() instanceof ProblemBinding
                        && nameBinding instanceof ProblemBinding
                        && e.getName().toString().equals(name.toString())) { // handle undefined variable
                    result.add(assignList.get(i));
                }
            }
        }
        return result;
    }


    private static ArrayList<DummyAssignExpression> createInitialAssigns(int startId, Collection<IASTIdExpression> idExpressionList) {
        int id = startId;
        ArrayList<DummyAssignExpression> result = new ArrayList<DummyAssignExpression>();
        for (IASTIdExpression idExpression : idExpressionList) {
            IType type = idExpression.getExpressionType();
            if (!(type instanceof IFunctionType)) {
                DummyAssignExpression e = new DummyAssignExpression(id, idExpression);
                result.add(e);
                id++;
            }
        }
        return result;
    }

    private ArrayList<AssignExpression> createAssignExpressionList(IASTTranslationUnit ast) {
        final ArrayList<AssignExpression> result = new ArrayList<AssignExpression>();
        ast.accept(new ASTVisitor(true) {
            private int id = 0;
            @Override
            public int visit(IASTExpression expression) {
                if (expression instanceof IASTBinaryExpression) {
                    IASTBinaryExpression e = (IASTBinaryExpression)expression;
                    if (e.getOperator() == IASTBinaryExpression.op_assign) {
                        result.add(new AssignExpression(id, e));
                        id++;
                    }
                }
                return super.visit(expression);
            }
            @Override
            public int visit(IASTDeclaration declaration) {
                if (declaration instanceof IASTSimpleDeclaration) {
                    IASTSimpleDeclaration d = (IASTSimpleDeclaration)declaration;
                    for (IASTDeclarator decl : d.getDeclarators()) {
                        if (decl.getInitializer() != null) { // why we exclude the declaration with initializer??
                            // 带初始化的变量声明
                            result.add(new AssignExpression(id, decl));
                            id++;
                        }
                    }
                }
                return super.visit(declaration);
            }
        });
        return result;
    }

    private Set<IASTIdExpression> createIdExpressionList(IASTTranslationUnit ast) {
        final Set<IASTIdExpression> result = new TreeSet<IASTIdExpression>(new Comparator<IASTIdExpression>() {
            @Override
            public int compare(IASTIdExpression o1, IASTIdExpression o2) {
                return o1.getName().toString().compareTo(o2.getName().toString());
            }
        });
        ast.accept(new ASTVisitor(true) {
            @Override
            public int visit(IASTExpression expression) {
                if (expression instanceof IASTIdExpression) {
                    result.add((IASTIdExpression)expression);
                }
                return super.visit(expression);
            }
        });
        return result;
    }

    public static void main(String[] args) {
        class RDEntry {
            final public String name;
            final public int id;
            public RDEntry(String name, int id) {
                this.name = name;
                this.id = id;
            }
        }

        if (args.length == 1) {
            String inputFilename = args[0];
            File inputFile = new File(inputFilename);

            try {
                String fileContent = Util.readFileAll(inputFile, "UTF-8");
                IASTTranslationUnit translationUnit =
                        new Parser(new FileInfo(inputFilename, false), fileContent).parse();
                Map<String, CFG> procToCFG =
                        new CFGCreator(translationUnit).create();
                for (Entry<String, CFG> entry : procToCFG.entrySet()) {
                    CFG cfg = entry.getValue();
                    System.out.println("function " + entry.getKey());

                    long start = System.currentTimeMillis();
                    Solver.Result<CFG.Vertex, AssignExpression> rd =
                        new RDSolver(cfg, cfg.entryVertex(), translationUnit).solve();
                    long end = System.currentTimeMillis();
                    System.out.println("time ellapsed: " + (end - start) + "ms");

                    for (CFG.Vertex vertex : Util.sort(cfg.getVertices())) {
                        IASTNode node = vertex.getASTNode();
                        IScope[] nodeScopes = Util.getAllScopes(node).toArray(new IScope[] {});

                        ArrayList<RDEntry> rdTemp = new ArrayList<RDEntry>();
                        Set<AssignExpression> exitSet = rd.analysisValue.get(vertex).exit();
                        for (AssignExpression assign : exitSet) {
                            IASTName name = Util.getName(assign.getLHS());

                            try {
                                if (Util.contains(name.resolveBinding().getScope(), nodeScopes)) {
                                    /*
                                    System.out.println(
                                            name.toString() + ":"
                                            + name.getFileLocation().getStartingLineNumber() + " scope in "
                                            + node.getFileLocation().getStartingLineNumber() + " node scopes");
                                            */
                                    rdTemp.add(new RDEntry(
                                                assign.getLHS().getRawSignature()
                                                + ":"
                                                + assign.getLHS().getFileLocation().getStartingLineNumber(),
                                                assign.getRHS() == null ? -1 : assign.getId()));
                                }
                            } catch (DOMException e) {
                                e.printStackTrace();
                            }
                        }
                        Collections.sort(rdTemp, new Comparator<RDEntry>() {
                            @Override
                            public int compare(RDEntry o1, RDEntry o2) {
                                int diff = o1.name.compareTo(o2.name);
                                if (diff == 0) {
                                    return o1.id - o2.id;
                                }
                                return diff;
                            }
                        });
                        System.out.println("exit of " + vertex.label());
                        for (RDEntry rdEntry : rdTemp) {
                            System.out.print("(" + rdEntry.name + ","
                                    + (rdEntry.id == -1 ? "?" : String.valueOf(rdEntry.id)) + ")");
                        }
                        System.out.println();
                    }
                }
            } catch (CoreException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

