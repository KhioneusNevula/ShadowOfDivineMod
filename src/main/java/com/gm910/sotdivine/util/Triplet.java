package com.gm910.sotdivine.util;

import com.google.common.collect.Table.Cell;

/**
 * Three values
 * 
 * @param <R>
 * @param <C>
 * @param <V>
 */
record Triplet<R, C, V>(R getRowKey, C getColumnKey, V getValue) implements Cell<R, C, V> {

}
