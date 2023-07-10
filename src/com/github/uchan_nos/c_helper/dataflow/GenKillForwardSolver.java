package com.github.uchan_nos.c_helper.dataflow;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.github.uchan_nos.c_helper.analysis.IGraph;

public abstract class GenKillForwardSolver<Vertex, Value> extends ForwardSolver<Vertex, Value> {
    public GenKillForwardSolver(IGraph<Vertex> cfg, Vertex entryVertex) {
        super(cfg, entryVertex);
    }

    private Map<Vertex, GenKill<Value>> genkill = null;

    /**
     * 执行数据流分析并返回结果.
     */
    @Override
    public Result<Vertex, Value> solve() {
        // 生成gen/kill集合
        this.genkill = new HashMap<Vertex, GenKill<Value>>();
        for (Vertex v : getCFG().getVertices()) {
            genkill.put(v, getGenKill(v));
        }

        // 执行分析
        Result<Vertex, Value> result = super.solve();

        // 整理
        this.genkill = null;
        return result;
    }

    /**
     * transfer.
     */
    protected boolean transfer(Vertex v, Set<Value> entry, Set<Value> result) {
        Set<Value> oldResult = clone(result);

        final GenKill<Value> gk = genkill.get(v);
        result.addAll(entry);
        result.removeAll(gk.kill);
        result.addAll(gk.gen);

        return !result.equals(oldResult);
    }

    /**
     * 获取指定顶点的gen/kill集合.
     */
    protected abstract GenKill<Value> getGenKill(Vertex v);
}
