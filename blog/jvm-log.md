# Logs na JVM

Logs na JVM são bastante complexos, principalmente por motivos legados

A JVM não tinha log em sua biblioteca padão, então houveram várias bibliotecas que se auto-denominaram padrão.

O ecossistema evoluiu para que todas essas fossem compativeis, intercambiaveis e configuraveis. Com isso, terminamos com uma solução complexa.

Vamos separar esse problema complexo em 2 estágios: produção e consumo.

## Produção

Você sempre pode usar o produtor de logs que bem entender. Eu gosto da `io.pedestal/pedestal.log` por exemplo.

Vamos tentar usar a `pedestal.log`

```bash
~ clj -Sdeps '{:deps {io.pedestal/pedestal.log {:mvn/version "0.5.8"}}}' 
Clojure 1.10.1
user=> (require '[io.pedestal.log :as log])
SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
SLF4J: Defaulting to no-operation (NOP) logger implementation
SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
nil
user=> (log/info :hello "world")
nil
user=> 
```

Ok. Nem fiz nada, já ganhei um erro. Tentei usar a biblioteca, por padrão ela não funciona. Vamos entender isso.

O `io.pedestal/pedestal.log` implementa logs usando o `org.slf4j/slf4j-api`. O `org.slf4j/slf4j-api` por sua vez, é uma API abstrata que recebe os logs.

Existem pelo menos 5 implementações segundo esse diagrama. O erro que vimos é acima para o caso `0`, onde não há implementação e ele vai para `/dev/null`

[diagrama de implementações do slf4j]: https://www.slf4j.org/images/concrete-bindings.png

Existem basicamente 2 tipos de implementação: 

1. Adaptação (aqua no diagrama): Ele "adapta" e repassa para outro produtor de logs (passa a bola para frente)
2. Framework (azul no diagrama): Ele recebe o log do slf4j e efetivamente faz algo (chuta a bola para o gol)*

> Vamos ver em breve que o `2. Framework` também pode passar a bola para frente.

Eu por falta de tempo, gosto de usar o `logback-classic`, pois é o que aprendi a configurar.

```
;; DUMP/WORK IN PROGRESS
e essas paradas de log só pode ter 1 no classpath
19h57
se tem 2 framework de log ao mesmo tempo, elas ficam de mau uma com a outra e explodem a JVM
19h57
(implementação low-level de log, as de clojure são compativeis entre si)
```

## Consumo 

Appenders do logback

```
;; DUMP/WORK IN PROGRESS
Ai o logback le o arquivo resource/logback.xml
Lá a gente especifica "para onde ele manda os logs"
No caso, a gemte tem varios logback.xml: um para usar no REPL, um para usar durante os testes, e um de produção
O de produção, que é resources/logback.xml tem 2 appendes:
- Arquivo
- STDOUT
Arquivo escreve em arquivo
STDOUT escreve em STDOUT
Poderia ter um appender de slack por exemplo
Poderia ter um appender de cloudwatch por exemplo
```


# Curiosidades

Vc pode sobrescrever o logger do pedestal
- https://github.com/pedestal/pedestal/blob/master/log/src/io/pedestal/log.clj#L250
