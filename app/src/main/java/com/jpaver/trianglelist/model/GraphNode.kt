package com.jpaver.trianglelist.model

open class GraphNode<T>(val value: T) {
    open val neighbors: MutableList<GraphNode<T>> = mutableListOf()

    fun connect(node: GraphNode<T>, function: (GraphNode<T>) -> Unit) {
        if (!neighbors.contains(node)) {
            neighbors.add(node)
            node.neighbors.add(this) // 双方向の接続を確立する
        }
    }

    fun traverseAndApply(visited: MutableSet<GraphNode<T>>, function: (GraphNode<T>) -> Unit) {
        if (this in visited) return  // 既に訪問したノードはスキップ
        visited.add(this)  // 現在のノードを訪問済みに追加
        function(this)  // 現在のノードに関数を適用

        // 隣接ノードに対して再帰的に関数を適用
        for (neighbor in neighbors) {
            neighbor.traverseAndApply(visited, function)
        }
    }
}

