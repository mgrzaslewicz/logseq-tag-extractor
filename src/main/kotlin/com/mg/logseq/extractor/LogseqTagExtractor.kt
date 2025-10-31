package com.mg.logseq.extractor

import java.nio.file.Path
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText


class LogseqTagExtractor(
    private val fileContent: String,
) {

    private class Node(
        val text: String,
        val indentLevel: Int,
        val parent: Node? = null,
        val children: MutableList<Node> = mutableListOf(),
    ) {
        /**
         * Merges path from root to other node. Other node will have children included
         */
        fun mergeFromRoot(other: Node) {
            getRoot().addPath(other.pathFromRoot())
        }

        private fun addPath(topToBottomPath: List<Node>) {
            var currentMatchingNode = this
            val topToBottomWithoutRoot = topToBottomPath.drop(1)
            topToBottomWithoutRoot.forEachIndexed { index, node ->
                val copy = if (index == topToBottomWithoutRoot.lastIndex) {
                    // we want all the nested content
                    node.copyWithChildren()
                } else {
                    node.copyWithoutChildren()
                }
                if (!currentMatchingNode.children.contains(node)) {
                    val newNode = copy
                    currentMatchingNode.children.add(newNode)
                    currentMatchingNode = currentMatchingNode.children.find { it == node }!!
                } else {
                    currentMatchingNode.children.add(copy)
                    currentMatchingNode = copy
                }
            }
        }

        private fun pathFromRoot(): List<Node> {
            val result = mutableListOf<Node>()
            var currentNode: Node? = this
            while (currentNode != null) {
                result.add(0, currentNode)
                currentNode = currentNode.parent
            }
            return result
        }

        private fun copyWithoutChildren(): Node {
            return Node(
                text = text,
                indentLevel = indentLevel,
                parent = parent,
                children = mutableListOf(),
            )
        }

        private fun copyWithChildren(): Node {
            return Node(
                text = text,
                indentLevel = indentLevel,
                parent = parent,
                children = children,
            )
        }

        private fun getRoot(): Node {
            var current = this
            while (current.parent != null) {
                current = current.parent
            }
            return current
        }

        fun isRoot() = parent == null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Node

            if (indentLevel != other.indentLevel) return false
            if (text != other.text) return false
            if (parent != other.parent) return false

            return true
        }

        override fun hashCode(): Int {
            var result = indentLevel
            result = 31 * result + text.hashCode()
            result = 31 * result + (parent?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return if (indentLevel == 0 && text.isEmpty()) {
                "ROOT"
            } else {
                "Node(text='$text', indentLevel=$indentLevel, childrenCount=${children.size}, hasParent=${parent != null})"
            }
        }

        fun parentOrRoot() = parent ?: this

        companion object {
            fun createRootNode() = Node("", 0)
        }

    }

    private fun String.countIdent() = if (startsWith(" ")) {
        takeWhile { it.isWhitespace() }.count() / 2
    } else {
        takeWhile { it.isWhitespace() }.count()
    }

    fun extract(tag: String): List<String> {
        val root = Node.createRootNode()
        var currentNode = root
        fileContent.lines().forEach { line ->
            if (line.isBlank()) {
                return@forEach
            }
            val nextIndentLevel = line.countIdent()
            if (nextIndentLevel > currentNode.indentLevel) {
                val newNode = Node(line, nextIndentLevel, currentNode)
                currentNode.children.add(newNode)
                currentNode = newNode
            } else if (nextIndentLevel < currentNode.indentLevel) {
                val levelDiff = currentNode.indentLevel - nextIndentLevel
                repeat(levelDiff + 1) {
                    currentNode = currentNode.parentOrRoot()
                }
                val newNode = Node(line, nextIndentLevel, currentNode)
                currentNode.children.add(newNode)
                currentNode = newNode
            } else {
                val parent = currentNode.parentOrRoot()
                val newNode = Node(line, nextIndentLevel, parent)
                parent.children.add(newNode)
                currentNode = newNode
            }

        }
        val resultNode = Node.createRootNode()
        visit(root, resultNode, tag)

        val result = mutableListOf<String>()
        toStrings(resultNode, result)
        return result
    }

    private fun String.hasTag(tag: String): Boolean {
        return this.contains("#$tag") || this.contains("[[$tag]]") || this.contains("[[$tag/]]")
    }

    private fun visit(visitedNode: Node, collector: Node, tag: String) {
        if (visitedNode.text.hasTag(tag)) {
            collector.mergeFromRoot(visitedNode)
        }
        visitedNode.children.forEach { visit(it, collector, tag) }
    }

    private fun toStrings(visitedNode: Node, result: MutableList<String>) {
        if (!visitedNode.isRoot()) {
            result.add(visitedNode.text)
        }
        visitedNode.children.forEach { toStrings(it, result) }
    }
}

private fun Path.toLocalDate(): LocalDate {
    val filename = fileName.toString()
    val datePart = filename.substringBefore(".md")
    return try {
        LocalDate.parse(datePart, DateTimeFormatter.ofPattern("yyyy_MM_dd"))
    } catch (e: DateTimeParseException) {
        LocalDate.ofInstant(Instant.ofEpochMilli(this.getLastModifiedTime().toMillis()), ZoneId.systemDefault())
    }
}

fun main(args: List<String>) {
    val tag = args.first()
    val markdownFiles = Path.of(".")
        .listDirectoryEntries("*.md")
        .sortedBy { it.toLocalDate() }
    markdownFiles.forEach { markdownFile ->
        val markdown = markdownFile.readText()
        val extractor = LogseqTagExtractor(markdown)
        val result = extractor.extract(tag)
        if (result.isNotEmpty()) {
            println(">>>>>>>>>>>>>>>> ${markdownFile.fileName}")
            result.forEach { println(it) }
        }
    }
}
