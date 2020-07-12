---
layout: post
title:  Clojure no heroku
---

Costumo dar nomes de arvores (nesse caso, um fruto) em meus projetos, esse se chama [atemoia](https://pt.wikipedia.org/wiki/Atemoia)

0. Em um diretório vazio, crie o arquivo `deps.edn`
```bash
## Criando a pasta vazia
$ mkdir atemoia
## Entrando na pasta criada
$ cd atemoia
## Criando o arquivo, em branco
$ > deps.edn
``` 

Este é o arquivo onde declaramos as dependencias externas do nosso projeto.

`deps.edn`
```clojure
{:paths ["src"]
 :deps  {org.clojure/clojure          {:mvn/version "1.10.1"}
         org.clojure/tools.deps.alpha {:mvn/version "0.8.695"}
         io.pedestal/pedestal.jetty   {:mvn/version "0.5.8"}
         io.pedestal/pedestal.service {:mvn/version "0.5.8"}
         hiccup/hiccup                {:mvn/version "2.0.0-alpha2"}}} 
```

Uma vez declaradas nossas dependencias, podemos abrir nosso REPL, que não precisa ser fechado e vc deve
sempre fazer as coisas nele.

> Recomendo que vc use um RPEL integrado ao seu editor de texto. Mas vou fazer como se não estivesse.

```bash 
$ clj 
Clojure 1.10.1
user=> 
```

Vou criar o namespace `br.com.souenzzo.atemoia`. Você pode chamar como preferir.

```bash 
mkdir -p src/br/com/souenzzo
> src/br/com/souenzzo/atemoia.clj
``` 

Neste arquivo, vamos criar um "hello-world" do pedestal

`src/br/com/souenzzo/atemoia.clj`
```clojure
(ns br.com.souenzzo.atemoia
  (:require [io.pedestal.http :as http]
            [clojure.edn :as edn]))

(defn hello
  [req]
  {:body   "ok"
   :status 200})

(def routes
  "Aqui é a tabela de rotas do pedestal. Caso um `get` em `/`, então
chama a função `hello`" 
  `#{["/" :get hello]})

(def port
  "Heroku por padrão entrega a porta que a aplicação deve funcionar
na variavel de ambiente `PORT`. Para desenvolvimento local, vamos usar
a porta 5000" 
  (or (edn/read-string (System/getenv "PORT"))
      5000))

(def service-map
  "Configurações do pedestal:
- porta
- host: para receber requisições de servidores externos
- routes: tabela de rotas
- type: pedestal suporta varias plataformas. Vamos usar o jetty, que é bem comum no mundo java.
- join?: diz se o processo deve continuar após subir o servidor.
"
  {::http/port   port
   ::http/host   "0.0.0.0"
   ::http/routes routes
   ::http/type   :jetty
   ::http/join?  false})

;; Aqui vamos armazenar o "estado" do servidor HTTP.
;; será util para a poder parar o servidor quando quiser
(defonce state
         (atom nil))

(defn -main
  [& _]
  (swap! state
         (fn [st]
           (when st
             (http/stop st))
           (-> service-map
               http/default-interceptors
               http/create-server
               http/start))))
```

Vamos carregar esse arquivo no REPL

```clojure
;; seu editor de texto provavelmente tem um comando
;; "carregar arquivo para o repl"
;; que executa isso para vc
user=> (require 'br.com.souenzzo.atemoia :reaload)
```

E chamar a função main

```clojure
user=> (br.com.souenzzo.atemoia/-main)
```

Com isso, devemos ter um servidor HTTP rodando em sua porta 5000

Podemos testar no proprio REPL
```clojure
user=> (slurp "http://localhost:5000")
"ok"
user=>
```

Com isso, vamos criar um Dockerfile que execute o código.

`Dockerfile`
```dockerfile
## partimos de uma imagem com clojure tool deps.
## uso alpine apenas por ser menor
FROM clojure:tools-deps-alpine

## criando um usuário regular dentro do docker
## evita problemas com o ~/.m2
RUN adduser -D atemoia
USER atemoia
## copiando a pasta inteira.
## em algum momento vamos fazer um .dockerignore
ADD --chown=atemoia . /srv/atemoia
WORKDIR /srv/atemoia

## esse comando vai baixar as dependencias do aplicativo durante
## o build da aplicação
RUN clojure -Spath
CMD clojure -m br.com.souenzzo.atemoia
```

Já temos tudo pronto para o heroku, basta dizer que o target "web" usa esse dockerfile.

`heroku.yml`
```yaml
build:
  docker:
    web: Dockerfile
```

Basta criar um `git` e colocar tudo dentro 

```bash 
## criando um git no a pasta atual
$ git init .
## adicionando arquivos relevantes
$ git add .gitignore Dockerfile deps.edn heroku.yml src/br/com/souenzzo/atemoia.clj
## commitando tudo
$ git comm  -am'Commit inicial'
```

Após criar o git, precisamos sincronizar ele com o github

```bash 
$ git remote add origin 'ssh://github.com:souenzzo/atemoia.git'
$ git push origin master
```

E então, criamos um aplicativo no Heroku

Ao criar o aplicativo, o heroku irá te perguntar o metodo de deploy.

Ali você deve escolher que quer "integração com github"

Aponte seu repositório

O primeiro deploy (se não me engano) previsa ser manual

---

Em caso de duvidas ou comentários, abra uma issue ou entre em contato comigo

https://github.com/souenzzo/souenzzo.github.io/issues
