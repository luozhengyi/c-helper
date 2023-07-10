package com.github.uchan_nos.c_helper.analysis.values;

import org.eclipse.cdt.core.dom.ast.IType;

/**
 * 代表C语言程序中的各种数值.
 * @author uchan
 */
public abstract class Value {
    public static final int OVERFLOWED = 1 << 0;
    public static final int UNDEFINED = 1 << 1;
    public static final int IMPLDEPENDENT = 1 << 2; // implement dependent

    /**
     * 返回Value所代表的类型.
     * @return
     */
    public abstract IType getType();

    /**
     * 返回Value的状态.
     * @return OVERFLOWED, UNDEFINED, IMPLDEPENDENT情况之一
     */
    public abstract int getFlag();

    /**
     * 类型转换.
     * @param newType
     * @return
     */
    public abstract Value castTo(IType newType);

    /**
     * 如果这个Value代表一个真值，则返回true.
     * @return Value持有非0值返回true、0值返回false
     */
    public abstract boolean isTrue();
}
