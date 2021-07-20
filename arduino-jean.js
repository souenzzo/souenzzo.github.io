let ligar = async () => {
    await fetch("/ligar")
}

let desligar = async () => {
    await fetch("/desligar");
}

document.body.innerHTML = `
<button onclick="ligar()">Ligar</button>
<button onclick="desligar()">Desligar</button>
`
