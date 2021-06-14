(ns clj-insim.codecs
  (:require [marshal.core :as m]))

(def header
  (m/struct
   :header/size m/ubyte
   :header/type m/ubyte
   :header/request-info m/ubyte
   :header/data m/ubyte))

(def body
  {:isi
   (m/struct
    :body/udp-port m/ushort
    :body/is-flags m/ushort
    :body/insim-version m/ubyte
    :body/prefix m/ubyte
    :body/interval m/ushort
    :body/admin (m/ascii-string 16)
    :body/iname (m/ascii-string 16))

   :ver
   (m/struct
    :body/version (m/ascii-string 8)
    :body/product (m/ascii-string 6)
    :body/insim-version m/ubyte
    :body/spare m/ubyte)})
