# uberzip

# idea

```bash 
## on build
clj -Spath | uberzip -o app.zip
## on run
unzip app.zip
java -cp "$(cat app.classpath)" -m app.main
```

`uberzip` will receive a classpath and put all entries on a zip
It will also save the classpath on a file