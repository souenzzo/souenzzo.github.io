defmodule ExampleTest do
  use ExUnit.Case
  doctest EtfQueryLanguage

  test "Simple atom" do
    assert EtfQueryLanguage.query_to_ast([:a])
           == %{
             type: :root,
             children: [
               %{
                 type: :prop,
                 key: :a
               }
             ]
           }
  end
  test "Simple join" do
    assert EtfQueryLanguage.query_to_ast([%{a: [:b]}])
           == %{
             type: :root,
             children: [
               %{
                 type: :join,
                 key: :a,
                 children: [
                   %{
                     type: :prop,
                     key: :b
                   }
                 ]
               }
             ]
           }
  end
end
