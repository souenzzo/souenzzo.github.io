defmodule EtfQueryLanguage do
  def query_to_ast(query) do
    %{
      type: :root,
      children: Enum.map(
        query,
        fn x ->
          if is_atom(x) do
            %{
              type: :prop,
              key: x
            }
          else
            [key | _] = Map.keys(x)
            {:ok, el} = Map.fetch(x, key)

            el
            |> query_to_ast()
            |> Map.put(:type, :join)
            |> Map.put(:key, key)
          end
        end
      )
    }
  end

end
