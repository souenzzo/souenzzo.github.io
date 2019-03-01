---
layout: post
title:  "Hello World!"
date:   2019-03-01
categories: []
tags: []
---

Starting with `TDB`: Testing Driven Blogging

* Code blocks and syntax highlight

{% highlight clojure %}
(def highlighted [:clojure "code"])
{% endhighlight %}

* Interactive code block

{% klipse %}
(map inc [1 2 3])
{% endklipse %}

* Interactive vis graph

{% klipsevis %}
(def data 
  #js {:nodes (new js/vis.DataSet #js [#js{:id 1 :label "1"} #js{:id 2 :label "2"}])
       :edges (new js/vis.DataSet #js [#js{:from 1 :to 2}])})
(def options #js {})
(new js/vis.Network target data options)
{% endklipsevis %}

Now let's push and see if works!
