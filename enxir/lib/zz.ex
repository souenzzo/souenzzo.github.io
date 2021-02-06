defmodule ZZ do
  require Logger

  def accept() do
    {:ok, socket} =
      :gen_tcp.listen(
        8080,
        [
          :binary,
          packet: :line,
          active: false,
          reuseaddr: true
        ]
      )
    loop_acceptor(socket)
  end

  def loop_acceptor(socket) do
    Logger.info("loop_acceptor")
    {:ok, client} = :gen_tcp.accept(socket)
    spawn fn ->
      serve(client)
    end
    loop_acceptor(socket)
  end
  def serve(socket) do
    Logger.info("serve")
    {:ok, data} = :gen_tcp.recv(socket, 0)
    Logger.info(data)
    Logger.info(IEx.Info.info(data))
    :gen_tcp.send(socket, data)
    serve(socket)
  end
  def hello() do
    :xx
  end
end
