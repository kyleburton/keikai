;; vim: set ts=2 sw=2 et ai ft=clojure:
(ns keikai.core
  (:import (java.net InetSocketAddress)
           (java.util.concurrent Executors)
           (org.jboss.netty.bootstrap ServerBootstrap)
           (org.jboss.netty.buffer ChannelBufferInputStream)
           (org.jboss.netty.channel ChannelPipelineFactory
                                    ChannelPipeline
                                    Channels
                                    SimpleChannelUpstreamHandler)
           (org.jboss.netty.channel.group DefaultChannelGroup)
           (org.jboss.netty.channel.socket.nio NioServerSocketChannelFactory))
  (:use clojure.contrib.logging))

(defn handle
  "Handle an individual incoming message."
  [msg]
  nil)

(defn read-msg
  "Read a complete message from an input stream."
  [instream]
  nil)

(defn tcp-handler
  "Returns a TCP connection handler."
  [channel-group]
  (proxy [SimpleChannelUpstreamHandler] []
    (channelOpen [context state-event]
                 (.add channel-group (.getChannel state-event)))
    (messageReceived [context message-event]
                     (let [instream (ChannelBufferInputStream.
                                     (.getMessage message-event))
                           msg (read-msg instream)]
                       (handle msg)))
    (exceptionCaught [context exception-event]
                     (warn (.getCause exception-event) "TCP handler caught"))))

(defn tcp-channel-factory
  "Generate a channel factory for use with a TCP server socket."
  []
  (NioServerSocketChannelFactory.
   (Executors/newCachedThreadPool)
   (Executors/newCachedThreadPool)))

(defn tcp-pipeline-factory
  "Generate a TCP channel pipeline factory."
  [channel-group]
  (proxy [ChannelPipelineFactory] []
    (getPipeline []
                 (let [p (Channels/pipeline)
                       h (tcp-handler channel-group)]
                   (.addLast p "keikai-tcp-handler" h)
                   p))))

(defn tcp-server
  "Starts a TCP server listening on the specified port or 7007 if none specified."
  ([]
     (tcp-server {}))
  ([opts]
     (let [opts (merge {:port 7007}
                       opts)
           bootstrap (ServerBootstrap. (tcp-channel-factory))
           all-channels (DefaultChannelGroup.)
           factory (tcp-pipeline-factory all-channels)]
       (doto bootstrap
         (.setPipelineFactory factory)
         (.setOption "child.tcpNoDelay" true)
         (.setOption "child.keepAlive" true)
         (.setOption "child.connectTimeoutMillis" 5000))
       (let [server-channel (.bind bootstrap (InetSocketAddress. (:port opts)))]
         (.add all-channels server-channel))
       (info (str "TCP server " opts " started"))
       (fn []
         (-> all-channels .close .awaitUninterruptibly)
         (. bootstrap releaseExternalResources)))))

(defn start
  "Start core Keikai services."
  []
  (tcp-server))
