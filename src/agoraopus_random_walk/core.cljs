(ns agoraopus-random-walk.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [goog.string :as gstring]
            [goog.string.format]
            [re-frame-highcharts.utils :as chart-utils]))

(defn delayed-action
  "Used to execute a function in the future"
  [f d]
  (js/setTimeout f d))

(defn rnd!
  "Produce a uniform random value"
  []
  (js/Math.random))

(defn sqrt
  "Square root of input"
  [x]
  (js/Math.sqrt x))

(defn ln
  "Natural log of input"
  [x]
  (js/Math.log x))

(defn exp
  "Returns Euler's number e raised to the power of input"
  [x]
  (js/Math.exp x))

(defn box-muller
  "Produce a random number drawn from a normal distribution"
  [mu sigma]
  (letfn [(generator
            []
            (let [x1 (- (* 2.0 (rnd!)) 1.0)
                  x2 (- (* 2.0 (rnd!)) 1.0)
                  w (+ (* x1 x1) (* x2 x2))]
              [x1 x2 w]))]
    (let [[x1 _ w] (first (drop-while #(>= (last %) 1.0) (repeatedly generator)))
          w (sqrt (/ (* -2.0 (ln w)) w))
          r1 (* x1 w)]
      (+ (* r1 sigma) mu))))

; Constants defining our time periods:

; We assume t = 1 is one year, represented with milliseconds
(def millis-year (* 1000 60 60 24 365))

; Each increment is 1 day, represented with milliseconds
(def millis-inc (* 1000 60 60 24))

; The two below are constants needed in our Brownian motion
(def millis-inc-fraction (/ millis-inc millis-year))
(def sqrt-millis-inc-fraction (sqrt millis-inc-fraction))

(defn step
  "Produce one step in a Brownian motion with given drift and volatility"
  [mu sigma]
  (let [sigma2 (* sigma sigma)
        sigma2half (/ sigma2 2)
        part-1 (* (- mu sigma2half) millis-inc-fraction)
        part-2 (* sigma (box-muller 0 sqrt-millis-inc-fraction))]
    (+ part-1 part-2)))

(defn progress-data
  "Given the drift and volatility, add a new entry that follows on from the given data"
  [mu sigma data]
  (let [r (step mu sigma)
        last-data (-> data last)
        next-time (+ (first last-data) millis-inc)
        next-value (* (second last-data) (exp r))]
    (conj data (vector next-time next-value))))

; The below are mainly re-frame and UI specifics

(defn random-walk-chart
  "This will produce the data structure needed by highcharts"
  [data]
  (let [output {:title {:text nil}

                :legend {:enabled false}

                :xAxis {:type "datetime"}

                :series [{:type "line"
                          :id "series-1"
                          :name "Random walk"
                          :color "white"
                          :lineWidth 2
                          :data data
                          :tooltip {:valueDecimals 2}}]

                :plotOptions {:series {:animation false}}}]
    output))

;; -- Domino 1 - Event Dispatch -----------------------------------------------

(defn mu-change
  [e]
  (let [mu-pct (-> e .-target .-value)]
    (rf/dispatch [:update-mu (/ mu-pct 100)])))

(defn sigma-change
  [e]
  (let [sigma-pct (-> e .-target .-value)]
    (rf/dispatch [:update-sigma (/ sigma-pct 100)])))

;; -- Domino 2 - Event Handlers -----------------------------------------------

(rf/reg-fx
  :delayed-action
  (fn [[f d]]
    (delayed-action f d)))

(rf/reg-event-fx
  :initialize
  (fn [_ _]
    (let [mu 0.1 ; 10% drift
          sigma 0.2 ; 20% volatility
          delay 10]
      {:db {:mu mu
            :sigma sigma
            :delay delay
            :data (reduce #(progress-data mu sigma %1) [[(.getTime (js/Date.)) 100]] (range 1000))}
       :delayed-action [(fn [] (rf/dispatch [:progress])) delay]})))

(rf/reg-event-fx
  :progress
  (fn [{:keys [db]} [_ _]]
    (let [mu (:mu db)
          sigma (:sigma db)
          delay (:delay db)]
      {:db (assoc db :data (subvec (progress-data mu sigma (:data db)) 1))
       :delayed-action [(fn [] (rf/dispatch [:progress])) delay]})))

(rf/reg-event-db
  :update-mu
  (fn [db [_ mu]]
    (assoc db :mu mu)))

(rf/reg-event-db
  :update-sigma
  (fn [db [_ sigma]]
    (assoc db :sigma sigma)))

;; -- Domino 4 - Query  -------------------------------------------------------

(rf/reg-sub
  :data
  (fn [db _]
    (:data db)))

(rf/reg-sub
  :mu
  (fn [db _]
    (:mu db)))

(rf/reg-sub
  :sigma
  (fn [db _]
    (:sigma db)))

;; -- Domino 5 - View Functions ----------------------------------------------

(defn render-random-walk-chart
  [data]
  (fn
    []
    [:div.chart
     [chart-utils/chart {:chart-meta {:id "random-walk-chart-id" :redo false}
                         :chart-data (random-walk-chart @data)}]]))

(defn render-controls
  [mu-pct sigma-pct]
  [:div
   [:div.control
    [:div.control-label (str "Mu is at " (gstring/format "%.1f" mu-pct) "%")]
    [:input.control-range {:type "range"
                           :name "mu-pct"
                           :min "-100"
                           :max "100"
                           :step "0.1"
                           :value mu-pct
                           :on-change mu-change}]]
   [:div.control
    [:div.control-label (str "Sigma is at " (gstring/format "%.1f" sigma-pct) "%")]
    [:input.control-range {:type "range"
                           :name "sigma-pct"
                           :min "0"
                           :max "200"
                           :step "0.1"
                           :value sigma-pct
                           :on-change sigma-change}]]])

(defn ui
  []
  (let [data (rf/subscribe [:data])
        mu-pct (* 100 @(rf/subscribe [:mu]))
        sigma-pct (* 100 @(rf/subscribe [:sigma]))]
    [:div
     [render-random-walk-chart data]
     [render-controls mu-pct sigma-pct]]))

;; -- Entry Point -------------------------------------------------------------

(defn on-js-reload [])
;; optionally touch your app-state to force rerendering depending on
;; your application
;; (swap! app-state update-in [:__figwheel_counter] inc)

(defn ^:export run
  []
  (rf/dispatch-sync [:initialize])
  (reagent/render [ui] (js/document.getElementById "app")))
