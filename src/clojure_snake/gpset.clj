(ns clojure-snake.gpset
  (:import (java.awt Color Dimension)
           (javax.swing JPanel JFrame Timer JOptionPane WindowConstants)
           (java.awt.event ActionListener KeyListener))
  (:use clojure-snake.util.import-static))

(def snake (ref {}))
(def apple (ref {}))
(def direction (ref {}))
(def steps (ref {}))
(def score (ref {}))
(def snake-routine (ref {}))

(def WIDTH "Width of the game board" 19)
(def HEIGHT "Height of the game board" 10)
(def LEFT "Left direction" [-1 0])
(def RIGHT "Right direction" [1 0])
(def UP "Up direction" [0 -1])
(def DOWN "Down direction" [0 1])
(def RIGHT-TURN "Right turn vector" [1 -1])
(def LEFT-TURN "Left turn vector" [-1 1])

(defn set-routine
  [routine]
  (dosync
   (ref-set snake-routine routine)))

(defn create-apple
  "Create an apple."
  []
  {:location [(rand-int WIDTH) (rand-int HEIGHT)]
   :color (Color. 210 50 90)
   :type :apple})

(defn create-snake
  "Create the snake."
  []
  {:body (for [x (range 8 -1 -1)] [x 10])
   :type :snake
   :color (Color. 15 160 70)
   :score 0})

(defn add-points
  "Add vector points."
  [& pts]
  (vec (apply map + pts)))

(defn eats?
  "Check if the snake eats an apple."
  [{[snake-head] :body} {apple :location}]
  (= snake-head apple))

(defn change-direction
  "Change direction of the snake."
  [old-dir turn]
  (vec (reverse (map * old-dir turn))))

(defn move
  "Move the snake in a given direction."
  [{:keys [body] :as snake} dir apple-loc]
  (assoc snake :body (cons (add-points (first body) dir)
                           (if (eats? snake apple-loc)
                             (do
                               (ref-set apple (create-apple))
                               (ref-set score (inc @score))
                               body)
                             (butlast body)))))

(defn out-of-bounds?
  "Check if the snake is out of bounds (wall hit)."
  [{[head] :body}]
  (or (< (head 0) 0)
      (> (head 0) WIDTH)
      (< (head 1) 0)
      (> (head 1) HEIGHT)))

(defn head-overlaps-body?
  "Check if the snake has collided with itself."
  [{[head & body] :body}]
  (contains? (set body) head))

(defn lose? [snake]
  (or (head-overlaps-body? snake) (out-of-bounds? snake)))

(defn change-direction
  "Change direction of the snake."
  [old-dir turn]
  (vec (reverse (map * old-dir turn))))

;;;
;;; Util
;;;
(defn food-ahead?
  "Check if an apple is in line with the snake's current direction."
  [{[head] :body} dir {apple :location}]
  (let [x-head (first head) y-head (last head)
        x-apple (first apple) y-apple (last apple)]
    (cond
     (= dir UP) (if (and (= x-head x-apple) (> y-head y-apple)) true false)
     (= dir DOWN) (if (and (= x-head x-apple) (< y-head y-apple)) true false)
     (= dir LEFT) (if (and (= y-head y-apple) (> x-head x-apple)) true false)
     (= dir RIGHT) (if (and (= y-head y-apple) (< x-head x-apple)) true false))))

(defn danger-ahead?
  "Check if position ahead of current snake's direction is occupied by a wall or snake segment."
  [{[head] :body :as snake} dir]
  (let [next-pos (add-points head dir)]
    (if (or (out-of-bounds? {:body (list next-pos)}) (head-overlaps-body? {:body (conj (:body snake) next-pos)}))
      true
      false)))

(defn danger-right?
  "Check if position to the right of current snake's direction is occupied by a wall or snake segment."
  [{[head] :body :as snake} dir]
  (let [next-pos (add-points head (change-direction dir RIGHT-TURN))]
    (if (or (out-of-bounds? {:body (list next-pos)}) (head-overlaps-body? {:body (conj (:body snake) next-pos)}))
      true
      false)))

(defn danger-left?
  "Check if position to the left of current snake's direction is occupied by a wall or snake segment."
  [{[head] :body :as snake} dir]
  (let [next-pos (add-points head (change-direction dir LEFT-TURN))]
    (if (or (out-of-bounds? {:body (list next-pos)}) (head-overlaps-body? {:body (conj (:body snake) next-pos)}))
      true
      false)))

