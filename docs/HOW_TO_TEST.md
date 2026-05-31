# Testing

Run the following commands to trigger test cases. 

The test suite is divided by tags. Each type of test can be run individually using their respective tags as shown below

```bash
gradlew.bat test
gradlew.bat test -PtestTags=unit
gradlew.bat test -PtestTags=sanity,integration,regression
```

Test categories use JUnit 5 `@Tag`: `unit`, `sanity`, `integration`, `regression`.