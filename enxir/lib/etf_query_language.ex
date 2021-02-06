defmodule EtfQueryLanguage do
  # require Logger
  ## Logger.info(IEx.Info.info(x))
  def element_to_node(element) when is_atom(element) do
    %{
      type: :prop,
      key: element
    }
  end
  def element_to_node(element) when is_list(element) do
    split_key_params(element)
    |> Map.put(:type, :prop)
  end
  def element_to_node(element) do
    [k, el] = element
              |> Map.to_list()
              |> List.first()
              |> Tuple.to_list()
    el
    |> query_to_ast()
    |> Map.put(:type, :join)
    |> Map.merge(split_key_params(k))
  end
  def split_key_params(key) when is_atom(key) do
    %{key: key}
  end
  def split_key_params(key) do
    [key, params] = key
    %{key: key, params: params}
  end
  def query_to_ast(query) do
    %{
      type: :root,
      children: Enum.map(query, &element_to_node/1)
    }
  end
  def node_to_query(node) do
    key = if Map.has_key?(node, :params) do
      [node.key, node.params]
    else
      node.key
    end
    if Map.has_key?(node, :children) do
      %{key => ast_to_query(node)}
    else
      key
    end
  end
  def ast_to_query(ast) do
    Enum.map( ast.children, &node_to_query/1)
  end
end
