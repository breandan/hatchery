package org.duckietown.hatchery.achdjian.data

import com.intellij.openapi.diagnostic.Logger
import org.duckietown.hatchery.achdjian.utils.catkinFindLibexec
import org.w3c.dom.*
import java.nio.file.*
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.*

class RosPackage(val path: Path, val env: Map<String, String>) {
    companion object {
        val log = Logger.getInstance(RosPackage::class.java)
    }

    val name: String
    val version: String
    val description: String
    private var rosNodes: List<RosNode>? = null

    init {
        log.trace("package at $path, with ${env.size} environment")
        val packageFile = path.resolve("package.xml")
        val doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().parse(packageFile.toFile())

        val xPath = XPathFactory.newInstance().newXPath()
        name = getNodeValue(xPath, "/package/name", doc)
        version = getNodeValue(xPath, "/package/version", doc)
        description = getNodeValue(xPath, "/package/description", doc)
    }

    fun getNodes(): List<RosNode> = rosNodes ?: searchRosNodes()

    private fun searchRosNodes(): List<RosNode> {
        val foundNodes = ArrayList<RosNode>()
        catkinFindLibexec(name, env).forEach { libExecPath ->
            Files.walk(libExecPath)
                    .filter { !Files.isDirectory(it) && Files.isExecutable(it) }
                    .map { RosNode(it) }
                    .forEach { foundNodes.add(it) }
        }
        rosNodes = foundNodes
        return foundNodes
    }

    private fun getNodeValue(xPath: XPath, xpath: String, doc: Document): String {
        val nodes = xPath.evaluate(xpath, doc, XPathConstants.NODESET) as NodeList
        return if (nodes.length > 0) nodes.item(0).textContent.trim() else ""
    }
}