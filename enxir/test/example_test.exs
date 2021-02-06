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
  test "Simple atom raw rep" do
    assert EtfQueryLanguage.query_to_ast([EtfQueryLanguage.Key1])
           == %{
             type: :root,
             children: [
               %{
                 type: :prop,
                 key: EtfQueryLanguage.Key1
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
  test "Simple param" do
    assert EtfQueryLanguage.query_to_ast([[:a, %{v: 42}]])
           == %{
             type: :root,
             children: [%{
               type: :prop,
               key: :a,
               params: %{
                 v: 42
               }
             }
             ]
           }
  end
  test "Simple join param" do
    assert EtfQueryLanguage.query_to_ast([%{[:a, %{v: 42}] => [:b]}])
           == %{
             type: :root,
             children: [
               %{
                 type: :join,
                 key: :a,
                 params: %{
                   v: 42
                 },
                 children: [
                   %{
                     key: :b,
                     type: :prop
                   }
                 ]
               }
             ]
           }
  end
  test "ast to query" do
    assert EtfQueryLanguage.ast_to_query(
             %{
               type: :root,
               children: [
                 %{
                   type: :join,
                   key: :a,
                   params: %{
                     v: 42
                   },
                   children: [
                     %{
                       key: :b,
                       type: :prop
                     }
                   ]
                 }
               ]
             }
           )
           == [%{[:a, %{v: 42}] => [:b]}]
  end
end
