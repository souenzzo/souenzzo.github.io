const reDigit = RegExp("^[0-9]$");
const isDigit = x => reDigit.test(x);
const isEq = x => x === "=";
const isDel = x => x === "\uD83D\uDDD1";

export const handler = (state, e) => {
    var {input = ""} = state;
    var x = e.target.innerHTML;
    return isEq(x)
        ? {...state, output: `${eval(input)}`, input: ""}
        : isDel(x)
            ? {...state, input: input.substring(0, input.length - 1)}
            : {...state, input: `${input}${x}`};
};