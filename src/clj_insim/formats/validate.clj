(ns clj-insim.formats.validate
  "Scratch validation for the `.knw` curvature hypothesis: is
   `:segment/curvature-hyp` actually the path curvature over that segment's
   node range?

   Computes real geometric curvature (turn angle between consecutive node
   direction vectors, divided by distance) straight from `.pth` node data,
   then correlates it against the `.knw` segment's hypothesised curvature
   field. Not a pass/fail test - the point is to report the correlation, not
   assert a threshold, since the outcome (see comment at bottom) is
   inconclusive/negative and worth keeping visible rather than deleting.

   Run: `clojure -M -e \"(require 'clj-insim.formats.validate) (clj-insim.formats.validate/report! \\\"resources/BL1.pth\\\" \\\"resources/BL1_XFG.knw\\\")\"`"
  (:require [clj-insim.formats.knw :as knw]
            [clj-insim.formats.pth :as pth]))

(defn- distance [[x0 y0 z0] [x1 y1 z1]]
  (Math/sqrt (+ (Math/pow (- x1 x0) 2) (Math/pow (- y1 y0) 2) (Math/pow (- z1 z0) 2))))

(defn- dot [[x0 y0 z0] [x1 y1 z1]]
  (+ (* x0 x1) (* y0 y1) (* z0 z1)))

(defn- clamp [v lo hi] (max lo (min hi v)))

(defn node-curvatures
  "Per-node curvature: turn angle to the next node's direction, divided by
   the distance travelled to get there (radians/metre)."
  [nodes]
  (let [positions (mapv pth/position-metres nodes)
        directions (mapv pth/direction nodes)
        n (count nodes)]
    (mapv (fn [i]
            (if (>= (inc i) n)
              0.0
              (let [dist (distance (positions i) (positions (inc i)))]
                (if (< dist 0.01)
                  0.0
                  (let [d (clamp (dot (directions i) (directions (inc i))) -1.0 1.0)]
                    (/ (Math/acos d) dist))))))
          (range n))))

(defn- avg [xs] (/ (reduce + xs) (double (count xs))))

(defn- pearson [xs ys]
  (let [n (count xs)
        mx (avg xs) my (avg ys)
        cov (reduce + (map (fn [x y] (* (- x mx) (- y my))) xs ys))
        vx (reduce + (map (fn [x] (Math/pow (- x mx) 2)) xs))
        vy (reduce + (map (fn [y] (Math/pow (- y my) 2)) ys))]
    (if (or (zero? vx) (zero? vy))
      Double/NaN
      (/ cov (Math/sqrt (* vx vy))))))

(defn- rank [xs]
  (let [sorted-idx (map first (sort-by second (map-indexed vector xs)))]
    (vec (map first (sort-by second (map-indexed vector sorted-idx))))))

(defn- spearman [xs ys]
  (pearson (rank xs) (rank ys)))

(defn segment-stats
  "For each .knw segment, the node-range's average/max computed curvature
   alongside the segment's hypothesised curvature field."
  [pth-path knw-path]
  (let [{:keys [nodes]} (pth/parse pth-path)
        {:keys [segments]} (knw/parse knw-path)
        curvatures (node-curvatures nodes)
        n (count curvatures)]
    (for [{:segment/keys [start-node end-node curvature-hyp unknown]} segments
          :let [lo (min start-node end-node)
                hi (min n (max start-node end-node))
                rng (if (< lo hi) (subvec curvatures lo hi) [(nth curvatures lo)])]]
      {:start start-node :end end-node
       :curvature-hyp curvature-hyp :unknown-field unknown
       :avg-curvature (avg (map #(Math/abs %) rng))
       :max-curvature (apply max (map #(Math/abs %) rng))})))

(defn report!
  "Prints the per-segment comparison and the overall correlation between
   |:segment/curvature-hyp| and computed |curvature|."
  [pth-path knw-path]
  (let [stats (segment-stats pth-path knw-path)]
    (doseq [{:keys [start end curvature-hyp avg-curvature max-curvature]} stats]
      (println (format "seg %3d->%3d  |c4|=%.4f  avg|curv|=%.5f  max|curv|=%.5f"
                        start end (Math/abs curvature-hyp) avg-curvature max-curvature)))
    (let [xs (map #(Math/abs (:curvature-hyp %)) stats)
          y-avg (map :avg-curvature stats)
          y-max (map :max-curvature stats)]
      (println)
      (println "Pearson  |c4| vs avg computed |curvature|:" (pearson xs y-avg))
      (println "Spearman |c4| vs avg computed |curvature|:" (spearman xs y-avg))
      (println "Spearman |c4| vs max computed |curvature|:" (spearman xs y-max)))))

;; Result on BL1.pth / BL1_XFG.knw (the only sample we have):
;;   Pearson  |c4| vs avg curvature  ~ 0.34
;;   Spearman |c4| vs avg curvature  ~ 0.19-0.2
;;   Spearman |c4| vs max curvature  ~ 0.09-0.1
;; Weak/inconclusive - NOT strong enough to confirm curvature-hyp is really
;; curvature. Treat :segment/curvature-hyp's meaning as still open; would
;; need more samples (same car/different track, different car/same track)
;; to make further progress.
