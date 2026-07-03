(ns clj-insim.connection
  "Socket & stream lifecycle for connecting with LFS over TCP."
  (:import [java.net Socket]))

(defn make-socket
  "Opens and returns a `Socket` connected to `host`:`port`. Returns `nil` and
   prints a message when the connection is refused."
  [host port]
  (try
    (Socket. host port)
    (catch java.net.ConnectException e
      (println (.getMessage e) (format "\nPlease run `/insim %d` in LFS." port)))))

(defn close-socket!
  "Closes input-stream, output-stream and socket."
  [socket input-stream output-stream]
  (.close input-stream)
  (.close output-stream)
  (.close socket))
