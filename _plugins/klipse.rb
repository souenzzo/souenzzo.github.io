class Klipse < Liquid::Block
  def initialize(tag_name, input, tokens)
    super
  end

  def render(context)
    contents = super

    # pipe param through liquid to make additional replacements possible
    content = Liquid::Template.parse(contents).render context

    "<pre><code class=\"lang-eval-clojure language-klipse\">" + content + "</code></pre>"
  end
end

Liquid::Template.register_tag('klipse', Klipse)

class KlipseVis < Liquid::Block
  def initialize(tag_name, input, tokens)
    super
  end

  def render(context)
    contents = super

    # pipe param through liquid to make additional replacements possible
    content = Liquid::Template.parse(contents).render context

    content2 = "(def target (.getElementById js/document \"my-graph\"))\n" + content

    "<div id=\"my-graph\"></div><pre><code class=\"lang-eval-clojure language-klipse\">" + content2 + "</code></pre>"
  end
end

Liquid::Template.register_tag('klipsevis', KlipseVis)

