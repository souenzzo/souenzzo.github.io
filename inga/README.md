# ingá

O `ingá` tem o proposito de facilitar a criação de componentes reutilizaveis no ecossitema `hiccup`.

Deve ser possivel descrever o uso de um componente de uma forma simples, onde apenas com dados
se descreva:
- Qual componente será usado
- Uma descrição de qual dado deve ser renderizado. 

Exemplo

```
[:div
  [::table {::joi-key :todo/all-todos
            ::display-properties [::text
                                  ::done?]}]
  [::form {::op `new-todo}]]
```

Neste exemplo podemos observar
- Keywords qualificados identificam componentes que devem ser renderizados
- Um componente recebe parametros, que descreve como obter os dados necessários para exibir o componente
