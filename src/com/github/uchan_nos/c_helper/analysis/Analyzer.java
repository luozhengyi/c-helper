package com.github.uchan_nos.c_helper.analysis;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.gnu.c.GCCLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.DefaultLogService;
import org.eclipse.cdt.core.parser.FileContent;
import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IncludeFileContentProvider;
import org.eclipse.cdt.core.parser.ScannerInfo;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;

import com.github.uchan_nos.c_helper.Util;
import com.github.uchan_nos.c_helper.exceptions.InvalidEditorPartException;

public class Analyzer {
    private OutputStreamWriter cfgWriter;

    public Analyzer() {
        IPath filePath = new Path(System.getProperty("user.home"));
        filePath = filePath.append("cfg.dot");
        OutputStreamWriter writer;
        try {
            writer = new FileWriter(filePath.toFile());
        } catch (IOException e) {
            writer = new OutputStreamWriter(System.out);
        }
        this.cfgWriter = writer;
    }

    public Analyzer(OutputStreamWriter cfgWriter) {
        this.cfgWriter = cfgWriter;
    }

    public void close() throws IOException {
        this.cfgWriter.close();
    }

    public void analyze(IEditorPart activeEditorPart)
            throws InvalidEditorPartException {
        if (activeEditorPart instanceof ITextEditor) {
            ITextEditor textEditorPart = (ITextEditor) activeEditorPart;
            IDocument documentToAnalyze = textEditorPart.getDocumentProvider()
                    .getDocument(textEditorPart.getEditorInput());
            analyze(textEditorPart.getTitle(), documentToAnalyze.get());
        } else {
            throw new InvalidEditorPartException(
                    "We need a class implementing ITextEditor");
        }
    }

    public void analyze(String filePath, String source) {
        /*
        ILanguage language = GCCLanguage.getDefault();

        FileContent reader = FileContent.create(filePath, source);

        Map<String, String> macroDefinitions = null;
        String[] includeSearchPath = null;
        IScannerInfo scanInfo = new ScannerInfo(macroDefinitions,
                includeSearchPath);

        IncludeFileContentProvider fileCreator = IncludeFileContentProvider
                .getEmptyFilesProvider();
        IIndex index = null;
        int options = 0;
        IParserLogService log = new DefaultLogService();

        try {
            IASTTranslationUnit translationUnit = language
                    .getASTTranslationUnit(reader, scanInfo, fileCreator,
                            index, options, log);
            Map<String, CFG> procToCFG = createCFG(translationUnit);
            printCFG(procToCFG);

            for (Entry<String, CFG> cfg : procToCFG.entrySet()) {
                System.out.println("reaching definition of " + cfg.getKey());
                RDAnalyzer rd = new RDAnalyzer(translationUnit, cfg.getValue());
                rd.analyze();
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        */

        class RDEntry {
            final public String name;
            final public int id;
            public RDEntry(String name, int id) {
                this.name = name;
                this.id = id;
            }
        }

        try {
            IASTTranslationUnit translationUnit =
                    new Parser(filePath, source).parse();
            Map<String, CFG> procToCFG =
                    new CFGCreator(translationUnit).create();
            for (Entry<String, CFG> entry : procToCFG.entrySet()) {
                CFG cfg = entry.getValue();
                System.out.println("function " + entry.getKey());
                RD<CFG.Vertex> rd =
                        new RDAnalyzer(translationUnit, cfg).analyze();
                for (CFG.Vertex vertex : Util.sort(cfg.getVertices())) {
                    ArrayList<RDEntry> rdTemp = new ArrayList<RDEntry>();
                    BitSet exitSet = rd.getExitSets().get(vertex);
                    for (int assid = 0; assid < rd.getAssigns().length; ++assid) {
                        if (exitSet.get(assid)) {
                            AssignExpression assign = rd.getAssigns()[assid];
                            rdTemp.add(new RDEntry(assign.getLHS().getRawSignature(),
                                    assign.getRHS() == null ? -1 : assid));
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
        }
    }

    private void printCFG(Map<String, CFG> procToCFG) {
        String graphString = new CFGPrinter(procToCFG).toDotString();
        try {
            cfgWriter.write(graphString);
        } catch (IOException e) {
            System.out.println("cannot write");
            System.out.println(graphString);
        }
    }

    private Map<String, CFG> createCFG(IASTTranslationUnit ast) {
        return new CFGCreator(ast).create();
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
                        if (decl.getInitializer() != null) {
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
            private int id = 0;
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
}
