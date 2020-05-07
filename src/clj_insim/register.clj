(ns clj-insim.register)

(defn register! [map-atom k v]
  (swap! map-atom assoc k v))

(defn unregister! [map-atom k]
  (swap! map-atom dissoc k))

(defn init! [atm]
  (reset! atm {}))
