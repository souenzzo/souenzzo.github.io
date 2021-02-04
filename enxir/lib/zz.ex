defmodule ZZ do
  require Logger

  def accept() do
    port = 8080
    {:ok, socket} =
      :gen_tcp.listen(
        port,
        [
          :binary,
          packet: :line,
          active: false,
          reuseaddr: true
        ]
      )
    Logger.info("Accepting connections on port #{port}")
    loop_acceptor(socket)
  end

  defp loop_acceptor(socket) do
    {:ok, client} = :gen_tcp.accept(socket)
    serve(client)
    loop_acceptor(socket)
  end

  defp serve(socket) do

    x = read_line(socket)
    Logger.info("read:")
    Logger.info(x)
    Logger.info(IEx.Info.info(x))
    write_line(x, socket)
    write_line(x, socket)
    serve(socket)
  end

  defp read_line(socket) do
    {:ok, data} = :gen_tcp.recv(socket, 0)
    data
  end

  defp write_line(line, socket) do
    :gen_tcp.send(socket, line)
  end
end
