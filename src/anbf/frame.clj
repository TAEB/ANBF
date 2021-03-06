(ns anbf.frame
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :as pprint]
            [clojure.string :as string]))

; an immutable representation of a terminal window contents
; character attributes underline or blink etc. are not represented, only the foreground colors (affected by boldness) are important for NetHack
(defrecord Frame
  [lines ; vector of 24 Strings representing text on each row of the terminal
   colors ; vector of 24 vectors of keywords representing the FG color for the corresponding character (80 per line)
   cursor]
  anbf.bot.IFrame)

(defmethod print-method Frame [f w]
  (.write w "==== <Frame> ====\n")
  (pprint/write (:lines f) :stream w)
  (.write w (format "\nCursor: %s %s\n" (-> f :cursor :x) (-> f :cursor :y)))
  (.write w "=================\n"))

(def colormap
  [nil :red :green :brown :blue ; non-bold
   :magenta :cyan :gray
   :bold :orange :bright-green :yellow ; bold
   :bright-blue :bright-magenta :bright-cyan :white
   :inverse :inv-red :inv-green :inv-brown ; inverse
   :inv-blue :inv-magenta :inv-cyan :inv-gray
   :inv-bold :inv-orange :inv-bright-green :inv-yellow ; inverse+bold
   :inv-bright-blue :inv-bright-magenta :inv-bright-cyan :inv-white])

(defn inverse? [color]
  (#{:inverse :inv-red :inv-green :inv-brown :inv-blue :inv-magenta
     :inv-cyan :inv-gray :inv-bold :inv-orange :inv-bright-green
     :inv-yellow :inv-bright-blue :inv-bright-magenta :inv-bright-cyan
     :inv-white} color))

(defn non-inverse [color]
  (get {:inv-bright-green :bright-green, :inv-green :green, :inverse nil, :inv-brown :brown, :inv-orange :orange, :inv-magenta :magenta, :inv-blue :blue, :inv-bright-cyan :bright-cyan, :inv-cyan :cyan, :inv-red :red, :inv-bold :white, :inv-bright-magenta :bright-magenta, :inv-yellow :yellow, :inv-bright-blue :bright-blue, :inv-gray :gray, :inv-white :white} color color))

(defn print-colors [f]
  (println "Colors:")
  (doseq [c (:colors f)]
    #(if (every? nil? c)
       (println nil)
       (println c))))

(defn nth-line
  "Returns the line of text on n-th line of the frame."
  [frame n]
  ((:lines frame) n))

(defn botls [frame]
  (subvec (:lines frame) 22))

(defn cursor-line
  "Returns the line of text where the cursor is on the frame."
  [frame]
  (nth-line frame (-> frame :cursor :y)))

(defn before-cursor
  "Returns the part of the line to the left of the cursor."
  [frame]
  (subs (cursor-line frame) 0 (-> frame :cursor :x)))

(defn before-cursor?
  "Returns true if the given text appears just before the cursor."
  [frame text]
  (.endsWith ^String (before-cursor frame) text))

(defn topline [frame]
  (-> frame (nth-line 0) string/trim))

(defn topline+
  "Returns the top line with possible overflow on the second line appended."
  [frame]
  (if (= 1 (-> frame :cursor :y))
    (str (topline frame) " " (string/trim (cursor-line frame)))
    (topline frame)))

(defn looks-engulfed? [{:keys [cursor lines] :as frame}]
  (if (and (< 0 (:x cursor) 79)
           (< 1 (:y cursor) 21))
    (let [row-before (dec (:x cursor))
          row-after (inc (:x cursor))
          line-above (nth lines (dec (:y cursor)))
          line-at (nth lines (:y cursor))
          line-below (nth lines (inc (:y cursor)))]
      (and (or (= 1 (:y cursor))
               (= "/-\\" (subs line-above row-before (inc row-after))))
           (re-seq #"\|.\|" (subs line-at row-before (inc row-after)))
           (or (= "\\-/" (subs line-below row-before (inc row-after)))
               (= 21 (:y cursor)))))))
