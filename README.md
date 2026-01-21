# rate-limiter

ohhhhh yeahhhhhhhh

## Run

```bash
mvn test
mvn exec:java
```

## CLI args

The first argument selects the demo mode:

- (default) no args: single user burst mode
- `multi` / `concurrent` / `threads`: multi-threaded demo hammering the same
  user

Examples:

```bash
mvn exec:java
mvn exec:java -Dexec.args="multi"
```
