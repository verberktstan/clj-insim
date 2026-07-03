(ns clj-insim.channel
  "Packet channel plumbing: putting packets on and taking packets off the
   `:to-lfs`/`:from-lfs` core.async channels of a running client."
  (:require [clojure.core.async :as a]))

(defn >!!
  "(Blocking) put packet on the channel for sending to LFS."
  [client packet]
  (a/>!! (:to-lfs client) packet))

(defn <!!
  "(Blocking) take packet from the channel for receiving from LFS."
  [client packet]
  (a/<!! (:from-lfs client)))

(defn >!
  "(Async) put packet on the channel for sending to LFS."
  [client packet]
  (a/go (a/>! (:to-lfs client) packet)))

(defn <!
  "(Ascync) take packet from the channel for receiving from LFS."
  [client packet]
  (a/go (a/<! (:from-lfs client))))

(defn close!
  "Closes both the `:to-lfs` and `:from-lfs` channels of client."
  [{:keys [from-lfs to-lfs]}]
  (a/close! from-lfs)
  (a/close! to-lfs))
