package com.github.uchan_nos.c_helper.dataflow;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;


import com.github.uchan_nos.c_helper.analysis.IGraph;

public abstract class ForwardSolver<Vertex, Value> extends Solver<Vertex, Value> {
    public ForwardSolver(IGraph<Vertex> cfg, Vertex entryVertex) {
        super(cfg, entryVertex);
    }

    /**
     * 执行数据流分析并返回结果.
     */
    @Override
    public Result<Vertex, Value> solve() {
        Map<Vertex, EntryExitPair<Value>> analysisValue =
            new HashMap<Vertex, EntryExitPair<Value>>();

        // 初始化集合
        for (Vertex v : getCFG().getVertices()) {
            analysisValue.put(v, new EntryExitPair<Value>(
                        v.equals(getEntryVertex()) ? getInitValue() : createDefaultSet(), // entry
                        createDefaultSet() // exit
                        ));
        }

        // forward 分析
        solveForward(analysisValue);
        return new Result<Vertex, Value>(analysisValue);
    }

    private void solveForward(Map<Vertex, EntryExitPair<Value>> analysisValue) {
        Queue<Vertex> remainVertices = new ArrayDeque<Vertex>();
        Set<Vertex> visitedVertices = new HashSet<Vertex>();

        Vertex v;
        boolean modified;
        do {
            modified = false;
            visitedVertices.clear();
            remainVertices.add(getEntryVertex());

            while ((v = remainVertices.poll()) != null && !visitedVertices.contains(v)) {
                visitedVertices.add(v);
                Set<Vertex> connectedVertices = getCFG().getConnectedVerticesFrom(v);
                for (Vertex nextVisit : connectedVertices) {
                    remainVertices.add(nextVisit);
                }

                // 获取顶点V的分析值
                final EntryExitPair<Value> vInfo = analysisValue.get(v);

                // 頂点 v 的入口值计算
                // 頂点 v 的入口值是由其前驱的出口值join计算出来的
                for (Vertex prevVertex : getCFG().getConnectedVerticesTo(v)) {
                    final Set<Value> exitSet = analysisValue.get(prevVertex).exit();
                    modified |= join(vInfo.entry(), exitSet);
                }

                // 頂点 v 的出口值
                // 頂点 v 的入口值和出口值是由transfer()函数计算的
                modified |= transfer(v, vInfo.entry(), vInfo.exit());
            }
        } while (modified);
    }
}
