---
layout: post
title:  "How to add react-native in a running cljs project"
date:   2019-03-01
categories: []
tags: []
---

This tutorial will cover how to add react-native in a running cljs project.

I will flow this repo: [souenzzo/cljs-rn-tutorial](https://github.com/souenzzo/cljs-rn-tutorial/commits/master)

To run this app you need to start the repl with all profiles `clj -A:dev:cljs` then call `(user/-main)`

* [commit](https://github.com/souenzzo/cljs-rn-tutorial/commit/3f3ed613a45e6dacfa8b6eee1048749600dfb7e0) 
Add the react-native dependencie

```bash
yarn add react-native 
```

* [commmit](https://github.com/souenzzo/cljs-rn-tutorial/commit/58f4d4850211a21195af15a87d9aebd75f3300fd)
Generate react-native files 

Createa a `app.json` file
```json
{ "name":        "cljsrn"
, "displayName": "cljs+rn"}
```

Append this in your `.gitignore`
```text 
...
android/.gradle/
android/app/build/
```

run `./node_modules/.bin/react-native eject` and `git add ios app.json android`

I also do not like to commit generated files, but it is necessary `:'(`

* [commit](https://github.com/souenzzo/cljs-rn-tutorial/commit/dd2a3c5f1b50b76d53c58d566b70f19e6171b9cc)

Create a `index.js` this this template.

Then you can run `./node_modules/.bin/react-native start` and `./node_modules/.bin/react-native run-android`

To make a `./node_modules/.bin/react-native run-android`, you need to get a emulator running or a phone in the cabble
with android dev stuff configured.

Once you get the app running on android, you can edit the text, save, press `r-r` on the emulator and see the reload

* [commit](https://github.com/souenzzo/cljs-rn-tutorial/commit/217f4ca775aaf90226c0ce61cd3540524985637d)
Configure shadow-cljs
This commit will configure shadow-cljs and move the hello from js to cljs.

After do this, you will need to restart the REPL. That shoud be the only one restart.

* [commit](https://github.com/souenzzo/cljs-rn-tutorial/commit/3ef0602de20210e9c29942c2580cd68e7f0958b9)
Start fulcro app

I started the fulcro. Coping the code from the web and moving the dom ns to react-native.

[WIP] - I need to get the remote working.
