defmodule ExampleTest do
  use ExUnit.Case
  doctest ZZ

  test "greets the world" do
    assert ZZ.hello() == :world
  end
end
