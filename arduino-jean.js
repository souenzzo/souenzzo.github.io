let ligar = async () => {
    await fetch("/ligar")
}

let desligar = async () => {
    await fetch("/desligar");
}

window.addEventListener('load', (event) => {
    document.body.innerHTML = `
<button onclick="ligar()">Ligar</button>
<button onclick="desligar()">Desligar</button>
`
});
