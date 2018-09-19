# Intellij setup
Turn on annotation processing for project in intellij idea:
```
File -> 
  Settings -> 
    Build, Execution, Deployment -> 
      Compiler -> 
        Annotation processors: Enable annotation processing
```

# Cleaning level db journal/snapshots 
```
gradlew clean
```
or more specific
```
gradlew leveldbCleanup
```

# Running
```
gradlew [clean] run
```
