package org.duckietown.hatchery.ros.nodes


import org.ros.namespace.GraphName
import org.ros.node.*

class Listener: AbstractNodeMain() {
  override fun getDefaultNodeName() = GraphName.of("rosjava_tutorial/listener")!!

  override fun onStart(connectedNode: ConnectedNode?) {
    val log = connectedNode!!.log
    val subscriber = connectedNode.newSubscriber<std_msgs.String>("chatter", std_msgs.String._TYPE)
    subscriber.addMessageListener { p0 -> println("I heard: \"" + p0?.data + "\""); }
  }
}