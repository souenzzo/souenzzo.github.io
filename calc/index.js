import * as calc from './calc.js';

var output = document.getElementById("output");
var input = document.getElementById("input");
var setOutput = v => output.value = v;
var setInput = v => input.value = v;
var state = {};
const isString = x => typeof x === typeof "";

const handleButton = e => {
    const newState = calc.handler(state, e);
    if (isString(newState.output)) {
        setOutput(newState.output)
    }
    if (isString(newState.input)) {
        setInput(newState.input)
    }
    state = newState;
    return null;
};

var buttons = document.querySelectorAll("button");
for (var button of buttons) {
    button.addEventListener("click", handleButton)
}
