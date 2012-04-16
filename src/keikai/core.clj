;; vim: set ts=2 sw=2 et ai ft=clojure:
(ns keikai.core
  (:import (java.net InetSocketAddress)
           (java.util.concurrent Executors)
           (org.jboss.netty.bootstrap ConnectionlessBootstrap)
           (org.jboss.netty.buffer ChannelBufferInputStream)
           (org.jboss.netty.channel ChannelPipelineFactory
                                    ChannelPipeline
                                    Channels
                                    FixedReceiveBufferSizePredictorFactory
                                    SimpleChannelUpstreamHandler)
           (org.jboss.netty.channel.group DefaultChannelGroup)
           (org.jboss.netty.channel.socket.nio NioDatagramChannelFactory))
  (:use clojure.tools.logging)
  (:require [keikai.collectd :as collectd]))

(defn udp-handler
  "Returns a UDP connection handler."
  [channel-group]
  (proxy [SimpleChannelUpstreamHandler] []
    (channelOpen [context state-event]
                 (.add channel-group (.getChannel state-event)))
    (messageReceived [context message-event]
                     (let [instream (ChannelBufferInputStream.
                                     (.getMessage message-event))
                           msg (collectd/decode instream)]
                       (collectd/handle msg)))
    (exceptionCaught [context exception-event]
                     (warn (.getCause exception-event) "message caused error"))))

(defn udp-channel-factory
  "Generate a channel factory for use with a UDP datagram socket."
  []
  (NioDatagramChannelFactory.
   (Executors/newCachedThreadPool)))

(defn udp-pipeline-factory
  "Generate a UDP channel pipeline factory."
  [channel-group]
  (proxy [ChannelPipelineFactory] []
    (getPipeline []
                 (let [p (Channels/pipeline)
                       h (udp-handler channel-group)]
                   (.addLast p "keikai-udp-handler" h)
                   p))))

(defn udp-server
  "Starts a UDP server listening on the specified port or 25826 if none specified."
  ([]
     (udp-server {}))
  ([opts]
     (let [opts (merge {:port 25826
                        :max-len 1452}
                       opts)
           bootstrap (ConnectionlessBootstrap. (udp-channel-factory))
           all-channels (DefaultChannelGroup.)
           factory (udp-pipeline-factory all-channels)]
       (doto bootstrap
         (.setPipelineFactory factory)
         (.setOption "broadcast" "false")
         (.setOption "receiveBufferSizePredictorFactory"
                     (FixedReceiveBufferSizePredictorFactory. (:max-len opts))))
       (let [server-channel (.bind bootstrap (InetSocketAddress. (:port opts)))]
         (.add all-channels server-channel))
       (info "started UDP listener on port" (:port opts))
       ; use this fn to shutdown this server
       (fn []
         (-> all-channels .close .awaitUninterruptibly)
         (. bootstrap releaseExternalResources)))))

(def shutdown-fn (atom #()))

(defn start
  "Start core Keikai services."
  []
  (udp-server))

(defn stop
  "Stop core Keikai services."
  []
  (@shutdown-fn))
