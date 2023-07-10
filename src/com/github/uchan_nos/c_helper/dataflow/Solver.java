package com.github.uchan_nos.c_helper.dataflow;

import java.util.Map;
import java.util.Set;

import com.github.uchan_nos.c_helper.analysis.IGraph;

/**
 * 用于数据流分析的类.
 * @author uchan
 */
public abstract class Solver<Vertex, Value> {
    private final IGraph<Vertex> cfg;
    private final Vertex entryVertex;

    /**
     * 分析结果.
     * 在内部，它也被用来存储分析的进展.
     */
    public static class Result<Vertex, Value> {
        public final Map<Vertex, EntryExitPair<Value>> analysisValue;

        public Result(Map<Vertex, EntryExitPair<Value>> analysisValue) {
            this.analysisValue = analysisValue;
        }
    }

    public Solver(IGraph<Vertex> cfg, Vertex entryVertex) {
        this.cfg = cfg;
        this.entryVertex = entryVertex;
    }

    protected IGraph<Vertex> getCFG() {
        return cfg;
    }

    protected Vertex getEntryVertex() {
        return entryVertex;
    }

    /**
     * 执行数据流分析并返回结果.
     */
    public abstract Result<Vertex, Value> solve();

    /**
     * 返回用于分析的初始值.
     * 在前向分析中，流动图的入口节点的入口值、
     * 在后向分析中，它是流动图的出口节点的出口值.
     */
    protected abstract Set<Value> getInitValue();

    /**
     * 为存储分析值的集合建立并返回一个新的初始值.
     * 正向分析中入口节点的入口值和
     * 后向分析中出口节点的非出口值集合的初始值.
     */
    protected abstract Set<Value> createDefaultSet();

    /**
     * 某一顶点的transfer函数.
     * 前向分析，根据入口值计算出口值.
     * @return result 发生改变则返回 true
     */
    protected abstract boolean transfer(Vertex v, Set<Value> entry, Set<Value> result);

    /**
     * join运算符(执行并集操作).
     * 将set添加到result中.
     * @return result 发生改变则返回 true
     */
    protected abstract boolean join(Set<Value> result, Set<Value> set);

    /**
     * 复制一个集合.
     */
    protected abstract Set<Value> clone(Set<Value> set);
}
