(ns clj-insim.parsers)

(defn parse-race-laps [rl]
  (cond
    (#{0} rl) :practice
    (<= 100 rl 190) {:laps (-> rl (- 100) (* 10) (+ 100))}
    (<= 191 rl 238) {:hours (- rl 190)}
    :else {:laps rl}))