(defn danger-two-ahead?
  "Check if position two steps ahead of current snake's direction is occupied by a wall or snake segment."
  [{[head] :body :as snake} dir]
  (let [next-pos (add-points (add-points head dir) dir)]
    (if (or (out-of-bounds? {:body (list next-pos)}) (head-overlaps-body? {:body (conj (:body snake) next-pos)}))
      true
      false)))

(defn food-up?
  "Check if the current apple on the board is closer to the top of the board than the snake's head."
  [{[head] :body} {apple :location}]
  (let [head-y (head 1)
        apple-y (apple 1)]
    (if (> head-y apple-y)
      true
      false)))

(defn food-right?
  "Check if the current apple on the board is further to the right of the board than the snake's head."
  [{[head] :body} {apple :location}]
  (let [head-x (head 0)
        apple-x (apple 0)]
    (if (< head-x apple-x)
      true
      false)))

(defn moving-right?
  "Check if the snake's current direction is right."
  [dir]
  (if (= dir RIGHT)
    true
    false))

(defn moving-left?
  "Check if the snake's current direction is left."
  [dir]
  (if (= dir LEFT)
    true
    false))

(defn moving-up?
  "Check if the snake's current direction is up."
  [dir]
  (if (= dir UP)
    true
    false))

(defn moving-down?
  "Check if the snake's current direction is down."
  [dir]
  (if (= dir DOWN)
    true
    false))

(defn distance-from-food
  "Calculate the snake's distance from food."
  [{[head] :body} {apple :location}]
  (let [head-x (head 0) head-y (head 1)
        apple-x (apple 0) apple-y (apple 1)]
    (+ (Math/abs (- head-x apple-x)) (Math/abs (- head-y apple-y)))))


;;;
;;; GP terminals
;;;
(defn turn-right
  "Make the snake turn right."
  []
  (ref-set direction (change-direction @direction RIGHT-TURN))
  (ref-set snake (move @snake @direction @apple))
  (ref-set steps (inc @steps)))

(defn turn-left
  "Make the snake turn left."
  []
  (ref-set direction (change-direction @direction LEFT-TURN))
  (ref-set snake (move @snake @direction @apple))
  (ref-set steps (inc @steps)))

(defn move-forward
  "Make to snake continue forward."
  []
  (ref-set snake (move @snake @direction @apple))
  (ref-set steps (inc @steps)))

;;;
;;; GP functions
;;;
;;; (initial function set)
;;;
(defmacro if-food-ahead
  "GP food ahead macro."
  [food no-food]
  `(if (food-ahead? @snake @direction @apple)
     ~food
     ~no-food))

(defmacro if-danger-ahead
  "GP danger ahead macro."
  [danger no-danger]
  `(if (danger-ahead? @snake @direction)
     ~danger
     ~no-danger))

(defmacro if-danger-right
  "GP danger right macro."
  [danger-right no-danger-right]
  `(if (danger-right? @snake @direction)
     ~danger-right
     ~no-danger-right))

(defmacro if-danger-left
  "GP danger left macro."
  [danger-left no-danger-left]
  `(if (danger-left? @snake @direction)
     ~danger-left
     ~no-danger-left))

;;; function (do exprs*) is used as progn2

;;;
;;; GP functions
;;;
;;; (full function set)
;;;
(defmacro if-danger-two-ahead
  "GP danger two ahead macro."
  [danger-two-ahead no-danger-two-ahead]
  `(if (danger-two-ahead? @snake @direction)
     ~danger-two-ahead
     ~no-danger-two-ahead))

(defmacro if-food-up
  "GP food up macro."
  [food-up no-food-up]
  `(if (food-up? @snake @apple)
     ~food-up
     ~no-food-up))

(defmacro if-food-right
  "GP food right macro."
  [food-right no-food-right]
  `(if (food-right? @snake @apple)
     ~food-right
     ~no-food-right))

(defmacro if-moving-right
  "GP moving right macro."
  [moving-right no-moving-right]
  `(if (moving-right? @direction)
     ~moving-right
     ~no-moving-right))

(defmacro if-moving-left
  "GP moving left macro."
  [moving-left no-moving-left]
  `(if (moving-left? @direction)
     ~moving-left
     ~no-moving-left))

(defmacro if-moving-up
  "GP moving up macro."
  [moving-up no-moving-up]
  `(if (moving-up? @direction)
     ~moving-up
     ~no-moving-up))

(defmacro if-moving-down
  "GP moving down macro."
  [moving-down no-moving-down]
  `(if (moving-down? @direction)
     ~moving-down
     ~no-moving-down))
