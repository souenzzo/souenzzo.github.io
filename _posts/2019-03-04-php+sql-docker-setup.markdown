---
layout: post
title:  "PHP + SQL docker setup"
date:   2019-03-01
categories: []
tags: []
---

Let's say that it's 2019 and for some odd reason you need to setup a PHP dev env

First, let's start a postgres service.

{% highlight bash %}

docker run --name my-postgres --rm -p 5432:5432 postgres:alpine

{% endhighlight %}


This docker container has to have a name. `my-postgres` in this case. It will be used

To connect a client to postgres, we can run

{% highlight bash %}

docker run --link my-postgres --rm -it postgres:alpine psql -U postgres -h my-postgres

{% endhighlight %}

`--link my-postgres` will make `my-postgres` DNS points to the IP of `my-postgres` container.

Then we say to `psql` connect to `my-postgres` host.

Now we can run the `php` container. 

First, go to your working dir 

{% highlight bash %}

cd awesome-php-project

cat index.php
<!DOCTYPE html>
<html>
<head>
<title>PHP Hello!</title>
</head>
<body>
<?php 
  $conn = pg_connect("host=my-postgres port=5432 user=postgres");
  $result = pg_query($conn, "select * from pg_stat_activity");
  var_dump(pg_fetch_all($result));

?>
</body>
</html>

{% endhighlight %}

See that we are connecting the `pg_connect` to `mypostgres` host.

{% highlight bash %}

docker run \
  --rm \
  --name  my-php \
  --link my-postgres:postgres \
  -p 8000:8000 \
  -v"$(pwd):/usr/src/myapp" \
  -w /usr/src/myapp \
  php:cli sh -c \
  'apt-get update && apt-get install -y libpq-dev && docker-php-ext-configure pgsql -with-pgsql=/usr/local/pgsql && docker-php-ext-install pdo pdo_pgsql pgsql && php -S 0.0.0.0:8000'

{% endhighlight %}

Now you can connect to `http://localhost:8000` and see your awesome app.

For PHP reaons, PHP will not stop with `ctrl+c` nor `ctrl+d`

To stop this container, we need to run `docker kill my-php`
-----------------

Conexao Postgres utilizndo pHP

{% highlight bash %}
<?php
  $bdcon4 = pg_connect("host=my-postgres dbname='nome_do_banco' port=5432 user=postgres");
// $**** define uma variavel de conexao
//coneta a um banco de dados chamado "nome_do_banco" na máquina "my-postgres" com um usuário e senha
if(!$bdcon4)
{ print "Falha deu ruim";
}
//avisa de a  conexao deu errado "!" é a negativa
?>
{% endhighlight %}
------------------------

Insere um dado em uma tabela
{% highlight bash %}
<?php
include_once("conexao.php");
//inclui todos os dados de "conexao.php"

$nome = $_POST['nome'];
$email = $_POST['email'];
$profissao = $_POST['profissao'];
$sql =pg_query($bdcon4, "INSERT INTO usuarios(nome, email, profissao) VALUES('$nome','$email','$profissao')");
// insere dentro da tabela "usuarios" do banco de dados "nome_do_banco" que foi definido na conexao.php acima

pg_close($bdcon4);
?>

{% endhighlight %}
